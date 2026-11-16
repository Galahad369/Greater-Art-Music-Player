package com.local.listentomusic.playback

import android.content.Context
import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
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
    data class Result(val matches: Map<String, StackAlignment>, val quickTimedOut: Boolean,
        val failures: Map<String, StackPictureFailure> = emptyMap())

    suspend fun estimateAll(
        primary: MediaFile,
        companions: List<MediaFile>,
        search: StackPictureSearch,
        onProgress: (Int) -> Unit,
    ): Result = withContext(Dispatchers.Default) {
        val result = linkedMapOf<String, StackAlignment>()
        val failures = linkedMapOf<String, StackPictureFailure>()
        val completed = mutableSetOf<String>()
        var referenceFinished = false
        var currentPath = primary.path
        suspend fun analyze() {
            val analysisContext = currentCoroutineContext()
            val referenceRead = readSamples(primary, search)
            if (referenceRead is StackPictureReadResult.Failed) {
                failures[primary.path] = referenceRead.reason
                companions.forEach { failures[it.path] = StackPictureFailure.REFERENCE_UNAVAILABLE }
                return
            }
            val reference = (referenceRead as StackPictureReadResult.Ready).samples
            val anchors = prepareStackPictureReference(reference.frames) { analysisContext.ensureActive() }
            if (anchors == null) {
                failures[primary.path] = StackPictureFailure.INSUFFICIENT_REFERENCE
                companions.forEach { failures[it.path] = StackPictureFailure.REFERENCE_UNAVAILABLE }
                return
            }
            referenceFinished = true
            withContext(Dispatchers.Main.immediate) { onProgress(0) }
            for ((index, file) in companions.withIndex()) {
                analysisContext.ensureActive()
                currentPath = file.path
                when (val read = readSamples(file, search, reference.windows)) {
                    is StackPictureReadResult.Failed -> failures[file.path] = read.reason
                    is StackPictureReadResult.Ready -> estimateStackPictureOffset(anchors, read.samples.frames, search) {
                        analysisContext.ensureActive()
                    }?.let { result[file.path] = it }
                }
                completed += file.path
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
        if (timedOut) {
            if (!referenceFinished && currentPath == primary.path) failures[primary.path] = StackPictureFailure.QUICK_TIME_LIMIT
            // Earlier completed rejections are not relabelled as unprocessed tracks.
            for (file in companions) if (file.path !in completed && file.path !in failures)
                failures[file.path] = StackPictureFailure.QUICK_TIME_LIMIT
        }
        Result(result, timedOut, failures)
    }

    internal suspend fun readSamples(
        file: MediaFile,
        search: StackPictureSearch,
        reference: List<StackPictureWindow>? = null,
        allowLocalAvc: Boolean = true,
    ): StackPictureReadResult {
        val (read, timeout) = boundedStackPictureRead<StackPictureReadResult> { acquired ->
            withContext(Dispatchers.IO) {
                OfflineAnalysisBudget.mutex.withLock {
                    acquired()
                    try {
                        StackPictureReadResult.Ready(decode(file, search, reference, allowLocalAvc))
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: SecurityException) {
                        StackPictureReadResult.Failed(StackPictureFailure.UNREADABLE)
                    } catch (_: UnreadablePictureSource) {
                        StackPictureReadResult.Failed(StackPictureFailure.UNREADABLE)
                    } catch (_: IOException) {
                        StackPictureReadResult.Failed(StackPictureFailure.UNSUPPORTED)
                    } catch (_: UnsupportedPictureSource) {
                        StackPictureReadResult.Failed(StackPictureFailure.UNSUPPORTED)
                    } catch (failure: Exception) {
                        Log.w("GA-StackPictureDecode", "analysis failed type=${failure.javaClass.simpleName}")
                        StackPictureReadResult.Failed(StackPictureFailure.DECODER_ERROR)
                    }
                }
            }
        }
        val result = read ?: StackPictureReadResult.Failed(requireNotNull(timeout))
        if (result is StackPictureReadResult.Failed) Log.i("GA-StackPictureDecode", "readFailure=${result.reason}")
        return result
    }

    private suspend fun decode(
        file: MediaFile,
        search: StackPictureSearch,
        reference: List<StackPictureWindow>?,
        allowLocalAvc: Boolean = true,
    ): StackPictureSamples {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val decoderNames = linkedSetOf<String>()
        val fallbackWindows = linkedSetOf<Int>()
        val clipStartUs = file.clipStartMs * 1_000L
        try {
            try { extractor.openMedia(context, file.sourcePath) }
            catch (failure: IOException) { throw UnreadablePictureSource(failure) }
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: throw UnsupportedPictureSource()
            val format = extractor.getTrackFormat(track)
            val trackDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }?.minus(clipStartUs)
            } else null
            val durationUs = minOf(trackDurationUs
                ?: file.durationMs.takeIf { it > 0 }?.times(1_000L) ?: Long.MAX_VALUE,
                file.clipEndMs?.let { it * 1_000L - clipStartUs } ?: Long.MAX_VALUE)
            if (search == StackPictureSearch.QUICK && reference == null && durationUs == Long.MAX_VALUE) throw UnsupportedPictureSource()
            val windows = if (reference == null) stackPictureWindows(durationUs, search)
                else stackPictureCompanionWindows(reference, durationUs, search)
            if (windows.isEmpty()) throw UnsupportedPictureSource()
            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            val rotation = if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else 0
            format.setInteger(MediaFormat.KEY_ROTATION, 0)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            extractor.selectTrack(track)
            fun startDecoder(decoder: MediaCodec): MediaCodec {
                codec = decoder
                decoder.configure(format, null, null, 0)
                decoder.start()
                decoderNames += decoder.name
                Log.i("GA-StackPictureDecode", "decoder=${decoder.name} search=$search")
                return decoder
            }
            // Ranchu's host AVC decoder transfers analysis frames across the VM boundary.
            val localAvc = if (allowLocalAvc && stackPictureUsesLocalAvc(search, Build.HARDWARE,
                    Build.SUPPORTED_ABIS.firstOrNull(), mime)) {
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
                    Log.i("GA-StackPictureDecode", "local initialization failed type=${failure.javaClass.simpleName}; use platform")
                    null
                }
            } else null
            var decoder = localAvc ?: startDecoder(MediaCodec.createDecoderByType(mime))
            var usingLocal = localAvc != null
            var attempts = 0
            val info = MediaCodec.BufferInfo()
            val luma = FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID)
            val maximumInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0
            var syncProbe = if (localAvc == null) null else ByteBuffer.allocateDirect(stackPictureProbeCapacity(-1, maximumInput))
            val frames = readStackPictureWindows(windows, localAvc != null) { index, window, useLocal ->
                val windowFrames = ArrayList<StackPictureFrame>()
                val began = SystemClock.elapsedRealtime()
                try {
                    currentCoroutineContext().ensureActive()
                    val start = clipStartUs + window.startUs
                    var fresh = false
                    if (!useLocal && usingLocal) {
                        codec = null
                        decoder.release()
                        decoder = startDecoder(MediaCodec.createDecoderByType(mime))
                        usingLocal = false
                        fresh = true
                    }
                    if (attempts > 0 && !fresh) decoder.flush()
                    if (start > 0 || attempts > 0) extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    attempts++
                    if (useLocal) syncProbe = seekToLocalAvcIdr(extractor, requireNotNull(syncProbe))
                    if (!decodeWindow(extractor, decoder, info, luma, start, clipStartUs + window.endUs,
                            clipStartUs, rotation, windowFrames, batchBuffers = useLocal)
                    ) throw IncompleteLocalDecode()
                } catch (failure: MediaCodec.CodecException) {
                    if (!useLocal) throw failure
                    fallbackWindows += index
                    Log.i("GA-StackPictureDecode", "window=$index fallback=codec-error decoder=${decoder.name}")
                    throw IncompleteLocalDecode(failure, "codec-error")
                } catch (failure: IncompleteLocalDecode) {
                    if (useLocal) {
                        fallbackWindows += index
                        Log.i("GA-StackPictureDecode", "window=$index fallback=${failure.reason} decoder=${decoder.name}")
                    }
                    throw failure
                }
                Log.i("GA-StackPictureDecode", "window=$index decoder=${decoder.name} local=$useLocal frames=${windowFrames.size} elapsedMs=${SystemClock.elapsedRealtime() - began}")
                windowFrames
            }
            return StackPictureSamples(windows, frames, decoderNames, fallbackWindows)
        } finally {
            try { codec?.release() } finally { extractor.release() }
        }
    }

    // The local AVC decoder can drop frames when a container sync sample is
    // a recovery point in an open GOP. Start it at a nearby IDR instead.
    private suspend fun seekToLocalAvcIdr(extractor: MediaExtractor, initialPacket: ByteBuffer): ByteBuffer {
        var packet = initialPacket
        val firstSync = extractor.sampleTime
        var sync = firstSync
        while (sync >= 0 && firstSync - sync <= 5_000_000L) {
            currentCoroutineContext().ensureActive()
            val sampleSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) extractor.sampleSize else -1L
            val capacity = stackPictureProbeCapacity(sampleSize, packet.capacity())
            if (capacity > packet.capacity()) packet = ByteBuffer.allocateDirect(capacity)
            packet.clear()
            val size = try {
                extractor.readSampleData(packet, 0)
            } catch (failure: IllegalArgumentException) {
                throw IncompleteLocalDecode(failure, "probe-read")
            }
            if (size !in 4..packet.capacity()) break
            if (stackPictureHasAvcIdr(packet, size)) return packet
            if (sync == 0L) break
            extractor.seekTo(sync - 1, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val earlier = extractor.sampleTime
            if (earlier >= sync) break
            sync = earlier
        }
        throw IncompleteLocalDecode(reason = "idr-not-found")
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
        var usableOutput = true
        val submittedTimes = if (batchBuffers) ArrayList<Long>() else null
        val returnedTimes = if (batchBuffers) ArrayList<Long>() else null
        val analysisContext = currentCoroutineContext()
        val sampler = StackPictureNearestSampler(30, end - clipStartUs) { time, pixels ->
            stackPictureFrame(time, pixels)?.let(frames::add)
        }
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
                val image = decoder.getOutputImage(index) ?: run { usableOutput = false; return true }
                image.use {
                    if (it.format != ImageFormat.YUV_420_888) { usableOutput = false; return@use }
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
                        if (position >= buffer.limit()) { usableOutput = false; return@use }
                        luma[y * STACK_PICTURE_GRID + x] = (buffer.get(position).toInt() and 255).toFloat()
                    }
                    sampler.offer(time - clipStartUs, luma)
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
        sampler.finish()
        // Some AVC sync samples start a GOP that the local decoder only partially returns.
        return usableOutput && (!batchBuffers || stackPictureOutputComplete(requireNotNull(submittedTimes), requireNotNull(returnedTimes)))
    }
}
