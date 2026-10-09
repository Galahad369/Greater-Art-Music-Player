package com.local.listentomusic.playback

import kotlin.math.*

/** Original-rate attacks; resampling here is analysis-only, never an audible proxy. */
internal fun stackNativeAttack(samples: FloatArray, rate: Int): FloatArray {
    require(rate in 8000..192000 && samples.size <= rate)
    val fastAlpha = 1 - exp(-1.0 / (rate * .0008))
    val slowAlpha = 1 - exp(-1.0 / (rate * .008))
    var fast = 0.0; var slow = 0.0; var previous = 0.0
    return FloatArray(samples.size) { i ->
        val current = samples[i].toDouble().takeIf { it.isFinite() } ?: 0.0
        val magnitude = abs(current - previous); previous = current
        fast += fastAlpha * (magnitude - fast); slow += slowAlpha * (magnitude - slow)
        (fast - slow).coerceAtLeast(0.0).toFloat()
    }
}

internal fun stackNativeLagUs(primary: FloatArray, companion: FloatArray, rate: Int,
    checkActive: () -> Unit = {}): Double? {
    require(rate in 8000..192000 && primary.size <= rate && companion.size <= rate)
    val radius = rate * 12 / 1000
    val start = radius
    val n = minOf(primary.size, companion.size) - radius * 2
    if (n < rate / 10) return null
    fun correlation(lag: Int, stride: Int = 1): Double {
        var x = 0.0; var y = 0.0; var xx = 0.0; var yy = 0.0; var xy = 0.0
        var count = 0
        for (i in start until start + n step stride) {
            val a = primary[i].toDouble(); val b = companion[i + lag].toDouble()
            x += a; y += b; xx += a * a; yy += b * b; xy += a * b
            count++
        }
        val variance = (xx - x * x / count) * (yy - y * y / count)
        return if (variance > 1e-16) (xy - x * y / count) / sqrt(variance) else -1.0
    }
    val step = maxOf(1, rate / 4000)
    var best = 0; var score = -1.0
    // Visit every native-sample lag: skipping lags can miss narrow attacks entirely.
    // Subsample the comparison points, not the timing candidates, to bound work.
    val stride = maxOf(1, n / 4096)
    for (lag in -radius..radius) {
        checkActive()
        val value = correlation(lag, stride)
        if (value > score) { score = value; best = lag }
    }
    val seed = best
    score = -1.0
    for (lag in maxOf(-radius, seed - step)..minOf(radius, seed + step)) {
        checkActive()
        val value = correlation(lag)
        if (value > score) { score = value; best = lag }
    }
    if (score < .45 || abs(best) >= radius) return null
    val alternative = (-radius..radius step step).filter { abs(it - best) > rate / 500 }
        .maxOfOrNull { checkActive(); correlation(it, stride) } ?: -1.0
    if (score - alternative < .02) return null
    val correction = stackRefinePeak(doubleArrayOf(correlation(best - 1), score, correlation(best + 1)), 1)
    return (best + correction) * 1_000_000 / rate
}

internal fun stackResampleAnalysis(samples: FloatArray, sourceRate: Int, targetRate: Int): FloatArray {
    require(sourceRate in 8000..192000 && targetRate in 8000..192000 && samples.size <= sourceRate)
    if (sourceRate == targetRate) return samples
    return FloatArray((samples.size.toLong() * targetRate / sourceRate).toInt()) { i ->
        val position = i * sourceRate.toDouble() / targetRate
        val a = position.toInt().coerceAtMost(samples.lastIndex)
        val fraction = position - a
        (samples[a] * (1 - fraction) + samples[minOf(a + 1, samples.lastIndex)] * fraction).toFloat()
    }
}
