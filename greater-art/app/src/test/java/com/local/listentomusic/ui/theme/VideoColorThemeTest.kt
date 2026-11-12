package com.local.listentomusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.colorThemeOptions
import org.junit.Assert.*
import org.junit.Test

class VideoColorThemeTest {
    @Test fun oledIsDefaultAndVideoIsSecondWithoutDroppingSavedPalettes() {
        assertEquals(ColorTheme.VOID, UserPreferences().colorTheme)
        val options = colorThemeOptions()
        assertEquals(listOf(ColorTheme.VOID, ColorTheme.VIDEO), options.take(2))
        assertEquals(ColorTheme.entries.toSet(), options.toSet())
        assertEquals(options.size, options.distinct().size)
        assertEquals("FOREST", ColorTheme.FOREST.name)
        assertEquals("VOID", ColorTheme.VOID.name)
    }

    @Test fun videoChoiceDoesNotDependOnWallpaperOrAmbientStrength() {
        for (background in AppBackgroundMode.entries) {
            val settings = UserPreferences(colorTheme = ColorTheme.VIDEO, backgroundMode = background,
                adaptiveIntensity = 0f, adaptiveReactToVideo = false)
            assertTrue(mediaThemeEnabled(settings, "current-video"))
            assertFalse(mediaThemeEnabled(settings, null))
        }
        assertFalse(mediaThemeEnabled(UserPreferences(colorTheme = ColorTheme.FOREST,
            backgroundMode = AppBackgroundMode.DEFAULT), "current-video"))
    }

    @Test fun videoColorsAreVisibleReadableAndKeepTheBaseSurfaces() {
        for (dark in listOf(false, true)) {
            val base = appColorScheme(ColorTheme.VIDEO, dark)
            val red = artworkAccentScheme(base, listOf(0xffd40018.toInt()), 1f, ColorTheme.VIDEO)
            val blue = artworkAccentScheme(base, listOf(0xff102bff.toInt()), 1f, ColorTheme.VIDEO)
            assertNotEquals(base.primary, red.primary)
            assertNotEquals(red.primary, blue.primary)
            for (colors in listOf(red, blue)) {
                assertTrue(accentContrast(colors.primary, colors.onPrimary) >= 4.49f)
                assertTrue(accentContrast(colors.secondary, colors.onSecondary) >= 4.49f)
                assertTrue(accentContrast(colors.tertiary, colors.onTertiary) >= 4.49f)
                assertEquals(base.background, colors.background)
                assertEquals(base.surface, colors.surface)
                assertEquals(base.error, colors.error)
            }
            if (dark) assertEquals(Color.Black, red.background)
            assertSame(base, artworkAccentScheme(base, emptyList(), 1f, ColorTheme.VIDEO))
        }
    }
}
