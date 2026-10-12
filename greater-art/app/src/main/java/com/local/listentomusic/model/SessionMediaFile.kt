package com.local.listentomusic.model

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** No disk IO on the player thread. The scanner enriches this snapshot when ready. */
internal fun mediaFileFromSession(item: MediaItem): MediaFile {
    val uri = item.localConfiguration?.uri
    val source = if (uri?.scheme == "content") uri.toString()
        else uri?.path ?: sourceMediaPath(item.mediaId)
    val video = item.mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO ||
        source.substringAfterLast('.').lowercase(java.util.Locale.ROOT) in setOf("mp4", "mov", "mkv", "webm", "m4v", "avi", "3gp", "ts", "mpeg", "mpg", "flv")
    return MediaFile(path = item.mediaId, name = item.mediaMetadata.title?.toString() ?: source.substringAfterLast('/'),
        durationMs = 0, sizeBytes = 0, modifiedMs = 0, kind = if (video) MediaKind.VIDEO else MediaKind.AUDIO,
        sourcePath = source, clipStartMs = item.clippingConfiguration.startPositionMs,
        clipEndMs = item.clippingConfiguration.endPositionMs.takeIf { it >= 0 },
        artist = item.mediaMetadata.artist?.toString().orEmpty(), album = item.mediaMetadata.albumTitle?.toString().orEmpty())
}
