package com.local.listentomusic.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeDragSpringTest {
    private val points = listOf(GraphPoint(0f, 0f), GraphPoint(60f, 0f), GraphPoint(0f, 70f),
        GraphPoint(260f, 260f))
    private val edges = listOf(GraphEdge(0, 1, .98f), GraphEdge(0, 2, .2f))

    @Test fun draggedNodeStaysExactlyPinnedAndStrongNeighborsRespond() {
        val spring = NodeDragSpring(points, edges, GraphOptions(), 0)
        val newPositions = spring.advance(GraphPoint(100f, 0f), steps = 6)
        assertEquals(GraphPoint(100f, 0f), newPositions[0])
        assertTrue(newPositions.getValue(1).x > points[1].x)
        assertTrue(newPositions.values.all { it.x.isFinite() && it.y.isFinite() })
        assertEquals(3, spring.activeCount)
        assertTrue(3 !in newPositions)
    }

    @Test fun draggingWithoutLinksDoesNotDisturbUnrelatedNodes() {
        val spring = NodeDragSpring(points, edges, GraphOptions(), 3)
        assertEquals(mapOf(3 to GraphPoint(275f, 275f)),
            spring.advance(GraphPoint(275f, 275f), steps = 6))
    }

    @Test fun releaseSettlesWithFiniteBoundedPositions() {
        val spring = NodeDragSpring(points, edges, GraphOptions(bounce = .55f), 0)
        spring.advance(GraphPoint(30f, 0f), steps = 6)
        var result = emptyMap<Int, GraphPoint>()
        repeat(220) { result = spring.advance(steps = 6) }
        assertEquals(GraphPoint(30f, 0f), result[0])
        assertTrue(result.values.all { it.x.isFinite() && it.y.isFinite() })
        assertTrue(spring.isQuiet)
    }

    @Test fun hugeGraphCapsDynamicNeighborhood() {
        val many = (0..500).map { GraphPoint(it * 30f, 0f) }
        val star = (1..500).map { GraphEdge(0, it, .75f) }
        val spring = NodeDragSpring(many, star, GraphOptions(), 0)
        assertEquals(NodeDragSpring.MAX_ACTIVE, spring.activeCount)
        val result = spring.advance(GraphPoint(45f, 0f), steps = 4)
        assertEquals(NodeDragSpring.MAX_ACTIVE, result.size)
        assertEquals(GraphPoint(45f, 0f), result[0])
    }
}
