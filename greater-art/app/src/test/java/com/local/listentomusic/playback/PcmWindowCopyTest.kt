package com.local.listentomusic.playback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PcmWindowCopyTest {
    private fun check(channels: Int, first: Long, frames: Int, duration: Long = 19) {
        val chunks = listOf(0L to 5, 5L to 7, 12L to 8).map { (start, count) ->
            NativePcmBlock(start, FloatArray(count * channels) { index ->
                val sourceFrame = start + index / channels
                (sourceFrame * 17 + index % channels * 3).toFloat() / 8_388_608f
            }, channels)
        }
        val expected = FloatArray(frames * 2) { index ->
            val frame = first + index / 2
            val channel = index % 2
            val block = chunks.firstOrNull { frame >= it.start && frame < it.end }
            if (frame < 0 || frame >= duration || block == null) 0f
            else block.samples[(frame - block.start).toInt() * channels + if (channels == 1) 0 else channel]
        }
        val destination = FloatArray(frames * 2 + 11) { Float.NaN }
        copyPcmStereoWindow(chunks, duration, first, frames, destination)
        assertArrayEquals(expected, destination.copyOf(frames * 2), 0f)
        for (index in frames * 2 until destination.size) assertEquals(true, destination[index].isNaN())
    }

    @Test fun stereoCrossesChunksWithExactSubPcm16Values() = check(2, 2, 15)
    @Test fun monoDuplicatesTheExactSourceChannel() = check(1, 2, 15)
    @Test fun stereoPreservesNegativeEntryAndClipTailZeros() = check(2, -3, 28)
    @Test fun monoPreservesNegativeEntryAndClipTailZeros() = check(1, -3, 28)
    @Test fun clippedBeforeWindowIsAllZero() = check(2, 21, 6)
    @Test fun whollyDelayedEntryIsAllZero() = check(1, -8, 4)
    @Test fun fractionalKernelHaloCanStraddleTwoChunkBoundaries() = check(2, 3, 14)
    @Test fun zeroLengthLeavesReusableBufferUntouched() = check(2, 9, 0)
    @Test fun absentChunksClearPreviousWindowSamples() {
        val destination = FloatArray(12) { .25f }
        copyPcmStereoWindow(emptyList(), 100, 0, 6, destination)
        assertArrayEquals(FloatArray(12), destination, 0f)
    }
    @Test fun overlappingDecoderTimestampsKeepFirstBlockOwnership() {
        val blocks = listOf(NativePcmBlock(0, FloatArray(10) { .125f }, 2),
            NativePcmBlock(4, FloatArray(8) { .25f }, 2))
        val destination = FloatArray(16)
        copyPcmStereoWindow(blocks, 8, 0, 8, destination)
        assertArrayEquals(FloatArray(16) { if (it < 10) .125f else .25f }, destination, 0f)
    }
    @Test fun missingDecoderFramesRemainZeroRatherThanRepeatingPriorSamples() {
        val blocks = listOf(NativePcmBlock(0, floatArrayOf(.125f, .125f), 1),
            NativePcmBlock(4, floatArrayOf(.25f, .25f), 1))
        val destination = FloatArray(12)
        copyPcmStereoWindow(blocks, 6, 0, 6, destination)
        assertArrayEquals(floatArrayOf(.125f,.125f,.125f,.125f,0f,0f,0f,0f,.25f,.25f,.25f,.25f), destination, 0f)
    }
    @Test fun repeatedHalosKeepActualScalarCacheHistoryAcrossOverlappingChunks() {
        val blocks = listOf(NativePcmBlock(0, FloatArray(2048) { .125f }, 2),
            NativePcmBlock(1023, FloatArray(2048) { .25f }, 2),
            NativePcmBlock(2046, FloatArray(2048) { .5f }, 2))
        var scalarCurrent: NativePcmBlock? = null
        var copiedCurrent: NativePcmBlock? = null
        for (first in listOf(-1L, 1023L, 2047L, 1023L, -100L, 3070L, 3100L)) {
            val expected = FloatArray(1057 * 2) { index ->
                val frame = first + index / 2
                if (frame < 0 || frame >= 3070) 0f else {
                    var block = scalarCurrent
                    if (block == null || frame < block.start || frame >= block.end) {
                        block = blocks.firstOrNull { frame >= it.start && frame < it.end }
                        scalarCurrent = block
                    }
                    block?.samples?.get((frame - block.start).toInt() * 2 + index % 2) ?: 0f
                }
            }
            val destination = FloatArray(expected.size)
            copiedCurrent = copyPcmStereoWindow(blocks, 3070, first, 1057, destination, copiedCurrent)
            assertArrayEquals("Window first=$first", expected, destination, 0f)
            assertSame("Cached ownership first=$first", scalarCurrent, copiedCurrent)
        }
    }
}
