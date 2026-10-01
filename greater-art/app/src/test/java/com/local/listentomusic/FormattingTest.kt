package com.local.listentomusic

import com.local.listentomusic.ui.formatDuration
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import androidx.media3.common.C
import androidx.media3.common.Player

class FormattingTest {
    @Test
    fun durationResolverIgnoresUnknownAndInvalidCandidates() {
        assertEquals(0L, resolveDurationMs(C.TIME_UNSET, 0L, -4L))
        assertEquals(215_000L, resolveDurationMs(C.TIME_UNSET, 0L, 215_000L, 220_000L))
    }
    @Test fun formatsUnknownDuration() = assertEquals("--:--", formatDuration(0))

    @Test fun formatsMinutesAndSeconds() = assertEquals("2:05", formatDuration(125_000))

    @Test fun formatsHours() = assertEquals("1:02:03", formatDuration(3_723_000))

    @Test fun playbackCycleIncludesRandomInOneStableLoop() {
        assertEquals(CycleMode.ONE, nextCycleMode(CycleMode.OFF))
        assertEquals(CycleMode.ALL, nextCycleMode(CycleMode.ONE))
        assertEquals(CycleMode.RANDOM, nextCycleMode(CycleMode.ALL))
        assertEquals(CycleMode.OFF, nextCycleMode(CycleMode.RANDOM))
        assertEquals(CycleMode.RANDOM, resolveCycleMode(Player.REPEAT_MODE_ALL, random = true))
    }

    @Test fun defaultsToTheSmallestLibraryRow() =
        assertEquals(LibraryRowSize.SMALL, UserPreferences().libraryRowSize)

    @Test fun requestedPlaybackAndPresentationDefaultsRemainStable() {
        val defaults = UserPreferences()
        assertEquals(Player.REPEAT_MODE_ONE, defaults.repeatMode)
        assertEquals(FloatingWindowMode.MINI_WINDOW, defaults.floatingWindowMode)
        assertEquals(ThemeMode.DARK, defaults.themeMode)
        assertEquals(AppLanguage.ENGLISH, defaults.appLanguage)
        assertFalse(defaults.showFileDetails)
        assertEquals(AppBackgroundMode.CURRENT_VIDEO, defaults.backgroundMode)
        assertFalse(defaults.blackDiscMode)
        assertFalse(defaults.playHistoryEnabled)
        assertEquals(0.35f, defaults.backgroundDim, 0.001f)
        assertEquals(
            setOf(SortMode.CUSTOM, SortMode.NAME_ASC, SortMode.NAME_DESC, SortMode.DATE_DESC, SortMode.DATE_ASC),
            SortMode.entries.toSet(),
        )
    }

    @Test fun miniWindowUsesVisibleFootprintAndSquareVariant() {
        assertEquals(103, com.local.listentomusic.model.MiniWindowMetrics.widthPx(1f))
        assertEquals(206, com.local.listentomusic.model.MiniWindowMetrics.widthPx(2f))
        assertEquals(112, com.local.listentomusic.model.MiniWindowMetrics.heightPx(2f))
        assertEquals(56, com.local.listentomusic.model.MiniWindowMetrics.squareWidthPx(1f))
        assertTrue(com.local.listentomusic.model.MiniWindowMetrics.isSquareAspect(1f))
        assertFalse(com.local.listentomusic.model.MiniWindowMetrics.isSquareAspect(16f / 9f))
    }

    @Test fun doubleTapSeekUsesOnlySideZones() {
        assertEquals(-5_000L, com.local.listentomusic.ui.doubleTapSeekDelta(10f, 100f, 5_000L))
        assertNull(com.local.listentomusic.ui.doubleTapSeekDelta(50f, 100f, 5_000L))
        assertEquals(5_000L, com.local.listentomusic.ui.doubleTapSeekDelta(90f, 100f, 5_000L))
    }
}
