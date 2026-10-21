package com.local.listentomusic.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class ThumbnailQueueStats(
    val queued: Int = 0,
    val running: Int = 0,
    val shared: Int = 0,
    val cancelled: Int = 0,
    val rejected: Int = 0,
    val peakPending: Int = 0,
)

/**
 * One bounded queue for all artwork consumers. Cancellation belongs to a consumer:
 * dropping one row must not cancel the same image in another visible presentation.
 * Native decoders may finish their current call after cancellation; a permit is not
 * returned until that coroutine really exits, and checkpoints prevent later work.
 */
internal class ThumbnailRequestScheduler<K : Any, V : Any>(
    private val scope: CoroutineScope,
    private val workers: Int,
    private val capacity: Int = 96,
    private val onStats: (ThumbnailQueueStats) -> Unit = {},
) {
    private enum class Phase { DISK, GENERATION }

    private class Request<V : Any>(
        val diskProbe: (suspend () -> V?)?,
        val generation: (suspend () -> V?)?,
        val prefetch: Boolean,
    ) {
        val result = CompletableDeferred<V?>()
        var consumers = 1
        var job: Job? = null
        var phase = if (diskProbe != null) Phase.DISK else Phase.GENERATION
    }

    private val guard = Any()
    private val requests = linkedMapOf<K, Request<V>>()
    private val viewports = mutableMapOf<String, Set<K>>()
    private var paused = false
    private var running = 0
    private var runningDisk = 0
    private var shared = 0
    private var cancelled = 0
    private var rejected = 0
    private var peakPending = 0

    init { require(workers > 0 && capacity >= workers) }

    // Legacy generation-only work retains its existing cancellation contract.
    suspend fun load(key: K, prefetch: Boolean = false, loader: suspend () -> V?): V? =
        enqueue(key, prefetch, null, loader)

    /**
     * A visible request may read its small persistent disk cache while flinging.
     * On a miss, leave the same request queued for the expensive generation
     * stage after scrolling ends. Cache-only callers omit generation.
     */
    suspend fun loadStaged(
        key: K,
        prefetch: Boolean = false,
        diskProbe: suspend () -> V?,
        generation: (suspend () -> V?)? = null,
    ): V? = enqueue(key, prefetch, diskProbe, generation)

    private suspend fun enqueue(
        key: K,
        prefetch: Boolean,
        diskProbe: (suspend () -> V?)?,
        generation: (suspend () -> V?)?,
    ): V? {
        coroutineContext.ensureActive()
        val request = synchronized(guard) {
            requests[key]?.also {
                it.consumers++
                shared++
            } ?: if (requests.size >= capacity) {
                rejected++
                publishLocked()
                null
            } else {
                Request(diskProbe, generation, prefetch).also {
                    requests[key] = it
                    peakPending = maxOf(peakPending, requests.size)
                }
            }
        } ?: return null
        try {
            synchronized(guard) { pumpLocked() }
            return request.result.await()
        } finally {
            synchronized(guard) {
                if (requests[key] === request && --request.consumers == 0) {
                    requests.remove(key)
                    cancelled++
                    request.job?.cancel()
                    request.result.cancel()
                    pumpLocked()
                }
            }
        }
    }

    fun setPaused(value: Boolean) = synchronized(guard) {
        if (paused == value) return@synchronized
        paused = value
        if (value) requests.values.forEach { request ->
            // Preserve in-flight disk reads; only native frame/artwork extraction
            // gets interrupted. A cancelled generation remains queued for idle.
            if (request.phase == Phase.GENERATION) {
                request.job?.takeIf { it.isActive }?.let { cancelled++; it.cancel() }
            }
        }
        pumpLocked()
    }

    fun setViewport(holder: String, visible: Set<K>) = synchronized(guard) {
        // Avoid a queue pump when the viewport membership hasn't changed.
        if (viewports[holder] == visible || (visible.isEmpty() && holder !in viewports)) return@synchronized
        if (visible.isEmpty()) viewports.remove(holder) else viewports[holder] = visible
        pumpLocked()
    }

    fun clear() = synchronized(guard) {
        val old = requests.values.toList()
        requests.clear()
        cancelled += old.size
        old.forEach { it.result.complete(null); it.job?.cancel() }
        publishLocked()
    }

    fun snapshot(): ThumbnailQueueStats = synchronized(guard) { statsLocked() }

    private fun pumpLocked() {
        while (scope.isActive && running < workers && (!paused || runningDisk == 0)) {
            // Stable insertion order within each priority class. Select in one
            // pass without allocating a filtered request list on each pump.
            var next: Map.Entry<K, Request<V>>? = null
            var bestPriority = 3
            for (entry in requests.entries) {
                val request = entry.value
                if (request.job != null || (paused && request.phase == Phase.GENERATION)) continue
                val priority = when {
                    viewports.values.any { entry.key in it } -> 0
                    !request.prefetch -> 1
                    else -> 2
                }
                if (priority < bestPriority) {
                    next = entry
                    bestPriority = priority
                    if (priority == 0) break
                }
            }
            val selected = next ?: break
            val key = selected.key
            val request = selected.value
            val phase = request.phase
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val value = when (phase) {
                        Phase.DISK -> request.diskProbe!!.invoke()
                        Phase.GENERATION -> request.generation!!.invoke()
                    }
                    coroutineContext.ensureActive()
                    synchronized(guard) {
                        if (requests[key] === request) {
                            if (phase == Phase.DISK && value == null && request.generation != null) {
                                request.phase = Phase.GENERATION
                            } else if (phase == Phase.DISK || !paused) {
                                // Disk hits are safe to deliver during a fling.
                                requests.remove(key)
                                request.result.complete(value)
                            }
                            // A native generation that ignored cancellation must
                            // never publish its result during a scroll.
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (failure: Exception) {
                    synchronized(guard) {
                        if (requests[key] === request) {
                            requests.remove(key)
                            request.result.completeExceptionally(failure)
                        }
                    }
                }
            }
            request.job = job
            running++
            if (phase == Phase.DISK) runningDisk++
            // Keep resource permits held until the actual job exits (including
            // uncancellable native decode), and retry generation after idle.
            job.invokeOnCompletion { cause -> synchronized(guard) {
                request.job = null
                running--
                if (phase == Phase.DISK) runningDisk--
                if (requests[key] === request) {
                    when {
                        !scope.isActive -> {
                            requests.remove(key)
                            request.result.cancel()
                        }
                        cause != null && cause !is CancellationException -> {
                            requests.remove(key)
                            request.result.completeExceptionally(cause)
                        }
                    }
                }
                pumpLocked()
            } }
            job.start()
        }
        publishLocked()
    }

    private fun statsLocked() = ThumbnailQueueStats(
        queued = requests.values.count { it.job == null }, running = running,
        shared = shared, cancelled = cancelled, rejected = rejected, peakPending = peakPending,
    )
    private var publishedStats: ThumbnailQueueStats? = null
    private fun publishLocked() {
        val stats = statsLocked()
        if (stats != publishedStats) {
            publishedStats = stats
            onStats(stats)
        }
    }
}
