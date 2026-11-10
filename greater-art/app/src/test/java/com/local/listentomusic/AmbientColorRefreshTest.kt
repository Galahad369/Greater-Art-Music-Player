package com.local.listentomusic

import com.local.listentomusic.data.AmbientMotion
import com.local.listentomusic.ui.components.ambientShouldAnimate
import com.local.listentomusic.ui.components.ambientShouldRefreshColors
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientColorRefreshTest {
    @Test fun visibleNowPlayingRefreshesMediaColorsInPowerSaving() {
        assertTrue(
            "Now Playing must refresh media colors while Power saving is on",
            ambientShouldRefreshColors(active = true, scrolling = false, powerSave = true,
                playerPresentation = true, intensity = .8f),
        )
    }

    @Test fun browsingStillStopsRefreshingColorsInPowerSaving() {
        assertFalse(ambientShouldRefreshColors(active = true, scrolling = false, powerSave = true,
            playerPresentation = false, intensity = .8f))
        assertTrue(ambientShouldRefreshColors(active = true, scrolling = false, powerSave = false,
            playerPresentation = false, intensity = .8f))
    }

    @Test fun hiddenScrollingOrDisabledPlayerDoesNotRefreshColors() {
        assertFalse(ambientShouldRefreshColors(active = false, scrolling = false, powerSave = true,
            playerPresentation = true, intensity = .8f))
        assertFalse(ambientShouldRefreshColors(active = true, scrolling = true, powerSave = true,
            playerPresentation = true, intensity = .8f))
        assertFalse(ambientShouldRefreshColors(active = true, scrolling = false, powerSave = true,
            playerPresentation = true, intensity = 0f))
    }

    @Test fun powerSavingStillSuppressesDecorativeMotion() {
        assertFalse(ambientShouldAnimate(visible = true, resumed = true, scrolling = false,
            reducedMotion = false, powerSave = true, motion = AmbientMotion.NORMAL))
    }
}
