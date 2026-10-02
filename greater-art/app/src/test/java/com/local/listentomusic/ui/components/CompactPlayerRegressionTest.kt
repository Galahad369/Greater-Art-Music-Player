package com.local.listentomusic.ui.components

import com.local.listentomusic.model.CompactPlayerMetrics
import org.junit.Assert.*
import org.junit.Test

class CompactPlayerRegressionTest {
    @Test fun samsungContentHeightExcludesSystemInset() {
        assertEquals(173, CompactPlayerMetrics.heightPx(2.8125f))
        assertEquals(1013, CompactPlayerMetrics.widthPx(2.8125f, 1080))
        assertEquals(500, CompactPlayerMetrics.widthPx(2.8125f, 500))
    }
    @Test fun repeatKeepsActualRenderedEvidence() {
        val rendered = SurfaceLease().attach("LIBRARY_MINI", 1, 10).frame(11, true)
        assertSame(rendered, rendered.mediaChanged(20, repeated = true))
        assertTrue(rendered.mediaChanged(20, repeated = true).firstFrame)
        assertFalse(rendered.mediaChanged(20).firstFrame)
        assertFalse(rendered.mediaChanged(20).mediaFirstFrame)
    }
    @Test fun repeatedMiniBindingSkipsDuplicateRefreshWork() {
        assertFalse(shouldRefreshCompactBinding(samePlayer = true, samePresentation = true))
        assertTrue(shouldRefreshCompactBinding(samePlayer = false, samePresentation = true))
        assertTrue(shouldRefreshCompactBinding(samePlayer = true, samePresentation = false))
    }

    @Test fun libraryAndMiniRetainSourceWhileDestinationRegisters() {
        assertTrue(shouldRetainPrimarySurfaceDuringHandoff("LIBRARY_MINI", "MINI_WINDOW", false))
        assertTrue(shouldRetainPrimarySurfaceDuringHandoff("MINI_WINDOW", "LIBRARY_MINI", false))
        assertFalse(shouldRetainPrimarySurfaceDuringHandoff("LIBRARY_MINI", "MINI_WINDOW", true))
    }
}
