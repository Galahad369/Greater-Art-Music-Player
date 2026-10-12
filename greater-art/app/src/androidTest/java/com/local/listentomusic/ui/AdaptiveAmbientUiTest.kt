package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.*
import com.local.listentomusic.ui.components.adaptiveAmbientBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AdaptiveAmbientUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun localizedSettingsHaveMeasuredTargetsAndRowToggle() {
        val prefs = mutableStateOf(UserPreferences(backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC))
        compose.setContent { MaterialTheme {
            AdaptiveAmbientSettings(prefs.value,
                { prefs.value = prefs.value.copy(adaptiveIntensity = it) },
                { prefs.value = prefs.value.copy(adaptiveReactToVideo = it) },
                { prefs.value = prefs.value.copy(adaptiveMotion = it) })
        } }
        compose.onNodeWithText("React to video").assertIsOn().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("React to video").assertIsOff()
        compose.onNodeWithText("Off", substring = false).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(AmbientMotion.OFF, prefs.value.adaptiveMotion) }
        compose.runOnIdle { prefs.value = prefs.value.copy(appLanguage = AppLanguage.JAPANESE) }
        compose.onNodeWithText("映像の色変化に反応").assertIsDisplayed()
        compose.onNodeWithText("効果の強さ").assertIsDisplayed()
    }
    @Test fun artworkPaletteChangesActualCompositeWithoutMovingControls() {
        val red = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xffff2020.toInt()) }
        val blue = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff2040ff.toInt()) }
        val art = mutableStateOf(red)
        val id = mutableStateOf("warm-audio")
        compose.setContent { MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
            Box(Modifier.size(340.dp, 400.dp).testTag("adaptive-render")
                .adaptiveAmbientBackdrop(art.value, id.value,
                    UserPreferences(backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC,
                        adaptiveMotion = AmbientMotion.OFF, adaptiveIntensity = .75f))) {
                Text("Current track", Modifier.padding(16.dp).testTag("fixed-title"),
                    color = MaterialTheme.colorScheme.onBackground)
            }
        } }
        try {
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(2200)
            compose.waitForIdle()
            val titleBefore = compose.onNodeWithTag("fixed-title").fetchSemanticsNode().boundsInRoot
            val first = compose.onNodeWithTag("adaptive-render").captureToImage().asAndroidBitmap()
            save(first, "adaptive-audio-warm.png")
            compose.runOnIdle { art.value = blue; id.value = "cool-audio" }
            compose.waitForIdle(); compose.mainClock.advanceTimeBy(2200); compose.waitForIdle()
            val second = compose.onNodeWithTag("adaptive-render").captureToImage().asAndroidBitmap()
            save(second, "adaptive-audio-cool.png")
            assertEquals(titleBefore, compose.onNodeWithTag("fixed-title").fetchSemanticsNode().boundsInRoot)
            // Sample away from the fixed title: actual composed Canvas, not just engine values.
            assertNotEquals(first.getPixel(first.width / 2, first.height / 2),
                second.getPixel(second.width / 2, second.height / 2))
        } finally { red.recycle(); blue.recycle() }
    }
    private fun save(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "ambient-evidence").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun zeroViewportDuringTransitionsDoesNotCreateInvalidRadialShader() {
        compose.setContent { MaterialTheme {
            Box(Modifier.size(0.dp).testTag("zero-ambient").adaptiveAmbientBackdrop(null, null,
                UserPreferences(adaptiveMotion = AmbientMotion.OFF)))
        } }
        compose.waitForIdle()
        compose.onNodeWithTag("zero-ambient").assertWidthIsEqualTo(0.dp).assertHeightIsEqualTo(0.dp)
    }
}
