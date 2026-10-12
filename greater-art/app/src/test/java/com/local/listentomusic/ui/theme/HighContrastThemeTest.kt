package com.local.listentomusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.local.listentomusic.data.ColorTheme
import org.junit.Assert.*
import org.junit.Test

class HighContrastThemeTest {
    private fun ratio(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
    }
    @Test fun lightAndDarkTextControlsAndOutlinesHaveMeasuredContrast() {
        for (dark in listOf(false, true)) {
            val scheme = appColorScheme(ColorTheme.HIGH_CONTRAST, dark)
            assertTrue(ratio(scheme.onSurface, scheme.surface) >= 7f)
            assertTrue(ratio(scheme.onSurfaceVariant, scheme.surfaceVariant) >= 7f)
            assertTrue(ratio(scheme.onPrimary, scheme.primary) >= 7f)
            assertTrue(ratio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 7f)
            assertTrue(ratio(scheme.outline, scheme.surface) >= 3f)
            assertTrue(ratio(scheme.outlineVariant, scheme.surfaceVariant) >= 3f)
            assertTrue(ratio(scheme.primary, scheme.surface) >= 3f)
        }
    }
    @Test fun voidRemainsIndependentlyBlackAndDefaultPaletteUnchanged() {
        assertEquals(Color.Black, appColorScheme(ColorTheme.VOID, false).background)
        assertEquals(Color.Black, appColorScheme(ColorTheme.VOID, true).surface)
        assertEquals(DarkColors.primary, appColorScheme(ColorTheme.FOREST, true).primary)
    }
}
