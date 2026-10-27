package com.local.listentomusic.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.local.listentomusic.graph.GraphEdge
import com.local.listentomusic.graph.GraphInput
import com.local.listentomusic.graph.GraphOptions
import com.local.listentomusic.graph.GraphPoint
import com.local.listentomusic.graph.LibraryGraph
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.min

class NodesMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dragReleaseControlsAndGraphReplacementKeepNavigationWorking() {
        val graph = mutableStateOf(LibraryGraph(
            listOf(GraphInput("a", "Singer A.mp3"), GraphInput("b", "Singer B.mp3")),
            listOf(GraphEdge(0, 1, .9f)), listOf(GraphPoint(-100f, 0f), GraphPoint(100f, 0f))))
        val options = mutableStateOf(GraphOptions())
        var played: String? = null
        compose.setContent { MaterialTheme {
            NodesScreen(graph.value, false, null, "a", PaddingValues(0.dp), {}, { played = it },
                options.value, { options.value = it })
        } }
        val canvas = compose.onNodeWithContentDescription(
            "Filename similarity graph. Pinch to zoom, drag individual nodes, tap a node to play, or swipe empty space to change page. Use Find / play a node for a text list.")
        val bounds = canvas.fetchSemanticsNode().boundsInRoot
        val scale = min(bounds.width / 300f, bounds.height / 100f).coerceIn(.08f, 3f)
        val point = Offset(bounds.width / 2 - 100 * scale, bounds.height / 2)
        canvas.performTouchInput { click(point) }
        compose.runOnIdle { assertEquals("a", played); played = null }
        canvas.performTouchInput { swipe(point, point + Offset(90f, 60f), 450) }
        compose.runOnIdle { assertEquals(null, played) }
        compose.onNodeWithText("Fit").performClick()
        compose.onNodeWithText("Controls").performClick()
        compose.onNodeWithText("Link distance").assertIsDisplayed()
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { graph.value = LibraryGraph(listOf(GraphInput("c", "New take.mp3")),
            emptyList(), listOf(GraphPoint(0f, 0f))) }
        compose.onNodeWithText("Find").performClick()
        compose.onNodeWithText("New take.mp3").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("c", played) }
    }
}
