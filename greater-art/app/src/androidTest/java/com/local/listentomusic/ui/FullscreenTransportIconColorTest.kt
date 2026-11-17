package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.ui.theme.GaControl
import com.local.listentomusic.ui.theme.GaVideoOverlay
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.ui.theme.appColorScheme
import com.local.listentomusic.ui.theme.videoOverlayAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class FullscreenTransportIconColorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disabledGlyphsDimAndClicksStayDisabledWithoutChangingButtonBounds() {
        val enabled = mutableStateOf(false)
        val expectedAccent = videoOverlayAccent(appColorScheme(ColorTheme.VOID, true).secondary)
        var calls = 0
        compose.setContent {
            GreaterArtTheme(ThemeMode.DARK, ColorTheme.VOID) {
                Row(Modifier.background(Color.White)) {
                    OverlayIconButton({ calls++ }, enabled.value) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(GaControl.prominentIcon))
                    }
                    OverlayIconButton({ calls++ }, enabled.value) {
                        Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(GaControl.prominentIcon))
                    }
                }
            }
        }
        val initialBounds = listOf("Previous", "Next").map { label ->
            val button = compose.onNode(hasClickAction() and hasContentDescription(label))
            button.assertIsNotEnabled()
            button.performClick()
            val plate = GaVideoOverlay.control.compositeOver(Color.White)
            assertTint(label, expectedAccent.copy(alpha = .38f).compositeOver(plate))
            button.fetchSemanticsNode().boundsInRoot
        }
        compose.runOnIdle { assertEquals(0, calls); enabled.value = true }
        for ((index, label) in listOf("Previous", "Next").withIndex()) {
            val button = compose.onNode(hasClickAction() and hasContentDescription(label))
            assertEquals(initialBounds[index], button.fetchSemanticsNode().boundsInRoot)
            assertTint(label, expectedAccent)
            button.performClick()
        }
        compose.runOnIdle { assertEquals(2, calls) }
    }

    @Test fun themedFullscreenSpeedGetsTheSameDarkPlateAsToolbarGlyphs() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 180.dp))) {
                GreaterArtTheme(ThemeMode.DARK, ColorTheme.INDIGO) {
                    Box(Modifier.width(384.dp).background(Color.White)) {
                        NowPlayingTopBar(
                            language = AppLanguage.ENGLISH,
                            onPictureInPicture = {}, onHome = {}, onFullscreen = {},
                            onClose = {}, onLock = {}, onSpeed = {}, fullscreen = true,
                            overlay = true,
                        )
                    }
                }
            }
        }
        val screenshot = compose.onNodeWithTag("FULLSCREEN_SPEED_BUTTON", useUnmergedTree = true)
            .captureToImage().asAndroidBitmap()
        // Sample the empty left edge of the 44x30dp badge, not antialiased text.
        val x = screenshot.width / 8
        val y = screenshot.height / 2
        val pixel = screenshot.getPixel(x, y)
        assertTrue("Speed label must have an actual dark local plate over white video",
            Color(pixel).alpha >= .35f && Color(pixel).red < .5f &&
                Color(pixel).green < .5f && Color(pixel).blue < .5f)
    }

    @Test fun themedFullscreenTimelineHasDarkTrackAndThumbBacking() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 100.dp))) {
                GreaterArtTheme(ThemeMode.DARK, ColorTheme.INDIGO) {
                    Box(Modifier.width(384.dp).background(Color.White)) {
                        CompactSlider(
                            value = 0.5f, onValueChange = {}, onValueChangeFinished = {},
                            valueRange = 0f..1f, enabled = true, videoOverlay = true,
                            activeColor = videoOverlayAccent(appColorScheme(ColorTheme.INDIGO, true).secondary),
                            inactiveColor = GaVideoOverlay.inactiveTrack,
                        )
                    }
                }
            }
        }
        val screenshot = compose.onNodeWithTag("PLAYBACK_TIMELINE", useUnmergedTree = true)
            .captureToImage().asAndroidBitmap()
        val pixel = screenshot.getPixel(screenshot.width / 4,
            screenshot.height / 2 - screenshot.height / 12)
        assertTrue("Fullscreen timeline must have a dark local backing under its accent",
            Color(pixel).alpha >= .35f && Color(pixel).red < .6f &&
                Color(pixel).green < .6f && Color(pixel).blue < .6f)
    }

    @Test fun ordinarySliderKeepsTheSingleOriginalInactiveTrackLayer() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 100.dp))) {
                GreaterArtTheme(ThemeMode.DARK, ColorTheme.INDIGO) {
                    Box(Modifier.width(384.dp).background(Color.White).testTag("ordinary-slider-fixture")) {
                        CompactSlider(
                            value = 0f, onValueChange = {}, onValueChangeFinished = {},
                            valueRange = 0f..1f, enabled = true, videoOverlay = false,
                            activeColor = Color.White, inactiveColor = Color.Black,
                        )
                    }
                }
            }
        }
        val screenshot = compose.onNodeWithTag("ordinary-slider-fixture")
            .captureToImage().asAndroidBitmap()
        val pixel = Color(screenshot.getPixel(screenshot.width * 3 / 4, screenshot.height / 2))
        // One 34%-black layer on white is about 66% luminance in sRGB.
        // Two overdrawn layers become 44% and would fail this regression.
        assertTrue("Ordinary playback must retain the original single-layer track",
            pixel.alpha >= .95f && pixel.red in 0.58f..0.75f &&
                pixel.green in 0.58f..0.75f && pixel.blue in 0.58f..0.75f)
    }

    private fun assertTint(label: String, color: Color) {
        val image = compose.onNodeWithContentDescription(label, useUnmergedTree = true)
            .captureToImage().asAndroidBitmap()
        val expected = color.toArgb()
        var matches = 0
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val pixel = image.getPixel(x, y)
            if (listOf(16, 8, 0).all { abs(((pixel ushr it) and 255) - ((expected ushr it) and 255)) <= 3 }) matches++
        }
        assertTrue("$label must render its enabled/disabled foreground; matches=$matches", matches >= 8)
    }
}
