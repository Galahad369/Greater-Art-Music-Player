package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackConnectionTest {
    @Test fun handoffBorrowsTheSameConnectionUntilLastPresentationCloses() {
        var created = 0
        var released = 0
        val pool = SharedPlaybackResource<Any> { released++ }
        fun open() = pool.acquire { created++; Any() }
        val library = open()
        val expanded = open()
        assertSame(library.value, expanded.value)
        library.close()
        val mini = open()
        expanded.close()
        expanded.close()
        assertEquals(1, created)
        assertEquals(0, released)
        assertSame(expanded.value, mini.value)
        mini.close()
        assertEquals(1, released)
        val nextSession = open()
        assertNotSame(mini.value, nextSession.value)
        assertEquals(2, created)
        nextSession.close()
        assertEquals(2, released)
    }

    @Test fun failedCreationDoesNotKeepAPhantomBorrower() {
        var released = 0
        val pool = SharedPlaybackResource<Any> { released++ }
        runCatching { pool.acquire { error("Connection failed") } }
        pool.acquire { Any() }.close()
        assertEquals(1, released)
    }

    @Test fun repeatedLibraryExpandedFullscreenReturnsReleaseOnlyTheLastBorrower() {
        var created = 0
        val released = mutableSetOf<Int>()
        val pool = SharedPlaybackResource<Int> { identity ->
            assertTrue("A connection must release only once", released.add(identity))
        }
        repeat(1_000) {
            val library = pool.acquire { ++created }
            val expanded = pool.acquire { ++created }
            val fullscreen = pool.acquire { ++created }
            val identity = library.value
            assertEquals(identity, expanded.value)
            assertEquals(identity, fullscreen.value)
            library.close()
            fullscreen.close()
            assertFalse("Expanded still owns the connection", identity in released)
            val returned = pool.acquire { ++created }
            assertEquals(identity, returned.value)
            expanded.close()
            expanded.close()
            assertFalse(identity in released)
            returned.close()
            assertTrue(identity in released)
        }
        assertEquals(1_000, created)
        assertEquals(1_000, released.size)
    }
}
