package com.local.listentomusic.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExpandedPlayerGesturesTest {
    @get:Rule val compose = createComposeRule()
    private var consumedMoves = 0
    private var pulls = 0
    private var ends = 0
    private var cancels = 0
    private var clicks = 0

    private fun show(legacy: Boolean = false) {
        compose.setContent { MaterialTheme {
            val density = LocalDensity.current.density
            val parent = if (legacy) Modifier.pointerInput(Unit) {
                // Retained original failure: location filtering occurs only after
                // this detector has consumed touch-slop movement outside the handle.
                detectVerticalDragGestures(onVerticalDrag = { _, _ -> }, onDragEnd = { cancels++ })
            } else Modifier.expandedPlayerPullInput(true, density, { pulls++ }, { _, _ -> ends++ }, { cancels++ })
            Box(Modifier.size(384.dp, 200.dp).then(parent).testTag("pull-parent")) {
                Box(Modifier.size(48.dp).clickable { clicks++ }.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.any { it.position != it.previousPosition && it.isConsumed }) consumedMoves++
                        } while (event.changes.any { it.pressed })
                    }
                }.testTag("action"))
            }
        } }
    }

    private fun outsideDrag() {
        compose.onNodeWithTag("pull-parent").performTouchInput {
            swipe(Offset(24.dp.toPx(), 24.dp.toPx()), Offset(24.dp.toPx(), 130.dp.toPx()), 350)
        }
    }

    private fun assertUntouched() {
        assertEquals("Do not consume movement over actions", 0, consumedMoves)
        assertEquals(0, pulls)
        assertEquals(0, ends)
        assertEquals(0, cancels)
    }

    @Test fun sameAssertionRejectsOriginalOutsideHandleConsumption() {
        show(legacy = true)
        outsideDrag()
        compose.runOnIdle {
            assertTrue("The baseline must fail for consumed movement, not only a cancel callback", consumedMoves > 0)
            assertThrows(AssertionError::class.java) { assertUntouched() }
        }
    }

    @Test fun outsideActionDragDoesNotBelongToTheHandleAndClicksStillWork() {
        show()
        outsideDrag()
        compose.runOnIdle { assertUntouched() }
        compose.onNodeWithTag("action").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun originalDownNearHandleBottomRemainsValidAfterTouchSlop() {
        show()
        compose.onNodeWithTag("pull-parent").performTouchInput {
            swipe(Offset(center.x, 50.dp.toPx()), Offset(center.x, 150.dp.toPx()), 350)
        }
        compose.runOnIdle { assertTrue(pulls > 0); assertEquals(1, ends); assertEquals(0, cancels) }
    }
}
