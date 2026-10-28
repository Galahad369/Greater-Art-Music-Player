package com.local.listentomusic.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun showDetachedPlayer(appVisible: Boolean, expandedVisible: Boolean) =
    !appVisible && !expandedVisible

internal fun showDockedPlayer(appVisible: Boolean, libraryVisible: Boolean, expandedVisible: Boolean, windowFocused: Boolean,
    stackTransportVisible: Boolean = false, libraryBackgroundRevealed: Boolean = false, imeVisible: Boolean = false) =
    appVisible && libraryVisible && !expandedVisible && windowFocused &&
        !stackTransportVisible && !libraryBackgroundRevealed && !imeVisible

/** Presentation visibility, independent of video-surface readiness during transfer. */
internal object PlayerWindowVisibility {
    private var appVisible = false
    private var libraryVisible = false
    private var expandedVisible = false
    private var windowFocused = true
    private var stackTransportVisible = false
    private var libraryBackgroundRevealed = false
    private var imeVisible = false
    private val imeAnimations = mutableMapOf<Any, Boolean>()
    private val detached = MutableStateFlow(true)
    private val library = MutableStateFlow(false)
    private val docked = MutableStateFlow(false)
    private val keyboard = MutableStateFlow(false)
    private val dockReservation = MutableStateFlow(false)
    private val expanded = MutableStateFlow(false)
    val detachedVisible = detached.asStateFlow()
    val libraryShowing = library.asStateFlow()
    val dockedVisible = docked.asStateFlow()
    val keyboardShowing = keyboard.asStateFlow()
    // An IME hides the window, not its video surface. Keep the primary decoder
    // attached instead of handing it to the wallpaper during every search.
    val dockSurfaceReserved = dockReservation.asStateFlow()
    val expandedShowing = expanded.asStateFlow()
    fun app(visible: Boolean) { appVisible = visible; publish() }
    // A system overlay otherwise sits ABOVE an Activity's AlertDialog and steals
    // taps intended for Add/Save/Delete. Focus gates only the dock, not detached.
    fun windowFocus(focused: Boolean) { windowFocused = focused; publish() }
    fun library(visible: Boolean) { libraryVisible = visible; library.value = visible; publish() }
    fun stackTransport(visible: Boolean) { stackTransportVisible = visible; publish() }
    fun libraryBackgroundReveal(revealed: Boolean) { libraryBackgroundRevealed = revealed; publish() }
    fun ime(visible: Boolean) {
        if (imeVisible == visible) return
        imeVisible = visible
        publish()
    }
    fun imeAnimationPrepared(animation: Any) {
        imeAnimations[animation] = false
        publish()
    }
    fun imeAnimationStarted(animation: Any) {
        imeAnimations[animation] = true
        publish()
    }
    fun imeAnimationFinished(animation: Any) {
        if (imeAnimations.remove(animation) != null) publish()
    }
    fun discardPreparedImeAnimation(animation: Any) {
        // Android 11 can cancel before onStart without sending onEnd.
        if (imeAnimations[animation] == false) {
            imeAnimations.remove(animation)
            publish()
        }
    }
    fun resetIme() {
        imeVisible = false
        imeAnimations.clear()
        publish()
    }
    fun expanded(visible: Boolean) {
        expandedVisible = visible
        expanded.value = visible
        publish()
    }
    private fun publish() {
        keyboard.value = imeVisible || imeAnimations.isNotEmpty()
        detached.value = showDetachedPlayer(appVisible, expandedVisible)
        dockReservation.value = showDockedPlayer(
            appVisible,
            libraryVisible,
            expandedVisible,
            windowFocused,
            stackTransportVisible,
            libraryBackgroundRevealed,
        )
        docked.value = dockReservation.value && !keyboard.value
    }
}
