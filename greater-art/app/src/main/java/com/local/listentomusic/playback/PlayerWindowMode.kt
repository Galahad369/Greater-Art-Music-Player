package com.local.listentomusic.playback

/** One overlay window, three geometries. Playback and its PlayerView outlive mode changes. */
internal enum class PlayerWindowMode { DOCKED, DETACHED, EXPANDED }

/** A full-size expanded player keeps the display awake; Mini Window never does. */
internal fun playerWindowScreenAwakeFlags(flags: Int, mode: PlayerWindowMode): Int {
    val keepAwake = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    return if (mode == PlayerWindowMode.EXPANDED) flags or keepAwake else flags and keepAwake.inv()
}
