package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.AmbientMotion
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.AppBackground
import com.local.listentomusic.ui.theme.GreaterArtTheme
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Actual app background, including the classic cached-artwork path and final dim veil. */
class ReflectedAmbientUiTest {
    @get:Rule val compose = createComposeRule()
    private val captures = mutableListOf<Bitmap>()

    @After fun releaseCaptures() {
        captures.forEach { it.recycle() }
    }

    @Test fun classicDarkUsesCachedArtworkWithoutMovingControls() =
        exercise(AppBackgroundMode.AMBIENT, ThemeMode.DARK, DpSize(320.dp, 480.dp))

    @Test fun classicLightUsesCachedArtworkWithoutMovingControls() =
        exercise(AppBackgroundMode.AMBIENT, ThemeMode.LIGHT, DpSize(360.dp, 640.dp))

    @Test fun adaptiveDarkUsesCachedArtworkWithoutMovingControls() =
        exercise(AppBackgroundMode.ADAPTIVE_DYNAMIC, ThemeMode.DARK, DpSize(360.dp, 640.dp))

    @Test fun adaptiveLightUsesCachedArtworkWithoutMovingControls() =
        exercise(AppBackgroundMode.ADAPTIVE_DYNAMIC, ThemeMode.LIGHT, DpSize(320.dp, 480.dp))

    private fun exercise(mode: AppBackgroundMode, theme: ThemeMode, size: DpSize) {
        val red = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xfff02030.toInt()) }
        val blue = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff2040ff.toInt()) }
        val artwork = mutableStateOf(red)
        val media = mutableStateOf("reflected-warm-$mode-$theme")
        var clicks = 0
        compose.setContent {
            GreaterArtTheme(theme) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(Modifier.size(size).testTag("reflected-background")) {
                        AppBackground(
                            UserPreferences(backgroundMode = mode, adaptiveMotion = AmbientMotion.OFF,
                                adaptiveIntensity = .9f, backgroundDim = .10f),
                            currentPath = media.value, isVideo = false, controller = null,
                            modifier = Modifier.matchParentSize(), ambientArtwork = artwork.value,
                        )
                        Button(onClick = { clicks++ }, contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.align(Alignment.BottomCenter).size(48.dp).testTag("fixed-ambient-control")) {
                            Text("Play")
                        }
                    }
                }
            }
        }
        try {
            settlePalette()
            val button = compose.onNodeWithTag("fixed-ambient-control")
                .assertIsDisplayed().assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            val bounds = button.fetchSemanticsNode().boundsInRoot
            val warm = capture("$mode-$theme-warm")
            assertOpaque(warm)
            compose.mainClock.advanceTimeBy(6_000)
            compose.waitForIdle()
            val still = capture("$mode-$theme-motion-off")
            assertTrue("Motion OFF must not move reflective geometry", warm.sameAs(still))
            compose.runOnIdle { artwork.value = blue; media.value = "reflected-cool-$mode-$theme" }
            settlePalette()
            val cool = capture("$mode-$theme-cool")
            assertOpaque(cool)
            assertFalse("$mode must use the supplied audio artwork, not a blank fallback", warm.sameAs(cool))
            assertEquals("Decorative effects must not move controls", bounds, button.fetchSemanticsNode().boundsInRoot)
            button.performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        } finally {
            red.recycle()
            blue.recycle()
        }
    }

    @Test fun zeroIntensityLeavesAnOpaqueUniformBaseWithoutMediaTint() {
        val red = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xffff0000.toInt()) }
        val blue = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff0000ff.toInt()) }
        val art = mutableStateOf(red)
        val media = mutableStateOf("zero-warm")
        compose.setContent {
            GreaterArtTheme(ThemeMode.DARK) {
                AppBackground(
                    UserPreferences(backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC,
                        adaptiveMotion = AmbientMotion.OFF, adaptiveIntensity = 0f, backgroundDim = .1f),
                    currentPath = media.value, isVideo = false, controller = null,
                    modifier = Modifier.size(320.dp, 480.dp).testTag("reflected-background"),
                    ambientArtwork = art.value,
                )
            }
        }
        try {
            settlePalette()
            val first = capture("adaptive-zero-warm")
            assertOpaque(first)
            val base = first.getPixel(first.width / 2, first.height / 2)
            val pixels = IntArray(first.width * first.height)
            first.getPixels(pixels, 0, first.width, 0, 0, first.width, first.height)
            assertTrue("Zero intensity must leave only the base and veil", pixels.all { it == base })
            compose.runOnIdle { art.value = blue; media.value = "zero-cool" }
            settlePalette()
            assertTrue("Zero intensity must not leak a previous media palette", first.sameAs(capture("adaptive-zero-cool")))
        } finally {
            red.recycle()
            blue.recycle()
        }
    }

    @Test fun bothAmbientModesHandleAZeroViewport() {
        val mode = mutableStateOf(AppBackgroundMode.AMBIENT)
        compose.setContent {
            GreaterArtTheme(ThemeMode.DARK) {
                AppBackground(UserPreferences(backgroundMode = mode.value, adaptiveMotion = AmbientMotion.OFF),
                    currentPath = null, isVideo = false, controller = null,
                    modifier = Modifier.size(0.dp).testTag("zero-reflected-background"))
            }
        }
        for (value in listOf(AppBackgroundMode.AMBIENT, AppBackgroundMode.ADAPTIVE_DYNAMIC)) {
            compose.runOnIdle { mode.value = value }
            compose.onNodeWithTag("zero-reflected-background")
                .assertWidthIsEqualTo(0.dp).assertHeightIsEqualTo(0.dp)
        }
    }

    private fun settlePalette() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_200)
        compose.waitForIdle()
    }

    private fun assertOpaque(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue("All output pixels must be opaque", pixels.all { Color.alpha(it) == 255 })
    }

    private fun capture(label: String): Bitmap {
        val image = compose.onNodeWithTag("reflected-background").captureToImage().asAndroidBitmap()
        captures += image
        if (InstrumentationRegistry.getArguments().getString("reflectedAmbientScreenshots") == "true") {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val file = File(requireNotNull(context.getExternalFilesDir(null)), "reflected-$label-${UUID.randomUUID()}.png")
            file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            Log.i("ReflectedAmbientTest", "SCREENSHOT ${file.absolutePath}")
        }
        return image
    }
}
