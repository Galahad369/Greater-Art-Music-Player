package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class StackAlignmentTest {
    @Test fun cancellationCheckedDuringCorrelation() {
        var checks = 0
        val features = StackAudioFeatures(pattern(), Array(1500) { FloatArray(12) })
        try {
            correlateStackFeatures(features, features) { if (++checks == 3) throw java.util.concurrent.CancellationException() }
            fail("Cancelled analysis must stop")
        } catch (_: java.util.concurrent.CancellationException) {
            assertEquals(3, checks)
        }
    }
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
    @Test fun harmonicFingerprintCanRecoverOffsetWhenEnvelopeShapeChanges() {
        val frames = 900
        val lag = 35
        val primaryEnvelope = FloatArray(frames) { .45f + (it % 17) * .01f }
        val companionEnvelope = FloatArray(frames + lag) { index ->
            if (index < lag) .05f else .15f + ((index * 11) % 23) * .01f
        }
        var state = 17
        val pitches = IntArray(frames) {
            state = state * 1103515245 + 12345
            (state ushr 16) % 12
        }
        val primaryChroma = Array(frames) { index ->
            FloatArray(12).also { it[pitches[index]] = 1f }
        }
        val companionChroma = Array(frames + lag) { index ->
            FloatArray(12).also {
                if (index >= lag) it[pitches[index - lag]] = 1f
            }
        }
        val result = correlateStackFeatures(
            StackAudioFeatures(primaryEnvelope, primaryChroma),
            StackAudioFeatures(companionEnvelope, companionChroma),
        )
        assertTrue(result.confident)
        assertEquals(lag * 20L, result.offsetMs)
    }

    @Test fun unrelatedHarmonicPatternStillFailsConfidenceGate() {
        val frames = 900
        val envelope = FloatArray(frames) { .5f }
        val a = Array(frames) { index -> FloatArray(12).also { it[(index / 7) % 12] = 1f } }
        val b = Array(frames) { index -> FloatArray(12).also { it[(index * 5 + 3) % 12] = 1f } }
        assertFalse(correlateStackFeatures(
            StackAudioFeatures(envelope, a),
            StackAudioFeatures(envelope, b),
        ).confident)
    }

    @Test fun promotionPreservesRelativeMusicalPositionsAndNegativeDelay() {
        assertEquals(listOf(-1000L, 0L, -1500L), rebaseStackOffsets(listOf(0L, 1000L, -500L), 1000L))
        assertEquals(-400L, stackVoiceTarget(100L, -500L))
    }
}
