package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import kotlin.math.*
import kotlin.random.Random

class StackInstrumentTest {
    private fun region(n: Int = 300, start: Long = 0, rotation: Int = 0, shift: Int = 0): StackInstrumentRegion {
        val random = Random(713)
        val beats = FloatArray(1800) { random.nextFloat().pow(4) }
        val bass = FloatArray(1800) { random.nextFloat().pow(2) }
        val harmonic = Array(1800) { FloatArray(12) { random.nextFloat() - .5f } }
        return StackInstrumentRegion(start,
            FloatArray(n + shift) { if (it < shift) 0f else beats[it - shift] },
            FloatArray(n + shift) { if (it < shift) 0f else bass[it - shift] },
            Array(n + shift) { i -> FloatArray(12) { p -> if (i < shift) 0f else harmonic[i - shift][(p - rotation + 12) % 12] } })
    }

    @Test fun constantOffsetsAndGainChanges() {
        val a = region()
        val b = region(300, start = 0, shift = 35)
        val amplified = b.copy(percussion = b.percussion.map { it * .2f }.toFloatArray(), bass = b.bass.map { it * 4 }.toFloatArray())
        val result = requireNotNull(stackMatchInstrumentRegion(a, amplified))
        assertEquals(700.0, result.companionMs - result.primaryMs, 1.0)
        val negative = requireNotNull(stackMatchInstrumentRegion(a.copy(startMs = 1200), b))
        assertEquals(-500.0, negative.companionMs - negative.primaryMs, 1.0)
    }

    @Test fun offGridPeaksAndUnrelatedCorpusReportErrors() {
        val errors = (1..9).map { shift ->
            val result = requireNotNull(stackMatchInstrumentRegion(region(), region(300, shift = shift * 3)))
            abs(result.companionMs - result.primaryMs - shift * 60)
        }
        var falseMatches = 0
        repeat(20) { seed ->
            val random = Random(seed + 950)
            val unrelated = StackInstrumentRegion(0, FloatArray(900) { random.nextFloat() },
                FloatArray(900) { random.nextFloat() }, Array(900) { FloatArray(12) { random.nextFloat() - .5f } })
            if (stackMatchInstrumentRegion(region(), unrelated) != null) falseMatches++
        }
        assertTrue(errors.maxOrNull()!! < 1)
        assertEquals(0, falseMatches)
        println("v7 feature corpus: accepted=9/9 maxErrorMs=${errors.maxOrNull()} unrelatedFalseMatches=$falseMatches/20")
    }

    @Test fun distributedFeaturesRecoverActualLinearDrift() {
        val random = Random(777)
        val percussion = FloatArray(9000) { random.nextFloat().pow(3) }
        val bass = FloatArray(9000) { random.nextFloat().pow(2) }
        fun sample(values: FloatArray, frame: Double): Float {
            val i = floor(frame).toInt()
            if (i !in 0 until values.lastIndex) return 0f
            val fraction = frame - i
            return (values[i] * (1 - fraction) + values[i + 1] * fraction).toFloat()
        }
        val scale = 1.002
        val offset = 713.25
        val anchors = listOf(0L, 58000L, 116000L, 170000L).map { start ->
            val search = (start - 15000).coerceAtLeast(0)
            val primary = StackInstrumentRegion(start,
                FloatArray(300) { sample(percussion, (start + it * 20.0) / 20) },
                FloatArray(300) { sample(bass, (start + it * 20.0) / 20) }, Array(300) { FloatArray(12) })
            val companion = StackInstrumentRegion(search,
                FloatArray(1800) { sample(percussion, ((search + it * 20.0 - offset) / scale) / 20) },
                FloatArray(1800) { sample(bass, ((search + it * 20.0 - offset) / scale) / 20) }, Array(1800) { FloatArray(12) })
            requireNotNull(stackMatchInstrumentRegion(primary, companion))
        }
        val map = stackFitInstrumentMap(anchors, 180000)
        assertTrue(map.confident)
        assertEquals(scale, map.timeScale, .0001)
        assertEquals(offset, map.offsetUs / 1000, 10.0)
        println("v7 feature drift: scaleError=${abs(scale - map.timeScale)} offsetErrorMs=${abs(offset - map.offsetUs / 1000)} residualMs=${map.residualMs}")
    }

    @Test fun transpositionDoesNotMoveRhythm() {
        val result = requireNotNull(stackMatchInstrumentRegion(region(), region(300, rotation = 4, shift = 35)))
        assertEquals(4, result.rotation)
        assertEquals(700.0, result.companionMs - result.primaryMs, 1.0)
    }

    @Test fun repeatedChorusesAreRejected() {
        fun loop(n: Int) = StackInstrumentRegion(0, FloatArray(n) { if (it % 25 == 0) 1f else 0f },
            FloatArray(n) { if (it % 25 == 5) .7f else 0f }, Array(n) { FloatArray(12) { p -> if (p == (it / 25) % 2) 1f else 0f } })
        assertNull(stackMatchInstrumentRegion(loop(300), loop(900)))
    }

    @Test fun silenceUnrelatedAndStaticHarmonicsAbstain() {
        val silence = StackInstrumentRegion(0, FloatArray(300), FloatArray(300), Array(300) { FloatArray(12) })
        assertNull(stackMatchInstrumentRegion(silence, silence))
        val wrong = region().copy(percussion = FloatArray(500) { Random(it + 800).nextFloat() },
            bass = FloatArray(500) { Random(it + 200).nextFloat() }, chroma = Array(500) { FloatArray(12) })
        assertNull(stackMatchInstrumentRegion(region(), wrong))
        assertNull(stackMatchInstrumentRegion(silence.copy(chroma = Array(300) { FloatArray(12) { 1f } }), silence))
    }

    @Test fun distributedMapMeasuresBothSignsAndLinearDrift() {
        for (offset in listOf(-713.25, 825.75)) {
            val anchors = listOf(3000.0, 63000.0, 123000.0, 177000.0).map {
                StackInstrumentAnchor(it, it * 1.002 + offset, .9, 0)
            }
            val result = stackFitInstrumentMap(anchors, 180000)
            assertTrue(result.confident)
            assertEquals(1.002, result.timeScale, 1e-9)
            assertEquals(offset * 1000, result.offsetUs, 1e-5)
            assertEquals(0.0, result.residualMs, 1e-8)
        }
    }

    @Test fun inconsistentRepeatedSectionsAndNonlinearDriftAbstain() {
        val anchors = listOf(3000.0, 63000.0, 123000.0, 177000.0).map {
            StackInstrumentAnchor(it, it + 700, .9, 0)
        }
        assertFalse(stackFitInstrumentMap(anchors.take(2), 180000).confident)
        assertFalse(stackFitInstrumentMap(anchors.mapIndexed { i, a -> if (i == 2) a.copy(companionMs = a.companionMs + 4000) else a }, 180000).confident)
        assertFalse(stackFitInstrumentMap(anchors.mapIndexed { i, a -> a.copy(rotation = i) }, 180000).confident)
        assertFalse(stackFitInstrumentMap(anchors.map { it.copy(primaryMs = it.primaryMs / 100) }, 180000).confident)
    }

    @Test fun clippedPlanningIsBoundedAndDistributed() {
        assertTrue(stackRegionStarts(17000).isEmpty())
        assertEquals(listOf(0L, 6000L, 12000L), stackRegionStarts(18000))
        assertEquals(6, stackRegionStarts(7_200_000).size)
        assertEquals(7_194_000L, stackRegionStarts(7_200_000).last())
    }

    @Test fun cancellationInterruptsSpectralExtractionAndMatching() {
        var calls = 0
        try {
            stackInstrumentFeatures(FloatArray(48000), 0) { if (++calls == 3) throw java.util.concurrent.CancellationException() }
            fail()
        } catch (_: java.util.concurrent.CancellationException) { assertEquals(3, calls) }
        try {
            stackMatchInstrumentRegion(region(), region(300, shift = 35)) { throw java.util.concurrent.CancellationException() }
            fail()
        } catch (_: java.util.concurrent.CancellationException) { }
    }

    @Test fun originalRateRefinementRetainsFractionalSamplePrecision() {
        val rate = 48000
        val random = Random(91)
        val a = FloatArray(rate / 3) { random.nextFloat() * random.nextFloat() }
        val shift = 83
        val b = FloatArray(a.size) { if (it >= shift) a[it - shift] else 0f }
        val measured = requireNotNull(stackNativeLagUs(a, b, rate))
        assertEquals(shift * 1e6 / rate, measured, 1e6 / rate)
        assertNull(stackNativeLagUs(FloatArray(a.size), FloatArray(a.size), rate))
    }

    @Test fun v7CacheIsBoundedValidatedAndSeparateFromV6() {
        val a = region()
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { writeStackInstrumentViews(it, StackInstrumentViews(a, a)) }
        val bytes = buffer.toByteArray()
        val restored = readStackInstrumentViews(DataInputStream(ByteArrayInputStream(bytes)), bytes.size.toLong(), 123)
        assertArrayEquals(a.percussion, restored.mono.percussion, 0f)
        assertEquals(123, restored.mono.startMs)
        for (length in listOf(0L, bytes.size - 1L, bytes.size + 1L)) try {
            readStackInstrumentViews(DataInputStream(ByteArrayInputStream(bytes)), length, 0); fail()
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun rebaseRetainsTimingMapsNotJustOffsets() {
        fun slot(name: String, scale: Double, offset: Double) = StackSlot(
            MediaFile(name, name, 180000, 0, 0, MediaKind.AUDIO), alignmentScale = scale, alignmentOffsetUs = offset)
        val slots = listOf(slot("main", 1.0, 0.0), slot("take", 1.002, 700250.0))
        val rebased = stackRebaseMappings(slots, slots[1])
        assertEquals(1.0, rebased[1].alignmentScale, 0.0)
        assertEquals(0.0, rebased[1].alignmentOffsetUs!!, 0.0)
        val newPosition = stackMappedPosition(100000, slots[1])
        assertEquals(100000.0, stackMappedPosition(newPosition, rebased[0]).toDouble(), 1.0)
    }

    @Test fun featureMemoryInputLimitsRejectUnboundedRegions() {
        try { stackInstrumentFeatures(FloatArray(16_000 * 36 + 1), 0); fail() }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun sameBackingWithDifferentVocalPhrasingAndMastering() {
        val rate = STACK_INSTRUMENT_RATE
        val padding = rate * 700 / 1000
        val hits = listOf(.41, 1.13, 1.73, 2.58, 3.43, 4.11, 5.23)
        val backing = FloatArray(rate * 6) { i ->
            val t = i.toDouble() / rate
            val attack = hits.sumOf { at ->
                val dt = t - at
                if (dt in 0.0..0.18) exp(-dt * 35) * (.8 * sin(2 * PI * 2300 * dt) + .5 * sin(2 * PI * 90 * dt)) else 0.0
            }
            attack.toFloat()
        }
        val a = FloatArray(backing.size) { i ->
            val t = i.toDouble() / rate
            (backing[i] + .55 * sin(2 * PI * 470 * t) * (.5 + .5 * sin(2 * PI * .7 * t))).toFloat()
        }
        val b = FloatArray(backing.size + padding) { i ->
            val t = i.toDouble() / rate
            ((if (i >= padding) backing[i - padding] * .65 else 0.0) +
                .8 * sin(2 * PI * 630 * t) * (.5 + .5 * sin(2 * PI * .9 * (t + .4)))).toFloat()
        }
        val result = requireNotNull(stackMatchInstrumentRegion(stackInstrumentFeatures(a, 0), stackInstrumentFeatures(b, 0)))
        assertEquals(700.0, result.companionMs - result.primaryMs, 20.0)
        println("Instrument synthetic: different phrasing/gain offset errorMs=${abs(result.companionMs - result.primaryMs - 700)} score=${result.score}")
    }
}
