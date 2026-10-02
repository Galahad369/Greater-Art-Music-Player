package com.local.listentomusic.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether any virtualized media list is mid-scroll/fling.
 * Used to pause decorative video wallpaper so the primary decoder and
 * Compose scroll work are not fighting a second ExoPlayer for GPU/time.
 */
object ListScrollBudget {
    private val holders = linkedSetOf<String>()
    private val mutable = MutableStateFlow(false)
    val scrolling: StateFlow<Boolean> = mutable.asStateFlow()

    @Synchronized
    fun set(holderId: String, scrolling: Boolean) {
        if (scrolling) holders.add(holderId) else holders.remove(holderId)
        val next = holders.isNotEmpty()
        if (mutable.value != next) mutable.value = next
    }
}
