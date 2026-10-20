package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.theme.MediaAdaptiveTheme
import com.local.listentomusic.ui.theme.appColorScheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ArtworkAccentUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cachedArtworkAdaptsControlsAndZeroIntensityRestoresTheExactTheme() {
        val art = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xffff5000.toInt()) }
        val preferences = mutableStateOf(UserPreferences(backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC, adaptiveIntensity = 1f))
        val base = appColorScheme(preferences.value.colorTheme, true).primary.toArgb()
        var primary = base
        compose.setContent {
            MediaAdaptiveTheme(preferences.value, "owned-audio", art) {
                primary = MaterialTheme.colorScheme.primary.toArgb()
                Text(if (primary == base) "base" else "adapted")
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("adapted").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertNotEquals(base, primary); preferences.value = preferences.value.copy(adaptiveIntensity = 0f) }
        compose.onNodeWithText("base").assertIsDisplayed()
        compose.runOnIdle { assertEquals(base, primary) }
    }

    @Test fun highContrastAndNoMatchingMediaKeepTheFixedPalette() {
        val preferences = mutableStateOf(UserPreferences(backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC,
            colorTheme = ColorTheme.HIGH_CONTRAST, adaptiveIntensity = 1f))
        var primary = 0
        compose.setContent {
            MediaAdaptiveTheme(preferences.value, "not-the-active-video", null) {
                primary = MaterialTheme.colorScheme.primary.toArgb()
                Text("fixed")
            }
        }
        compose.runOnIdle { assertEquals(appColorScheme(ColorTheme.HIGH_CONTRAST, true).primary.toArgb(), primary) }
        compose.runOnIdle { preferences.value = preferences.value.copy(colorTheme = ColorTheme.FOREST) }
        compose.runOnIdle { assertEquals(appColorScheme(ColorTheme.FOREST, true).primary.toArgb(), primary) }
    }
}
