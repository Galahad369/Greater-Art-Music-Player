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

    @Test fun existingToolbarAndVideoScrimStayUnchanged() {
        assertEquals(Color.Black.copy(alpha = .44f), GaVideoOverlay.scrim)
        assertEquals(Color(0xFFF6F7F4), GaVideoOverlay.foreground)
    }
}
