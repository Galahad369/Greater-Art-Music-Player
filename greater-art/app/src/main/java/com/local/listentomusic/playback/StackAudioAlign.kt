package com.local.listentomusic.playback

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Separate, bounded analysis decoder. Never changes the real player's tracks or source.
 * v2 stores both the existing 20 ms energy envelope and a lightweight 12-bin chroma
 * fingerprint so different vocal timbres can still align on shared harmonic structure.
 */
class StackAudioAlign(context: Context) {
    private val cache = File(context.applicationContext.cacheDir, "stack-align-v3")

    companion object {
        private val mutex = Mutex()
        private const val FEATURE_BINS = 4500
        private const val CHROMA_SIZE = 12
        private const val CHROMA_SAMPLES = 64
        private const val CHROMA_FREQUENCIES = 36
        private const val CHROMA_WINDOW_BINS = 5
        private const val CHROMA_WINDOW_SAMPLES = CHROMA_SAMPLES * CHROMA_WINDOW_BINS
        private const val CHROMA_SAMPLE_RATE = CHROMA_SAMPLES * 50.0
        private const val CACHE_LIMIT = 32

        private val chromaWindow = DoubleArray(CHROMA_WINDOW_SAMPLES) { index ->
            0.5 - 0.5 * cos(2.0 * PI * index / (CHROMA_WINDOW_SAMPLES - 1))
        }
        private val chromaCoefficients = DoubleArray(CHROMA_FREQUENCIES) { semitone ->
            val frequency = 130.81278265 * 2.0.pow(semitone / 12.0) // C3..B4
            2.0 * cos(2.0 * PI * frequency / CHROMA_SAMPLE_RATE)
        }
    }

    suspend fun estimate(primary: MediaFile, companion: MediaFile): StackAlignment = withContext(Dispatchers.Default) {
        val a = features(primary)
        val b = features(companion)
        coroutineContext.ensureActive()
        correlateStackFeatures(a, b)
    }

    suspend fun estimateAll(primary: MediaFile, companions: List<MediaFile>, progress: (Int) -> Unit): Map<String, StackAlignment> {
        val reference = features(primary)
        return companions.mapIndexed { index, companion ->
            progress(index)
            val candidate = features(companion)
            val match = withContext(Dispatchers.Default) {
                val activeContext = coroutineContext
                correlateStackFeatures(reference, candidate) { activeContext.ensureActive() }
            }
            companion.path to match
        }.toMap()
    }

    private suspend fun features(file: MediaFile): StackAudioFeatures = withContext(Dispatchers.IO) {
        mutex.withLock {
            val source = File(file.sourcePath)
            require(source.isFile && source.canRead() && MediaScanner.isInsideTarget(source))
            val identity = "${source.canonicalPath}|${source.length()}|${source.lastModified()}|${file.clipStartMs}|${file.clipEndMs}"
            val key = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
                .joinToString("") { "%02x".format(it) }
            cache.mkdirs()
            val target = File(cache, "$key.bin")
            val cached = runCatching {
                DataInputStream(target.inputStream().buffered()).use { input ->
                    val n = input.readInt()
                    require(n in 1..FEATURE_BINS)
                    val envelope = FloatArray(n) {
                        input.readFloat().also { value -> require(value.isFinite() && value >= 0f) }
                    }
                    val chroma = Array(n) {
                        FloatArray(CHROMA_SIZE) {
                            input.readFloat().also { value -> require(value.isFinite() && value >= 0f && value <= 1.1f) }
                        }
                    }
                    StackAudioFeatures(envelope, chroma)
                }
            }.getOrNull()
            if (cached != null) return@withLock cached

            val decoded = decode(file)
            coroutineContext.ensureActive()
            val temporary = File(cache, "$key.tmp")
            try {
                DataOutputStream(temporary.outputStream().buffered()).use { output ->
                    output.writeInt(decoded.envelope.size)
                    decoded.envelope.forEach(output::writeFloat)
                    decoded.chroma.forEach { frame -> frame.forEach(output::writeFloat) }
                }
                if (!temporary.renameTo(target)) temporary.delete()
                cache.listFiles()
                    ?.filter { it.extension == "bin" }
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(CACHE_LIMIT)
                    ?.forEach { it.delete() }
            } finally {
                temporary.delete()
            }
            decoded
        }
    }

    private suspend fun decode(file: MediaFile): StackAudioFeatures {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val energy = FloatArray(FEATURE_BINS)
        val counts = IntArray(FEATURE_BINS)
        val chromaSamples = FloatArray(FEATURE_BINS * CHROMA_SAMPLES)
        val chromaCounts = IntArray(FEATURE_BINS * CHROMA_SAMPLES)
        var rate = 44100
        var channels = 1
        var encoding = AudioFormat.ENCODING_PCM_16BIT
        val startUs = file.clipStartMs * 1000L
        val endUs = minOf(startUs + 90_000_000L, file.clipEndMs?.times(1000L) ?: Long.MAX_VALUE)

        fun accept(buffer: ByteBuffer, pts: Long) {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
            var frame = 0L
            while (buffer.remaining() >= channels * bytes) {
                var squared = 0.0
                var mono = 0.0
                repeat(channels) {
                    val value = if (bytes == 4) buffer.float.toDouble() else buffer.short / 32768.0
                    if (value.isFinite()) {
                        mono += value
                        squared += value * value
                    }
                }
                mono /= channels
                val time = pts + frame++ * 1_000_000L / rate
                val bin = ((time - startUs) / 20_000L).toInt()
                if (time >= startUs && time < endUs && bin in energy.indices) {
                    energy[bin] += (squared / channels).toFloat()
                    counts[bin]++

                    // Uniformly sample each 20 ms frame at 64 points. The resulting
                    // effective 3.2 kHz stream is sufficient for C3..B4 pitch classes.
                    val withinBinUs = (time - startUs) % 20_000L
                    val slot = ((withinBinUs * CHROMA_SAMPLES) / 20_000L)
                        .toInt()
                        .coerceIn(0, CHROMA_SAMPLES - 1)
                    val index = bin * CHROMA_SAMPLES + slot
                    // Average, rather than pick one high-rate sample: reduce aliasing
                    // from vocal harmonics before this analysis-only downsampling.
                    chromaSamples[index] += mono.toFloat()
                    chromaCounts[index]++
                }
            }
        }

        fun outputFormat(format: MediaFormat) {
            rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
            require(rate > 0 && channels in 1..8)
            require(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT)
        }

        fun chromaFrame(bin: Int): FloatArray {
            val base = (bin / CHROMA_WINDOW_BINS * CHROMA_WINDOW_BINS) * CHROMA_SAMPLES
            var populated = 0
            var mean = 0.0
            for (slot in 0 until CHROMA_WINDOW_SAMPLES) {
                if (base + slot < chromaCounts.size && chromaCounts[base + slot] > 0) {
                    populated++
                    mean += chromaSamples[base + slot] / chromaCounts[base + slot]
                }
            }
            if (populated < CHROMA_WINDOW_SAMPLES / 2) return FloatArray(CHROMA_SIZE)
            mean /= populated

            val folded = DoubleArray(CHROMA_SIZE)
            for (semitone in 0 until CHROMA_FREQUENCIES) {
                val coefficient = chromaCoefficients[semitone]
                var q1 = 0.0
                var q2 = 0.0
                for (slot in 0 until CHROMA_WINDOW_SAMPLES) {
                    val sample = if (base + slot < chromaCounts.size && chromaCounts[base + slot] > 0) {
                        (chromaSamples[base + slot] / chromaCounts[base + slot] - mean) * chromaWindow[slot]
                    } else {
                        0.0
                    }
                    val q0 = coefficient * q1 - q2 + sample
                    q2 = q1
                    q1 = q0
                }
                val power = (q1 * q1 + q2 * q2 - coefficient * q1 * q2).coerceAtLeast(0.0)
                folded[semitone % CHROMA_SIZE] += sqrt(power)
            }

            val norm = sqrt(folded.sumOf { it * it })
            if (norm <= 1e-8) return FloatArray(CHROMA_SIZE)
            return FloatArray(CHROMA_SIZE) { (folded[it] / norm).toFloat() }
        }

        try {
            extractor.setDataSource(file.sourcePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("No audio format")
            extractor.selectTrack(track)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val deadline = SystemClock.elapsedRealtime() + 60_000L

            if (mime == "audio/raw") {
                outputFormat(format)
                val buffer = ByteBuffer.allocate(1024 * 1024)
                while (extractor.sampleTime in 0 until endUs) {
                    coroutineContext.ensureActive()
                    check(SystemClock.elapsedRealtime() < deadline) { "Analysis timeout" }
                    buffer.clear()
                    val n = extractor.readSampleData(buffer, 0)
                    if (n < 0) break
                    buffer.position(0)
                    buffer.limit(n)
                    accept(buffer, extractor.sampleTime)
                    if (!extractor.advance()) break
                }
            } else {
                val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
                decoder.configure(format, null, null, 0)
                decoder.start()
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var done = false
                while (!done) {
                    coroutineContext.ensureActive()
                    check(SystemClock.elapsedRealtime() < deadline) { "Analysis timeout" }
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(10_000L)
                        if (index >= 0) {
                            val buffer = decoder.getInputBuffer(index)!!
                            buffer.clear()
                            val time = extractor.sampleTime
                            val n = if (time < 0 || time >= endUs) -1 else extractor.readSampleData(buffer, 0)
                            if (n < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(index, 0, n, time, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, 10_000L)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        outputFormat(decoder.outputFormat)
                    } else if (index >= 0) {
                        try {
                            decoder.getOutputBuffer(index)?.let { buffer ->
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                accept(buffer, info.presentationTimeUs)
                            }
                            done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally {
                            decoder.releaseOutputBuffer(index, false)
                        }
                    }
                }
            }

            val n = counts.indexOfLast { it > 0 } + 1
            require(n >= 150) { "At least three seconds of audio required" }
            val envelope = FloatArray(n) {
                if (counts[it] > 0) sqrt(energy[it] / counts[it]) else 0f
            }
            val windows = Array((n + CHROMA_WINDOW_BINS - 1) / CHROMA_WINDOW_BINS) {
                coroutineContext.ensureActive()
                chromaFrame(it * CHROMA_WINDOW_BINS)
            }
            val chroma = Array(n) { windows[it / CHROMA_WINDOW_BINS] }
            return StackAudioFeatures(envelope, chroma)
        } finally {
            codec?.let {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
            extractor.release()
        }
    }
}
