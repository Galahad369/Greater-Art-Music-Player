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
import java.util.concurrent.Executors
import java.nio.ByteBuffer

/** Host must silence ALL legacy voices before granting this exclusive lease.
 * Video may remain native quality and follow onClock; ownership is never replaced here. */
internal interface PcmPrototypeHost {
    suspend fun acquireExclusiveAudio(): Boolean
    suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?)
    fun onClock(positionMs: Long, playing: Boolean)
}

internal data class PcmPrototypeStats(val active: Boolean = false, val playing: Boolean = false,
    val outputRate: Int = 0, val frame: Long = 0, val submittedFrame: Long = 0,
    val underruns: Int = 0, val decoderWaits: Int = 0, val maxMixUs: Long = 0, val error: String? = null,
    val maxPrepareUs: Long = 0, val maxRenderUs: Long = 0, val bufferFrames: Int = 0,
    val speed: Float = 1f, val pitch: Float = 1f, val durationFrames: Long = 0)

/** Default-off, debug-build prototype. No routing changes to production PlaybackService.
 * Explicit host lease + ONE output; unsupported cases restore legacy only after release. */
internal class SharedClockPcmPrototype(context: Context, private val host: PcmPrototypeHost) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val renderDispatcher = Executors.newSingleThreadExecutor { work ->
        Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); work.run() },
            "ga-stack-output").apply { isDaemon = true }
    }.asCoroutineDispatcher()
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
    private var speed = 1f
    private var pitch = 1f
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
        val formats = try { withContext(Dispatchers.IO) { tracks.map { pcmSpec(it.file, context) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return@withLock false }
        val outputRate = formats.maxOf { it.rate }
        val preparedVoices = try { voiceLevels(tracks, formats) }
            catch (_: IllegalArgumentException) { return@withLock false }
        val sessionEnd = pcmSessionEndFrames(tracks, formats, outputRate)
        // A wholly trimmed-away Stack must not acquire audio or spin a zero-length loop.
        if (sessionEnd <= 0) return@withLock false
        if (AudioTrack.getMinBufferSize(outputRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT) <= 0)
            return@withLock false
        stopLocked(false, null)
        if (!host.acquireExclusiveAudio()) return@withLock false
        lease = true
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopLocked(true, "Audio focus denied"); return@withLock false
        }
        slots = tracks.toList(); specs = formats; rate = outputRate
        duration = sessionEnd
        frame = 0; voices = preparedVoices; loop = true
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
    suspend fun parameters(speed: Float, pitch: Float) = mutex.withLock {
        require(speed.isFinite() && speed > 0 && pitch.isFinite() && pitch > 0)
        if (this.speed == speed && this.pitch == pitch) return@withLock
        val playing = lease && job?.isActive == true
        if (lease) {
            frame = clockFrame()
            generation++
            output?.pause()
            job?.cancelAndJoin(); job = null
        }
        this.speed = speed; this.pitch = pitch
        state.value = state.value.copy(frame = frame, submittedFrame = frame, speed = speed, pitch = pitch)
        if (playing) launchRun()
    }
    suspend fun levels(tracks: List<StackSlot>) = mutex.withLock {
        require(tracks.map { it.file.path } == slots.map { it.file.path })
        require(tracks.indices.all { index ->
            tracks[index].alignmentScale == slots[index].alignmentScale &&
                tracks[index].offsetMs == slots[index].offsetMs &&
                tracks[index].alignmentOffsetUs == slots[index].alignmentOffsetUs && tracks[index].error == null
        }) { "Levels cannot silently replace timing maps" }
        val prepared = voiceLevels(tracks, specs)
        slots = tracks.toList()
        voices = prepared
    }
    private fun voiceLevels(tracks: List<StackSlot>, formats: List<PcmSpec>): List<PcmVoice> =
        tracks.mapIndexed { i, slot -> PcmVoice(formats[i].rate,
            slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0, slot.volume, slot.muted, slot.solo) }
    private fun clockFrame(): Long = output?.let {
        runClock.position(it.playbackHeadPosition).coerceAtMost(duration)
    } ?: frame

    private fun launchRun() {
        val run = ++generation
        runClock = PcmFrameClock(frame)
        job = scope.launch(renderDispatcher) {
            var failure: String? = null
            var completed = false
            var sink: AudioTrack? = null
            var tempo: FloatPcmTempo? = null
            var streams = emptyList<StackPcmStream>()
            try {
                coroutineScope {
                    val processor = FloatPcmTempo(rate, speed, pitch).also { tempo = it }
                    runClock = PcmFrameClock(frame, processor.clockSpeed)
                    streams = slots.mapIndexed { i, slot -> StackPcmStream(this, slot.file, specs[i],
                        floor(pcmSourceFrame(frame, rate, voices[i])).toLong().coerceAtLeast(0), context) }
                    val lanes = streams.map { stream -> PcmLane(stream) {
                        state.value = state.value.copy(decoderWaits = state.value.decoderWaits + 1)
                        sink?.pause()
                    } }
                    val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
                    val track = AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(
                        AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
                        // 100ms scheduling headroom, not a source-rate/quality reduction.
                        .setBufferSizeInBytes(maxOf(minimum, rate * 2 * 4 / 10))
                        .setTransferMode(AudioTrack.MODE_STREAM).build()
                    require(track.state == AudioTrack.STATE_INITIALIZED)
                    sink = track; output = track
                    var next = frame
                    var lastReport = 0L
                    var peakMixUs = 0L
                    var peakPrepareUs = 0L
                    var peakRenderUs = 0L
                    var started = false
                    var submittedOutputFrames = 0L
                    val outputClock = PcmFrameClock(0)
                    val prefillFrames = minOf(track.bufferSizeInFrames, rate / 12).coerceAtLeast(1)
                    val mixed = FloatArray(PCM_BLOCK_FRAMES * 2)
                    val gains = FloatArray(slots.size)
                    val windows = Array(slots.size) { FloatArray((PCM_BLOCK_FRAMES + PCM_SINC_RADIUS * 2 + 2) * 2) }
                    val windowStarts = LongArray(slots.size)
                    fun startOutput(force: Boolean = false) {
                        if (!started && (submittedOutputFrames >= prefillFrames || force && submittedOutputFrames > 0)) {
                            track.play(); started = true
                        } else if (started && track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                    }
                    // Sonic can emit more than one hardware buffer at slow speeds.
                    // Start after bounded prefill INSIDE each write, not after writing
                    // a whole processor buffer into a paused, possibly full AudioTrack.
                    suspend fun submitFloats(samples: Int) {
                        var written = 0
                        while (written < samples) {
                            ensureActive()
                            startOutput()
                            val limit = if (started) samples - written else
                                minOf((prefillFrames - submittedOutputFrames).toInt() * 2, samples - written)
                            val count = track.write(mixed, written, limit, AudioTrack.WRITE_BLOCKING)
                            check(count >= 0 && count % 2 == 0) { "AudioTrack float write failed: $count" }
                            if (count == 0) { delay(1); continue }
                            written += count; submittedOutputFrames += count / 2
                        }
                        startOutput()
                    }
                    suspend fun submitBytes(buffer: ByteBuffer) {
                        while (buffer.hasRemaining()) {
                            ensureActive()
                            startOutput()
                            val limit = if (started) buffer.remaining() else
                                minOf((prefillFrames - submittedOutputFrames).toInt() * 8, buffer.remaining())
                            val count = track.write(buffer, limit, AudioTrack.WRITE_BLOCKING)
                            check(count >= 0 && count % 8 == 0) { "AudioTrack float-buffer write failed: $count" }
                            if (count == 0) { delay(1); continue }
                            submittedOutputFrames += count / 8
                        }
                        startOutput()
                    }
                    state.value = PcmPrototypeStats(true, true, rate, frame, frame, bufferFrames = track.bufferSizeInFrames,
                        speed = speed, pitch = pitch, durationFrames = duration)
                    try { while (isActive) {
                        if (next >= duration) {
                            if (processor.active) {
                                processor.end()
                                while (!processor.ended) submitBytes(processor.drain())
                            }
                            startOutput(force = true)
                            // Hardware frames, not a rounded tempo-scaled media value:
                            // otherwise a two-sample Sonic tail difference can hang EOF.
                            while (isActive && outputClock.position(track.playbackHeadPosition) < submittedOutputFrames) delay(5)
                            completed = true
                            break
                        }
                        val blockFrames = minOf(PCM_BLOCK_FRAMES.toLong(), duration - next).toInt()
                        val levels = voices
                        val prepareStart = System.nanoTime()
                        levels.forEachIndexed { i, voice ->
                            val first = floor(pcmSourceFrame(next, rate, voice)).toLong() - PCM_SINC_RADIUS
                            val last = ceil(pcmSourceFrame(next + blockFrames - 1, rate, voice)).toLong() + PCM_SINC_RADIUS
                            lanes[i].prepare(first, last)
                            val window = windows[i]
                            check((last - first + 1) * 2 <= window.size) { "PCM window exceeds native-rate bound" }
                            windowStarts[i] = first
                            // Gather once, rather than doing deque/cache lookup for
                            // both channels of all 32 taps of every output frame.
                            var index = 0
                            for (sourceFrame in first..last) {
                                window[index++] = lanes[i].sample(sourceFrame, 0)
                                window[index++] = lanes[i].sample(sourceFrame, 1)
                            }
                        }
                        ensureActive()
                        val mixStart = System.nanoTime()
                        peakPrepareUs = maxOf(peakPrepareUs, (mixStart - prepareStart) / 1000)
                        mixPcmBlockInto(next, rate, levels, blockFrames, mixed, gains) { i, sample, channel ->
                            windows[i][((sample - windowStarts[i]) * 2 + channel).toInt()]
                        }
                        val mixUs = (System.nanoTime() - mixStart) / 1000
                        peakMixUs = maxOf(peakMixUs, mixUs)
                        val processed = if (processor.active) processor.queue(mixed, blockFrames) else null
                        peakRenderUs = maxOf(peakRenderUs, (System.nanoTime() - prepareStart) / 1000)
                        if (processed != null) submitBytes(processed) else submitFloats(blockFrames * 2)
                        next += blockFrames
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastReport >= 100) {
                            lastReport = now
                            state.value = state.value.copy(frame = clockFrame(), submittedFrame = next,
                                underruns = track.underrunCount, maxMixUs = peakMixUs,
                                maxPrepareUs = peakPrepareUs, maxRenderUs = peakRenderUs)
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
                frame = if (completed) duration else clockFrame().coerceAtMost(duration)
                output = null
                tempo?.close()
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
    suspend fun release() { stop(); scope.cancel(); renderDispatcher.close() }
}
