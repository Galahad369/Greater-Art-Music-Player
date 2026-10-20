package com.local.listentomusic.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class GraphLayoutPolicyTest {
    @Test fun strongerLinksHaveShorterTargetDistance() {
        assertTrue(graphLinkTargetDistance(.9f, 1f) < graphLinkTargetDistance(.5f, 1f))
        assertTrue(graphLinkTargetDistance(.5f, 1f) < graphLinkTargetDistance(.1f, 1f))
    }

    @Test fun strongestWeightedNodeBecomesHub() {
        val edges = listOf(GraphEdge(0, 1, .8f), GraphEdge(0, 2, .7f), GraphEdge(1, 3, .2f))
        assertEquals(0, strongestGraphHub(4, edges))
    }

    @Test fun boundedSolverPreservesOptionsAndFiniteCenteredResults(): Unit = runBlocking {
        val nodes = (0..7).map { GraphInput("$it", "take $it") }
        val edges = (1..7).map { GraphEdge(0, it, .9f - it * .05f) }
        for (bounce in listOf(.55f, .82f, .94f)) {
            val points = GraphLayout.settle(nodes, edges, linkDistance = .55f, repulsion = 2f, bounce = bounce)
            assertEquals(nodes.size, points.size)
            assertEquals(GraphPoint(0f, 0f), points[0])
            assertTrue(points.all { it.x.isFinite() && it.y.isFinite() })
            assertEquals(points, GraphLayout.settle(nodes, edges, .55f, 2f, bounce))
        }
    }

    @Test fun linkDistanceControlChangesTheSolvedGeometry(): Unit = runBlocking {
        val nodes = (0..2).map { GraphInput("$it", "song $it") }
        val edges = listOf(GraphEdge(0, 1, .9f), GraphEdge(0, 2, .6f))
        fun spread(points: List<GraphPoint>) = points.sumOf { (it.x * it.x + it.y * it.y).toDouble() }
        assertTrue(spread(GraphLayout.settle(nodes, edges, 1.8f)) > spread(GraphLayout.settle(nodes, edges, .55f)))
    }
}
