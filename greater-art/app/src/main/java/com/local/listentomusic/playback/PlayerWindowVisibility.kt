package com.local.listentomusic.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun showDetachedPlayer(appVisible: Boolean, expandedVisible: Boolean) =
    !appVisible && !expandedVisible

internal fun showDockedPlayer(appVisible: Boolean, libraryVisible: Boolean, expandedVisible: Boolean, windowFocused: Boolean,
    stackTransportVisible: Boolean = false) =
    appVisible && libraryVisible && !expandedVisible && windowFocused && !stackTransportVisible

/** Presentation visibility, independent of video-surface readiness during transfer. */
internal object PlayerWindowVisibility {
    private var appVisible = false
    private var libraryVisible = false
    private var expandedVisible = false
    private var windowFocused = true
    private var stackTransportVisible = false
    private val detached = MutableStateFlow(true)
    private val library = MutableStateFlow(false)
    private val docked = MutableStateFlow(false)
    private val expanded = MutableStateFlow(false)
    val detachedVisible = detached.asStateFlow()
    val libraryShowing = library.asStateFlow()
    val dockedVisible = docked.asStateFlow()
    val expandedShowing = expanded.asStateFlow()
    fun app(visible: Boolean) { appVisible = visible; publish() }
    // A system overlay otherwise sits ABOVE an Activity's AlertDialog and steals
    // taps intended for Add/Save/Delete. Focus gates only the dock, not detached.
    fun windowFocus(focused: Boolean) { windowFocused = focused; publish() }
    fun library(visible: Boolean) { libraryVisible = visible; library.value = visible; publish() }
    fun stackTransport(visible: Boolean) { stackTransportVisible = visible; publish() }
    fun expanded(visible: Boolean) {
        expandedVisible = visible
        expanded.value = visible
        publish()
    }
    private fun publish() {
        detached.value = showDetachedPlayer(appVisible, expandedVisible)
        docked.value = showDockedPlayer(appVisible, libraryVisible, expandedVisible, windowFocused, stackTransportVisible)
    }
}
