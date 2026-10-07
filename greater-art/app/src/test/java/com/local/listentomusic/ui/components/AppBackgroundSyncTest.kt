package com.local.listentomusic.ui.components

import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.backgroundScaleModeFromStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackgroundSyncTest {
    @Test
    fun currentVideoBackgroundLeaseMustFollowLiveSurfaceAvailability() {
        assertTrue(shouldClaimCurrentVideoBackground(
            usePrimaryVideoBackground = true,
            surfaceActive = true,
        ))
        assertFalse(shouldClaimCurrentVideoBackground(
            usePrimaryVideoBackground = true,
            surfaceActive = false,
        ))
        assertFalse(shouldClaimCurrentVideoBackground(
            usePrimaryVideoBackground = false,
            surfaceActive = true,
        ))
    }

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
    }

    @Test
    fun backgroundRenderPoliciesNeverUseFillStretching() {
        assertEquals(
            androidx.compose.ui.layout.ContentScale.Crop,
            backgroundImageContentScale(BackgroundScaleMode.CROP),
        )
        assertEquals(
            androidx.compose.ui.layout.ContentScale.Fit,
            backgroundImageContentScale(BackgroundScaleMode.FIT),
        )
        assertEquals(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            backgroundVideoResizeMode(BackgroundScaleMode.CROP),
        )
        assertEquals(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
            backgroundVideoResizeMode(BackgroundScaleMode.FIT),
        )
        assertTrue(
            listOf(BackgroundScaleMode.CROP, BackgroundScaleMode.FIT).all {
                backgroundVideoResizeMode(it) != androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            },
        )
    }

    @Test
    fun onlyAspectPreservingBackgroundModesExistAndLegacyStretchMigratesToCrop() {
        assertEquals(listOf(BackgroundScaleMode.CROP, BackgroundScaleMode.FIT), BackgroundScaleMode.entries.toList())
        assertEquals(BackgroundScaleMode.CROP, backgroundScaleModeFromStorage(null))
        assertEquals(BackgroundScaleMode.CROP, backgroundScaleModeFromStorage("STRETCH"))
        assertEquals(BackgroundScaleMode.CROP, backgroundScaleModeFromStorage("unknown"))
        assertEquals(BackgroundScaleMode.CROP, backgroundScaleModeFromStorage("CROP"))
        assertEquals(BackgroundScaleMode.FIT, backgroundScaleModeFromStorage("FIT"))
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
    fun currentVideoBackgroundUsesPrimaryPlayerOnlyWhenItCanOwnTheSurface() {
        assertTrue(shouldUsePrimaryVideoBackground(
            visible = true,
            allowVideoBackground = true,
            isVideo = true,
            controllerAvailable = true,
        ))
        assertFalse(shouldUsePrimaryVideoBackground(
            visible = false,
            allowVideoBackground = true,
            isVideo = true,
            controllerAvailable = true,
        ))
        assertFalse(shouldUsePrimaryVideoBackground(
            visible = true,
            allowVideoBackground = false,
            isVideo = true,
            controllerAvailable = true,
        ))
        assertFalse(shouldUsePrimaryVideoBackground(
            visible = true,
            allowVideoBackground = true,
            isVideo = false,
            controllerAvailable = true,
        ))
        assertFalse(shouldUsePrimaryVideoBackground(
            visible = true,
            allowVideoBackground = true,
            isVideo = true,
            controllerAvailable = false,
        ))
    }

}
