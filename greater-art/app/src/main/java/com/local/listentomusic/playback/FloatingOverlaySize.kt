package com.local.listentomusic.playback


internal data class FloatingOverlaySize(val widthPx: Int, val heightPx: Int)

/** The expanded player fills the already-inset frame: no second set of gutters. */
internal fun floatingOverlaySize(safeWidthPx: Int, safeHeightPx: Int): FloatingOverlaySize {
    val width = safeWidthPx.coerceAtLeast(1)
    val height = safeHeightPx.coerceAtLeast(1)
    return FloatingOverlaySize(width, height)
}

internal fun expandedPullReturnThresholdPx(viewportHeightPx: Int, density: Float): Int {
    val height = viewportHeightPx.coerceAtLeast(1)
    val safeDensity = density.coerceAtLeast(.1f)
    val portraitCap = (72f * safeDensity).toInt().coerceAtLeast(1).coerceAtMost(height)
    val minimum = (32f * safeDensity).toInt().coerceAtLeast(1).coerceAtMost(height)
    val adaptive = (height * 0.22f).toInt().coerceAtLeast(1)
    return adaptive.coerceIn(minimum, portraitCap.coerceAtLeast(minimum))
}

internal fun pullDismissReached(distancePx: Int, density: Float, viewportHeightPx: Int): Boolean =
    distancePx >= expandedPullReturnThresholdPx(viewportHeightPx, density)

internal fun expandedPullMaxDistancePx(viewportHeightPx: Int): Int =
    viewportHeightPx.coerceAtLeast(1)

internal fun shouldShrinkForSystemReason(reason: String?): Boolean =
    reason == "homekey" || reason == "recentapps"
