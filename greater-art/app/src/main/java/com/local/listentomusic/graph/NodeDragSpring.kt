package com.local.listentomusic.graph

import kotlin.math.hypot

/**
 * Interactive response around a held graph node. The global layout is already
 * settled off-main by GraphLayout; do not restart that solver on every pointer
 * event. Limit interactive work to a two-hop neighborhood (<=96 nodes).
 *
 * No UI, Android or Compose dependencies: deterministic JVM regression coverage.
 */
internal class NodeDragSpring(
    points: List<GraphPoint>,
    edges: List<GraphEdge>,
    private val options: GraphOptions,
    private val pinnedIndex: Int,
    initial: Map<Int, GraphPoint> = emptyMap(),
) {
    private val x = FloatArray(points.size) { initial[it]?.x ?: points[it].x }
    private val y = FloatArray(points.size) { initial[it]?.y ?: points[it].y }
    private val originalX = x.copyOf()
    private val originalY = y.copyOf()
    private val vx = FloatArray(points.size)
    private val vy = FloatArray(points.size)
    private val adjacent = Array(points.size) { mutableListOf<GraphEdge>() }
    private val active: List<Int>
    private val springs: List<Spring>
    private var quietFrames = 0

    private data class Spring(val a: Int, val b: Int, val strength: Float, val rest: Float)

    val activeCount: Int get() = active.size
    val isQuiet: Boolean get() = quietFrames >= 5

    init {
        require(pinnedIndex in points.indices)
        for (edge in edges) {
            if (edge.a !in points.indices || edge.b !in points.indices || edge.a == edge.b) continue
            adjacent[edge.a].add(edge)
            adjacent[edge.b].add(edge)
        }
        // Direct contacts take priority. The second hop is a soft perturbation
        // region; the rest of even a 10k-node graph remains completely untouched.
        val included = linkedSetOf(pinnedIndex)
        fun expand(frontier: Collection<Int>) {
            for (node in frontier) {
                for (edge in adjacent[node].sortedByDescending { it.strength }) {
                    included.add(if (edge.a == node) edge.b else edge.a)
                    if (included.size >= MAX_ACTIVE) return
                }
            }
        }
        expand(listOf(pinnedIndex))
        if (included.size < MAX_ACTIVE) expand(included.toList().drop(1))
        active = included.toList()
        springs = edges.filter { it.a in included && it.b in included && it.a != it.b }.map { edge ->
            val rest = hypot(x[edge.a] - x[edge.b], y[edge.a] - y[edge.b]).coerceAtLeast(1f)
            Spring(edge.a, edge.b, edge.strength.coerceIn(0f, 1f), rest)
        }
    }

    /**
     * The touched node is always exact. Other nodes move with bounded spring
     * impulses; damping prevents perpetual movement when the gesture finishes.
     */
    fun advance(pin: GraphPoint? = null, steps: Int = 1): Map<Int, GraphPoint> {
        if (pin != null) {
            require(pin.x.isFinite() && pin.y.isFinite())
            x[pinnedIndex] = pin.x
            y[pinnedIndex] = pin.y
            vx[pinnedIndex] = 0f
            vy[pinnedIndex] = 0f
            quietFrames = 0
        }
        repeat(steps.coerceIn(1, 6)) {
            val fx = FloatArray(x.size)
            val fy = FloatArray(y.size)
            for (spring in springs) {
                val dx = x[spring.b] - x[spring.a]
                val dy = y[spring.b] - y[spring.a]
                val distance = hypot(dx, dy).coerceAtLeast(.01f)
                val rest = spring.rest
                val force = (distance - rest) / distance *
                    (.012f + .090f * spring.strength) / options.linkDistance.coerceAtLeast(.55f)
                val pushX = dx * force
                val pushY = dy * force
                fx[spring.a] += pushX; fy[spring.a] += pushY
                fx[spring.b] -= pushX; fy[spring.b] -= pushY
                // Avoid links visually collapsing into overlapping nodes.
                if (distance < 18f) {
                    val repel = (18f - distance) * .02f * options.repulsion
                    fx[spring.a] -= dx / distance * repel; fy[spring.a] -= dy / distance * repel
                    fx[spring.b] += dx / distance * repel; fy[spring.b] += dy / distance * repel
                }
            }
            var energy = 0f
            for (index in active) {
                if (index == pinnedIndex) continue
                // A weak anchor preserves the original map's global placement
                // while connected neighbors visibly respond to the drag.
                val anchor = .003f
                val damping = options.bounce.coerceIn(.55f, .94f)
                vx[index] = ((vx[index] + fx[index] + (originalX[index] - x[index]) * anchor) * damping)
                    .coerceIn(-10f, 10f)
                vy[index] = ((vy[index] + fy[index] + (originalY[index] - y[index]) * anchor) * damping)
                    .coerceIn(-10f, 10f)
                x[index] += vx[index]
                y[index] += vy[index]
                energy += vx[index] * vx[index] + vy[index] * vy[index]
            }
            quietFrames = if (energy < .0003f * active.size) quietFrames + 1 else 0
        }
        return active.associateWith { GraphPoint(x[it], y[it]) }
    }

    companion object { const val MAX_ACTIVE = 96 }
}
