package com.local.listentomusic.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailMemoryHitCounterTest {
    @Test fun disabledHitsDoNotChangeDiagnostics() {
        val counter = ThumbnailMemoryHitCounter()
        repeat(100) { counter.record() }
        assertEquals(0, counter.snapshot())
        counter.enabled = true
        repeat(3) { counter.record() }
        counter.enabled = false
        counter.record()
        assertEquals(3, counter.snapshot())
    }

    @Test fun concurrentHitsAreNotLost() {
        val counter = ThumbnailMemoryHitCounter().apply { enabled = true }
        val threads = List(4) { Thread { repeat(1_000) { counter.record() } } }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(4_000, counter.snapshot())
    }
}
