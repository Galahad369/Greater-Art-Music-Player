package com.local.listentomusic.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("+10").performClick()
        compose.runOnIdle { assertEquals(emptyList<Pair<Double, Boolean>>(), changes) }
        apply.performClick()
        compose.runOnIdle { assertEquals(listOf(-2.375 to false), changes) }
        compose.onNodeWithText("Reset alignment").performClick()
        apply.performClick()
        compose.runOnIdle { assertEquals(listOf(-2.375 to false, 0.0 to true), changes) }
    }
}
