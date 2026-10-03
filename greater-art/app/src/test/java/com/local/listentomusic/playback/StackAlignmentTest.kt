package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class StackAlignmentTest {
    private fun pattern() = FloatArray(1500) { Random(it * 37 + 9).nextFloat() }
    @Test fun detectsPaddingAndDifferentGain() {
        val a = pattern()
        val b = FloatArray(a.size + 50) { if (it >= 50) a[it - 50] * .4f else 0f }
        val result = correlateStackEnvelopes(a, b)
        assertTrue(result.confident); assertEquals(1000L, result.offsetMs)
        val reverse = correlateStackEnvelopes(b, a)
        assertTrue(reverse.confident); assertEquals(-1000L, reverse.offsetMs)
    }
    @Test fun rejectsSilenceUnrelatedAudioAndShortSamples() {
        assertFalse(correlateStackEnvelopes(FloatArray(1500), pattern()).confident)
        assertFalse(correlateStackEnvelopes(pattern(), FloatArray(1500) { Random(it + 456).nextFloat() }).confident)
        assertFalse(correlateStackEnvelopes(FloatArray(20), FloatArray(20)).confident)
    }
    @Test fun repeatedBeatsDoNotInventUniqueOffsets() {
        val beat = FloatArray(1500) { if (it % 25 < 3) 1f else .01f }
        assertFalse(correlateStackEnvelopes(beat, beat).confident)
    }
    @Test fun promotionPreservesRelativeMusicalPositionsAndNegativeDelay() {
        assertEquals(listOf(-1000L, 0L, -1500L), rebaseStackOffsets(listOf(0L, 1000L, -500L), 1000L))
        assertEquals(-400L, stackVoiceTarget(100L, -500L))
    }
}
