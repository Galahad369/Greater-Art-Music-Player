package com.local.listentomusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidOledPaletteTest {
    @Test fun voidAlwaysChoosesBlackSurfaceEvenUnderLightAppearance() {
        val dark = appColorScheme(ColorTheme.VOID, dark = true)
        val light = appColorScheme(ColorTheme.VOID, dark = false)
        assertEquals(dark.background, light.background)
        assertEquals(dark.surface, light.surface)
        assertEquals(dark.onSurface, light.onSurface)
        assertEquals(Color.Black, dark.background)
        assertEquals(Color.Black, dark.surface)
        assertEquals(Color.Black, dark.surfaceContainerLowest)
        assertEquals(Color.Black, dark.surfaceContainerLow)
        assertEquals(Color.Black, dark.surfaceDim)
    }

    @Test fun voidKeepsReadableRaisedSurfacesAndControlOutlines() {
        val colors = appColorScheme(ColorTheme.VOID, dark = true)
        assertNotEquals(Color.Black, colors.surfaceVariant)
        assertNotEquals(Color.Black, colors.surfaceContainerHigh)
        assertNotEquals(Color.Black, colors.surfaceContainerHighest)
        assertNotEquals(colors.surface, colors.onSurface)
        assertNotEquals(colors.surfaceVariant, colors.outline)
        assertEquals(Color.White, colors.onPrimaryContainer)
        assertEquals(Color.Transparent, colors.surfaceTint)
    }

    @Test fun spaceBlackAndExistingPreferencesRemainUnchanged() {
        assertEquals(ColorTheme.FOREST, UserPreferences().colorTheme)
        assertTrue(ColorTheme.entries.contains(ColorTheme.VOID))
        assertNotEquals(Color.Black, appColorScheme(ColorTheme.MONOCHROME, true).background)
        assertNotEquals(Color.Black, appColorScheme(ColorTheme.FOREST, true).background)
        assertNotEquals(
            appColorScheme(ColorTheme.MONOCHROME, false).background,
            appColorScheme(ColorTheme.VOID, false).background,
        )
    }
}
