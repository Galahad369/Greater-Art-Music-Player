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
    fun cropAndFitPanUseOnlyLegalHorizontalSpace() {
        assertEquals(0f, backgroundPanTranslationX(900, 1080, BackgroundScaleMode.CROP, 1f), 0f)
        assertEquals(-90f, backgroundPanTranslationX(900, 1080, BackgroundScaleMode.FIT, 0f), 0f)
        assertEquals(0f, backgroundPanTranslationX(900, 1080, BackgroundScaleMode.FIT, .5f), 0f)
        assertEquals(90f, backgroundPanTranslationX(900, 1080, BackgroundScaleMode.FIT, 1f), 0f)
        assertEquals(0f, backgroundPanTranslationX(1600, 1080, BackgroundScaleMode.FIT, 1f), 0f)
        assertEquals(0f, backgroundCropTranslationX(1600, 1080, BackgroundScaleMode.FIT, 1f), 0f)
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
        assertFalse(
            shouldUsePrimaryVideoBackground(
                visible = true,
                allowVideoBackground = true,
                isVideo = true,
                controllerAvailable = true,
                primarySurfaceAvailable = false,
            ),
        )
    }

    @Test
    fun currentVideoMirrorKeepsWallpaperVisibleWhenDockOwnsPrimarySurface() {
        assertTrue(
            shouldMirrorCurrentVideoBackground(
                visible = true,
                allowVideoBackground = true,
                isVideo = true,
                controllerAvailable = true,
                primarySurfaceAvailable = false,
                primaryFrameReady = true,
            ),
        )
        assertFalse(
            shouldMirrorCurrentVideoBackground(
                visible = true,
                allowVideoBackground = true,
                isVideo = true,
                controllerAvailable = true,
                primarySurfaceAvailable = true,
                primaryFrameReady = true,
            ),
        )
        assertFalse(
            shouldMirrorCurrentVideoBackground(
                visible = true,
                allowVideoBackground = true,
                isVideo = true,
                controllerAvailable = true,
                primarySurfaceAvailable = false,
                primaryFrameReady = false,
            ),
        )
    }

    @Test
    fun visibleLibraryDockReservesPrimaryVideoSurfaceFromCurrentVideoWallpaper() {
        assertFalse(
            shouldUsePrimaryVideoBackground(
                visible = true,
                allowVideoBackground = true,
                isVideo = true,
                controllerAvailable = true,
                primarySurfaceAvailable = false,
            ),
        )
        assertEquals(
            "MINI_WINDOW",
            expectedSurfaceOwner(
                foreground = true,
                nowPlaying = false,
                pip = false,
                systemOverlayOwner = "MINI_WINDOW",
                currentVideoBackground = false,
                systemOverlayDocked = true,
            ),
        )
    }

}
