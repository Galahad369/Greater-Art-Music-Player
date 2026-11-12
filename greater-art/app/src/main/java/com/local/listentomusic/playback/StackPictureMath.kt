package com.local.listentomusic.playback

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal const val STACK_PICTURE_GRID = 16
internal const val STACK_MAX_OFFSET_MS = 30_000L
private const val MAX_SHIFT_US = STACK_MAX_OFFSET_MS * 1_000
private const val MAX_ANALYSIS_US = 210_000_000L
private const val QUICK_WINDOW_US = 8_000_000L
private const val BUCKET_US = 100_000L
private const val FRAME_TOLERANCE_US = 35_000L
private const val PICTURE_SIMILARITY = .86f
private const val REGIONS = 3

internal enum class StackPictureSearch(val maxShiftUs: Long) {
    QUICK(2_000_000L), WIDE(MAX_SHIFT_US),
}

/** Windows are relative to the start of the clip, including for a clipped media source. */
internal data class StackPictureWindow(val startUs: Long, val endUs: Long)

internal fun stackPictureWindows(durationUs: Long, search: StackPictureSearch): List<StackPictureWindow> {
    val end = durationUs.coerceAtMost(MAX_ANALYSIS_US)
    if (end <= 0) return emptyList()
    if (search == StackPictureSearch.WIDE || end <= REGIONS * QUICK_WINDOW_US) {
        return listOf(StackPictureWindow(0L, end))
    }
    return List(REGIONS) { index ->
        val start = end * (2 * index + 1) / (2 * REGIONS) - QUICK_WINDOW_US / 2
        StackPictureWindow(start, start + QUICK_WINDOW_US)
    }
}

internal fun stackPictureCompanionWindows(
    reference: List<StackPictureWindow>,
    durationUs: Long,
    search: StackPictureSearch,
): List<StackPictureWindow> {
    if (search == StackPictureSearch.WIDE) return stackPictureWindows(durationUs, search)
    val end = durationUs.coerceAtMost(MAX_ANALYSIS_US)
    return buildList {
        for (window in reference) {
            val start = (window.startUs - search.maxShiftUs).coerceAtLeast(0L)
            val stop = (window.endUs + search.maxShiftUs).coerceAtMost(end)
            if (start >= stop) continue
            val previous = lastOrNull()
            if (previous != null && previous.endUs >= start) {
                set(lastIndex, StackPictureWindow(previous.startUs, stop))
            } else add(StackPictureWindow(start, stop))
        }
    }
}

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

internal class StackPictureReference(val anchors: List<Anchor>, val totals: IntArray) {
    data class Anchor(val before: StackPictureFrame, val after: StackPictureFrame, val change: Float, val region: Int)
}

internal fun prepareStackPictureReference(
    primary: List<StackPictureFrame>,
    checkpoint: () -> Unit = {},
): StackPictureReference? {
    if (primary.size < 24) return null
    val start = primary.first().timeUs
    val span = primary.last().timeUs - start + 1
    if (span < 5_000_000L) return null
    val anchors = mutableListOf<StackPictureReference.Anchor>()
    var previousBucket = -1L
    for (index in 1 until primary.size) {
        if (index % 64 == 0) checkpoint()
        val before = primary[index - 1]
        val after = primary[index]
        // A seek between analysis windows is not a scene cut.
        if (after.timeUs - before.timeUs > 125_000L) continue
        val change = 1f - pictureSimilarity(before, after)
        if (change < .008f) continue
        val elapsed = after.timeUs - start
        val bucket = elapsed / 450_000L
        if (bucket == previousBucket && change <= anchors.last().change) continue
        val anchor = StackPictureReference.Anchor(before, after, change, (elapsed * REGIONS / span).toInt())
        if (bucket == previousBucket) anchors[anchors.lastIndex] = anchor else anchors += anchor
        previousBucket = bucket
    }
    val totals = IntArray(REGIONS)
    anchors.forEach { totals[it.region]++ }
    return if (totals.any { it < 4 }) null else StackPictureReference(anchors, totals)
}

internal fun estimateStackPictureOffset(
    primary: List<StackPictureFrame>,
    companion: List<StackPictureFrame>,
    checkpoint: () -> Unit = {},
): StackAlignment? = estimateStackPictureOffset(primary, companion, StackPictureSearch.WIDE, checkpoint)

internal fun estimateStackPictureOffset(
    primary: List<StackPictureFrame>,
    companion: List<StackPictureFrame>,
    search: StackPictureSearch,
    checkpoint: () -> Unit = {},
): StackAlignment? = prepareStackPictureReference(primary, checkpoint)?.let {
    estimateStackPictureOffset(it, companion, search, checkpoint)
}

/** Compare time-stamped pictures only. A single shared scene cannot move a track. */
internal fun estimateStackPictureOffset(
    reference: StackPictureReference,
    companion: List<StackPictureFrame>,
    search: StackPictureSearch,
    checkpoint: () -> Unit,
): StackAlignment? {
    if (companion.size < 24) return null
    val anchors = reference.anchors
    val maxShiftUs = search.maxShiftUs
    val referenceIntervals = anchors.map { it.after.timeUs - it.before.timeUs }.sorted()
    val companionIntervals = companion.zipWithNext { before, after -> after.timeUs - before.timeUs }
        .filter { it in 1L..125_000L }.sorted()
    if (companionIntervals.isEmpty()) return null
    val frameIntervalUs = maxOf(referenceIntervals[referenceIntervals.size / 2],
        companionIntervals[companionIntervals.size / 2])
    val resolvedSeparationUs = maxOf(2 * FRAME_TOLERANCE_US, 2 * frameIntervalUs)
    val bins = IntArray((2 * maxShiftUs / BUCKET_US).toInt() + 1)
    for ((_, anchor) in anchors) {
        checkpoint()
        var last = -1
        var i = lowerPictureBound(companion, anchor.timeUs - maxShiftUs)
        while (i < companion.size) {
            val frame = companion[i++]
            val offset = frame.timeUs - anchor.timeUs
            if (offset > maxShiftUs) break
            val bin = ((offset + maxShiftUs) / BUCKET_US).toInt()
            if (bin != last && java.lang.Long.bitCount(anchor.edges xor frame.edges) <= 13) {
                bins[bin]++
                last = bin
            }
        }
    }
    val seeds = mutableListOf<Long>()
    for (bin in bins.indices.sortedByDescending { bins[it] }) {
        val offset = bin * BUCKET_US - maxShiftUs + BUCKET_US / 2
        if (bins[bin] < 6 || seeds.size == 8) break
        // Each seed is refined over ±150 ms. Do not suppress a separate peak
        // outside that range merely because it is less than 300 ms away.
        if (seeds.none { abs(it - offset) <= 150_000L }) seeds += offset
    }
    if (seeds.isEmpty()) return null

    fun nearest(target: Long): StackPictureFrame? {
        val i = lowerPictureBound(companion, target)
        val left = companion.getOrNull(i - 1)
        val right = companion.getOrNull(i)
        val frame = if (left != null && (right == null || target - left.timeUs <= right.timeUs - target)) left else right
        return frame?.takeIf { abs(it.timeUs - target) <= FRAME_TOLERANCE_US }
    }
    data class Score(val offset: Long, val value: Double, val coverage: Boolean, val deltas: List<Long>,
        val matched: IntArray, val regionValues: DoubleArray)
    fun score(offset: Long): Score {
        checkpoint()
        val matched = IntArray(REGIONS)
        val regionSums = DoubleArray(REGIONS)
        val deltas = ArrayList<Long>()
        var sum = 0.0
        for ((before, after, change, region) in anchors) {
            val frame = nearest(after.timeUs + offset) ?: continue
            val similarity = pictureSimilarity(after, frame)
            if (similarity < PICTURE_SIMILARITY) continue
            val preceding = nearest(before.timeUs + offset)
            val beforeSimilarity = preceding?.let { pictureSimilarity(before, it).coerceAtLeast(0f) } ?: 0f
            // Use the picture score to rank candidates once they have enough
            // timing evidence; this avoids favoring a particular frame cadence.
            val pictureScore = .75 * similarity + .25 * beforeSimilarity
            sum += pictureScore
            regionSums[region] += pictureScore
            if (preceding == null || preceding.timeUs >= frame.timeUs || beforeSimilarity < PICTURE_SIMILARITY) continue
            // An image can agree for a whole still. Both sides must agree
            // better in order than swapped before this counts as timing evidence.
            val pairedGain = beforeSimilarity + similarity -
                pictureSimilarity(before, frame) - pictureSimilarity(after, preceding)
            if (pairedGain < .25f * change) continue
            matched[region]++
            deltas += frame.timeUs - after.timeUs
        }
        val value = sum / anchors.size
        // Mixed frame rates can miss a transition pair even when its pictures
        // agree. Require a distributed two-thirds timing majority; matching
        // only half must not imply a global map. The ranking score averages
        // missing pictures as zero, so it is not a per-image cosine threshold.
        val coverage = deltas.size * 3 >= anchors.size * 2 &&
            matched.indices.all { matched[it] >= 4 && matched[it] * 2 >= reference.totals[it] }
        return Score(offset, value, coverage, deltas, matched,
            DoubleArray(REGIONS) { regionSums[it] / reference.totals[it] })
    }
    val candidates = seeds.flatMap { seed ->
        checkpoint()
        (-15..15).map { score((seed + it * 10_000L).coerceIn(-maxShiftUs, maxShiftUs)) }
    }
    val rank = compareBy<Score> { it.value }.thenBy { -abs(it.offset) }
    val coarseBest = candidates.filter { it.coverage }.maxWithOrNull(rank)
        ?: return null
    val best = (-9..9).asSequence().map {
        score((coarseBest.offset + it * 1_000L).coerceIn(-maxShiftUs, maxShiftUs))
    }.filter { it.coverage }.maxWithOrNull(rank) ?: coarseBest
    if (candidates.any {
        val separation = abs(it.offset - best.offset)
        (separation >= 400_000L || it.coverage && separation >= resolvedSeparationUs) &&
            it.value >= best.value - .06
    }) return null
    // Same-cadence clips can agree on one exact PTS delta. Mixed frame rates
    // produce several deltas; keep the best search position instead of
    // snapping it to a frame that only agrees with part of the clip.
    val median = best.deltas.sorted()[best.deltas.size / 2]
    val sameCadence = best.deltas.count { abs(it - median) <= 1_000L } * 3 >= best.deltas.size * 2
    var result = if (sameCadence) {
        if (abs(median) > maxShiftUs) return null
        score(median).takeIf { it.coverage && it.value >= best.value - .002 } ?: best
    } else best
    if (!sameCadence) {
        // Mixed frame rates can make an interval equally good. Choosing its edge
        // nearest zero biases the answer when the same clip is shifted. Center
        // only the contiguous, exactly equal maximum, admitting no weaker score.
        fun plateauEdge(direction: Long): Long {
            var edge = result.offset
            for (step in 1..((frameIntervalUs + 999L) / 1_000L + 1L).toInt()) {
                val next = (result.offset + direction * step * 1_000L).coerceIn(-maxShiftUs, maxShiftUs)
                if (next == edge) break
                val candidate = score(next)
                if (!candidate.coverage || candidate.value != result.value) break
                edge = next
            }
            return edge
        }
        val midpoint = score((plateauEdge(-1L) + plateauEdge(1L)) / 2)
        if (midpoint.coverage && midpoint.value == result.value) result = midpoint
    }
    // A unit-rate result cannot fix separated regions with visibly different
    // local optima. Compare near the proposed peak, allowing one observed frame
    // interval for cadence quantisation rather than claiming subframe accuracy.
    val regionalBest = List(REGIONS) { region ->
        candidates.asSequence().filter {
            abs(it.offset - result.offset) <= 2 * FRAME_TOLERANCE_US &&
                it.matched[region] >= 4 && it.matched[region] * 2 >= reference.totals[region]
        }.maxWithOrNull(compareBy<Score> { it.regionValues[region] }
            .thenBy { -abs(it.offset - result.offset) }) ?: result
    }
    val lowestRegion = regionalBest.indices.minBy { regionalBest[it].offset }
    val highestRegion = regionalBest.indices.maxBy { regionalBest[it].offset }
    if (regionalBest[highestRegion].offset - regionalBest[lowestRegion].offset > frameIntervalUs &&
        listOf(lowestRegion, highestRegion).any {
            regionalBest[it].regionValues[it] > result.regionValues[it] + .002
        }
    ) return null
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
