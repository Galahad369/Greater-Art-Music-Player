package com.local.listentomusic.playback

import kotlin.math.abs

/** Exact same-rate windows. One scratch buffer belongs to one render worker. */
internal class PcmWindowMixer {
    private val accumulation = DoubleArray(PCM_BLOCK_FRAMES * 2)

    fun mix(frame: Long, outputRate: Int, voices: List<PcmVoice>, frames: Int,
        mixed: FloatArray, gains: FloatArray, windows: Array<FloatArray>, starts: LongArray) {
        if (voices.any { it.rate != outputRate }) {
            mixPcmWindowBlockInto(frame, outputRate, voices, frames, mixed, gains, windows, starts)
            return
        }
        require(frame >= 0 && outputRate in 8000..192000 && voices.size in 1..8 && frames in 1..PCM_BLOCK_FRAMES)
        require(mixed.size >= frames * 2 && gains.size >= voices.size)
        accumulation.fill(0.0, 0, frames * 2)
        var solo = false
        for (i in voices.indices) if (voices[i].solo && !voices[i].muted) solo = true
        var audible = 0
        for (i in voices.indices) {
            val voice = voices[i]
            gains[i] = if (voice.muted || solo && !voice.solo) 0f else voice.gain
            if (!voice.muted && (!solo || voice.solo)) audible++
        }
        // Voice order and Double sums are unchanged; only addressing moves out
        // of the tap loop. Do not replace the32-tap weights or normalize gains.
        for (i in voices.indices) {
            val gain = gains[i]
            if (gain <= 0f) continue
            val voice = voices[i]
            val window = windows[i]
            val center = frame + voice.wholeOffsetFrame
            var index = ((center - starts[i]) * 2).toInt()
            if (voice.integralOffset) {
                for (out in 0 until frames) {
                    if (center + out >= 0) {
                        accumulation[out * 2] += window[index] * gain
                        accumulation[out * 2 + 1] += window[index + 1] * gain
                    }
                    index += 2
                }
            } else if (abs(voice.delayWeightSum) > 1e-9) {
                index -= (PCM_SINC_RADIUS - 1) * 2
                val weights = voice.delayWeights
                for (out in 0 until frames) {
                    if (center + out >= 0) {
                        var left = 0.0
                        var right = 0.0
                        var cursor = index
                        for (tap in weights.indices) {
                            val weight = weights[tap]
                            left += window[cursor] * weight
                            right += window[cursor + 1] * weight
                            cursor += 2
                        }
                        accumulation[out * 2] += (left / voice.delayWeightSum).toFloat() * gain
                        accumulation[out * 2 + 1] += (right / voice.delayWeightSum).toFloat() * gain
                    }
                    index += 2
                }
            }
        }
        val headroom = audible.coerceAtLeast(1).toFloat()
        for (i in 0 until frames * 2) mixed[i] = (accumulation[i] / headroom).coerceIn(-1.0, 1.0).toFloat()
    }
}
