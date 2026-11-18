package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercises the actual audio branch, independent of decoder/artwork availability. */
class AudioNowPlayingTest {
    @get:Rule val compose = createComposeRule()

    private val file = MediaFile("/storage/emulated/0/Download/audio.mp3", "Audio track",
        180_000L, 1024L, 0L, MediaKind.AUDIO)
    private val artwork = mutableStateOf<Bitmap?>(null)
    private val mode = mutableStateOf(FloatingWindowMode.MINI_WINDOW)
    private val fullscreen = mutableListOf<Boolean>()
    private var homeCalls = 0
    private var closeCalls = 0
    private var shrinkCalls = 0
    private var playCalls = 0
    private val queueCalls = mutableListOf<Int>()
    private var editCalls = 0
    private var seekCalls = 0

    private fun show(size: DpSize = DpSize(384.dp, 760.dp), disc: Boolean = false,
        initialFullscreen: Boolean = false, rows: List<MediaFile> = listOf(file), editable: Boolean = false) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                MaterialTheme {
                    NowPlayingScreen(
                        floatingWindowMode = mode.value,
                        onFloatingWindowModeChange = { mode.value = it },
                        playback = PlaybackUiState(currentPath = file.path, currentQueueIndex = 0,
                            title = file.name, durationMs = file.durationMs, hasNext = true),
                        artwork = artwork.value,
                        queue = rows,
                        lyrics = null,
                        showFileDetails = true,
                        editableQueue = editable,
                        blackDiscMode = disc,
                        language = AppLanguage.ENGLISH,
                        controller = null,
                        contentPadding = PaddingValues(0.dp),
                        isPictureInPicture = false,
                        onVideoBoundsChanged = {},
                        onPictureInPicture = { shrinkCalls++ },
                        onHome = { homeCalls++ },
                        onClose = { closeCalls++ },
                        onTogglePlay = { playCalls++ },
                        onPrevious = {}, onNext = {}, onSeek = { seekCalls++ }, onSpeed = {}, onRepeat = {},
                        onSleepTimer = {}, sleepTimer = SleepTimerState(), onSeekBy = {},
                        onPlayQueueItem = { queueCalls.add(it) }, onLoadThumbnail = { null }, onLoadWaveform = { null },
                        onMoveQueueItem = { _, _ -> editCalls++ }, onRemoveQueueItem = { editCalls++ },
                        onBeginTemporaryDoubleSpeed = { false }, onEndTemporaryDoubleSpeed = {},
                        onLockTemporaryDoubleSpeed = { false }, isFavourite = false,
                        onToggleFavourite = {}, onShareCurrentMedia = {}, onShareQueue = {},
                        onAddQueueItemToList = { editCalls++ },
                        systemOverlay = true, initialFullscreen = initialFullscreen,
                        onFullscreenChanged = { fullscreen.add(it) },
                    )
                }
            }
        }
    }

    private fun assertChrome() {
        for (label in listOf("Open floating player", "Home", "Fullscreen", "Close player",
            "Lock player controls", floatingModeDescription(AppLanguage.ENGLISH, mode.value))) {
            compose.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithContentDescription("Play").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repeat mode").assertIsDisplayed()
        compose.onNode(hasText("1×") and hasClickAction()).assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertIsDisplayed()
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "audio-now-playing-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun missingAndLateArtworkKeepTopActionsAndTransport() {
        show()
        assertChrome()
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        try {
            compose.runOnIdle { artwork.value = bitmap }
            compose.onNodeWithContentDescription("Album artwork").assertIsDisplayed()
            assertChrome()
            capture("artwork")
            compose.runOnIdle { artwork.value = null }
            assertChrome()
            capture("no-artwork")
            compose.onNodeWithContentDescription("Open floating player").performClick()
            compose.onNodeWithContentDescription("Home").performClick()
            compose.onNodeWithContentDescription("Close player").performClick()
            compose.onNodeWithContentDescription("Play").performClick()
            compose.runOnIdle {
                assertEquals(1, shrinkCalls); assertEquals(1, homeCalls)
                assertEquals(1, closeCalls); assertEquals(1, playCalls)
            }
        } finally {
            compose.runOnIdle { artwork.value = null }
        }
    }

    @Test fun lockModeAndFullscreenRemainFunctionalForAudio() {
        show()
        compose.onNodeWithContentDescription("Lock player controls").performClick()
        compose.onNodeWithContentDescription("Home").assertDoesNotExist()
        compose.onNodeWithContentDescription("Unlock player controls").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Home").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(floatingModeDescription(AppLanguage.ENGLISH, mode.value)).performClick()
        compose.runOnIdle { assertEquals(FloatingWindowMode.FOLLOW_VIDEO, mode.value) }
        compose.onNodeWithContentDescription(floatingModeDescription(AppLanguage.ENGLISH, mode.value)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Fullscreen").performClick()
        compose.onNodeWithContentDescription("Exit fullscreen").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, homeCalls)
            assertEquals(listOf(false, true, false), fullscreen)
        }
    }

    @Test fun lockedQueueScrollsButCannotPlayOrEditUntilUnlocked() {
        val rows = (0..39).map { file.copy(path = "/test/$it.mp3", name = "Queue %02d".format(it)) }
        show(rows = rows, editable = true)
        val playPosition = compose.onNodeWithContentDescription("Play").fetchSemanticsNode().boundsInRoot.center
        val seekPosition = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .fetchSemanticsNode().boundsInRoot.center
        compose.onNodeWithContentDescription("Lock player controls").performClick()
        val list = compose.onNodeWithTag("NOW_PLAYING_QUEUE_LIST")
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        list.performTouchInput { swipeUp() }
        compose.waitForIdle()
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue("Locked queue must still scroll", after > before)
        list.performTouchInput { click(center) }
        list.performTouchInput { click(androidx.compose.ui.geometry.Offset(width * .94f, height * .5f)) }
        val gate = compose.onNodeWithTag("PLAYER_LOCK_INPUT_BLOCKER")
        val origin = gate.fetchSemanticsNode().boundsInRoot.topLeft
        gate.performTouchInput { click(playPosition - origin); click(seekPosition - origin) }
        compose.onNodeWithContentDescription("Play").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(emptyList<Int>(), queueCalls)
            assertEquals(0, editCalls)
            assertEquals(0, playCalls)
            assertEquals(0, seekCalls)
        }
        compose.onNodeWithContentDescription("Unlock player controls").performClick()
        list.performTouchInput { click(androidx.compose.ui.geometry.Offset(width * .4f, height * .5f)) }
        compose.runOnIdle { assertTrue("Unlocked queue must still play", queueCalls.isNotEmpty()) }
    }

    @Test fun audioTimelineReadoutsDoNotOverlapTheSlider() {
        show()
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .fetchSemanticsNode().boundsInRoot
        val hint = compose.onNodeWithText("Waveform unavailable; seeking still works").fetchSemanticsNode().boundsInRoot
        val time = compose.onNodeWithText("0:00").fetchSemanticsNode().boundsInRoot
        assertTrue("Waveform hint stays below the slider", hint.top >= slider.bottom)
        assertTrue("Time stays below the hint", time.top >= hint.bottom)
    }

    @Test fun lockedTouchShowsReminderButDoesNotActivatePlaybackAndNoticeExpires() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Lock player controls").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Controls locked").assertIsDisplayed()
        compose.onNodeWithTag("PLAYER_LOCK_INPUT_BLOCKER").performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Tap lock to unlock").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, playCalls); assertEquals(0, homeCalls) }
        compose.mainClock.advanceTimeBy(1_800)
        compose.onNodeWithTag("PLAYER_LOCK_NOTICE").assertDoesNotExist()
        compose.onNodeWithContentDescription("Unlock player controls").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Home").assertIsDisplayed()
        compose.mainClock.autoAdvance = true
    }

    @Test fun shortLandscapeKeepsAudioControlsAndArtworkVisible() {
        show(DpSize(800.dp, 360.dp))
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        compose.runOnIdle { artwork.value = bitmap }
        assertChrome()
        compose.onNodeWithContentDescription("Album artwork").assertIsDisplayed()
        capture("landscape")
    }

    @Test fun narrowPortraitAndBlackDiscKeepTopActions() {
        show(DpSize(320.dp, 640.dp), disc = true)
        assertChrome()
        compose.onNodeWithTag("BLACK_DISC_ARTWORK").assertIsDisplayed()
        capture("disc")
    }

    @Test fun discIsVisibleWithNoArtworkAndDefaultTheme() {
        show(disc = true)
        compose.onNodeWithTag("BLACK_DISC_ARTWORK").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test fun discIsAbsentWhenSettingDisabled() {
        show(disc = false)
        compose.onNodeWithTag("BLACK_DISC_ARTWORK").assertDoesNotExist()
    }

    @Test fun narrowOverlayFullscreenDoesNotOfferUnusableRotation() {
        show(DpSize(320.dp, 640.dp), initialFullscreen = true)
        for (label in listOf("Lock player controls", "Exit fullscreen", "Close player")) {
            compose.onNodeWithContentDescription(label).assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithContentDescription("Rotate to landscape").assertDoesNotExist()
        compose.onNodeWithContentDescription("Exit fullscreen").performClick()
        assertChrome()
    }

    @Test fun narrowFullscreenToolbarFitsRotationAndChangesDestinationIcon() {
        val landscape = mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                MaterialTheme {
                    NowPlayingTopBar(AppLanguage.ENGLISH, {}, {}, {}, {}, fullscreen = true,
                        landscape = landscape.value, onRotate = { landscape.value = !landscape.value })
                }
            }
        }
        for (label in listOf("Rotate to landscape", "Exit fullscreen", "Close player", "Home")) {
            compose.onNodeWithContentDescription(label).assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithContentDescription("Rotate to landscape").performClick()
        compose.onNodeWithContentDescription("Rotate to portrait").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Rotate to landscape").assertIsDisplayed()
    }
}
