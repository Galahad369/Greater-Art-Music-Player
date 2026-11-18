package com.local.listentomusic.ui

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Fullscreen light starts at the visible Fit edges, not the viewport boundary. */
@RunWith(AndroidJUnit4::class)
class FullscreenAmbientBoundsTest {
    @Test fun portraitAndLandscapeFitReserveOnlyActualBlackBars() {
        for ((width, height) in listOf(1080 to 2340, 2340 to 1080, 720 to 1280, 1280 to 720)) {
            for (ratio in listOf(16f / 9, 9f / 16, 1f, 4f / 3)) {
                val viewport = Rect(15, 25, 15 + width, 25 + height)
                val before = Rect(viewport)
                val video = requireNotNull(fullscreenAmbientVideoBounds(viewport, ratio))
                assertEquals(before, viewport)
                assertTrue(kotlin.math.abs(viewport.centerX() - video.centerX()) <= 1)
                assertTrue(kotlin.math.abs(viewport.centerY() - video.centerY()) <= 1)
                assertTrue(video.left >= viewport.left && video.right <= viewport.right)
                assertTrue(video.top >= viewport.top && video.bottom <= viewport.bottom)
                assertTrue(kotlin.math.abs(video.width().toFloat() / video.height() - ratio) <= .005f)
            }
        }
    }

    @Test fun invalidMetadataFallsBackWithoutStretchingOrChangingTheViewport() {
        val viewport = Rect(1, 2, 301, 402)
        assertSame(viewport, fullscreenAmbientVideoBounds(viewport, Float.NaN))
        assertSame(viewport, fullscreenAmbientVideoBounds(viewport, 0f))
        assertEquals(null, fullscreenAmbientVideoBounds(null, 1f))
    }
}
