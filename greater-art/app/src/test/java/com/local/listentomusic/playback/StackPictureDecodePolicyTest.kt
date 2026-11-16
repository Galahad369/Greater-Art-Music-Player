package com.local.listentomusic.playback

import java.nio.ByteBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class StackPictureDecodePolicyTest {
    @Test fun largerSyncPacketsAreNotForcedIntoTheOldHalfMiBProbe() {
        assertTrue("600 KiB IDR must fit its probe", stackPictureProbeCapacity(600 * 1024L, 0) >= 600 * 1024)
        assertTrue(stackPictureProbeCapacity(-1, 2 * 1024 * 1024) >= 2 * 1024 * 1024)
    }

    @Test fun localFailureRetriesOnlyItsWindowAndNeverKeepsPartialFrames(): Unit = runBlocking {
        val windows = List(3) { StackPictureWindow(it * 10L, it * 10L + 8) }
        val attempts = mutableListOf<Pair<Int, Boolean>>()
        val result = readStackPictureWindows(windows, true) { index, _, local ->
            attempts += index to local
            if (local && index == 1) throw IncompleteLocalDecode()
            listOf(index)
        }
        assertEquals(listOf(0, 1, 2), result)
        assertEquals("Completed evidence must not be decoded again", listOf(0 to true, 1 to true, 1 to false, 2 to false), attempts)
    }

    @Test fun cancellationAndPlatformFailureAreNeverRetried() {
        for (failure in listOf(CancellationException("cancelled"), IllegalStateException("platform failed"))) {
            var attempts = 0
            assertThrows(failure.javaClass) {
                runBlocking { readStackPictureWindows<Int>(listOf(StackPictureWindow(0, 8)), false) { _, _, _ ->
                    attempts++; throw failure
                } }
            }
            assertEquals(1, attempts)
        }
    }

    @Test fun onlyQuickAvcOnRanchuX86UsesTheGuestDecoder() {
        assertTrue(stackPictureUsesLocalAvc(StackPictureSearch.QUICK, "ranchu", "x86_64", "video/avc"))
        for ((hardware, abi, mime) in listOf(Triple("exynos", "arm64-v8a", "video/avc"),
            Triple("ranchu", "arm64-v8a", "video/avc"), Triple("ranchu", "x86_64", "video/hevc")))
            assertFalse(stackPictureUsesLocalAvc(StackPictureSearch.QUICK, hardware, abi, mime))
        assertFalse(stackPictureUsesLocalAvc(StackPictureSearch.WIDE, "ranchu", "x86_64", "video/avc"))
    }

    @Test fun idrScanSupportsBothAnnexBPrefixesAndRejectsNonIdrOrTruncation() {
        for (bytes in listOf(byteArrayOf(0,0,1,0x65), byteArrayOf(0,0,0,1,0x65),
            byteArrayOf(0,0,1,0x67,6,0,0,0,1,0x65)))
            assertTrue(stackPictureHasAvcIdr(ByteBuffer.wrap(bytes), bytes.size))
        for (bytes in listOf(byteArrayOf(0,0,1), byteArrayOf(0,0,1,0x41), byteArrayOf(0,0,0,4,0x65)))
            assertFalse(stackPictureHasAvcIdr(ByteBuffer.wrap(bytes), bytes.size))
    }

    @Test fun completenessRetainsMultiplicityAndPresentationOrder() {
        assertTrue(stackPictureOutputComplete(listOf(20,0,10), listOf(0,10,20)))
        assertTrue(stackPictureOutputComplete(listOf(0,10,10), listOf(0,10,10)))
        assertFalse(stackPictureOutputComplete(listOf(0,10,20), listOf(0,20)))
        assertFalse(stackPictureOutputComplete(listOf(0,10), listOf(10,0)))
        assertFalse(stackPictureOutputComplete(listOf(0,10,10), listOf(0,10)))
    }

    @Test fun invalidOrHostileSizeHintsCannotCauseAnUnboundedAllocation() {
        assertEquals(512 * 1024, stackPictureProbeCapacity(-1, -1))
        assertEquals(STACK_PICTURE_MAX_PROBE_BYTES, stackPictureProbeCapacity(-1, Int.MAX_VALUE))
        assertThrows(IncompleteLocalDecode::class.java) { stackPictureProbeCapacity(Long.MAX_VALUE, 0) }
    }

    @Test fun budgetWaitAndDecodeTimeoutsAreDifferentFailures(): Unit = runBlocking {
        val waiting = boundedStackPictureRead<Int>(20) { awaitCancellation() }
        assertEquals(StackPictureFailure.WAIT_TIME_LIMIT, waiting.second)
        val decoding = boundedStackPictureRead<Int>(20) { acquired -> acquired(); awaitCancellation() }
        assertEquals(StackPictureFailure.DECODE_TIME_LIMIT, decoding.second)
        val success = boundedStackPictureRead(1000) { acquired -> acquired(); 42 }
        assertEquals(42, success.first)
        assertEquals(null, success.second)
    }
}
