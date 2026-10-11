package com.local.listentomusic.model

import java.net.URI

/** A location is not a permission grant. Resolve readability at the IO boundary. */
sealed interface MediaSource {
    val value: String

    @ConsistentCopyVisibility
    data class LocalFile internal constructor(override val value: String) : MediaSource
    @ConsistentCopyVisibility
    data class ContentUri internal constructor(override val value: String) : MediaSource

    companion object {
        /** Preserves exact legacy keys; never canonicalizes or converts a content URI to File. */
        fun parse(value: String): MediaSource? {
            if (value.isEmpty() || value.any { it.code < 32 || it.code == 127 }) return null
            if (value.startsWith("/")) return LocalFile(value)
            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            if (uri.scheme != "content" || uri.isOpaque || uri.rawAuthority.isNullOrEmpty() ||
                uri.rawUserInfo != null || uri.port != -1 || uri.rawFragment != null ||
                uri.rawPath.isNullOrEmpty()) return null
            return ContentUri(value)
        }
    }
}

/** Logical identity remains independent of location, including existing cue-segment IDs. */
data class MediaReference(
    val id: String,
    val source: MediaSource,
    val startMs: Long = 0,
    val endMs: Long? = null,
) {
    init {
        require(id.isNotEmpty())
        require(startMs >= 0)
        require(endMs == null || endMs > startMs)
    }
}

/** Unsupported records are retained for reporting/recovery, not silently deleted. */
sealed interface LegacyMediaMigration {
    data class Migrated(val reference: MediaReference) : LegacyMediaMigration
    data class Unsupported(val original: String) : LegacyMediaMigration
}

fun migrateLegacyMediaId(id: String): LegacyMediaMigration {
    val bounds = cueBounds(id)
    val location = if (bounds != null) sourceMediaPath(id) else id
    val source = MediaSource.parse(location) ?: return LegacyMediaMigration.Unsupported(id)
    return LegacyMediaMigration.Migrated(
        MediaReference(id, source, bounds?.first ?: 0, bounds?.second),
    )
}
