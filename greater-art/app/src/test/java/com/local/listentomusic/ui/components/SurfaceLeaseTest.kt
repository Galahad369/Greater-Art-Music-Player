package com.local.listentomusic.ui.components

import org.junit.Assert.*
import org.junit.Test

class SurfaceLeaseTest {
    @Test fun overlayVisibilityUsesCanonicalFullscreenState() {
        assertTrue(shouldShowPlayerWindow(
            ready = true,
            fullscreenActivity = false,
            expanded = true,
            docked = false,
            dockedVisible = false,
            detachedVisible = false,
        ))
        assertFalse(shouldShowPlayerWindow(
            ready = true,
            fullscreenActivity = true,
            expanded = true,
            docked = false,
            dockedVisible = true,
            detachedVisible = true,
        ))
        assertFalse(shouldShowPlayerWindow(
            ready = false,
            fullscreenActivity = false,
            expanded = true,
            docked = false,
            dockedVisible = true,
            detachedVisible = true,
        ))
    }

    @Test fun miniWindowStaysVisibleWhileNowPlayingSurfaceRegisters() {
        assertTrue(shouldRetainPrimarySurfaceDuringHandoff(
            currentOwner = "MINI_WINDOW",
            expectedOwner = "NOW_PLAYING",
            expectedCandidateReady = false,
        ))
        assertFalse(shouldRetainPrimarySurfaceDuringHandoff(
            currentOwner = "MINI_WINDOW",
            expectedOwner = "NOW_PLAYING",
            expectedCandidateReady = true,
        ))
    }

    @Test fun nowPlayingAlsoStaysVisibleWhileMiniSurfaceRegisters() {
        assertTrue(shouldRetainPrimarySurfaceDuringHandoff(
            currentOwner = "NOW_PLAYING",
            expectedOwner = "MINI_WINDOW",
            expectedCandidateReady = false,
        ))
        assertFalse(shouldRetainPrimarySurfaceDuringHandoff(
            currentOwner = "NOW_PLAYING",
            expectedOwner = "MINI_WINDOW",
            expectedCandidateReady = true,
        ))
    }

    @Test fun handoffHoldDoesNotApplyToUnrelatedDestinations() {
        assertTrue(shouldRetainPrimarySurfaceDuringHandoff(
            currentOwner = "MINI_WINDOW",
            expectedOwner = "LIBRARY_MINI",
            expectedCandidateReady = false,
        ))
        assertFalse(shouldRetainPrimarySurfaceDuringHandoff(
            currentOwner = "LIBRARY_MINI",
            expectedOwner = "NOW_PLAYING",
            expectedCandidateReady = false,
        ))
    }
    @Test fun presentationPriorityIncludesForegroundAndSystemPip() {
        assertEquals("LIBRARY_MINI", expectedSurfaceOwner(true, false, false))
        assertEquals("NOW_PLAYING", expectedSurfaceOwner(true, true, false))
        assertEquals("MINI_WINDOW", expectedSurfaceOwner(false, true, false))
        assertEquals("NOW_PLAYING", expectedSurfaceOwner(false, true, true))
    }

    @Test fun fullscreenActivityKeepsNowPlayingOwnerAcrossTransientPause() {
        assertEquals(
            "NOW_PLAYING",
            expectedSurfaceOwner(
                foreground = false,
                nowPlaying = true,
                pip = false,
                fullscreenActivity = true,
                systemOverlayOwner = "MINI_WINDOW",
            ),
        )
    }

    @Test fun ordinarySystemOverlayStillOutranksForegroundActivityPresentation() {
        assertEquals(
            "MINI_WINDOW",
            expectedSurfaceOwner(
                foreground = true,
                nowPlaying = true,
                pip = false,
                fullscreenActivity = false,
                systemOverlayOwner = "MINI_WINDOW",
            ),
        )
    }
    @Test fun staleFrameFromPreviousMediaDoesNotSetMediaFlag() {
        val state = SurfaceLease().attach("NOW_PLAYING", 1, 100).mediaChanged(200)
        assertFalse(state.frame(150, true).mediaFirstFrame)
        assertTrue(state.frame(201, true).mediaFirstFrame)
    }
    @Test fun staleReleaseCannotInvalidateIncomingGeneration() {
        val library = SurfaceLease().attach("LIBRARY_MINI", 1, 100).frame(110, true)
        val playing = library.attach("NOW_PLAYING", 2, 200)
        val afterRelease = playing.detach(1, 210)
        assertEquals("NOW_PLAYING", afterRelease.owner)
        assertEquals(playing.generation, afterRelease.generation)
        assertEquals(2, afterRelease.view)
        assertEquals(1, afterRelease.staleDetaches)
        assertFalse(afterRelease.firstFrame)
        assertTrue(afterRelease.frame(220, true).firstFrame)
    }
    @Test fun oldFrameCannotSatisfyNewLease() {
        val lease = SurfaceLease().attach("NOW_PLAYING", 2, 200)
        assertFalse(lease.frame(199, true).firstFrame)
        assertFalse(lease.frame(201, false).firstFrame)
        assertTrue(lease.frame(201, true).firstFrame)
    }
    @Test fun mediaAndPresentationFrameStatesAreDifferent() {
        val first = SurfaceLease().attach("LIBRARY_MINI", 1, 100).frame(110, true)
        val second = first.attach("NOW_PLAYING", 2, 200)
        assertTrue(second.mediaFirstFrame)
        assertFalse(second.firstFrame)
        assertEquals("LIBRARY_MINI", second.lastFrameOwner)
        assertFalse(second.mediaChanged(300).mediaFirstFrame)
    }
    @Test fun tenTransitionsSurviveLateReleaseCallbacks() {
        var state = SurfaceLease()
        repeat(10) { cycle ->
            val previous = state.view
            state = state.attach(if (cycle % 2 == 0) "LIBRARY_MINI" else "NOW_PLAYING", cycle + 1, cycle * 100L)
            state = state.detach(previous, cycle * 100L + 1)
            state = state.frame(cycle * 100L + 2, true)
            assertEquals(cycle + 1, state.view)
            assertTrue(state.firstFrame)
        }
    }
    @Test fun warningReasonsAllowTransitionGraceTime() {
        val state = SurfaceLease().attach("LIBRARY_MINI", 1, 100)
        assertTrue(state.warnings("NOW_PLAYING", true, 200).isEmpty())
        assertTrue(state.warnings("NOW_PLAYING", false, 4000).isEmpty())
        assertEquals(listOf("SURFACE_OWNER_MISMATCH", "READY_VIDEO_NO_FRAME", "FIRST_FRAME_TIMEOUT"), state.warnings("NOW_PLAYING", true, 4000))
    }
    @Test fun identicalAttachIsDiagnosticNoOp() {
        val first = SurfaceLease().attach("NOW_PLAYING", 7, 100, 9, "first")
        val second = first.attach("NOW_PLAYING", 7, 200, 9, "recompose")
        assertEquals(first.generation, second.generation)
        assertEquals(first.sinceMs, second.sinceMs)
        assertEquals(1, second.noOpAttaches)
    }
    @Test fun controllerFirstFramePreventsFalseBlackFrameWarning() {
        val state = SurfaceLease().attach("NOW_PLAYING", 1, 100)
        assertTrue(state.warnings("NOW_PLAYING", true, 4000, controllerMediaFirstFrame = true).isEmpty())
    }
    @Test fun lateMediaTransitionKeepsFrameRenderedAfterTransitionTime() {
        val rendered = SurfaceLease().attach("MINI_WINDOW", 1, 100, 9).frame(250, true)
        val transitioned = rendered.mediaChanged(200)
        assertEquals(rendered.generation + 1, transitioned.generation)
        assertTrue(transitioned.firstFrame)
        assertTrue(transitioned.mediaFirstFrame)
        assertEquals(transitioned.generation, transitioned.lastFrameGeneration)
        assertEquals(250L, transitioned.lastFrameAtMs)
        assertEquals(true, transitioned.framesByOwner["MINI_WINDOW"])
        assertTrue(transitioned.warnings("MINI_WINDOW", true, 3000).isEmpty())
    }
    @Test fun frameBeforeMediaTransitionIsNotCarriedForward() {
        val rendered = SurfaceLease().attach("MINI_WINDOW", 1, 100, 9).frame(150, true)
        val transitioned = rendered.mediaChanged(200)
        assertFalse(transitioned.firstFrame)
        assertFalse(transitioned.mediaFirstFrame)
        assertEquals(rendered.lastFrameGeneration, transitioned.lastFrameGeneration)
        assertEquals(150L, transitioned.lastFrameAtMs)
    }
    @Test fun ambientIgnoresBlackVideoBorders() {
        val bordered = IntArray(100) { if (it < 90) 0xFF000000.toInt() else 0xFFFF0000.toInt() }
        assertEquals(artworkGradientColors(intArrayOf(0xFFFF0000.toInt()), false), artworkGradientColors(bordered, false))
    }
}
