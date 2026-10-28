package com.local.listentomusic.playback

import android.media.AudioFormat
import com.local.listentomusic.data.openMedia
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

internal data class PcmSpec(val rate: Int, val channels: Int, val durationFrames: Long, val nativeWav: Boolean = false)
internal data class NativePcmBlock(val start: Long, val samples: FloatArray, val channels: Int) {
    val end: Long get() = start + samples.size / channels
}

/** App-UID MediaExtractor can report native 24/32-bit WAV as already truncated PCM16. */
internal fun pcmPrecisionAdmissible(mime: String, sourceBits: Int?, outputEncoding: Int,
    sourceFloat: Boolean = false): Boolean =
    outputEncoding == AudioFormat.ENCODING_PCM_FLOAT &&
        (if (mime == "audio/raw" && sourceFloat) sourceBits == null || sourceBits in 1..32
        else sourceBits in 1..24 || sourceBits == null && mime != "audio/raw") ||
        outputEncoding == AudioFormat.ENCODING_PCM_16BIT && when (mime) {
            "audio/raw" -> sourceBits != null && sourceBits in 1..16
            "audio/mp4a-latm", "audio/mpeg" -> true
            else -> false
        }

private fun MediaFormat.sourceBits(): Int? =
    if (containsKey("bits-per-sample")) getInteger("bits-per-sample").takeIf { it > 0 } else null

private fun requirePcmPrecision(mime: String, source: MediaFormat, outputEncoding: Int) {
    val sourceBits = source.sourceBits()
    val sourceFloat = mime == "audio/raw" && source.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
        source.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
    require(pcmPrecisionAdmissible(mime, sourceBits, outputEncoding, sourceFloat)) {
        "Unsupported PCM precision: native bits=$sourceBits, output=$outputEncoding; legacy output retained"
    }
}

internal fun pcmSpec(file: MediaFile, context: android.content.Context? = null): PcmSpec {
    val source = File(file.sourcePath)
    require(if (context != null) com.local.listentomusic.data.mediaReadable(context, file)
        else source.isFile && source.canRead() && MediaScanner.isInsideTarget(source))
    NativeWavPcm.openOrNull(file, context)?.use { return it.spec(file) }
    val extractor = MediaExtractor()
    try {
        extractor.openMedia(context, file.sourcePath)
        val index = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("No audio track")
        val format = extractor.getTrackFormat(index)
        if (format.getString(MediaFormat.KEY_MIME) == "audio/raw") {
            val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
                format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
            requirePcmPrecision("audio/raw", format, encoding)
        }
        val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        require(rate in 8000..192000 && channels in 1..2) { "Prototype requires mono/stereo native PCM" }
        val endUs = minOf(format.getLong(MediaFormat.KEY_DURATION), file.clipEndMs?.times(1000) ?: Long.MAX_VALUE)
        val frames = ((endUs - file.clipStartMs * 1000) * rate / 1_000_000).coerceAtLeast(0)
        require(frames > 0)
        return PcmSpec(rate, channels, frames)
    } finally { extractor.release() }
}

/** About 250ms native decode-ahead per lane, bounded independently of file length.
 * Four chunks at 192kHz are only 21ms: that cannot cover output prefill or IO scheduling. */
internal class StackPcmStream(scope: CoroutineScope, private val file: MediaFile,
    val spec: PcmSpec, startFrame: Long, context: android.content.Context? = null) {
    val blocks = Channel<NativePcmBlock>(maxOf(4, (spec.rate + 4095) / 4096))
    private val job = scope.launch(Dispatchers.IO) {
        if (spec.nativeWav) {
            try {
                requireNotNull(NativeWavPcm.openOrNull(file, context)).use { reader ->
                    check(reader.spec(file) == spec) { "Native WAV changed after preflight" }
                    val encoding = reader.format.pcmEncoding
                    val bytes = nativePcmBytes(encoding)
                    val firstSource = (file.clipStartMs * spec.rate / 1000.0).roundToLong()
                    val seekFrame = (startFrame - PCM_SINC_RADIUS * 2).coerceAtLeast(0)
                    reader.seekTo(firstSource + seekFrame)
                    while (true) {
                        ensureActive()
                        val packet = reader.next() ?: break
                        val buffer = requireNotNull(packet.data).order(ByteOrder.LITTLE_ENDIAN)
                        val base = (packet.timeUs * spec.rate / 1_000_000.0).roundToLong() - firstSource
                        var consumed = 0
                        while (buffer.remaining() >= spec.channels * bytes) {
                            ensureActive()
                            val count = minOf(1024, buffer.remaining() / (spec.channels * bytes))
                            val values = FloatArray(count * spec.channels) { nativePcmSample(buffer, encoding) }
                            val begin = base + consumed
                            consumed += count
                            val first = maxOf(0L, seekFrame - begin).coerceAtMost(count.toLong()).toInt()
                            val last = minOf(count.toLong(), spec.durationFrames - begin).coerceAtLeast(0).toInt()
                            if (last > first) blocks.send(NativePcmBlock(begin + first,
                                if (first == 0 && last == count) values else values.copyOfRange(first * spec.channels, last * spec.channels), spec.channels))
                        }
                        if (base + consumed >= spec.durationFrames) break
                    }
                }
                blocks.close()
            } catch (failure: Exception) { blocks.close(failure) }
            return@launch
        }
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.openMedia(context, file.sourcePath)
            val index = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            val format = extractor.getTrackFormat(index)
            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            // AAC/MP3 have no integer source bit depth. Keep the codec's native
            // PCM16 when it ignores a float request, then convert losslessly to
            // mixer float. Never request a lower precision, cap source rate, or
            // accept PCM16 as a substitute for high-bit-depth lossless material.
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            fun setFormat(output: MediaFormat) {
                require(output.getInteger(MediaFormat.KEY_SAMPLE_RATE) == spec.rate &&
                    output.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == spec.channels) { "PCM format changed" }
                encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    else AudioFormat.ENCODING_PCM_16BIT
                requirePcmPrecision(mime, format, encoding)
            }
            if (mime == "audio/raw") setFormat(format)
            extractor.selectTrack(index)
            val seekFrame = (startFrame - PCM_SINC_RADIUS * 2).coerceAtLeast(0)
            val clipUs = file.clipStartMs * 1000
            extractor.seekTo(clipUs + seekFrame * 1_000_000 / spec.rate, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            suspend fun emit(buffer: ByteBuffer, pts: Long) {
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                val base = ((pts - clipUs) * spec.rate / 1_000_000.0).roundToLong()
                var consumed = 0
                while (buffer.remaining() >= spec.channels * bytes) {
                    ensureActive()
                    val count = minOf(1024, buffer.remaining() / (spec.channels * bytes))
                    val values = FloatArray(count * spec.channels) {
                        (if (bytes == 4) buffer.float else buffer.short / 32768f).also { require(it.isFinite()) }
                    }
                    val begin = base + consumed
                    consumed += count
                    val first = maxOf(0L, seekFrame - begin).coerceAtMost(count.toLong()).toInt()
                    val last = minOf(count.toLong(), spec.durationFrames - begin).coerceAtLeast(0).toInt()
                    if (last > first) blocks.send(NativePcmBlock(begin + first,
                        if (first == 0 && last == count) values else values.copyOfRange(first * spec.channels, last * spec.channels), spec.channels))
                }
            }
            val endUs = clipUs + spec.durationFrames * 1_000_000 / spec.rate
            if (mime == "audio/raw") {
                val buffer = ByteBuffer.allocate(256 * 1024)
                while (extractor.sampleTime in 0 until endUs) {
                    ensureActive(); buffer.clear()
                    val count = extractor.readSampleData(buffer, 0)
                    if (count < 0) break
                    buffer.position(0); buffer.limit(count); emit(buffer, extractor.sampleTime)
                    if (!extractor.advance()) break
                }
            } else {
                // Prefer native float; high-bit-depth/lossless sources require it.
                format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_FLOAT)
                val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
                decoder.configure(format, null, null, 0); decoder.start()
                val info = MediaCodec.BufferInfo()
                var inputDone = false; var done = false
                var lastProgress = android.os.SystemClock.elapsedRealtime()
                while (!done) {
                    ensureActive()
                    check(android.os.SystemClock.elapsedRealtime() - lastProgress < 10_000) { "PCM decoder stalled" }
                    if (!inputDone) {
                        val slot = decoder.dequeueInputBuffer(10_000)
                        if (slot >= 0) {
                            val input = requireNotNull(decoder.getInputBuffer(slot)); input.clear()
                            val pts = extractor.sampleTime
                            val size = if (pts < 0 || pts >= endUs) -1 else extractor.readSampleData(input, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(slot, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true
                            } else { decoder.queueInputBuffer(slot, 0, size, pts, 0); extractor.advance() }
                            lastProgress = android.os.SystemClock.elapsedRealtime()
                        }
                    }
                    val slot = decoder.dequeueOutputBuffer(info, 10_000)
                    if (slot == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        setFormat(decoder.outputFormat)
                    }
                    else if (slot >= 0) try {
                        decoder.getOutputBuffer(slot)?.let { output ->
                            output.position(info.offset); output.limit(info.offset + info.size)
                            emit(output, info.presentationTimeUs)
                        }
                        lastProgress = android.os.SystemClock.elapsedRealtime()
                        done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    } finally { decoder.releaseOutputBuffer(slot, false) }
                }
            }
            blocks.close()
        } catch (failure: Exception) { blocks.close(failure) }
        finally {
            codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
            extractor.release()
        }
    }
    suspend fun close() { job.cancelAndJoin(); blocks.cancel() }
}

internal class PcmLane(private val stream: StackPcmStream, private val onWait: () -> Unit) {
    private val held = ArrayDeque<NativePcmBlock>()
    private var current: NativePcmBlock? = null
    private var ended = false
    suspend fun prepare(first: Long, last: Long) {
        while (held.size > 1 && held.first().end < first) {
            if (current === held.removeFirst()) current = null
        }
        if (last < 0 || first >= stream.spec.durationFrames) return
        while (!ended && (held.lastOrNull()?.end ?: Long.MIN_VALUE) <= last) {
            val next = withTimeoutOrNull(200) { stream.blocks.receiveCatching() }
                ?: run { onWait(); withTimeout(2000) { stream.blocks.receiveCatching() } }
            next.exceptionOrNull()?.let { throw it }
            val block = next.getOrNull()
            if (block == null) ended = true else {
                require(held.lastOrNull()?.let { block.start <= it.end + 2 } != false) { "Discontinuous PCM timeline" }
                held.addLast(block)
                // The output rate is >= every lane's rate; a render block plus halo holds only a few chunks.
                require(held.size <= 6) { "PCM lane memory bound exceeded" }
            }
        }
    }
    fun sample(frame: Long, channel: Int): Float {
        if (frame < 0 || frame >= stream.spec.durationFrames) return 0f
        var block = current
        if (block == null || frame < block.start || frame >= block.end) {
            block = null
            for (index in 0 until held.size) {
                val candidate = held[index]
                if (frame >= candidate.start && frame < candidate.end) { block = candidate; break }
            }
            current = block
        }
        block ?: return 0f
        return block.samples[(frame - block.start).toInt() * block.channels + if (block.channels == 1) 0 else channel]
    }
}
