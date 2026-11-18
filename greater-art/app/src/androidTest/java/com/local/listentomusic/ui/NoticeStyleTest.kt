package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoticeStyleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noticePlateIsHalfTransparentAndFollowsPaletteWithoutFadingText() {
        val plate = mutableStateOf(Color.Red)
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(surfaceContainerHigh = plate.value, onSurface = Color.White)) {
                Box(Modifier.fillMaxSize().background(Color.Blue)) {
                    ThemedNotice(Modifier.width(320.dp).testTag("notice")) { Text("Notification") }
                }
            }
        }
        fun check(red: Float, green: Float) {
            val pixels = compose.onNodeWithTag("notice").captureToImage().toPixelMap()
            val sample = pixels[pixels.width / 2, 5]
            assertEquals(red, sample.red, .04f)
            assertEquals(green, sample.green, .04f)
            assertEquals(.5f, sample.blue, .04f)
            val brightest = (0 until pixels.height).flatMap { y ->
                (0 until pixels.width).map { x -> pixels[x,y] }
            }.maxOf { minOf(it.red, it.green, it.blue) }
            assertEquals("Text remains opaque white", 1f, brightest, .04f)
        }
        check(.5f, 0f)
        compose.runOnIdle { plate.value = Color.Green }
        check(0f, .5f)
    }

    @Test fun inspectorPromptIsCompactButCancelRetainsItsTouchTarget() {
        val inspector = UiInspectorState()
        compose.setContent {
            MaterialTheme {
                UiInspectorHost(true, inspector) { Text("Content") }
            }
        }
        compose.runOnIdle { inspector.arm() }
        val prompt = compose.onNodeWithTag("INSPECTOR_PROMPT").assertIsDisplayed()
        val bounds = prompt.getUnclippedBoundsInRoot()
        assertTrue("Inspector label stays compact", bounds.right - bounds.left <= 112.dp)
        compose.onNodeWithContentDescription("Cancel inspector").assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertFalse(inspector.armed) }
    }
}
