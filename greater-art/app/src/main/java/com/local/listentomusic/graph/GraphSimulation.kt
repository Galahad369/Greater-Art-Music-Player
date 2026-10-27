package com.local.listentomusic.graph

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Force percentages and per-substep velocity retention, as in the desktop GraphForces. */
internal class GraphForces(
    val center: Double = 100.0,
    val repel: Double = 100.0,
    val link: Double = 100.0,
    val distance: Double = 100.0,
    val damping: Double = DEFAULT_DAMPING,
) {
    companion object { const val DEFAULT_DAMPING = .94 }
}

/**
 * The phone's Link distance / Repulsion / Spring damping controls drive the same simulation
 * the desktop app uses. The damping slider keeps its 0.55..0.94 range; its default (0.82)
 * maps to the desktop's fixed 0.94 per-substep retention.
 */
internal fun GraphOptions.toForces() = GraphForces(
    repel = repulsion * 100.0,
    distance = linkDistance * 100.0,
    damping = 1.0 - (1.0 - bounce.coerceIn(.55f, .94f)) / 3.0,
)

/**
 * Kotlin port of desktop/native/GraphMotion.h `GraphSimulation`: same constants, centre
 * gravity, Barnes-Hut repulsion with overlap clearance, degree-biased link springs, cooling
 * and sleep rules, so the phone graph moves like the Windows one.
 *
 * World space and refresh-rate independent: callers pass elapsed seconds and physics advance
 * in fixed 1/240 s substeps. Pure JVM (no Android/Compose) for deterministic tests. Not
 * thread-safe: own it from one thread (see [GraphMotion]).
 */
internal class GraphSimulation {
    private var n = 0
    private var x = DoubleArray(0)
    private var y = DoubleArray(0)
    private var vx = DoubleArray(0)
    private var vy = DoubleArray(0)
    private var fx = DoubleArray(0)
    private var fy = DoubleArray(0)
    private var weight = DoubleArray(0)
    private var radius = DoubleArray(0)
    private var edgeA = IntArray(0)
    private var edgeB = IntArray(0)
    private var edgeTarget = DoubleArray(0)
    private var edgeSpring = DoubleArray(0)
    private var edgeBias = DoubleArray(0)
    private var forces = GraphForces()

    // Quadtree cells (parallel arrays, grown on demand) and the point order they index.
    private var cells = 0
    private var cellX = DoubleArray(0)
    private var cellY = DoubleArray(0)
    private var cellSize = DoubleArray(0)
    private var massX = DoubleArray(0)
    private var massY = DoubleArray(0)
    private var cellBegin = IntArray(0)
    private var cellEnd = IntArray(0)
    private var cellChild = IntArray(0)
    private var order = IntArray(0)
    private var scratch = IntArray(0)

    private var accumulator = 0.0
    private var quiet = 0.0
    private var lastPin = -1

    val count: Int get() = n
    var awake = false
        private set
    /** Seconds since the last (re)heat; drives cooling. */
    private var age = 0.0

    fun x(i: Int): Double = x[i]
    fun y(i: Int): Double = y[i]

    fun copyTo(out: FloatArray) {
        for (i in 0 until n) { out[2 * i] = x[i].toFloat(); out[2 * i + 1] = y[i].toFloat() }
    }

    /** [startAwake] false keeps an already-settled layout asleep until a drag or setting reheats it. */
    fun reset(points: List<GraphPoint>, edges: List<GraphEdge>, weights: List<Float>,
        settings: GraphForces = GraphForces(), startAwake: Boolean = true) {
        forces = settings
        n = points.size
        x = DoubleArray(n) { points[it].x.toDouble() }
        y = DoubleArray(n) { points[it].y.toDouble() }
        vx = DoubleArray(n); vy = DoubleArray(n); fx = DoubleArray(n); fy = DoubleArray(n)
        weight = DoubleArray(n) { weights.getOrNull(it)?.toDouble() ?: 0.0 }
        radius = DoubleArray(n) { 6.0 + 12.0 * weight[it].coerceIn(0.0, 1.0).pow(.72) }
        order = IntArray(n); scratch = IntArray(n)
        val valid = edges.filter { it.a in 0 until n && it.b in 0 until n && it.a != it.b }
        val degree = DoubleArray(n) { 1.0 }
        valid.forEach { degree[it.a] += it.strength; degree[it.b] += it.strength }
        edgeA = IntArray(valid.size) { valid[it].a }
        edgeB = IntArray(valid.size) { valid[it].b }
        val linkScale = (settings.distance / 100.0).toFloat()
        edgeTarget = DoubleArray(valid.size) {
            val e = valid[it]
            max(radius[e.a] + radius[e.b] + 4.0, graphLinkTargetDistance(e.strength, linkScale).toDouble())
        }
        edgeSpring = DoubleArray(valid.size) {
            (.01 + .035 * valid[it].strength.coerceIn(0f, 1f)) * settings.link / 100.0
        }
        // A hub pulls its satellites without accelerating itself once per edge.
        edgeBias = DoubleArray(valid.size) { degree[valid[it].a] / (degree[valid[it].a] + degree[valid[it].b]) }
        age = 0.0; accumulator = 0.0; quiet = 0.0; lastPin = -1
        awake = startAwake && n > 1
    }

    /** Advance by [seconds]; a pinned node is held exactly and reheats the simulation. */
    fun advance(seconds: Double, pin: Int = -1, pinX: Double = 0.0, pinY: Double = 0.0) {
        if (!seconds.isFinite() || seconds < 0) return
        val held = if (pin >= n || pin < -1 || !pinX.isFinite() || !pinY.isFinite()) -1 else pin
        if (held >= 0 || held != lastPin) { age = 0.0; quiet = 0.0; awake = n > 1 }
        lastPin = held
        if (held >= 0) { x[held] = pinX; y[held] = pinY; vx[held] = 0.0; vy[held] = 0.0 }
        if (!awake) return
        age += seconds
        accumulator += min(seconds, .05)
        while (accumulator + 1e-10 >= STEP) {
            accumulator -= STEP
            if (substep(held)) { accumulator = 0.0; break }
        }
    }

    /** One 1/240 s step. Returns true when the graph has gone to sleep. */
    private fun substep(pin: Int): Boolean {
        var loX = 0.0; var loY = 0.0; var hiX = 1.0; var hiY = 1.0
        for (i in 0 until n) {
            loX = min(loX, x[i]); loY = min(loY, y[i]); hiX = max(hiX, x[i]); hiY = max(hiY, y[i])
        }
        cells = 0
        for (i in 0 until n) order[i] = i
        cell(0, n, loX - 1, loY - 1, max(hiX - loX, hiY - loY) + 2, 0)
        for (i in 0 until n) {
            if (i == pin) continue
            val hub = weight[i]
            val centre = (.0015 + .004 * hub * hub) * forces.center / 100.0
            fx[i] = -x[i] * centre
            fy[i] = -y[i] * centre
            visit(i, 0)
        }
        for (e in edgeA.indices) {
            val a = edgeA[e]; val b = edgeB[e]
            val dx = x[b] - x[a]; val dy = y[b] - y[a]
            val distance = max(.01, sqrt(dx * dx + dy * dy))
            val spring = (distance - edgeTarget[e]) / distance * edgeSpring[e]
            val bias = edgeBias[e]
            fx[a] += dx * spring * (1 - bias); fy[a] += dy * spring * (1 - bias)
            fx[b] -= dx * spring * bias; fy[b] -= dy * spring * bias
        }
        val cooling = ((12.0 - age) / 4.0).coerceIn(0.0, 1.0)
        var energy = 0.0
        for (i in 0 until n) {
            if (i == pin) continue
            // Integrator units are 60 Hz steps, subdivided for refresh independence.
            var nvx = (vx[i] + fx[i] * .25 * cooling) * forces.damping
            var nvy = (vy[i] + fy[i] * .25 * cooling) * forces.damping
            val speed = sqrt(nvx * nvx + nvy * nvy)
            if (speed > MAX_SPEED) { nvx *= MAX_SPEED / speed; nvy *= MAX_SPEED / speed }
            vx[i] = nvx; vy[i] = nvy
            x[i] += nvx * .25; y[i] += nvy * .25
            energy += speed * speed
        }
        quiet = if (energy / n < .001) quiet + STEP else 0.0
        if (pin < 0 && (quiet > .5 || age > 12)) { awake = false; return true }
        return false
    }

    private fun newCell(cx: Double, cy: Double, size: Double, begin: Int, end: Int): Int {
        if (cells == cellX.size) {
            val grown = max(64, cells * 2)
            cellX = cellX.copyOf(grown); cellY = cellY.copyOf(grown); cellSize = cellSize.copyOf(grown)
            massX = massX.copyOf(grown); massY = massY.copyOf(grown)
            cellBegin = cellBegin.copyOf(grown); cellEnd = cellEnd.copyOf(grown)
            cellChild = cellChild.copyOf(grown * 4)
        }
        val index = cells++
        cellX[index] = cx; cellY[index] = cy; cellSize[index] = size
        cellBegin[index] = begin; cellEnd[index] = end
        for (q in 0..3) cellChild[index * 4 + q] = -1
        return index
    }

    private fun cell(begin: Int, end: Int, cx: Double, cy: Double, size: Double, depth: Int): Int {
        val index = newCell(cx, cy, size, begin, end)
        var sumX = 0.0; var sumY = 0.0
        for (k in begin until end) { sumX += x[order[k]]; sumY += y[order[k]] }
        massX[index] = sumX / (end - begin)
        massY[index] = sumY / (end - begin)
        if (end - begin <= LEAF_POINTS || depth >= MAX_DEPTH) return index
        val half = size / 2
        fun quadrant(i: Int) = (if (x[i] >= cx + half) 1 else 0) or (if (y[i] >= cy + half) 2 else 0)
        val counts = IntArray(4)
        for (k in begin until end) counts[quadrant(order[k])]++
        val starts = IntArray(5)
        starts[0] = begin
        for (q in 0..3) starts[q + 1] = starts[q] + counts[q]
        val cursor = intArrayOf(starts[0], starts[1], starts[2], starts[3])
        for (k in begin until end) scratch[cursor[quadrant(order[k])]++] = order[k]
        System.arraycopy(scratch, begin, order, begin, end - begin)
        for (q in 0..3) if (counts[q] > 0) {
            val child = cell(starts[q], starts[q + 1], cx + (q and 1) * half, cy + (q shr 1) * half, half, depth + 1)
            cellChild[index * 4 + q] = child
        }
        return index
    }

    private fun repel(i: Int, ox: Double, oy: Double, count: Double, otherRadius: Double) {
        var dx = x[i] - ox
        var dy = y[i] - oy
        var square = dx * dx + dy * dy
        // Coincident starts separate deterministically: no random shake, no divide by zero.
        if (square < .001) {
            dx = cos(i * GOLDEN_ANGLE); dy = sin(i * GOLDEN_ANGLE); square = 1.0
        }
        var amount = 46.0 * forces.repel / 100.0 * count / (square + 36)
        if (otherRadius > 0) {
            val distance = sqrt(square)
            val clearance = radius[i] + otherRadius + 4
            if (distance < clearance) amount += .32 * (clearance - distance) / distance
        }
        fx[i] += dx * amount
        fy[i] += dy * amount
    }

    private fun visit(i: Int, index: Int) {
        val px = x[i]; val py = y[i]
        val dx = px - massX[index]; val dy = py - massY[index]
        val size = cellSize[index]
        val inside = px >= cellX[index] && px <= cellX[index] + size && py >= cellY[index] && py <= cellY[index] + size
        val clearance = size * 1.414 + radius[i] + 22
        val d2 = dx * dx + dy * dy
        if (!inside && size * size < .49 * d2 && d2 > clearance * clearance) {
            repel(i, massX[index], massY[index], (cellEnd[index] - cellBegin[index]).toDouble(), 0.0)
            return
        }
        var leaf = true
        for (q in 0..3) {
            val child = cellChild[index * 4 + q]
            if (child >= 0) { leaf = false; visit(i, child) }
        }
        if (leaf) for (k in cellBegin[index] until cellEnd[index]) {
            val j = order[k]
            if (j != i) repel(i, x[j], y[j], 1.0, radius[j])
        }
    }

    private companion object {
        const val STEP = 1.0 / 240
        const val MAX_SPEED = 10.0
        const val LEAF_POINTS = 8
        const val MAX_DEPTH = 20
        const val GOLDEN_ANGLE = 2.3999632297
    }
}
