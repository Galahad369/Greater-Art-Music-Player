package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AmbientMotion
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.ui.theme.GreaterArtTheme
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercise the actual player route, not only an isolated AppBackground modifier. */
class NowPlayingReflectionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun currentVideoAudioDarkUsesTheReflectedPlayerBackdrop() =
        exercise(false, ThemeMode.DARK)

    @Test fun currentVideoAudioLightUsesTheReflectedPlayerBackdrop() =
        exercise(false, ThemeMode.LIGHT)

    @Test fun currentVideoVideoDarkUsesTheReflectedPlayerBackdrop() =
        exercise(true, ThemeMode.DARK)

    @Test fun currentVideoVideoLightUsesTheReflectedPlayerBackdrop() =
        exercise(true, ThemeMode.LIGHT)

    @Test fun zeroIntensityAudioDarkHasNoMediaTintAndKeepsTheTitleTransparent() =
        exercise(false, ThemeMode.DARK, zeroIntensity = true)

    @Test fun zeroIntensityVideoLightHasNoMediaTintAndKeepsTheTitleTransparent() =
        exercise(true, ThemeMode.LIGHT, zeroIntensity = true)

    @Test fun maximumStrengthVideoDarkPreservesControlsAndLayout() =
        exercise(true, ThemeMode.DARK, strong = true)

    @Test fun maximumStrengthAudioLightPreservesControlsAndLayout() =
        exercise(false, ThemeMode.LIGHT, strong = true)

    private fun exercise(video: Boolean, theme: ThemeMode, zeroIntensity: Boolean = false, strong: Boolean = false) {
        assertFalse("The isolated player fixture requires no active Stack", StackPlayback.state.value.active)
        val red = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xfff02030.toInt()) }
        val blue = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff2040ff.toInt()) }
        val neutral = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff9b9b9b.toInt()) }
        val art = mutableStateOf<Bitmap?>(red)
        val mode = mutableStateOf(AppBackgroundMode.CURRENT_VIDEO)
        val extension = if (video) "mp4" else "mp3"
        val fixtureId = UUID.randomUUID()
        val files = List(12) { index ->
            MediaFile("/owned/player-reflection-$fixtureId-$index.$extension", "Reflection track $index.$extension",
                180_000L, 1_048_576L, 0L, if (video) MediaKind.VIDEO else MediaKind.AUDIO)
        }
        val playback = PlaybackUiState(connected = true, currentPath = files[1].path,
            title = files[1].name, currentQueueIndex = 1, durationMs = 180_000L,
            positionMs = 12_000L, hasPrevious = true, hasNext = true, videoAspectRatio = 16f / 9f)
        var density = 0f
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                GreaterArtTheme(theme) {
                    density = LocalDensity.current.density
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                        NowPlayingScreen(
                            floatingWindowMode = FloatingWindowMode.MINI_WINDOW, onFloatingWindowModeChange = {},
                            playback = playback, artwork = art.value, queue = files, lyrics = null,
                            showFileDetails = false, editableQueue = false, blackDiscMode = false,
                            language = AppLanguage.ENGLISH, controller = null, contentPadding = PaddingValues(0.dp),
                            isPictureInPicture = false, onVideoBoundsChanged = {}, onPictureInPicture = {},
                            onHome = {}, onClose = {}, onTogglePlay = {}, onPrevious = {}, onNext = {},
                            onSeek = {}, onSpeed = {}, onRepeat = {}, onSleepTimer = {},
                            sleepTimer = SleepTimerState(), onSeekBy = {}, onPlayQueueItem = {},
                            onLoadThumbnail = { null }, onLoadWaveform = { FloatArray(40) { .5f } },
                            onMoveQueueItem = { _, _ -> }, onRemoveQueueItem = {},
                            onBeginTemporaryDoubleSpeed = { false }, onEndTemporaryDoubleSpeed = {},
                            onLockTemporaryDoubleSpeed = { false }, isFavourite = false,
                            onToggleFavourite = {}, onShareCurrentMedia = {}, onShareQueue = {},
                            systemOverlay = true,
                            ambientPreferences = UserPreferences(backgroundMode = mode.value,
                                adaptiveIntensity = if (zeroIntensity) 0f else if (strong) 1f else .5f,
                                adaptiveMotion = AmbientMotion.OFF, backgroundDim = if (strong) .25f else .35f),
                        )
                    }
                }
            }
        }
        try {
            settlePalette()
            val titleBounds = bounds("CURRENT_MEDIA_HEADER_CONTAINER")
            val transportBounds = bounds("TRANSPORT_CONTROLS")
            val queueBounds = bounds("NOW_PLAYING_QUEUE")
            compose.onNodeWithTag("CURRENT_MEDIA_TITLE", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("PLAY_PAUSE_BUTTON", useUnmergedTree = true)
                .assertIsDisplayed().assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            for (label in listOf("Previous", "Play", "Next", "Repeat mode", "Add to Favorites", "Search current queue", "Share")) {
                compose.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
                    .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            }
            val label = "${if (video) "video" else "audio"}-$theme-${if (zeroIntensity) "zero" else if (strong) "strong" else "default"}"
            val warm = captureBackdrop("$label-current-video-warm", density)
            if (zeroIntensity) {
                assertEquals("Zero intensity must leave a uniform backdrop", 1, warm.distinct().size)
                assertTransparentTitleOnUniformBackdrop(density)
            } else {
                assertTrue("The actual player must render nonuniform reflected light at the selected dimming", warm.distinct().size >= 3)
            }

            compose.runOnIdle { mode.value = AppBackgroundMode.ADAPTIVE_DYNAMIC }
            settlePalette()
            val adaptive = captureBackdrop("$label-adaptive-warm", density)
            assertEquivalentBackdrop("The browsing wallpaper selection must not remove the player's reflected treatment", warm, adaptive)
            assertEquals(titleBounds, bounds("CURRENT_MEDIA_HEADER_CONTAINER"))
            assertEquals(transportBounds, bounds("TRANSPORT_CONTROLS"))
            assertEquals(queueBounds, bounds("NOW_PLAYING_QUEUE"))

            compose.runOnIdle { mode.value = AppBackgroundMode.CURRENT_VIDEO; art.value = blue }
            settlePalette()
            val cool = captureBackdrop("$label-current-video-cool", density)
            if (zeroIntensity) assertEquivalentBackdrop("Zero intensity must not leak a changed artwork palette", warm, cool)
            else assertTrue("The actual player backdrop must follow media colour, not a fixed theme gradient", warm != cool)

            compose.runOnIdle { art.value = neutral }
            settlePalette()
            val silver = captureBackdrop("$label-current-video-neutral", density)
            if (zeroIntensity) assertEquivalentBackdrop("Zero intensity must remain quiet for neutral media", warm, silver)
            else assertTrue("Neutral artwork must retain visible light structure rather than a blank slab", silver.distinct().size >= 3)
            assertEquals(titleBounds, bounds("CURRENT_MEDIA_HEADER_CONTAINER"))
            assertEquals(transportBounds, bounds("TRANSPORT_CONTROLS"))
        } finally {
            compose.runOnIdle { art.value = null }
            compose.waitForIdle()
            red.recycle(); blue.recycle(); neutral.recycle()
        }
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun settlePalette() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_200)
        compose.waitForIdle()
    }

    private fun captureBackdrop(label: String, density: Float): List<Int> {
        val root = compose.onRoot()
        val rootBounds = root.fetchSemanticsNode().boundsInRoot
        val queue = bounds("NOW_PLAYING_QUEUE")
        val image = root.captureToImage().asAndroidBitmap()
        try {
            save(image, label)
            // The player has 12/16dp content gutters. Sample their empty outer
            // strip alongside the flexible queue, never artwork, glyphs or cards.
            return listOf(3f, 6f).flatMap { insetDp ->
                val x = (insetDp * density * image.width / rootBounds.width)
                    .roundToInt().coerceIn(0, image.width - 1)
                List(21) { index ->
                    val yInRoot = queue.top + queue.height * (.04f + .92f * index / 20f)
                    val y = ((yInRoot - rootBounds.top) * image.height / rootBounds.height)
                        .roundToInt().coerceIn(0, image.height - 1)
                    image.getPixel(x, y)
                }
            }
        } finally { image.recycle() }
    }

    private fun assertEquivalentBackdrop(message: String, expected: List<Int>, actual: List<Int>) {
        assertEquals(message, expected.size, actual.size)
        // Allow one-pixel-channel rounding while still distinguishing the old
        // plain-gradient route from the actual reflected treatment.
        assertTrue(message, expected.zip(actual).all { (first, second) ->
            abs(Color.red(first) - Color.red(second)) <= 1 &&
                abs(Color.green(first) - Color.green(second)) <= 1 &&
                abs(Color.blue(first) - Color.blue(second)) <= 1
        })
    }

    private fun assertTransparentTitleOnUniformBackdrop(density: Float) {
        val root = compose.onRoot()
        val rootBounds = root.fetchSemanticsNode().boundsInRoot
        val header = bounds("CURRENT_MEDIA_HEADER_CONTAINER")
        val image = root.captureToImage().asAndroidBitmap()
        try {
            val y = ((header.top + 2f * density - rootBounds.top) * image.height / rootBounds.height)
                .roundToInt().coerceIn(0, image.height - 1)
            val insideX = ((header.center.x - rootBounds.left) * image.width / rootBounds.width)
                .roundToInt().coerceIn(0, image.width - 1)
            val outsideX = ((header.left - 2f * density - rootBounds.left) * image.width / rootBounds.width)
                .roundToInt().coerceIn(0, image.width - 1)
            assertEquals("Blank title-holder padding must remain transparent", image.getPixel(outsideX, y), image.getPixel(insideX, y))
        } finally { image.recycle() }
    }

    private fun save(image: Bitmap, label: String) {
        if (InstrumentationRegistry.getArguments().getString("nowPlayingReflectionScreenshots") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(requireNotNull(context.getExternalFilesDir(null)), "player-reflections-$label-${UUID.randomUUID()}.png")
        file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        Log.i("NowPlayingReflectionsTest", "SCREENSHOT ${file.absolutePath}")
    }
}
