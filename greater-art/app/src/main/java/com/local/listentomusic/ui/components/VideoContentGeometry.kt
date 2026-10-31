package com.local.listentomusic.ui.components

import kotlin.math.roundToInt

internal data class VideoContentSize(val width: Int, val height: Int)

/** Rendered VideoSize already includes decoder-applied rotation. Do not rotate it again. */
internal fun sourceVideoAspectRatio(width: Int, height: Int, pixelWidthHeightRatio: Float): Float? {
    if (width <= 0 || height <= 0 || !pixelWidthHeightRatio.isFinite() || pixelWidthHeightRatio <= 0f) return null
    val ratio = (width.toDouble() * pixelWidthHeightRatio / height).toFloat()
    return ratio.takeIf { it.isFinite() && it > 0f }
}

/** One proportional scale; only final physical-pixel rounding can alter the ratio. */
internal fun aspectPreservingVideoSize(
    viewportWidth: Int,
    viewportHeight: Int,
    displayAspectRatio: Float,
    crop: Boolean,
): VideoContentSize? {
    if (viewportWidth <= 0 || viewportHeight <= 0 ||
        !displayAspectRatio.isFinite() || displayAspectRatio <= 0f) return null
    val aspect = displayAspectRatio.toDouble()
    val widthLimited = (aspect >= viewportWidth.toDouble() / viewportHeight) != crop
    val width = if (widthLimited) viewportWidth.toDouble() else viewportHeight * aspect
    val height = if (widthLimited) viewportWidth / aspect else viewportHeight.toDouble()
    // Do not independently clamp oversized dimensions: that would itself stretch
    // malformed metadata into a representable, but incorrect, rectangle.
    // Android View packs measured sizes into 24 bits (MEASURED_SIZE_MASK),
    // smaller than either an Int or MeasureSpec's 30-bit size field.
    val maxMeasuredSize = 0x00ffffff
    if (!width.isFinite() || !height.isFinite() || width > maxMeasuredSize || height > maxMeasuredSize) return null
    return VideoContentSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
}
