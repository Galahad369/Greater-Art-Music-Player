package com.local.listentomusic.ui

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InspectorSelectedCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortLandscapeKeepsFullCopyAndCloseWithLargeFrenchText() = exercise(DpSize(640.dp, 240.dp))
    @Test fun narrowCardKeepsLocalizedActionsReachable() = exercise(DpSize(320.dp, 360.dp))

    private fun exercise(size: DpSize) {
        val inspector = UiInspectorState()
        var density = 1f
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(1.7f)) {
                MaterialTheme {
                    val currentDensity = LocalDensity.current.density
                    SideEffect { density = currentDensity }
                    UiInspectorHost(true, inspector, AppLanguage.FRENCH) { Box(Modifier.fillMaxSize()) }
                }
            }
        }
        compose.runOnIdle {
            val detail = (1..80).joinToString("\n") { "detail_$it=$it" }
            inspector.update(Any(), "PARENT", detail, Rect(0f, 0f, 200f, 200f))
            inspector.update(Any(), "SELECTED", detail, Rect(0f, 0f, 100f, 100f))
            inspector.pick(Offset(10f, 10f))
        }
        compose.onNodeWithText("Copier").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription("Fermer la sélection").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.runOnIdle {
            val expected = inspector.selectionReport(density)
            assertTrue(expected!!.contains("detail_80=80"))
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(ClipboardManager::class.java)
            assertEquals(expected, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
        compose.onNodeWithContentDescription("Fermer la sélection").performClick()
        compose.runOnIdle { assertEquals(null, inspector.selected) }
    }
}
