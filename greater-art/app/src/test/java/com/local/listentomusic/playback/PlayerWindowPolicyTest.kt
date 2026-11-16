package com.local.listentomusic.playback

import android.view.WindowManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerWindowPolicyTest {
    @Test fun clearingReadinessCannotRestoreAnExpandedWindowBeforeReturnSelectsMini() = runBlocking {
        assertEquals(emptyList<Boolean>(), flowOf(false).fullscreenFrameCommits().toList())
    }

    @Test fun onlyNewRenderedFramesCommitAcrossRepeatedFullscreenVisits() = runBlocking {
        assertEquals(listOf(true, true), flowOf(false, true, false, true, false).fullscreenFrameCommits().toList())
    }

    @Test fun expandedFrameCoversFullscreenStartupWithoutTakingInput() {
        assertEquals("Keep the outgoing frame until the destination renders",
            1f, playerWindowVisualAlpha(visible = false, expanded = true,
                fullscreenActivity = true, fullscreenFrameReady = false), 0f)
        val flags = playerWindowInputFlags(0, expanded = true, visible = false)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(0f, playerWindowDimAmount(true, false), 0f)
    }

    @Test fun firstFullscreenFrameRemovesCoverAndOtherHiddenModesStayHidden() {
        assertEquals(0f, playerWindowVisualAlpha(false, true, true, true), 0f)
        assertEquals(0f, playerWindowVisualAlpha(false, false, true, false), 0f)
        assertEquals(0f, playerWindowVisualAlpha(false, true, false, false), 0f)
        assertEquals(1f, playerWindowVisualAlpha(true, true, false, false), 0f)
    }

    @Test fun hiddenExpandedWindowCannotDimOrTakeInputFromFullscreen() {
        val flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        assertEquals(0f, playerWindowDimAmount(expanded = true, visible = false), 0f)
        val hidden = playerWindowInputFlags(flags, expanded = true, visible = false)
        assertTrue(hidden and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertTrue(hidden and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertEquals(0, hidden and WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    @Test fun visibleExpandedWindowRestoresFocusAndItsOriginalDim() {
        val hidden = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val shown = playerWindowInputFlags(hidden, expanded = true, visible = true)
        assertEquals(0, shown and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        assertEquals(0, shown and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        assertTrue(shown and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
        assertEquals(.14f, playerWindowDimAmount(expanded = true, visible = true), 0f)
    }

    @Test fun visibleCompactWindowKeepsTouchesWithoutTakingKeyboardFocus() {
        val stale = WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val mini = playerWindowInputFlags(stale, expanded = false, visible = true)
        assertEquals(0, mini and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        assertTrue(mini and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(0, mini and WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        assertEquals(0f, playerWindowDimAmount(expanded = false, visible = true), 0f)
    }

    @Test fun repeatedVisibilityUpdatesPreserveGeometryAndAwakeFlags() {
        val preserved = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        var flags = preserved
        repeat(1_000) { index ->
            val visible = index % 2 == 0
            flags = playerWindowInputFlags(flags, expanded = true, visible = visible)
            assertEquals(preserved, flags and preserved)
            assertEquals(if (visible) .14f else 0f, playerWindowDimAmount(true, visible), 0f)
        }
    }
}
