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

    private fun show(size: DpSize = DpSize(384.dp, 760.dp), disc: Boolean = false,
        initialFullscreen: Boolean = false) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                MaterialTheme {
                    NowPlayingScreen(
                        floatingWindowMode = mode.value,
                        onFloatingWindowModeChange = { mode.value = it },
                        playback = PlaybackUiState(currentPath = file.path, currentQueueIndex = 0,
                            title = file.name, durationMs = file.durationMs, hasNext = true),
                        artwork = artwork.value,
                        queue = listOf(file),
                        lyrics = null,
                        showFileDetails = true,
                        editableQueue = false,
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
                        onPrevious = {}, onNext = {}, onSeek = {}, onSpeed = {}, onRepeat = {},
                        onSleepTimer = {}, sleepTimer = SleepTimerState(), onSeekBy = {},
                        onPlayQueueItem = {}, onLoadThumbnail = { null }, onLoadWaveform = { null },
                        onMoveQueueItem = { _, _ -> }, onRemoveQueueItem = {},
                        onBeginTemporaryDoubleSpeed = { false }, onEndTemporaryDoubleSpeed = {},
                        onLockTemporaryDoubleSpeed = { false }, isFavourite = false,
                        onToggleFavourite = {}, onShareCurrentMedia = {}, onShareQueue = {},
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
        capture("disc")
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
