package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class CompactPlayerIconColorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ambientDockUsesTheExpandedPaletteAndMatchingRippleInBothThemes() = withCompact { view ->
        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT)) {
            for (theme in listOf(ColorTheme.FOREST, ColorTheme.ROSE, ColorTheme.VIDEO, ColorTheme.HIGH_CONTRAST)) {
                for (color in listOf(0xffff3018.toInt(), 0xff2050ff.toInt())) {
                    val art = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
                    val settings = UserPreferences(themeMode = mode, colorTheme = theme,
                        backgroundMode = AppBackgroundMode.AMBIENT, adaptiveIntensity = .8f)
                    val base = appColorScheme(theme, mode == ThemeMode.DARK)
                    val expected = artworkAccentScheme(base, artworkThemePalette(art),
                        if (theme == ColorTheme.VIDEO) 1f else .8f, theme).secondary.toArgb()
                    try {
                        compose.runOnIdle { view.appearance(settings); view.setArtwork(art); assertTints(view, expected) }
                        assertRipple(view, expected)
                    } finally { compose.runOnIdle { view.setArtwork(null); art.recycle() } }
                }
            }
        }
    }

    @Test fun changingIntensityAndWallpaperRestoresTheBaseWithoutStaleTint() = withCompact { view ->
        val art = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xffff3018.toInt()) }
        val settings = UserPreferences(themeMode = ThemeMode.DARK, colorTheme = ColorTheme.FOREST,
            backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC, adaptiveIntensity = 1f)
        val base = appColorScheme(ColorTheme.FOREST, true)
        val adapted = artworkAccentScheme(base, artworkThemePalette(art), 1f, ColorTheme.FOREST)
        assertNotEquals(base.secondary, adapted.secondary)
        try {
            compose.runOnIdle { view.setArtwork(art) }
            for ((preferences, expected) in listOf(settings to adapted.secondary,
                settings.copy(adaptiveIntensity = 0f) to base.secondary, settings to adapted.secondary,
                settings.copy(backgroundMode = AppBackgroundMode.DEFAULT) to base.secondary)) {
                compose.runOnIdle { view.appearance(preferences); assertTints(view, expected.toArgb()) }
                assertRipple(view, expected.toArgb())
            }
        } finally { compose.runOnIdle { view.setArtwork(null); art.recycle() } }
    }

    @Test fun theSameOnscreenCheckRejectsTheOldHardcodedMintRipple() = withCompact { view ->
        val accent = Color.rgb(255, 48, 24)
        compose.runOnIdle {
            view.colors(Color.BLACK, Color.WHITE, accent)
            buttons(view).forEach { (it.background as RippleDrawable).setColor(android.content.res.ColorStateList.valueOf(0x338fe3cf)) }
        }
        assertThrows(AssertionError::class.java) { assertRipple(view, accent) }
        compose.runOnIdle { view.colors(Color.BLACK, Color.WHITE, accent) }
        assertRipple(view, accent)
    }

    private fun withCompact(check: (CompactPlayerView) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var player: ExoPlayer
        lateinit var view: CompactPlayerView
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext).build()
            player.setMediaItem(MediaItem.Builder().setUri("file:///owned/icon-color.mp3")
                .setMediaId("owned-icon-color")
                .setMediaMetadata(MediaMetadata.Builder().setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()).build())
            view = CompactPlayerView(instrumentation.targetContext).apply { bind(player, "LIBRARY_MINI") }
        }
        compose.setContent { AndroidView(factory = { view }, modifier = Modifier.width(384.dp).height(64.dp)) }
        compose.waitForIdle()
        try { check(view) } finally { compose.runOnIdle { view.release(); player.release() } }
    }

    private fun buttons(child: View): List<ImageButton> = when (child) {
        is androidx.media3.ui.PlayerView -> emptyList()
        is ImageButton -> listOf(child)
        is ViewGroup -> (0 until child.childCount).flatMap { buttons(child.getChildAt(it)) }
        else -> emptyList()
    }

    private fun assertTints(view: View, accent: Int) {
        val actions = buttons(view)
        assertEquals(3, actions.size)
        actions.forEach { assertEquals(accent, it.imageTintList!!.defaultColor) }
    }

    private fun assertRipple(view: View, accent: Int) {
        lateinit var button: ImageButton
        val location = IntArray(2)
        compose.runOnIdle {
            button = buttons(view).single { it.visibility == View.VISIBLE }
            button.isPressed = false
            button.background.jumpToCurrentState()
            button.getLocationOnScreen(location)
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val before = automation.takeScreenshot()
        compose.runOnIdle {
            button.drawableHotspotChanged(button.width / 2f, button.height / 2f)
            button.isPressed = true
        }
        // Native RippleDrawable uses the platform clock, not Compose's test clock.
        SystemClock.sleep(250)
        val after = automation.takeScreenshot()
        try {
            var matching = 0
            for (y in 2 until button.height - 2) for (x in 2 until button.width - 2) {
                val a = before.getPixel(location[0] + x, location[1] + y)
                val b = after.getPixel(location[0] + x, location[1] + y)
                val base = intArrayOf(Color.red(a), Color.green(a), Color.blue(a))
                val delta = intArrayOf(Color.red(b)-base[0], Color.green(b)-base[1], Color.blue(b)-base[2])
                val direction = intArrayOf(Color.red(accent)-base[0], Color.green(accent)-base[1], Color.blue(accent)-base[2])
                val magnitude = direction.sumOf { it * it }.toFloat()
                if (magnitude == 0f || delta.maxOf { abs(it) } < 3) continue
                val alpha = delta.indices.sumOf { delta[it] * direction[it] } / magnitude
                if (alpha in .01f.. .30f && delta.indices.all { abs(delta[it] - alpha * direction[it]) <= 2f }) matching++
            }
            assertTrue("Actual pressed pixels must blend toward the current accent; matches=$matching", matching >= 8)
        } finally {
            compose.runOnIdle { button.isPressed = false; button.background.jumpToCurrentState() }
            before.recycle(); after.recycle()
        }
    }
}
