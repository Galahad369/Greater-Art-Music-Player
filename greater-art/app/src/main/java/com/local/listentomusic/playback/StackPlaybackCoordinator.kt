package com.local.listentomusic.playback

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
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

/** One MediaSession/visual player; up to seven bounded audio-only companions. */
internal class StackPlaybackCoordinator(
    private val context: Context,
    private val main: ExoPlayer,
    private val scope: CoroutineScope,
    private val onLevelsChanged: () -> Unit,
) {
    private data class Voice(var file: MediaFile, val player: ExoPlayer)
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
    private var ticker: Job? = null
    private var lastCorrectionMs = 0L
    private var internalMainChange = false
    val active: Boolean get() = slots.isNotEmpty()
    val primary: String? get() = primaryPath
    val changingMain: Boolean get() = internalMainChange

    fun voiceDiagnostics(): String = voices.mapIndexed { index, voice ->
        "${index + 1}:${voice.player.playbackState}/${voice.player.isPlaying}@${voice.player.currentPosition}ms"
    }.joinToString(",").ifEmpty { "none" }

    private fun valid(file: MediaFile): Boolean {
        val source = File(file.sourcePath)
        return source.isFile && source.canRead() && MediaScanner.isInsideTarget(source) &&
            source.extension.lowercase() in MediaScanner.supportedExtensions
    }

    private fun createVoice(file: MediaFile): Voice? {
        var engine: ExoPlayer? = null
        return runCatching {
        ExoPlayer.Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1_000, 5_000, 100, 200)
                .setTargetBufferBytes(2 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).build())
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().also { engine = it }.also { engine ->
                engine.trackSelectionParameters = engine.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
                engine.repeatMode = Player.REPEAT_MODE_OFF
                engine.setMediaItem(file.toMediaItem())
                engine.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        val failed = voices.firstOrNull { it.player === engine } ?: return
                        voices.remove(failed)
                        failed.player.release()
                        val index = slots.indexOfFirst { it.file.path == failed.file.path }
                        if (index >= 0) slots[index] = slots[index].copy(error = "Playback unavailable")
                        publish()
                        onLevelsChanged()
                    }
                })
                engine.prepare()
            }.let { Voice(file, it) }
        }.getOrElse { engine?.release(); null }
    }

    private fun boundedSeek(positionMs: Long, durationMs: Long): Long =
        if (durationMs > 0L) positionMs.coerceAtMost(durationMs) else positionMs

    private fun knownDuration(file: MediaFile): Long {
        val live = if (file.path == primaryPath) main.duration
            else voices.firstOrNull { it.file.path == file.path }?.player?.duration ?: C.TIME_UNSET
        return if (live > 0L && live != C.TIME_UNSET) live else file.durationMs.coerceAtLeast(0L)
    }

    private fun sessionDuration(): Long = slots.maxOfOrNull { knownDuration(it.file) } ?: 0L

    private fun clampToSession(value: Long): Long {
        val duration = sessionDuration()
        return if (duration > 0L) value.coerceIn(0L, duration) else value.coerceAtLeast(0L)
    }

    fun start(files: List<MediaFile>): Boolean {
        val unique = files.distinctBy { it.path }
        if (unique.size !in 2..StackPlayback.MAX_TRACKS || unique.size != files.size || unique.any { !valid(it) }) return false
        // Do not temporarily double the decoder count when replacing an active Stack.
        stop(clearMain = false)
        val prepared = mutableListOf<Voice>()
        for (file in unique.drop(1)) {
            val voice = createVoice(file) ?: run { prepared.forEach { it.player.release() }; return false }
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
        internalMainChange = true
        try {
            main.repeatMode = Player.REPEAT_MODE_OFF
            main.shuffleModeEnabled = false
            main.setMediaItem(unique.first().toMediaItem(), 0L)
            main.prepare()
        } catch (_: Exception) {
            stop(clearMain = false)
            return false
        } finally { internalMainChange = false }
        playing = true
        voices.forEach { it.player.playWhenReady = false }
        main.playWhenReady = true
        onLevelsChanged()
        publish()
        startTicker()
        return true
    }

    fun add(file: MediaFile): Boolean {
        if (!active || !canAddStackTrack(slots, file) || !valid(file)) return false
        val voice = createVoice(file) ?: return false
        val now = position()
        slots += StackSlot(file)
        voices += voice
        voice.player.seekTo(boundedSeek(now, knownDuration(file)))
        voice.player.playWhenReady = playing && main.isPlaying &&
            (knownDuration(file) <= 0L || now < knownDuration(file))
        voice.player.playbackParameters = main.playbackParameters
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
            // With one file left, return to normal playback instead of retaining a
            // one-voice Stack. The same main player keeps its position and video.
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
        voices.firstOrNull { it.file.path == path }?.let { voices.remove(it); it.player.release() }
        onLevelsChanged()
        publish()
    }

    fun setPrimary(path: String): Boolean {
        if (path == primaryPath) return true
        val target = slots.firstOrNull { it.file.path == path } ?: return false
        val replacement = voices.firstOrNull { it.file.path == path } ?: return false
        val old = slots.firstOrNull { it.file.path == primaryPath }?.file ?: return false
        val now = position()
        anchorMs = now
        anchorTimeMs = SystemClock.elapsedRealtime()
        val targetDuration = knownDuration(target.file)
        if (targetDuration > 0L && now >= targetDuration) return false
        internalMainChange = true
        try {
            replacement.player.pause()
            replacement.player.setMediaItem(old.toMediaItem(), boundedSeek(now, knownDuration(old)))
            replacement.player.prepare()
            replacement.file = old
            main.setMediaItem(target.file.toMediaItem(), boundedSeek(now, knownDuration(target.file)))
            main.prepare()
            primaryPath = path
            replacement.player.playWhenReady = false
            main.playWhenReady = playing && (knownDuration(target.file) <= 0L || now < knownDuration(target.file))
        } catch (_: Exception) {
            stop(clearMain = false)
            return false
        } finally { internalMainChange = false }
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
        val fallback = if (playing) {
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
        val now = position()
        voices.forEach { it.player.pause() }
        internalMainChange = true
        try { main.playWhenReady = slots.firstOrNull { it.file.path == primaryPath }?.file?.let { knownDuration(it) <= 0L || now < knownDuration(it) } ?: false }
        finally { internalMainChange = false }
        publish()
    }

    fun pause() {
        if (!active) return
        anchorMs = position()
        playing = false
        voices.forEach { it.player.pause() }
        internalMainChange = true
        try { main.pause() } finally { internalMainChange = false }
        publish()
    }

    fun seek(targetMs: Long) {
        if (!active) return
        anchorMs = clampToSession(targetMs)
        anchorTimeMs = SystemClock.elapsedRealtime()
        voices.forEach { voice ->
            voice.player.seekTo(boundedSeek(anchorMs, knownDuration(voice.file)))
            voice.player.playWhenReady = playing && main.isPlaying &&
                (knownDuration(voice.file) <= 0L || anchorMs < knownDuration(voice.file))
        }
        val primaryDuration = slots.firstOrNull { it.file.path == primaryPath }?.file?.let(::knownDuration) ?: 0L
        internalMainChange = true
        try {
            main.seekTo(boundedSeek(anchorMs, primaryDuration))
            main.playWhenReady = playing && (primaryDuration <= 0L || anchorMs < primaryDuration)
        } finally { internalMainChange = false }
        publish()
    }

    fun onMainPlayChanged(ready: Boolean) {
        if (!active || internalMainChange) return
        if (ready) play() else pause()
    }

    fun onMainSeek() {
        if (active && !internalMainChange) seek(main.currentPosition)
    }

    fun onMainIsPlayingChanged(isPlaying: Boolean) {
        if (!active || !playing || internalMainChange) return
        val now = position()
        if (!isPlaying) {
            voices.forEach { it.player.pause() }
            anchorMs = now
            anchorTimeMs = SystemClock.elapsedRealtime()
            publish()
            return
        }
        voices.forEach { voice ->
            val duration = knownDuration(voice.file)
            if (duration > 0L && now >= duration) {
                voice.player.pause()
            } else {
                val target = boundedSeek(now, duration)
                if (kotlin.math.abs(voice.player.currentPosition - target) > STACK_START_ALIGNMENT_MS) {
                    voice.player.seekTo(target)
                }
                voice.player.playbackParameters = main.playbackParameters
                voice.player.playWhenReady = true
            }
        }
        anchorMs = now
        anchorTimeMs = SystemClock.elapsedRealtime()
        lastCorrectionMs = SystemClock.elapsedRealtime()
        publish()
    }

    fun onMainSpeedChanged() {
        if (!active || internalMainChange) return
        anchorMs = position()
        anchorTimeMs = SystemClock.elapsedRealtime()
        clockSpeed = main.playbackParameters.speed
        voices.forEach { it.player.playbackParameters = main.playbackParameters }
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
        val solo = slots.any { it.solo }
        val count = slots.size
        main.volume = (slots.firstOrNull { it.file.path == primaryPath }?.let { stackAudibleVolume(it, solo, count) } ?: 0f) * sourceGain
        voices.forEach { voice ->
            voice.player.volume = slots.firstOrNull { it.file.path == voice.file.path }
                ?.let { stackAudibleVolume(it, solo, count) } ?: 0f
        }
    }

    fun stop(clearMain: Boolean) {
        if (!active) return
        ticker?.cancel(); ticker = null
        voices.forEach { it.player.release() }
        voices.clear()
        slots.clear()
        playing = false
        primaryPath = null
        anchorMs = 0L
        if (clearMain) main.stop()
        main.repeatMode = previousRepeat
        main.shuffleModeEnabled = previousShuffle
        StackPlayback.publish(StackSession())
        onLevelsChanged()
    }

    fun release() { stop(clearMain = false) }

    private fun publish() {
        StackPlayback.publish(StackSession(slots.map { it.copy(resolvedDurationMs = knownDuration(it.file)) }, primaryPath, position(), sessionDuration(), playing))
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && active) {
                delay(500L)
                val now = position()
                if (playing && sessionDuration() > 0L && now >= sessionDuration()) {
                    pause()
                    anchorMs = sessionDuration()
                    publish()
                    continue
                }
                val currentPrimary = slots.firstOrNull { it.file.path == primaryPath }?.file
                val primaryDuration = currentPrimary?.let(::knownDuration) ?: 0L
                if (playing && primaryDuration > 0L && now >= primaryDuration) {
                    // Keep the sole MediaSession/video surface on a track that still exists.
                    val nextVisual = slots.asSequence()
                        .filter { it.file.path != primaryPath && it.error == null }
                        .filter { knownDuration(it.file) <= 0L || knownDuration(it.file) > now }
                        .maxByOrNull { knownDuration(it.file) }
                    if (nextVisual == null || !setPrimary(nextVisual.file.path)) pause()
                }
                if (playing && main.isPlaying &&
                    SystemClock.elapsedRealtime() - lastCorrectionMs >= STACK_CORRECTION_INTERVAL_MS) {
                    lastCorrectionMs = SystemClock.elapsedRealtime()
                    voices.forEach { voice ->
                        val duration = knownDuration(voice.file)
                        if (duration > 0L && now >= duration) voice.player.pause()
                        else if (voice.player.playbackState == Player.STATE_READY &&
                            shouldCorrectStackVoice(voice.player.currentPosition, now)) {
                            voice.player.seekTo(boundedSeek(now, duration))
                        }
                    }
                }
                publish()
            }
        }
    }
}
