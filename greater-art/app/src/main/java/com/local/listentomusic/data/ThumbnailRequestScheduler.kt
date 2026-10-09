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
    private class Request<V : Any>(val load: suspend () -> V?, val prefetch: Boolean) {
        val result = CompletableDeferred<V?>()
        var consumers = 1
        var job: Job? = null
    }

    private val guard = Any()
    private val requests = linkedMapOf<K, Request<V>>()
    private val viewports = mutableMapOf<String, Set<K>>()
    private var paused = false
    private var running = 0
    private var shared = 0
    private var cancelled = 0
    private var rejected = 0
    private var peakPending = 0

    init { require(workers > 0 && capacity >= workers) }

    suspend fun load(key: K, prefetch: Boolean = false, loader: suspend () -> V?): V? {
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
                Request(loader, prefetch).also {
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
            request.job?.takeIf { it.isActive }?.let { cancelled++; it.cancel() }
        }
        pumpLocked()
    }

    fun setViewport(holder: String, visible: Set<K>) = synchronized(guard) {
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
        while (scope.isActive && !paused && running < workers) {
            val next = requests.entries.filter { it.value.job == null }
                .minByOrNull { (key, request) ->
                    when {
                        viewports.values.any { key in it } -> 0
                        !request.prefetch -> 1
                        else -> 2
                    }
                } ?: break
            val key = next.key
            val request = next.value
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val value = request.load()
                    coroutineContext.ensureActive()
                    synchronized(guard) {
                        if (requests[key] === request && !paused) {
                            requests.remove(key)
                            request.result.complete(value)
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
            // Handles cancellation before the LAZY coroutine even enters its body.
            job.invokeOnCompletion { cause -> synchronized(guard) {
                request.job = null
                running--
                if (requests[key] === request && (cause !is CancellationException || !scope.isActive)) {
                    requests.remove(key)
                    if (cause == null) request.result.complete(null)
                    else request.result.completeExceptionally(cause)
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
    private fun publishLocked() { onStats(statsLocked()) }
}
