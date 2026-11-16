package com.local.listentomusic.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackAlignmentLifecycleTest {
    @Test fun cancelledAnalysisOwnsItsStateUntilDecoderCleanupCompletes() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cleaningUp = CompletableDeferred<Unit>()
        val releaseCleanup = CompletableDeferred<Unit>()
        val analysis = launch(Dispatchers.Default) {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    cleaningUp.complete(Unit)
                    releaseCleanup.await()
                }
            }
        }
        try {
            withTimeout(5000) { started.await() }
            assertTrue(stackAlignmentRequestBusy(analysis))
            analysis.cancel()
            withTimeout(5000) { cleaningUp.await() }
            assertFalse(analysis.isActive)
            assertFalse(analysis.isCompleted)
            assertTrue("A new analysis must wait for cancelled decoder cleanup",
                stackAlignmentRequestBusy(analysis))
        } finally {
            releaseCleanup.complete(Unit)
            withTimeout(5000) { analysis.cancelAndJoin() }
        }
        assertFalse(stackAlignmentRequestBusy(analysis))
        assertFalse(stackAlignmentRequestBusy(null))
    }
}
