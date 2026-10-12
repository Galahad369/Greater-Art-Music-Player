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
            DeveloperDiagnostics("screen=LIBRARY", emptyList(), false,
                remember { UiInspectorState() }, onDisableDeveloperMode = {})
        } }
        compose.onNodeWithText("DEV").assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Inspector").assertIsDisplayed()
        compose.onNodeWithText("Pick an element").performClick()
        compose.onNodeWithText("Inspector").assertDoesNotExist()
    }

    @Test fun developerModeCanBeTurnedOffFromInsideTheInspector() {
        val enabled = androidx.compose.runtime.mutableStateOf(true)
        var disableCalls = 0
        compose.setContent { MaterialTheme {
            if (enabled.value) DeveloperDiagnostics(
                report = "screen=LIBRARY",
                regions = emptyList(),
                warning = false,
                inspector = remember { UiInspectorState() },
                onDisableDeveloperMode = { disableCalls++; enabled.value = false },
            )
        } }
        compose.onNodeWithText("DEV").performClick()
        compose.onNodeWithText("Turn off Developer Mode").assertIsDisplayed()
            .assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, disableCalls) }
        compose.onNodeWithText("DEV").assertDoesNotExist()
        compose.onNodeWithText("Inspector").assertDoesNotExist()
    }

    @Test fun percentageEditorPreserves26AndCancelDoesNotChangeValue() {
        var value = .6f
        compose.setContent { MaterialTheme {
            DimSliderSetting("Background dimming", "Adjust visibility", .6f, { value = it })
        } }
        compose.onNodeWithContentDescription("Background dimming: 60%").performClick()
        compose.onNodeWithText("25–85%").performTextReplacement("26")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(.26f, value, .00001f) }
        compose.onNodeWithContentDescription("Background dimming: 60%").performClick()
        compose.onNodeWithText("25–85%").performTextReplacement("80")
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(.26f, value, .00001f) }
    }

    @Test fun labelledToggleRowsHaveOneActionAndRespectDisabledState() {
        val checked = androidx.compose.runtime.mutableStateOf(false)
        var changes = 0
        compose.setContent { MaterialTheme { Column {
            SwitchSetting("Setting label", "Description", checked.value, { checked.value = it; changes++ })
            SwitchSetting("Disabled setting", "Description", false, { changes++ }, enabled = false)
            GraphToggle("Graph label", checked.value, { checked.value = it; changes++ })
        } } }
        compose.onNodeWithText("Setting label").assertIsOff().assertHasClickAction().performClick()
        compose.onNodeWithText("Setting label").assertIsOn()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, changes) }
        compose.onNodeWithText("Graph label").assertIsOn().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Graph label").assertIsOff()
        compose.onNodeWithText("Disabled setting").assertIsNotEnabled()
        compose.runOnIdle { org.junit.Assert.assertEquals(2, changes) }
    }

    @Test fun nodesLoadingAndEmptyStatesUseChosenLanguage() {
        val graph = androidx.compose.runtime.mutableStateOf<com.local.listentomusic.graph.LibraryGraph?>(null)
        compose.setContent { MaterialTheme {
            NodesScreen(graph.value, false, null, null, androidx.compose.foundation.layout.PaddingValues(0.dp),
                {}, {}, com.local.listentomusic.graph.GraphOptions(), {},
                language = com.local.listentomusic.data.AppLanguage.FRENCH)
        } }
        compose.onNodeWithText("Préparation des connexions…").assertIsDisplayed()
        compose.runOnIdle { graph.value = com.local.listentomusic.graph.LibraryGraph(emptyList(), emptyList(), emptyList()) }
        compose.onNodeWithText("Ajoutez des médias dans Download puis actualisez la bibliothèque.").assertIsDisplayed()
        compose.onNodeWithText("Your library is empty. Add media in Download, then refresh Library.").assertDoesNotExist()
    }
}
