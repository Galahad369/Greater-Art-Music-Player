package com.local.listentomusic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniWindowMetricsTest {
    @Test fun landscapeMediaFitsWithoutAWindowGutter() {
        val density = 2.8125f
        val (width, height) = MiniWindowMetrics.detachedSizePx(density, 16f / 9f)
        assertEquals(MiniWindowMetrics.heightPx(density), height)
        assertTrue(width < MiniWindowMetrics.widthPx(density))
        assertTrue(kotlin.math.abs(width.toFloat() / height - 16f / 9f) < .01f)
    }

    @Test fun squareArtworkUsesTheExactSquareWindow() {
        val size = MiniWindowMetrics.detachedSizePx(2f, 1f)
        assertEquals(MiniWindowMetrics.heightPx(2f) to MiniWindowMetrics.heightPx(2f), size)
    }

    @Test fun unusualAspectRatiosStillFitTheOldMaximumBounds() {
        listOf(.5f, 2.5f, 0f, Float.NaN).forEach { aspect ->
            val (width, height) = MiniWindowMetrics.detachedSizePx(2f, aspect)
            assertTrue(width in 1..MiniWindowMetrics.widthPx(2f))
            assertTrue(height in 1..MiniWindowMetrics.heightPx(2f))
        }
    }
}
