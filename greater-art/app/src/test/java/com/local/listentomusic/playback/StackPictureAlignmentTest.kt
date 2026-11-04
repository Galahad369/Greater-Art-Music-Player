package com.local.listentomusic.playback

import com.local.listentomusic.ui.parseStackOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class StackPictureAlignmentTest {
    private fun pictures(seed: Int, offsetUs: Long = 0, brightness: Float = 0f) = List(288) { index ->
        // Scene content evolves every four 24 fps frames. Use the same visual
        // timeline in the copy, but change the brightness and timestamps.
        val pattern = Random(seed * 1_009 + index / 4)
        val luma = FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID) {
            25f + pattern.nextInt(176) + brightness
        }
        requireNotNull(stackPictureFrame(index * 1_000_000L / 24 + offsetUs, luma))
    }

    @Test fun matchesPositiveNegativeAndZeroVisualShifts() {
        val primary = pictures(1)
        for (offset in listOf(-721_500L, 0L, 2_300_000L)) {
            val match = requireNotNull(estimateStackPictureOffset(primary, pictures(1, offset, 15f)))
            assertEquals(offset.toDouble(), match.offsetUs, 0.0)
            assertEquals(1.0, match.timeScale, 0.0)
        }
    }

    @Test fun anIsolatedMatchingSceneDoesNotMoveTheEntireTrack() {
        val primary = pictures(1)
        val different = pictures(2)
        val oneRegion = primary.take(96) + different.drop(96)
        assertNull(estimateStackPictureOffset(primary, oneRegion))
        assertNull(estimateStackPictureOffset(primary, different))
    }

    @Test fun twoEquallyGoodOffsetsAreNotApplied() {
        val primary = pictures(1)
        assertNull(estimateStackPictureOffset(primary, pictures(1, -15_000_000L) + pictures(1, 15_000_000L)))
    }

    @Test fun flatPicturesAndInvalidManualEntriesAreRejected() {
        assertNull(stackPictureFrame(0L, FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID) { 128f }))
        assertNull(parseStackOffset("NaN"))
        assertNull(parseStackOffset("Infinity"))
        assertNull(parseStackOffset("30000.001"))
        assertEquals(-31.25, requireNotNull(parseStackOffset("−31,25")), 0.0)
        assertEquals(30_000.0, requireNotNull(parseStackOffset("+30000")), 0.0)
    }
}
