package com.local.listentomusic.playback

import kotlin.math.*

internal const val STACK_INSTRUMENT_HOP_MS = 20
internal const val STACK_INSTRUMENT_RATE = 16_000
internal const val STACK_REGION_MS = 6_000L
internal const val STACK_SEARCH_MS = 15_000L

/** Coordinates are relative to the clipped track, never to the untrimmed source. */
internal data class StackInstrumentRegion(
    val startMs: Long,
    val percussion: FloatArray,
    val bass: FloatArray,
    val chroma: Array<FloatArray>,
) {
    init {
        require(percussion.size in 150..1800 && bass.size == percussion.size && chroma.size == bass.size)
        require(percussion.all { it.isFinite() } && bass.all { it.isFinite() })
        require(chroma.all { it.size == 12 && it.all(Float::isFinite) })
    }
}

internal data class StackInstrumentAnchor(
    val primaryMs: Double, val companionMs: Double, val score: Double, val rotation: Int,
)

internal fun stackRegionStarts(durationMs: Long): List<Long> {
    if (durationMs < STACK_REGION_MS * 3) return emptyList()
    val last = durationMs - STACK_REGION_MS
    val count = (durationMs / STACK_REGION_MS).coerceIn(3, 6).toInt()
    return List(count) { last * it / (count - 1) }
}

/** In-place radix-2 FFT, with scratch allocated once per bounded region. */
private fun fft(real: DoubleArray, imag: DoubleArray) {
    val n = real.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
        j = j xor bit
        if (i < j) {
            val r = real[i]; real[i] = real[j]; real[j] = r
            val v = imag[i]; imag[i] = imag[j]; imag[j] = v
        }
    }
    var length = 2
    while (length <= n) {
        val angle = -2 * PI / length
        val wrStep = cos(angle); val wiStep = sin(angle)
        for (start in 0 until n step length) {
            var wr = 1.0; var wi = 0.0
            for (k in 0 until length / 2) {
                val a = start + k; val b = a + length / 2
                val br = wr * real[b] - wi * imag[b]
                val bi = wr * imag[b] + wi * real[b]
                real[b] = real[a] - br; imag[b] = imag[a] - bi
                real[a] += br; imag[a] += bi
                val next = wr * wrStep - wi * wiStep
                wi = wr * wiStep + wi * wrStep; wr = next
            }
        }
        length *= 2
    }
}

/** Bounded median-mask HPSS emphasizes attacks, NOT an isolated instrumental stem. */
internal fun stackInstrumentFeatures(samples: FloatArray, startMs: Long, checkActive: () -> Unit = {}): StackInstrumentRegion {
    require(samples.size in STACK_INSTRUMENT_RATE * 3..STACK_INSTRUMENT_RATE * 36)
    val fftSize = 1024
    val hop = STACK_INSTRUMENT_RATE * STACK_INSTRUMENT_HOP_MS / 1000
    val frames = samples.size / hop
    val spectrum = Array(frames) { FloatArray(fftSize / 2) }
    val real = DoubleArray(fftSize); val imaginary = DoubleArray(fftSize)
    val window = DoubleArray(fftSize) { .5 - .5 * cos(2 * PI * it / (fftSize - 1)) }
    for (frame in 0 until frames) {
        checkActive()
        for (i in 0 until fftSize) {
            real[i] = (samples.getOrElse(frame * hop + i) { 0f }.takeIf { it.isFinite() } ?: 0f) * window[i]
            imaginary[i] = 0.0
        }
        fft(real, imaginary)
        for (bin in 1 until fftSize / 2) spectrum[frame][bin] = hypot(real[bin], imaginary[bin]).toFloat()
    }
    val percussion = FloatArray(frames); val bass = FloatArray(frames)
    val chroma = Array(frames) { FloatArray(12) }
    val previous = DoubleArray(fftSize / 2)
    val median = FloatArray(7)
    for (frame in 0 until frames) {
        checkActive()
        var flux = 0.0; var bassFlux = 0.0
        for (bin in 3 until fftSize / 2 - 3) {
            for (k in -2..2) median[k + 2] = spectrum[(frame + k).coerceIn(0, frames - 1)][bin]
            median.sort(0, 5)
            val harmonicMedian = median[2].toDouble()
            for (k in -3..3) median[k + 3] = spectrum[frame][bin + k]
            median.sort()
            val percussiveMedian = median[3].toDouble()
            val magnitude = spectrum[frame][bin].toDouble()
            val mask = percussiveMedian.pow(2) / (harmonicMedian.pow(2) + percussiveMedian.pow(2) + 1e-12)
            val attack = magnitude * mask
            val frequency = bin * STACK_INSTRUMENT_RATE.toDouble() / fftSize
            val novelty = (ln1p(attack) - previous[bin]).coerceAtLeast(0.0)
            previous[bin] = ln1p(attack)
            if (frequency in 600.0..6000.0) flux += novelty
            if (frequency in 45.0..260.0) bassFlux += novelty
            if (frequency in 65.0..4000.0) {
                val pitch = ((69 + 12 * log2(frequency / 440.0)).roundToInt() % 12 + 12) % 12
                chroma[frame][pitch] += (magnitude * (1 - mask)).toFloat()
            }
        }
        percussion[frame] = flux.toFloat(); bass[frame] = bassFlux.toFloat()
    }
    // Remove a static spectral floor; sustained singer timbre must not create a high match.
    for (pitch in 0 until 12) {
        val mean = chroma.sumOf { ln1p(it[pitch].toDouble()) } / frames
        for (frame in 0 until frames) chroma[frame][pitch] = (ln1p(chroma[frame][pitch].toDouble()) - mean).toFloat()
    }
    return StackInstrumentRegion(startMs, percussion, bass, chroma)
}

private fun regionCorrelation(a: FloatArray, b: FloatArray, lag: Int): Double {
    if (lag < 0 || lag + a.size > b.size) return -1.0
    var x = 0.0; var y = 0.0; var xx = 0.0; var yy = 0.0; var xy = 0.0
    for (i in a.indices) {
        val av = a[i].toDouble(); val bv = b[i + lag].toDouble()
        x += av; y += bv; xx += av * av; yy += bv * bv; xy += av * bv
    }
    val variance = (xx - x * x / a.size) * (yy - y * y / a.size)
    return if (variance > 1e-12) (xy - x * y / a.size) / sqrt(variance) else -1.0
}

/** Independent local match. Competing repeated-section peaks cause abstention. */
internal fun stackMatchInstrumentRegion(
    primary: StackInstrumentRegion, companion: StackInstrumentRegion, checkActive: () -> Unit = {},
): StackInstrumentAnchor? {
    val candidates = stackInstrumentCandidates(primary, companion, checkActive = checkActive)
    val best = candidates.firstOrNull() ?: return null
    if (candidates.drop(1).any { best.score - it.score < .06 }) return null
    return best
}

/** Retain local alternatives for distributed agreement instead of discarding a repeated beat. */
internal fun stackInstrumentCandidates(
    primary: StackInstrumentRegion, companion: StackInstrumentRegion, minimumScore: Double = .50,
    checkActive: () -> Unit = {},
): List<StackInstrumentAnchor> {
    require(minimumScore in .4..1.0)
    val limit = companion.percussion.size - primary.percussion.size
    if (limit < 0) return emptyList()
    fun score(lag: Int): Pair<Double, Int> {
        val percussive = regionCorrelation(primary.percussion, companion.percussion, lag)
        val bass = regionCorrelation(primary.bass, companion.bass, lag)
        if (percussive < .30 && bass < .40) return -1.0 to 0
        var bestHarmonic = -1.0; var bestRotation = 0
        for (rotation in 0 until 12) {
            var dot = 0.0; var xx = 0.0; var yy = 0.0
            for (i in primary.chroma.indices step 4) for (p in 0 until 12) {
                val a = primary.chroma[i][p].toDouble()
                val b = companion.chroma[i + lag][(p + rotation) % 12].toDouble()
                dot += a * b; xx += a * a; yy += b * b
            }
            val harmonic = if (xx * yy > 1e-12) dot / sqrt(xx * yy) else 0.0
            if (harmonic > bestHarmonic) { bestHarmonic = harmonic; bestRotation = rotation }
        }
        return (.50 * max(0.0, percussive) + .35 * max(0.0, bass) + .15 * max(0.0, bestHarmonic)) to
            if (bestHarmonic >= .55) bestRotation else -1
    }
    val candidates = ArrayList<Triple<Int, Double, Int>>()
    // Cheap percussion/bass gates avoid pitch search for unrelated lags. Inspect every
    // hop so narrow attacks and repeated sections cannot hide between coarse samples.
    for (lag in 0..limit) {
        checkActive()
        val (value, rotation) = score(lag)
        candidates += Triple(lag, value, rotation)
    }
    val center = primary.percussion.size * STACK_INSTRUMENT_HOP_MS / 2.0
    val peaks = ArrayList<Triple<Int, Double, Int>>(4)
    for (candidate in candidates.sortedByDescending { it.second }) {
        if (candidate.second < minimumScore) break
        if (peaks.any { abs(it.first - candidate.first) <= 25 }) continue
        peaks += candidate
        if (peaks.size == 4) break
    }
    return peaks.map { best ->
        val left = candidates.getOrNull(best.first - 1)?.second ?: -1.0
        val right = candidates.getOrNull(best.first + 1)?.second ?: -1.0
        val refined = stackRefinePeak(doubleArrayOf(left, best.second, right), 1)
        StackInstrumentAnchor(primary.startMs + center,
            companion.startMs + center + (best.first + refined) * STACK_INSTRUMENT_HOP_MS,
            best.second, best.third)
    }
}

/** One vote per independent region. Competing whole-song maps still abstain. */
internal fun stackFitInstrumentCandidates(regions: List<List<StackInstrumentAnchor>>, durationMs: Long): StackAlignment {
    val groups = regions.filter { it.isNotEmpty() }
    if (groups.size < 3) return StackAlignment(0, 0.0, false)
    val models = ArrayList<StackAlignment>()
    for (i in groups.indices) for (j in i + 1 until groups.size) {
        for (a in groups[i]) for (b in groups[j]) {
            val distance = b.primaryMs - a.primaryMs
            if (distance < durationMs * .4) continue
            val scale = (b.companionMs - a.companionMs) / distance
            val offset = a.companionMs - scale * a.primaryMs
            if (scale !in .985..1.015 || abs(offset) > 30_000) continue
            val inliers = groups.mapNotNull { group ->
                group.minByOrNull { abs(it.companionMs - (scale * it.primaryMs + offset)) }
                    ?.takeIf { abs(it.companionMs - (scale * it.primaryMs + offset)) <= 25 }
            }
            // Reject scattered local hits; never let two regions manufacture a map.
            if (inliers.size < 3 || inliers.size * 4 < groups.size * 3) continue
            val fitted = stackFitInstrumentMap(inliers, durationMs)
            if (fitted.confident) models += fitted
        }
    }
    val best = models.maxWithOrNull(compareBy<StackAlignment> { it.anchorCount }.thenBy { it.correlation })
        ?: return StackAlignment(0, 0.0, false)
    val alternative = models.filter {
        abs(it.offsetUs - best.offsetUs) > 500_000 ||
            abs((it.timeScale - best.timeScale) * durationMs + (it.offsetUs - best.offsetUs) / 1000) > 500
    }.maxWithOrNull(compareBy<StackAlignment> { it.anchorCount }.thenBy { it.correlation })
    if (alternative != null && alternative.anchorCount >= best.anchorCount && best.correlation - alternative.correlation < .06)
        return StackAlignment(0, best.correlation, false)
    return best
}

/** A mono-only decision requires independent native-rate attack agreement, not a relaxed score. */
internal fun stackValidateNativeCorrections(coarse: StackAlignment, corrections: List<Double>, mandatory: Boolean): StackAlignment {
    val values = corrections.filter { it.isFinite() && abs(it) <= 12000 }.sorted()
    val cluster = values.indices.map { first -> values.drop(first).takeWhile { it - values[first] <= 4000 } }
        .maxByOrNull { it.size }.orEmpty()
    if (!coarse.confident || cluster.size < 3 || cluster.size * 4 < values.size * 3)
        return if (mandatory) StackAlignment(0, coarse.correlation, false) else coarse
    val correction = if (cluster.size % 2 == 0) (cluster[cluster.size / 2 - 1] + cluster[cluster.size / 2]) / 2
        else cluster[cluster.size / 2]
    val offset = coarse.offsetUs + correction
    if (abs(offset) > 30_000_000) return if (mandatory) StackAlignment(0, coarse.correlation, false) else coarse
    return coarse.copy(offsetMs = (offset / 1000).roundToLong(), offsetUs = offset)
}

/** Robustly gated linear map: no unconstrained DTW, repeated loops, or fabricated correspondences. */
internal fun stackFitInstrumentMap(anchors: List<StackInstrumentAnchor>, durationMs: Long): StackAlignment {
    val supported = anchors.filter { it.rotation >= 0 }
    val dominant = supported.groupingBy { it.rotation }.eachCount().maxByOrNull { it.value }
    val coherent = if (dominant != null && supported.map { it.rotation }.distinct().size > 1) {
        // A different intro melody is not evidence for the backing's pitch shift.
        // Reject isolated pitch conflicts only when a strong distributed majority exists.
        if (dominant.value < 3 || dominant.value < supported.size * .75) return StackAlignment(0, 0.0, false)
        anchors.filter { it.rotation < 0 || it.rotation == dominant.key }
    } else anchors
    return stackFitCoherentInstrumentMap(coherent, durationMs)
}

private fun stackFitCoherentInstrumentMap(anchors: List<StackInstrumentAnchor>, durationMs: Long): StackAlignment {
    if (anchors.size < 3) return StackAlignment(0, 0.0, false)
    val xMean = anchors.map { it.primaryMs }.average()
    val yMean = anchors.map { it.companionMs }.average()
    val spread = anchors.maxOf { it.primaryMs } - anchors.minOf { it.primaryMs }
    if (spread < max(STACK_REGION_MS.toDouble(), durationMs * .4)) return StackAlignment(0, 0.0, false)
    val denominator = anchors.sumOf { (it.primaryMs - xMean).pow(2) }
    val scale = anchors.sumOf { (it.primaryMs - xMean) * (it.companionMs - yMean) } / denominator
    val offset = yMean - scale * xMean
    val residual = anchors.maxOf { abs(it.companionMs - (scale * it.primaryMs + offset)) }
    if (!scale.isFinite() || scale !in .985..1.015 || abs(offset) > 30_000 || residual > 25) {
        return StackAlignment(0, anchors.map { it.score }.average(), false)
    }
    return StackAlignment(offset.roundToLong(), anchors.map { it.score }.average(), true,
        timeScale = scale, offsetUs = offset * 1000, anchorCount = anchors.size, residualMs = residual)
}

internal fun stackMappedPosition(masterMs: Long, slot: StackSlot): Long =
    (masterMs * slot.alignmentScale + (slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0) / 1000).roundToLong()

internal fun stackRebaseMappings(slots: List<StackSlot>, primary: StackSlot): List<StackSlot> {
    val b = primary.alignmentOffsetUs ?: primary.offsetMs * 1000.0
    return slots.map { slot ->
        val scale = slot.alignmentScale / primary.alignmentScale
        val offset = (slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0) - scale * b
        slot.copy(offsetMs = (offset / 1000).roundToLong(), alignmentScale = scale, alignmentOffsetUs = offset)
    }
}
