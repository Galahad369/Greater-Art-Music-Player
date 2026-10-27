package com.local.listentomusic.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.source.SampleQueue
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.wav.WavExtractor
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaSource
import java.io.Closeable
import java.nio.ByteBuffer
import kotlin.math.roundToLong

internal fun nativePcmBytes(encoding: Int): Int = when (encoding) {
    C.ENCODING_PCM_8BIT -> 1
    C.ENCODING_PCM_16BIT -> 2
    C.ENCODING_PCM_24BIT -> 3
    C.ENCODING_PCM_FLOAT -> 4
    // A float mixer cannot preserve every bit of integer PCM32 or float64.
    else -> error("Unsupported native PCM precision: $encoding")
}

internal fun nativePcmSample(buffer: ByteBuffer, encoding: Int): Float = when (encoding) {
    C.ENCODING_PCM_8BIT -> ((buffer.get().toInt() and 255) - 128) / 128f
    C.ENCODING_PCM_16BIT -> buffer.short / 32768f
    C.ENCODING_PCM_24BIT -> ((buffer.get().toInt() and 255) or
        ((buffer.get().toInt() and 255) shl 8) or (buffer.get().toInt() shl 16)) / 8388608f
    C.ENCODING_PCM_FLOAT -> buffer.float.also { require(it.isFinite()) { "Nonfinite native PCM" } }
    else -> error("Unsupported native PCM precision: $encoding")
}

/** Existing Media3 extractor, not Android's app-UID WAV converter. One bounded sample at a time.
 * No new parser/dependency, whole-file PCM cache, resampling or precision fallback. */
internal class NativeWavPcm private constructor(private val source: DataSource, private val uri: Uri) : Closeable {
    private val extractor = WavExtractor()
    private val queue = SampleQueue.createWithoutDrm(DefaultAllocator(true, 64 * 1024))
    private val holder = FormatHolder()
    private val buffer = DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL)
    private val seek = PositionHolder()
    private lateinit var input: DefaultExtractorInput
    private var seekMap: SeekMap? = null
    private var ended = false
    val format: Format get() = requireNotNull(queue.upstreamFormat)

    private fun open(position: Long): DefaultExtractorInput {
        val remaining = source.open(DataSpec.Builder().setUri(uri).setPosition(position).build())
        return DefaultExtractorInput(source, position, if (remaining == C.LENGTH_UNSET.toLong()) remaining else position + remaining)
    }

    private fun read() {
        when (extractor.read(input, seek)) {
            Extractor.RESULT_END_OF_INPUT -> ended = true
            Extractor.RESULT_SEEK -> { source.close(); input = open(seek.position) }
        }
    }

    fun spec(file: MediaFile): PcmSpec {
        val rate = format.sampleRate
        require(rate in 8000..192000 && format.channelCount in 1..2) { "Native WAV requires mono/stereo" }
        nativePcmBytes(format.pcmEncoding)
        val durationUs = requireNotNull(seekMap).durationUs
        require(durationUs > 0)
        // Media3 rounds duration down to microseconds; recover the final native frame.
        val totalFrames = (durationUs * rate + 999_999) / 1_000_000
        val clipFirst = (file.clipStartMs * rate / 1000.0).roundToLong()
        val clipEnd = minOf(totalFrames, file.clipEndMs?.let { (it * rate / 1000.0).roundToLong() } ?: totalFrames)
        require(clipEnd > clipFirst) { "Empty native WAV clip" }
        return PcmSpec(rate, format.channelCount, clipEnd - clipFirst, nativeWav = true)
    }

    fun seekTo(frame: Long) {
        val point = requireNotNull(seekMap).getSeekPoints(frame * 1_000_000 / format.sampleRate).first
        source.close()
        input = open(point.position)
        queue.reset()
        extractor.seek(point.position, point.timeUs)
        ended = false
    }

    /** The buffer belongs to this reader and remains valid only until the next call. */
    fun next(): DecoderInputBuffer? {
        while (true) {
            buffer.clear()
            when (queue.read(holder, buffer, 0, ended)) {
                C.RESULT_BUFFER_READ -> {
                    if (buffer.isEndOfStream) return null
                    buffer.flip()
                    queue.discardToRead()
                    return buffer
                }
                C.RESULT_FORMAT_READ -> {
                    require(holder.format?.pcmEncoding == format.pcmEncoding) { "Native WAV format changed" }
                }
                C.RESULT_NOTHING_READ -> read()
            }
        }
    }

    override fun close() {
        try { source.close() } finally { extractor.release(); queue.release() }
    }

    companion object {
        fun openOrNull(file: MediaFile, context: Context?): NativeWavPcm? {
            val location = file.reference.source
            val uri = when (location) {
                is MediaSource.LocalFile -> Uri.fromFile(java.io.File(location.value))
                is MediaSource.ContentUri -> Uri.parse(location.value)
            }
            val source = if (context == null) FileDataSource() else DefaultDataSource(context, FileDataSource())
            val reader = NativeWavPcm(source, uri)
            try {
                reader.input = reader.open(0)
                if (!reader.extractor.sniff(reader.input)) { reader.close(); return null }
                reader.input.resetPeekPosition()
                reader.extractor.init(object : ExtractorOutput {
                    override fun track(id: Int, type: Int): TrackOutput = reader.queue
                    override fun endTracks() = Unit
                    override fun seekMap(map: SeekMap) { reader.seekMap = map }
                })
                while (reader.queue.upstreamFormat == null || reader.seekMap == null) {
                    check(!reader.ended) { "Incomplete native WAV header" }
                    reader.read()
                }
                return reader
            } catch (failure: Exception) { reader.close(); throw failure }
        }
    }
}
