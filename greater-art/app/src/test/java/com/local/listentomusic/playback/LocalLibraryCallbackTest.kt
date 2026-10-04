package com.local.listentomusic.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryCallbackTest {
    @Test
    fun scanCacheReusesOnlySameExclusionsInsideTtl() {
        val excluded = setOf("private", "tmp")

        assertTrue(
            shouldReuseLibraryScanCache(
                cachedExcluded = excluded,
                requestedExcluded = excluded,
                cachedAtMs = 1_000L,
                nowMs = 1_000L + LIBRARY_SCAN_CACHE_TTL_MS - 1L,
            ),
        )
        assertFalse(
            shouldReuseLibraryScanCache(
                cachedExcluded = excluded,
                requestedExcluded = setOf("private"),
                cachedAtMs = 1_000L,
                nowMs = 2_000L,
            ),
        )
        assertFalse(
            shouldReuseLibraryScanCache(
                cachedExcluded = excluded,
                requestedExcluded = excluded,
                cachedAtMs = 1_000L,
                nowMs = 1_000L + LIBRARY_SCAN_CACHE_TTL_MS,
            ),
        )
        assertFalse(
            shouldReuseLibraryScanCache(
                cachedExcluded = excluded,
                requestedExcluded = excluded,
                cachedAtMs = 2_000L,
                nowMs = 1_999L,
            ),
        )
    }
}
