package com.local.listentomusic.playback


internal data class FloatingOverlaySize(val widthPx: Int, val heightPx: Int)

internal const val EXPANDED_PULL_ACTIVATION_DP = 24f
internal const val EXPANDED_PULL_RESISTANCE = 0.72f
internal const val EXPANDED_PULL_FAST_FLING_MIN_DP = 56f
internal const val EXPANDED_PULL_FAST_FLING_VELOCITY_DP_PER_SEC = 1400f

internal fun expandedPullActivationPx(density: Float): Float =
    EXPANDED_PULL_ACTIVATION_DP * density.coerceAtLeast(.1f)

internal fun expandedPullResistedDelta(deltaPx: Float): Float =
    deltaPx * EXPANDED_PULL_RESISTANCE

internal fun expandedPullFastFlingReached(
    totalDragPx: Float,
    velocityYPxPerSecond: Float,
    density: Float,
): Boolean {
    val safeDensity = density.coerceAtLeast(.1f)
    return totalDragPx >= EXPANDED_PULL_FAST_FLING_MIN_DP * safeDensity &&
        velocityYPxPerSecond >= EXPANDED_PULL_FAST_FLING_VELOCITY_DP_PER_SEC * safeDensity
}

/** The expanded player fills the already-inset frame: no second set of gutters. */
internal fun floatingOverlaySize(safeWidthPx: Int, safeHeightPx: Int): FloatingOverlaySize {
    val width = safeWidthPx.coerceAtLeast(1)
    val height = safeHeightPx.coerceAtLeast(1)
    return FloatingOverlaySize(width, height)
}

internal fun expandedPullReturnThresholdPx(viewportHeightPx: Int, density: Float): Int {
    val height = viewportHeightPx.coerceAtLeast(1)
    val safeDensity = density.coerceAtLeast(.1f)
    val maximum = (120f * safeDensity).toInt().coerceAtLeast(1).coerceAtMost(height)
    val minimum = (56f * safeDensity).toInt().coerceAtLeast(1).coerceAtMost(height)
    val adaptive = (height * 0.30f).toInt().coerceAtLeast(1)
    return adaptive.coerceIn(minimum, maximum.coerceAtLeast(minimum))
}

internal fun pullDismissReached(distancePx: Int, density: Float, viewportHeightPx: Int): Boolean =
    distancePx >= expandedPullReturnThresholdPx(viewportHeightPx, density)

internal fun expandedPullMaxDistancePx(viewportHeightPx: Int): Int =
    viewportHeightPx.coerceAtLeast(1)

internal fun shouldShrinkForSystemReason(reason: String?): Boolean =
    reason == "homekey" || reason == "recentapps"
