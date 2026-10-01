package com.local.listentomusic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueSnapshotTest {
    @Test fun unscannedEntriesStayInSessionOrder() {
        val ids = listOf("b", "a", "c")
        assertEquals(listOf("session-b", "scanned-a", "session-c"),
            reconcileSessionQueue(ids, mapOf("a" to "scanned-a")) { "session-${ids[it]}" })
    }
    @Test fun coldScannerDoesNotEmptyQueue() {
        val ids = listOf("one", "two")
        assertEquals(ids, reconcileSessionQueue(ids, emptyMap()) { ids[it] })
    }
    @Test fun duplicateSessionEntriesArePreserved() {
        val ids = listOf("a", "a")
        assertEquals(listOf("known", "known"), reconcileSessionQueue(ids, mapOf("a" to "known")) { "fallback" })
    }

    @Test fun duplicateQueueRowsReceiveDistinctStableKeysAndExactIndices() {
        val a = MediaFile("a", "a", 0, 1, 1, MediaKind.AUDIO)
        val b = MediaFile("b", "b", 0, 1, 1, MediaKind.AUDIO)
        val entries = queueEntries(listOf(a, b, a))
        assertEquals(listOf(0, 1, 2), entries.map(QueueEntry::index))
        assertEquals(3, entries.map(QueueEntry::stableKey).toSet().size)
        assertEquals(entries[0].stableKey, queueEntries(listOf(a, b, a))[0].stableKey)
        assertEquals(entries[2].stableKey, queueEntries(listOf(a, b, a))[2].stableKey)
    }
}
