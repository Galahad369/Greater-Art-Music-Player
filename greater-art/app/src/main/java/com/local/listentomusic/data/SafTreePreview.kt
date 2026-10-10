package com.local.listentomusic.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.util.ArrayDeque
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** A bounded SAF-only preview. It never maps provider URIs onto raw File paths. */
internal data class SafTreePreview(
    val mediaCount: Int,
    val folderCount: Int,
    val inspectedDocuments: Int,
    val truncated: Boolean,
)

internal fun safMediaCandidate(name: String, mime: String?): Boolean {
    val type = mime.orEmpty().lowercase(Locale.ROOT)
    if (type.startsWith("audio/") || type.startsWith("video/")) return true
    val suffix = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return suffix in setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma",
        "mp4", "m4v", "mov", "mkv", "webm", "3gp", "avi", "ts", "mpeg", "mpg")
}

/** Reads only documents under the explicit persisted tree grant.
 * This is an inspection stage; existing filesystem-backed Library playback is not redirected. */
internal class SafTreeReader(private val resolver: ContentResolver) {
    suspend fun preview(tree: Uri, maxDocuments: Int = 2000, maxFolders: Int = 128, maxDepth: Int = 24): SafTreePreview {
        require(tree.scheme == ContentResolver.SCHEME_CONTENT && DocumentsContract.isTreeUri(tree))
        require(maxDocuments in 1..10000 && maxFolders in 1..2000 && maxDepth in 1..64)
        if (resolver.persistedUriPermissions.none { it.uri == tree && it.isReadPermission }) {
            throw SecurityException("Selected folder grant is missing or revoked")
        }
        val pending = ArrayDeque<Pair<String, Int>>()
        val seen = HashSet<String>()
        pending.add(DocumentsContract.getTreeDocumentId(tree) to 0)
        var media = 0
        var folders = 0
        var inspected = 0
        var truncated = false
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (pending.isNotEmpty() && !truncated) {
            currentCoroutineContext().ensureActive()
            val (folderId, depth) = pending.removeFirst()
            if (!seen.add(folderId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId)
            val cursor = resolver.query(childrenUri, columns, null, null, null)
                ?: throw IllegalStateException("Folder provider returned no listing")
            cursor.use {
                val id = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val name = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mime = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (inspected >= maxDocuments) { truncated = true; break }
                    inspected++
                    val documentId = it.getString(id) ?: continue
                    val mediaType = it.getString(mime)
                    if (mediaType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        folders++
                        if (depth < maxDepth && folders <= maxFolders) pending.add(documentId to (depth + 1))
                        else truncated = true
                    } else if (safMediaCandidate(it.getString(name).orEmpty(), mediaType)) {
                        media++
                    }
                }
            }
        }
        return SafTreePreview(media, folders, inspected, truncated)
    }
}
