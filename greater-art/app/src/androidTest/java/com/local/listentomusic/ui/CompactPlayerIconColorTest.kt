package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.CompactPlayerView
import com.local.listentomusic.ui.theme.appColorScheme
import com.local.listentomusic.ui.theme.artworkAccentScheme
import com.local.listentomusic.ui.theme.artworkThemePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactPlayerIconColorTest {
    @Test fun ambientDockUsesTheExpandedPaletteAndMatchingRippleInBothThemes() = withCompact { view ->
        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT)) {
            for (theme in listOf(ColorTheme.FOREST, ColorTheme.ROSE, ColorTheme.VIDEO, ColorTheme.HIGH_CONTRAST)) {
                for (color in listOf(0xffff3018.toInt(), 0xff2050ff.toInt())) {
                    val art = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
                    try {
                        val settings = UserPreferences(themeMode = mode, colorTheme = theme,
                            backgroundMode = AppBackgroundMode.AMBIENT, adaptiveIntensity = .8f)
                        view.appearance(settings)
                        view.setArtwork(art)
                        val base = appColorScheme(theme, mode == ThemeMode.DARK)
                        val expected = artworkAccentScheme(base, artworkThemePalette(art),
                            if (theme == ColorTheme.VIDEO) 1f else .8f, theme)
                        assertColors(view, expected.secondary.toArgb())
                    } finally { view.setArtwork(null); art.recycle() }
                }
            }
        }
    }

    @Test fun changingIntensityAndWallpaperRestoresTheBaseWithoutStaleTint() = withCompact { view ->
        val art = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xffff3018.toInt()) }
        val settings = UserPreferences(themeMode = ThemeMode.DARK, colorTheme = ColorTheme.FOREST,
            backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC, adaptiveIntensity = 1f)
        try {
            view.appearance(settings)
            view.setArtwork(art)
            val base = appColorScheme(ColorTheme.FOREST, true)
            val adapted = artworkAccentScheme(base, artworkThemePalette(art), 1f, ColorTheme.FOREST)
            assertNotEquals(base.secondary, adapted.secondary)
            assertColors(view, adapted.secondary.toArgb())
            view.appearance(settings.copy(adaptiveIntensity = 0f))
            assertColors(view, base.secondary.toArgb())
            view.appearance(settings)
            assertColors(view, adapted.secondary.toArgb())
            view.appearance(settings.copy(backgroundMode = AppBackgroundMode.DEFAULT))
            assertColors(view, base.secondary.toArgb())
        } finally { view.setArtwork(null); art.recycle() }
    }

    private fun withCompact(check: (CompactPlayerView) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val player = ExoPlayer.Builder(instrumentation.targetContext).build()
            val view = CompactPlayerView(instrumentation.targetContext)
            try {
                player.setMediaItem(MediaItem.Builder().setUri("file:///owned/icon-color.mp3")
                    .setMediaId("owned-icon-color")
                    .setMediaMetadata(MediaMetadata.Builder().setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()).build())
                view.bind(player, "LIBRARY_MINI")
                check(view)
            } finally { view.release(); player.release() }
        }
    }

    private fun assertColors(view: View, accent: Int) {
        fun buttons(child: View): List<ImageButton> = when (child) {
            is androidx.media3.ui.PlayerView -> emptyList()
            is ImageButton -> listOf(child)
            is ViewGroup -> (0 until child.childCount).flatMap { buttons(child.getChildAt(it)) }
            else -> emptyList()
        }
        val buttons = buttons(view)
        assertEquals(3, buttons.size)
        for (button in buttons) {
            assertEquals(accent, button.imageTintList!!.defaultColor)
            val ripple = button.background as RippleDrawable
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            try {
                ripple.setBounds(0, 0, 96, 96)
                ripple.setHotspot(48f, 48f)
                ripple.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
                ripple.jumpToCurrentState()
                ripple.draw(Canvas(bitmap))
                val pixel = bitmap.getPixel(48, 48)
                assertTrue("Pressed ripple must be visible", Color.alpha(pixel) > 8)
                assertEquals(Color.red(accent).toDouble(), Color.red(pixel).toDouble(), 6.0)
                assertEquals(Color.green(accent).toDouble(), Color.green(pixel).toDouble(), 6.0)
                assertEquals(Color.blue(accent).toDouble(), Color.blue(pixel).toDouble(), 6.0)
            } finally {
                ripple.state = intArrayOf(android.R.attr.state_enabled)
                bitmap.recycle()
            }
        }
    }
}
