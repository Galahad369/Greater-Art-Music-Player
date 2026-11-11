package com.local.listentomusic.playback

import java.util.concurrent.atomic.AtomicLong

/** A Library file check can outlive the screen that issued it. Order app transport across hosts. */
internal object QueueStartOrder {
    private val revision = AtomicLong()

    fun advance(): Long = revision.incrementAndGet()
    fun isCurrent(request: Long): Boolean = request == revision.get()
}

internal fun <Entry : Any> isActiveListSelection(requested: Entry, current: Entry?, playRequested: Boolean): Boolean =
    playRequested && requested == current

internal inline fun <Entry : Any> playListSelection(
    requested: Entry,
    current: Entry?,
    playRequested: Boolean,
    play: () -> Unit,
) {
    if (isActiveListSelection(requested, current, playRequested)) return
    play()
}
