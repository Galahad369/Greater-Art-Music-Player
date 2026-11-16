package com.local.listentomusic.playback

import android.view.WindowManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

/** One overlay window, three geometries. Playback and its PlayerView outlive mode changes. */
internal enum class PlayerWindowMode { DOCKED, DETACHED, EXPANDED }

/** A full-size expanded player keeps the display awake; Mini Window never does. */
internal fun playerWindowScreenAwakeFlags(flags: Int, mode: PlayerWindowMode): Int {
    val keepAwake = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    return if (mode == PlayerWindowMode.EXPANDED) flags or keepAwake else flags and keepAwake.inv()
}

/** Alpha alone does not release a window's input focus or background dim. */
internal fun playerWindowInputFlags(flags: Int, expanded: Boolean, visible: Boolean): Int {
    val touchFlags = if (visible) flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        else flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    return if (expanded && visible)
        (touchFlags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) or WindowManager.LayoutParams.FLAG_DIM_BEHIND
    else (touchFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
}

internal fun playerWindowDimAmount(expanded: Boolean, visible: Boolean): Float =
    if (expanded && visible) .14f else 0f

/** Visual coverage is independent of input ownership during a fullscreen transfer. */
internal fun playerWindowVisualAlpha(visible: Boolean, expanded: Boolean,
    fullscreenActivity: Boolean, fullscreenFrameReady: Boolean): Float =
    if (visible || (expanded && fullscreenActivity && !fullscreenFrameReady)) 1f else 0f

/** A readiness reset is not permission to show the old window before return selects its mode. */
internal fun Flow<Boolean>.fullscreenFrameCommits(): Flow<Boolean> = filter { it }
