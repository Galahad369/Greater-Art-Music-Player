package com.local.listentomusic.playback

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal const val STACK_PICTURE_GRID = 16
internal const val STACK_MAX_OFFSET_MS = 30_000L
private const val MAX_SHIFT_US = STACK_MAX_OFFSET_MS * 1_000
private const val BUCKET_US = 100_000L
private const val FRAME_TOLERANCE_US = 35_000L
private const val REGIONS = 3

internal data class StackPictureFrame(val timeUs: Long, val pixels: FloatArray, val edges: Long)

/** Retain shape, not brightness. Flat and near-black frames are not evidence. */
internal fun stackPictureFrame(timeUs: Long, luma: FloatArray): StackPictureFrame? {
    val mean = luma.average().toFloat()
    val centered = FloatArray(luma.size) { luma[it] - mean }
    val energy = centered.sumOf { (it * it).toDouble() }
    if (energy < luma.size * 36.0) return null
    val norm = sqrt(energy).toFloat()
    for (i in centered.indices) centered[i] /= norm
    var edges = 0L
    for (y in 0 until 8) for (x in 0 until 8) {
        val i = y * 2 * STACK_PICTURE_GRID + x * 2
        if (luma[i] > luma[i + 1]) edges = edges or (1L shl (y * 8 + x))
    }
    return StackPictureFrame(timeUs, centered, edges)
}

/** Compare time-stamped pictures only. A single shared scene cannot move a track. */
internal fun estimateStackPictureOffset(
    primary: List<StackPictureFrame>,
    companion: List<StackPictureFrame>,
    checkpoint: () -> Unit = {},
): StackAlignment? {
    if (primary.size < 24 || companion.size < 24) return null
    val start = primary.first().timeUs
    val span = primary.last().timeUs - start + 1
    if (span < 5_000_000L) return null
    fun region(frame: StackPictureFrame) = ((frame.timeUs - start) * REGIONS / span).toInt()
    data class Anchor(val before: StackPictureFrame, val after: StackPictureFrame, val change: Float)
    val anchors = primary.zipWithNext().asSequence().mapNotNull { (before, after) ->
        val change = 1f - pictureSimilarity(before, after)
        if (change >= .008f) Anchor(before, after, change) else null
    }.groupBy { (it.after.timeUs - start) / 450_000L }
        .values.map { bucket -> bucket.maxBy { it.change } }
    val totals = IntArray(REGIONS)
    anchors.forEach { totals[region(it.after)]++ }
    if (totals.any { it < 4 }) return null

    val bins = IntArray((2 * MAX_SHIFT_US / BUCKET_US).toInt() + 1)
    for ((_, anchor) in anchors) {
        checkpoint()
        var last = -1
        var i = lowerPictureBound(companion, anchor.timeUs - MAX_SHIFT_US)
        while (i < companion.size) {
            val frame = companion[i++]
            val offset = frame.timeUs - anchor.timeUs
            if (offset > MAX_SHIFT_US) break
            val bin = ((offset + MAX_SHIFT_US) / BUCKET_US).toInt()
            if (bin != last && java.lang.Long.bitCount(anchor.edges xor frame.edges) <= 13) {
                bins[bin]++
                last = bin
            }
        }
    }
    val seeds = mutableListOf<Long>()
    for (bin in bins.indices.sortedByDescending { bins[it] }) {
        val offset = bin * BUCKET_US - MAX_SHIFT_US + BUCKET_US / 2
        if (bins[bin] < 6 || seeds.size == 8) break
        if (seeds.none { abs(it - offset) <= 300_000L }) seeds += offset
    }
    if (seeds.isEmpty()) return null

    fun nearest(target: Long): StackPictureFrame? {
        val i = lowerPictureBound(companion, target)
        val left = companion.getOrNull(i - 1)
        val right = companion.getOrNull(i)
        val frame = if (left != null && (right == null || target - left.timeUs <= right.timeUs - target)) left else right
        return frame?.takeIf { abs(it.timeUs - target) <= FRAME_TOLERANCE_US }
    }
    data class Score(val offset: Long, val value: Double, val coverage: Boolean, val deltas: List<Long>)
    fun score(offset: Long): Score {
        val matched = IntArray(REGIONS)
        val deltas = ArrayList<Long>()
        var sum = 0.0
        for ((before, after) in anchors) {
            val frame = nearest(after.timeUs + offset) ?: continue
            val similarity = pictureSimilarity(after, frame)
            if (similarity >= .86f) {
                matched[region(after)]++
                deltas += frame.timeUs - after.timeUs
                // A still can match for many frames. The preceding picture
                // distinguishes the actual cut from the interior of the still.
                val preceding = nearest(before.timeUs + offset)
                    ?.let { pictureSimilarity(before, it).coerceAtLeast(0f) } ?: 0f
                sum += .75 * similarity + .25 * preceding
            }
        }
        val coverage = deltas.size * 2 >= anchors.size &&
            matched.indices.all { matched[it] >= 4 && matched[it] * 3 >= totals[it] }
        return Score(offset, sum / anchors.size, coverage, deltas)
    }
    val candidates = seeds.flatMap { seed ->
        checkpoint()
        (-15..15).map { score((seed + it * 10_000L).coerceIn(-MAX_SHIFT_US, MAX_SHIFT_US)) }
    }
    val rank = compareBy<Score> { it.value }.thenBy { -abs(it.offset) }
    val coarseBest = candidates.filter { it.coverage }.maxWithOrNull(rank)
        ?: return null
    val best = (-9..9).asSequence().map {
        score((coarseBest.offset + it * 1_000L).coerceIn(-MAX_SHIFT_US, MAX_SHIFT_US))
    }.filter { it.coverage }.maxWithOrNull(rank) ?: coarseBest
    if (candidates.any { abs(it.offset - best.offset) >= 400_000L && it.value >= best.value - .06 }) return null
    // Same-cadence clips can agree on one exact PTS delta. Mixed frame rates
    // produce several deltas; keep the best search position instead of
    // snapping it to a frame that only agrees with part of the clip.
    val median = best.deltas.sorted()[best.deltas.size / 2]
    val result = if (best.deltas.count { abs(it - median) <= 1_000L } * 3 >= best.deltas.size * 2) {
        score(median).takeIf { it.coverage && it.value >= best.value - .002 } ?: best
    } else best
    val offset = result.offset
    return StackAlignment((offset / 1_000.0).roundToLong(), result.value, true,
        timeScale = 1.0, offsetUs = offset.toDouble())
}

private fun pictureSimilarity(a: StackPictureFrame, b: StackPictureFrame): Float {
    var dot = 0f
    for (i in a.pixels.indices) dot += a.pixels[i] * b.pixels[i]
    return dot
}

private fun lowerPictureBound(frames: List<StackPictureFrame>, timeUs: Long): Int {
    var left = 0
    var right = frames.size
    while (left < right) {
        val mid = (left + right) ushr 1
        if (frames[mid].timeUs < timeUs) left = mid + 1 else right = mid
    }
    return left
}
