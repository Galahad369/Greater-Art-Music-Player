package com.local.listentomusic.ui.components

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AspectPreservingPlayerViewTest {
    @Test fun wideVideoFitsOrCropsPortraitWithoutChangingItsProportions() {
        // The Float representation of 16/9 places the ideal height just below
        // 607.5px; nearest-integer rounding therefore selects 607, not 608.
        assertEquals(VideoContentSize(1080, 607), aspectPreservingVideoSize(1080, 2340, 16f / 9f, crop = false))
        assertEquals(VideoContentSize(4160, 2340), aspectPreservingVideoSize(1080, 2340, 16f / 9f, crop = true))
    }

    @Test fun wideVideoFitsOrCropsLandscapeWithoutUsingTheScreenAspect() {
        assertEquals(VideoContentSize(1920, 1080), aspectPreservingVideoSize(2340, 1080, 16f / 9f, crop = false))
        assertEquals(VideoContentSize(2340, 1316), aspectPreservingVideoSize(2340, 1080, 16f / 9f, crop = true))
    }

    @Test fun portraitVideoKeepsItsOwnAspectInALandscapeViewport() {
        assertEquals(VideoContentSize(608, 1080), aspectPreservingVideoSize(2340, 1080, 9f / 16f, crop = false))
        assertEquals(VideoContentSize(2340, 4160), aspectPreservingVideoSize(2340, 1080, 9f / 16f, crop = true))
    }

    @Test fun squareVideoStaysSquareAcrossViewportOrientations() {
        for ((width, height) in listOf(1080 to 2340, 2340 to 1080)) {
            assertEquals(VideoContentSize(1080, 1080), aspectPreservingVideoSize(width, height, 1f, crop = false))
            assertEquals(VideoContentSize(2340, 2340), aspectPreservingVideoSize(width, height, 1f, crop = true))
        }
    }

    @Test fun nearMatchingAspectDoesNotUseTheNativeOnePercentStretchTolerance() {
        val fit = aspectPreservingVideoSize(1000, 1000, 1.009f, crop = false)
        val crop = aspectPreservingVideoSize(1000, 1000, 1.009f, crop = true)
        assertEquals(VideoContentSize(1000, 991), fit)
        assertEquals(VideoContentSize(1009, 1000), crop)
        assertNotEquals(VideoContentSize(1000, 1000), fit)
        assertNotEquals(VideoContentSize(1000, 1000), crop)
    }

    @Test fun alreadyMatchingVideoNeedsNoResize() {
        assertEquals(VideoContentSize(1920, 1080), aspectPreservingVideoSize(1920, 1080, 16f / 9f, crop = false))
        assertEquals(VideoContentSize(1920, 1080), aspectPreservingVideoSize(1920, 1080, 16f / 9f, crop = true))
    }

    @Test fun fitContainsAndCropCoversWithOnlyFinalPixelRounding() {
        val viewports = listOf(1 to 1, 2 to 17, 17 to 2, 317 to 641, 1080 to 2340, 2340 to 1080)
        val aspects = listOf(.125f, .5f, 9f / 16f, 1f, 1.009f, 4f / 3f, 16f / 9f, 3f, 8f)
        for ((width, height) in viewports) for (aspect in aspects) for (crop in listOf(false, true)) {
            val label = "viewport=${width}x$height aspect=$aspect crop=$crop"
            val result = aspectPreservingVideoSize(width, height, aspect, crop)
            assertNotNull(label, result)
            val actual = requireNotNull(result)
            assertTrue(label, actual.width >= 1 && actual.height >= 1)
            if (crop) {
                assertTrue(label, actual.width >= width && actual.height >= height)
            } else {
                assertTrue(label, actual.width <= width && actual.height <= height)
            }
            // Independent proportional-scale formulation. Rounding tiny results
            // up to one physical pixel may differ by more than half a pixel.
            val scale = if (crop) max(width / aspect.toDouble(), height.toDouble())
                else min(width / aspect.toDouble(), height.toDouble())
            assertTrue("$label width must retain one proportional scale", abs(actual.width - aspect * scale) <= 1.0)
            assertTrue("$label height must retain one proportional scale", abs(actual.height - scale) <= 1.0)
        }
    }

    @Test fun invalidViewportsAndAspectRatiosDoNotProduceAFrame() {
        for (crop in listOf(false, true)) {
            for ((width, height) in listOf(0 to 1080, 1080 to 0, -1 to 1080, 1080 to -1)) {
                assertNull(aspectPreservingVideoSize(width, height, 16f / 9f, crop))
            }
            for (aspect in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
                assertNull(aspectPreservingVideoSize(1080, 2340, aspect, crop))
            }
        }
    }

    @Test fun unrepresentableCropIsRejectedRatherThanClampingEachAxis() {
        assertNull(aspectPreservingVideoSize(1080, 2340, Float.MAX_VALUE, crop = true))
        assertNull(aspectPreservingVideoSize(1080, 2340, Float.MIN_VALUE, crop = true))
    }

    @Test fun cropBeyondTheNativeMeasuredSizeMaskIsRejected() {
        // 23.4 million pixels fit in an Int and a MeasureSpec, but exceed
        // View.MEASURED_SIZE_MASK. Native measured dimensions would lose bits.
        assertNull(aspectPreservingVideoSize(1080, 2340, 10_000f, crop = true))
        assertNull(aspectPreservingVideoSize(2340, 1080, .0001f, crop = true))
    }

    @Test fun tinyFitUsesAtLeastOnePixelWithoutOverflow() {
        assertEquals(VideoContentSize(1080, 1), aspectPreservingVideoSize(1080, 2340, Float.MAX_VALUE, crop = false))
        assertEquals(VideoContentSize(1, 2340), aspectPreservingVideoSize(1080, 2340, Float.MIN_VALUE, crop = false))
    }

    @Test fun sourceAspectIncludesAnamorphicPixels() {
        assertEquals(16f / 9f, requireNotNull(sourceVideoAspectRatio(1440, 1080, 4f / 3f)), .00001f)
        assertEquals(4f / 3f, requireNotNull(sourceVideoAspectRatio(1440, 1080, 1f)), .00001f)
        assertEquals(2f, requireNotNull(sourceVideoAspectRatio(720, 720, 2f)), .00001f)
    }

    @Test fun renderedPortraitDimensionsAreNotRotatedAgain() {
        assertEquals(9f / 16f, requireNotNull(sourceVideoAspectRatio(720, 1280, 1f)), .00001f)
        assertEquals(16f / 9f, requireNotNull(sourceVideoAspectRatio(1280, 720, 1f)), .00001f)
    }

    @Test fun unknownSourceAspectStaysUnknownInsteadOfGuessingSixteenByNine() {
        for ((width, height) in listOf(0 to 0, 0 to 1080, 1920 to 0, -1 to 1080, 1920 to -1)) {
            assertNull(sourceVideoAspectRatio(width, height, 1f))
        }
        for (pixelAspect in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertNull(sourceVideoAspectRatio(1920, 1080, pixelAspect))
        }
    }

    @Test fun overflowingOrUnderflowingSourceAspectIsRejected() {
        assertNull(sourceVideoAspectRatio(Int.MAX_VALUE, 1, Float.MAX_VALUE))
        assertNull(sourceVideoAspectRatio(1, Int.MAX_VALUE, Float.MIN_VALUE))
    }
}
