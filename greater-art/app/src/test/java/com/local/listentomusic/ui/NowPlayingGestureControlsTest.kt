package com.local.listentomusic.ui

import com.local.listentomusic.playback.expandedPullHandleContains
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingGestureControlsTest {
    @Test fun heldDoubleSpeedRequiresTheReverseUpwardTravelNotReleaseOrDownwardMovement() {
        assertFalse("A stationary release must retain 2×", heldDoubleSpeedUnlockRequested(true, 0f, 72f))
        assertFalse(heldDoubleSpeedUnlockRequested(true, 72f, 72f))
        assertFalse(heldDoubleSpeedUnlockRequested(true, -71f, 72f))
        assertTrue(heldDoubleSpeedUnlockRequested(true, -72f, 72f))
        assertFalse(heldDoubleSpeedUnlockRequested(false, -72f, 72f))
        assertFalse(heldDoubleSpeedUnlockRequested(true, -72f, 0f))
    }

    @Test fun pullHandleDoesNotClaimAdjacentButtonSpace() {
        for (density in listOf(1f, 2.625f, 2.8125f, 3f)) {
            val width = 384 * density
            val center = width / 2
            assertTrue(expandedPullHandleContains(center, width, density))
            assertTrue(expandedPullHandleContains(center + 35 * density, width, density))
            assertFalse("The adjoining action is not a pull handle", expandedPullHandleContains(center + 40 * density, width, density))
            assertFalse(expandedPullHandleContains(center - 40 * density, width, density))
        }
    }
}
