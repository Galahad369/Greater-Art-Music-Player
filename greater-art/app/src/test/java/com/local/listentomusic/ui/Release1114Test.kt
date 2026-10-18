package com.local.listentomusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.luminance
import com.local.listentomusic.data.*
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.ui.components.ambientBottomColor
import com.local.listentomusic.ui.components.artworkGradientColors
import org.junit.Assert.*
import org.junit.Test

class Release1114Test {
    @Test fun defaultsComeFirstWithoutLosingOptions() {
        assertEquals(ThemeMode.DARK, defaultFirst(ThemeMode.entries, ThemeMode.DARK).first())
        assertEquals(FloatingWindowMode.MINI_WINDOW, defaultFirst(FloatingWindowMode.entries, FloatingWindowMode.MINI_WINDOW).first())
        assertEquals(AppBackgroundMode.CURRENT_VIDEO, defaultFirst(AppBackgroundMode.entries, AppBackgroundMode.CURRENT_VIDEO).first())
        assertEquals(BackgroundScaleMode.CROP, defaultFirst(BackgroundScaleMode.entries, BackgroundScaleMode.CROP).first())
        assertEquals(playbackSpeeds.toSet(), defaultFirst(playbackSpeeds, 1f).toSet())
        assertEquals(1f, defaultFirst(playbackSpeeds, 1f).first())
    }

    @Test fun graphAndSafetyCopyExistForEveryLanguage() {
        for (language in AppLanguage.entries) for (key in additionalTranslations.keys) {
            val actual = uiText(language, key, "MISSING")
            assertTrue("$language $key", actual.isNotBlank())
            assertNotEquals("MISSING", actual)
        }
        assertEquals("関係", uiText(AppLanguage.TRADITIONAL_CHINESE, "Unregistered", "関係"))
        assertEquals("関連グラフ", uiText(AppLanguage.JAPANESE, "Nodes", "關聯圖"))
        assertEquals("關聯圖", uiText(AppLanguage.CANTONESE, "Nodes", "Nodes"))
        assertEquals("Verbindungen", uiText(AppLanguage.GERMAN, "Nodes", "Nodes"))
        assertEquals("Connexions", uiText(AppLanguage.FRENCH, "Nodes", "Nodes"))
    }

    @Test fun artworkColorsKeepTextReadableEvenForExtremeArt() {
        for (pixel in listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())) {
            val dark = artworkGradientColors(intArrayOf(pixel), false)
            val light = artworkGradientColors(intArrayOf(pixel), true)
            dark.forEach { assertTrue("white on $it", 1.05f / (it.luminance() + .05f) >= 4.5f) }
            light.forEach { assertTrue("black on $it", (it.luminance() + .05f) / .05f >= 4.5f) }
        }
    }

    @Test fun nowPlayingAmbientKeepsArtworkTintAtTheBottom() {
        val colors = artworkGradientColors(intArrayOf(0xFFFF0000.toInt()), false)
        val plainBottom = ambientBottomColor(colors, 0f)
        val extendedBottom = ambientBottomColor(colors, NOW_PLAYING_AMBIENT_BOTTOM_BLEND)
        assertEquals(colors.last(), plainBottom)
        assertNotEquals(colors.last(), extendedBottom)
        assertTrue(NOW_PLAYING_AMBIENT_BOTTOM_BLEND in 0f..1f)
        assertTrue(NOW_PLAYING_AMBIENT_PANEL_ALPHA in 0f..0.99f)
        assertTrue(NOW_PLAYING_AMBIENT_ROW_ALPHA in 0f..NOW_PLAYING_AMBIENT_PANEL_ALPHA)
        assertTrue(NOW_PLAYING_AMBIENT_OUTLINE_ALPHA in 0f..1f)
        assertTrue(NOW_PLAYING_ART_STAGE_ALPHA in 0f..NOW_PLAYING_AMBIENT_PANEL_ALPHA)
    }

    @Test fun fullscreenVideoLockFollowsPlaybackControlVisibility() {
        assertTrue(shouldShowPlayerLock(immersiveVideo = true, videoControlsVisible = true))
        assertFalse(shouldShowPlayerLock(immersiveVideo = true, videoControlsVisible = false))
        // The audio/portrait lock remains independently accessible.
        assertTrue(shouldShowPlayerLock(immersiveVideo = false, videoControlsVisible = true))
        assertTrue(shouldShowPlayerLock(immersiveVideo = false, videoControlsVisible = false))
    }

    @Test fun fullscreenToolbarAdaptsToCutoutSafeWidthWithoutClippingActions() {
        // Layout width excludes the display-cutout inset before these thresholds.
        val wide = toolbarActionVisibility(400f, hasRotate = true)
        assertEquals(ToolbarActionVisibility(true, true, true), wide)
        val normal = toolbarActionVisibility(320f, hasRotate = true)
        assertEquals(ToolbarActionVisibility(true, true, false), normal)
        val narrow = toolbarActionVisibility(280f, hasRotate = true)
        assertEquals(ToolbarActionVisibility(false, true, false), narrow)
        val cutout = toolbarActionVisibility(240f, hasRotate = true)
        assertEquals(ToolbarActionVisibility(false, false, false), cutout)
        assertEquals(ToolbarActionVisibility(true, true, true),
            toolbarActionVisibility(320f, hasRotate = false))
        assertEquals(ToolbarActionVisibility(false, false, false),
            toolbarActionVisibility(185f, hasRotate = false))
    }

    @Test fun videoPanClampsToFittedPictureRatherThanBlackLetterboxes() {
        val viewport = IntSize(1000, 600)
        // A 4:3 video fits to 800x600: no x-axis movement until zoom > 1.25.
        assertEquals(Offset.Zero, fittedVideoPanLimit(viewport, 4f / 3f, 1f))
        val first = fittedVideoPanLimit(viewport, 4f / 3f, 1.2f)
        assertEquals(0f, first.x, 0.01f)
        assertEquals(60f, first.y, 0.01f)
        val second = fittedVideoPanLimit(viewport, 4f / 3f, 2f)
        assertEquals(300f, second.x, 0.01f)
        assertEquals(300f, second.y, 0.01f)
        assertEquals(Offset(300f, -300f),
            clampVideoPan(Offset(2000f, -900f), viewport, 4f / 3f, 2f))
        assertEquals(Offset.Zero, clampVideoPan(Offset(50f, 50f), viewport, 4f / 3f, 1f))
        // Guard uninitialized and corrupt aspect ratio/scale without NaN transforms.
        assertEquals(Offset.Zero, fittedVideoPanLimit(IntSize.Zero, 1.78f, 2f))
        assertEquals(Offset.Zero, fittedVideoPanLimit(viewport, Float.NaN, 2f))
        assertEquals(Offset.Zero, fittedVideoPanLimit(viewport, 1f, Float.POSITIVE_INFINITY))
    }

    @Test fun playerLockUsesMeasuredAnchorCoordinatesInsteadOfButtonCountOffsets() {
        val root = androidx.compose.ui.geometry.Rect(20f, 40f, 420f, 840f)
        val slot = androidx.compose.ui.geometry.Rect(272f, 58f, 320f, 106f)
        assertEquals(
            androidx.compose.ui.unit.IntOffset(252, 18),
            playerLockLocalOffset(root, slot),
        )
        assertNull(playerLockLocalOffset(root, null))
        assertNull(playerLockLocalOffset(null, slot))
    }

    @Test fun nodeDragNeedsMeaningfulNetMovementNotOrdinaryFingerJitter() {
        assertEquals(20f, nodeDragActivationDistancePx(8f, 20f), 0f)
        assertEquals(24f, nodeDragActivationDistancePx(16f, 20f), 0f)
        assertEquals(20f, nodeDragActivationDistancePx(0f, 20f), 0f)
    }

    @Test fun nowPlayingMetadataUsesArtistAndAlbumWithoutDuplicateNoise() {
        fun file(artist: String, album: String) = MediaFile(
            path = "/tmp/song.mp3",
            name = "song.mp3",
            modifiedMs = 0L,
            sizeBytes = 1L,
            durationMs = 1L,
            kind = MediaKind.AUDIO,
            artist = artist,
            album = album,
        )
        assertEquals("Artist · Album", nowPlayingMetadataLine(file("Artist", "Album")))
        assertEquals("Artist", nowPlayingMetadataLine(file("Artist", "Artist")))
        assertEquals("Album", nowPlayingMetadataLine(file("", "Album")))
        assertEquals("", nowPlayingMetadataLine(null))
    }

    @Test fun missingOrTransparentArtUsesNeutralThemeFallback() {
        assertEquals(artworkGradientColors(intArrayOf(), false), artworkGradientColors(intArrayOf(0x000000FF), false))
        assertTrue(artworkGradientColors(intArrayOf(), true).first().luminance() > .8f)
        val red = artworkGradientColors(intArrayOf(0xFFFF0000.toInt()), false).first()
        assertTrue(red.red > red.blue)
    }
}
