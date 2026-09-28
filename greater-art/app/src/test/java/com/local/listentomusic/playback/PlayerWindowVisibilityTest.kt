package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test

class PlayerWindowVisibilityTest {
    @Test fun detachedOnlyAppearsAfterTheAppAndExpandedPlayerLeave() {
        assertTrue(showDetachedPlayer(false, false))
        assertFalse(showDetachedPlayer(true, false))
        assertFalse(showDetachedPlayer(false, true))
        assertFalse(showDetachedPlayer(true, true))
    }
    @Test fun settingsHidesMiniWithoutDockingIt() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.expanded(false)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.app(false)
        assertTrue(PlayerWindowVisibility.detachedVisible.value)
    }
    @Test fun switchingHostsNeverBrieflyShowsDetached() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.expanded(true)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.library(false)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.expanded(false)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.app(false)
        assertTrue(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.app(false)
    }
}
