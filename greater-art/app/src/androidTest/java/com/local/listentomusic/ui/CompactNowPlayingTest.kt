package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AmbientMotion
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.ui.theme.GreaterArtTheme
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compact portrait presentation, without changing the player or sharing contracts. */
class CompactNowPlayingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun audioAt320KeepsCompactControlsAndActions() = exercise(DpSize(320.dp, 640.dp), false)
    @Test fun audioAt384KeepsCompactControlsAndActions() = exercise(DpSize(384.dp, 760.dp), false)
    @Test fun videoAt320KeepsCompactControlsAndActions() = exercise(DpSize(320.dp, 640.dp), true)
    @Test fun videoAt384KeepsCompactControlsAndActions() = exercise(DpSize(384.dp, 760.dp), true)
    @Test fun audioLightHeaderStaysTransparent() = exercise(DpSize(320.dp, 640.dp), false, ThemeMode.LIGHT)
    @Test fun videoLightHeaderStaysTransparent() = exercise(DpSize(320.dp, 640.dp), true, ThemeMode.LIGHT)

    private fun exercise(size: DpSize, video: Boolean, theme: ThemeMode = ThemeMode.DARK) {
        assertFalse("The isolated compact-player fixture requires no active Stack", StackPlayback.state.value.active)
        val extension = if (video) "mp4" else "mp3"
        val files = List(20) { index ->
            MediaFile("/owned/compact-$index.$extension",
                "Track $index · 【syudou】美学_PENTATONIC_プライド Live Digest【期間限定】孤独毒毒.$extension",
                180_000L, 1_048_576L, 0L, if (video) MediaKind.VIDEO else MediaKind.AUDIO)
        }
        val current = files[1]
        val playback = mutableStateOf(PlaybackUiState(
            connected = true, currentPath = current.path, title = current.name,
            currentQueueIndex = 1, durationMs = current.durationMs, positionMs = 12_000L,
            hasPrevious = true, hasNext = true, videoAspectRatio = 16f / 9f,
        ))
        val favourite = mutableStateOf(false)
        var favouriteCalls = 0
        var shareFileCalls = 0
        var shareQueueCalls = 0
        var playCalls = 0
        var previousCalls = 0
        var nextCalls = 0
        var repeatCalls = 0
        val speeds = mutableListOf<Float>()
        val seeks = mutableListOf<Long>()
        val playedQueueIndices = mutableListOf<Int>()
        var density = 0f

        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                GreaterArtTheme(theme) {
                    density = LocalDensity.current.density
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                        NowPlayingScreen(
                            floatingWindowMode = FloatingWindowMode.MINI_WINDOW,
                            onFloatingWindowModeChange = {}, playback = playback.value,
                            artwork = null, queue = files, lyrics = null, showFileDetails = true,
                            editableQueue = false, blackDiscMode = false, language = AppLanguage.ENGLISH,
                            controller = null, contentPadding = PaddingValues(0.dp),
                            isPictureInPicture = false, onVideoBoundsChanged = {},
                            onPictureInPicture = {}, onHome = {}, onClose = {},
                            onTogglePlay = { playCalls++; playback.value = playback.value.copy(isPlaying = !playback.value.isPlaying) },
                            onPrevious = { previousCalls++ }, onNext = { nextCalls++ },
                            onSeek = { seeks += it },
                            onSpeed = { speeds += it; playback.value = playback.value.copy(speed = it) },
                            onRepeat = { repeatCalls++ }, onSleepTimer = {}, sleepTimer = SleepTimerState(),
                            onSeekBy = {}, onPlayQueueItem = { playedQueueIndices += it },
                            onLoadThumbnail = { null }, onLoadWaveform = { FloatArray(40) { .5f } },
                            onMoveQueueItem = { _, _ -> }, onRemoveQueueItem = {},
                            onBeginTemporaryDoubleSpeed = { false }, onEndTemporaryDoubleSpeed = {},
                            onLockTemporaryDoubleSpeed = { false }, isFavourite = favourite.value,
                            onToggleFavourite = { favouriteCalls++; favourite.value = !favourite.value },
                            onShareCurrentMedia = { shareFileCalls++ }, onShareQueue = { shareQueueCalls++ },
                            systemOverlay = true,
                            ambientPreferences = UserPreferences(backgroundMode = AppBackgroundMode.ADAPTIVE_DYNAMIC,
                                adaptiveIntensity = 0f, adaptiveMotion = AmbientMotion.OFF, backgroundDim = 0f),
                        )
                    }
                }
            }
        }

        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_200)
        compose.waitForIdle()
        assertHeaderIsTransparent(density)
        val title = compose.onNodeWithTag("CURRENT_MEDIA_TITLE", useUnmergedTree = true).assertIsDisplayed()
        val textLayouts = mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayouts) }
        assertEquals(16.sp, textLayouts.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.Medium, textLayouts.single().layoutInput.style.fontWeight)
        assertEquals("A long title must not create another row", 1, textLayouts.single().lineCount)
        compose.onAllNodesWithTag("PLAYER_HEADER_ACTION_ICON", useUnmergedTree = true).assertCountEquals(3)
        repeat(3) { index ->
            compose.onAllNodesWithTag("PLAYER_HEADER_ACTION_ICON", useUnmergedTree = true)[index]
                .assertWidthIsEqualTo(22.dp).assertHeightIsEqualTo(22.dp)
        }
        compose.onNodeWithTag("PLAY_PAUSE_BUTTON", useUnmergedTree = true)
            .assertIsDisplayed().assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        for (label in listOf("Previous", "Play", "Next", "Repeat mode", "Add to Favorites", "Search current queue", "Share")) {
            compose.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        val speed = compose.onNode(hasText("1×") and hasClickAction())
        speed.assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.assertIsDisplayed()

        val transportBefore = bounds("TRANSPORT_CONTROLS")
        val seekBefore = slider.fetchSemanticsNode().boundsInRoot
        val queueBounds = bounds("NOW_PLAYING_QUEUE")
        assertTrue("The flexible queue must remain above the fixed timeline", queueBounds.bottom <= seekBefore.top + 1f)
        assertTrue("Transport must fit inside the visible player", transportBefore.bottom <= compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom)
        val list = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
        list.performScrollToIndex(19)
        assertEquals("List scrolling must not move transport", transportBefore, bounds("TRANSPORT_CONTROLS"))
        assertEquals("List scrolling must not move seeking", seekBefore, slider.fetchSemanticsNode().boundsInRoot)
        list.performScrollToIndex(0)
        capture("${if (video) "video" else "audio"}-${size.width.value.toInt()}-$theme-portrait")

        compose.onNodeWithContentDescription("Add to Favorites").performClick()
        compose.onNodeWithContentDescription("Remove from Favorites").assertIsDisplayed()
        compose.onNodeWithContentDescription("Share").performClick()
        compose.onNodeWithText("Current media file").performClick()
        compose.onNodeWithContentDescription("Share").performClick()
        compose.onNodeWithText("Queue as M3U8").performClick()
        compose.onNodeWithContentDescription("Search current queue").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Track 19")
        compose.onNodeWithText(files.last().name.substringBeforeLast('.')).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithContentDescription("Close search").performClick()
        speed.performClick()
        compose.onNodeWithText("1.25×").performClick()
        compose.onNode(hasText("1.25×") and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").performClick()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous").performClick()
        compose.onNodeWithContentDescription("Next").performClick()
        compose.onNodeWithContentDescription("Repeat mode").performClick()
        slider.performTouchInput { click(Offset(width * .6f, center.y)) }
        compose.runOnIdle {
            assertEquals(1, favouriteCalls)
            assertEquals(1, shareFileCalls)
            assertEquals(1, shareQueueCalls)
            assertEquals(listOf(19), playedQueueIndices)
            assertEquals(listOf(1.25f), speeds)
            assertEquals(1, playCalls)
            assertEquals(1, previousCalls)
            assertEquals(1, nextCalls)
            assertEquals(1, repeatCalls)
            assertTrue("Timeline must still send a bounded seek", seeks.isNotEmpty() && seeks.all { it in 0L..current.durationMs })
        }
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun assertHeaderIsTransparent(density: Float) {
        compose.onNodeWithTag("CURRENT_MEDIA_HEADER_CONTAINER", useUnmergedTree = true).assertIsDisplayed()
        val header = bounds("CURRENT_MEDIA_HEADER_CONTAINER")
        val root = compose.onRoot()
        val rootBounds = root.fetchSemanticsNode().boundsInRoot
        val image = root.captureToImage().asAndroidBitmap()
        try {
            val inset = 2f * density
            assertTrue("The fixture needs backdrop outside the title holder", header.left - inset >= rootBounds.left)
            // Both fixtures use a 48dp identity row. Its top 2dp is blank above the
            // centered text/icons; audio intentionally has no horizontal gutter.
            // Zero ambient intensity makes the backdrop uniform, so compare one
            // blank interior pixel with the same frame immediately outside it.
            val y = ((header.top + inset - rootBounds.top) * image.height / rootBounds.height)
                .roundToInt().coerceIn(0, image.height - 1)
            val insideX = ((header.center.x - rootBounds.left) * image.width / rootBounds.width)
                .roundToInt().coerceIn(0, image.width - 1)
            val outsideX = ((header.left - inset - rootBounds.left) * image.width / rootBounds.width)
                .roundToInt().coerceIn(0, image.width - 1)
            assertEquals("Blank title-holder padding must reveal the unchanged backdrop",
                image.getPixel(outsideX, y), image.getPixel(insideX, y))
        } finally { image.recycle() }
    }

    private fun capture(label: String) {
        if (InstrumentationRegistry.getArguments().getString("compactPlayerScreenshots") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(requireNotNull(context.getExternalFilesDir(null)), "compact-player-$label-${UUID.randomUUID()}.png")
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        Log.i("CompactNowPlayingTest", "SCREENSHOT ${file.absolutePath}")
    }
}
