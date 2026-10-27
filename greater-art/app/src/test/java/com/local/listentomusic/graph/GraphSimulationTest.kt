package com.local.listentomusic.graph

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class GraphSimulationTest {
    private val pair = listOf(GraphPoint(-100f, 0f), GraphPoint(100f, 0f))
    private fun simulation(strength: Float) = GraphSimulation().apply {
        reset(pair, listOf(GraphEdge(0, 1, strength)), listOf(1f, 1f))
    }
    private fun run(sim: GraphSimulation, seconds: Int, rate: Int = 60) {
        repeat(seconds * rate) { sim.advance(1.0 / rate) }
    }

    @Test fun strongerLinksSettleCloserWithoutOverlap() {
        val strong = simulation(.95f)
        val weak = simulation(.15f)
        run(strong, 8); run(weak, 8)
        val near = strong.x(1) - strong.x(0)
        val far = weak.x(1) - weak.x(0)
        assertTrue(near >= 36)
        assertTrue(far > near * 1.5)
    }

    @Test fun dynamicsAreRefreshRateIndependent() {
        val fast = simulation(.7f); val slow = simulation(.7f)
        run(fast, 2, 180); run(slow, 2)
        assertEquals(slow.x(0), fast.x(0), 1e-7)
    }

    @Test fun dragPinsNodeNeighborsFollowThenSleepAndReheat() {
        val sim = simulation(.95f)
        run(sim, 8)
        val before = sim.x(0)
        repeat(180) { sim.advance(1.0 / 180, 1, 160.0, 70.0) }
        assertEquals(160.0, sim.x(1), 0.0)
        assertEquals(70.0, sim.y(1), 0.0)
        assertTrue(sim.x(0) > before + 10)
        run(sim, 14)
        assertFalse(sim.awake)
        sim.advance(1.0 / 60, 1, 160.0, 70.0)
        assertTrue(sim.awake)
    }

    @Test fun coincidentNodesSeparateAndLargeGraphStaysFinite() {
        val sim = GraphSimulation()
        sim.reset(List(32) { GraphPoint(0f, 0f) }, emptyList(), List(32) { 0f })
        run(sim, 1)
        assertTrue(hypot(sim.x(0) - sim.x(1), sim.y(0) - sim.y(1)) > 1)
        val points = List(2000) { i -> GraphPoint(i.toFloat(), (i % 13).toFloat()) }
        sim.reset(points, (1 until 2000).map { GraphEdge(it - 1, it, .3f) }, List(2000) { .2f })
        repeat(12) { sim.advance(1.0 / 180) }
        assertEquals(2000, sim.count)
        assertTrue((0 until sim.count).all { sim.x(it).isFinite() && sim.y(it).isFinite() })
    }

    @Test fun cachedLayoutsEmptyAndSingleStayAsleep() {
        val sim = GraphSimulation()
        for (points in listOf(emptyList(), listOf(GraphPoint(0f, 0f)), pair)) {
            sim.reset(points, emptyList(), emptyList(), startAwake = false)
            sim.advance(.1)
            assertFalse(sim.awake)
            assertEquals(points.size, sim.count)
        }
    }

    @Test fun workerDropsObsoleteGraphsAndCancelsCleanly() = runBlocking {
        val tasks = ArrayDeque<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.addLast(block) }
        }
        val worker = GraphMotion(dispatcher)
        val job = worker.start(this)
        worker.reset(List(100) { GraphPoint(it.toFloat(), 0f) }, emptyList(), emptyList(), GraphForces())
        val generation = worker.reset(pair, listOf(GraphEdge(0, 1, .8f)), listOf(1f, 1f), GraphForces())
        worker.request(.02, -1, 0.0, 0.0)
        while (tasks.isNotEmpty()) tasks.removeFirst().run()
        val frame = requireNotNull(worker.take())
        assertEquals(generation, frame.generation)
        assertEquals(4, frame.xy.size)
        assertTrue(frame.xy.all { it.isFinite() })
        assertEquals(null, worker.take())
        worker.request(0.0, 1, 320.0, 80.0)
        worker.request(0.0, -1, 0.0, 0.0)
        while (tasks.isNotEmpty()) tasks.removeFirst().run()
        val released = requireNotNull(worker.take())
        assertEquals(320f, released.xy[2], 0f)
        assertEquals(80f, released.xy[3], 0f)
        job.cancel()
        while (tasks.isNotEmpty()) tasks.removeFirst().run()
        job.join()
        assertTrue(job.isCancelled)
    }
}
