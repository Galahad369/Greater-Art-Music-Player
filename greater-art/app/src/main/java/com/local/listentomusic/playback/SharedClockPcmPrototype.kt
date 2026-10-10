package com.local.listentomusic.playback

import android.content.*
import android.media.*
import android.os.Build
import androidx.core.content.ContextCompat
import com.local.listentomusic.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.*

/** Host must silence ALL legacy voices before granting this exclusive lease.
 * Video may remain native quality and follow onClock; ownership is never replaced here. */
internal interface PcmPrototypeHost {
    suspend fun acquireExclusiveAudio(): Boolean
    suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?)
    fun onClock(positionMs: Long, playing: Boolean)
}

internal data class PcmPrototypeStats(val active: Boolean = false, val playing: Boolean = false,
    val outputRate: Int = 0, val frame: Long = 0, val submittedFrame: Long = 0,
    val underruns: Int = 0, val decoderWaits: Int = 0, val maxMixUs: Long = 0, val error: String? = null)

/** Default-off, debug-build prototype. No routing changes to production PlaybackService.
 * Explicit host lease + ONE output; unsupported cases restore legacy only after release. */
internal class SharedClockPcmPrototype(context: Context, private val host: PcmPrototypeHost) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val state = MutableStateFlow(PcmPrototypeStats())
    val stats = state.asStateFlow()
    private var job: Job? = null
    @Volatile private var output: AudioTrack? = null
    @Volatile private var voices: List<PcmVoice> = emptyList()
    private var slots: List<StackSlot> = emptyList()
    private var specs: List<PcmSpec> = emptyList()
    private var rate = 0
    private var frame = 0L
    @Volatile private var runClock = PcmFrameClock(0)
    private var duration = 0L
    private var lease = false
    private var generation = 0L
    private var receiverRegistered = false
    @Volatile private var loop = true
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change -> if (change < 0) onOutputInterrupted() }.build()
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onOutputInterrupted()
        }
    }
    /** Also the instrumentation seam: apps cannot send Android's protected noisy broadcast. */
    internal fun onOutputInterrupted() { scope.launch { pause() } }

    suspend fun start(tracks: List<StackSlot>, primaryPath: String): Boolean = mutex.withLock {
        if (!BuildConfig.DEBUG || !BuildConfig.STACK_PCM_PROTOTYPE) return@withLock false
        // Linear tempo correction needs a qualified pitch-preserving stretcher. Never
        // substitute pitch-shifting resampling or downgrade the real player as a fallback.
        if (tracks.size !in 1..8 || tracks.map { it.file.path }.distinct().size != tracks.size ||
            tracks.any { it.alignmentScale != 1.0 || it.error != null } || tracks.none { it.file.path == primaryPath })
            return@withLock false
        val formats = try { withContext(Dispatchers.IO) { tracks.map { pcmSpec(it.file) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return@withLock false }
        val outputRate = formats.maxOf { it.rate }
        if (AudioTrack.getMinBufferSize(outputRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT) <= 0)
            return@withLock false
        stopLocked(false, null)
        if (!host.acquireExclusiveAudio()) return@withLock false
        lease = true
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopLocked(true, "Audio focus denied"); return@withLock false
        }
        slots = tracks; specs = formats; rate = outputRate
        val primary = tracks.indexOfFirst { it.file.path == primaryPath }
        duration = formats[primary].durationFrames * rate / formats[primary].rate
        frame = 0; updateLevelsLocked(tracks); loop = true
        try {
            ContextCompat.registerReceiver(context, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
            launchRun()
        } catch (failure: Exception) {
            stopLocked(true, failure.javaClass.simpleName); return@withLock false
        }
        true
    }

    suspend fun pause() = mutex.withLock {
        if (!lease) return@withLock
        frame = clockFrame()
        generation++
        output?.let { runCatching { it.pause() } }
        job?.cancelAndJoin(); job = null
        state.value = state.value.copy(playing = false, frame = frame)
        host.onClock(frame * 1000 / rate, false)
    }

    suspend fun play() = mutex.withLock {
        if (lease && job?.isActive != true) {
            if (audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) launchRun()
            else stopLocked(true, "Audio focus denied")
        }
    }

    suspend fun seek(positionMs: Long) = mutex.withLock {
        if (!lease) return@withLock
        val playing = job?.isActive == true
        generation++
        output?.let { runCatching { it.pause() } }
        job?.cancelAndJoin(); job = null
        frame = (positionMs.coerceAtLeast(0) * rate / 1000).coerceAtMost(duration)
        state.value = state.value.copy(frame = frame, submittedFrame = frame)
        host.onClock(frame * 1000 / rate, playing)
        if (playing) launchRun()
    }

    fun setLoop(enabled: Boolean) { loop = enabled }
    suspend fun levels(tracks: List<StackSlot>) = mutex.withLock {
        require(tracks.map { it.file.path } == slots.map { it.file.path })
        updateLevelsLocked(tracks)
    }
    private fun updateLevelsLocked(tracks: List<StackSlot>) {
        voices = tracks.mapIndexed { i, slot -> PcmVoice(specs[i].rate,
            slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0, slot.volume, slot.muted, slot.solo) }
    }
    private fun clockFrame(): Long = output?.let {
        runClock.position(it.playbackHeadPosition)
    } ?: frame

    private fun launchRun() {
        val run = ++generation
        runClock = PcmFrameClock(frame)
        job = scope.launch {
            var failure: String? = null
            var completed = false
            var sink: AudioTrack? = null
            var streams = emptyList<StackPcmStream>()
            try {
                coroutineScope {
                    streams = slots.mapIndexed { i, slot -> StackPcmStream(this, slot.file, specs[i],
                        floor(pcmSourceFrame(frame, rate, voices[i])).toLong().coerceAtLeast(0)) }
                    val lanes = streams.map { stream -> PcmLane(stream) {
                        state.value = state.value.copy(decoderWaits = state.value.decoderWaits + 1)
                        sink?.pause()
                    } }
                    val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
                    val track = AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(
                        AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
                        .setBufferSizeInBytes(maxOf(minimum, PCM_BLOCK_FRAMES * 2 * 4 * 4))
                        .setTransferMode(AudioTrack.MODE_STREAM).build()
                    require(track.state == AudioTrack.STATE_INITIALIZED)
                    sink = track; output = track
                    var next = frame
                    var lastReport = 0L
                    var peakMixUs = 0L
                    var started = false
                    val prefillFrames = minOf(track.bufferSizeInFrames / 2, 4096).coerceAtLeast(PCM_BLOCK_FRAMES)
                    state.value = PcmPrototypeStats(true, true, rate, frame, frame)
                    try { while (isActive) {
                        if (next >= duration) {
                            // Drain the final output instead of cutting the primary early.
                            while (isActive && clockFrame() < next) delay(5)
                            completed = true
                            break
                        }
                        val blockFrames = minOf(PCM_BLOCK_FRAMES.toLong(), duration - next).toInt()
                        val levels = voices
                        levels.forEachIndexed { i, voice ->
                            val first = floor(pcmSourceFrame(next, rate, voice)).toLong() - PCM_SINC_RADIUS
                            val last = ceil(pcmSourceFrame(next + blockFrames - 1, rate, voice)).toLong() + PCM_SINC_RADIUS
                            lanes[i].prepare(first, last)
                        }
                        ensureActive()
                        val mixStart = System.nanoTime()
                        val mixed = mixPcmBlock(next, rate, levels, blockFrames) { i, sample, channel -> lanes[i].sample(sample, channel) }
                        val mixUs = (System.nanoTime() - mixStart) / 1000
                        peakMixUs = maxOf(peakMixUs, mixUs)
                        if (started && track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                        var written = 0
                        while (written < mixed.size) {
                            ensureActive()
                            val count = track.write(mixed, written, mixed.size - written, AudioTrack.WRITE_BLOCKING)
                            check(count >= 0) { "AudioTrack write failed: $count" }
                            if (count == 0) { delay(1); continue }
                            written += count
                        }
                        next += blockFrames
                        // Fill a bounded part of the native output buffer before the
                        // first audible frame; cold decoder/JIT work cannot starve it.
                        if (!started && (next - frame >= prefillFrames || next >= duration)) {
                            track.play(); started = true
                        }
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastReport >= 100) {
                            lastReport = now
                            state.value = state.value.copy(frame = clockFrame(), submittedFrame = next,
                                underruns = track.underrunCount, maxMixUs = peakMixUs)
                            host.onClock(state.value.frame * 1000 / rate, true)
                        }
                    } } finally { withContext(NonCancellable) { streams.forEach { it.close() } } }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.e("StackPcmPrototype", "Prototype output retired", error)
                failure = error.javaClass.simpleName
            }
            finally {
                // Cancelled decoders finish before legacy playback can become audible again.
                withContext(NonCancellable) { streams.forEach { it.close() } }
                frame = clockFrame().coerceAtMost(duration)
                output = null
                sink?.let { runCatching { it.pause() }; runCatching { it.flush() }; it.release() }
                state.value = state.value.copy(playing = false, frame = frame, error = failure)
            }
            if (failure != null) scope.launch { mutex.withLock { if (generation == run) stopLocked(true, failure) } }
            else if (completed) scope.launch { mutex.withLock {
                if (generation == run) {
                    if (lease && loop) { frame = 0; launchRun() } else stopLocked(false, null)
                }
            } }
        }
    }

    suspend fun stop() = mutex.withLock { stopLocked(false, null) }
    private suspend fun stopLocked(resume: Boolean, reason: String?) {
        generation++
        output?.let { runCatching { it.pause() } }
        job?.cancelAndJoin(); job = null
        if (receiverRegistered) { context.unregisterReceiver(noisy); receiverRegistered = false }
        audio.abandonAudioFocusRequest(focus)
        if (lease) {
            lease = false
            host.restoreLegacy(if (rate > 0) frame * 1000 / rate else 0, resume, reason)
        }
        state.value = state.value.copy(active = false, playing = false, error = reason)
    }
    suspend fun release() { stop(); scope.cancel() }
}
