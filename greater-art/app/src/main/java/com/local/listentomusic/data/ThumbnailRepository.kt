package com.local.listentomusic.data

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.scale
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.coroutines.coroutineContext
import com.local.listentomusic.ui.ListScrollBudget
import com.local.listentomusic.playback.StackPlayback

data class ThumbnailStats(
    val memoryHits: Int = 0,
    val diskHits: Int = 0,
    val generated: Int = 0,
    val failed: Int = 0,
    val missingArtwork: Int = 0,
    val inFlight: Int = 0,
    val queued: Int = 0,
    val sharedRequests: Int = 0,
    val cancelled: Int = 0,
    val rejected: Int = 0,
    val peakPending: Int = 0,
    val memoryKb: Int = 0,
    val memoryBudgetKb: Int = 0,
    val peakMemoryKb: Int = 0,
    val artworkReuses: Int = 0,
)

internal fun thumbnailDigestHex(bytes: ByteArray): String {
    val chars = CharArray(bytes.size * 2)
    for (i in bytes.indices) {
        val value = bytes[i].toInt() and 0xFF
        chars[i * 2] = THUMBNAIL_HEX_DIGITS[value ushr 4]
        chars[i * 2 + 1] = THUMBNAIL_HEX_DIGITS[value and 0x0F]
    }
    return String(chars)
}

private const val THUMBNAIL_HEX_DIGITS = "0123456789abcdef"
private const val THUMBNAIL_CONSTRAINED_HEAP_BYTES = 384L * 1024L * 1024L

internal data class ThumbnailWorkerPolicy(
    val generationPermits: Int,
    val diskDecodePermits: Int,
)

internal fun thumbnailWorkerPolicy(lowRamDevice: Boolean, maxHeapBytes: Long): ThumbnailWorkerPolicy {
    val constrained = lowRamDevice || maxHeapBytes in 1 until THUMBNAIL_CONSTRAINED_HEAP_BYTES
    return if (constrained) ThumbnailWorkerPolicy(generationPermits = 1, diskDecodePermits = 2)
    else ThumbnailWorkerPolicy(generationPermits = 2, diskDecodePermits = 3)
}

internal fun thumbnailMemoryBudgetKb(maxHeapBytes: Long): Int =
    (maxHeapBytes / 12L / 1024L).coerceIn(8_192L, 65_536L).toInt()

internal fun thumbnailPlaybackBudgetKb(maxHeapBytes: Long, lowRam: Boolean, video: Boolean, stackTracks: Int): Int {
    val base = thumbnailMemoryBudgetKb(maxHeapBytes)
    val divisor = when {
        stackTracks >= 4 -> 4
        stackTracks >= 2 || lowRam -> 2
        video -> 2
        else -> 1
    }
    // The floor must never consume an unreasonable fraction of a tiny heap.
    return (base / divisor).coerceAtLeast(2_048)
        .coerceAtMost((maxHeapBytes / 8 / 1024).coerceAtLeast(1).toInt())
}

internal fun thumbnailSampleSize(width: Int, height: Int, target: Int): Int {
    if (width <= 0 || height <= 0 || target <= 0) return 1
    var sample = 1
    while (maxOf(width, height) / sample > target * 2L && sample < (1 shl 29)) sample *= 2
    return sample
}

internal fun thumbnailTrimTargetKb(maxSizeKb: Int, level: Int): Int? = when {
    maxSizeKb <= 0 -> 0
    level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> 0
    level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> (maxSizeKb / 2).coerceAtLeast(1)
    level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> (maxSizeKb / 4).coerceAtLeast(1)
    level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> (maxSizeKb / 2).coerceAtLeast(1)
    else -> null
}

internal fun shouldPersistMissingArtwork(
    kind: MediaKind,
    coverUri: String,
    embeddedProbeSucceeded: Boolean,
    siblingArtworkFound: Boolean,
): Boolean =
    kind == MediaKind.AUDIO &&
        coverUri.isBlank() &&
        embeddedProbeSucceeded &&
        !siblingArtworkFound

internal fun thumbnailMissingMarkerName(key: String): String = "$key.missing"

/**
 * Local-only thumbnail pipeline:
 * 1. memory LRU, 2. persistent disk cache, 3. Android system thumbnail API,
 * 4. MediaMetadataRetriever fallback. No network image loader is involved.
 */
class ThumbnailRepository(private val context: Context) {
    private val cacheDirectory = File(context.cacheDir, "media_thumbnails").apply { mkdirs() }
    private val maxHeapBytes = Runtime.getRuntime().maxMemory()
    private val lowRamDevice = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    private val workerPolicy = thumbnailWorkerPolicy(lowRamDevice, maxHeapBytes)
    // Heavy frame/artwork extraction is deliberately single-flight on constrained devices.
    private val decodeWorkers = kotlinx.coroutines.sync.Semaphore(workerPolicy.generationPermits)
    // Disk bitmap decodes still allocate bitmap memory, so reduce fan-out under the same policy.
    private val diskWorkers = kotlinx.coroutines.sync.Semaphore(workerPolicy.diskDecodePermits)
    private val pruneScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pruning = java.util.concurrent.atomic.AtomicBoolean(false)
    private data class ArtStamp(val value: Long, val atMs: Long)
    private val artStamps = ConcurrentHashMap<String, ArtStamp>()
    private val locks = Array(64) { Mutex() }
    private val pruned = java.util.concurrent.atomic.AtomicBoolean(false)
    private val recentFailures = ConcurrentHashMap<String, Long>()
    private val missingWrites = java.util.concurrent.atomic.AtomicInteger()
    private val generation = java.util.concurrent.atomic.AtomicInteger()
    private data class MemoryAlias(val key: String, val atMs: Long)
    private val memoryAliases = object : LinkedHashMap<String, MemoryAlias>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoryAlias>?) = size > 4_000
    }
    private val artworkLocks = Array(32) { Mutex() }
    @Volatile private var videoPlayback = false
    @Volatile private var stackTracks = 0
    private val prefetchJobs = mutableMapOf<String, Job>()

    private sealed class GenerationResult {
        class Ready(val bitmap: Bitmap, val artworkKey: String? = null) : GenerationResult()
        object MissingArtwork : GenerationResult()
        object Failed : GenerationResult()
    }
    private val _stats = MutableStateFlow(ThumbnailStats())
    val stats: StateFlow<ThumbnailStats> = _stats.asStateFlow()
    private val memoryCache = object : LruCache<String, Bitmap>(thumbnailMemoryBudgetKb(maxHeapBytes)) {
        override fun sizeOf(key: String, value: Bitmap): Int = max(1, (value.allocationByteCount + 1023) / 1024)
    }
    private val scheduler = ThumbnailRequestScheduler<String, Bitmap>(
        pruneScope, workers = workerPolicy.diskDecodePermits,
    ) { queue -> _stats.update { it.copy(
        queued = queue.queued, sharedRequests = queue.shared, cancelled = queue.cancelled,
        rejected = queue.rejected, peakPending = queue.peakPending,
    ) } }

    init {
        updateMemoryBudget()
        scheduler.setPaused(ListScrollBudget.scrolling.value)
        pruneScope.launch { ListScrollBudget.scrolling.collect { scheduler.setPaused(it) } }
        pruneScope.launch {
            StackPlayback.state.map { if (it.active) it.slots.size else 0 }.distinctUntilChanged().collect {
                stackTracks = it
                updateMemoryBudget()
            }
        }
    }

    fun setVideoPlayback(active: Boolean) {
        if (videoPlayback != active) { videoPlayback = active; updateMemoryBudget() }
    }

    private fun updateMemoryBudget() {
        memoryCache.resize(thumbnailPlaybackBudgetKb(maxHeapBytes, lowRamDevice, videoPlayback, stackTracks))
        publishMemory()
    }

    private fun publishMemory() {
        val size = memoryCache.size()
        _stats.update { it.copy(memoryKb = size, memoryBudgetKb = memoryCache.maxSize(), peakMemoryKb = maxOf(it.peakMemoryKb, size)) }
    }

    private fun rememberBitmap(id: String, key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
        synchronized(memoryAliases) { memoryAliases[id] = MemoryAlias(key, System.currentTimeMillis()) }
        publishMemory()
    }

    /** No filesystem calls on the hot RAM path, including while a list is flinging. */
    private fun memoryHit(id: String): Bitmap? {
        val alias = synchronized(memoryAliases) { memoryAliases[id] }
        if (alias == null || System.currentTimeMillis() - alias.atMs >= ART_STAMP_TTL_MS) return null
        return memoryCache.get(alias.key)?.also { _stats.update { it.copy(memoryHits = it.memoryHits + 1) } }
    }

    private fun requestId(file: MediaFile) = "${file.sourcePath}|${file.sizeBytes}|${file.modifiedMs}|${file.coverUri}"

    /** Visible rows win; idle prefetch reads at most two adjacent disk entries, never extracts frames. */
    fun setViewport(holder: String, visible: List<MediaFile>, adjacent: List<MediaFile>, scrolling: Boolean) {
        scheduler.setViewport(holder, visible.mapTo(mutableSetOf(), ::requestId))
        synchronized(prefetchJobs) {
            prefetchJobs.remove(holder)?.cancel()
            if (!scrolling && adjacent.isNotEmpty()) prefetchJobs[holder] = pruneScope.launch {
                try {
                    delay(160)
                    for (file in adjacent.take(2)) {
                        if (ListScrollBudget.scrolling.value) break
                        val id = requestId(file)
                        if (memoryHit(id) == null) scheduler.load("prefetch:$id", prefetch = true) {
                            loadUnscheduled(file, id, diskOnly = true)
                        }
                    }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { _stats.update { it.copy(failed = it.failed + 1) } }
            }
        }
    }

    /**
     * Drop only process-memory thumbnails when Android reports pressure.
     * The persistent disk cache is intentionally retained so returning rows can reload
     * cheaply instead of re-running MediaMetadataRetriever / video frame extraction.
     */
    fun trimMemory(level: Int) {
        val targetKb = thumbnailTrimTargetKb(memoryCache.maxSize(), level) ?: return
        if (targetKb <= 0) memoryCache.evictAll() else memoryCache.trimToSize(targetKb)
        publishMemory()
    }

    suspend fun load(file: MediaFile): Bitmap? {
        val id = requestId(file)
        memoryHit(id)?.let { return it }
        return try { scheduler.load(id) { loadUnscheduled(file, id) } }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { _stats.update { it.copy(failed = it.failed + 1) }; null }
    }

    private suspend fun checkpoint() {
        coroutineContext.ensureActive()
        // Closing the small collector race also protects a task waiting for a resource permit.
        if (ListScrollBudget.scrolling.value) throw CancellationException("Thumbnail deferred for scrolling")
    }

    private suspend fun loadUnscheduled(file: MediaFile, id: String, diskOnly: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        checkpoint()
        val epoch = generation.get()
        if (pruned.compareAndSet(false, true)) pruneScope.launch { pruneDiskCache() }
        val key = cacheKey(file)
        memoryCache.get(key)?.let {
            rememberBitmap(id, key, it)
            _stats.update { value -> value.copy(memoryHits = value.memoryHits + 1) }
            return@withContext it
        }
        _stats.update { it.copy(inFlight = it.inFlight + 1) }

        val mutex = locks[(key.hashCode() and Int.MAX_VALUE) % locks.size]
        try {
            mutex.withLock {
                memoryCache.get(key)?.let { rememberBitmap(id, key, it); return@withLock it }
                val artifactKey = resolveArtworkKey(key)
                diskWorkers.withPermit { checkpoint(); readDisk(artifactKey) }?.let {
                    checkpoint()
                    if (generation.get() == epoch) rememberBitmap(id, artifactKey, it)
                    _stats.update { value -> value.copy(diskHits = value.diskHits + 1) }
                    return@withLock it
                }
                if (diskOnly) return@withLock null
                if (readMissing(key)) {
                    _stats.update { value -> value.copy(missingArtwork = value.missingArtwork + 1) }
                    return@withLock null
                }
                if (System.currentTimeMillis() - (recentFailures[key] ?: 0L) < FAILURE_RETRY_MS) return@withLock null

                when (val generated = decodeWorkers.withPermit {
                    OfflineAnalysisBudget.mutex.withLock { checkpoint(); generateGuarded(file, epoch) }
                }.also { checkpoint() }) {
                    is GenerationResult.Ready -> {
                        if (generation.get() == epoch) {
                            val artifact = generated.artworkKey ?: key
                            rememberBitmap(id, artifact, generated.bitmap)
                            if (generated.artworkKey != null) writeArtworkReference(key, artifact)
                            else writeDisk(key, generated.bitmap)
                        }
                        recentFailures.remove(key)
                        _stats.update { value -> value.copy(generated = value.generated + 1) }
                        if (_stats.value.generated % 32 == 0) pruneScope.launch { pruneDiskCache() }
                        generated.bitmap
                    }
                    GenerationResult.MissingArtwork -> {
                        if (generation.get() == epoch) writeMissing(key)
                        recentFailures.remove(key)
                        _stats.update { value -> value.copy(missingArtwork = value.missingArtwork + 1) }
                        null
                    }
                    GenerationResult.Failed -> {
                        recentFailures[key] = System.currentTimeMillis()
                        if (recentFailures.size > 600) recentFailures.clear()
                        _stats.update { value -> value.copy(failed = value.failed + 1) }
                        null
                    }
                }
            }
        } finally {
            _stats.update { it.copy(inFlight = (it.inFlight - 1).coerceAtLeast(0)) }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        generation.incrementAndGet()
        scheduler.clear()
        synchronized(prefetchJobs) { prefetchJobs.values.forEach { it.cancel() }; prefetchJobs.clear() }
        // Wait for writers before deleting. A finished decode must not refill a
        // just-cleared cache or race a partially written file into the next read.
        withThumbnailWriterLocks(locks) {
            memoryCache.evictAll()
            synchronized(memoryAliases) { memoryAliases.clear() }
            recentFailures.clear()
            missingWrites.set(0)
            artStamps.clear()
            cacheDirectory.listFiles()?.forEach { it.delete() }
            pruned.set(false)
            publishMemory()
        }
        Unit
    }

    /** A pathological embedded cover must not take the whole player down. */
    private suspend fun generateGuarded(file: MediaFile, epoch: Int): GenerationResult = try {
        generate(file, epoch)
    } catch (_: OutOfMemoryError) {
        // Release our own retained bitmap budget before giving up this request.
        memoryCache.evictAll()
        publishMemory()
        GenerationResult.Failed
    }

    private suspend fun generate(file: MediaFile, epoch: Int): GenerationResult {
        customArtwork(file.coverUri)?.let { return GenerationResult.Ready(it) }
        checkpoint()
        return when (file.kind) {
            MediaKind.VIDEO -> {
                createVideoThumbnail(file)?.let { return GenerationResult.Ready(it) }
                checkpoint()
                createIndexedVideoThumbnail(file)?.let { return GenerationResult.Ready(it) }
                checkpoint()
                createEmbeddedArtwork(file, epoch) ?: createSiblingArtwork(file, epoch) ?: GenerationResult.Failed
            }
            MediaKind.AUDIO -> generateAudioArtwork(file, epoch)
        }
    }

    private suspend fun generateAudioArtwork(file: MediaFile, epoch: Int): GenerationResult {
        var embeddedProbeSucceeded = false
        var embeddedDecodeFailed = false
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.sourcePath)
            embeddedProbeSucceeded = true
            retriever.embeddedPicture?.let { bytes ->
                val decoded = sharedArtwork(bytes, epoch)
                if (decoded != null) return decoded
                embeddedDecodeFailed = true
            }
        } catch (cancel: CancellationException) { throw cancel
        } catch (_: Exception) {
            embeddedProbeSucceeded = false
        } finally {
            runCatching { retriever.release() }
        }

        val sibling = findSiblingArtwork(File(file.sourcePath))
        if (sibling != null) {
            return sharedArtworkFile(sibling, epoch) ?: GenerationResult.Failed
        }

        return if (
            !embeddedDecodeFailed &&
            shouldPersistMissingArtwork(
                kind = file.kind,
                coverUri = file.coverUri,
                embeddedProbeSucceeded = embeddedProbeSucceeded,
                siblingArtworkFound = false,
            )
        ) {
            GenerationResult.MissingArtwork
        } else {
            GenerationResult.Failed
        }
    }

    private fun customArtwork(uri: String): Bitmap? = if (uri.isBlank()) null else runCatching {
        if (Build.VERSION.SDK_INT < 28) {
            val parsed = android.net.Uri.parse(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
            options.inJustDecodeBounds = false
            options.inSampleSize = 1
            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 768) options.inSampleSize *= 2
            return@runCatching context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, android.net.Uri.parse(uri))
        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val ratio = 384f / maxOf(info.size.width, info.size.height).coerceAtLeast(1)
            decoder.setTargetSize((info.size.width * ratio.coerceAtMost(1f)).toInt().coerceAtLeast(1),
                (info.size.height * ratio.coerceAtMost(1f)).toInt().coerceAtLeast(1))
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()

    private suspend fun createVideoThumbnail(media: MediaFile): Bitmap? {
        val source = File(media.sourcePath)
        val systemThumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cancellableNativeThumbnail { signal ->
                ThumbnailUtils.createVideoThumbnail(source, Size(VIDEO_WIDTH, VIDEO_HEIGHT), signal)
            }
        } else {
            null
        }
        if (systemThumbnail != null) return centerCrop(systemThumbnail, VIDEO_WIDTH, VIDEO_HEIGHT)
        checkpoint()

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(media.sourcePath)
            val frame = (if (Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, VIDEO_WIDTH, VIDEO_HEIGHT)
            } else retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC))
                ?: (if (Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, VIDEO_WIDTH, VIDEO_HEIGHT)
                    else retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC))
                ?: return null
            centerCrop(frame, VIDEO_WIDTH, VIDEO_HEIGHT)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private suspend fun createEmbeddedArtwork(media: MediaFile, epoch: Int): GenerationResult.Ready? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(media.sourcePath)
            val bytes = retriever.embeddedPicture ?: return null
            sharedArtwork(bytes, epoch)
        } catch (cancel: CancellationException) { throw cancel
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Reuse Android's indexed cover if direct container extraction was unavailable. */
    private suspend fun createIndexedVideoThumbnail(media: MediaFile): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) return null
        return cancellableNativeThumbnail { signal ->
            val collection = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(collection, arrayOf(android.provider.MediaStore.MediaColumns._ID),
                "${android.provider.MediaStore.MediaColumns.DATA} = ?", arrayOf(media.sourcePath), null, signal)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                context.contentResolver.loadThumbnail(uri, Size(VIDEO_WIDTH, VIDEO_HEIGHT), signal)
            }
        }
    }

    private suspend fun cancellableNativeThumbnail(call: (android.os.CancellationSignal) -> Bitmap?): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            val signal = android.os.CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            val bitmap = try { call(signal) } catch (_: Exception) { null }
            if (continuation.isActive) continuation.resumeWith(Result.success(bitmap))
            else bitmap?.recycle()
        }

    /** Same-name cover first, then conventional folder artwork. Entirely local. */
    private suspend fun createSiblingArtwork(media: MediaFile, epoch: Int): GenerationResult.Ready? {
        val artwork = findSiblingArtwork(File(media.sourcePath)) ?: return null
        return sharedArtworkFile(artwork, epoch)
    }

    private suspend fun sharedArtworkFile(file: File, epoch: Int): GenerationResult.Ready? {
        // Hash a stream, not a second full-resolution cover allocation.
        val length = file.length()
        val modified = file.lastModified()
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8_192)
            while (true) {
                checkpoint()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        if (length != file.length() || modified != file.lastModified()) return null
        return sharedArtworkKey("art-${thumbnailDigestHex(digest.digest())}", epoch) {
            val decoded = decodeSampledFile(file, ARTWORK_SIZE, ARTWORK_SIZE)
            if (length == file.length() && modified == file.lastModified()) decoded
            else { decoded?.recycle(); null }
        }
    }

    private suspend fun sharedArtwork(bytes: ByteArray, epoch: Int): GenerationResult.Ready? {
        checkpoint()
        val key = "art-${thumbnailDigestHex(MessageDigest.getInstance("SHA-256").digest(bytes))}"
        return sharedArtworkKey(key, epoch) { decodeSampled(bytes, ARTWORK_SIZE, ARTWORK_SIZE) }
    }

    private suspend fun sharedArtworkKey(key: String, epoch: Int, decode: () -> Bitmap?): GenerationResult.Ready? =
        artworkLocks[(key.hashCode() and Int.MAX_VALUE) % artworkLocks.size].withLock {
            checkpoint()
            val existing = memoryCache.get(key) ?: readDisk(key)
            if (existing != null) {
                _stats.update { it.copy(artworkReuses = it.artworkReuses + 1) }
                return@withLock GenerationResult.Ready(existing, key)
            }
            val bitmap = decode() ?: return@withLock null
            checkpoint()
            if (generation.get() == epoch) writeDisk(key, bitmap)
            GenerationResult.Ready(bitmap, key)
        }

    private fun resolveArtworkKey(sourceKey: String): String {
        val reference = File(cacheDirectory, "$sourceKey.ref")
        if (!reference.isFile || reference.length() > 80) return sourceKey
        val key = runCatching { reference.readText(Charsets.US_ASCII) }.getOrNull()
        reference.setLastModified(System.currentTimeMillis())
        return key?.takeIf { ARTWORK_KEY.matches(it) } ?: sourceKey
    }

    private fun writeArtworkReference(sourceKey: String, artifact: String) {
        val atomic = android.util.AtomicFile(File(cacheDirectory, "$sourceKey.ref"))
        var output: FileOutputStream? = null
        try {
            output = atomic.startWrite()
            output.write(artifact.toByteArray(Charsets.US_ASCII))
            atomic.finishWrite(output)
            File(cacheDirectory, thumbnailMissingMarkerName(sourceKey)).delete()
        } catch (_: Exception) { output?.let { atomic.failWrite(it) } }
    }

    private fun findSiblingArtwork(source: File): File? {
        val parent = source.parentFile ?: return null
        val candidates = buildList {
            IMAGE_EXTENSIONS.forEach { ext -> add(File(parent, "${source.nameWithoutExtension}.$ext")) }
            FOLDER_ART_NAMES.forEach { name -> IMAGE_EXTENSIONS.forEach { ext -> add(File(parent, "$name.$ext")) } }
        }
        return candidates.firstOrNull { it.isFile && it.canRead() }
    }

    private fun decodeSampledFile(file: File, width: Int, height: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = thumbnailSampleSize(bounds.outWidth, bounds.outHeight, maxOf(width, height))
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        })
    }

    private fun decodeSampled(bytes: ByteArray, width: Int, height: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = thumbnailSampleSize(bounds.outWidth, bounds.outHeight, maxOf(width, height))
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        if (source.width == width && source.height == height) return source
        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val scaledWidth = max(width, (source.width * scale).toInt())
        val scaledHeight = max(height, (source.height * scale).toInt())
        val scaled = source.scale(scaledWidth, scaledHeight)
        val left = ((scaledWidth - width) / 2).coerceAtLeast(0)
        val top = ((scaledHeight - height) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, left, top, width, height)
        if (scaled !== source && !source.isRecycled) source.recycle()
        if (cropped !== scaled && !scaled.isRecycled) scaled.recycle()
        return cropped
    }

    private fun readDisk(key: String): Bitmap? {
        val cached = File(cacheDirectory, "$key.webp")
        if (!cached.isFile) return null

        var hitMemoryPressure = false
        var decoded = try {
            decodeSampledFile(cached, ARTWORK_SIZE, ARTWORK_SIZE)
        } catch (_: OutOfMemoryError) {
            hitMemoryPressure = true
            // A warm disk hit can still allocate a Bitmap while the LRU is near its
            // limit. Give back retained thumbnails and retry once before treating this
            // request as a miss. The disk file itself is not corrupt.
            memoryCache.evictAll()
            publishMemory()
            null
        }

        if (decoded == null && hitMemoryPressure) {
            decoded = try {
                decodeSampledFile(cached, ARTWORK_SIZE, ARTWORK_SIZE)
            } catch (_: OutOfMemoryError) {
                null
            }
        }

        return decoded?.also {
            cached.setLastModified(System.currentTimeMillis())
        } ?: run {
            // Do not destroy a valid persistent cache entry merely because the process
            // could not allocate its Bitmap under transient memory pressure.
            if (!hitMemoryPressure) cached.delete()
            null
        }
    }

    private fun readMissing(key: String): Boolean {
        val marker = File(cacheDirectory, thumbnailMissingMarkerName(key))
        if (!marker.isFile) return false
        marker.setLastModified(System.currentTimeMillis())
        return true
    }

    private fun writeMissing(key: String) {
        runCatching {
            val marker = File(cacheDirectory, thumbnailMissingMarkerName(key))
            if (!marker.exists()) marker.createNewFile()
            marker.setLastModified(System.currentTimeMillis())
            if (missingWrites.incrementAndGet() % 32 == 0) {
                pruneScope.launch { pruneDiskCache() }
            }
        }
    }

    private fun writeDisk(key: String, bitmap: Bitmap) {
        val destination = File(cacheDirectory, "$key.webp")
        val atomic = android.util.AtomicFile(destination)
        var output: FileOutputStream? = null
        try {
            output = atomic.startWrite()
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.PNG
                }
                check(bitmap.compress(format, 92, output))
            atomic.finishWrite(output)
            File(cacheDirectory, thumbnailMissingMarkerName(key)).delete()
        } catch (_: Exception) { output?.let { atomic.failWrite(it) } }
    }

    private fun cacheKey(file: MediaFile): String {
        val source = File(file.sourcePath)
        val artStamp = siblingArtStamp(source)
        val fingerprint = "${file.sourcePath}|${source.length()}|${source.lastModified()}|$artStamp|${file.coverUri}"
        return thumbnailDigestHex(
            MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray(Charsets.UTF_8)),
        )
    }

    /**
     * cacheKey() is evaluated before the memory lookup, so probing every possible sibling
     * artwork filename on every row defeats the cheap-memory-hit path. Cache only the
     * derived stamp for a short interval; cache clear/restart always forces a fresh probe.
     */
    private fun siblingArtStamp(source: File): Long {
        val now = System.currentTimeMillis()
        artStamps[source.path]?.let { cached ->
            if (now - cached.atMs < ART_STAMP_TTL_MS) return cached.value
        }
        val stamp = findSiblingArtwork(source)?.lastModified() ?: 0L
        if (artStamps.size > ART_STAMP_CACHE_LIMIT) artStamps.clear()
        artStamps[source.path] = ArtStamp(stamp, now)
        return stamp
    }

    private fun pruneDiskCache() {
        if (!pruning.compareAndSet(false, true)) return
        try {
            runCatching {
                // Snapshot mtime once per file; sortedByDescending(selector) otherwise stats the
                // same FUSE-backed files repeatedly during comparison.
                val files = cacheDirectory.listFiles()
                    ?.filter { it.isFile && it.extension in setOf("webp", "missing", "ref") }
                    .orEmpty()
                    .map { file -> Triple(file, file.lastModified(), file.length()) }
                    .sortedByDescending { it.second }

                var retainedBytes = 0L
                files.forEachIndexed { index, entry ->
                    retainedBytes += entry.third
                    if (index >= MAX_DISK_FILES || retainedBytes > MAX_DISK_BYTES) {
                        entry.first.delete()
                    }
                }
            }
        } finally {
            pruning.set(false)
        }
    }

    private companion object {
        const val VIDEO_WIDTH = 240
        const val VIDEO_HEIGHT = 135
        const val ARTWORK_SIZE = 256
        const val MAX_DISK_FILES = 6_000
        const val MAX_DISK_BYTES = 256L * 1024L * 1024L
        const val FAILURE_RETRY_MS = 30_000L
        const val ART_STAMP_TTL_MS = 5L * 60L * 1_000L
        const val ART_STAMP_CACHE_LIMIT = 4_000
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")
        val FOLDER_ART_NAMES = setOf("cover", "folder", "front", "album", "artwork")
        val ARTWORK_KEY = Regex("art-[0-9a-f]{64}")
    }
}
