package com.local.listentomusic.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger

class PcmRenderPipelineTest {
    @Test fun queuedAudioBudgetCanAwaitAPreparedBlockWithoutPausing() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 7) { frame, frames, samples ->
            release.await(); PreparedPcmBlock(frame, frames, samples)
        }
        var pauses = 0
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { pipeline.take(1000) { pauses++ } }
        try {
            assertFalse(waiting.isCompleted)
            assertEquals(0, pauses)
            release.complete(Unit)
            pipeline.recycle(withTimeout(2000) { requireNotNull(waiting.await()) })
            assertEquals(0, pauses)
        } finally { waiting.cancelAndJoin(); pipeline.close() }
    }

    @Test fun expiredHeadroomBudgetPausesBeforeTheRemainingDataWait() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val paused = CompletableDeferred<Unit>()
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 7) { frame, frames, samples ->
            release.await(); PreparedPcmBlock(frame, frames, samples)
        }
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { pipeline.take(20) { paused.complete(Unit) } }
        try {
            assertFalse(paused.isCompleted)
            withTimeout(1000) { paused.await() }
            assertFalse(waiting.isCompleted)
            release.complete(Unit)
            pipeline.recycle(withTimeout(2000) { requireNotNull(waiting.await()) })
        } finally { waiting.cancelAndJoin(); pipeline.close() }
    }

    @Test fun headroomWaitKeepsAtLeastHalfTheExistingOutputBuffer() {
        assertEquals(0L, pcmReadyWaitMs(-10, 192000, 19200))
        assertEquals(0L, pcmReadyWaitMs(9600, 192000, 19200))
        assertEquals(20L, pcmReadyWaitMs(13440, 192000, 19200))
        assertEquals(50L, pcmReadyWaitMs(19200, 192000, 19200))
        assertEquals(50L, pcmReadyWaitMs(38400, 192000, 19200))
    }

    @Test fun bufferedBlockPrecedesTheOriginalProducerFailure() = runBlocking {
        val failure = IllegalStateException("failure after one valid block")
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 2L * PCM_BLOCK_FRAMES) { frame, frames, samples ->
            if (frame > 0) throw failure
            samples[0] = .0000001f
            PreparedPcmBlock(frame, frames, samples)
        }
        try {
            val block = withTimeout(2000) { requireNotNull(pipeline.take {}) }
            assertEquals(0L, block.frame)
            assertEquals(.0000001f, block.samples[0], 0f)
            pipeline.recycle(block)
            val observed = runCatching { withTimeout(2000) { pipeline.take {} } }.exceptionOrNull()
            assertTrue(observed === failure || observed?.cause === failure)
        } finally { pipeline.close() }
    }

    @Test fun fifoAndFinalPartialKeepEverySampleBitAndReuseOnlyThreeArrays() = runBlocking {
        val identities = Collections.newSetFromMap(IdentityHashMap<FloatArray, Boolean>())
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 17, 17 + 10L * PCM_BLOCK_FRAMES + 23) { frame, frames, samples ->
            for (index in 0 until frames * 2) samples[index] = Float.fromBits(((frame + index) % 8388608).toInt() + 0x33000000)
            PreparedPcmBlock(frame, frames, samples)
        }
        try {
            var expected = 17L
            withTimeout(3000) {
                while (true) {
                    val block = pipeline.take {} ?: break
                    assertEquals(expected, block.frame)
                    assertEquals(minOf(PCM_BLOCK_FRAMES.toLong(), 17 + 10L * PCM_BLOCK_FRAMES + 23 - expected).toInt(), block.frames)
                    identities.add(block.samples)
                    for (index in 0 until block.frames * 2) assertEquals(((block.frame + index) % 8388608).toInt() + 0x33000000, block.samples[index].toRawBits())
                    expected += block.frames
                    pipeline.recycle(block)
                }
            }
            assertEquals(17 + 10L * PCM_BLOCK_FRAMES + 23, expected)
            assertEquals(3, identities.size)
        } finally { pipeline.close() }
    }

    @Test fun heldArrayIsNotOverwrittenAndLevelSnapshotHasABoundedFifoBoundary() = runBlocking {
        val count = AtomicInteger()
        val level = AtomicInteger(1)
        val third = CompletableDeferred<Unit>()
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 6L * PCM_BLOCK_FRAMES) { frame, frames, samples ->
            samples.fill(level.get().toFloat())
            if (count.incrementAndGet() == 3) third.complete(Unit)
            PreparedPcmBlock(frame, frames, samples)
        }
        try {
            val held = withTimeout(2000) { requireNotNull(pipeline.take {}) }
            val original = held.samples.copyOf()
            withTimeout(2000) { third.await() }
            assertEquals(3, count.get()) // consumer1 + ready1 + producer1
            level.set(2)
            assertArrayEquals(original, held.samples, 0f)
            pipeline.recycle(held)
            for (index in 1..5) {
                val block = withTimeout(2000) { requireNotNull(pipeline.take {}) }
                assertEquals(if (index < 3) 1f else 2f, block.samples[0], 0f)
                pipeline.recycle(block)
            }
            assertNull(pipeline.take {})
        } finally { pipeline.close() }
    }

    @Test fun emptyReadyQueueProtectsOutputBeforeSuspending() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 7) { frame, frames, samples ->
            release.await(); PreparedPcmBlock(frame, frames, samples)
        }
        var pauses = 0
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { pipeline.take { pauses++ } }
        try {
            assertFalse(waiting.isCompleted)
            assertEquals(1, pauses)
            release.complete(Unit)
            val block = withTimeout(2000) { requireNotNull(waiting.await()) }
            pipeline.recycle(block)
            assertNull(pipeline.take {})
            assertEquals(1, pauses)
        } finally { waiting.cancelAndJoin(); pipeline.close() }
    }

    @Test fun failureCloseIsNotSuccessfulEof() = runBlocking {
        val failure = IllegalStateException("owned mix failure")
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 7) { _, _, _ -> throw failure }
        try {
            val observed = runCatching { withTimeout(2000) { pipeline.take {} } }.exceptionOrNull()
            // Coroutine stack recovery can copy the exception and retain the
            // original as its cause; it must never become successful EOF.
            assertTrue("Original failure missing: $observed", observed === failure || observed?.cause === failure)
        }
        finally { pipeline.close() }
    }

    @Test fun closeJoinsBlockedPreparationBeforeReturning() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        var cleaned = false
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 7) { _, _, _ ->
            try { entered.complete(Unit); awaitCancellation() } finally { cleaned = true }
        }
        withTimeout(2000) { entered.await(); pipeline.close() }
        assertTrue(cleaned)
    }

    @Test fun fullReadyQueueStopsPreparationAndCanBeCancelledWithoutConsumer() = runBlocking {
        val count = AtomicInteger()
        val second = CompletableDeferred<Unit>()
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 20L * PCM_BLOCK_FRAMES) { frame, frames, samples ->
            if (count.incrementAndGet() == 2) second.complete(Unit)
            PreparedPcmBlock(frame, frames, samples)
        }
        withTimeout(2000) { second.await() }
        assertEquals(2, count.get()) // ready1 + producer1; send suspends
        withTimeout(2000) { pipeline.close() }
        assertEquals(2, count.get())
    }

    @Test fun cancelledEmptyConsumerDoesNotLeaveItsProducerRunning() = runBlocking {
        var cleaned = false
        val entered = CompletableDeferred<Unit>()
        val pipeline = PcmRenderPipeline(this, Dispatchers.Default, 0, 7) { _, _, _ ->
            try { entered.complete(Unit); awaitCancellation() } finally { cleaned = true }
        }
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { pipeline.take {} }
        withTimeout(2000) { entered.await(); waiting.cancelAndJoin(); pipeline.close() }
        assertTrue(waiting.isCancelled)
        assertTrue(cleaned)
    }

    @Test fun slowAndLowerRatePlaybackKeepTheSerialControlLatency() {
        assertFalse(shouldPipelinePcm(8000, .25))
        assertFalse(shouldPipelinePcm(48000, 2.0))
        assertFalse(shouldPipelinePcm(192000, .25))
        assertTrue(shouldPipelinePcm(96000, 1.0))
        assertTrue(shouldPipelinePcm(192000, .5))
        assertTrue(shouldPipelinePcm(192000, 1.25))
        for (rate in listOf(96000, 192000)) for (speed in listOf(.25, .5, 1.0, 1.25, 2.0)) {
            if (shouldPipelinePcm(rate, speed)) assertTrue(PCM_BLOCK_FRAMES * 2.0 / rate / speed <= .025)
        }
    }
}
