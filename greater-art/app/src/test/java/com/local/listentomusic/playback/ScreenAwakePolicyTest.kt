package com.local.listentomusic.playback

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAwakePolicyTest {
    private val keepAwake = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    private val unrelated = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    @Test fun onlyExpandedNowPlayingKeepsScreenAwake() {
        assertFalse(playerWindowScreenAwakeFlags(unrelated, PlayerWindowMode.DOCKED) and keepAwake != 0)
        assertFalse(playerWindowScreenAwakeFlags(unrelated, PlayerWindowMode.DETACHED) and keepAwake != 0)
        assertTrue(playerWindowScreenAwakeFlags(unrelated, PlayerWindowMode.EXPANDED) and keepAwake != 0)
    }

    @Test fun collapsingExpandedPlayerRestoresScreenTimeoutAndPreservesOtherFlags() {
        val expandedFlags = playerWindowScreenAwakeFlags(unrelated, PlayerWindowMode.EXPANDED)
        assertEquals(unrelated, playerWindowScreenAwakeFlags(expandedFlags, PlayerWindowMode.DETACHED))
        assertEquals(unrelated, playerWindowScreenAwakeFlags(expandedFlags, PlayerWindowMode.DOCKED))
        assertEquals(expandedFlags, playerWindowScreenAwakeFlags(expandedFlags, PlayerWindowMode.EXPANDED))
    }
}
