package com.local.listentomusic.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailViewportPacingTest {
    @Test fun latestViewportAndIdleSurviveTheMotionBurstWhileBudgetEdgesStayUpstream() = runBlocking {
        val upstream = MutableSharedFlow<Triple<Boolean, Int, Int>>()
        val first = CompletableDeferred<Unit>()
        val idleBudget = CompletableDeferred<Unit>()
        val idleDelivery = CompletableDeferred<Unit>()
        val delivered = mutableListOf<Triple<Boolean, Int, Int>>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            upstream.onEach { if (!it.first) idleBudget.complete(Unit) }
                .pacedThumbnailViewports().collect {
                    delivered += it
                    first.complete(Unit)
                    if (!it.first) idleDelivery.complete(Unit)
                }
        }
        try {
            withTimeout(2_000) { upstream.subscriptionCount.first { it > 0 } }
            upstream.emit(Triple(true, 0, 8))
            withTimeout(2_000) { first.await() }
            repeat(100) { upstream.emit(Triple(true, it + 1, it + 9)) }
            val stopped = Triple(false, 100, 108)
            upstream.emit(stopped)
            withTimeout(2_000) { idleBudget.await(); idleDelivery.await() }
            assertEquals(Triple(true, 0, 8), delivered.first())
            assertEquals(stopped, delivered.last())
            assertTrue("Motion updates coalesce rather than queuing every obsolete range", delivered.size < 102)
        } finally { collector.cancelAndJoin() }
    }
}
