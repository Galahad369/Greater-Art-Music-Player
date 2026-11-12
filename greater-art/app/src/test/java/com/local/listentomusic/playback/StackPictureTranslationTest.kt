package com.local.listentomusic.playback

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test

class StackPictureTranslationTest {
    @Test fun mixedCadenceTimingDoesNotDependOnWhichSideOfZeroItFalls() {
        val primary = List(24 * 18) { index ->
            val random = java.util.Random(317L + index * 1009L)
            val pixels = FloatArray(256) { 25f + random.nextInt(176) }
            requireNotNull(stackPictureFrame((index * 1_000_000L + 12L) / 24L, pixels))
        }
        val companion = List(30 * 18) { index ->
            val time = (index * 1_000_000L + 15L) / 30L
            val source = ((time - 17_000L) * 24.0 / 1_000_000.0).roundToInt()
            primary[source.coerceIn(primary.indices)].copy(timeUs = time)
        }
        for (search in StackPictureSearch.entries) {
            val baseline = requireNotNull(estimateStackPictureOffset(primary, companion, search))
            for (shift in listOf(-1_000_000L, -500_000L, 500_000L, 1_000_000L)) {
                val translated = companion.map { it.copy(timeUs = it.timeUs + shift) }
                val result = requireNotNull(estimateStackPictureOffset(primary, translated, search))
                assertEquals("PICTURE_TIMELINE_TRANSLATION: padding must translate the same scene ($search)",
                    baseline.offsetUs + shift, result.offsetUs, 1.0)
            }
        }
    }
}
