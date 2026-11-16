package com.local.listentomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackNativeSupportTest {
    private fun coarse(regions: Int) = StackAlignment(700, .9, true, 1.0, 700000.0, regions, 1.0)

    @Test fun missingNativeRegionsCannotApproveAMonoMap() {
        for (count in 3..4) {
            val corrections = List(count) { (it + 1) * 100.0 }
            assertFalse("Only $count of six regions confirmed the native timing",
                stackValidateNativeCorrections(coarse(6), corrections, mandatory = true).confident)
        }
    }

    @Test fun partialNativeEvidenceDoesNotMoveAStereoConfirmedMap() {
        val confirmed = coarse(6)
        for (count in 3..4) {
            assertEquals(confirmed, stackValidateNativeCorrections(
                confirmed, List(count) { (it + 1) * 100.0 }, mandatory = false))
        }
    }

    @Test fun supportedNativeQuorumsStillRefineWithoutChangingTheRate() {
        for ((regions, confirmed) in listOf(3 to 3, 4 to 3, 5 to 4, 6 to 5, 6 to 6)) {
            val original = coarse(regions)
            val refined = stackValidateNativeCorrections(original,
                List(confirmed) { (it + 1) * 100.0 }, mandatory = true)
            assertTrue("$confirmed of $regions native regions meet the existing quorum", refined.confident)
            assertEquals(700000.0 + (confirmed + 1) * 50.0, refined.offsetUs, .001)
            assertEquals(original.timeScale, refined.timeScale, 0.0)
        }
    }
}
