package com.local.listentomusic.playback

import android.content.Context
import androidx.media3.common.MediaItem
import com.local.listentomusic.data.*
import com.local.listentomusic.model.MediaSource
import com.local.listentomusic.model.sourceMediaPath
import java.io.File
import java.net.URI
import kotlinx.coroutines.CancellationException

/** Stable SAF IDs and grant-qualified playback URIs must identify the same document. */
internal fun internalContentIdentityMatches(id: String, location: String): Boolean {
    val source = MediaSource.parse(sourceMediaPath(id)) as? MediaSource.ContentUri ?: return false
    if (MediaSource.parse(location) !is MediaSource.ContentUri) return false
    if (source.value == location) return true // MediaStore or the same stable document URI.
    val expected = parseStableDocumentKey(source.value) ?: return false
    val uri = runCatching { URI(location) }.getOrNull() ?: return false
    if (uri.rawQuery != null) return false
    val parts = uri.rawPath.split('/')
    if (parts.size != 5 || parts[1] != "tree" || parts[3] != "document" ||
        decodeUriComponent(parts[2]).isNullOrEmpty()) return false
    return expected.authority == uri.rawAuthority && expected.documentId == decodeUriComponent(parts[4])
}

/** IO boundary: identity is not a grant; recheck readability on every queue admission. */
internal fun readableInternalMediaItem(context: Context, item: MediaItem): Boolean = try {
    val uri = item.localConfiguration?.uri
    when (val source = MediaSource.parse(sourceMediaPath(item.mediaId))) {
        is MediaSource.LocalFile -> {
            val file = File(source.value)
            uri?.scheme == "file" && uri.path == file.path && MediaScanner.isInsideTarget(file) &&
                file.isFile && file.canRead() && MediaScanner.isSupportedLocalMedia(file)
        }
        is MediaSource.ContentUri -> uri != null && internalContentIdentityMatches(item.mediaId, uri.toString()) &&
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } == true
        null -> false
    }
} catch (cancelled: CancellationException) { throw cancelled }
catch (_: Exception) { false }
