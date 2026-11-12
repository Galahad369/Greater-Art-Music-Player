package com.local.listentomusic.playback

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class StackPictureNearestSamplerTest {
    private fun select(times: List<Long>, endUs: Long, fps: Int = 30): List<Long> {
        val result = mutableListOf<Long>()
        val sampler = StackPictureNearestSampler(fps, endUs) { time, _ -> result += time }
        val pixels = FloatArray(256)
        times.forEach { sampler.offer(it, pixels) }
        sampler.finish()
        sampler.finish()
        return result
    }

    @Test fun nativeThirtyFpsKeepsEveryPictureAndItsOriginalTimestamp() {
        val times = List(1080) { (it * 1_000_000L + 15L) / 30L }
        assertEquals(times, select(times, 36_000_000L))
    }

    @Test fun admissionDoesNotDependOnTheAbsoluteClipClock() {
        for (origin in listOf(0L, 17_000L, -721_500L, 1_000_000_000_000L)) {
            val times = List(120) { origin + (it * 1_000_000L + 15L) / 30L }
            assertEquals(times, select(times, origin + 4_000_000L))
        }
    }

    @Test fun higherFrameRatesUseTheClosestPicturesNotTheFirstPictureOfEachBucket() {
        val times = List(120) { (it * 1_000_000L + 30L) / 60L }
        assertEquals(times.filterIndexed { index, _ -> index % 2 == 0 }, select(times, 2_000_000L))
    }

    @Test fun endOfWindowEmitsAPendingNearestPictureOnlyOnce() {
        assertEquals(listOf(0L, 25_000L), select(listOf(0L, 25_000L), 50_000L))
        assertEquals(listOf(12_345L), select(listOf(12_345L), 40_000L))
        assertEquals(emptyList<Long>(), select(emptyList(), 40_000L))
    }

    @Test fun pendingPictureSurvivesReuseOfTheDecoderLumaArray() {
        val result = mutableListOf<Pair<Long, Float>>()
        val sampler = StackPictureNearestSampler(30, 60_000L) { time, pixels -> result += time to pixels[0] }
        val pixels = FloatArray(256)
        pixels[0] = 1f; sampler.offer(0L, pixels)
        pixels[0] = 2f; sampler.offer(25_000L, pixels)
        pixels[0] = 9f
        pixels[0] = 3f; sampler.offer(50_000L, pixels)
        sampler.finish()
        assertEquals(listOf(0L to 1f, 25_000L to 2f), result)
    }

    @Test fun variableCadenceAgreesWithAnIndependentFullWindowOracle() {
        repeat(8) { seed ->
            val random = Random(seed)
            var time = -721_500L
            val times = List(300) { time += random.nextLong(1_000L, 100_000L); time }
            val end = times.last() + 17_000L
            val expected = mutableListOf<Long>()
            var tick = 0L
            while (true) {
                val target = times.first() + (tick++ * 1_000_000L + 15L) / 30L
                if (target >= end) break
                val nearest = times.minBy { abs(it - target) }
                if (expected.lastOrNull() != nearest) expected += nearest
            }
            assertEquals(expected, select(times, end))
        }
    }
}
