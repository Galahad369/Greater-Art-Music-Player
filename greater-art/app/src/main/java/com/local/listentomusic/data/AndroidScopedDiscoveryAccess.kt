package com.local.listentomusic.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lists metadata only. No decoding, raw-path conversion, writes or new permission requests. */
internal class AndroidScopedDiscoveryAccess(private val resolver: ContentResolver) : ScopedDiscoveryAccess {
    override fun treeRoot(tree: String): String {
        val uri = Uri.parse(tree)
        require(uri.scheme == "content" && !uri.authority.isNullOrBlank() && DocumentsContract.isTreeUri(uri))
        return DocumentsContract.getTreeDocumentId(uri)
    }

    override fun hasReadGrant(tree: String): Boolean = resolver.persistedUriPermissions.any {
        it.uri.toString() == tree && it.isReadPermission
    }

    override suspend fun children(tree: String, documentId: String, limit: Int): ScopedListing {
        treeRoot(tree)
        if (!hasReadGrant(tree)) throw SecurityException("Folder read grant missing")
        val root = Uri.parse(tree)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        return query(DocumentsContract.buildChildDocumentsUriUsingTree(root, documentId), columns, null, null, limit) {
            val id = string(DocumentsContract.Document.COLUMN_DOCUMENT_ID) ?: return@query null
            val mime = string(DocumentsContract.Document.COLUMN_MIME_TYPE)
            ScopedRow(id, DocumentsContract.buildDocumentUri(root.authority!!, id).toString(),
                DocumentsContract.buildDocumentUriUsingTree(root, id).toString(),
                string(DocumentsContract.Document.COLUMN_DISPLAY_NAME).orEmpty(), mime,
                directory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                size = number(DocumentsContract.Document.COLUMN_SIZE),
                modifiedMs = number(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
        }
    }

    override suspend fun mediaStore(limit: Int): ScopedListing {
        val rows = mutableListOf<ScopedRow>()
        var truncated = false
        var inspected = 0
        val problems = mutableSetOf<DiscoveryProblem>()
        val collections = listOf(
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
        val columns = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.Video.Media.DURATION)
        // Legacy DATA is used only as a query predicate, never as identity or playable location.
        val selection = if (Build.VERSION.SDK_INT >= 29)
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.IS_PENDING}=0" +
                if (Build.VERSION.SDK_INT >= 30) " AND ${MediaStore.MediaColumns.IS_TRASHED}=0" else ""
        else "${MediaStore.MediaColumns.DATA} LIKE ?"
        val arguments = arrayOf(if (Build.VERSION.SDK_INT >= 29) "Download/%" else "%/Download/%")
        for (collection in collections) {
            currentCoroutineContext().ensureActive()
            if (inspected >= limit) { truncated = true; break }
            val listing = try { query(collection, columns, selection, arguments, limit - inspected) {
                val id = number(MediaStore.MediaColumns._ID)
                if (id <= 0) return@query null
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val seconds = number(MediaStore.MediaColumns.DATE_MODIFIED).coerceIn(0, Long.MAX_VALUE / 1000)
                ScopedRow(id.toString(), uri, uri, string(MediaStore.MediaColumns.DISPLAY_NAME).orEmpty(),
                    string(MediaStore.MediaColumns.MIME_TYPE), size = number(MediaStore.MediaColumns.SIZE),
                    modifiedMs = seconds * 1000, durationMs = number(MediaStore.Video.Media.DURATION))
            } } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { problems += DiscoveryProblem.PERMISSION_DENIED; continue }
            catch (_: Exception) { problems += DiscoveryProblem.PROVIDER_UNAVAILABLE; continue }
            rows += listing.rows
            inspected += listing.inspected
            truncated = truncated || listing.truncated
        }
        return ScopedListing(rows, truncated, inspected, problems)
    }

    private suspend fun query(uri: Uri, projection: Array<String>, selection: String?, args: Array<String>?,
        limit: Int, row: Cursor.() -> ScopedRow?): ScopedListing = withContext(Dispatchers.IO) {
        require(limit > 0)
        val context = currentCoroutineContext()
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val result = resolver.query(uri, projection, selection, args, null, signal)?.use { cursor ->
                    val rows = mutableListOf<ScopedRow>()
                    var inspected = 0
                    var truncated = false
                    while (cursor.moveToNext()) {
                        context.ensureActive()
                        if (inspected >= limit) { truncated = true; break }
                        inspected++
                        cursor.row()?.let(rows::add)
                    }
                    ScopedListing(rows, truncated, inspected)
                } ?: throw IllegalStateException("Provider returned no listing")
                continuation.resume(result)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }

    private fun Cursor.string(column: String): String? = getColumnIndex(column).let {
        if (it < 0 || isNull(it)) null else getString(it)
    }
    private fun Cursor.number(column: String): Long = getColumnIndex(column).let {
        if (it < 0 || isNull(it)) 0 else getLong(it)
    }
}
