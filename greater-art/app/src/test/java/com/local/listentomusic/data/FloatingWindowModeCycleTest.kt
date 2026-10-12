package com.local.listentomusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** User-facing Now Playing mode selector and legacy preference compatibility. */
class FloatingWindowModeCycleTest {
    @Test fun selectorIsExactlyThreeStatesAndWraps() {
        assertEquals(FloatingWindowMode.FOLLOW_VIDEO, FloatingWindowMode.MINI_WINDOW.nextFloatingMode())
        assertEquals(FloatingWindowMode.NONE, FloatingWindowMode.FOLLOW_VIDEO.nextFloatingMode())
        assertEquals(FloatingWindowMode.MINI_WINDOW, FloatingWindowMode.NONE.nextFloatingMode())
    }

    @Test fun legacyCompactMapsOntoTheSameThreeStateCycle() {
        assertEquals(FloatingWindowMode.NONE, FloatingWindowMode.COMPACT.nextFloatingMode())
        assertTrue(FloatingWindowMode.COMPACT.usesAndroidPip())
    }

    @Test fun onlyNativePipVariantsEnableSystemPictureInPicture() {
        assertTrue(FloatingWindowMode.FOLLOW_VIDEO.usesAndroidPip())
        assertFalse(FloatingWindowMode.MINI_WINDOW.usesAndroidPip())
        assertFalse(FloatingWindowMode.NONE.usesAndroidPip())
    }

    @Test fun existingInstallsStartInMiniWindow() {
        assertEquals(FloatingWindowMode.MINI_WINDOW, UserPreferences().floatingWindowMode)
    }
}
