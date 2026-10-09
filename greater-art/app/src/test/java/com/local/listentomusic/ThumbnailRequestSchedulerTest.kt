package com.local.listentomusic

import com.local.listentomusic.data.ThumbnailRequestScheduler
import com.local.listentomusic.data.thumbnailPlaybackBudgetKb
import com.local.listentomusic.data.thumbnailSampleSize
import com.local.listentomusic.data.withThumbnailWriterLocks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class ThumbnailRequestSchedulerTest {
    private suspend fun until(condition: () -> Boolean) = withTimeout(4_000) {
        while (!condition()) delay(1)
    }

    @Test fun cacheMissesWaitForIdle() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            queue.setPaused(true)
            val calls = AtomicInteger()
            val result = async { queue.load("cover") { calls.incrementAndGet(); "bitmap" } }
            until { queue.snapshot().queued == 1 }
            assertEquals(0, calls.get())
            queue.setPaused(false)
            assertEquals("bitmap", withTimeout(4_000) { result.await() })
        } finally { scope.cancel() }
    }

    @Test fun visibleRequestsBeatEarlierOffscreenAndPrefetchWork() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            queue.setPaused(true)
            val order = Collections.synchronizedList(mutableListOf<String>())
            val prefetch = async { queue.load("prefetch", true) { order.add("prefetch"); "p" } }
            val offscreen = async { queue.load("offscreen") { order.add("offscreen"); "o" } }
            val visible = async { queue.load("visible") { order.add("visible"); "v" } }
            until { queue.snapshot().queued == 3 }
            queue.setViewport("library", setOf("visible"))
            queue.setPaused(false)
            withTimeout(4_000) { prefetch.await(); offscreen.await(); visible.await() }
            assertEquals(listOf("visible", "offscreen", "prefetch"), order.toList())
        } finally { scope.cancel() }
    }

    @Test fun cancellingOneConsumerDoesNotCancelSharedDecode() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            val gate = CompletableDeferred<Unit>()
            val calls = AtomicInteger()
            val first = async { queue.load("same") { calls.incrementAndGet(); gate.await(); "one" } }
            until { calls.get() == 1 }
            val second = async { queue.load("same") { error("Must share the existing operation") } }
            until { queue.snapshot().shared == 1 }
            first.cancelAndJoin()
            gate.complete(Unit)
            assertEquals("one", withTimeout(4_000) { second.await() })
            assertEquals(1, calls.get())
            assertEquals(0, queue.snapshot().cancelled)
        } finally { scope.cancel() }
    }

    @Test fun obsoleteQueuedConsumerNeverStarts() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            queue.setPaused(true)
            val calls = AtomicInteger()
            val row = async { queue.load("gone") { calls.incrementAndGet(); "b" } }
            until { queue.snapshot().queued == 1 }
            row.cancelAndJoin()
            queue.setPaused(false)
            assertEquals(0, queue.snapshot().queued)
            assertEquals(1, queue.snapshot().cancelled)
            assertEquals(0, calls.get())
        } finally { scope.cancel() }
    }

    @Test fun flingCancelsThenRestartsStillWantedOperation() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            val entered = CompletableDeferred<Unit>()
            val calls = AtomicInteger()
            val row = async { queue.load("wanted") {
                if (calls.incrementAndGet() == 1) { entered.complete(Unit); awaitCancellation() }
                "ready"
            } }
            withTimeout(4_000) { entered.await() }
            queue.setPaused(true)
            until { queue.snapshot().running == 0 }
            assertFalse(row.isCompleted)
            assertEquals(1, queue.snapshot().queued)
            queue.setPaused(false)
            assertEquals("ready", withTimeout(4_000) { row.await() })
            assertEquals(2, calls.get())
        } finally { scope.cancel() }
    }

    @Test fun decoderPermitIsHeldUntilCancelledNativeWorkReallyExits() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            val entered = CompletableDeferred<Unit>()
            val cleanup = CompletableDeferred<Unit>()
            val calls = AtomicInteger()
            val row = async { queue.load("wanted") {
                if (calls.incrementAndGet() == 1) {
                    entered.complete(Unit)
                    try { awaitCancellation() }
                    finally { withContext(NonCancellable) { cleanup.await() } }
                }
                "ready"
            } }
            withTimeout(4_000) { entered.await() }
            queue.setPaused(true)
            queue.setPaused(false)
            assertEquals(1, queue.snapshot().running)
            assertEquals(1, calls.get())
            cleanup.complete(Unit)
            assertEquals("ready", withTimeout(4_000) { row.await() })
        } finally { scope.cancel() }
    }

    @Test fun pendingQueueIsBoundedButDuplicatesCanStillJoin() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1, capacity = 2)
            queue.setPaused(true)
            val a = async { queue.load("a") { "a" } }
            val b = async { queue.load("b") { "b" } }
            until { queue.snapshot().queued == 2 }
            assertNull(queue.load("overflow") { error("Over capacity") })
            val shared = async { queue.load("a") { error("Duplicate") } }
            until { queue.snapshot().shared == 1 }
            assertEquals(2, queue.snapshot().peakPending)
            assertEquals(1, queue.snapshot().rejected)
            queue.setPaused(false)
            withTimeout(4_000) { assertEquals("a", a.await()); b.await(); assertEquals("a", shared.await()) }
        } finally { scope.cancel() }
    }

    @Test fun clearingReleasesWaitingConsumersWithoutRefillingQueue() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = ThumbnailRequestScheduler<String, String>(scope, 1)
            val entered = CompletableDeferred<Unit>()
            val first = async { queue.load("active") { entered.complete(Unit); awaitCancellation() } }
            withTimeout(4_000) { entered.await() }
            val second = async { queue.load("queued") { error("Cleared queued work ran") } }
            until { queue.snapshot().queued == 1 }
            queue.clear()
            withTimeout(4_000) { assertNull(first.await()); assertNull(second.await()) }
            until { queue.snapshot().running == 0 }
            assertEquals(0, queue.snapshot().queued)
            assertEquals("new", queue.load("active") { "new" })
        } finally { scope.cancel() }
    }

    @Test fun playbackPressureReducesOnlyArtworkBudget() {
        val heap = 384L * 1024 * 1024
        assertEquals(32_768, thumbnailPlaybackBudgetKb(heap, false, false, 0))
        assertEquals(16_384, thumbnailPlaybackBudgetKb(heap, false, true, 0))
        assertEquals(8_192, thumbnailPlaybackBudgetKb(heap, false, true, 6))
        assertEquals(16_384, thumbnailPlaybackBudgetKb(heap, true, false, 0))
        assertTrue(thumbnailPlaybackBudgetKb(8L * 1024 * 1024, true, true, 8) <= 1_024)
    }

    @Test fun diskDecodeSamplingIsTargetBoundedAndOverflowSafe() {
        assertEquals(1, thumbnailSampleSize(240, 135, 256))
        assertEquals(16, thumbnailSampleSize(8_000, 8_000, 256))
        assertEquals(1, thumbnailSampleSize(0, -1, 256))
        assertTrue(thumbnailSampleSize(Int.MAX_VALUE, Int.MAX_VALUE, 256) > 1)
    }

    @Test fun cancelledClearReleasesAcquiredWritersWithoutUnlockingAnotherWriter() = runBlocking {
        val first = Mutex()
        val second = Mutex(locked = true)
        val clear = async {
            withThumbnailWriterLocks(arrayOf(first, second)) { error("Other writer is still active") }
        }
        until { first.isLocked }
        clear.cancelAndJoin()
        assertFalse(first.isLocked)
        assertTrue(second.isLocked)
        second.unlock()
        assertEquals("cleared", withThumbnailWriterLocks(arrayOf(first, second)) { "cleared" })
        assertFalse(first.isLocked)
        assertFalse(second.isLocked)
    }
}
