package com.local.listentomusic.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun showDetachedPlayer(appVisible: Boolean, expandedVisible: Boolean) =
    !appVisible && !expandedVisible

/** Presentation visibility, independent of video-surface readiness during transfer. */
internal object PlayerWindowVisibility {
    private var appVisible = false
    private var libraryVisible = false
    private var expandedVisible = false
    private val detached = MutableStateFlow(true)
    private val library = MutableStateFlow(false)
    private val docked = MutableStateFlow(false)
    private val expanded = MutableStateFlow(false)
    val detachedVisible = detached.asStateFlow()
    val libraryShowing = library.asStateFlow()
    val dockedVisible = docked.asStateFlow()
    val expandedShowing = expanded.asStateFlow()
    fun app(visible: Boolean) { appVisible = visible; publish() }
    fun library(visible: Boolean) { libraryVisible = visible; library.value = visible; publish() }
    fun expanded(visible: Boolean) {
        expandedVisible = visible
        expanded.value = visible
        publish()
    }
    private fun publish() {
        detached.value = showDetachedPlayer(appVisible, expandedVisible)
        docked.value = appVisible && libraryVisible && !expandedVisible
    }
}
