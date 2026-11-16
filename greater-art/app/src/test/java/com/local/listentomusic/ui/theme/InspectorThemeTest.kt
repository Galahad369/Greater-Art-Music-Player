package com.local.listentomusic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectorThemeTest {
    @Test fun inspectorFollowsAppPaletteForOledAndLightThemes() {
        for (scheme in listOf(
            darkColorScheme(primary = Color(0xffefb8c8), surface = Color.Black, onSurface = Color(0xfffff1f3)),
            lightColorScheme(primary = Color(0xff9b1a5e), surface = Color(0xfffff8fa), onSurface = Color(0xff241820)),
        )) {
            val colors = inspectorThemeColors(scheme)
            assertEquals(scheme.surface, colors.surface)
            assertEquals(scheme.surfaceContainerHigh, colors.raised)
            assertEquals(scheme.onSurface, colors.foreground)
            assertEquals("Do not force a second turquoise theme", scheme.primary, colors.accent)
            assertEquals(scheme.error, colors.error)
            val light = maxOf(colors.surface.luminance(), colors.foreground.luminance())
            val dark = minOf(colors.surface.luminance(), colors.foreground.luminance())
            assertTrue((light + .05f) / (dark + .05f) >= 4.5f)
        }
    }
}
