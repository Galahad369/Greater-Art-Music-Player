package com.local.listentomusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.AtomicFile
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.findMatchingLrc
import com.local.listentomusic.model.loadLocalLyrics
import com.local.listentomusic.model.searchText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Optional search index. Cached tags are reused until the source or sidecar changes. No history. */
class MetadataIndex(private val context: Context) {
    private val storage = AtomicFile(File(context.cacheDir, "library-metadata-v1.json"))
    suspend fun enrich(files: List<MediaFile>, onBatch: suspend (List<MediaFile>) -> Unit) = withContext(Dispatchers.IO) {
        val old = runCatching {
            if (storage.baseFile.length() > 16_000_000) JSONObject() else JSONObject(storage.openRead().bufferedReader().use { it.readText() })
        }.getOrElse { JSONObject() }
        val next = JSONObject()
        val results = ArrayList<MediaFile>(files.size)
        files.forEachIndexed { index, file ->
            currentCoroutineContext().ensureActive()
            val sidecar = findMatchingLrc(file.sourcePath)
            val key = "${file.sourcePath}:${file.sizeBytes}:${file.modifiedMs}:${sidecar?.lastModified()}:${sidecar?.length()}"
            val tags = next.optJSONObject(key) ?: old.optJSONObject(key) ?: readTags(file)
            if (index < 10_000) next.put(key, tags)
            results += file.copy(artist = file.artist.ifBlank { tags.optString("artist") }, album = file.album.ifBlank { tags.optString("album") },
                searchExtras = searchText(listOf(tags.optString("title"), tags.optString("artist"), tags.optString("album"), tags.optString("lyrics")).joinToString(" ")))
            if (index % 40 == 39) onBatch(results.toList() + files.drop(index + 1))
        }
        onBatch(results)
        currentCoroutineContext().ensureActive()
        runCatching {
            val output = storage.startWrite()
            try { output.write(next.toString().toByteArray()); storage.finishWrite(output) }
            catch (error: Exception) { storage.failWrite(output); throw error }
        }
    }
    private fun readTags(file: MediaFile): JSONObject {
        val json = JSONObject()
        val reader = MediaMetadataRetriever()
        try {
            reader.openMedia(context, file.sourcePath)
            json.put("title", reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty())
            json.put("artist", reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty())
            json.put("album", reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty())
        } catch (_: Exception) { /* An unreadable tag must never hide a playable file. */ }
        finally { runCatching { reader.release() } }
        json.put("lyrics", runCatching { loadLocalLyrics(file.sourcePath)?.lines?.joinToString(" ") { it.text }?.take(16_384) }.getOrNull().orEmpty())
        return json
    }
}
