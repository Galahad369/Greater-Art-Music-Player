package com.local.listentomusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.local.listentomusic.data.ColorTheme
import org.junit.Assert.*
import org.junit.Test

class ArtworkAccentTest {
    private val vivid = listOf(0xffffd000.toInt(), 0xffe00030.toInt(), 0xff2040ff.toInt())

    @Test fun disabledInvalidAndFixedThemesAreExactIdentity() {
        val base = appColorScheme(ColorTheme.FOREST, true)
        for (amount in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY))
            assertSame(base, artworkAccentScheme(base, vivid, amount, ColorTheme.FOREST))
        for (theme in listOf(ColorTheme.HIGH_CONTRAST, ColorTheme.MONOCHROME))
            assertSame(base, artworkAccentScheme(base, vivid, 1f, theme))
        assertSame(base, artworkAccentScheme(base, emptyList(), 1f, ColorTheme.FOREST))
        assertSame(base, artworkAccentScheme(base, listOf(0x00112233), 1f, ColorTheme.FOREST))
    }

    @Test fun allAdaptivePalettesKeepReadableTextAndInverseAccents() {
        for (theme in ColorTheme.entries.filter { it !in listOf(ColorTheme.HIGH_CONTRAST, ColorTheme.MONOCHROME) }) {
            for (dark in listOf(false, true)) for (source in vivid) {
                val base = appColorScheme(theme, dark)
                val result = artworkAccentScheme(base, listOf(source), 1f, theme)
                assertTrue("$theme/$dark primary", accentContrast(result.primary, result.onPrimary) >= 4.49f)
                assertTrue("$theme/$dark secondary", accentContrast(result.secondary, result.onSecondary) >= 4.49f)
                assertTrue("$theme/$dark inverse", accentContrast(result.inversePrimary, result.inverseSurface) >= 4.49f)
                assertEquals(base.background, result.background)
                assertEquals(base.surface, result.surface)
                assertEquals(base.onSurface, result.onSurface)
                assertEquals(base.error, result.error)
            }
        }
    }

    @Test fun voidKeepsAllBlackSurfacesAndTransparentTonalTint() {
        val base = appColorScheme(ColorTheme.VOID, false)
        val result = artworkAccentScheme(base, vivid, 1f, ColorTheme.VOID)
        assertEquals(Color.Black, result.background)
        assertEquals(Color.Black, result.surface)
        assertEquals(base.surfaceContainer, result.surfaceContainer)
        assertEquals(Color.Transparent, result.surfaceTint)
    }

    @Test fun intensityActuallyBlendsInsteadOfReplacingTheWholeTheme() {
        val base = appColorScheme(ColorTheme.FOREST, true)
        val a = artworkAccentScheme(base, vivid, .25f, ColorTheme.FOREST)
        val b = artworkAccentScheme(base, vivid, .75f, ColorTheme.FOREST)
        assertNotEquals(base.primary, a.primary)
        assertNotEquals(a.primary, b.primary)
        assertEquals(base.primaryContainer, b.primaryContainer)
        assertEquals(base.tertiary, b.tertiary)
    }
}
