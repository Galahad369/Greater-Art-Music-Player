package com.local.listentomusic.playback

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

internal data class StackPcmWindow(val mono: FloatArray, val side: FloatArray, val sampleRate: Int, val stereo: Boolean)

/** Analysis only. Never attaches to a Surface or creates an audible output. */
internal suspend fun decodeStackPcmWindow(file: MediaFile, startMs: Long, lengthMs: Long,
    requestedRate: Int? = STACK_INSTRUMENT_RATE): StackPcmWindow {
    require(startMs >= 0 && lengthMs in 100..36_000)
    require(requestedRate != null || lengthMs <= 1000)
    val source = File(file.sourcePath)
    require(source.isFile && source.canRead() && MediaScanner.isInsideTarget(source))
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    val startUs = (file.clipStartMs + startMs) * 1000
    val endUs = minOf(startUs + lengthMs * 1000, file.clipEndMs?.times(1000) ?: Long.MAX_VALUE)
    var rate = 0; var channels = 0; var encoding = AudioFormat.ENCODING_PCM_16BIT
    var analysisRate = 0
    var mono = FloatArray(0); var side = FloatArray(0); var counts = IntArray(0)
    var last = -1
    fun format(value: MediaFormat) {
        rate = value.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        channels = value.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        encoding = if (value.containsKey(MediaFormat.KEY_PCM_ENCODING)) value.getInteger(MediaFormat.KEY_PCM_ENCODING)
            else AudioFormat.ENCODING_PCM_16BIT
        require(rate in 8000..192000 && channels in 1..8)
        require(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT)
        val nextRate = requestedRate ?: rate
        require(last < 0 || analysisRate == nextRate) { "Decoder changed sample rate mid-window" }
        if (analysisRate != nextRate) {
            analysisRate = nextRate
            val n = ((endUs - startUs) * analysisRate / 1_000_000).toInt()
            require(n in 1..576000)
            mono = FloatArray(n); side = FloatArray(n); counts = IntArray(n)
        }
    }
    fun accept(buffer: ByteBuffer, pts: Long) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        var frame = 0L
        while (buffer.remaining() >= channels * bytes) {
            var sum = 0.0; var left = 0.0; var right = 0.0
            repeat(channels) { channel ->
                val sample = if (bytes == 4) buffer.float.toDouble() else buffer.short / 32768.0
                val value = sample.takeIf { it.isFinite() } ?: 0.0
                sum += value
                if (channel == 0) left = value
                if (channel == 1) right = value
            }
            // Avoid twice truncating a native sample through an integer microsecond PTS.
            val position = (pts - startUs) * analysisRate / 1_000_000.0 + frame++ * analysisRate.toDouble() / rate
            if (position < 0 || position >= mono.size) continue
            val bin = if (requestedRate == null) kotlin.math.round(position).toInt() else position.toInt()
            if (bin in mono.indices) {
                mono[bin] += (sum / channels).toFloat()
                side[bin] += if (channels >= 2) ((left - right) * .5).toFloat() else 0f
                counts[bin]++; last = maxOf(last, bin)
            }
        }
    }
    try {
        extractor.setDataSource(file.sourcePath)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("No audio track")
        val sourceFormat = extractor.getTrackFormat(track)
        val mime = sourceFormat.getString(MediaFormat.KEY_MIME) ?: error("No MIME")
        format(sourceFormat)
        extractor.selectTrack(track)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val deadline = SystemClock.elapsedRealtime() + 45_000
        if (mime == "audio/raw") {
            val buffer = ByteBuffer.allocate(1024 * 1024)
            while (extractor.sampleTime in 0 until endUs) {
                coroutineContext.ensureActive()
                check(SystemClock.elapsedRealtime() < deadline) { "Analysis decoder timeout" }
                buffer.clear()
                val n = extractor.readSampleData(buffer, 0)
                if (n < 0) break
                buffer.position(0); buffer.limit(n)
                accept(buffer, extractor.sampleTime)
                if (!extractor.advance()) break
            }
        } else {
            val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
            decoder.configure(sourceFormat, null, null, 0); decoder.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false; var done = false
            while (!done) {
                coroutineContext.ensureActive()
                check(SystemClock.elapsedRealtime() < deadline) { "Analysis decoder timeout" }
                if (!inputDone) {
                    val index = decoder.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = requireNotNull(decoder.getInputBuffer(index))
                        buffer.clear()
                        val pts = extractor.sampleTime
                        val n = if (pts < 0 || pts >= endUs) -1 else extractor.readSampleData(buffer, 0)
                        if (n < 0) {
                            decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true
                        } else {
                            decoder.queueInputBuffer(index, 0, n, pts, 0); extractor.advance()
                        }
                    }
                }
                val index = decoder.dequeueOutputBuffer(info, 10_000)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) format(decoder.outputFormat)
                else if (index >= 0) try {
                    decoder.getOutputBuffer(index)?.let { buffer ->
                        buffer.position(info.offset); buffer.limit(info.offset + info.size)
                        accept(buffer, info.presentationTimeUs)
                    }
                    done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                } finally { decoder.releaseOutputBuffer(index, false) }
            }
        }
        require(last >= 0) { "No samples in clipped window" }
        for (i in 0..last) if (counts[i] > 0) { mono[i] /= counts[i]; side[i] /= counts[i] }
        // Low-rate sources leave empty analysis bins. Interpolate only this fingerprint,
        // never the real playback source. Native-rate refinement needs no upsampling.
        if (requestedRate != null && rate < analysisRate) {
            var previous = -1
            for (i in 0..last) if (counts[i] > 0) {
                if (previous >= 0) for (j in previous + 1 until i) {
                    val weight = (j - previous).toFloat() / (i - previous)
                    mono[j] = mono[previous] * (1 - weight) + mono[i] * weight
                    side[j] = side[previous] * (1 - weight) + side[i] * weight
                }
                previous = i
            }
        }
        return StackPcmWindow(mono.copyOf(last + 1), side.copyOf(last + 1), analysisRate, channels >= 2)
    } finally {
        codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
        extractor.release()
    }
}
