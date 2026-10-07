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
    fun revealGeometryAndLightScrimStayBounded() {
        assertTrue(LIBRARY_BACKGROUND_REVEAL_MAX_DP > 0f)
        assertTrue(LIBRARY_BACKGROUND_REVEAL_SNAP_THRESHOLD in 0f..1f)
        assertEquals(0.94f, libraryBackgroundContentAlpha(lightPalette = true), 0f)
        assertEquals(0f, libraryBackgroundContentAlpha(lightPalette = false), 0f)
    }
}
