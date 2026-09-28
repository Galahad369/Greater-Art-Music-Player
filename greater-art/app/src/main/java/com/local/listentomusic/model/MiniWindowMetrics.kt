package com.local.listentomusic.model

import kotlin.math.max
import kotlin.math.roundToInt

/** Shared physical size contract for the overlay and the default Library thumbnail. */
object MiniWindowMetrics {
    // The older window was 111×64dp with a 4dp content gutter on every side.
    // Keep its actual visible 103×56dp footprint without bringing the invisible gutter back.
    const val WIDTH_DP = 103
    const val HEIGHT_DP = 56
    const val SHRINK_PX = 0

    fun widthPx(density: Float): Int = max(1, (WIDTH_DP * density).toInt() - SHRINK_PX)
    fun heightPx(density: Float): Int = max(1, (HEIGHT_DP * density).toInt() - SHRINK_PX)
    fun squareWidthPx(density: Float): Int = heightPx(density)

    fun isSquareAspect(aspectRatio: Float): Boolean = aspectRatio in 0.90f..1.10f

    /** Fit the media itself inside the old maximum footprint, without a padded window. */
    fun detachedSizePx(density: Float, aspectRatio: Float): Pair<Int, Int> {
        val maxWidth = widthPx(density)
        val maxHeight = heightPx(density)
        if (!aspectRatio.isFinite() || aspectRatio <= 0f || isSquareAspect(aspectRatio)) {
            return maxHeight to maxHeight
        }
        val width = (maxHeight * aspectRatio).roundToInt().coerceIn(1, maxWidth)
        val height = (width / aspectRatio).roundToInt().coerceIn(1, maxHeight)
        return width to height
    }
}
