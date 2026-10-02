package com.local.listentomusic.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether any virtualized media list is mid-scroll/fling.
 *
 * YouTube-style budget: while any list is flinging, decorative video wallpaper
 * detaches its surface and pauses decode so Compose/GPU scroll work is not
 * competing with a second video pipeline. The wallpaper player instance stays
 * alive so settling does not re-allocate a decoder.
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
