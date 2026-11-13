package com.local.listentomusic.playback

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class StackPcmWaitTest {
    @Test fun missingDataDeadlineIsFailureNotOwnerCancellation() = runBlocking {
        val input = Channel<NativePcmBlock>(1)
        var pauses = 0
        try {
            val failure = runCatching {
                withTimeout(3000) { awaitStackPcmBlock(input) { pauses++ } }
            }.exceptionOrNull()
            assertTrue("A decoder deadline must enter failure cleanup, not silently retain its lease: $failure",
                failure is IOException)
            assertFalse(failure is CancellationException)
            assertEquals(1, pauses)
        } finally { input.cancel() }
    }

    @Test fun ownerCancellationStillCancelsAnEmptyWait() = runBlocking {
        val input = Channel<NativePcmBlock>(1)
        val receive = async(start = CoroutineStart.UNDISPATCHED) { awaitStackPcmBlock(input) {} }
        receive.cancelAndJoin()
        assertTrue(receive.isCancelled)
        input.cancel()
    }

    @Test fun outerTimeoutIsStillOwnerCancellation() = runBlocking {
        val input = Channel<NativePcmBlock>(1)
        try {
            val failure = runCatching { withTimeout(50) { awaitStackPcmBlock(input) {} } }.exceptionOrNull()
            assertTrue(failure is TimeoutCancellationException)
            assertFalse(failure is IOException)
        } finally { input.cancel() }
    }

    @Test fun pausesBeforeBlockingForMissingDecoderData() = runBlocking {
        val input = Channel<NativePcmBlock>(1)
        var pauses = 0
        val receive = async(start = CoroutineStart.UNDISPATCHED) { awaitStackPcmBlock(input) { pauses++ } }
        try {
            assertFalse(receive.isCompleted)
            // The sink holds only100ms. Waiting200ms before pausing can underrun
            // even when a producer recovers before the old timeout fires.
            assertEquals("Pause output before any missing-block wait, not after200ms", 1, pauses)
            val block = NativePcmBlock(0, floatArrayOf(.1f, -.1f), 2)
            input.send(block)
            assertSame(block, withTimeout(1000) { receive.await() }.getOrThrow())
        } finally {
            receive.cancelAndJoin()
            input.cancel()
        }
    }

    @Test fun bufferedDataNeedsNoPauseAndKeepsSamples() = runBlocking {
        val input = Channel<NativePcmBlock>(1)
        val block = NativePcmBlock(17, floatArrayOf(.1234567f, -.9876543f), 2)
        input.send(block)
        var pauses = 0
        assertSame(block, awaitStackPcmBlock(input) { pauses++ }.getOrThrow())
        assertEquals(0, pauses)
        input.cancel()
    }

    @Test fun closedAndFailedDecodersDoNotMasqueradeAsWaits() = runBlocking {
        val closed = Channel<NativePcmBlock>(1)
        closed.close()
        var pauses = 0
        assertTrue(awaitStackPcmBlock(closed) { pauses++ }.isClosed)
        val failed = Channel<NativePcmBlock>(1)
        val cause = IllegalStateException("owned decoder failure")
        failed.close(cause)
        assertSame(cause, awaitStackPcmBlock(failed) { pauses++ }.exceptionOrNull())
        assertEquals(0, pauses)
    }
}
