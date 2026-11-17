package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.playback.expandedPlayerPullInput
import com.local.listentomusic.ui.theme.GreaterArtTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingToolbarChromeTest {
    @get:Rule val compose = createComposeRule()
    private val backdrop = 0xff386992.toInt()
    private val clicks = mutableListOf<String>()
    private var topInsetPx = 0
    private var leftInsetPx = 0
    private val locked = mutableStateOf(false)

    @Test fun selectedLockIsVisibleWithoutMovingAnyActionOrHandle() {
        showToolbar(overlay = false)
        val before = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .map { it.boundsInRoot }.sortedBy { it.left }
        val gap = compose.onNodeWithTag("EXPANDED_TOOLBAR_HANDLE_GAP").fetchSemanticsNode().boundsInRoot
        val normal = compose.onNodeWithTag("toolbar-content").captureToImage().asAndroidBitmap()
        compose.runOnIdle { locked.value = true }
        compose.onNodeWithTag("PLAYER_LOCK_SELECTED", useUnmergedTree = true)
            .assertWidthIsEqualTo(32.dp).assertHeightIsEqualTo(32.dp)
        compose.onNodeWithContentDescription("Unlock player controls")
            .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        val after = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .map { it.boundsInRoot }.sortedBy { it.left }
        assertEquals("No action may move when Lock changes state", before, after)
        assertEquals("The pull zone must not move when Lock is toggled", gap,
            compose.onNodeWithTag("EXPANDED_TOOLBAR_HANDLE_GAP").fetchSemanticsNode().boundsInRoot)
        val selected = compose.onNodeWithTag("toolbar-content").captureToImage().asAndroidBitmap()
        try {
            val bounds = compose.onNodeWithContentDescription("Unlock player controls").fetchSemanticsNode().boundsInRoot
            val toolbar = compose.onNodeWithTag("toolbar-content").fetchSemanticsNode().boundsInRoot
            val x = (bounds.center.x - toolbar.left).toInt()
            val y = (bounds.center.y - toolbar.top + bounds.height * .25f).toInt()
            assertNotEquals("A real filled selection must be visible", normal.getPixel(x, y), selected.getPixel(x, y))
        } finally { normal.recycle(); selected.recycle() }
        compose.runOnIdle { locked.value = false }
        compose.onNodeWithTag("PLAYER_LOCK_SELECTED", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun expandedToolbarIsTransparentAndRetainsItsActions() {
        showToolbar(overlay = false)
        val buttons = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.left }
        assertEquals("Keep all six top actions", 6, buttons.size)
        val gap = compose.onNodeWithTag("EXPANDED_TOOLBAR_HANDLE_GAP").fetchSemanticsNode().boundsInRoot
        val toolbar = compose.onNodeWithTag("toolbar-content").fetchSemanticsNode().boundsInRoot
        val minimumGapPx = buttons.first().boundsInRoot.width * (72f / 48f)
        assertTrue("Protect the 72dp center pull gesture: $gap", gap.width >= minimumGapPx - 1f)
        assertTrue("Keep the pull lane centered", kotlin.math.abs(gap.center.x - toolbar.center.x) <= 1.01f)
        for (button in buttons) assertTrue("No action may overlap the pull gesture",
            button.boundsInRoot.right <= gap.left + 1f || button.boundsInRoot.left >= gap.right - 1f)
        for (side in buttons.chunked(3)) {
            val intervals = side.zipWithNext { left, right ->
                right.boundsInRoot.center.x - left.boundsInRoot.center.x
            }
            assertTrue("Distribute each group symmetrically: $intervals",
                intervals.max() - intervals.min() <= 1.01f)
        }
        checkToolbarPixels { bitmap ->
            val centerX = bitmap.width / 2
            assertEquals("Expanded toolbar must show the shared background", backdrop,
                bitmap.getPixel(centerX, bitmap.height / 5))
            assertEquals("The top edge must not paint a backplate or outline", backdrop,
                bitmap.getPixel(centerX, topInsetPx))
            assertEquals("The corner must show the shared background", backdrop,
                bitmap.getPixel(leftInsetPx, topInsetPx))
        }
        val actions = listOf("Open floating player", "Home", "Lock player controls", "Fullscreen", "Close player")
        for (action in actions) {
            compose.onNodeWithContentDescription(action).assertHasClickAction()
                .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        }
        compose.runOnIdle { assertEquals(actions, clicks) }
    }


    @Test fun nativeIconCenterTapsAndCentralPullGestureCoexist() {
        val clicked = mutableListOf<String>()
        var pulls = 0
        var ends = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 200.dp))) {
                val density = LocalDensity.current.density
                GreaterArtTheme(ThemeMode.DARK) {
                    Box(Modifier.width(384.dp).background(Color(backdrop))) {
                        NowPlayingTopBar(
                            language = AppLanguage.ENGLISH,
                            onPictureInPicture = { clicked += "Open floating player" },
                            onHome = { clicked += "Home" },
                            onFullscreen = { clicked += "Fullscreen" },
                            onClose = { clicked += "Close player" },
                            onLock = { clicked += "Lock player controls" },
                            reservePullHandle = true,
                            modifier = Modifier.testTag("gesture-toolbar").expandedPlayerPullInput(
                                true, density, { pulls++ }, { _, _ -> ends++ }, {},
                            ),
                        )
                    }
                }
            }
        }
        val buttons = listOf("Open floating player", "Home", "Lock player controls",
            "Fullscreen", "Close player")
        for (label in buttons) {
            compose.onNodeWithContentDescription(label).performTouchInput { click(center) }
        }
        compose.runOnIdle {
            assertEquals(buttons, clicked)
            assertEquals("Button presses must not become pull gestures", 0, pulls)
            assertEquals(0, ends)
        }
        compose.onNodeWithTag("gesture-toolbar").performTouchInput {
            swipe(Offset(center.x, 14.dp.toPx()), Offset(center.x, 62.dp.toPx()), 350)
        }
        compose.runOnIdle {
            assertTrue("The central pull lane remains usable", pulls > 0)
            assertEquals(1, ends)
            assertEquals("Central pull must not activate toolbar actions", buttons, clicked)
        }
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
                            locked = locked.value,
                            modifier = Modifier.testTag("toolbar-content"),
                            overlay = overlay,
                            fullscreen = overlay,
                            reservePullHandle = !overlay,
                        )
                    }
                }
            }
        }
    }

    private fun checkToolbarPixels(check: (Bitmap) -> Unit) {
        val bitmap = compose.onNodeWithTag("toolbar-content").assertHeightIsEqualTo(64.dp)
            .captureToImage().asAndroidBitmap()
        try { check(bitmap) }
        finally { bitmap.recycle() }
    }
}
