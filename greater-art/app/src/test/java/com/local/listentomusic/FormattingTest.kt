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

    @Test fun everyLibraryRowSizeUsesTheImmutableMiniWindowThumbnailFootprint() {
        com.local.listentomusic.data.LibraryRowSize.entries.forEach { rowSize ->
            assertEquals(
                com.local.listentomusic.model.MiniWindowMetrics.WIDTH_DP to
                    com.local.listentomusic.model.MiniWindowMetrics.HEIGHT_DP,
                com.local.listentomusic.ui.libraryThumbnailSizeDp(rowSize),
            )
        }
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

    @Test fun sideDoubleTapRequiresSameSideTwice() {
        val left = com.local.listentomusic.ui.SeekSide.LEFT
        val right = com.local.listentomusic.ui.SeekSide.RIGHT
        val window = com.local.listentomusic.ui.SIDE_DOUBLE_TAP_MS
        assertFalse(com.local.listentomusic.ui.sideDoubleTapSeeks(left, 1_000L, null, 0L))
        assertFalse(com.local.listentomusic.ui.sideDoubleTapSeeks(right, 1_200L, left, 1_000L))
        assertTrue(com.local.listentomusic.ui.sideDoubleTapSeeks(left, 1_200L, left, 1_000L))
        assertTrue(com.local.listentomusic.ui.sideDoubleTapSeeks(left, 1_000L + window, left, 1_000L))
        assertFalse(com.local.listentomusic.ui.sideDoubleTapSeeks(left, 1_001L + window, left, 1_000L))
        assertFalse(com.local.listentomusic.ui.sideDoubleTapSeeks(null, 1_200L, left, 1_000L))
        assertFalse(com.local.listentomusic.ui.sideDoubleTapSeeks(left, 1_000L, left, 1_000L))
    }

    @Test fun heldDoubleSpeedLocksOnlyAfterDeliberateDownwardPull() {
        assertFalse(com.local.listentomusic.ui.shouldLockHeldDoubleSpeed(71f, 72f))
        assertTrue(com.local.listentomusic.ui.shouldLockHeldDoubleSpeed(72f, 72f))
        assertTrue(com.local.listentomusic.ui.shouldLockHeldDoubleSpeed(120f, 72f))
        assertFalse(com.local.listentomusic.ui.shouldLockHeldDoubleSpeed(-120f, 72f))
        assertFalse(com.local.listentomusic.ui.shouldLockHeldDoubleSpeed(120f, 0f))
    }

    @Test fun twoXSpeedIsRecognizedForHoldToUnlock() {
        assertTrue(com.local.listentomusic.ui.isDoubleSpeed(2f))
        assertTrue(com.local.listentomusic.ui.isDoubleSpeed(2.005f))
        assertFalse(com.local.listentomusic.ui.isDoubleSpeed(1.98f))
        assertFalse(com.local.listentomusic.ui.isDoubleSpeed(2.5f))
    }

    @Test fun thumbnailDigestHexIsStableAndUnsigned() {
        assertEquals(
            "00017f80ff",
            com.local.listentomusic.data.thumbnailDigestHex(
                byteArrayOf(0x00, 0x01, 0x7F, 0x80.toByte(), 0xFF.toByte()),
            ),
        )
    }

    @Test fun persistentMissingArtworkRequiresAStableAudioNoArtworkResult() {
        val audio = com.local.listentomusic.model.MediaKind.AUDIO
        val video = com.local.listentomusic.model.MediaKind.VIDEO
        assertTrue(com.local.listentomusic.data.shouldPersistMissingArtwork(audio, "", true, false))
        assertFalse(com.local.listentomusic.data.shouldPersistMissingArtwork(video, "", true, false))
        assertFalse(com.local.listentomusic.data.shouldPersistMissingArtwork(audio, "content://cover", true, false))
        assertFalse(com.local.listentomusic.data.shouldPersistMissingArtwork(audio, "", false, false))
        assertFalse(com.local.listentomusic.data.shouldPersistMissingArtwork(audio, "", true, true))
        assertEquals("abc.missing", com.local.listentomusic.data.thumbnailMissingMarkerName("abc"))
    }

    @Test fun thumbnailWorkersReduceBitmapFanoutOnConstrainedDevices() {
        val mib = 1024L * 1024L
        assertEquals(
            com.local.listentomusic.data.ThumbnailWorkerPolicy(1, 2),
            com.local.listentomusic.data.thumbnailWorkerPolicy(lowRamDevice = true, maxHeapBytes = 512L * mib),
        )
        assertEquals(
            com.local.listentomusic.data.ThumbnailWorkerPolicy(1, 2),
            com.local.listentomusic.data.thumbnailWorkerPolicy(lowRamDevice = false, maxHeapBytes = 256L * mib),
        )
        assertEquals(
            com.local.listentomusic.data.ThumbnailWorkerPolicy(2, 3),
            com.local.listentomusic.data.thumbnailWorkerPolicy(lowRamDevice = false, maxHeapBytes = 512L * mib),
        )
    }

    @Test fun thumbnailMemoryPressureTrimsRamWithoutChangingDiskPolicy() {
        val maxKb = 24_000
        assertNull(com.local.listentomusic.data.thumbnailTrimTargetKb(maxKb, 5))
        assertEquals(12_000, com.local.listentomusic.data.thumbnailTrimTargetKb(maxKb, 10))
        assertEquals(6_000, com.local.listentomusic.data.thumbnailTrimTargetKb(maxKb, 15))
        assertEquals(12_000, com.local.listentomusic.data.thumbnailTrimTargetKb(maxKb, 20))
        assertEquals(0, com.local.listentomusic.data.thumbnailTrimTargetKb(maxKb, 40))
        assertEquals(0, com.local.listentomusic.data.thumbnailTrimTargetKb(maxKb, 80))
    }

    @Test fun thumbnailRamBudgetRemainsBoundedByHeap() {
        val mib = 1024L * 1024L
        assertEquals(8_192, com.local.listentomusic.data.thumbnailMemoryBudgetKb(64L * mib))
        assertEquals(32_768, com.local.listentomusic.data.thumbnailMemoryBudgetKb(384L * mib))
        assertEquals(65_536, com.local.listentomusic.data.thumbnailMemoryBudgetKb(2L * 1024L * mib))
    }

    @Test fun immersiveFullscreenRehidesAnyVisibleSystemBar() {
        assertTrue(com.local.listentomusic.ui.shouldRehideImmersiveBars(true, true, false))
        assertTrue(com.local.listentomusic.ui.shouldRehideImmersiveBars(true, false, true))
        assertTrue(com.local.listentomusic.ui.shouldRehideImmersiveBars(true, true, true))
        assertFalse(com.local.listentomusic.ui.shouldRehideImmersiveBars(true, false, false))
        assertFalse(com.local.listentomusic.ui.shouldRehideImmersiveBars(false, true, true))
    }

    @Test fun immersiveFullscreenUsesCutoutSpaceWhenAndroidSupportsIt() {
        assertNull(com.local.listentomusic.ui.immersiveCutoutModeForSdk(27))
        assertEquals(
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
            com.local.listentomusic.ui.immersiveCutoutModeForSdk(28),
        )
        assertEquals(
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            com.local.listentomusic.ui.immersiveCutoutModeForSdk(30),
        )
    }

    @Test fun landscapeVideoNowPlayingUsesImmersiveOverlayInsets() {
        assertTrue(com.local.listentomusic.playback.expandedPlayerUsesImmersiveWindow(
            expanded = true, explicitFullscreen = false, video = true, landscape = true,
        ))
        assertTrue(com.local.listentomusic.playback.expandedPlayerUsesImmersiveWindow(
            expanded = true, explicitFullscreen = true, video = true, landscape = false,
        ))
        assertFalse(com.local.listentomusic.playback.expandedPlayerUsesImmersiveWindow(
            expanded = true, explicitFullscreen = false, video = true, landscape = false,
        ))
        assertFalse(com.local.listentomusic.playback.expandedPlayerUsesImmersiveWindow(
            expanded = true, explicitFullscreen = false, video = false, landscape = true,
        ))
        assertFalse(com.local.listentomusic.playback.expandedPlayerUsesImmersiveWindow(
            expanded = false, explicitFullscreen = true, video = true, landscape = true,
        ))
    }

    @Test fun landscapeNowPlayingCutoutModeTracksImmersiveState() {
        assertNull(com.local.listentomusic.playback.expandedPlayerCutoutMode(27, immersive = true))
        assertEquals(
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
            com.local.listentomusic.playback.expandedPlayerCutoutMode(28, immersive = true),
        )
        assertEquals(
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            com.local.listentomusic.playback.expandedPlayerCutoutMode(30, immersive = true),
        )
        assertEquals(
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT,
            com.local.listentomusic.playback.expandedPlayerCutoutMode(30, immersive = false),
        )
    }
}
