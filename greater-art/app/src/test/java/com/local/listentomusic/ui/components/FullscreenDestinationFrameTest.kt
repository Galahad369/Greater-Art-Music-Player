package com.local.listentomusic.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenDestinationFrameTest {
    @Test fun theOutgoingFrameCannotCompleteEntry() {
        assertFalse(fullscreenDestinationHasFrame(5,
            SurfaceLease(owner = "MINI_WINDOW", generation = 5, firstFrame = true)))
    }

    @Test fun aStaleFullscreenFrameCannotCompleteANewEntry() {
        assertFalse(fullscreenDestinationHasFrame(5,
            SurfaceLease(owner = "NOW_PLAYING", generation = 5, firstFrame = true)))
    }

    @Test fun attachmentWithoutARenderedFrameIsNotReady() {
        assertFalse(fullscreenDestinationHasFrame(5,
            SurfaceLease(owner = "NOW_PLAYING", generation = 6, firstFrame = false)))
    }

    @Test fun onlyTheNewFullscreenFrameCompletesEntry() {
        assertTrue(fullscreenDestinationHasFrame(5,
            SurfaceLease(owner = "NOW_PLAYING", generation = 6, firstFrame = true)))
        assertFalse(fullscreenDestinationHasFrame(5,
            SurfaceLease(owner = "MINI_WINDOW", generation = 6, firstFrame = true)))
    }
}
