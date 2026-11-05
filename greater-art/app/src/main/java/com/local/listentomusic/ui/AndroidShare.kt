package com.local.listentomusic.ui

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.exportPortableM3u
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AndroidShare {
    private const val LIST_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1_000L

    private data class ShareFile(val uri: android.net.Uri, val mime: String, val name: String)

    private fun shareFile(context: Context, media: MediaFile): ShareFile = when (val source = media.reference.source) {
        is com.local.listentomusic.model.MediaSource.LocalFile -> {
            val file = File(source.value).canonicalFile
            require(file.isFile && file.canRead() && MediaScanner.isInsideTarget(file))
            ShareFile(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), mimeFor(file), file.name)
        }
        is com.local.listentomusic.model.MediaSource.ContentUri -> {
            val uri = android.net.Uri.parse(source.value)
            ShareFile(uri, mimeFor(File(media.name)), media.name)
        }
    }

    fun mediaChooser(context: Context, media: MediaFile, chooserTitle: String): Result<Intent> = runCatching {
        val file = shareFile(context, media)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.mime
            putExtra(Intent.EXTRA_STREAM, file.uri)
            clipData = android.content.ClipData.newRawUri(file.name, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(intent, chooserTitle)
    }

    fun media(context: Context, media: MediaFile, chooserTitle: String): Result<Unit> =
        mediaChooser(context, media, chooserTitle).mapCatching { chooser ->
            if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }

    fun mediaFiles(context: Context, media: List<MediaFile>, chooserTitle: String): Result<Unit> = runCatching {
        require(media.isNotEmpty())
        val sources = media.map { shareFile(context, it) }.distinctBy { it.uri }
        val uris = ArrayList(sources.map { it.uri })
        val mime = sources.map { it.mime }.let { types ->
            when {
                types.distinct().size == 1 -> types.first()
                types.all { it.startsWith("audio/") } -> "audio/*"
                types.all { it.startsWith("video/") } -> "video/*"
                else -> "*/*"
            }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = android.content.ClipData.newRawUri(sources.first().name, uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    suspend fun listChooser(context: Context, label: String, files: List<MediaFile>, chooserTitle: String): Result<Intent> {
        val exported = withContext(Dispatchers.IO) {
            runCatching {
                require(files.isNotEmpty())
                val folder = File(context.cacheDir, "shared_lists").apply { mkdirs() }
                val cutoff = System.currentTimeMillis() - LIST_CACHE_MAX_AGE_MS
                folder.listFiles()?.filter { it.lastModified() < cutoff }?.forEach(File::delete)
                val safe = label.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().take(60).ifBlank { "Greater-Art-list" }
                File(folder, "$safe-${System.currentTimeMillis()}.m3u8").apply {
                    writeText(exportPortableM3u(files, MediaScanner.targetFolder()), Charsets.UTF_8)
                }
            }
        }
        return exported.mapCatching { file ->
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.apple.mpegurl"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(intent, chooserTitle)
        }
    }

    suspend fun list(context: Context, label: String, files: List<MediaFile>, chooserTitle: String): Result<Unit> =
        listChooser(context, label, files, chooserTitle).mapCatching { chooser ->
            if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }

    private fun mimeFor(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "aiff", "aif" -> "audio/aiff"
            "opus" -> "audio/opus"
            "ape" -> "audio/ape"
            "dsf" -> "audio/dsf"
            "dff" -> "audio/dff"
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: if (extension in MediaScanner.videoExtensions) "video/*" else "audio/*"
        }
    }
}
