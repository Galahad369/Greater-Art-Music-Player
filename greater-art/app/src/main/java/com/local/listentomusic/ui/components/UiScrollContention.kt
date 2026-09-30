package com.local.listentomusic.ui.components

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide scroll busy signal so secondary work (CURRENT_VIDEO wallpaper decode)
 * can freeze while Library / Now Playing queue flings without changing primary playback.
 */
enum class UiScrollSource {
    LIBRARY,
    NOW_PLAYING_QUEUE,
}

object UiScrollContention {
    private val active = ConcurrentHashMap.newKeySet<UiScrollSource>()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun set(source: UiScrollSource, scrolling: Boolean) {
        if (scrolling) active.add(source) else active.remove(source)
        _busy.value = active.isNotEmpty()
    }
}

/**
 * Secondary wallpaper decode policy.
 * - CUSTOM_VIDEO may play when lifecycle/primary allow and UI is not contending.
 * - CURRENT_VIDEO mirrors the same file as the primary player; keep it paused on an
 *   aligned frame so mid-range GPUs are not dual-decoding during ordinary playback.
 */
internal fun shouldPlayBackgroundVideo(
    lifecycleActive: Boolean,
    primaryIsPlaying: Boolean,
    listScrolling: Boolean,
    overlayCovering: Boolean = false,
    sameFileAsPrimary: Boolean = false,
): Boolean = !listScrolling &&
    !overlayCovering &&
    !sameFileAsPrimary &&
    shouldMirrorPrimaryPlayback(lifecycleActive, primaryIsPlaying)
