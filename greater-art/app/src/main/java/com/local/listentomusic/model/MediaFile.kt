package com.local.listentomusic.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

enum class MediaKind { AUDIO, VIDEO }

@Immutable
data class MediaFile(
    val path: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val modifiedMs: Long,
    val kind: MediaKind,
    val sourcePath: String = path,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long? = null,
    val artist: String = "",
    val album: String = "",
    val searchExtras: String = "",
    val coverUri: String = "",
    /** Original path relative to the discovered root, not an opaque provider document ID. */
    val relativePath: String? = null,
) {
    /** Shared by row RAM probes and the scheduler; copy() creates a fresh identity. */
    internal val thumbnailRequestId: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        "$sourcePath|$sizeBytes|$modifiedMs|$coverUri"
    }
    val id: String get() = path
    val reference: MediaReference
        get() = MediaReference(id, requireNotNull(MediaSource.parse(sourcePath)) {
            "Unsupported local media location"
        }, clipStartMs, clipEndMs)

    private fun playbackUri(): Uri = when (val source = reference.source) {
        is MediaSource.LocalFile -> Uri.fromFile(File(source.value))
        is MediaSource.ContentUri -> Uri.parse(source.value)
    }

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(path)
        .setUri(playbackUri())
        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clipStartMs)
            .apply { clipEndMs?.let { setEndPositionMs(it) } }.build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(artist.takeIf { it.isNotBlank() })
                .setAlbumTitle(album.takeIf { it.isNotBlank() })
                .setMediaType(
                    if (kind == MediaKind.VIDEO) {
                        MediaMetadata.MEDIA_TYPE_VIDEO
                    } else {
                        MediaMetadata.MEDIA_TYPE_MUSIC
                    }
                )
                .build()
        )
        .build()
}

enum class SortMode(val label: String) {
    CUSTOM("Custom order"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
}
