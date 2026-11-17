package com.local.listentomusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoOverlayContrastTest {
    @Test fun transportGlyphsStayReadableOnBothVideoExtremes() {
        for (video in listOf(Color.White, Color.Black)) {
            for (plate in listOf(GaVideoOverlay.control, GaVideoOverlay.playSurface)) {
                assertTrue("Fullscreen transport contrast must be at least 3:1 over $video; plate=$plate",
                    accentContrast(GaVideoOverlay.foreground, plate.compositeOver(video)) >= 3f)
            }
        }
    }

    @Test fun videoControlsUseThemeOnlyWhenReadableOnBrightAndDarkFrames() {
        val themed = Color(0xFFCCFFDD)
        assertEquals(themed, videoOverlayAccent(themed))
        assertEquals(GaVideoOverlay.foreground, videoOverlayAccent(Color.Black))
        for (accent in listOf(themed, Color.Black, Color(0xFF4477BB), Color.White)) {
            val selected = videoOverlayAccent(accent)
            for (frame in listOf(Color.Black, Color.White)) {
                assertTrue("Theme-aware glyph must remain readable over $frame",
                    accentContrast(selected, GaVideoOverlay.control.compositeOver(frame)) >= 3f)
            }
        }
    }

    @Test fun existingToolbarAndVideoScrimStayUnchanged() {
        assertEquals(Color.Black.copy(alpha = .44f), GaVideoOverlay.scrim)
        assertEquals(Color(0xFFF6F7F4), GaVideoOverlay.foreground)
    }
}
