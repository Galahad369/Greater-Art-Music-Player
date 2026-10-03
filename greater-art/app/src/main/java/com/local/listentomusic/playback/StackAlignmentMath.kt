package com.local.listentomusic.playback

import kotlin.math.ln
import kotlin.math.sqrt

data class StackAlignment(val offsetMs: Long, val correlation: Double, val confident: Boolean)

/** Grok's envelope proposal: positive lag means the companion has extra leading padding.
 * This measures arrangement timing, not phase or independent tempo drift. */
internal fun correlateStackEnvelopes(primary: FloatArray, companion: FloatArray): StackAlignment {
    if (minOf(primary.size, companion.size) < 150) return StackAlignment(0, 0.0, false)
    val a = primary.map { ln(1.0 + it.coerceAtLeast(0f) * 100.0) }
    val b = companion.map { ln(1.0 + it.coerceAtLeast(0f) * 100.0) }
    val limit = minOf(750, minOf(a.size, b.size) / 3)
    val scores = DoubleArray(limit * 2 + 1) { -1.0 }
    var best = -1.0
    var bestLag = 0
    for (lag in -limit..limit) {
        val start = maxOf(0, -lag)
        val end = minOf(a.size, b.size - lag)
        val count = end - start
        if (count < 150) continue
        var x = 0.0; var y = 0.0; var xx = 0.0; var yy = 0.0; var xy = 0.0
        for (i in start until end) {
            val av = a[i]; val bv = b[i + lag]
            x += av; y += bv; xx += av * av; yy += bv * bv; xy += av * bv
        }
        val variance = (xx - x * x / count) * (yy - y * y / count)
        val score = if (variance > 1e-8) (xy - x * y / count) / sqrt(variance) else -1.0
        scores[lag + limit] = score
        if (score > best) { best = score; bestLag = lag }
    }
    val alternative = scores.indices.filter { kotlin.math.abs(it - limit - bestLag) > 25 }
        .maxOfOrNull { scores[it] } ?: -1.0
    // A repeated beat or unrelated take must not silently become a guessed offset.
    val confident = best >= .35 && best - alternative >= .025 && kotlin.math.abs(bestLag) < limit
    return StackAlignment(if (confident) bestLag * 20L else 0L, best.coerceAtLeast(0.0), confident)
}

internal fun stackVoiceTarget(masterMs: Long, offsetMs: Long): Long = masterMs + offsetMs.coerceIn(-30_000L, 30_000L)

internal fun rebaseStackOffsets(offsets: List<Long>, newPrimaryOffset: Long): List<Long> =
    offsets.map { (it - newPrimaryOffset).coerceIn(-30_000L, 30_000L) }
