package com.local.listentomusic.playback

/**
 * Same-process bridge for the hold-for-2x gesture. The service owns the saved
 * pre-hold speed until release, or can commit the current 2x speed when the user
 * deliberately pulls down to lock it.
 */
object TemporaryPlaybackSpeed {
    internal var beginCommand: (() -> Boolean)? = null
    internal var endCommand: (() -> Unit)? = null
    internal var lockCommand: (() -> Boolean)? = null

    fun begin(): Boolean = beginCommand?.invoke() == true
    fun end() { endCommand?.invoke() }
    fun lock(): Boolean = lockCommand?.invoke() == true
    internal fun detach() { beginCommand = null; endCommand = null; lockCommand = null }
}
