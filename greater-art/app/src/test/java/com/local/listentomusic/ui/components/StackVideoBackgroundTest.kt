package com.local.listentomusic.ui.components

import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.StackSession
import com.local.listentomusic.playback.StackSlot
import org.junit.Assert.*
import org.junit.Test

class StackVideoBackgroundTest {
    @Test fun fitTilingIsExplicitAndNeverRunsBehindHiddenOrBackgroundedActivity() {
        fun active(mode: AppBackgroundMode = AppBackgroundMode.CURRENT_VIDEO, scale: BackgroundScaleMode = BackgroundScaleMode.FIT,
            visible: Boolean = true, foreground: Boolean = true, allowed: Boolean = true, controller: Boolean = true, count: Int = 6) =
            shouldTileStackBackground(mode, scale, visible, foreground, allowed, controller, count)
        assertTrue(active())
        assertFalse(active(scale = BackgroundScaleMode.CROP))
        assertFalse(active(mode = AppBackgroundMode.CUSTOM_VIDEO))
        assertFalse(active(visible = false))
        assertFalse(active(foreground = false))
        assertFalse(active(allowed = false))
        assertFalse(active(controller = false))
        assertFalse(active(count = 1))
    }

    @Test fun tileIdentityIgnoresAudioTicksAndKeepsUnavailableVideoSeparateFromAudioFailure() {
        fun file(path: String, kind: MediaKind) = MediaFile(path, path, 0, 0, 0, kind)
        val slots = listOf(StackSlot(file("primary", MediaKind.VIDEO)),
            StackSlot(file("companion", MediaKind.VIDEO), videoUnavailable = true),
            StackSlot(file("audio", MediaKind.AUDIO)), StackSlot(file("failed", MediaKind.VIDEO), error = "Playback unavailable"))
        val session = StackSession(slots, "primary")
        assertEquals(listOf(StackVideoTile("primary", true, false), StackVideoTile("companion", false, true)), stackVideoTiles(session))
        assertEquals(stackVideoTiles(session), stackVideoTiles(session.copy(positionMs = 123_000, maxDriftMs = 50)))
    }

    @Test fun adaptiveFitGridAlwaysHasBoundedColumnsOnPortraitAndLandscape() {
        for (count in 1..8) for (aspect in listOf(1080f / 2340f, 2340f / 1080f, 1f, Float.NaN)) {
            assertTrue(stackTileColumns(count, aspect) in 1..minOf(count, 4))
        }
        assertEquals(1, stackTileColumns(2, 1080f / 2340f))
        assertEquals(2, stackTileColumns(2, 2340f / 1080f))
    }
}
