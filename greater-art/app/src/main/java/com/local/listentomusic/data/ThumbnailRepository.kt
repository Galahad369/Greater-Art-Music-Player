package com.local.listentomusic.data

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class ThumbnailStats(
    val memoryHits: Int = 0,
    val diskHits: Int = 0,
    val generated: Int = 0,
    val failed: Int = 0,
    val missingArtwork: Int = 0,
    val inFlight: Int = 0,
)

/**
 * Local-only thumbnail pipeline:
 * 1. memory LRU, 2. persistent disk cache, 3. Android system thumbnail API,
 * 4. MediaMetadataRetriever fallback. No network image loader is involved.
 */
class ThumbnailRepository(private val context: Context) {
    private val cacheDirectory = File(context.cacheDir, "media_thumbnails").apply { mkdirs() }
    // Bound expensive frame/artwork decoding even when several visible rows request it.
    private val decodeWorkers = kotlinx.coroutines.sync.Semaphore(2)
    private val locks = Array(64) { Mutex() }
    private val pruned = java.util.concurrent.atomic.AtomicBoolean(false)
    private val recentFailures = ConcurrentHashMap<String, Long>()
    private val _stats = MutableStateFlow(ThumbnailStats())
    val stats: StateFlow<ThumbnailStats> = _stats.asStateFlow()
    private val memoryCache = object : LruCache<String, Bitmap>(memoryBudgetKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = max(1, value.byteCount / 1024)
    }

    suspend fun load(file: MediaFile): Bitmap? = withContext(Dispatchers.IO) {
        if (pruned.compareAndSet(false, true)) pruneDiskCache()
        val key = cacheKey(file)
        memoryCache.get(key)?.let {
            _stats.update { value -> value.copy(memoryHits = value.memoryHits + 1) }
            return@withContext it
        }
        _stats.update { it.copy(inFlight = it.inFlight + 1) }

        val mutex = locks[(key.hashCode() and Int.MAX_VALUE) % locks.size]
        try {
            mutex.withLock {
                memoryCache.get(key)?.let { return@withLock it }
                readDisk(key)?.let {
                    memoryCache.put(key, it)
                    _stats.update { value -> value.copy(diskHits = value.diskHits + 1) }
                    return@withLock it
                }
                if (System.currentTimeMillis() - (recentFailures[key] ?: 0L) < FAILURE_RETRY_MS) return@withLock null

                val generated = decodeWorkers.withPermit { generate(file) } ?: run {
                    recentFailures[key] = System.currentTimeMillis()
                    if (recentFailures.size > 600) recentFailures.clear()
                    _stats.update { value ->
                        if (file.kind == MediaKind.AUDIO && File(file.sourcePath).canRead()) value.copy(missingArtwork = value.missingArtwork + 1)
                        else value.copy(failed = value.failed + 1)
                    }
                    return@withLock null
                }
                memoryCache.put(key, generated)
                writeDisk(key, generated)
                recentFailures.remove(key)
                _stats.update { value -> value.copy(generated = value.generated + 1) }
                generated
            }
        } finally {
            _stats.update { it.copy(inFlight = (it.inFlight - 1).coerceAtLeast(0)) }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        recentFailures.clear()
        cacheDirectory.listFiles()?.forEach { it.delete() }
        pruned.set(false)
        Unit
    }

    private fun generate(file: MediaFile): Bitmap? = customArtwork(file.coverUri) ?: when (file.kind) {
        MediaKind.VIDEO -> createVideoThumbnail(file) ?: createIndexedVideoThumbnail(file) ?: createEmbeddedArtwork(file) ?: createSiblingArtwork(file)
        MediaKind.AUDIO -> createEmbeddedArtwork(file) ?: createSiblingArtwork(file)
    }

    private fun customArtwork(uri: String): Bitmap? = if (uri.isBlank()) null else runCatching {
        if (Build.VERSION.SDK_INT < 28) {
            val parsed = android.net.Uri.parse(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
            options.inJustDecodeBounds = false
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

    private fun createVideoThumbnail(media: MediaFile): Bitmap? {
        val source = File(media.sourcePath)
        val systemThumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                ThumbnailUtils.createVideoThumbnail(source, Size(VIDEO_WIDTH, VIDEO_HEIGHT), null)
            }.getOrNull()
        } else {
            null
        }
        if (systemThumbnail != null) return centerCrop(systemThumbnail, VIDEO_WIDTH, VIDEO_HEIGHT)

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

    private fun createEmbeddedArtwork(media: MediaFile): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(media.sourcePath)
            val bytes = retriever.embeddedPicture ?: return null
            decodeSampled(bytes, ARTWORK_SIZE, ARTWORK_SIZE)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Reuse Android's indexed cover if direct container extraction was unavailable. */
    private fun createIndexedVideoThumbnail(media: MediaFile): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) return null
        return runCatching {
            val collection = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(collection, arrayOf(android.provider.MediaStore.MediaColumns._ID),
                "${android.provider.MediaStore.MediaColumns.DATA} = ?", arrayOf(media.sourcePath), null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                context.contentResolver.loadThumbnail(uri, Size(VIDEO_WIDTH, VIDEO_HEIGHT), null)
            }
        }.getOrNull()
    }

    /** Same-name cover first, then conventional folder artwork. Entirely local. */
    private fun createSiblingArtwork(media: MediaFile): Bitmap? {
        val artwork = findSiblingArtwork(File(media.sourcePath)) ?: return null
        return decodeSampledFile(artwork, ARTWORK_SIZE, ARTWORK_SIZE)
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
        var sample = 1
        while (bounds.outWidth / sample > width * 2 || bounds.outHeight / sample > height * 2) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        })
    }

    private fun decodeSampled(bytes: ByteArray, width: Int, height: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > width * 2 || bounds.outHeight / sample > height * 2) {
            sample *= 2
        }
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
        return BitmapFactory.decodeFile(cached.absolutePath)?.also {
            cached.setLastModified(System.currentTimeMillis())
        } ?: run {
            cached.delete()
            null
        }
    }

    private fun writeDisk(key: String, bitmap: Bitmap) {
        val destination = File(cacheDirectory, "$key.webp")
        val temporary = File(cacheDirectory, "$key.tmp")
        runCatching {
            FileOutputStream(temporary).use { output ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.PNG
                }
                check(bitmap.compress(format, 92, output))
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
        }.onFailure { temporary.delete() }
    }

    private fun cacheKey(file: MediaFile): String {
        val source = File(file.sourcePath)
        val artStamp = findSiblingArtwork(source)?.lastModified() ?: 0L
        val fingerprint = "${file.sourcePath}|${source.length()}|${source.lastModified()}|$artStamp|${file.coverUri}"
        return MessageDigest.getInstance("SHA-256")
            .digest(fingerprint.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun pruneDiskCache() {
        runCatching {
            val files = cacheDirectory.listFiles()?.filter { it.isFile }.orEmpty()
                .sortedByDescending { it.lastModified() }
            var retainedBytes = 0L
            files.forEachIndexed { index, file ->
                retainedBytes += file.length()
                if (index >= MAX_DISK_FILES || retainedBytes > MAX_DISK_BYTES) file.delete()
            }
        }
    }

    private fun memoryBudgetKb(): Int =
        (Runtime.getRuntime().maxMemory() / 12L / 1024L).coerceIn(8_192L, 65_536L).toInt()

    private companion object {
        const val VIDEO_WIDTH = 240
        const val VIDEO_HEIGHT = 135
        const val ARTWORK_SIZE = 256
        const val MAX_DISK_FILES = 600
        const val MAX_DISK_BYTES = 256L * 1024L * 1024L
        const val FAILURE_RETRY_MS = 30_000L
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")
        val FOLDER_ART_NAMES = setOf("cover", "folder", "front", "album", "artwork")
    }
}
