package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class PlayerWindowVisibilityTest {
    @Before @After fun resetPresentationState() {
        PlayerWindowVisibility.app(false)
        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.expanded(false)
        PlayerWindowVisibility.windowFocus(true)
        PlayerWindowVisibility.stackTransport(false)
        PlayerWindowVisibility.libraryBackgroundReveal(false)
        PlayerWindowVisibility.resetIme()
    }

    @Test fun hideEndStateDoesNotRestoreDockUntilImeAnimationEnds() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.ime(true)
        val animation = Any()
        PlayerWindowVisibility.imeAnimationPrepared(animation)
        PlayerWindowVisibility.ime(false)
        assertTrue(PlayerWindowVisibility.keyboardShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.imeAnimationStarted(animation)
        PlayerWindowVisibility.discardPreparedImeAnimation(animation)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        assertTrue(PlayerWindowVisibility.dockSurfaceReserved.value)
        PlayerWindowVisibility.imeAnimationFinished(animation)
        assertFalse(PlayerWindowVisibility.keyboardShowing.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
    }

    @Test fun overlappingImeAnimationsKeepDockHiddenUntilBothEnd() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        val first = Any()
        val second = Any()
        PlayerWindowVisibility.imeAnimationPrepared(first)
        PlayerWindowVisibility.imeAnimationStarted(first)
        PlayerWindowVisibility.imeAnimationPrepared(second)
        PlayerWindowVisibility.imeAnimationStarted(second)
        PlayerWindowVisibility.imeAnimationFinished(first)
        assertTrue(PlayerWindowVisibility.keyboardShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.imeAnimationFinished(second)
        assertFalse(PlayerWindowVisibility.keyboardShowing.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
    }

    @Test fun cancelledBeforeStartCanClearWithoutAnEndCallback() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        val cancelled = Any()
        PlayerWindowVisibility.imeAnimationPrepared(cancelled)
        assertTrue(PlayerWindowVisibility.keyboardShowing.value)
        PlayerWindowVisibility.discardPreparedImeAnimation(cancelled)
        assertFalse(PlayerWindowVisibility.keyboardShowing.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)

        PlayerWindowVisibility.ime(true)
        PlayerWindowVisibility.imeAnimationPrepared(cancelled)
        PlayerWindowVisibility.discardPreparedImeAnimation(cancelled)
        assertTrue("A cancelled animation must not erase an actually visible IME",
            PlayerWindowVisibility.keyboardShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
    }

    @Test fun lifecycleResetDropsStaleImeAnimations() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        val old = Any()
        PlayerWindowVisibility.ime(true)
        PlayerWindowVisibility.imeAnimationPrepared(old)
        PlayerWindowVisibility.imeAnimationStarted(old)
        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.app(false)
        PlayerWindowVisibility.resetIme()
        PlayerWindowVisibility.imeAnimationFinished(old)
        assertFalse(PlayerWindowVisibility.keyboardShowing.value)
        assertTrue(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
    }

    @Test fun imeBlocksEveryOtherwiseEligibleDockWithoutChangingOtherConditions() {
        for (mask in 0 until 64) {
            val app = mask and 1 != 0
            val library = mask and 2 != 0
            val expanded = mask and 4 != 0
            val focused = mask and 8 != 0
            val stack = mask and 16 != 0
            val reveal = mask and 32 != 0
            val before = app && library && !expanded && focused && !stack && !reveal
            assertEquals("Default policy, mask=$mask", before,
                showDockedPlayer(app, library, expanded, focused, stack, reveal))
            assertEquals("IME hidden, mask=$mask", before,
                showDockedPlayer(app, library, expanded, focused, stack, reveal, imeVisible = false))
            assertFalse("IME visible, mask=$mask",
                showDockedPlayer(app, library, expanded, focused, stack, reveal, imeVisible = true))
        }
    }

    @Test fun imeHidesDockAndRestoresItWithoutReleasingItsSurface() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        assertTrue(PlayerWindowVisibility.dockSurfaceReserved.value)

        PlayerWindowVisibility.ime(true)
        assertTrue(PlayerWindowVisibility.keyboardShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        assertTrue(PlayerWindowVisibility.dockSurfaceReserved.value)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        assertFalse(PlayerWindowVisibility.expandedShowing.value)

        PlayerWindowVisibility.ime(false)
        assertFalse(PlayerWindowVisibility.keyboardShowing.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        assertTrue(PlayerWindowVisibility.dockSurfaceReserved.value)
    }

    @Test fun keyboardDismissalDoesNotRestoreDockOnAnotherPage() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.ime(true)
        PlayerWindowVisibility.library(false)
        PlayerWindowVisibility.ime(false)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        assertFalse(PlayerWindowVisibility.dockSurfaceReserved.value)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)

        PlayerWindowVisibility.library(true)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        PlayerWindowVisibility.stackTransport(true)
        PlayerWindowVisibility.ime(true)
        PlayerWindowVisibility.ime(false)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        assertFalse(PlayerWindowVisibility.dockSurfaceReserved.value)
    }

    @Test fun imeDoesNotSuppressDetachedOrExpandedPresentations() {
        PlayerWindowVisibility.ime(true)
        assertTrue(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.expanded(true)
        assertTrue(PlayerWindowVisibility.expandedShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
        assertFalse(PlayerWindowVisibility.dockSurfaceReserved.value)
        PlayerWindowVisibility.ime(false)
        assertTrue(PlayerWindowVisibility.expandedShowing.value)
        assertFalse(PlayerWindowVisibility.dockedVisible.value)
    }

    @Test fun focusedWindowWithoutVisibleImeKeepsTheDock() {
        PlayerWindowVisibility.app(true)
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.windowFocus(false)
        PlayerWindowVisibility.windowFocus(true)
        // Text focus with a hardware keyboard does not emit an IME-visible inset.
        assertFalse(PlayerWindowVisibility.keyboardShowing.value)
        assertTrue(PlayerWindowVisibility.dockedVisible.value)
        assertTrue(PlayerWindowVisibility.dockSurfaceReserved.value)
    }

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
