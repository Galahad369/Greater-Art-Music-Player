package com.local.listentomusic.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaybackSpeedHoldTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bottomButtonRestoresFasterOriginalSpeedOnRelease() {
        exercise(fullscreen = false, initialSpeed = 3f)
    }

    @Test fun fullscreenButtonRestoresSpeedOnCancelAndDisposal() {
        exercise(fullscreen = true, initialSpeed = 1.25f)
    }

    private fun exercise(fullscreen: Boolean, initialSpeed: Float) {
        val speed = mutableStateOf(initialSpeed)
        val showButton = mutableStateOf(true)
        var starts = 0
        var ends = 0
        var previous = initialSpeed
        val begin = {
            starts++
            previous = speed.value
            speed.value = 2f
            true
        }
        val end = { ends++; speed.value = previous }
        val select: (Float) -> Unit = { speed.value = it }
        compose.setContent {
            if (showButton.value) {
                if (fullscreen) {
                    NowPlayingTopBar(
                        AppLanguage.ENGLISH, {}, {}, {}, {}, fullscreen = true,
                        speed = speed.value, onSpeed = select,
                        onBeginSpeedHold = begin, onEndSpeedHold = end,
                    )
                } else {
                    PlayerBottomControls(
                        PlaybackUiState(speed = speed.value), {}, {}, {}, {}, select, begin, end,
                    )
                }
            }
        }
        val tag = if (fullscreen) "FULLSCREEN_SPEED_BUTTON" else "SPEED_BUTTON"
        val button = compose.onNodeWithTag(tag)
        fun hold() {
            button.performTouchInput {
                down(center)
                advanceEventTime(750)
                moveTo(center)
            }
            compose.waitUntil(timeoutMillis = 5_000) { speed.value == 2f }
            compose.onNodeWithText("2×").assertIsDisplayed()
            compose.onNodeWithText("0.75×").assertDoesNotExist()
        }
        hold()
        button.performTouchInput { if (fullscreen) cancel() else up() }
        compose.waitUntil(timeoutMillis = 5_000) { speed.value == initialSpeed }
        compose.onNodeWithText("0.75×").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, starts)
            assertEquals(1, ends)
        }
        button.performTouchInput { click() }
        compose.onNodeWithText("0.75×").assertIsDisplayed().performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 5_000) { speed.value == .75f }
        if (fullscreen) {
            hold()
            compose.runOnIdle { showButton.value = false }
            compose.waitUntil(timeoutMillis = 5_000) { speed.value == .75f }
            compose.onRoot().performTouchInput { cancel() }
            compose.runOnIdle {
                assertEquals(2, starts)
                assertEquals(2, ends)
            }
        }
    }
}
