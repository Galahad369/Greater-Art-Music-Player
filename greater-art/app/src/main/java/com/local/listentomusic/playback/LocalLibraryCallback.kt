package com.local.listentomusic.playback

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.local.listentomusic.data.*
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Browsers see audio only. Every requested ID resolves through the permitted local index. */
internal const val LIBRARY_SCAN_CACHE_TTL_MS = 15_000L

internal fun shouldReuseLibraryScanCache(
    cachedExcluded: Set<String>?,
    requestedExcluded: Set<String>,
    cachedAtMs: Long,
    nowMs: Long,
): Boolean =
    cachedExcluded == requestedExcluded &&
        cachedAtMs > 0L &&
        nowMs >= cachedAtMs &&
        nowMs - cachedAtMs < LIBRARY_SCAN_CACHE_TTL_MS

class LocalLibraryCallback(private val context: Context, private val scope: CoroutineScope) : MediaLibrarySession.Callback {
    private val preferences = AppPreferences(context.applicationContext)
    private val scanLock = Mutex()
    private var cachedFiles: List<MediaFile> = emptyList()
    private var cachedExcluded: Set<String>? = null
    private var cachedAtMs = 0L

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
        if (controller.packageName != context.packageName && !controller.isTrusted) return MediaSession.ConnectionResult.reject()
        return super.onConnect(session, controller)
    }
    private suspend fun files(): List<MediaFile> {
        val excluded = preferences.current().excludedFolders.toSet()
        return scanLock.withLock {
            val now = SystemClock.elapsedRealtime()
            if (shouldReuseLibraryScanCache(cachedExcluded, excluded, cachedAtMs, now)) {
                return@withLock cachedFiles
            }
            when (val scanned = MediaScanner.scan(excluded)) {
                is ScanResult.Success -> {
                    cachedFiles = scanned.files
                    cachedExcluded = excluded
                    cachedAtMs = now
                    scanned.files
                }
                else -> {
                    cachedFiles = emptyList()
                    cachedExcluded = null
                    cachedAtMs = 0L
                    emptyList()
                }
            }
        }
    }
    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        scope.launch { try { result.set(block()) } catch (error: Exception) { result.setException(error) } }
        return result
    }
    private fun root() = MediaItem.Builder().setMediaId("local-root").setMediaMetadata(MediaMetadata.Builder()
        .setTitle("Greater Art").setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build()).build()
    private fun browseItem(item: com.local.listentomusic.model.MediaFile) = item.toMediaItem().buildUpon()
        .setMediaMetadata(item.toMediaItem().mediaMetadata.buildUpon().setIsBrowsable(false).setIsPlayable(true).build()).build()
    override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?) =
        Futures.immediateFuture(LibraryResult.ofItem(root(), params))
    override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
        val audio = if (parentId == "local-root") files().filter { it.kind == MediaKind.AUDIO }.sortedBy { it.name.lowercase() } else emptyList()
        val offset = (page.toLong() * pageSize).coerceIn(0, audio.size.toLong()).toInt()
        LibraryResult.ofItemList(audio.drop(offset).take(pageSize.coerceIn(0, 500)).map(::browseItem), params)
    }
    override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String): ListenableFuture<LibraryResult<MediaItem>> = future {
        if (mediaId == "local-root") LibraryResult.ofItem(root(), null)
        else files().firstOrNull { it.path == mediaId && it.kind == MediaKind.AUDIO }?.let { LibraryResult.ofItem(browseItem(it), null) }
            ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }
    override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>): ListenableFuture<List<MediaItem>> = future {
        if (controller.packageName == context.packageName) {
            // Internal queues already came from our scanner. Avoid another complete
            // recursive scan on every row tap; validate only the requested paths.
            return@future withContext(Dispatchers.IO) { mediaItems.filter { request ->
                val uri = request.localConfiguration?.uri
                val file = java.io.File(com.local.listentomusic.model.sourceMediaPath(request.mediaId))
                uri?.scheme == "file" && uri.path == file.path && MediaScanner.isInsideTarget(file) && file.isFile && file.extension.lowercase() in MediaScanner.supportedExtensions
            } }
        }
        val allowed = files().filter { controller.packageName == context.packageName || it.kind == MediaKind.AUDIO }
        val byId = allowed.associateBy { it.path }
        mediaItems.flatMap { request ->
            byId[request.mediaId]?.let { listOf(it.toMediaItem()) }
                ?: request.requestMetadata.searchQuery?.takeIf { it.isNotBlank() }?.let { query -> allowed.filter { it.name.contains(query, true) }.take(100).map { it.toMediaItem() } }.orEmpty()
        }
    }
    override fun onSearch(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> = future {
        val count = files().count { it.kind == MediaKind.AUDIO && it.name.contains(query, true) }
        session.notifySearchResultChanged(browser, query, count, params)
        LibraryResult.ofVoid()
    }
    override fun onGetSearchResult(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
        val matches = files().filter { it.kind == MediaKind.AUDIO && it.name.contains(query, true) }
        val offset = (page.toLong() * pageSize).coerceIn(0, matches.size.toLong()).toInt()
        LibraryResult.ofItemList(matches.drop(offset).take(pageSize.coerceIn(0, 500)).map(::browseItem), params)
    }
}
