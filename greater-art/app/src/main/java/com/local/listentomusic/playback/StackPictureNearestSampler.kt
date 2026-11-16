package com.local.listentomusic.playback

/**
 * Select existing pictures nearest a window-local target grid. Never retime them.
 * One previous luma buffer supplies look-ahead without holding a codec buffer.
 * The consumer must copy pixels if it retains them beyond the callback.
 */
internal class StackPictureNearestSampler(
    private val fps: Int,
    private val endTimeUs: Long,
    private val onFrame: (Long, FloatArray) -> Unit,
) {
    private var started = false
    private var originUs = 0L
    private var tick = 0L
    private var previousTimeUs = 0L
    private lateinit var previousPixels: FloatArray
    private var lastEmittedUs = Long.MIN_VALUE

    private fun targetUs(): Long = originUs + (tick * 1_000_000L + fps / 2) / fps

    private fun emit(timeUs: Long, pixels: FloatArray) {
        if (timeUs == lastEmittedUs) return
        onFrame(timeUs, pixels)
        lastEmittedUs = timeUs
    }

    fun offer(timeUs: Long, pixels: FloatArray) {
        if (!started) {
            originUs = timeUs
            previousTimeUs = timeUs
            previousPixels = pixels.copyOf()
            started = true
        }
        while (targetUs() <= timeUs) {
            val target = targetUs()
            if (target - previousTimeUs <= timeUs - target) emit(previousTimeUs, previousPixels)
            else emit(timeUs, pixels)
            tick++
        }
        previousTimeUs = timeUs
        pixels.copyInto(previousPixels)
    }

    fun finish() {
        if (started && targetUs() < endTimeUs) emit(previousTimeUs, previousPixels)
    }
}
