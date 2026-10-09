package com.local.listentomusic.playback

import kotlin.math.*

internal const val PCM_BLOCK_FRAMES = 256
internal const val PCM_SINC_RADIUS = 16

/** AudioTrack exposes a wrapping unsigned 32-bit head, not an unbounded timeline. */
internal class PcmFrameClock(private val base: Long) {
    private var previous = 0L
    private var elapsed = 0L
    @Synchronized fun position(head: Int): Long {
        val current = head.toLong() and 0xffffffffL
        elapsed += (current - previous) and 0xffffffffL
        previous = current
        return base + elapsed
    }
}

internal data class PcmVoice(val rate: Int, val offsetUs: Double = 0.0,
    val gain: Float = 1f, val muted: Boolean = false, val solo: Boolean = false) {
    init { require(rate in 8000..192000 && offsetUs.isFinite() && abs(offsetUs) <= 30_000_000 && gain.isFinite() && gain in 0f..1f) }
}

/** One output frame determines EVERY voice position. No wall clock or per-voice timer. */
internal fun pcmSourceFrame(outputFrame: Long, outputRate: Int, voice: PcmVoice): Double =
    outputFrame.toDouble() * voice.rate / outputRate + voice.offsetUs * voice.rate / 1_000_000

/** Native-rate integer positions are exact; rate conversion uses a bounded windowed sinc. */
internal inline fun pcmInterpolated(position: Double, sample: (Long) -> Float): Float {
    val center = floor(position).toLong()
    val fraction = position - center
    if (fraction < 1e-9) return sample(center)
    var sum = 0.0; var weightSum = 0.0
    for (index in center - PCM_SINC_RADIUS + 1..center + PCM_SINC_RADIUS) {
        val x = position - index
        val sinc = if (abs(x) < 1e-12) 1.0 else sin(PI * x) / (PI * x)
        val weight = sinc * (.5 + .5 * cos(PI * x / PCM_SINC_RADIUS))
        sum += sample(index) * weight; weightSum += weight
    }
    return if (abs(weightSum) > 1e-9) (sum / weightSum).toFloat() else 0f
}

/** Caller prepares each lane through the final source position + sinc radius. */
internal inline fun mixPcmBlock(frame: Long, outputRate: Int, voices: List<PcmVoice>,
    frames: Int = PCM_BLOCK_FRAMES, crossinline sample: (Int, Long, Int) -> Float): FloatArray {
    require(frame >= 0 && outputRate in 8000..192000 && voices.size in 1..8 && frames in 1..PCM_BLOCK_FRAMES)
    val solo = voices.any { it.solo && !it.muted }
    val weights = voices.map { if (it.muted || solo && !it.solo) 0f else it.gain }
    // Worst-case coherent sum stays within float full scale, including identical tracks.
    val headroom = max(1f, weights.sum())
    return FloatArray(frames * 2) { i ->
        val outputFrame = frame + i / 2
        val channel = i % 2
        var sum = 0.0
        voices.forEachIndexed { voiceIndex, voice ->
            if (weights[voiceIndex] > 0f) {
                val source = pcmSourceFrame(outputFrame, outputRate, voice)
                // Negative mapping means this take has not entered, not wrap/clamp to frame zero.
                if (source >= 0) sum += pcmInterpolated(source) { sample(voiceIndex, it, channel) } * weights[voiceIndex]
            }
        }
        (sum / headroom).coerceIn(-1.0, 1.0).toFloat()
    }
}
