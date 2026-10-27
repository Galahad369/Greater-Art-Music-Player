package com.local.listentomusic.graph

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.*

/** Desktop link rest length: strong filename matches sit close, weak ones stay visibly long. */
internal fun graphLinkTargetDistance(strength: Float, linkDistance: Float): Float =
    (32f + 125f * (1f - strength.coerceIn(0f, 1f))) * linkDistance

/** Summed link strength per node, normalised to the strongest (>= 1), as desktop `Graph::weights`. */
internal fun graphNodeWeights(count: Int, edges: List<GraphEdge>): List<Float> {
    val sum = FloatArray(count)
    edges.forEach { edge ->
        if (edge.a in 0 until count && edge.b in 0 until count) { sum[edge.a] += edge.strength; sum[edge.b] += edge.strength }
    }
    val largest = max(1f, sum.maxOrNull() ?: 1f)
    return sum.map { it / largest }
}

internal fun strongestGraphHub(nodeCount: Int, edges: List<GraphEdge>): Int {
    if (nodeCount <= 0) return 0
    val degree = FloatArray(nodeCount)
    edges.forEach { edge -> if (edge.a in degree.indices && edge.b in degree.indices) {
        degree[edge.a] += edge.strength; degree[edge.b] += edge.strength
    } }
    return degree.indices.maxByOrNull { degree[it] } ?: 0
}

/**
 * Finite background solve: the same [GraphSimulation] the live Nodes view runs (desktop physics),
 * stepped headlessly until it sleeps, so the cached layout is already at rest when shown.
 */
object GraphLayout {
    // Bound the solve's node-step count; wall-clock cost still depends on graph shape/device.
    private const val SETTLE_WORK = 1_500_000
    private const val GOLDEN_ANGLE = 2.3999632297

    suspend fun settle(nodes: List<GraphInput>, edges: List<GraphEdge>, linkDistance: Float = 1f,
        repulsion: Float = 1f, bounce: Float = .82f, initial: List<GraphPoint>? = null): List<GraphPoint> {
        val n = nodes.size
        if (n == 0) return emptyList()
        val hub = strongestGraphHub(n, edges)
        // Deterministic phyllotaxis seed (no RNG), strongest hub at the origin.
        val seed = initial ?: List(n) { i ->
            if (i == hub) GraphPoint(0f, 0f) else {
                val angle = i * GOLDEN_ANGLE; val radius = 24.0 * sqrt(i + 1.0)
                GraphPoint((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
            }
        }
        val simulation = GraphSimulation()
        simulation.reset(seed, edges, graphNodeWeights(n, edges),
            GraphOptions(linkDistance = linkDistance, repulsion = repulsion, bounce = bounce).toForces())
        val maxSteps = (SETTLE_WORK / n).coerceIn(60, 730)
        var step = 0
        while (simulation.awake && step++ < maxSteps) {
            coroutineContext.ensureActive()
            simulation.advance(1.0 / 60)
        }
        // Exact invariant: the strongest weighted hub is the graph origin.
        val hubX = simulation.x(hub); val hubY = simulation.y(hub)
        return List(n) { GraphPoint((simulation.x(it) - hubX).toFloat(), (simulation.y(it) - hubY).toFloat()) }
    }
}
