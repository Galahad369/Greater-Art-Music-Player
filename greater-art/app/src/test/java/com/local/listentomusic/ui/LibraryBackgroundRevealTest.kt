package com.local.listentomusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBackgroundRevealTest {
    @Test
    fun snapThresholdFavorsEasyRevealWithoutAccidentalTinyPulls() {
        assertEquals(0f, libraryBackgroundRevealTarget(0f), 0f)
        assertEquals(0f, libraryBackgroundRevealTarget(0.179f), 0f)
        assertEquals(1f, libraryBackgroundRevealTarget(0.18f), 0f)
        assertEquals(1f, libraryBackgroundRevealTarget(1f), 0f)
    }

    @Test
    fun upwardWallpaperDragAlwaysChoosesCollapse() {
        assertEquals(0f, libraryBackgroundRecoveryTarget(-1f, 0.95f), 0f)
        assertEquals(0f, libraryBackgroundRecoveryTarget(-200f, 0.80f), 0f)
        assertEquals(1f, libraryBackgroundRecoveryTarget(1f, 0.80f), 0f)
        assertEquals(1f, libraryBackgroundRecoveryTarget(1f, 0.20f), 0f)
    }

    @Test
    fun flingDirectionWinsOverCurrentFraction() {
        assertEquals(0f, libraryBackgroundFlingTarget(-20f, 0.95f), 0f)
        assertEquals(1f, libraryBackgroundFlingTarget(20f, 0.05f), 0f)
        assertEquals(0f, libraryBackgroundFlingTarget(0f, 0.10f), 0f)
        assertEquals(1f, libraryBackgroundFlingTarget(0f, 0.50f), 0f)
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
    fun revealCanMoveTheEntireSheetOffScreenOnEveryViewport() {
        assertEquals(1000f, libraryBackgroundRevealMaxPx(1000f, 240f), 0f)
        assertEquals(500f, libraryBackgroundRevealMaxPx(500f, 240f), 0f)
        assertEquals(300f, libraryBackgroundRevealMaxPx(300f, 240f), 0f)
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
