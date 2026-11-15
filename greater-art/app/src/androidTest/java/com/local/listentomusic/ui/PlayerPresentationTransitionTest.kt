package com.local.listentomusic.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import com.local.listentomusic.ui.theme.GreaterArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerPresentationTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun overlayRequestsFullscreenWithoutAnIntermediateVideoRelayout() {
        val requests = mutableListOf<Boolean>()
        val file = MediaFile("/owned/transition.mp4", "Transition fixture", 180_000L, 1000L, 0L, MediaKind.VIDEO)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 760.dp))) {
                GreaterArtTheme(ThemeMode.DARK) {
                    NowPlayingScreen(
                        floatingWindowMode = FloatingWindowMode.MINI_WINDOW,
                        onFloatingWindowModeChange = {},
                        playback = PlaybackUiState(connected = true, currentPath = file.path,
                            title = file.name, durationMs = file.durationMs, videoAspectRatio = 16f / 9f),
                        artwork = null, queue = listOf(file), lyrics = null, showFileDetails = false,
                        editableQueue = false, blackDiscMode = false, language = AppLanguage.ENGLISH,
                        controller = null, contentPadding = PaddingValues(0.dp),
                        isPictureInPicture = false, onVideoBoundsChanged = {},
                        onPictureInPicture = {}, onHome = {}, onClose = {}, onTogglePlay = {},
                        onPrevious = {}, onNext = {}, onSeek = {}, onSpeed = {}, onRepeat = {},
                        onSleepTimer = {}, sleepTimer = SleepTimerState(), onSeekBy = {},
                        onPlayQueueItem = {}, onLoadThumbnail = { null }, onLoadWaveform = { null },
                        onMoveQueueItem = { _, _ -> }, onRemoveQueueItem = {},
                        onBeginTemporaryDoubleSpeed = { false }, onEndTemporaryDoubleSpeed = {},
                        onLockTemporaryDoubleSpeed = { false }, isFavourite = false,
                        onToggleFavourite = {}, onShareCurrentMedia = {}, onShareQueue = {},
                        systemOverlay = true, onFullscreenChanged = { requests += it },
                        ambientPreferences = UserPreferences(backgroundMode = AppBackgroundMode.AMBIENT,
                            adaptiveIntensity = 0f, adaptiveMotion = AmbientMotion.OFF),
                    )
                }
            }
        }
        val title = compose.onNodeWithTag("CURRENT_MEDIA_TITLE", useUnmergedTree = true)
        val before = title.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Fullscreen").performClick()
        compose.waitForIdle()
        // The Activity owns the next layout. Until it arrives, keep the working
        // overlay geometry instead of first enlarging this same surface locally.
        assertEquals(before, title.assertIsDisplayed().fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { assertEquals(listOf(false, true), requests) }
    }
}
