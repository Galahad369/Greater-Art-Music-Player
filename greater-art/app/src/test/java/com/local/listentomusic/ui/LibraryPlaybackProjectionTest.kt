package com.local.listentomusic.ui

import com.local.listentomusic.PlaybackUiState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPlaybackProjectionTest {
    @Test fun clockTicksAreOmittedButMediaAndControlsStillPropagate() = runBlocking {
        val initial = PlaybackUiState(currentPath = "one.mp4", isPlaying = true, durationMs = 10_000L)
        val paused = initial.copy(isPlaying = false, positionMs = 1_500L)
        val next = paused.copy(currentPath = "two.mp4", speed = 2f, currentQueueIndex = 1)
        val result = flowOf(initial, initial.copy(positionMs = 500L), initial.copy(positionMs = 1_000L),
            paused, next).libraryShellPlayback().toList()
        assertEquals(listOf(initial, paused.copy(positionMs = 0L), next.copy(positionMs = 0L)), result)
    }
}
