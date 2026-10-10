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
    val gains = FloatArray(voices.size) { i ->
        val voice = voices[i]
        if (voice.muted || (solo && !voice.solo)) 0f else voice.gain
    }
    val headroom = max(1f, gains.sum())
    val mixed = FloatArray(frames * 2)
    for (frameIndex in 0 until frames) {
        val outputFrame = frame + frameIndex
        var left = 0.0
        var right = 0.0
        for (voiceIndex in voices.indices) {
            val gain = gains[voiceIndex]
            if (gain <= 0f) continue
            val position = pcmSourceFrame(outputFrame, outputRate, voices[voiceIndex])
            if (position < 0) continue  // Negative-offset delayed entry, not a clamped read.
            val center = floor(position).toLong()
            if (position - center < 1e-9) {
                left += sample(voiceIndex, center, 0) * gain
                right += sample(voiceIndex, center, 1) * gain
            } else {
                // The stereo channels share the same 32-tap windowed-sinc kernel;
                // avoid computing trigonometric weights twice per voice/output frame.
                var leftSum = 0.0
                var rightSum = 0.0
                var weightSum = 0.0
                for (index in center - PCM_SINC_RADIUS + 1..center + PCM_SINC_RADIUS) {
                    val x = position - index
                    val sinc = if (abs(x) < 1e-12) 1.0 else sin(PI * x) / (PI * x)
                    val weight = sinc * (.5 + .5 * cos(PI * x / PCM_SINC_RADIUS))
                    leftSum += sample(voiceIndex, index, 0) * weight
                    rightSum += sample(voiceIndex, index, 1) * weight
                    weightSum += weight
                }
                if (abs(weightSum) > 1e-9) {
                    left += (leftSum / weightSum).toFloat() * gain
                    right += (rightSum / weightSum).toFloat() * gain
                }
            }
        }
        mixed[frameIndex * 2] = (left / headroom).coerceIn(-1.0, 1.0).toFloat()
        mixed[frameIndex * 2 + 1] = (right / headroom).coerceIn(-1.0, 1.0).toFloat()
    }
    return mixed
}
