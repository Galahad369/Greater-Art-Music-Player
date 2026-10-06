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
        // Sub-frame peak refinement may move the estimate inside the winning 20 ms frame.
        assertEquals((lag * 20L).toDouble(), result.offsetMs.toDouble(), 10.0)
    }

    @Test fun onsetNoveltyAlignsSharedBeatDespiteDifferentVocalLoudness() {
        val frames = 1500
        val lag = 42
        var state = 99
        val hits = BooleanArray(frames + lag) {
            state = state * 1103515245 + 12345
            (state ushr 16) % 9 == 0
        }
        val primary = FloatArray(frames) { index -> (if (hits[index + lag]) 1f else .05f) + (index % 13) * .02f }
        val companion = FloatArray(frames + lag) { index -> (if (hits[index]) .6f else .03f) + ((index * 7) % 29) * .015f }
        val result = correlateStackFeatures(
            StackAudioFeatures(primary, Array(frames) { FloatArray(12) }),
            StackAudioFeatures(companion, Array(frames + lag) { FloatArray(12) }),
        )
        assertTrue(result.confident)
        assertEquals(lag * 20.0, result.offsetMs.toDouble(), 10.0)
    }

    @Test fun musicEnvelopeWinsOverMisleadingVocalTiming() {
        val frames = 1500
        val musicLag = 40
        val vocalLag = 9
        var musicState = 12345
        val beat = FloatArray(frames) {
            musicState = musicState * 1103515245 + 12345
            if ((musicState ushr 16) % 11 == 0) 1f else .03f
        }
        var vocalState = 54321
        val vocal = FloatArray(frames) {
            vocalState = vocalState * 1103515245 + 12345
            if ((vocalState ushr 16) % 7 == 0) 1f else .02f
        }

        val primaryFull = FloatArray(frames) { beat[it] * .35f + vocal[it] * 1.8f }
        val companionFull = FloatArray(frames + musicLag) { index ->
            val music = if (index >= musicLag) beat[index - musicLag] * .35f else 0f
            val voice = if (index >= vocalLag && index - vocalLag < vocal.size) vocal[index - vocalLag] * 1.8f else 0f
            music + voice
        }
        val primaryMusic = beat
        val companionMusic = FloatArray(frames + musicLag) { index ->
            if (index >= musicLag) beat[index - musicLag] else 0f
        }
        val result = correlateStackFeatures(
            StackAudioFeatures(primaryFull, Array(frames) { FloatArray(12) }, primaryMusic),
            StackAudioFeatures(companionFull, Array(frames + musicLag) { FloatArray(12) }, companionMusic),
        )
        assertTrue(result.confident)
        assertEquals(musicLag * 20.0, result.offsetMs.toDouble(), 10.0)
    }

    @Test fun stereoSideIsUsedOnlyWhenItCarriesMeaningfulProgrammeEnergy() {
        assertTrue(stackPreferStereoSideSignal(fullPower = 100.0, sidePower = 2.0, stereo = true))
        assertFalse(stackPreferStereoSideSignal(fullPower = 100.0, sidePower = .5, stereo = true))
        assertFalse(stackPreferStereoSideSignal(fullPower = 100.0, sidePower = 50.0, stereo = false))
    }

    @Test fun onsetNoveltyIsHalfWaveRectified() {
        val novelty = stackOnsetNovelty(doubleArrayOf(0.0, 1.0, 0.5, 2.0))
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 0.0, 1.5), novelty, 1e-9)
    }

    @Test fun peakRefinementInterpolatesBetweenFrames() {
        assertEquals(0.0, stackRefinePeak(doubleArrayOf(.2, .8, .2), 1), 1e-9)
        assertTrue(stackRefinePeak(doubleArrayOf(.2, .8, .6), 1) > 0.0)
        assertTrue(stackRefinePeak(doubleArrayOf(.6, .8, .2), 1) < 0.0)
        assertEquals(0.0, stackRefinePeak(doubleArrayOf(.8, .2), 0), 1e-9)
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
