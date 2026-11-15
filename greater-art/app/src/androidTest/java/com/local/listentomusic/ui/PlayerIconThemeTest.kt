package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.ui.theme.appColorScheme
import com.local.listentomusic.ui.theme.artworkAccentScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Uses the normal rendered components; no new production API is needed by the old APK. */
class PlayerIconThemeTest {
    @get:Rule val compose = createComposeRule()
    private val base = appColorScheme(ColorTheme.VIDEO, true)
    private val red = artworkAccentScheme(base, List(3) { 0xffff3018.toInt() }, 1f, ColorTheme.VIDEO)
    private val blue = artworkAccentScheme(base, List(3) { 0xff2050ff.toInt() }, 1f, ColorTheme.VIDEO)
    private val colors = mutableStateOf(red)

    @Test fun ambientLitToolbarIconsFollowBothPalettesAndKeepTheirActions() {
        var homeCalls = 0
        var fullscreenCalls = 0
        compose.setContent {
            GreaterArtTheme(ThemeMode.DARK, ColorTheme.VIDEO) {
                MaterialTheme(colorScheme = colors.value) {
                    Box(Modifier.width(384.dp).background(Color.Black)) {
                        NowPlayingTopBar(AppLanguage.ENGLISH, {}, { homeCalls++ },
                            { fullscreenCalls++ }, {}, frameLit = true, onLock = {}, reservePullHandle = true)
                    }
                }
            }
        }
        for (scheme in listOf(red, blue)) {
            compose.runOnIdle { colors.value = scheme }
            for (label in listOf("Open floating player", "Home", "Lock player controls", "Fullscreen", "Close player")) {
                assertIconTint(label, scheme.secondary)
            }
        }
        assertNotEquals(red.secondary, blue.secondary)
        compose.onNodeWithContentDescription("Home").performClick()
        compose.onNodeWithContentDescription("Fullscreen").performClick()
        compose.runOnIdle {
            assertEquals(1, homeCalls)
            assertEquals(1, fullscreenCalls)
        }
    }

    @Test fun currentMediaAndTransportIconsFollowBothPalettesWithoutChangingCallbacks() {
        val track = MediaFile("/owned/theme.mp3", "Theme fixture", 180_000L, 1000L, 0L, MediaKind.AUDIO)
        val other = track.copy(path = "/owned/next.mp3", name = "Next fixture")
        var favouriteCalls = 0
        var shareCalls = 0
        var previousCalls = 0
        var nextCalls = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 760.dp))) {
                GreaterArtTheme(ThemeMode.DARK, ColorTheme.VIDEO) {
                    MaterialTheme(colorScheme = colors.value) {
                        CompositionLocalProvider(LocalContentColor provides colors.value.onSurface) {
                            NowPlayingScreen(
                                floatingWindowMode = FloatingWindowMode.MINI_WINDOW,
                                onFloatingWindowModeChange = {},
                                playback = PlaybackUiState(connected = true, currentPath = track.path,
                                    title = track.name, currentQueueIndex = 0, durationMs = track.durationMs,
                                    positionMs = 12_000L, hasPrevious = true, hasNext = true),
                                artwork = null, queue = listOf(track, other), lyrics = null,
                                showFileDetails = false, editableQueue = false, blackDiscMode = false,
                                language = AppLanguage.ENGLISH, controller = null, contentPadding = PaddingValues(0.dp),
                                isPictureInPicture = false, onVideoBoundsChanged = {},
                                onPictureInPicture = {}, onHome = {}, onClose = {}, onTogglePlay = {},
                                onPrevious = { previousCalls++ }, onNext = { nextCalls++ },
                                onSeek = {}, onSpeed = {}, onRepeat = {}, onSleepTimer = {}, sleepTimer = SleepTimerState(),
                                onSeekBy = {}, onPlayQueueItem = {}, onLoadThumbnail = { null }, onLoadWaveform = { null },
                                onMoveQueueItem = { _, _ -> }, onRemoveQueueItem = {},
                                onBeginTemporaryDoubleSpeed = { false }, onEndTemporaryDoubleSpeed = {},
                                onLockTemporaryDoubleSpeed = { false }, isFavourite = false,
                                onToggleFavourite = { favouriteCalls++ }, onShareCurrentMedia = { shareCalls++ },
                                onShareQueue = {}, systemOverlay = true,
                                ambientPreferences = UserPreferences(backgroundMode = AppBackgroundMode.DEFAULT),
                            )
                        }
                    }
                }
            }
        }
        for (scheme in listOf(red, blue)) {
            compose.runOnIdle { colors.value = scheme }
            for (label in listOf("Add to Favorites", "Search current queue", "Share", "Previous", "Next")) {
                assertIconTint(label, scheme.secondary)
            }
            assertIconTint("Actions", scheme.secondary, index = 0)
        }
        compose.onNodeWithContentDescription("Add to Favorites").performClick()
        compose.onNodeWithContentDescription("Previous").performClick()
        compose.onNodeWithContentDescription("Next").performClick()
        compose.onNodeWithContentDescription("Share").performClick()
        compose.onNodeWithText("Current media file").performClick()
        compose.runOnIdle {
            assertEquals(1, favouriteCalls)
            assertEquals(1, shareCalls)
            assertEquals(1, previousCalls)
            assertEquals(1, nextCalls)
        }
    }

    private fun assertIconTint(label: String, color: Color, index: Int? = null) {
        val node = if (index == null) compose.onNodeWithContentDescription(label, useUnmergedTree = true)
            else compose.onAllNodesWithContentDescription(label, useUnmergedTree = true)[index]
        val image = node.captureToImage().asAndroidBitmap()
        val expected = color.toArgb()
        val rgb = listOf(16, 8, 0).map { (expected ushr it) and 255 }
        var matches = 0
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val actual = image.getPixel(x, y)
            if (listOf(16, 8, 0).withIndex().all { (index, shift) ->
                    abs(((actual ushr shift) and 255) - rgb[index]) <= 3
                }) matches++
        }
        assertTrue("$label must render the current accent ${expected.toUInt().toString(16)}, matching pixels=$matches",
            matches >= 8)
    }
}
