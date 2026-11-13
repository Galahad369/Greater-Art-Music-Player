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
import java.util.concurrent.atomic.AtomicInteger
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
    val speed: Float = 1f, val pitch: Float = 1f, val durationFrames: Long = 0,
    val outputWaits: Int = 0)

/** Opt-in diagnostic samples. The default path neither allocates nor reads CPU counters. */
internal data class PcmRenderTrace(val frame: Long, val prepareUs: Long, val mixUs: Long,
    val tempoUs: Long, val writeUs: Long, val prepareCpuUs: Long, val mixCpuUs: Long,
    val tempoCpuUs: Long, val writeCpuUs: Long, val queuedBeforeFrames: Long,
    val queuedBeforeWriteFrames: Long, val underrunsBefore: Int, val underrunsAfter: Int,
    val outputStarted: Boolean)

/** Default-off, debug-build prototype. No routing changes to production PlaybackService.
 * Explicit host lease + ONE output; unsupported cases restore legacy only after release. */
internal class SharedClockPcmPrototype(context: Context, private val host: PcmPrototypeHost,
    private val renderObserver: ((PcmRenderTrace) -> Unit)? = null) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val renderDispatcher = Executors.newSingleThreadExecutor { work ->
        Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); work.run() },
            "ga-stack-output").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val mixDispatcher = Executors.newSingleThreadExecutor { work ->
        Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); work.run() },
            "ga-stack-mix").apply { isDaemon = true }
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
                    // The measured pipeline has NOT passed its cadence/underrun
                    // gates. Preserve the existing private prototype by default.
                    val parallel = BuildConfig.STACK_PCM_PARALLEL && shouldPipelinePcm(rate, processor.clockSpeed)
                    val decoderWaits = AtomicInteger()
                    runClock = PcmFrameClock(frame, processor.clockSpeed)
                    streams = slots.mapIndexed { i, slot -> StackPcmStream(this, slot.file, specs[i],
                        floor(pcmSourceFrame(frame, rate, voices[i])).toLong().coerceAtLeast(0), context) }
                    val lanes = streams.map { stream -> PcmLane(stream) {
                        decoderWaits.incrementAndGet()
                        // Only the output consumer may pause a pipelined sink.
                        // A producer pause racing WRITE_BLOCKING can strand it.
                        if (!parallel) sink?.pause()
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
                    var mixed = FloatArray(PCM_BLOCK_FRAMES * 2)
                    val gains = FloatArray(slots.size)
                    val windows = Array(slots.size) { FloatArray((PCM_BLOCK_FRAMES + PCM_SINC_RADIUS * 2 + 2) * 2) }
                    val windowStarts = LongArray(slots.size)
                    var outputWaits = 0
                    suspend fun prepareBlock(at: Long, count: Int, samples: FloatArray): PreparedPcmBlock {
                        val observing = renderObserver != null
                        val prepareCpu = if (observing) android.os.Debug.threadCpuTimeNanos() else 0
                        val prepareStart = System.nanoTime()
                        val levels = voices // One immutable gain/mute/solo snapshot per block.
                        levels.forEachIndexed { i, voice ->
                            val first = floor(pcmSourceFrame(at, rate, voice)).toLong() - PCM_SINC_RADIUS
                            val last = ceil(pcmSourceFrame(at + count - 1, rate, voice)).toLong() + PCM_SINC_RADIUS
                            lanes[i].prepare(first, last)
                            val window = windows[i]
                            check((last - first + 1) * 2 <= window.size) { "PCM window exceeds native-rate bound" }
                            windowStarts[i] = first
                            lanes[i].copyStereoWindow(first, (last - first + 1).toInt(), window)
                        }
                        currentCoroutineContext().ensureActive()
                        val mixStart = System.nanoTime()
                        val mixCpu = if (observing) android.os.Debug.threadCpuTimeNanos() else 0
                        mixPcmWindowBlockInto(at, rate, levels, count, samples, gains, windows, windowStarts)
                        val end = System.nanoTime()
                        val endCpu = if (observing) android.os.Debug.threadCpuTimeNanos() else 0
                        return PreparedPcmBlock(at, count, samples, (mixStart - prepareStart) / 1000,
                            (end - mixStart) / 1000, (mixCpu - prepareCpu) / 1000, (endCpu - mixCpu) / 1000)
                    }
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
                    val pipeline = if (parallel) PcmRenderPipeline(this, mixDispatcher, frame, duration, ::prepareBlock) else null
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
                        val observer = renderObserver
                        val observing = observer != null
                        val outputStarted = started
                        val queuedBefore = if (observing) submittedOutputFrames - outputClock.position(track.playbackHeadPosition) else 0
                        val underrunsBefore = if (observing) track.underrunCount else 0
                        val readyBudgetMs = if (pipeline != null && started) pcmReadyWaitMs(
                            submittedOutputFrames - outputClock.position(track.playbackHeadPosition), rate, track.bufferSizeInFrames) else 0
                        val prepared = if (pipeline != null) requireNotNull(pipeline.take(readyBudgetMs) {
                            outputWaits++
                            track.pause()
                        }) { "PCM pipeline ended before the final frame" }
                        else prepareBlock(next, blockFrames, mixed)
                        check(prepared.frame == next && prepared.frames == blockFrames) { "PCM pipeline lost frame order" }
                        mixed = prepared.samples
                        ensureActive()
                        peakPrepareUs = maxOf(peakPrepareUs, prepared.prepareUs)
                        val tempoStart = System.nanoTime()
                        val tempoCpu = if (observing) android.os.Debug.threadCpuTimeNanos() else 0
                        peakMixUs = maxOf(peakMixUs, prepared.mixUs)
                        val processed = if (processor.active) processor.queue(mixed, blockFrames) else null
                        val writeStart = System.nanoTime()
                        val writeCpu = if (observing) android.os.Debug.threadCpuTimeNanos() else 0
                        val queuedBeforeWrite = if (observing) submittedOutputFrames - outputClock.position(track.playbackHeadPosition) else 0
                        // Per-block work sum; producer and consumer may overlap.
                        peakRenderUs = maxOf(peakRenderUs, prepared.prepareUs + prepared.mixUs + (writeStart - tempoStart) / 1000)
                        if (processed != null) submitBytes(processed) else submitFloats(blockFrames * 2)
                        if (observer != null) observer(PcmRenderTrace(next,
                            prepared.prepareUs, prepared.mixUs, (writeStart - tempoStart) / 1000,
                            (System.nanoTime() - writeStart) / 1000, prepared.prepareCpuUs,
                            prepared.mixCpuUs, (writeCpu - tempoCpu) / 1000,
                            (android.os.Debug.threadCpuTimeNanos() - writeCpu) / 1000,
                            queuedBefore, queuedBeforeWrite, underrunsBefore, track.underrunCount, outputStarted))
                        pipeline?.recycle(prepared) // AudioTrack and Sonic no longer own this input.
                        next += blockFrames
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastReport >= 100) {
                            lastReport = now
                            state.value = state.value.copy(frame = clockFrame(), submittedFrame = next,
                                underruns = track.underrunCount, maxMixUs = peakMixUs,
                                maxPrepareUs = peakPrepareUs, maxRenderUs = peakRenderUs,
                                decoderWaits = decoderWaits.get(), outputWaits = outputWaits)
                            host.onClock(state.value.frame * 1000 / rate, true)
                        }
                    } } finally { withContext(NonCancellable) {
                        pipeline?.close()
                        streams.forEach { it.close() }
                    } }
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
    suspend fun release() { stop(); scope.cancel(); renderDispatcher.close(); mixDispatcher.close() }
}
