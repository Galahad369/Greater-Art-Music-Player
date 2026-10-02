package com.local.listentomusic.ui.components

import com.local.listentomusic.data.BackgroundScaleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackgroundSyncTest {
    @Test
    fun cropPanUsesOnlyValidHorizontalOverflow() {
        assertEquals(260f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, 0f), 0f)
        assertEquals(130f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, .25f), 0f)
        assertEquals(0f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, .5f), 0f)
        assertEquals(-130f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, .75f), 0f)
        assertEquals(-260f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, 1f), 0f)
        assertEquals(-260f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, 2f), 0f)
        assertEquals(260f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.CROP, -1f), 0f)
    }

    @Test
    fun noCropPanWithoutOverflowOrInOtherScaleModes() {
        assertEquals(0f, backgroundCropTranslationX(900, 1080, BackgroundScaleMode.CROP, 1f), 0f)
        assertEquals(0f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.FIT, 1f), 0f)
        assertEquals(0f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.STRETCH, 1f), 0f)
    }

    @Test
    fun videoBackgroundWaitsForPrimaryVideoFrame() {
        assertFalse(shouldAttachVideoBackground(visible = true, primaryIsVideo = true, primaryFrameReady = false))
        assertTrue(shouldAttachVideoBackground(visible = true, primaryIsVideo = true, primaryFrameReady = true))
    }

    @Test
    fun audioPlaybackDoesNotBlockIndependentVideoBackground() {
        assertTrue(shouldAttachVideoBackground(visible = true, primaryIsVideo = false, primaryFrameReady = false))
    }

    @Test
    fun hiddenBackgroundNeverAllocatesVideoOutput() {
        assertFalse(shouldAttachVideoBackground(visible = false, primaryIsVideo = false, primaryFrameReady = true))
        assertFalse(shouldAttachVideoBackground(visible = false, primaryIsVideo = true, primaryFrameReady = true))
    }

    @Test
    fun currentVideoBackgroundMirrorsPrimaryPauseState() {
        assertTrue(shouldMirrorPrimaryPlayback(lifecycleActive = true, primaryIsPlaying = true))
        assertFalse(shouldMirrorPrimaryPlayback(lifecycleActive = true, primaryIsPlaying = false))
        assertFalse(shouldMirrorPrimaryPlayback(lifecycleActive = false, primaryIsPlaying = true))
    }

    @Test
    fun listFlingKeepsDecorativeVideoDetachedUntilSurfaceSettle() {
        assertFalse(shouldUseLiveVideoSurface(attachVideoBackground = true, listScrolling = true, surfaceSettled = true))
        assertFalse(shouldUseLiveVideoSurface(attachVideoBackground = true, listScrolling = false, surfaceSettled = false))
        assertFalse(shouldUseLiveVideoSurface(attachVideoBackground = false, listScrolling = false, surfaceSettled = true))
        assertTrue(shouldUseLiveVideoSurface(attachVideoBackground = true, listScrolling = false, surfaceSettled = true))
    }


    @Test
    fun normalPlaybackDriftDoesNotContinuouslyFlushVideoDecoder() {
        assertFalse(shouldResyncBackground(10_000, 10_350, true))
        assertFalse(shouldResyncBackground(10_000, 12_000, true))
        assertTrue(shouldResyncBackground(10_000, 12_001, true))
    }

    @Test
    fun pausedFrameAndExplicitSeekUseTighterAlignment() {
        assertFalse(shouldResyncBackground(1_000, 1_080, false))
        assertTrue(shouldResyncBackground(1_000, 1_081, false))
        assertTrue(shouldResyncBackground(30_000, 1_000, false))
    }
}
