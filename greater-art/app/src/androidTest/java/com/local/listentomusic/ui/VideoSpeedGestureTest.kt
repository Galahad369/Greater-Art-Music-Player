package com.local.listentomusic.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoSpeedGestureTest {
    @get:Rule val compose = createComposeRule()
    private val speed = mutableStateOf(1f)
    private var previous = 1f

    private fun show(initial: Float) {
        speed.value = initial
        val file = MediaFile("/test/video.mp4", "Video", 180000, 1024, 0, MediaKind.VIDEO)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 800.dp))) {
                MaterialTheme {
                    NowPlayingScreen(FloatingWindowMode.MINI_WINDOW, {},
                        PlaybackUiState(currentPath = file.path, title = file.name, currentQueueIndex = 0,
                            isPlaying = true, durationMs = file.durationMs, speed = speed.value, mediaKind = MediaKind.VIDEO),
                        null, listOf(file), null, false, false, false, AppLanguage.ENGLISH, null,
                        PaddingValues(0.dp), false, {}, {}, {}, {}, {}, {}, {}, {}, { speed.value = it }, {}, {},
                        SleepTimerState(), onSeekBy = {}, onPlayQueueItem = {}, onLoadThumbnail = { null },
                        onLoadWaveform = { null }, onMoveQueueItem = { _, _ -> }, onRemoveQueueItem = {},
                        onBeginTemporaryDoubleSpeed = { previous = speed.value; speed.value = 2f; true },
                        onEndTemporaryDoubleSpeed = { speed.value = previous }, onLockTemporaryDoubleSpeed = { true },
                        isFavourite = false, onToggleFavourite = {}, onShareCurrentMedia = {}, onShareQueue = {},
                        systemOverlay = true)
                }
            }
        }
    }

    private fun hold(hint: String? = null) {
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput {
            down(center)
            advanceEventTime(750)
            moveTo(center)
        }
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("VIDEO_SPEED_HINT").fetchSemanticsNodes().isNotEmpty()
        }
        if (hint == null) return
        compose.onNodeWithText(hint).assertIsDisplayed()
        val stage = compose.onNodeWithTag("VIDEO_STAGE").fetchSemanticsNode().boundsInRoot
        val caption = compose.onNodeWithTag("VIDEO_SPEED_HINT").fetchSemanticsNode().boundsInRoot
        val density = stage.width / 384f
        assertTrue("Hint is at video bottom, not its middle", stage.bottom - caption.bottom in (6f * density)..(40f * density))
        assertTrue("Keep the caption compact", caption.height <= 32f * density)
    }

    @Test fun stationaryReleaseAtDoubleSpeedDoesNotUnlockButReverseUpwardDragDoes() {
        show(2f)
        hold()
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput { up() }
        compose.runOnIdle { assertEquals(2f, speed.value) }
        hold()
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput {
            moveTo(Offset(center.x, center.y - 96.dp.toPx()))
            up()
        }
        compose.runOnIdle { assertEquals(1f, speed.value) }
    }

    @Test fun pullDownStillLocksTheInitialTemporaryHold() {
        show(1f)
        hold("2× · Pull down to lock")
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput {
            moveTo(Offset(center.x, center.y + 96.dp.toPx()))
            up()
        }
        compose.runOnIdle { assertEquals(2f, speed.value) }
    }

    @Test fun cancellingAnUnlockAttemptDoesNotChangeDoubleSpeed() {
        show(2f)
        hold("2× · Pull up for 1×")
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(2f, speed.value) }
    }

    @Test fun initialUnlatchedVideoHoldStillRestoresOnRelease() {
        show(1f)
        hold("2× · Pull down to lock")
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput { up() }
        compose.runOnIdle { assertEquals(1f, speed.value) }
    }

    @Test fun secondPointerCannotUnlockDoubleSpeed() {
        show(2f)
        hold()
        compose.onNodeWithTag("VIDEO_STAGE").performTouchInput {
            down(1, Offset(center.x + 20.dp.toPx(), center.y))
            moveTo(0, Offset(center.x, center.y - 96.dp.toPx()))
            up(1)
            up(0)
        }
        compose.runOnIdle { assertEquals(2f, speed.value) }
    }
}
