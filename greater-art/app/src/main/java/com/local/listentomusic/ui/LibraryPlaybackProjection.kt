package com.local.listentomusic.ui

import com.local.listentomusic.PlaybackUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Shell consumers use media/control identity, never this intentionally omitted clock. */
internal fun Flow<PlaybackUiState>.libraryShellPlayback(): Flow<PlaybackUiState> =
    map { it.copy(positionMs = 0L) }.distinctUntilChanged()
