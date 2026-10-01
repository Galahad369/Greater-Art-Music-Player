package com.local.listentomusic.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

enum class WaveformStatus { IDLE, CACHE_HIT, DECODING, READY, FAILED }
data class WaveformDiagnostics(
    val status: WaveformStatus = WaveformStatus.IDLE,
    val fileName: String? = null,
    val error: String? = null,
)

/** Noise-gated percentile scaling keeps quiet decoder noise from looking like music. */
internal fun normalizeWaveformPeaks(
    peaks: FloatArray,
    minimum: Float = 0.015f,
): FloatArray {
    val sorted = peaks.filter { it.isFinite() && it > 0f }.sorted()
    if (sorted.isEmpty()) return FloatArray(peaks.size) { minimum }
    val floor = (sorted[(sorted.lastIndex * 0.10f).toInt()] * 0.9f)
        .coerceAtMost(sorted.last() * 0.45f)
    val ceiling = sorted[(sorted.lastIndex * 0.92f).toInt()]
        .coerceAtLeast(floor + 0.001f)
    return FloatArray(peaks.size) { index ->
        val signal = ((peaks[index] - floor) / (ceiling - floor)).coerceIn(0f, 1f)
        minimum + signal.pow(0.78f) * (1f - minimum)
    }
}

/** Decodes real PCM peaks off the UI thread and keeps only a tiny local cache. */
class WaveformRepository(context: Context) {
    private val directory = File(context.cacheDir, "waveforms").apply { mkdirs() }
    private val _diagnostics = MutableStateFlow(WaveformDiagnostics())
    val diagnostics: StateFlow<WaveformDiagnostics> = _diagnostics.asStateFlow()
    // The player screen and media-transition warmup may request the same file at
    // once. One decoder prevents duplicate full-file work and codec contention.
    private val decodeMutex = Mutex()
    private val memory = object : LinkedHashMap<String, FloatArray>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?) = size > 32
    }

    suspend fun load(path: String, size: Long, modified: Long): FloatArray? = withContext(Dispatchers.IO) {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("$path|$size|$modified|$BINS|$CACHE_VERSION".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val cached = File(directory, "$key.bin")
        synchronized(memory) { memory[key] }?.let { return@withContext it }
        read(cached)?.let {
            synchronized(memory) { memory[key] = it }
            _diagnostics.value = WaveformDiagnostics(WaveformStatus.CACHE_HIT, File(path).name)
            return@withContext it
        }
        decodeMutex.withLock {
            // Another caller may have completed while this one waited.
            read(cached)?.let {
                synchronized(memory) { memory[key] = it }
                _diagnostics.value = WaveformDiagnostics(WaveformStatus.CACHE_HIT, File(path).name)
                return@withLock it
            }
            _diagnostics.value = WaveformDiagnostics(WaveformStatus.DECODING, File(path).name)
            val decoded = withTimeoutOrNull(90_000L) {
                (if (File(path).extension.equals("wav", true)) decodeWav(path) else null) ?: decode(path)
            }
            if (decoded == null) {
                if (_diagnostics.value.status != WaveformStatus.FAILED) {
                    _diagnostics.value = WaveformDiagnostics(WaveformStatus.FAILED, File(path).name, "No decodable PCM output")
                }
                return@withLock null
            }
            runCatching { write(cached, decoded) }
            synchronized(memory) { memory[key] = decoded }
            _diagnostics.value = WaveformDiagnostics(WaveformStatus.READY, File(path).name)
            decoded
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(memory) { memory.clear() }
        directory.listFiles()?.forEach { file -> runCatching { file.delete() } }
        _diagnostics.value = WaveformDiagnostics()
    }

    private suspend fun decode(path: String): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
            } else return null
            extractor.selectTrack(track)
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            val peaks = FloatArray(BINS)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val input = codec.getInputBuffer(index) ?: continue
                        val count = extractor.readSampleData(input, 0)
                        if (count < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, count, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = codec.outputFormat
                        pcmEncoding = if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) out.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        else AudioFormat.ENCODING_PCM_16BIT
                    }
                    else -> if (index >= 0) {
                        codec.getOutputBuffer(index)?.let { buffer ->
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            var peak = 0f
                            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                val floats = buffer.slice().order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                                while (floats.hasRemaining()) peak = max(peak, abs(floats.get()).coerceAtMost(1f))
                            } else {
                                val shorts = buffer.slice().order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                while (shorts.hasRemaining()) peak = max(peak, abs(shorts.get().toInt()) / 32768f)
                            }
                            val bin = ((info.presentationTimeUs.toDouble() / durationUs) * BINS).toInt().coerceIn(0, BINS - 1)
                            peaks[bin] = max(peaks[bin], peak)
                        }
                        codec.releaseOutputBuffer(index, false)
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                }
            }
            normalize(peaks)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _diagnostics.value = WaveformDiagnostics(
                WaveformStatus.FAILED,
                File(path).name,
                "${error.javaClass.simpleName}: ${error.message.orEmpty()}".take(180),
            )
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Direct WAV reader avoids device codec quirks and samples every time segment. */
    private suspend fun decodeWav(path: String): FloatArray? = runCatching {
        java.io.RandomAccessFile(path, "r").use { input ->
            if (input.length() < 44L) return null
            val riff = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
            val little = riff == "RIFF"
            if (!little && riff != "RIFX") return null
            readInt(input, little)
            if (ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII) != "WAVE") return null
            var formatCode = 1
            var channels = 1
            var bits = 16
            var dataStart = -1L
            var dataSize = 0L
            while (input.filePointer + 8 <= input.length()) {
                val id = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
                val size = readInt(input, little).toLong() and 0xffffffffL
                val start = input.filePointer
                if (id == "fmt " && size >= 16) {
                    formatCode = readShort(input, little)
                    channels = readShort(input, little).coerceAtLeast(1)
                    input.skipBytes(6)
                    input.skipBytes(4)
                    bits = readShort(input, little)
                } else if (id == "data") {
                    dataStart = start
                    dataSize = minOf(size, input.length() - start)
                    break
                }
                input.seek((start + size + (size and 1L)).coerceAtMost(input.length()))
            }
            if (dataStart < 0 || dataSize <= 0 || formatCode !in setOf(1, 3)) return null
            val bytesPerSample = ((bits + 7) / 8).coerceIn(1, 4)
            val frameBytes = bytesPerSample * channels
            val peaks = FloatArray(BINS)
            repeat(BINS) { bin ->
                coroutineContext.ensureActive()
                val segmentStart = dataStart + dataSize * bin / BINS
                val segmentEnd = dataStart + dataSize * (bin + 1) / BINS
                val frames = ((segmentEnd - segmentStart) / frameBytes).coerceAtLeast(1L)
                val stride = (frames / MAX_SAMPLES_PER_BIN).coerceAtLeast(1L)
                var frame = 0L
                var peak = 0f
                while (frame < frames) {
                    input.seek(segmentStart + frame * frameBytes)
                    repeat(channels) {
                        peak = max(peak, readPcmSample(input, bits, formatCode, little))
                    }
                    frame += stride
                }
                peaks[bin] = peak
            }
            normalize(peaks)
        }
    }.getOrElse { error ->
        _diagnostics.value = WaveformDiagnostics(WaveformStatus.FAILED, File(path).name, "WAV: ${error.message.orEmpty()}".take(180))
        null
    }

    private fun readPcmSample(input: java.io.RandomAccessFile, bits: Int, format: Int, little: Boolean): Float {
        if (format == 3 && bits == 32) {
            return abs(Float.fromBits(readInt(input, little))).coerceAtMost(1f)
        }
        val signed = when (bits) {
            8 -> input.readUnsignedByte() - 128
            16 -> readShort(input, little).toShort().toInt()
            24 -> {
                val b = ByteArray(3).also(input::readFully)
                var value = if (little) (b[0].toInt() and 255) or ((b[1].toInt() and 255) shl 8) or ((b[2].toInt() and 255) shl 16)
                else (b[2].toInt() and 255) or ((b[1].toInt() and 255) shl 8) or ((b[0].toInt() and 255) shl 16)
                if (value and 0x800000 != 0) value = value or -0x1000000
                value
            }
            else -> readInt(input, little)
        }
        val denominator = when (bits) { 8 -> 128f; 16 -> 32768f; 24 -> 8388608f; else -> 2147483648f }
        return abs(signed / denominator).coerceAtMost(1f)
    }

    private fun readShort(input: java.io.RandomAccessFile, little: Boolean): Int {
        val a = input.readUnsignedByte(); val b = input.readUnsignedByte()
        return if (little) a or (b shl 8) else (a shl 8) or b
    }

    private fun readInt(input: java.io.RandomAccessFile, little: Boolean): Int {
        val a = input.readUnsignedByte(); val b = input.readUnsignedByte()
        val c = input.readUnsignedByte(); val d = input.readUnsignedByte()
        return if (little) a or (b shl 8) or (c shl 16) or (d shl 24)
        else (a shl 24) or (b shl 16) or (c shl 8) or d
    }

    private fun normalize(peaks: FloatArray): FloatArray {
        // Ignore tiny decoder noise and one-off spikes instead of dividing every
        // bar by the single loudest sample.
        return normalizeWaveformPeaks(peaks, MIN_VISUAL_PEAK)
    }

    private fun read(file: File): FloatArray? = runCatching {
        if (!file.isFile) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            val count = input.readInt()
            if (count != BINS) return null
            FloatArray(count) { input.readFloat().also { require(it.isFinite() && it in 0f..1f) } }
        }
    }.getOrNull()

    private fun write(file: File, values: FloatArray) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        DataOutputStream(temporary.outputStream().buffered()).use { output ->
            output.writeInt(values.size)
            values.forEach(output::writeFloat)
        }
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
    }

    private companion object {
        const val BINS = 96
        const val CACHE_VERSION = 2
        const val MIN_VISUAL_PEAK = 0.015f
        const val TIMEOUT_US = 10_000L
        const val MAX_SAMPLES_PER_BIN = 12_000L
    }
}
