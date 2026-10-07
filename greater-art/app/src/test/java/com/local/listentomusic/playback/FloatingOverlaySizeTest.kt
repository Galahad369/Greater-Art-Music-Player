package com.local.listentomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingOverlaySizeTest {
    @Test fun portraitPhoneFillsTheSafeFrameWithoutYellowGutters() {
        val size = floatingOverlaySize(1080, 2130)
        assertEquals(1080, size.widthPx)
        assertEquals(2130, size.heightPx)
    }

    @Test fun smallAndLandscapeDisplaysNeverOverflow() {
        listOf(240 to 300, 600 to 360, 1200 to 650).forEach { (width, height) ->
            val size = floatingOverlaySize(width, height)
            assertTrue(size.widthPx in 1..width)
            assertTrue(size.heightPx in 1..height)
        }
    }

    @Test fun tabletAndInvalidBoundsRemainSafe() {
        val size = floatingOverlaySize(3000, 2400)
        assertEquals(3000, size.widthPx)
        assertEquals(2400, size.heightPx)
        assertEquals(FloatingOverlaySize(1, 1), floatingOverlaySize(0, 0))
    }

    @Test fun homeAndRecentsShrinkButOtherSystemDialogsDoNot() {
        assertTrue(shouldShrinkForSystemReason("homekey"))
        assertTrue(shouldShrinkForSystemReason("recentapps"))
        org.junit.Assert.assertFalse(shouldShrinkForSystemReason(null))
        org.junit.Assert.assertFalse(shouldShrinkForSystemReason("assist"))
        org.junit.Assert.assertFalse(shouldShrinkForSystemReason("globalactions"))
    }
    @Test fun pullDismissThresholdUsesDeliberatePortraitCap() {
        org.junit.Assert.assertFalse(pullDismissReached(239, 2f, 1200))
        assertTrue(pullDismissReached(240, 2f, 1200))
        org.junit.Assert.assertFalse(pullDismissReached(-1, 1f, 1200))
    }

    @Test fun landscapePullThresholdHasAUsableMinimum() {
        assertEquals(112, expandedPullReturnThresholdPx(360, 2f))
        org.junit.Assert.assertFalse(pullDismissReached(111, 2f, 360))
        assertTrue(pullDismissReached(112, 2f, 360))
        assertEquals(360, expandedPullMaxDistancePx(360))
    }

    @Test fun tinyLandscapeStillKeepsTheFiftySixDpMinimumWhenPossible() {
        assertEquals(112, expandedPullReturnThresholdPx(180, 2f))
        assertEquals(1, expandedPullMaxDistancePx(0))
    }

    @Test fun pullActivationResistanceAndFastFlingRequireIntent() {
        assertEquals(48f, expandedPullActivationPx(2f), 0f)
        assertEquals(72f, expandedPullResistedDelta(100f), 0.001f)
        org.junit.Assert.assertFalse(expandedPullFastFlingReached(111f, 4000f, 2f))
        org.junit.Assert.assertFalse(expandedPullFastFlingReached(112f, 2799f, 2f))
        assertTrue(expandedPullFastFlingReached(112f, 2800f, 2f))
    }
}
