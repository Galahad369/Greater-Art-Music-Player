package com.local.listentomusic.playback

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

internal data class PreparedPcmBlock(val frame: Long, val frames: Int, val samples: FloatArray,
    val prepareUs: Long = 0, val mixUs: Long = 0,
    val prepareCpuUs: Long = 0, val mixCpuUs: Long = 0)

/** At most two extra premixed blocks, capped at25ms. Slow/lower-rate runs stay serial. */
internal fun shouldPipelinePcm(rate: Int, speed: Double): Boolean =
    rate >= 96000 && PCM_BLOCK_FRAMES * 2.0 / rate / speed <= .025

/** Let already queued audio cover a short producer wait, retaining half a sink
 * buffer as guard. This does not change AudioTrack's size or source quality. */
internal fun pcmReadyWaitMs(queuedFrames: Long, rate: Int, bufferFrames: Int): Long =
    ((queuedFrames - bufferFrames / 2).coerceAtLeast(0) * 1000 / rate).coerceAtMost(50)

/** One producer, one output consumer. Arrays are owned until recycle AFTER write.
 * A cancelled channel transfer may already have transferred ownership: retire
 * the per-run pool on cancellation rather than returning an array twice. */
internal class PcmRenderPipeline(scope: CoroutineScope, dispatcher: CoroutineDispatcher,
    start: Long, end: Long, prepare: suspend (Long, Int, FloatArray) -> PreparedPcmBlock) {
    private val free = Channel<FloatArray>(3)
    private val ready = Channel<PreparedPcmBlock>(1)
    init {
        require(start >= 0 && end >= start)
        repeat(3) { check(free.trySend(FloatArray(PCM_BLOCK_FRAMES * 2)).isSuccess) }
    }
    private val producer = scope.launch(dispatcher) {
        try {
            var frame = start
            while (frame < end) {
                ensureActive()
                val samples = free.receive()
                val frames = minOf(PCM_BLOCK_FRAMES.toLong(), end - frame).toInt()
                ready.send(prepare(frame, frames, samples))
                frame += frames
            }
        } catch (cancelled: CancellationException) {
            ready.close(cancelled)
            throw cancelled
        } catch (failure: Exception) {
            ready.close(failure)
        } finally { ready.close() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun take(waitBeforePauseMs: Long = 0, onWait: () -> Unit): PreparedPcmBlock? {
        val available = ready.tryReceive()
        val budgeted = if (available.isSuccess || available.isClosed) available
            else if (waitBeforePauseMs > 0) select<ChannelResult<PreparedPcmBlock>?> {
                ready.onReceiveCatching { it }
                onTimeout(waitBeforePauseMs) { null }
            } else null
        // Atomic select does not cancel a receive after it transferred a
        // block merely because the headroom timer also expired.
        val result = budgeted ?: awaitStackPcmBlock(ready, onWait)
        result.exceptionOrNull()?.let { throw it }
        return result.getOrNull()
    }

    fun recycle(block: PreparedPcmBlock) { check(free.trySend(block.samples).isSuccess) }

    suspend fun close() {
        producer.cancelAndJoin()
        ready.cancel()
        free.cancel()
    }
}
