package com.local.listentomusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.local.listentomusic.ui.components.GaIconAction
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Actual measured semantics/layout, not a grep-based accessibility claim. */
class DesignAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun labelledActionsKeep48DpTargetsAndSpeedNeedleIsDecorative() {
        var clicked = false
        compose.setContent { MaterialTheme {
            Column {
                GaIconAction(Icons.Rounded.Speed, "播放速度", { clicked = true })
                IconButton(onClick = {}, modifier = Modifier.semantics { contentDescription = "速度 1.0×" }) {
                    SpeedDialIcon(1f, MaterialTheme.colorScheme.primary)
                }
            }
        } }
        compose.onNodeWithContentDescription("播放速度").assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertTrue(clicked) }
        compose.onNodeWithContentDescription("速度 1.0×").assertHasClickAction()
        compose.onAllNodesWithContentDescription("Playback speed", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun numericDimmingActionIsMeasuredAndOpensEditorAtLargeFontScale() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { DimSliderSetting("Background dimming", "Adjust visibility", .6f, {}) }
            }
        }
        compose.onNodeWithContentDescription("Background dimming: 60%").assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Background dimming %").assertIsDisplayed()
    }

    @Test fun developerBadgeHasMeasuredTargetAndPickStillWorks() {
        compose.setContent { MaterialTheme {
            DeveloperDiagnostics("screen=LIBRARY", emptyList(), false, remember { UiInspectorState() })
        } }
        compose.onNodeWithText("DEV").assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Inspector").assertIsDisplayed()
        compose.onNodeWithText("Pick an element").performClick()
        compose.onNodeWithText("Inspector").assertDoesNotExist()
    }
}
