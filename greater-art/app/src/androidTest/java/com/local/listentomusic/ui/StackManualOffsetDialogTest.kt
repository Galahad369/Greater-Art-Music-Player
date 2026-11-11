package com.local.listentomusic.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StackManualOffsetDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signedOffsetAndFineNudgeOnlyApplyAfterConfirmation() {
        val changes = mutableListOf<Pair<Double, Boolean>>()
        compose.setContent {
            StackManualOffsetDialog(AppLanguage.ENGLISH, "Companion", 12.375, {}) { offset, reset ->
                changes += offset to reset
            }
        }
        val field = compose.onNodeWithTag("STACK_OFFSET_INPUT")
        val apply = compose.onNodeWithTag("STACK_OFFSET_APPLY")
        field.performTextReplacement("-30001")
        apply.assertIsNotEnabled()
        field.performTextReplacement("-12.375")
        compose.onNodeWithTag("STACK_OFFSET_EARLIER_10").performScrollTo().performClick()
        field.assertTextContains("-2.375")
        compose.runOnIdle { assertEquals(emptyList<Pair<Double, Boolean>>(), changes) }
        apply.performClick()
        compose.runOnIdle { assertEquals(listOf(-2.375 to false), changes) }
        compose.onNodeWithTag("STACK_OFFSET_RESET").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(-2.375 to false), changes) }
        apply.performClick()
        compose.runOnIdle { assertEquals(listOf(-2.375 to false, 0.0 to true), changes) }
    }

    @Test fun earlierAndLaterNudgesKeepFractionsAndRespectTheRange() {
        val changes = mutableListOf<Pair<Double, Boolean>>()
        compose.setContent {
            StackManualOffsetDialog(AppLanguage.ENGLISH, "Companion", 12.375, {}) { offset, reset ->
                changes += offset to reset
            }
        }
        val field = compose.onNodeWithTag("STACK_OFFSET_INPUT")
        compose.onNodeWithTag("STACK_OFFSET_LATER_1").performScrollTo().performClick()
        field.assertTextContains("11.375")
        compose.onNodeWithTag("STACK_OFFSET_EARLIER_100").performScrollTo().performClick()
        field.assertTextContains("111.375")
        field.performScrollTo().performTextReplacement("29999.75")
        compose.onNodeWithTag("STACK_OFFSET_EARLIER_1").performScrollTo().performClick()
        field.assertTextContains("30000")
        field.performScrollTo().performTextReplacement("-29999.75")
        compose.onNodeWithTag("STACK_OFFSET_LATER_100").performScrollTo().performClick()
        field.assertTextContains("-30000")
        compose.runOnIdle { assertEquals(emptyList<Pair<Double, Boolean>>(), changes) }
        compose.onNodeWithTag("STACK_OFFSET_APPLY").performClick()
        compose.runOnIdle { assertEquals(listOf(-30000.0 to false), changes) }
    }

    @Test fun resetAndNudgesCanBeCancelledWithoutApplying() {
        val changes = mutableListOf<Pair<Double, Boolean>>()
        var dismissals = 0
        compose.setContent {
            StackManualOffsetDialog(AppLanguage.ENGLISH, "Companion", 12.375,
                onDismiss = { dismissals++ }) { offset, reset -> changes += offset to reset }
        }
        compose.onNodeWithTag("STACK_OFFSET_RESET").performScrollTo().performClick()
        compose.onNodeWithTag("STACK_OFFSET_EARLIER_1").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertEquals(emptyList<Pair<Double, Boolean>>(), changes)
            assertEquals(1, dismissals)
        }
    }
}
