package com.local.listentomusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class UiInspectorStateTest {
    @Test fun preciseCanvasHitTakesPriorityOverScreenRegion() {
        val inspector = UiInspectorState()
        inspector.update("screen", "NODES_SCREEN", "Graph page", Rect(0f, 0f, 400f, 800f))
        inspector.update("canvas", "NODES_GRAPH_CANVAS", "Graph", Rect(0f, 80f, 400f, 700f))
        inspector.setHitProvider("node") { point ->
            if (Rect(90f, 90f, 140f, 140f).contains(point))
                InspectorRegion("NODES_MEDIA_NODE", "A song.mp4", Rect(90f, 90f, 140f, 140f), Long.MAX_VALUE)
            else null
        }

        inspector.pick(Offset(110f, 110f))
        assertEquals("NODES_MEDIA_NODE", inspector.selected?.label)
        inspector.selectNextMatch()
        assertEquals("NODES_GRAPH_CANVAS", inspector.selected?.label)
    }

    @Test fun blankSpaceReportsBackgroundNotMissingImplementation() {
        val inspector = UiInspectorState()
        inspector.pick(Offset(10f, 20f))
        assertEquals("WINDOW_BACKGROUND", inspector.selected?.label)
    }
}
