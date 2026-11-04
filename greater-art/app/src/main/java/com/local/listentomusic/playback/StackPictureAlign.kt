package com.local.listentomusic.playback

import android.content.Context
import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import com.local.listentomusic.data.OfflineAnalysisBudget
import com.local.listentomusic.data.openMedia
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal class StackPictureAlign(private val context: Context) {
    suspend fun estimateAll(
        primary: MediaFile,
        companions: List<MediaFile>,
        onProgress: (Int) -> Unit,
    ): Map<String, StackAlignment> = withContext(Dispatchers.Default) {
        val reference = readFrames(primary)
        val result = linkedMapOf<String, StackAlignment>()
        for ((index, file) in companions.withIndex()) {
            ensureActive()
            val frames = readFrames(file)
            estimateStackPictureOffset(reference, frames) { ensureActive() }?.let { result[file.path] = it }
            withContext(Dispatchers.Main.immediate) { onProgress(index + 1) }
        }
        result
    }

    internal suspend fun readFrames(file: MediaFile): List<StackPictureFrame> =
        withContext(Dispatchers.IO) {
            OfflineAnalysisBudget.mutex.withLock {
                try {
                    decode(file)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    // An unreadable clip or an unsupported analysis decoder must not change a track.
                    emptyList()
                }
            }
        }

    private suspend fun decode(file: MediaFile): List<StackPictureFrame> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val frames = ArrayList<StackPictureFrame>()
        val start = file.clipStartMs * 1_000L
        val end = minOf(start + 210_000_000L, file.clipEndMs?.times(1_000L) ?: Long.MAX_VALUE)
        try {
            extractor.openMedia(context, file.sourcePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: return emptyList()
            val format = extractor.getTrackFormat(track)
            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            val rotation = if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else 0
            format.setInteger(MediaFormat.KEY_ROTATION, 0)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            extractor.selectTrack(track)
            if (start > 0) extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
            decoder.configure(format, null, null, 0)
            decoder.start()
            val info = MediaCodec.BufferInfo()
            val deadline = SystemClock.elapsedRealtime() + 90_000L
            var inputDone = false
            var outputDone = false
            var lastBucket = -1L
            val luma = FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID)
            while (!outputDone) {
                coroutineContext.ensureActive()
                check(SystemClock.elapsedRealtime() < deadline) { "Picture analysis timed out" }
                if (!inputDone) {
                    val index = decoder.dequeueInputBuffer(10_000L)
                    if (index >= 0) {
                        val time = extractor.sampleTime
                        val size = if (time < 0 || time >= end + 250_000L) -1
                            else extractor.readSampleData(requireNotNull(decoder.getInputBuffer(index)), 0)
                        inputDone = size < 0
                        decoder.queueInputBuffer(index, 0, size.coerceAtLeast(0), time.coerceAtLeast(0),
                            if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        if (!inputDone) extractor.advance()
                    }
                }
                val index = decoder.dequeueOutputBuffer(info, 10_000L)
                if (index < 0) continue
                try {
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    val time = info.presentationTimeUs
                    if (info.size == 0 || time !in start until end) continue
                    val bucket = (time - start) * 30L / 1_000_000L
                    if (bucket <= lastBucket) continue
                    lastBucket = bucket
                    val image = decoder.getOutputImage(index) ?: continue
                    image.use {
                        if (it.format != ImageFormat.YUV_420_888) return@use
                        val plane = it.planes[0]
                        val crop = it.cropRect
                        val buffer = plane.buffer
                        for (y in 0 until STACK_PICTURE_GRID) for (x in 0 until STACK_PICTURE_GRID) {
                            val max = STACK_PICTURE_GRID - 1
                            val sx = when (rotation) { 90 -> y; 180 -> max - x; 270 -> max - y; else -> x }
                            val sy = when (rotation) { 90 -> max - x; 180 -> max - y; 270 -> x; else -> y }
                            val px = crop.left + (2 * sx + 1) * crop.width() / (2 * STACK_PICTURE_GRID)
                            val py = crop.top + (2 * sy + 1) * crop.height() / (2 * STACK_PICTURE_GRID)
                            val position = buffer.position() + py * plane.rowStride + px * plane.pixelStride
                            if (position >= buffer.limit()) return@use
                            luma[y * STACK_PICTURE_GRID + x] = (buffer.get(position).toInt() and 255).toFloat()
                        }
                        stackPictureFrame(time - start, luma)?.let(frames::add)
                    }
                } finally {
                    decoder.releaseOutputBuffer(index, false)
                }
            }
            return frames
        } finally {
            try { codec?.release() } finally { extractor.release() }
        }
    }
}
