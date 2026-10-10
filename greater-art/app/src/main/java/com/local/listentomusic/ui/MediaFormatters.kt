package com.local.listentomusic.ui

import java.util.Locale

/** Metadata duration may be unknown; a playback position of zero is not. */
internal fun formatDuration(milliseconds: Long): String =
    if (milliseconds <= 0L) "--:--" else formatPlaybackTime(milliseconds)

internal fun formatPlaybackTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1000L
    val hours = seconds / 3600L
    val minutes = seconds % 3600L / 60L
    return if (hours > 0L) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds % 60L)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds % 60L)
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.ROOT, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.ROOT, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024L -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "${bytes.coerceAtLeast(0L)} B"
}
