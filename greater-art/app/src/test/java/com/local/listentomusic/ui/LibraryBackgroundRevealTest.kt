package com.local.listentomusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBackgroundRevealTest {
    @Test
    fun snapThresholdMatchesGrokContract() {
        assertEquals(0f, libraryBackgroundRevealTarget(0f), 0f)
        assertEquals(0f, libraryBackgroundRevealTarget(0.349f), 0f)
        assertEquals(1f, libraryBackgroundRevealTarget(0.35f), 0f)
        assertEquals(1f, libraryBackgroundRevealTarget(1f), 0f)
    }

    @Test
    fun revealTranslatesContentWithoutResizingBackground() {
        assertEquals(0f, libraryBackgroundRevealOffsetPx(0f, 1000f), 0f)
        assertEquals(500f, libraryBackgroundRevealOffsetPx(0.5f, 1000f), 0f)
        assertEquals(1000f, libraryBackgroundRevealOffsetPx(1f, 1000f), 0f)
        assertEquals(1000f, libraryBackgroundRevealOffsetPx(2f, 1000f), 0f)
        assertEquals(0f, libraryBackgroundRevealOffsetPx(-1f, 1000f), 0f)
        assertEquals(0f, libraryBackgroundRevealOffsetPx(0.5f, -1f), 0f)
    }

    @Test
    fun revealUsesMoreOfTheViewportWithoutTakingTheWholeScreen() {
        assertEquals(940f, libraryBackgroundRevealMaxPx(1000f, 240f), 0f)
        assertEquals(470f, libraryBackgroundRevealMaxPx(500f, 240f), 0f)
        assertEquals(282f, libraryBackgroundRevealMaxPx(300f, 240f), 0f)
        assertEquals(0f, libraryBackgroundRevealMaxPx(0f, 240f), 0f)
    }

    @Test
    fun revealGeometryAndLightScrimStayBounded() {
        assertTrue(LIBRARY_BACKGROUND_REVEAL_MIN_DP > 0f)
        assertTrue(LIBRARY_BACKGROUND_REVEAL_VIEWPORT_FRACTION in 0f..1f)
        assertTrue(LIBRARY_BACKGROUND_REVEAL_VIEWPORT_CAP_FRACTION in 0f..1f)
        assertTrue(LIBRARY_BACKGROUND_REVEAL_VIEWPORT_CAP_FRACTION >= LIBRARY_BACKGROUND_REVEAL_VIEWPORT_FRACTION)
        assertTrue(LIBRARY_BACKGROUND_REVEAL_SNAP_THRESHOLD in 0f..1f)
        assertEquals(0.94f, libraryBackgroundContentAlpha(lightPalette = true), 0f)
        assertEquals(0f, libraryBackgroundContentAlpha(lightPalette = false), 0f)
    }
}
