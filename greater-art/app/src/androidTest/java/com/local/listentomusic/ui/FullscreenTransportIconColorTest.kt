package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
