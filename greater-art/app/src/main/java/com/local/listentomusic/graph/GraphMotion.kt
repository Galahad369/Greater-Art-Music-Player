package com.local.listentomusic.graph

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

/**
 * Off-main driver for [GraphSimulation], the Kotlin twin of desktop `GraphMotion`: one coalesced
 * request and one replaceable result. The UI never waits for physics; results from an obsolete
 * graph (older [reset] generation) are dropped.
 */
internal class GraphMotion(private val dispatcher: CoroutineDispatcher) {
    class Frame(val xy: FloatArray, val awake: Boolean, val generation: Int)

    private class Seed(val points: List<GraphPoint>, val edges: List<GraphEdge>, val weights: List<Float>, val forces: GraphForces, val startAwake: Boolean)
    private class Work(val seed: Seed?, val seconds: Double, val pin: Int, val pinX: Double, val pinY: Double,
        val releasedPin: Int, val releasedX: Double, val releasedY: Double, val generation: Int)

    private val lock = Any()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val frames = Channel<Unit>(Channel.CONFLATED)
    private val output = AtomicReference<Frame?>(null)
    private var generation = 0
    private var seed: Seed? = null
    private var seconds = 0.0
    private var pin = -1
    private var pinX = 0.0
    private var pinY = 0.0
    private var releasedPin = -1
    private var releasedX = 0.0
    private var releasedY = 0.0

    fun start(scope: CoroutineScope): Job = scope.launch(dispatcher) {
        val simulation = GraphSimulation()
        for (signal in wake) {
            val work = synchronized(lock) {
                Work(seed, seconds, pin, pinX, pinY, releasedPin, releasedX, releasedY, generation).also {
                    seed = null; seconds = 0.0; releasedPin = -1
                }
            }
            work.seed?.let { simulation.reset(it.points, it.edges, it.weights, it.forces, it.startAwake) }
            // A quick up/cancel can coalesce with the last drag request. Retain its
            // final coordinate before releasing, rather than snapping back to the old point.
            if (work.releasedPin >= 0) simulation.advance(0.0, work.releasedPin, work.releasedX, work.releasedY)
            simulation.advance(work.seconds, work.pin, work.pinX, work.pinY)
            val xy = FloatArray(simulation.count * 2).also(simulation::copyTo)
            val frame = Frame(xy, simulation.awake, work.generation)
            synchronized(lock) {
                if (work.generation == generation) {
                    output.set(frame)
                    frames.trySend(Unit)
                }
            }
        }
    }

    /** Replace the graph (and wake the worker); returns the new generation. */
    fun reset(points: List<GraphPoint>, edges: List<GraphEdge>, weights: List<Float>, forces: GraphForces,
        startAwake: Boolean = true): Int =
        synchronized(lock) {
            generation++
            seed = Seed(points, edges, weights, forces, startAwake)
            seconds = 0.0
            pin = -1
            releasedPin = -1
            output.set(null)
            wake.trySend(Unit)
            generation
        }

    /** Ask for [dt] more seconds of physics; calls between worker runs coalesce (max 0.25 s). */
    fun request(dt: Double, pinIndex: Int, x: Double, y: Double) {
        synchronized(lock) {
            seconds = min(.25, seconds + max(0.0, dt))
            if (pinIndex < 0 && pin >= 0) {
                releasedPin = pin; releasedX = pinX; releasedY = pinY
            }
            pin = pinIndex; pinX = x; pinY = y
        }
        wake.trySend(Unit)
    }

    /** Latest finished frame, or null. Taking it clears it. */
    fun take(): Frame? = output.getAndSet(null)

    /** Suspend idle UI work until physics has a new result; cancellation follows its owner. */
    suspend fun awaitFrame(): Frame? {
        frames.receive()
        return take()
    }
}
