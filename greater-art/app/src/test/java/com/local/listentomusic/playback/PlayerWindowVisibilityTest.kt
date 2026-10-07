package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test

class PlayerWindowVisibilityTest {
    @Test fun dialogFocusLossHidesOnlyDockAndRestoresItAfterDismissal() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.expanded(false)
        PlayerWindowVisibility.windowFocus(true)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.windowFocus(false)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.windowFocus(true)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.app(false)
    }
    @Test fun wallpaperRevealHidesDockAndRestoresItAfterCollapse() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.expanded(false)
        PlayerWindowVisibility.windowFocus(true)
        PlayerWindowVisibility.stackTransport(false)
        PlayerWindowVisibility.libraryBackgroundReveal(false)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)

        PlayerWindowVisibility.libraryBackgroundReveal(true)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)

        PlayerWindowVisibility.libraryBackgroundReveal(false)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)

        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.app(false)
    }

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
    @Test fun expandedVisibilityIsPublishedForCoveredBackgroundConsumers() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.expanded(false)
        assertFalse(PlayerWindowVisibility.expandedShowing.value)

        PlayerWindowVisibility.expanded(true)
        assertTrue(PlayerWindowVisibility.expandedShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)

        PlayerWindowVisibility.expanded(false)
        assertFalse(PlayerWindowVisibility.expandedShowing.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)

        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.app(false)
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
