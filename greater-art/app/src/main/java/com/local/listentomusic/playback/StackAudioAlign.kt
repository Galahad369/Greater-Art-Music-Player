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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

/** Separate, bounded analysis decoder. Never changes the real player's tracks or source. */
class StackAudioAlign(context: Context) {
    private val cache = File(context.applicationContext.cacheDir, "stack-align-v1")
    companion object { private val mutex = Mutex() }

    suspend fun estimate(primary: MediaFile, companion: MediaFile): StackAlignment = withContext(Dispatchers.Default) {
        val a = envelope(primary)
        val b = envelope(companion)
        coroutineContext.ensureActive()
        correlateStackEnvelopes(a, b)
    }

    private suspend fun envelope(file: MediaFile): FloatArray = withContext(Dispatchers.IO) {
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
                    val n = input.readInt(); require(n in 1..4500)
                    FloatArray(n) { input.readFloat().also { require(it.isFinite() && it >= 0f) } }
                }
            }.getOrNull()
            if (cached != null) return@withLock cached
            val decoded = decode(file)
            coroutineContext.ensureActive()
            val temporary = File(cache, "$key.tmp")
            try {
                DataOutputStream(temporary.outputStream().buffered()).use { output ->
                    output.writeInt(decoded.size); decoded.forEach(output::writeFloat)
                }
                if (!temporary.renameTo(target)) temporary.delete()
                cache.listFiles()?.filter { it.extension == "bin" }?.sortedByDescending { it.lastModified() }
                    ?.drop(64)?.forEach { it.delete() }
            } finally { temporary.delete() }
            decoded
        }
    }

    private suspend fun decode(file: MediaFile): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val result = FloatArray(4500)
        val counts = IntArray(4500)
        var rate = 44100; var channels = 1; var encoding = AudioFormat.ENCODING_PCM_16BIT
        val startUs = file.clipStartMs * 1000L
        val endUs = minOf(startUs + 90_000_000L, file.clipEndMs?.times(1000L) ?: Long.MAX_VALUE)
        fun accept(buffer: ByteBuffer, pts: Long) {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
            var frame = 0L
            while (buffer.remaining() >= channels * bytes) {
                var energy = 0.0
                repeat(channels) {
                    val value = if (bytes == 4) buffer.float.toDouble() else buffer.short / 32768.0
                    if (value.isFinite()) energy += value * value
                }
                val time = pts + frame++ * 1_000_000L / rate
                val bin = ((time - startUs) / 20_000L).toInt()
                if (time >= startUs && time < endUs && bin in result.indices) {
                    result[bin] += (energy / channels).toFloat(); counts[bin]++
                }
            }
        }
        fun outputFormat(format: MediaFormat) {
            rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                else AudioFormat.ENCODING_PCM_16BIT
            require(rate > 0 && channels in 1..8)
            require(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT)
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
                    coroutineContext.ensureActive(); check(SystemClock.elapsedRealtime() < deadline) { "Analysis timeout" }
                    buffer.clear(); val n = extractor.readSampleData(buffer, 0)
                    if (n < 0) break
                    buffer.position(0); buffer.limit(n); accept(buffer, extractor.sampleTime)
                    if (!extractor.advance()) break
                }
            } else {
                val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
                decoder.configure(format, null, null, 0); decoder.start()
                val info = MediaCodec.BufferInfo()
                var inputDone = false; var done = false
                while (!done) {
                    coroutineContext.ensureActive(); check(SystemClock.elapsedRealtime() < deadline) { "Analysis timeout" }
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(10_000L)
                        if (index >= 0) {
                            val buffer = decoder.getInputBuffer(index)!!; buffer.clear()
                            val time = extractor.sampleTime
                            val n = if (time < 0 || time >= endUs) -1 else extractor.readSampleData(buffer, 0)
                            if (n < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true
                            } else {
                                decoder.queueInputBuffer(index, 0, n, time, 0); extractor.advance()
                            }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, 10_000L)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) outputFormat(decoder.outputFormat)
                    else if (index >= 0) {
                        try {
                            decoder.getOutputBuffer(index)?.let { buffer ->
                                buffer.position(info.offset); buffer.limit(info.offset + info.size)
                                accept(buffer, info.presentationTimeUs)
                            }
                            done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally { decoder.releaseOutputBuffer(index, false) }
                    }
                }
            }
            val n = counts.indexOfLast { it > 0 } + 1
            require(n >= 150) { "At least three seconds of audio required" }
            return FloatArray(n) { if (counts[it] > 0) sqrt(result[it] / counts[it]) else 0f }
        } finally {
            codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
            extractor.release()
        }
    }
}
