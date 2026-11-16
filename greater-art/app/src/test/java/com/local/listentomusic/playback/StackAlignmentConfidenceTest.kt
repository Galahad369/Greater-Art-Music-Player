package com.local.listentomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StackAlignmentConfidenceTest {
    private val times = listOf(3000.0, 39000.0, 75000.0, 111000.0, 147000.0, 177000.0)
    private fun anchor(time: Double, offset: Double) = StackInstrumentAnchor(time, time + offset, .9, 0)

    @Test fun completeSoundEvidenceRetainsKnownOffset() {
        val result = stackFitInstrumentCandidates(times.map { listOf(anchor(it, 700.0)) }, 180000)
        assertTrue(result.confident)
        assertEquals(700000.0, result.offsetUs, .01)
    }

    @Test fun missingHalfTheSoundWindowsDoesNotBecomeConfident() {
        val groups = times.mapIndexed { index, time ->
            if (index % 2 == 0) listOf(anchor(time, 700.0)) else emptyList()
        }
        assertFalse("Missing evidence must not inflate whole-track support", stackFitInstrumentCandidates(groups, 180000).confident)
    }

    @Test fun equallyStrongQuarterSecondSoundAlternativesAbstain() {
        val groups = times.map { listOf(anchor(it, 700.0), anchor(it, 950.0)) }
        assertFalse("Two equally plausible timing maps must not be applied", stackFitInstrumentCandidates(groups, 180000).confident)
    }

    private fun pictures(offsetUs: Long = 0) = List(432) { index ->
        val random = Random(1009 + index / 12)
        requireNotNull(stackPictureFrame(index * 1000000L / 24 + offsetUs,
            FloatArray(STACK_PICTURE_GRID * STACK_PICTURE_GRID) { 25f + random.nextInt(176) }))
    }

    @Test fun consistentPictureEvidenceRetainsKnownOffset() {
        val result = requireNotNull(estimateStackPictureOffset(pictures(), pictures(250000), StackPictureSearch.QUICK))
        assertEquals(250000.0, result.offsetUs, .01)
    }

    @Test fun distributedPictureEditsDoNotBecomeAConfidentGlobalOffset() {
        val primary = pictures()
        val edited = primary.mapIndexed { index, frame ->
            if (index % 144 < 72) frame
            else primary[(index + 6).coerceAtMost(primary.lastIndex)].copy(timeUs = frame.timeUs)
        }
        for (mode in StackPictureSearch.entries) {
            assertNull("A constant offset cannot correct distributed timeline edits ($mode)",
                estimateStackPictureOffset(primary, edited, mode))
        }
    }
}
