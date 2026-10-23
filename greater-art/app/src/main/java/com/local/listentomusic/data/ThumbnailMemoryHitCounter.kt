package com.local.listentomusic.data

import java.util.concurrent.atomic.AtomicInteger

/** Hot RAM hits never publish snapshot/Flow state. Counts cover diagnostics-enabled periods. */
internal class ThumbnailMemoryHitCounter {
    @Volatile var enabled: Boolean = false
    private val hits = AtomicInteger()

    fun record() {
        if (enabled) hits.incrementAndGet()
    }

    fun snapshot(): Int = hits.get()
}
