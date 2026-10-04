package com.local.listentomusic.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * One MediaSession/primary player; up to seven companions. Companion video is
 * audio-only. Optional Fit tiles use silent, video-only previews so a decorative
 * decoder stall cannot pause the audio synchronization group.
 *
 * Sync model (1.15.59):
 * - Start gate: after start/seek/resume/primary swap/stall, every player is parked and
 *   released in the same main-looper message once all are READY, so no voice joins late.
 * - Drift is corrected with a pitch-preserving rate trim (at most ±5%) instead of seeks.
 *   Seeking a live voice forces a rebuffer that leaves it behind again, which was the
 *   source of the stutter/echo loop. Seeks remain only for large drift, with a learned lead.
 */
internal class StackPlaybackCoordinator(
    private val context: Context,
    private val main: ExoPlayer,
    private val scope: CoroutineScope,
    private val onLevelsChanged: () -> Unit,
) {
    private class Voice(var file: MediaFile, val player: ExoPlayer) {
        var driftEma = 0.0
        var hasDrift = false
        var settleUntilMs = 0L
        var lastSeekMs = 0L
        var lastRateMs = 0L
        var rateTrim = 1f
        var seekLeadMs = STACK_INITIAL_SEEK_LEAD_MS
        var measuringSeekResidual = false
        var bufferingSinceMs = 0L
        var controlledSeekUntilMs = 0L
        var videoOwner: Any? = null
        var videoView: androidx.media3.ui.PlayerView? = null
        var videoUnavailable = false
        var videoFrames = 0
        var videoPreview: ExoPlayer? = null
        var lastVideoSyncMs = 0L
        var lastVideoSeekMs = 0L

        fun resetDrift(nowMs: Long) {
            driftEma = 0.0
            hasDrift = false
            settleUntilMs = nowMs + STACK_SETTLE_MS
        }
    }

    private val voices = mutableListOf<Voice>()
    private val slots = mutableListOf<StackSlot>()
    private var primaryPath: String? = null
    private var anchorMs = 0L
    private var anchorTimeMs = 0L
    private var playing = false
    private var clockSpeed = 1f
    private var sourceGain = 1f
    private var previousRepeat = Player.REPEAT_MODE_OFF
    private var previousShuffle = false
    private var loopEnabled = false
    private var ticker: Job? = null
    private var internalMainChange = false
    private var released = false
    private var gateDeadlineMs = 0L
    private var armingGate = false
    private var lastPublishMs = 0L
    private var lastFailureReportMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gated: Boolean get() = gateDeadlineMs != 0L
    val active: Boolean get() = slots.isNotEmpty()
    val primary: String? get() = primaryPath
    val changingMain: Boolean get() = internalMainChange
    val repeatModeForPersistence: Int
        get() = persistedRepeatMode(active, previousRepeat, main.repeatMode)

    fun voiceDiagnostics(): String = voices.mapIndexed { index, voice ->
        "${index + 1}:${voice.player.playbackState}/${voice.player.isPlaying}@${voice.player.currentPosition}ms" +
            " trim=${voice.rateTrim} drift=${voice.driftEma.toLong()}ms lead=${voice.seekLeadMs}ms" +
            " tile=${voice.videoView != null} video=${voice.videoPreview?.videoSize?.width ?: 0}x${voice.videoPreview?.videoSize?.height ?: 0} frames=${voice.videoFrames}"
    }.joinToString(",").ifEmpty { "none" }

    private fun valid(file: MediaFile): Boolean {
        val source = File(file.sourcePath)
        return source.isFile && source.canRead() && MediaScanner.isInsideTarget(source) &&
            source.extension.lowercase() in MediaScanner.supportedExtensions
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == mainHandler.looper) block() else mainHandler.post(block)
    }

    private fun safePlayer(op: () -> Unit) {
        try { op() } catch (failure: Exception) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastFailureReportMs > 5_000L) {
                lastFailureReportMs = now
                com.local.listentomusic.diagnostics.CrashReports.recordRecoverable("stack-player", failure)
            }
        }
    }

    private fun releaseVoice(voice: Voice) {
        detachVideo(voice)
        safePlayer { voice.player.stop() }
        safePlayer { voice.player.release() }
    }

    private fun detachVideo(voice: Voice) {
        voice.videoOwner = null
        voice.videoView?.let { view -> safePlayer { view.player = null } }
        voice.videoView = null
        val preview = voice.videoPreview
        voice.videoPreview = null
        preview?.let { safePlayer { it.release() } }
    }

    private fun failVideo(voice: Voice, failure: Exception) {
        voice.videoUnavailable = true
        detachVideo(voice)
        val index = slots.indexOfFirst { it.file.path == voice.file.path }
        if (index >= 0) slots[index] = slots[index].copy(videoUnavailable = true)
        com.local.listentomusic.diagnostics.CrashReports.recordRecoverable("stack-video-tile", failure)
        publish()
    }

    /** Presentation only: no duplicate audio, primary reload or quality constraint. */
    fun attachVideo(path: String, owner: Any, view: androidx.media3.ui.PlayerView?) = onMain {
        val voice = voices.firstOrNull { it.file.path == path } ?: return@onMain
        if (view == null) {
            if (voice.videoOwner === owner) detachVideo(voice)
        } else if (!released && !voice.videoUnavailable) {
            if (voice.videoOwner === owner && voice.videoView === view) return@onMain
            voice.videoView?.let { old -> safePlayer { old.player = null } }
            voice.videoOwner = owner
            voice.videoView = view
            try {
                val preview = voice.videoPreview ?: createVideoPreview(voice).also { voice.videoPreview = it }
                view.player = preview
            } catch (failure: Exception) {
                failVideo(voice, failure)
            }
        }
    }

    private fun createVideoPreview(voice: Voice): ExoPlayer {
        val preview = ExoPlayer.Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1_500, 6_000, 100, 200)
                .setTargetBufferBytes(4 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(true).build())
            .build()
        try {
            preview.trackSelectionParameters = preview.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
            preview.volume = 0f
            preview.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() { if (voice.videoPreview === preview) voice.videoFrames++ }
                override fun onPlayerError(error: PlaybackException) {
                    if (!released && voice.videoPreview === preview) failVideo(voice, error)
                }
            })
            preview.setMediaItem(voice.file.toMediaItem(), voice.player.currentPosition)
            preview.playbackParameters = voice.player.playbackParameters
            preview.playWhenReady = voice.player.playWhenReady && playing && !gated
            preview.prepare()
            return preview
        } catch (failure: Exception) { preview.release(); throw failure }
    }

    private fun syncVideo(voice: Voice, nowMs: Long) {
        val preview = voice.videoPreview ?: return
        if (nowMs - voice.lastVideoSyncMs < 250L) return
        voice.lastVideoSyncMs = nowMs
        preview.playWhenReady = voice.player.playWhenReady && playing && !gated
        if (preview.playbackParameters != voice.player.playbackParameters)
            preview.playbackParameters = voice.player.playbackParameters
        if (shouldRealignStackVideo(preview.currentPosition, voice.player.currentPosition,
                preview.playbackState == Player.STATE_READY, nowMs - voice.lastVideoSeekMs)) {
            voice.lastVideoSeekMs = nowMs
            preview.seekTo(voice.player.currentPosition)
        }
    }

    private fun createVoice(file: MediaFile): Voice? {
        var engine: ExoPlayer? = null
        return runCatching {
        ExoPlayer.Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1_500, 6_000, 100, 200)
                .setTargetBufferBytes(2 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).build())
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().also { engine = it }.also { engine ->
                engine.trackSelectionParameters = engine.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
                engine.repeatMode = Player.REPEAT_MODE_OFF
                engine.skipSilenceEnabled = false
                engine.setMediaItem(file.toMediaItem())
                engine.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_BUFFERING) {
                            onMain {
                                val voice = voices.firstOrNull { it.player === engine }
                                if (voice != null && voice.bufferingSinceMs == 0L)
                                    voice.bufferingSinceMs = SystemClock.elapsedRealtime()
                            }
                        }
                        if (playbackState != Player.STATE_READY) return
                        onMain { onVoiceReady(engine) }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        // Same pattern as Parallel layers: mutate on the main looper only.
                        onMain {
                            if (released || !active) return@onMain
                            val failed = voices.firstOrNull { it.player === engine } ?: return@onMain
                            voices.remove(failed)
                            releaseVoice(failed)
                            val index = slots.indexOfFirst { it.file.path == failed.file.path }
                            if (index >= 0) slots[index] = slots[index].copy(error = "Playback unavailable")
                            // Fewer than two healthy tracks is normal single-track playback.
                            if (slots.count { it.error == null } < 2) {
                                stop(clearMain = false)
                            } else {
                                if (gated) tryOpenStartGate()
                                publish()
                                onLevelsChanged()
                            }
                        }
                    }
                })
                engine.prepare()
            }.let { Voice(file, it) }
        }.getOrElse { engine?.release(); null }
    }

    private fun boundedSeek(positionMs: Long, durationMs: Long): Long =
        if (durationMs > 0L) positionMs.coerceIn(0L, durationMs) else positionMs.coerceAtLeast(0L)

    private fun voicePosition(file: MediaFile, masterMs: Long): Long =
        stackVoiceTarget(masterMs, slots.firstOrNull { it.file.path == file.path }?.offsetMs ?: 0L)

    private fun voiceInWindow(voice: Voice, masterMs: Long): Boolean {
        val target = voicePosition(voice.file, masterMs)
        val duration = knownDuration(voice.file)
        return target >= 0L && (duration <= 0L || target < duration)
    }

    /** Rate trim is relative to the master's own speed; pitch always follows the master. */
    private fun applyRate(voice: Voice, trim: Float) {
        val base = main.playbackParameters
        val next = PlaybackParameters(base.speed * trim, base.pitch)
        voice.rateTrim = trim
        if (voice.player.playbackParameters != next) voice.player.playbackParameters = next
    }

    /** Park a voice exactly on its target so the gate or entry logic can start it on time. */
    private fun parkVoice(voice: Voice, masterMs: Long, nowMs: Long, alwaysSeek: Boolean, leadMs: Long = 0L) {
        val target = boundedSeek(voicePosition(voice.file, masterMs) + leadMs, knownDuration(voice.file))
        voice.player.playWhenReady = false
        if (alwaysSeek || kotlin.math.abs(voice.player.currentPosition - target) > STACK_START_ALIGNMENT_MS) {
            voice.player.seekTo(target)
        }
        applyRate(voice, 1f)
        voice.resetDrift(nowMs)
    }

    private fun setMainPlayWhenReady(value: Boolean) {
        if (main.playWhenReady == value) return
        internalMainChange = true
        try { main.playWhenReady = value } finally { internalMainChange = false }
    }

    /** Hold every player until all of them can produce audio at the same instant. */
    private fun armStartGate(requestedMaster: Long? = null) {
        if (!playing || !active) return
        val master = requestedMaster ?: position()
        anchorMs = master
        anchorTimeMs = SystemClock.elapsedRealtime()
        gateDeadlineMs = SystemClock.elapsedRealtime() + STACK_START_GATE_TIMEOUT_MS
        armingGate = true
        try {
            setMainPlayWhenReady(false)
            voices.toList().forEach { voice -> safePlayer { parkVoice(voice, master, anchorTimeMs, alwaysSeek = false) } }
        } finally { armingGate = false }
    }

    private fun tryOpenStartGate() {
        if (!gated || armingGate || released || !active) return
        if (!playing) { gateDeadlineMs = 0L; return }
        if (main.playerError != null) { pause(); return }
        if (main.playbackState == Player.STATE_ENDED) {
            gateDeadlineMs = 0L
            onPrimaryEnded()
            return
        }
        val master = position()
        val joining = voices.filter { voiceInWindow(it, master) }
        val expired = SystemClock.elapsedRealtime() >= gateDeadlineMs
        if (!stackStartGateCanOpen(main.playbackState == Player.STATE_READY,
                joining.all { it.player.playbackState == Player.STATE_READY }, expired)) return
        gateDeadlineMs = 0L
        val nowMs = SystemClock.elapsedRealtime()
        anchorMs = master
        anchorTimeMs = nowMs
        // Late voices (gate timeout) stay parked; the entry logic starts them on time.
        joining.filter { it.player.playbackState == Player.STATE_READY }.forEach { voice ->
            safePlayer {
                applyRate(voice, 1f)
                voice.resetDrift(nowMs)
                voice.player.playWhenReady = true
            }
        }
        val primaryDuration = sessionDuration()
        setMainPlayWhenReady(primaryDuration <= 0L || master < primaryDuration)
        publish()
    }

    private fun onVoiceReady(player: ExoPlayer) {
        if (released || !active) return
        voices.firstOrNull { it.player === player }?.bufferingSinceMs = 0L
        if (gated) { tryOpenStartGate(); return }
        if (!playing || !main.isPlaying) return
        val voice = voices.firstOrNull { it.player === player } ?: return
        safePlayer { syncVoice(voice, position(), SystemClock.elapsedRealtime()) }
    }

    /** Single place where a running voice is started, parked, trimmed or (rarely) seeked. */
    private fun syncVoice(voice: Voice, masterMs: Long, nowMs: Long) {
        val duration = knownDuration(voice.file)
        val rawTarget = voicePosition(voice.file, masterMs)
        if (duration > 0L && rawTarget >= duration) {
            if (voice.player.playWhenReady) voice.player.pause()
            return
        }
        val current = voice.player.currentPosition
        if (!voice.player.playWhenReady) {
            if (voice.player.playbackState != Player.STATE_READY) return
            val gap = current - rawTarget // > 0: parked ahead, waiting for the master
            val desired = boundedSeek(rawTarget + voice.seekLeadMs, duration)
            if (rawTarget >= 0L && kotlin.math.abs(gap) >= STACK_HARD_RESYNC_MS &&
                kotlin.math.abs(current - desired) >= STACK_HARD_RESYNC_MS
            ) {
                voice.player.seekTo(desired)
                return
            }
            if (gap <= STACK_ENTRY_LEAD_MS) {
                applyRate(voice, 1f)
                voice.resetDrift(nowMs)
                voice.player.playWhenReady = true
            }
            return
        }
        if (rawTarget < 0L) {
            voice.player.pause()
            voice.player.seekTo(0L)
            return
        }
        if (!voice.player.isPlaying) {
            // Buffering: settle time counts from when audio actually flows again.
            voice.settleUntilMs = nowMs + STACK_SETTLE_MS
            return
        }
        if (nowMs < voice.settleUntilMs) return

        val drift = (current - rawTarget).toDouble()
        voice.driftEma = if (voice.hasDrift) voice.driftEma * 0.6 + drift * 0.4 else drift
        voice.hasDrift = true
        if (voice.measuringSeekResidual) {
            voice.seekLeadMs = stackNextSeekLead(voice.seekLeadMs, drift)
            voice.measuringSeekResidual = false
        }
        when (stackSyncAction(voice.driftEma, nowMs - voice.lastSeekMs)) {
            StackSyncAction.SEEK -> {
                applyRate(voice, 1f)
                voice.controlledSeekUntilMs = nowMs + STACK_CONTROLLED_SEEK_GRACE_MS
                voice.player.seekTo(boundedSeek(rawTarget + voice.seekLeadMs, duration))
                voice.lastSeekMs = nowMs
                voice.measuringSeekResidual = true
                voice.resetDrift(nowMs)
            }
            StackSyncAction.RATE -> if (nowMs - voice.lastRateMs >= STACK_RATE_UPDATE_MS) {
                voice.lastRateMs = nowMs
                val trim = stackRateTrim(voice.driftEma)
                if (trim != voice.rateTrim) applyRate(voice, trim)
            }
            StackSyncAction.NONE -> {
                // Hysteresis: only drop the trim once comfortably inside the deadband.
                if (voice.rateTrim != 1f && kotlin.math.abs(voice.driftEma) <= STACK_SYNC_DEADBAND_MS / 2.0) {
                    applyRate(voice, 1f)
                }
            }
        }
    }

    fun setOffsets(offsets: Map<String, Long>) {
        if (!active) return
        val changed = mutableSetOf<String>()
        for (i in slots.indices) {
            val slot = slots[i]
            offsets[slot.file.path]?.let { value ->
                val next = if (slot.file.path == primaryPath) 0L else value.coerceIn(-30_000L, 30_000L)
                if (next != slot.offsetMs) {
                    slots[i] = slot.copy(offsetMs = next)
                    changed += slot.file.path
                }
            }
        }
        if (changed.isEmpty()) return
        val master = position()
        val nowMs = SystemClock.elapsedRealtime()
        // Parked voices are started by the entry logic exactly when the master arrives.
        val live = playing && !gated && main.isPlaying
        voices.filter { it.file.path in changed }.forEach { voice ->
            safePlayer { parkVoice(voice, master, nowMs, alwaysSeek = true, leadMs = if (live) voice.seekLeadMs else 0L) }
        }
        publish()
    }

    private fun knownDuration(file: MediaFile): Long {
        val live = if (file.path == primaryPath) main.duration
            else voices.firstOrNull { it.file.path == file.path }?.player?.duration ?: C.TIME_UNSET
        return if (live > 0L && live != C.TIME_UNSET) live else file.durationMs.coerceAtLeast(0L)
    }

    private fun sessionDuration(): Long =
        slots.firstOrNull { it.file.path == primaryPath }?.let { knownDuration(it.file) } ?: 0L

    private fun clampToSession(value: Long): Long {
        val duration = sessionDuration()
        return if (duration > 0L) value.coerceIn(0L, duration) else value.coerceAtLeast(0L)
    }

    fun start(files: List<MediaFile>): Boolean {
        if (released) return false
        val unique = files.distinctBy { it.path }
        if (unique.size !in 2..StackPlayback.MAX_TRACKS || unique.size != files.size || unique.any { !valid(it) }) return false
        stop(clearMain = false)
        val prepared = mutableListOf<Voice>()
        for (file in unique.drop(1)) {
            val voice = createVoice(file) ?: run { prepared.forEach(::releaseVoice); return false }
            prepared += voice
        }
        previousRepeat = main.repeatMode
        previousShuffle = main.shuffleModeEnabled
        slots += unique.map(::StackSlot)
        voices += prepared
        primaryPath = unique.first().path
        anchorMs = 0L
        anchorTimeMs = SystemClock.elapsedRealtime()
        clockSpeed = main.playbackParameters.speed
        loopEnabled = STACK_LOOP_DEFAULT
        internalMainChange = true
        try {
            main.repeatMode = Player.REPEAT_MODE_OFF
            main.shuffleModeEnabled = false
            main.playWhenReady = false
            main.setMediaItem(unique.first().toMediaItem(), 0L)
            main.prepare()
        } catch (_: Exception) {
            stop(clearMain = false)
            return false
        } finally { internalMainChange = false }
        playing = true
        val nowMs = SystemClock.elapsedRealtime()
        voices.toList().forEach { voice -> safePlayer { voice.player.playWhenReady = false; voice.resetDrift(nowMs) } }
        armStartGate()
        onLevelsChanged()
        publish()
        startTicker()
        return true
    }

    fun add(file: MediaFile): Boolean {
        if (released || !active || !canAddStackTrack(slots, file) || !valid(file)) return false
        val voice = createVoice(file) ?: return false
        val master = position()
        slots += StackSlot(file)
        voices += voice
        // Park slightly ahead; the entry logic starts it the moment the master catches up.
        voice.player.playWhenReady = false
        voice.player.seekTo(boundedSeek(master + voice.seekLeadMs, knownDuration(file)))
        applyRate(voice, 1f)
        voice.resetDrift(SystemClock.elapsedRealtime())
        onLevelsChanged()
        publish()
        return true
    }

    fun remove(path: String) {
        val index = slots.indexOfFirst { it.file.path == path }
        if (index < 0) return
        if (slots.size == 1) { stop(clearMain = true); return }
        if (slots.size == 2) {
            if (path == primaryPath) {
                val survivor = slots.first { it.file.path != path }
                if (!setPrimary(survivor.file.path)) { stop(clearMain = true); return }
            }
            stop(clearMain = false)
            return
        }
        if (path == primaryPath) {
            val now = position()
            val replacement = slots.firstOrNull {
                it.file.path != path && it.error == null && (knownDuration(it.file) <= 0L || now < knownDuration(it.file))
            }
            if (replacement == null || !setPrimary(replacement.file.path)) {
                stop(clearMain = true)
                return
            }
        }
        slots.removeAll { it.file.path == path }
        voices.firstOrNull { it.file.path == path }?.let { voices.remove(it); releaseVoice(it) }
        if (gated) tryOpenStartGate()
        onLevelsChanged()
        publish()
    }

    fun setPrimary(path: String): Boolean {
        if (path == primaryPath) return true
        val target = slots.firstOrNull { it.file.path == path } ?: return false
        val replacement = voices.firstOrNull { it.file.path == path } ?: return false
        val old = slots.firstOrNull { it.file.path == primaryPath }?.file ?: return false
        val now = position()
        val promotedPosition = stackVoiceTarget(now, target.offsetMs)
        if (promotedPosition < 0L) return false // The take has not started yet.
        val targetDuration = knownDuration(target.file)
        if (targetDuration > 0L && promotedPosition >= targetDuration) return false
        voices.toList().forEach { voice -> safePlayer { voice.player.pause() } }
        internalMainChange = true
        try {
            detachVideo(replacement)
            replacement.videoUnavailable = false
            replacement.videoFrames = 0
            replacement.player.setMediaItem(old.toMediaItem(), boundedSeek(now, knownDuration(old)))
            replacement.player.prepare()
            replacement.file = old
            main.playWhenReady = false
            main.setMediaItem(target.file.toMediaItem(), boundedSeek(promotedPosition, knownDuration(target.file)))
            main.prepare()
            primaryPath = path
            val rebased = rebaseStackOffsets(slots.map { it.offsetMs }, target.offsetMs)
            for (i in slots.indices) slots[i] = slots[i].copy(offsetMs = rebased[i],
                videoUnavailable = if (slots[i].file.path == old.path || slots[i].file.path == path) false else slots[i].videoUnavailable)
            anchorMs = promotedPosition
            anchorTimeMs = SystemClock.elapsedRealtime()
            replacement.player.playWhenReady = false
        } catch (_: Exception) {
            stop(clearMain = false)
            return false
        } finally { internalMainChange = false }
        val nowMs = SystemClock.elapsedRealtime()
        voices.toList().forEach { voice -> safePlayer { applyRate(voice, 1f); voice.resetDrift(nowMs) } }
        if (playing) armStartGate()
        onLevelsChanged()
        publish()
        return true
    }

    fun setVolume(path: String, level: Float) = edit(path) { it.copy(volume = if (level.isFinite()) level.coerceIn(0f, 1f) else 0f) }
    fun toggleMute(path: String) = edit(path) { it.copy(muted = !it.muted) }
    fun toggleSolo(path: String) = edit(path) { it.copy(solo = !it.solo) }
    private fun edit(path: String, change: (StackSlot) -> StackSlot) {
        val index = slots.indexOfFirst { it.file.path == path }
        if (index < 0) return
        slots[index] = change(slots[index])
        onLevelsChanged()
        publish()
    }

    fun position(): Long {
        // Media3 acknowledges seeks asynchronously; the barrier must retain the
        // requested timeline, not replace it with a stale pre-seek position.
        if (gated) return clampToSession(anchorMs)
        val fallback = if (playing && !gated) {
            anchorMs + ((SystemClock.elapsedRealtime() - anchorTimeMs) * clockSpeed).toLong()
        } else anchorMs
        val primaryAvailable = main.currentMediaItem?.mediaId == primaryPath &&
            main.playbackState != Player.STATE_IDLE
        return clampToSession(stackMasterPosition(main.currentPosition, fallback, primaryAvailable))
    }

    fun play() {
        if (!active) return
        if (anchorMs >= sessionDuration() && sessionDuration() > 0L) seek(0L)
        if (playing) return
        playing = true
        anchorTimeMs = SystemClock.elapsedRealtime()
        val master = position()
        val nowMs = SystemClock.elapsedRealtime()
        voices.toList().forEach { voice -> safePlayer { parkVoice(voice, master, nowMs, alwaysSeek = false) } }
        armStartGate()
        publish()
    }

    fun pause() {
        if (!active) return
        anchorMs = position()
        playing = false
        gateDeadlineMs = 0L
        voices.toList().forEach { voice -> safePlayer { voice.player.pause() } }
        internalMainChange = true
        try { main.pause() } finally { internalMainChange = false }
        publish()
    }

    fun seek(targetMs: Long) = seekAll(targetMs, primaryEvent = false)

    private fun seekAll(targetMs: Long, primaryEvent: Boolean) {
        if (!active) return
        val plan = stackSeekPlan(targetMs, sessionDuration(), primaryEvent)
        anchorMs = plan.positionMs
        anchorTimeMs = SystemClock.elapsedRealtime()
        val nowMs = SystemClock.elapsedRealtime()
        voices.toList().forEach { voice ->
            safePlayer { parkVoice(voice, anchorMs, nowMs, alwaysSeek = !primaryEvent) }
        }
        if (plan.seekPrimary) {
            val primaryDuration = sessionDuration()
            internalMainChange = true
            try { main.seekTo(boundedSeek(anchorMs, primaryDuration)) } finally { internalMainChange = false }
        }
        if (playing) armStartGate(plan.positionMs)
        publish()
    }

    fun onMainPlayChanged(ready: Boolean) {
        if (!active || internalMainChange) return
        if (gated) {
            // An external Play request must still respect the readiness barrier.
            if (ready) { setMainPlayWhenReady(false); tryOpenStartGate() }
            return
        }
        if (ready) play() else pause()
    }

    fun onMainSeek() {
        if (active && !internalMainChange) seekAll(main.currentPosition, primaryEvent = true)
    }

    fun onMainReady() {
        if (active && gated) tryOpenStartGate()
    }

    fun onMainIsPlayingChanged(isPlaying: Boolean) {
        if (!active || !playing || gated) return
        if (!isPlaying && main.playbackState == Player.STATE_ENDED && !internalMainChange) {
            onPrimaryEnded()
            return
        }
        val master = position()
        val nowMs = SystemClock.elapsedRealtime()
        anchorMs = master
        anchorTimeMs = nowMs
        if (!isPlaying) {
            voices.toList().forEach { voice -> safePlayer { voice.player.pause() } }
            // A primary stall would otherwise let companions race ahead; regroup instead.
            if (main.playWhenReady && main.playbackState == Player.STATE_BUFFERING) armStartGate()
            publish()
            return
        }
        voices.toList().forEach { voice -> safePlayer { syncVoice(voice, master, nowMs) } }
        publish()
    }

    fun onMainSpeedChanged() {
        if (!active || internalMainChange) return
        anchorMs = position()
        anchorTimeMs = SystemClock.elapsedRealtime()
        clockSpeed = main.playbackParameters.speed
        val nowMs = SystemClock.elapsedRealtime()
        voices.toList().forEach { voice -> safePlayer { applyRate(voice, 1f); voice.resetDrift(nowMs) } }
    }

    fun onMainMediaChanged(path: String?) {
        if (active && !internalMainChange && path != primaryPath) stop(clearMain = false)
    }

    fun onPrimaryError() {
        if (!active) return
        val index = slots.indexOfFirst { it.file.path == primaryPath }
        if (index >= 0) slots[index] = slots[index].copy(error = "Playback unavailable")
        val replacement = slots.firstOrNull { it.file.path != primaryPath && it.error == null && voices.any { voice -> voice.file.path == it.file.path } }
        if (replacement == null || !setPrimary(replacement.file.path)) pause()
        publish()
    }

    fun applyVolumes(mainBaseGain: Float) {
        sourceGain = mainBaseGain
        if (!active) return
        val solo = slots.any { it.solo && !it.muted && it.error == null }
        val audibleCount = stackAudibleTrackCount(slots)
        main.volume = (slots.firstOrNull { it.file.path == primaryPath }
            ?.let { stackAudibleVolume(it, solo, audibleCount) } ?: 0f) * sourceGain
        voices.toList().forEach { voice ->
            voice.player.volume = (slots.firstOrNull { it.file.path == voice.file.path }
                ?.let { stackAudibleVolume(it, solo, audibleCount) } ?: 0f) * sourceGain
        }
    }

    fun setLoop(enabled: Boolean) {
        if (!active || loopEnabled == enabled) return
        loopEnabled = enabled
        publish()
    }

    fun onPrimaryEnded() {
        // ENDED arrives via both isPlaying and playbackState callbacks; restart once.
        if (!active || !playing || internalMainChange || gated) return
        if (loopEnabled) seek(0L) else pause()
    }

    fun stop(clearMain: Boolean) {
        if (!active) return
        ticker?.cancel(); ticker = null
        val dying = voices.toList()
        voices.clear()
        slots.clear()
        playing = false
        loopEnabled = false
        primaryPath = null
        anchorMs = 0L
        gateDeadlineMs = 0L
        dying.forEach(::releaseVoice)
        safePlayer {
            if (clearMain) main.stop()
            main.repeatMode = previousRepeat
            main.shuffleModeEnabled = previousShuffle
        }
        StackPlayback.publish(StackSession())
        onLevelsChanged()
    }

    fun release() {
        stop(clearMain = false)
        released = true
    }

    private fun publish() {
        lastPublishMs = SystemClock.elapsedRealtime()
        for (index in slots.indices) {
            val resolved = knownDuration(slots[index].file)
            if (slots[index].resolvedDurationMs != resolved) {
                slots[index] = slots[index].copy(resolvedDurationMs = resolved)
            }
        }
        val running = (if (main.isPlaying) 1 else 0) + voices.count { it.player.isPlaying }
        val drift = voices.filter { it.player.isPlaying && it.hasDrift }.maxOfOrNull { kotlin.math.abs(it.driftEma).toLong() } ?: 0L
        StackPlayback.publish(StackSession(slots.toList(), primaryPath, position(), sessionDuration(), playing, loopEnabled,
            synchronizing = gated, runningTracks = running, maxDriftMs = drift))
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && active) {
                delay(STACK_SYNC_TICK_MS)
                if (gated) {
                    tryOpenStartGate()
                } else {
                    val now = position()
                    val duration = sessionDuration()
                    if (playing && duration > 0L && now >= duration) {
                        if (shouldRestartStack(loopEnabled, playing, now, duration)) {
                            seek(0L)
                        } else {
                            pause()
                            anchorMs = duration
                            publish()
                        }
                        continue
                    }
                    val currentPrimary = slots.firstOrNull { it.file.path == primaryPath }?.file
                    val primaryDuration = currentPrimary?.let(::knownDuration) ?: 0L
                    if (playing && primaryDuration > 0L && now >= primaryDuration) {
                        val nextVisual = slots.asSequence()
                            .filter { it.file.path != primaryPath && it.error == null }
                            .filter { knownDuration(it.file) <= 0L || knownDuration(it.file) > now }
                            .maxByOrNull { knownDuration(it.file) }
                        if (nextVisual == null || !setPrimary(nextVisual.file.path)) pause()
                    }
                    if (playing && !gated && main.isPlaying) {
                        val master = position()
                        val nowMs = SystemClock.elapsedRealtime()
                        val stalled = voices.any { voice ->
                            voice.player.playbackState == Player.STATE_BUFFERING && voiceInWindow(voice, master) &&
                                stackBufferRequiresGate(nowMs, voice.bufferingSinceMs, voice.controlledSeekUntilMs)
                        }
                        if (stalled) { armStartGate(master); publish(); continue }
                        voices.toList().forEach { voice -> safePlayer { syncVoice(voice, master, nowMs) } }
                    }
                }
                // Delayed starts need a fine timer; Compose does not need 20 updates/second.
                voices.toList().forEach { voice -> safePlayer { syncVideo(voice, SystemClock.elapsedRealtime()) } }
                if (SystemClock.elapsedRealtime() - lastPublishMs >= 250L) publish()
            }
        }
    }
}
