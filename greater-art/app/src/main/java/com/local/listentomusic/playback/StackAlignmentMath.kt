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
    val musicEnvelope: FloatArray = envelope,
    /** Vocal-resistant transient fingerprint sampled much finer than the 20 ms coarse grid. */
    val fineSignal: FloatArray = FloatArray(0),
    val fineSampleRateHz: Int = 3_200,
    /**
     * The same file analysed from the plain mono mix. Present only when [musicEnvelope] was
     * built from the stereo side signal; otherwise this view already IS the mono view.
     */
    val monoView: StackAudioFeatures? = null,
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

    val maxA = primary.musicEnvelope.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    val maxB = companion.musicEnvelope.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    if (maxA <= 1e-6f || maxB <= 1e-6f) return -1.0
    val gateA = maxA * 0.06f
    val gateB = maxB * 0.06f

    var score = 0.0
    var frames = 0
    // Chroma changes more slowly than the 20 ms lag grid. Score every fourth
    // frame (~80 ms) while still evaluating every lag at 20 ms resolution.
    for (i in start until end step 4) {
        val j = i + lag
        if (primary.musicEnvelope[i] < gateA || companion.musicEnvelope[j] < gateB) continue
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
    val a = DoubleArray(primary.musicEnvelope.size) { ln(1.0 + primary.musicEnvelope[it].coerceAtLeast(0f) * 100.0) }
    val b = DoubleArray(companion.musicEnvelope.size) { ln(1.0 + companion.musicEnvelope[it].coerceAtLeast(0f) * 100.0) }
    // The decoder supplies an accompaniment-biased envelope when stereo side information
    // is usable. Onset novelty then follows backing-track rhythm instead of vocal phrasing.
    val onsetA = stackOnsetNovelty(a)
    val onsetB = stackOnsetNovelty(b)
    val limit = minOf(750, minOf(a.size, b.size) / 3)
    val scores = DoubleArray(limit * 2 + 1) { -1.0 }
    var best = -1.0
    var bestLag = 0
    var bestEnergy = -1.0
    var bestHarmonic = -1.0
    var bestOnset = -1.0

    for (lag in -limit..limit) {
        if (lag % 16 == 0) checkActive()
        val energy = energyCorrelation(a, b, lag)
        val onset = energyCorrelation(onsetA, onsetB, lag)
        val harmonic = harmonicCorrelation(primary, companion, lag)
        val score = stackFusedScore(energy, onset, harmonic)
        scores[lag + limit] = score
        if (score > best) {
            best = score
            bestLag = lag
            bestEnergy = energy
            bestHarmonic = harmonic
            bestOnset = onset
        }
    }

    val alternative = scores.indices
        .filter { kotlin.math.abs(it - limit - bestLag) > 25 }
        .maxOfOrNull { scores[it] } ?: -1.0
    val evidence = bestEnergy >= .35 || bestHarmonic >= .55 || bestOnset >= .30
    val confident = best >= .40 && evidence && best - alternative >= .025 && kotlin.math.abs(bestLag) < limit
    if (!confident) return StackAlignment(0L, best.coerceAtLeast(0.0), false)
    val refined = stackRefinePeak(scores, bestLag + limit)
    val coarse = StackAlignment(kotlin.math.round((bestLag + refined) * 20.0).toLong(), best.coerceAtLeast(0.0), true)
    return stackRefineMusicOffset(
        coarse = coarse,
        primaryFine = primary.fineSignal,
        companionFine = companion.fineSignal,
        sampleRateHz = primary.fineSampleRateHz.takeIf { it == companion.fineSampleRateHz } ?: 0,
        checkActive = checkActive,
    )
}

/** Side-view and mono-view answers further apart than this are treated as disagreement. */
internal const val STACK_VIEW_AGREEMENT_MS = 40L

/**
 * Always compare the two files on the SAME view.
 *
 * Each file independently decides whether to use its stereo-side signal as the music anchor. If
 * only one of them did, correlating one file's side features against the other's mono features
 * compares different signals and can return a confident but wrong lag. So:
 *  - both used the side view: trust it only if the mono view independently agrees,
 *  - only one used it: compare both files as mono,
 *  - neither: unchanged, see [correlateStackFeatures].
 */
internal fun correlateStackFeaturesOnCommonView(
    primary: StackAudioFeatures,
    companion: StackAudioFeatures,
    checkActive: () -> Unit = {},
): StackAlignment {
    val primaryMono = primary.monoView
    val companionMono = companion.monoView
    return when {
        primaryMono != null && companionMono != null -> {
            val side = correlateStackFeatures(primary, companion, checkActive)
            if (!side.confident) side
            else stackAgreeOnViews(side, correlateStackFeatures(primaryMono, companionMono, checkActive))
        }
        primaryMono != null || companionMono != null ->
            correlateStackFeatures(primaryMono ?: primary, companionMono ?: companion, checkActive)
        else -> correlateStackFeatures(primary, companion, checkActive)
    }
}

internal fun stackAgreeOnViews(side: StackAlignment, mono: StackAlignment): StackAlignment =
    if (
        side.confident &&
        mono.confident &&
        kotlin.math.abs(side.offsetMs - mono.offsetMs) <= STACK_VIEW_AGREEMENT_MS
    ) {
        side
    } else {
        StackAlignment(0L, minOf(side.correlation, mono.correlation), false)
    }

internal fun stackOnsetNovelty(logEnvelope: DoubleArray): DoubleArray =
    DoubleArray(logEnvelope.size) { index ->
        if (index == 0) 0.0 else (logEnvelope[index] - logEnvelope[index - 1]).coerceAtLeast(0.0)
    }

internal fun stackFusedScore(energy: Double, onset: Double, harmonic: Double): Double = when {
    harmonic < 0.0 && onset < 0.0 -> energy
    harmonic < 0.0 -> energy.coerceAtLeast(0.0) * 0.35 + onset.coerceAtLeast(0.0) * 0.65
    energy < 0.0 && onset < 0.0 -> harmonic * 0.72
    else -> energy.coerceAtLeast(0.0) * 0.15 + onset.coerceAtLeast(0.0) * 0.30 + harmonic.coerceAtLeast(0.0) * 0.55
}

internal fun stackPreferStereoSideSignal(fullPower: Double, sidePower: Double, stereo: Boolean): Boolean =
    stereo && fullPower > 1e-8 && sidePower.isFinite() && sidePower / fullPower >= 0.015

/**
 * Parabolic interpolation around the winning lag for sub-frame (< 20 ms) precision.
 * Returns a fractional lag correction in [-0.5, 0.5]; 0 when the peak is flat or at an edge.
 */
internal fun stackRefinePeak(scores: DoubleArray, peakIndex: Int): Double {
    if (peakIndex <= 0 || peakIndex >= scores.size - 1) return 0.0
    val left = scores[peakIndex - 1]
    val center = scores[peakIndex]
    val right = scores[peakIndex + 1]
    if (left < -0.5 || right < -0.5) return 0.0
    val curvature = left - 2.0 * center + right
    if (curvature >= -1e-9) return 0.0
    val shift = 0.5 * (left - right) / curvature
    return if (shift.isFinite()) shift.coerceIn(-0.5, 0.5) else 0.0
}


internal const val STACK_FINE_SEARCH_MS = 30
internal const val STACK_FINE_WINDOW_MS = 650
internal const val STACK_FINE_MAX_ANCHORS = 6
internal const val STACK_FINE_MIN_ANCHORS = 3
internal const val STACK_FINE_CONSENSUS_MS = 4

/**
 * Build an attack-weighted signal for fine alignment. A fast envelope minus a slower
 * baseline suppresses sustained voice/notes and emphasizes shared backing-track attacks.
 * The output is peak-normalized so gain/mastering differences do not affect thresholds.
 */
internal fun stackFineTransientSignal(samples: FloatArray): FloatArray {
    if (samples.isEmpty()) return samples
    val out = FloatArray(samples.size)
    var fast = 0.0
    var slow = 0.0
    var peak = 0.0
    for (i in samples.indices) {
        val magnitude = kotlin.math.abs(samples[i].toDouble()).coerceAtMost(4.0)
        fast += (magnitude - fast) * 0.35
        slow += (magnitude - slow) * 0.035
        val transient = (fast - slow).coerceAtLeast(0.0)
        out[i] = transient.toFloat()
        if (transient > peak) peak = transient
    }
    if (peak > 1e-8) {
        val scale = (1.0 / peak).toFloat()
        for (i in out.indices) out[i] *= scale
    }
    return out
}

private fun stackFineWindowCorrelation(
    primary: FloatArray,
    companion: FloatArray,
    start: Int,
    lag: Int,
    window: Int,
): Double {
    val companionStart = start + lag
    if (start < 0 || companionStart < 0 ||
        start + window > primary.size || companionStart + window > companion.size
    ) return -1.0

    var x = 0.0
    var y = 0.0
    var xx = 0.0
    var yy = 0.0
    var xy = 0.0
    for (k in 0 until window) {
        val av = primary[start + k].toDouble()
        val bv = companion[companionStart + k].toDouble()
        x += av
        y += bv
        xx += av * av
        yy += bv * bv
        xy += av * bv
    }
    val variance = (xx - x * x / window) * (yy - y * y / window)
    return if (variance > 1e-10) (xy - x * y / window) / sqrt(variance) else -1.0
}

/**
 * Second-stage alignment around a confident coarse result.
 *
 * The 20 ms matcher finds the arrangement. This searches only ±30 ms at the cached
 * fine-signal rate, using up to six separated transient-rich anchors. A fine result is
 * accepted only when at least three anchors agree within ~4 ms; otherwise the coarse
 * result is returned unchanged. This makes refinement fail-safe rather than mandatory.
 */
internal fun stackRefineMusicOffset(
    coarse: StackAlignment,
    primaryFine: FloatArray,
    companionFine: FloatArray,
    sampleRateHz: Int,
    checkActive: () -> Unit = {},
): StackAlignment {
    if (!coarse.confident || sampleRateHz !in 1_000..8_000) return coarse
    if (minOf(primaryFine.size, companionFine.size) < sampleRateHz * 3) return coarse

    val radius = maxOf(1, sampleRateHz * STACK_FINE_SEARCH_MS / 1_000)
    val window = maxOf(256, sampleRateHz * STACK_FINE_WINDOW_MS / 1_000)
    val coarseLag = kotlin.math.round(coarse.offsetMs * sampleRateHz / 1_000.0).toInt()
    val minLag = coarseLag - radius
    val maxLag = coarseLag + radius
    val minStart = maxOf(0, -minLag)
    val maxStart = minOf(primaryFine.size - window, companionFine.size - window - maxLag)
    if (maxStart <= minStart) return coarse

    val step = maxOf(window / 2, 1)
    val scored = ArrayList<Pair<Int, Double>>()
    var start = minStart
    while (start <= maxStart) {
        var pa = 0.0
        var pb = 0.0
        val companionStart = start + coarseLag
        if (companionStart >= 0 && companionStart + window <= companionFine.size) {
            for (k in 0 until window) {
                val a = primaryFine[start + k].toDouble()
                val b = companionFine[companionStart + k].toDouble()
                pa += a * a
                pb += b * b
            }
            val joint = sqrt(pa * pb)
            if (joint > 1e-6) scored += start to joint
        }
        start += step
    }
    if (scored.size < STACK_FINE_MIN_ANCHORS) return coarse

    val separation = window * 2
    val anchors = ArrayList<Int>(STACK_FINE_MAX_ANCHORS)
    for ((candidate, _) in scored.sortedByDescending { it.second }) {
        if (anchors.all { kotlin.math.abs(it - candidate) >= separation }) {
            anchors += candidate
            if (anchors.size == STACK_FINE_MAX_ANCHORS) break
        }
    }
    if (anchors.size < STACK_FINE_MIN_ANCHORS) return coarse

    data class Candidate(val anchor: Int, val lag: Int, val score: Double)
    val candidates = ArrayList<Candidate>(anchors.size)
    for ((anchorIndex, anchor) in anchors.withIndex()) {
        if (anchorIndex % 2 == 0) checkActive()
        var bestLag = coarseLag
        var bestScore = -1.0
        for (lag in minLag..maxLag) {
            val score = stackFineWindowCorrelation(primaryFine, companionFine, anchor, lag, window)
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        if (bestScore >= 0.18) candidates += Candidate(anchor, bestLag, bestScore)
    }
    if (candidates.size < STACK_FINE_MIN_ANCHORS) return coarse

    val sortedLags = candidates.map { it.lag }.sorted()
    val medianLag = sortedLags[sortedLags.size / 2]
    val tolerance = maxOf(2, sampleRateHz * STACK_FINE_CONSENSUS_MS / 1_000)
    val inliers = candidates.filter { kotlin.math.abs(it.lag - medianLag) <= tolerance }
    if (inliers.size < STACK_FINE_MIN_ANCHORS || inliers.size * 3 < candidates.size * 2) return coarse

    var bestLag = medianLag
    var bestScore = -1.0
    val consensusMin = maxOf(minLag, medianLag - tolerance)
    val consensusMax = minOf(maxLag, medianLag + tolerance)
    for (lag in consensusMin..consensusMax) {
        var total = 0.0
        for (candidate in inliers) {
            total += stackFineWindowCorrelation(primaryFine, companionFine, candidate.anchor, lag, window)
        }
        val score = total / inliers.size
        if (score > bestScore) {
            bestScore = score
            bestLag = lag
        }
    }
    if (bestScore < 0.20) return coarse

    val refinedMs = kotlin.math.round(bestLag * 1_000.0 / sampleRateHz).toLong()
    if (kotlin.math.abs(refinedMs - coarse.offsetMs) > STACK_FINE_SEARCH_MS) return coarse
    return coarse.copy(offsetMs = refinedMs)
}

internal fun stackVoiceTarget(masterMs: Long, offsetMs: Long): Long =
    masterMs + offsetMs.coerceIn(-30_000L, 30_000L)

internal fun rebaseStackOffsets(offsets: List<Long>, newPrimaryOffset: Long): List<Long> =
    offsets.map { (it - newPrimaryOffset).coerceIn(-30_000L, 30_000L) }
