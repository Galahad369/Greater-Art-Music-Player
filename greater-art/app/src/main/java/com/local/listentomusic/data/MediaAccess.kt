package com.local.listentomusic.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaSource
import java.io.File

/** Resolve at the point of use: a successful scan does not guarantee a surviving grant. */
internal fun MediaExtractor.openMedia(context: Context?, location: String) {
    when (val source = requireNotNull(MediaSource.parse(location))) {
        is MediaSource.LocalFile -> setDataSource(source.value)
        is MediaSource.ContentUri -> setDataSource(requireNotNull(context), Uri.parse(source.value), null)
    }
}

internal fun MediaMetadataRetriever.openMedia(context: Context, location: String) {
    when (val source = requireNotNull(MediaSource.parse(location))) {
        is MediaSource.LocalFile -> setDataSource(source.value)
        is MediaSource.ContentUri -> setDataSource(context, Uri.parse(source.value))
    }
}

internal fun mediaReadable(context: Context, file: MediaFile): Boolean = runCatching {
    when (val source = file.reference.source) {
        is MediaSource.LocalFile -> File(source.value).let {
            it.isFile && it.canRead() && MediaScanner.isInsideTarget(it)
        }
        is MediaSource.ContentUri -> context.contentResolver.openFileDescriptor(Uri.parse(source.value), "r")
            ?.use { true } ?: false
    }
}.getOrDefault(false)

internal fun mediaFingerprint(file: MediaFile): String {
    val source = file.reference.source
    val local = (source as? MediaSource.LocalFile)?.let { File(it.value) }
    return "${file.id}|${source.value}|${local?.length() ?: file.sizeBytes}|${local?.lastModified() ?: file.modifiedMs}|${file.clipStartMs}|${file.clipEndMs}"
}
