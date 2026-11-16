package com.local.listentomusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.StackAlignment
import com.local.listentomusic.playback.StackAlignmentMethod
import com.local.listentomusic.playback.StackAlignmentProgress
import com.local.listentomusic.playback.StackSlot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StackAlignmentReviewTest {
    @Test fun pictureTimeoutAndReadFailureShowTheirActualReasonsAndKeepManualControls() {
        val slots = listOf(slot("/reference.mp4"), slot("/timeout.mp4"), slot("/unreadable.mp4"))
        val progress = StackAlignmentProgress(total = 2, completed = 2, matched = 0,
            primaryPath = "/reference.mp4", paths = slots.map { it.file.path },
            method = StackAlignmentMethod.PICTURE, widePictureSearch = true,
            eligiblePaths = slots.drop(1).map { it.file.path }, pictureFailures = mapOf(
                "/timeout.mp4" to com.local.listentomusic.playback.StackPictureFailure.DECODE_TIME_LIMIT,
                "/unreadable.mp4" to com.local.listentomusic.playback.StackPictureFailure.UNREADABLE))
        var retries = 0
        var manual = ""
        compose.setContent { StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, {}, { manual = it }, { retries++ }) }
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/timeout.mp4").assertTextContains("Picture decoding reached its time limit.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_WIDER").performClick()
        compose.onNodeWithTag("STACK_ALIGNMENT_REVIEW_LIST").performScrollToNode(hasTestTag("STACK_ALIGNMENT_MANUAL_/unreadable.mp4"))
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/unreadable.mp4").assertTextContains("This video could not be read. Check folder access.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_MANUAL_/unreadable.mp4").performClick()
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retries); assertEquals("/unreadable.mp4", manual) }
    }

    @Test fun failedReferenceIsNotMisreportedAsDifferentCompanionPictures() {
        val slots = listOf(slot("/reference.mp4"), slot("/companion.mp4"))
        val progress = StackAlignmentProgress(total = 1, matched = 0, primaryPath = "/reference.mp4",
            paths = slots.map { it.file.path }, eligiblePaths = listOf("/companion.mp4"),
            method = StackAlignmentMethod.PICTURE, pictureFailures = mapOf(
                "/reference.mp4" to com.local.listentomusic.playback.StackPictureFailure.UNREADABLE,
                "/companion.mp4" to com.local.listentomusic.playback.StackPictureFailure.REFERENCE_UNAVAILABLE))
        compose.setContent { StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, {}, {}, {}) }
        compose.onNodeWithTag("STACK_ALIGNMENT_REFERENCE_FAILURE").assertTextContains("This video could not be read. Check folder access.")
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/companion.mp4").assertTextContains("The reference video could not be analysed.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").assertDoesNotExist()
    }

    @get:Rule val compose = createComposeRule()

    private fun slot(path: String, offsetMs: Double = 0.0) = StackSlot(
        file = MediaFile(path, path.substringAfterLast('/'), 60000, 1000, 0, MediaKind.VIDEO),
        alignmentOffsetUs = offsetMs * 1000.0,
    )

    @Test fun reviewNamesTheReferenceAndShowsAcceptedAndUnmatchedTracks() {
        val slots = listOf(slot("/reference.mp4"), slot("/matched.mp4", -12.375), slot("/unmatched.mp4"))
        val proposal = mapOf("/matched.mp4" to StackAlignment(26, 0.9, true, timeScale = 1.00025))
        val progress = StackAlignmentProgress(total = 2, matched = 1, primaryPath = "/reference.mp4",
            paths = slots.map { it.file.path }, eligiblePaths = slots.drop(1).map { it.file.path },
            results = proposal + ("/unmatched.mp4" to StackAlignment(0, 0.1, false)), proposal = proposal)
        var manualPath: String? = null
        var applied = 0
        compose.setContent {
            StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, { applied++ },
                { manualPath = it }, {})
        }
        compose.onNodeWithTag("STACK_ALIGNMENT_REFERENCE").assertTextContains("reference.mp4", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_METHOD").assertTextContains(
            "Sound estimates backing timing. Vocals can affect the result; review before applying.")
        compose.onNodeWithTag("STACK_ALIGNMENT_TIMING_/matched.mp4").assertTextContains(
            "-12.375 → +26 ms", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_SPEED_/matched.mp4").assertTextContains(
            "1× → 1.00025×", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").assertTextContains("Apply timing (1)")
        compose.onNodeWithTag("STACK_ALIGNMENT_REVIEW_LIST").performScrollToNode(
            hasTestTag("STACK_ALIGNMENT_MANUAL_/unmatched.mp4"))
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/unmatched.mp4").assertTextContains(
            "Could not establish one consistent sound offset.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_MANUAL_/unmatched.mp4").performClick()
        compose.runOnIdle {
            assertEquals("/unmatched.mp4", manualPath)
            assertEquals(0, applied)
        }
        compose.onNodeWithTag("STACK_ALIGNMENT_MANUAL_/reference.mp4").assertDoesNotExist()
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").performClick()
        compose.runOnIdle { assertEquals(1, applied) }
    }

    @Test fun zeroMatchesStillShowManualControlsAndHonestPictureReason() {
        val slots = listOf(slot("/reference.mp4"), slot("/unmatched.mp4", 8.125))
        val progress = StackAlignmentProgress(total = 1, matched = 0, primaryPath = "/reference.mp4",
            paths = slots.map { it.file.path }, method = StackAlignmentMethod.PICTURE,
            eligiblePaths = listOf("/unmatched.mp4"))
        compose.setContent {
            StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, {}, {}, {})
        }
        compose.onNodeWithTag("STACK_ALIGNMENT_METHOD").assertTextContains(
            "Picture timing is frame-limited. Matching images does not verify matching sound.")
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/unmatched.mp4").assertTextContains(
            "Could not establish one consistent picture offset.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_MANUAL_/unmatched.mp4").assertIsDisplayed()
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").assertDoesNotExist()
    }

    @Test fun widerPictureSearchRemainsAvailableWhenEveryQuickTrackMatched() {
        val slots = listOf(slot("/reference.mp4"), slot("/matched.mp4"))
        val proposal = mapOf("/matched.mp4" to StackAlignment(26, 0.9, true))
        val progress = StackAlignmentProgress(total = 1, matched = 1, primaryPath = "/reference.mp4",
            paths = slots.map { it.file.path }, method = StackAlignmentMethod.PICTURE,
            eligiblePaths = listOf("/matched.mp4"), results = proposal, proposal = proposal)
        var retries = 0
        compose.setContent {
            StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, {}, {}, { retries++ })
        }
        compose.onNodeWithTag("STACK_ALIGNMENT_WIDER").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun unfinishedQuickTrackIsNotPresentedAsAnAnalysedRejection() {
        val slots = listOf(slot("/reference.mp4"), slot("/rejected.mp4"), slot("/unfinished.mp4"))
        val progress = StackAlignmentProgress(total = 2, completed = 1, matched = 0,
            primaryPath = "/reference.mp4", paths = slots.map { it.file.path },
            method = StackAlignmentMethod.PICTURE, quickPictureTimedOut = true,
            eligiblePaths = slots.drop(1).map { it.file.path })
        compose.setContent {
            StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, {}, {}, {})
        }
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/rejected.mp4").assertTextContains(
            "Could not establish one consistent picture offset.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_REVIEW_LIST").performScrollToNode(
            hasTestTag("STACK_ALIGNMENT_MANUAL_/unfinished.mp4"))
        compose.onNodeWithTag("STACK_ALIGNMENT_REASON_/unfinished.mp4").assertTextContains(
            "Not completed before the quick scan time limit.", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").assertDoesNotExist()
    }

    @Test fun appliedTimingIsNotReportedAsVerifiedAndCannotBeAppliedTwice() {
        val slots = listOf(slot("/reference.mp4"), slot("/matched.mp4", 26.0))
        val results = mapOf("/matched.mp4" to StackAlignment(26, 0.9, true))
        val progress = StackAlignmentProgress(total = 1, matched = 1, primaryPath = "/reference.mp4",
            paths = slots.map { it.file.path }, eligiblePaths = listOf("/matched.mp4"),
            results = results, applied = true)
        compose.setContent {
            StackAlignmentReview(AppLanguage.ENGLISH, slots, progress, {}, {}, {}, {})
        }
        compose.onNodeWithTag("STACK_ALIGNMENT_STATUS").assertTextContains("Timing applied", substring = true)
        compose.onNodeWithTag("STACK_ALIGNMENT_CAUTION").assertTextContains(
            "Timing was applied, not verified. Listen and adjust if needed.")
        compose.onNodeWithTag("STACK_ALIGNMENT_APPLY").assertDoesNotExist()
    }
}
