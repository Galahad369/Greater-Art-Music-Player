package com.local.listentomusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One float/stereo processor after the shared mix, never one independent clock per singer.
 * The caller drains every returned buffer before the next call. Unity playback bypasses this
 * stage entirely; changing parameters creates a fresh run at the audible media position. */
internal class FloatPcmTempo(rate: Int, speed: Float, pitch: Float) : AutoCloseable {
    private val processor = SonicAudioProcessor()
    val active: Boolean
    val clockSpeed: Double
    private val input: ByteBuffer
    private var pending = AudioProcessor.EMPTY_BUFFER
    init {
        require(speed.isFinite() && speed > 0 && pitch.isFinite() && pitch > 0)
        processor.setSpeed(speed)
        processor.setPitch(pitch)
        val format = processor.configure(AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_FLOAT))
        check(format.sampleRate == rate && format.channelCount == 2 && format.encoding == C.ENCODING_PCM_FLOAT)
        active = processor.isActive
        clockSpeed = if (active) speed.toDouble() else 1.0
        input = if (active) ByteBuffer.allocateDirect(PCM_BLOCK_FRAMES * 8).order(ByteOrder.nativeOrder())
            else AudioProcessor.EMPTY_BUFFER
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
    }

    fun queue(samples: FloatArray, frames: Int): ByteBuffer {
        check(active && !pending.hasRemaining()) { "Drain float tempo output before queueing input" }
        require(frames in 1..PCM_BLOCK_FRAMES && samples.size >= frames * 2)
        input.clear()
        for (index in 0 until frames * 2) input.putFloat(samples[index])
        input.flip()
        processor.queueInput(input)
        check(!input.hasRemaining())
        return drain()
    }

    fun end() {
        check(active && !pending.hasRemaining())
        processor.queueEndOfStream()
    }

    fun drain(): ByteBuffer {
        check(!pending.hasRemaining()) { "Float tempo output was not drained" }
        return processor.output.also { pending = it }
    }

    val ended: Boolean get() = processor.isEnded
    override fun close() = processor.reset()
}
