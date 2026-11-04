package com.local.listentomusic.playback

/**
 * Same-process bridge for the hold-for-2x gesture. The service owns the saved
 * pre-hold speed until release, or can commit the current 2x speed when the user
 * deliberately pulls down to lock it.
 */
object TemporaryPlaybackSpeed {
    enum class Origin { VIDEO_GESTURE, SPEED_BUTTON }

    internal var beginCommand: ((Origin) -> Boolean)? = null
    internal var endCommand: (() -> Unit)? = null
    internal var lockCommand: (() -> Boolean)? = null

    fun begin(origin: Origin): Boolean = beginCommand?.invoke(origin) == true
    fun end() { endCommand?.invoke() }
    fun lock(): Boolean = lockCommand?.invoke() == true
    internal fun detach() { beginCommand = null; endCommand = null; lockCommand = null }
}
