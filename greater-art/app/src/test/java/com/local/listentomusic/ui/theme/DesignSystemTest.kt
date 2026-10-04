package com.local.listentomusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {
    @Test fun everyPaletteOwnsItsContainersAndReadableText() {
        for (theme in com.local.listentomusic.data.ColorTheme.entries) for (dark in listOf(false, true)) {
            val scheme = appColorScheme(theme, dark)
            assertTrue(contrast(scheme.onBackground, scheme.background) >= 4.5f)
            assertTrue(contrast(scheme.onSecondaryContainer, scheme.secondaryContainer) >= 4.5f)
            assertTrue(contrast(scheme.onSurface, scheme.surfaceContainerHigh) >= 4.5f)
            org.junit.Assert.assertEquals(scheme.onSurface, scheme.onBackground)
            org.junit.Assert.assertEquals(scheme.surfaceVariant, scheme.surfaceContainerHighest)
        }
    }
    @Test fun repeatedIconActionsKeepAndroidSafeTouchTargets() {
        assertTrue(GaControl.touchTarget >= 48.dp)
        assertTrue(GaControl.hero >= GaControl.touchTarget)
    }

    @Test fun motionScaleIsOrderedAndRestrained() {
        assertTrue(GaMotion.quickMs in 80..160)
        assertTrue(GaMotion.standardMs > GaMotion.quickMs)
        assertTrue(GaMotion.emphasizedMs > GaMotion.standardMs)
        assertTrue(GaMotion.emphasizedMs <= 320)
    }

    @Test fun secondaryTextMeetsReadableContrastOnThemeSurfaces() {
        assertTrue(contrast(LightColors.onSurfaceVariant, LightColors.surface) >= 4.5f)
        assertTrue(contrast(DarkColors.onSurfaceVariant, DarkColors.surface) >= 4.5f)
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
