package com.local.listentomusic.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.local.listentomusic.PlaybackUiState
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.AppBackground
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BackgroundScrollRecompositionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrollEdgesDoNotRecomposeTheShell() {
        val scrolling = MutableStateFlow(false)
        val shellCommits = AtomicInteger()
        val preferences = UserPreferences(backgroundMode = AppBackgroundMode.DEFAULT)
        compose.setContent {
            val scrollState = scrolling.collectAsStateWithLifecycle()
            SideEffect { shellCommits.incrementAndGet() }
            GreaterArtTheme(ThemeMode.DARK) {
                AppBackground(preferences, null, false, null, Modifier, listScrolling = scrollState)
                ScrollProbe(scrollState)
            }
        }
        compose.waitForIdle()
        val baseline = shellCommits.get()
        assertTrue(baseline > 0)
        compose.runOnIdle { scrolling.value = true }
        compose.waitForIdle()
        compose.onNodeWithText("scrolling").assertExists()
        assertEquals(baseline, shellCommits.get())
        compose.runOnIdle { scrolling.value = false }
        compose.waitForIdle()
        compose.onNodeWithText("idle").assertExists()
        assertEquals(baseline, shellCommits.get())
    }

    @Composable private fun ScrollProbe(state: State<Boolean>) {
        Text(if (state.value) "scrolling" else "idle")
    }

    @Test fun clockUpdatesReachTheLeafWithoutRecomposingTheShell() {
        val initial = PlaybackUiState(currentPath = "one.mp4", isPlaying = true)
        val playback = MutableStateFlow(initial)
        val commits = AtomicInteger()
        compose.setContent {
            val projected = remember { playback.libraryShellPlayback() }
            val shell by projected.collectAsStateWithLifecycle(initialValue = initial)
            SideEffect { commits.incrementAndGet() }
            Text(if (shell.isPlaying) "playing" else "paused")
            ClockProbe(playback)
        }
        compose.waitForIdle()
        val baseline = commits.get()
        compose.runOnIdle { playback.value = playback.value.copy(positionMs = 500L) }
        compose.onNodeWithText("position=500").assertExists()
        compose.runOnIdle { assertEquals(baseline, commits.get()) }
        compose.runOnIdle { playback.value = playback.value.copy(isPlaying = false) }
        compose.onNodeWithText("paused").assertExists()
        compose.runOnIdle { assertTrue(commits.get() > baseline) }
    }

    @Composable private fun ClockProbe(source: StateFlow<PlaybackUiState>) {
        val state by source.collectAsStateWithLifecycle()
        Text("position=${state.positionMs}")
    }
}
