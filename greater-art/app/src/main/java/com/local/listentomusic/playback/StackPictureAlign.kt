package com.local.listentomusic.playback

import android.content.Context
import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
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
import java.io.IOException
import java.nio.ByteBuffer

internal class StackPictureAlign(private val context: Context) {
    data class Result(val matches: Map<String, StackAlignment>, val quickTimedOut: Boolean)

    private class IncompleteLocalDecode(cause: Throwable? = null) : IllegalStateException(cause)

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
                    try {
                        decode(file, search, reference)
                    } catch (_: IncompleteLocalDecode) {
                        decode(file, search, reference, allowLocalAvc = false)
                    }
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
        allowLocalAvc: Boolean = true,
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
            fun startDecoder(decoder: MediaCodec): MediaCodec {
                codec = decoder
                decoder.configure(format, null, null, 0)
                decoder.start()
                return decoder
            }
            // Ranchu's host AVC decoder transfers analysis frames across the VM boundary.
            val localAvc = if (allowLocalAvc && search == StackPictureSearch.QUICK && mime == MediaFormat.MIMETYPE_VIDEO_AVC &&
                Build.HARDWARE == "ranchu" && Build.SUPPORTED_ABIS.firstOrNull() == "x86_64"
            ) {
                try {
                    MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull {
                        it.name == "c2.android.avc.decoder" && it.getCapabilitiesForType(mime).isFormatSupported(format)
                    }?.let { startDecoder(MediaCodec.createByCodecName(it.name)) }
                } catch (failure: Exception) {
                    if (failure !is IOException && failure !is IllegalArgumentException &&
                        failure !is IllegalStateException
                    ) throw failure
                    val failed = codec
                    codec = null
                    failed?.release()
                    null
                }
            } else null
            val decoder = localAvc ?: startDecoder(MediaCodec.createDecoderByType(mime))
            val info = MediaCodec.BufferInfo()
            val luma = FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID)
            val syncProbe = if (localAvc == null) null else ByteBuffer.allocateDirect(512 * 1024)
            try {
                for ((index, window) in windows.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    val start = clipStartUs + window.startUs
                    if (index > 0) decoder.flush() // Synchronous codec; resume with a keyframe.
                    if (start > 0) extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    if (syncProbe != null) seekToLocalAvcIdr(extractor, syncProbe)
                    if (!decodeWindow(extractor, decoder, info, luma, start, clipStartUs + window.endUs,
                            clipStartUs, rotation, frames, batchBuffers = localAvc != null)
                    ) throw IncompleteLocalDecode()
                }
            } catch (failure: MediaCodec.CodecException) {
                if (localAvc == null) throw failure
                throw IncompleteLocalDecode(failure)
            }
            return Samples(windows, frames)
        } finally {
            try { codec?.release() } finally { extractor.release() }
        }
    }

    // The local AVC decoder can drop frames when a container sync sample is
    // a recovery point in an open GOP. Start it at a nearby IDR instead.
    private suspend fun seekToLocalAvcIdr(extractor: MediaExtractor, packet: ByteBuffer) {
        val firstSync = extractor.sampleTime
        var sync = firstSync
        while (sync >= 0 && firstSync - sync <= 5_000_000L) {
            currentCoroutineContext().ensureActive()
            packet.clear()
            val size = try {
                extractor.readSampleData(packet, 0)
            } catch (failure: IllegalArgumentException) {
                throw IncompleteLocalDecode(failure)
            }
            if (size !in 4..packet.capacity()) break
            // MediaExtractor exposes AVC samples with Annex B start codes.
            for (index in 0 until size - 3) {
                if (packet.get(index).toInt() != 0 || packet.get(index + 1).toInt() != 0 ||
                    packet.get(index + 2).toInt() != 1
                ) continue
                when (packet.get(index + 3).toInt() and 31) {
                    5 -> return
                    in 1..4 -> break
                }
            }
            if (sync == 0L) break
            extractor.seekTo(sync - 1, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val earlier = extractor.sampleTime
            if (earlier >= sync) break
            sync = earlier
        }
        throw IncompleteLocalDecode()
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
        batchBuffers: Boolean,
    ): Boolean {
        var inputDone = false
        var outputDone = false
        var lastBucket = -1L
        val submittedTimes = if (batchBuffers) ArrayList<Long>() else null
        val returnedTimes = if (batchBuffers) ArrayList<Long>() else null
        val analysisContext = currentCoroutineContext()
        fun feedInput(waitUs: Long): Boolean {
            analysisContext.ensureActive()
            val index = decoder.dequeueInputBuffer(waitUs)
            if (index < 0) return false
            val time = extractor.sampleTime
            val size = if (time < 0 || time >= end + 250_000L) -1
                else extractor.readSampleData(requireNotNull(decoder.getInputBuffer(index)), 0)
            inputDone = size < 0
            decoder.queueInputBuffer(index, 0, size.coerceAtLeast(0), time.coerceAtLeast(0),
                if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
            if (!inputDone) {
                if (time in start until end) submittedTimes?.add(time)
                extractor.advance()
            }
            return true
        }
        fun drainOutput(waitUs: Long): Boolean {
            analysisContext.ensureActive()
            val index = decoder.dequeueOutputBuffer(info, waitUs)
            if (index < 0) return index != MediaCodec.INFO_TRY_AGAIN_LATER
            try {
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                val time = info.presentationTimeUs
                if (info.size == 0 || time !in start until end) return true
                returnedTimes?.add(time)
                val bucket = (time - clipStartUs) * 30L / 1_000_000L
                if (bucket <= lastBucket) return true
                lastBucket = bucket
                val image = decoder.getOutputImage(index) ?: return true
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
            return true
        }
        while (!outputDone) {
            if (batchBuffers) {
                var progressed = false
                while (!outputDone && drainOutput(0L)) progressed = true
                while (!outputDone && !inputDone && feedInput(0L)) progressed = true
                if (!progressed && !outputDone) drainOutput(1_000L)
            } else {
                if (!inputDone) feedInput(10_000L)
                drainOutput(10_000L)
            }
        }
        // Some AVC sync samples start a GOP that the local decoder only partially returns.
        submittedTimes?.sort()
        return submittedTimes == returnedTimes
    }
}
