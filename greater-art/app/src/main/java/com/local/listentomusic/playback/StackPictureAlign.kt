package com.local.listentomusic.playback

import android.content.Context
import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import com.local.listentomusic.data.OfflineAnalysisBudget
import com.local.listentomusic.data.openMedia
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class StackPictureAlign(private val context: Context) {
    data class Result(val matches: Map<String, StackAlignment>, val quickTimedOut: Boolean)

    private data class Samples(val windows: List<StackPictureWindow>, val frames: List<StackPictureFrame>)

    suspend fun estimateAll(
        primary: MediaFile,
        companions: List<MediaFile>,
        search: StackPictureSearch,
        onProgress: (Int) -> Unit,
    ): Result = withContext(Dispatchers.Default) {
        val result = linkedMapOf<String, StackAlignment>()
        suspend fun analyze() {
            val analysisContext = currentCoroutineContext()
            val reference = readSamples(primary, search) ?: return
            val anchors = prepareStackPictureReference(reference.frames) { analysisContext.ensureActive() } ?: return
            withContext(Dispatchers.Main.immediate) { onProgress(0) }
            for ((index, file) in companions.withIndex()) {
                analysisContext.ensureActive()
                val frames = readSamples(file, search, reference.windows)?.frames.orEmpty()
                estimateStackPictureOffset(anchors, frames, search) {
                    analysisContext.ensureActive()
                }?.let { result[file.path] = it }
                withContext(Dispatchers.Main.immediate) { onProgress(index + 1) }
            }
        }
        val timedOut = if (search == StackPictureSearch.QUICK) {
            // Budget the whole quick attempt, including waits for the analysis decoder.
            withTimeoutOrNull(45_000L) { analyze(); true } != true
        } else {
            analyze()
            false
        }
        Result(result, timedOut)
    }

    private suspend fun readSamples(
        file: MediaFile,
        search: StackPictureSearch,
        reference: List<StackPictureWindow>? = null,
    ): Samples? = withTimeoutOrNull(90_000L) {
        withContext(Dispatchers.IO) {
            OfflineAnalysisBudget.mutex.withLock {
                try {
                    decode(file, search, reference)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    // An unreadable clip or an unsupported analysis decoder must not change a track.
                    null
                }
            }
        }
    }

    private suspend fun decode(
        file: MediaFile,
        search: StackPictureSearch,
        reference: List<StackPictureWindow>?,
    ): Samples? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val frames = ArrayList<StackPictureFrame>()
        val clipStartUs = file.clipStartMs * 1_000L
        try {
            extractor.openMedia(context, file.sourcePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: return null
            val format = extractor.getTrackFormat(track)
            val trackDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }?.minus(clipStartUs)
            } else null
            val durationUs = minOf(trackDurationUs
                ?: file.durationMs.takeIf { it > 0 }?.times(1_000L) ?: Long.MAX_VALUE,
                file.clipEndMs?.let { it * 1_000L - clipStartUs } ?: Long.MAX_VALUE)
            if (search == StackPictureSearch.QUICK && reference == null && durationUs == Long.MAX_VALUE) return null
            val windows = if (reference == null) stackPictureWindows(durationUs, search)
                else stackPictureCompanionWindows(reference, durationUs, search)
            if (windows.isEmpty()) return null
            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            val rotation = if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else 0
            format.setInteger(MediaFormat.KEY_ROTATION, 0)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            extractor.selectTrack(track)
            val decoder = MediaCodec.createDecoderByType(mime).also { codec = it }
            decoder.configure(format, null, null, 0)
            decoder.start()
            val info = MediaCodec.BufferInfo()
            val luma = FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID)
            for ((index, window) in windows.withIndex()) {
                currentCoroutineContext().ensureActive()
                val start = clipStartUs + window.startUs
                if (index > 0) decoder.flush() // Synchronous codec; resume with a keyframe.
                if (start > 0) extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                decodeWindow(extractor, decoder, info, luma, start, clipStartUs + window.endUs,
                    clipStartUs, rotation, frames)
            }
            return Samples(windows, frames)
        } finally {
            try { codec?.release() } finally { extractor.release() }
        }
    }

    private suspend fun decodeWindow(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        info: MediaCodec.BufferInfo,
        luma: FloatArray,
        start: Long,
        end: Long,
        clipStartUs: Long,
        rotation: Int,
        frames: MutableList<StackPictureFrame>,
    ) {
        var inputDone = false
        var outputDone = false
        var lastBucket = -1L
        while (!outputDone) {
            currentCoroutineContext().ensureActive()
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
                val bucket = (time - clipStartUs) * 30L / 1_000_000L
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
                    stackPictureFrame(time - clipStartUs, luma)?.let(frames::add)
                }
            } finally {
                decoder.releaseOutputBuffer(index, false)
            }
        }
    }
}
