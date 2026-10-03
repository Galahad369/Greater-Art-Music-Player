package com.local.listentomusic.playback

import kotlin.math.ln
import kotlin.math.sqrt

data class StackAlignment(val offsetMs: Long, val correlation: Double, val confident: Boolean)

/**
 * Alignment features use the existing 20 ms loudness envelope plus a normalized
 * 12-bin pitch-class (chroma) vector for each envelope frame.
 */
internal data class StackAudioFeatures(
    val envelope: FloatArray,
    val chroma: Array<FloatArray>,
)

private fun energyCorrelation(a: DoubleArray, b: DoubleArray, lag: Int): Double {
    val start = maxOf(0, -lag)
    val end = minOf(a.size, b.size - lag)
    val count = end - start
    if (count < 150) return -1.0
    var x = 0.0
    var y = 0.0
    var xx = 0.0
    var yy = 0.0
    var xy = 0.0
    for (i in start until end) {
        val av = a[i]
        val bv = b[i + lag]
        x += av
        y += bv
        xx += av * av
        yy += bv * bv
        xy += av * bv
    }
    val variance = (xx - x * x / count) * (yy - y * y / count)
    return if (variance > 1e-8) (xy - x * y / count) / sqrt(variance) else -1.0
}

private fun harmonicCorrelation(primary: StackAudioFeatures, companion: StackAudioFeatures, lag: Int): Double {
    val aCount = minOf(primary.envelope.size, primary.chroma.size)
    val bCount = minOf(companion.envelope.size, companion.chroma.size)
    val start = maxOf(0, -lag)
    val end = minOf(aCount, bCount - lag)
    if (end - start < 75) return -1.0

    val maxA = primary.envelope.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    val maxB = companion.envelope.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    if (maxA <= 1e-6f || maxB <= 1e-6f) return -1.0
    val gateA = maxA * 0.06f
    val gateB = maxB * 0.06f

    var score = 0.0
    var frames = 0
    // Chroma changes more slowly than the 20 ms lag grid. Score every fourth
    // frame (~80 ms) while still evaluating every lag at 20 ms resolution.
    for (i in start until end step 4) {
        val j = i + lag
        if (primary.envelope[i] < gateA || companion.envelope[j] < gateB) continue
        val a = primary.chroma[i]
        val b = companion.chroma[j]
        if (a.size < 12 || b.size < 12) continue
        var dot = 0.0
        var aa = 0.0
        var bb = 0.0
        for (pitch in 0 until 12) {
            val av = a[pitch].toDouble()
            val bv = b[pitch].toDouble()
            dot += av * bv
            aa += av * av
            bb += bv * bv
        }
        if (aa <= 1e-8 || bb <= 1e-8) continue
        score += dot / sqrt(aa * bb)
        frames++
    }
    return if (frames >= 20) score / frames else -1.0
}

/** Grok's envelope proposal retained as the fallback for files without spectral features.
 * Positive lag means the companion has extra leading padding. */
internal fun correlateStackEnvelopes(primary: FloatArray, companion: FloatArray): StackAlignment {
    if (minOf(primary.size, companion.size) < 150) return StackAlignment(0, 0.0, false)
    val a = DoubleArray(primary.size) { ln(1.0 + primary[it].coerceAtLeast(0f) * 100.0) }
    val b = DoubleArray(companion.size) { ln(1.0 + companion[it].coerceAtLeast(0f) * 100.0) }
    val limit = minOf(750, minOf(a.size, b.size) / 3)
    val scores = DoubleArray(limit * 2 + 1) { -1.0 }
    var best = -1.0
    var bestLag = 0
    for (lag in -limit..limit) {
        val score = energyCorrelation(a, b, lag)
        scores[lag + limit] = score
        if (score > best) {
            best = score
            bestLag = lag
        }
    }
    val alternative = scores.indices
        .filter { kotlin.math.abs(it - limit - bestLag) > 25 }
        .maxOfOrNull { scores[it] } ?: -1.0
    val confident = best >= .35 && best - alternative >= .025 && kotlin.math.abs(bestLag) < limit
    return StackAlignment(if (confident) bestLag * 20L else 0L, best.coerceAtLeast(0.0), confident)
}

/**
 * MVP harmonic fingerprint alignment. Loudness timing and pitch-class similarity are
 * fused per lag; a strong result must have either meaningful envelope evidence or
 * meaningful chroma evidence, and still pass the existing distinct-peak rejection.
 */
internal fun correlateStackFeatures(primary: StackAudioFeatures, companion: StackAudioFeatures, checkActive: () -> Unit = {}): StackAlignment {
    if (minOf(primary.envelope.size, companion.envelope.size) < 150) {
        return StackAlignment(0, 0.0, false)
    }
    val a = DoubleArray(primary.envelope.size) { ln(1.0 + primary.envelope[it].coerceAtLeast(0f) * 100.0) }
    val b = DoubleArray(companion.envelope.size) { ln(1.0 + companion.envelope[it].coerceAtLeast(0f) * 100.0) }
    val limit = minOf(750, minOf(a.size, b.size) / 3)
    val scores = DoubleArray(limit * 2 + 1) { -1.0 }
    var best = -1.0
    var bestLag = 0
    var bestEnergy = -1.0
    var bestHarmonic = -1.0

    for (lag in -limit..limit) {
        if (lag % 16 == 0) checkActive()
        val energy = energyCorrelation(a, b, lag)
        val harmonic = harmonicCorrelation(primary, companion, lag)
        val score = when {
            harmonic < 0.0 -> energy
            energy < 0.0 -> harmonic * 0.72
            else -> energy.coerceAtLeast(0.0) * 0.45 + harmonic.coerceAtLeast(0.0) * 0.55
        }
        scores[lag + limit] = score
        if (score > best) {
            best = score
            bestLag = lag
            bestEnergy = energy
            bestHarmonic = harmonic
        }
    }

    val alternative = scores.indices
        .filter { kotlin.math.abs(it - limit - bestLag) > 25 }
        .maxOfOrNull { scores[it] } ?: -1.0
    val evidence = bestEnergy >= .35 || bestHarmonic >= .55
    val confident = best >= .40 && evidence && best - alternative >= .025 && kotlin.math.abs(bestLag) < limit
    return StackAlignment(if (confident) bestLag * 20L else 0L, best.coerceAtLeast(0.0), confident)
}

internal fun stackVoiceTarget(masterMs: Long, offsetMs: Long): Long =
    masterMs + offsetMs.coerceIn(-30_000L, 30_000L)

internal fun rebaseStackOffsets(offsets: List<Long>, newPrimaryOffset: Long): List<Long> =
    offsets.map { (it - newPrimaryOffset).coerceIn(-30_000L, 30_000L) }
