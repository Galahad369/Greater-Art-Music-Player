package com.local.listentomusic.ui

import android.content.ClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeveloperDiagnosticsTest {
    @get:Rule val compose = createComposeRule()
    private val detail = "version=test\ndecoder=complete\nfirstFrameAttribution=retained\nlastImportantKey=42"

    private fun show(warning: Boolean = false) {
        compose.setContent { MaterialTheme {
            DeveloperDiagnostics(listOf("App: test", "Screen: NODES", "Paused · Audio", "Warnings: None"),
                detail, listOf("NODES_CANVAS · 100×100px"), warning, remember { UiInspectorState() }, {})
        } }
        compose.onNodeWithText(if (warning) "DEV!" else "DEV").performClick()
    }

    @Test fun defaultIsConciseAndTechnicalDetailsCanExpandAndCollapse() {
        show()
        compose.onNodeWithText("Developer").assertIsDisplayed()
        capture("summary")
        compose.onNodeWithText(detail).assertDoesNotExist()
        compose.onNodeWithText("Show region IDs").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Technical details"))
        compose.onNodeWithText("Technical details").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(detail))
        compose.onNodeWithText(detail).assertIsDisplayed()
        capture("details")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Hide technical details"))
        compose.onNodeWithText("Hide technical details").performClick()
        compose.onNodeWithText(detail).assertDoesNotExist()
    }

    @Test fun copyDoesNotTruncateToTheSummary() {
        show()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Copy bug report"))
        compose.onNodeWithText("Copy bug report").performClick()
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
            assertEquals(completeDeveloperReport(detail, listOf("NODES_CANVAS · 100×100px")),
                clip?.getItemAt(0)?.text.toString())
        }
    }

    @Test fun warningBadgeAndPickerArePreserved() {
        show(warning = true)
        compose.onNodeWithText("Needs attention").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Pick an element"))
        compose.onNodeWithText("Pick an element").performClick()
        compose.onNodeWithText("Developer").assertDoesNotExist()
    }

    @Test fun shortLargeFontPanelKeepsActionsReachable() {
        var disabled = false
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 360.dp)) then
                DeviceConfigurationOverride.FontScale(1.7f)) {
                MaterialTheme {
                    DeveloperDiagnostics(listOf("App: test", "Screen: NODES", "Playing · Video · Queue: 219",
                        "Stack: 6", "Surface: BACKGROUND", "Warnings: None"), detail, emptyList(), false,
                        remember { UiInspectorState() }, { disabled = true }, systemOverlay = true)
                }
            }
        }
        compose.onNodeWithText("DEV").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Copy bug report"))
        compose.onNodeWithText("Copy bug report").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Turn off Developer Mode"))
        compose.onNodeWithText("Turn off Developer Mode").performClick()
        compose.runOnIdle { assertTrue(disabled) }
    }

    @Test fun explicitlyEnabledRegionsRemainVisibleAfterClosingThePanel() {
        show()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Technical details"))
        compose.onNodeWithText("Technical details").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Show region IDs"))
        compose.onNodeWithText("Show region IDs").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Close"))
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("NODES_CANVAS · 100×100px").assertIsDisplayed()
        compose.onNodeWithText("DEV").performClick()
        compose.onNodeWithText(detail).assertDoesNotExist()
        compose.onNodeWithText("Show region IDs").assertDoesNotExist()
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "developer-$name.png")
                .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
}
