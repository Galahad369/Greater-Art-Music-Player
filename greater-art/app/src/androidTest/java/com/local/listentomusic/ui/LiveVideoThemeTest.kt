package com.local.listentomusic.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.VideoAmbientColors
import com.local.listentomusic.ui.components.VideoAmbientSample
import com.local.listentomusic.ui.theme.MediaAdaptiveTheme
import com.local.listentomusic.ui.theme.appColorScheme
import com.local.listentomusic.ui.theme.artworkAccentScheme
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/** Exercises the real theme boundary, not a second implementation of its flow. */
class LiveVideoThemeTest {
    @get:Rule val compose = createComposeRule()
    private val settings = UserPreferences(colorTheme = ColorTheme.VIDEO, themeMode = ThemeMode.DARK)
    private val red = 0xffff0000.toInt()
    private val blue = 0xff0000ff.toInt()
    private val id = "theme-regression-" + UUID.randomUUID()
    private val observed = AtomicInteger()

    @Suppress("UNCHECKED_CAST")
    private fun sample(color: Int) {
        val field = VideoAmbientColors::class.java.getDeclaredField("mutable").apply { isAccessible = true }
        val flow = field.get(VideoAmbientColors) as MutableStateFlow<VideoAmbientSample>
        // The retained original APK has a two-field sample. Keep this same check runnable
        // against it, so a missing constructor cannot be mistaken for the color-latch bug.
        val constructor = VideoAmbientSample::class.java.constructors.first { it.parameterCount in 2..3 }
        val value = if (constructor.parameterCount == 2) constructor.newInstance(id, List(256) { color })
            else constructor.newInstance(id, List(256) { color }, SystemClock.uptimeMillis())
        compose.runOnIdle { flow.value = value as VideoAmbientSample }
    }

    private fun expected(color: Int) = artworkAccentScheme(appColorScheme(ColorTheme.VIDEO, true),
        List(3) { color }, 1f, ColorTheme.VIDEO).primary.toArgb()

    @Test fun actualThemeKeepsFollowingLaterVideoFrames() {
        compose.setContent {
            MediaAdaptiveTheme(settings, id, null) {
                val color = MaterialTheme.colorScheme.primary
                SideEffect { observed.set(color.toArgb()) }
                Box(Modifier.size(80.dp).background(color).testTag("live-accent"))
            }
        }
        sample(red)
        compose.waitUntil(5_000) { observed.get() == expected(red) }
        sample(blue)
        compose.waitUntil(5_000) { observed.get() == expected(blue) }
        assertNotEquals(expected(red), observed.get())
    }

    @Test fun removingAndReenteringThePresentationKeepsItsFirstVisibleAccent() {
        val visible = mutableStateOf(true)
        val returned = AtomicInteger()
        compose.setContent {
            if (visible.value) MediaAdaptiveTheme(settings, id, null) {
                val color = MaterialTheme.colorScheme.primary.toArgb()
                SideEffect {
                    observed.set(color)
                    returned.compareAndSet(0, color)
                }
            }
        }
        sample(red)
        compose.waitUntil(5_000) { observed.get() == expected(red) }
        sample(blue)
        compose.waitUntil(5_000) { observed.get() == expected(blue) }
        val before = observed.get()
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        returned.set(0)
        compose.runOnIdle { visible.value = true }
        compose.waitForIdle()
        assertEquals("Re-entry must not flash the base theme or re-latch an entry snapshot", before, returned.get())
        assertEquals(before, observed.get())
    }
}
