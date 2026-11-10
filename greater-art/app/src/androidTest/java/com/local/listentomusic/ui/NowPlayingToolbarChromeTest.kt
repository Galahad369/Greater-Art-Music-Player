package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.ui.theme.GreaterArtTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingToolbarChromeTest {
    @get:Rule val compose = createComposeRule()
    private val backdrop = 0xff386992.toInt()
    private val clicks = mutableListOf<String>()
    private var topInsetPx = 0
    private var leftInsetPx = 0

    @Test fun expandedToolbarKeepsRoundedFillWithoutOutlineAndRetainsItsActions() {
        showToolbar(overlay = false)
        checkToolbarPixels { bitmap ->
            val centerX = bitmap.width / 2
            val fill = bitmap.getPixel(centerX, bitmap.height / 5)
            assertNotEquals("Expanded toolbar must retain its fill", backdrop, fill)
            assertEquals("The straight top edge must not paint an outline", fill, bitmap.getPixel(centerX, topInsetPx))
            assertEquals("The top-left corner must remain rounded", backdrop, bitmap.getPixel(leftInsetPx, topInsetPx))
        }
        val actions = listOf("Open floating player", "Home", "Lock player controls", "Fullscreen", "Close player")
        for (action in actions) {
            compose.onNodeWithContentDescription(action).assertHasClickAction()
                .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        }
        compose.runOnIdle { assertEquals(actions, clicks) }
    }

    @Test fun fullscreenToolbarRetainsItsContrastScrim() {
        showToolbar(overlay = true)
        checkToolbarPixels { bitmap ->
            assertNotEquals("Fullscreen controls still need a scrim over video", backdrop,
                bitmap.getPixel(bitmap.width / 2, bitmap.height / 5))
        }
    }

    private fun showToolbar(overlay: Boolean) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 128.dp))) {
                val density = LocalDensity.current
                SideEffect {
                    topInsetPx = with(density) { 6.dp.roundToPx() }
                    leftInsetPx = with(density) { 12.dp.roundToPx() }
                }
                GreaterArtTheme(ThemeMode.DARK) {
                    Box(Modifier.width(384.dp).background(Color(backdrop)).testTag("toolbar-fixture")) {
                        NowPlayingTopBar(
                            language = AppLanguage.ENGLISH,
                            onPictureInPicture = { clicks += "Open floating player" },
                            onHome = { clicks += "Home" },
                            onFullscreen = { clicks += "Fullscreen" },
                            onClose = { clicks += "Close player" },
                            onLock = { clicks += "Lock player controls" },
                            overlay = overlay,
                            fullscreen = overlay,
                        )
                    }
                }
            }
        }
    }

    private fun checkToolbarPixels(check: (Bitmap) -> Unit) {
        val bitmap = compose.onNodeWithTag("toolbar-fixture").assertHeightIsEqualTo(64.dp)
            .captureToImage().asAndroidBitmap()
        try { check(bitmap) }
        finally { bitmap.recycle() }
    }
}
