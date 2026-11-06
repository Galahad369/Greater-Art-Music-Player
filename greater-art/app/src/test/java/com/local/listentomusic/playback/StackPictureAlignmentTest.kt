package com.local.listentomusic.playback

import com.local.listentomusic.ui.parseStackOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StackPictureAlignmentTest {
    private fun pictures(
        seed: Int, offsetUs: Long = 0, brightness: Float = 0f, count: Int = 288, framesPerScene: Int = 4,
    ) = List(count) { index ->
        // Scene content evolves every four 24 fps frames. Use the same visual
        // timeline in the copy, but change the brightness and timestamps.
        val pattern = Random(seed * 1_009 + index / framesPerScene)
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

    @Test fun aQuarterSecondEditInTheLastSectionInvalidatesAGlobalOffset() {
        val primary = pictures(1, count = 432, framesPerScene = 12)
        for (shift in listOf(-6, 6)) {
            val edited = primary.mapIndexed { index, frame ->
                if (index < 288) frame
                else primary[(index + shift).coerceAtMost(primary.lastIndex)].copy(timeUs = frame.timeUs)
            }
            assertNull(estimateStackPictureOffset(primary, edited, StackPictureSearch.QUICK))
            assertNull(estimateStackPictureOffset(primary, edited, StackPictureSearch.WIDE))
        }
    }

    @Test fun quickSearchOnlyMatchesNearOffsetsAndWideCanFindEitherSign() {
        val primary = pictures(1)
        for (offset in listOf(-2_300_000L, 2_300_000L, -27_000_000L, 27_000_000L)) {
            val companion = pictures(1, offset)
            assertNull(estimateStackPictureOffset(primary, companion, StackPictureSearch.QUICK))
            val match = requireNotNull(estimateStackPictureOffset(primary, companion, StackPictureSearch.WIDE))
            assertEquals(offset.toDouble(), match.offsetUs, 0.0)
        }
        for (offset in listOf(-2_000_000L, 2_000_000L)) {
            val match = requireNotNull(estimateStackPictureOffset(primary, pictures(1, offset), StackPictureSearch.QUICK))
            assertEquals(offset.toDouble(), match.offsetUs, 0.0)
        }
    }

    @Test fun quickSamplesThreeSeparatedSectionsAndClipsCompanionWindows() {
        val quick = StackPictureSearch.QUICK
        val windows = stackPictureWindows(300_000_000L, quick)
        assertEquals(listOf(
            StackPictureWindow(31_000_000L, 39_000_000L),
            StackPictureWindow(101_000_000L, 109_000_000L),
            StackPictureWindow(171_000_000L, 179_000_000L),
        ), windows)
        assertEquals(listOf(
            StackPictureWindow(29_000_000L, 41_000_000L),
            StackPictureWindow(99_000_000L, 106_500_000L),
        ), stackPictureCompanionWindows(windows, 106_500_000L, quick))
        val near = stackPictureWindows(24_600_000L, quick)
        assertEquals(listOf(StackPictureWindow(0, 24_600_000L)),
            stackPictureCompanionWindows(near, 24_600_000L, quick))
        assertEquals(listOf(StackPictureWindow(0, 12_000_000L)), stackPictureWindows(12_000_000L, quick))
        assertTrue(stackPictureWindows(0, quick).isEmpty())
        assertEquals(listOf(StackPictureWindow(0, 210_000_000L)), stackPictureWindows(300_000_000L, StackPictureSearch.WIDE))
    }

    @Test fun widerRetryIsOnlyOfferedAfterIncompleteQuickPictureResults() {
        val quick = StackAlignmentProgress(method = StackAlignmentMethod.PICTURE, total = 2)
        assertFalse(quick.canRetryPictureWider)
        assertFalse(quick.copy(running = true, matched = 1).canRetryPictureWider)
        assertFalse(quick.copy(matched = 2).canRetryPictureWider)
        assertTrue(quick.copy(matched = 0).canRetryPictureWider)
        assertTrue(quick.copy(matched = 1, quickPictureTimedOut = true).canRetryPictureWider)
        assertFalse(quick.copy(matched = 1, widePictureSearch = true).canRetryPictureWider)
        assertFalse(quick.copy(matched = 1, method = StackAlignmentMethod.SOUND).canRetryPictureWider)
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
