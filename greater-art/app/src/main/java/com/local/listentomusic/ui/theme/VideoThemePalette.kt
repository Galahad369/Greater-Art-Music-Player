package com.local.listentomusic.ui.theme

import kotlin.math.roundToInt

/** Numeric state only. Presentation lifetimes and artwork objects are not cache keys. */
internal class VideoThemePalette(private val capacity: Int = 8) {
    private class Entry(var colors: List<Int>) {
        var from = colors
        var target = colors
        var started = 0L
        var lastSample = Long.MIN_VALUE
        var samples = 0L
        val mean = DoubleArray(3)
        var frozen: Int? = null
    }
    private val entries = LinkedHashMap<String, Entry>(capacity, .75f, true)
    private var saving = false

    fun colors(id: String): List<Int> = entries[id]?.colors.orEmpty()
    fun snapshot(): Map<String, List<Int>> = entries.mapValues { it.value.colors }

    fun seedArtwork(id: String, colors: List<Int>) {
        if (id.isBlank() || colors.isEmpty() || entries.containsKey(id)) return
        entry(id, List(3) { colors[it % colors.size] })
    }

    fun sample(id: String, palette: List<Int>, average: Int, timestampMs: Long, nowMs: Long) {
        if (id.isBlank() || palette.isEmpty()) return
        val value = entry(id, List(3) { palette[it % palette.size] })
        if (timestampMs <= value.lastSample) return
        value.lastSample = timestampMs
        value.samples++
        for (i in 0..2) {
            val channel = (average ushr (16 - i * 8)) and 255
            value.mean[i] += (channel - value.mean[i]) / value.samples
        }
        if (saving) {
            // The first actual video sample can replace an artwork-only fallback.
            // Once frozen, later scenes must not recolor this track in Power saving.
            if (value.frozen == null) value.frozen = mean(value)
            value.colors = List(3) { value.frozen!! }
            value.target = value.colors
            value.from = value.colors
        } else {
            advance(value, nowMs)
            value.from = value.colors
            value.target = List(3) { palette[it % palette.size] }
            value.started = nowMs
        }
    }

    fun powerSaving(enabled: Boolean, nowMs: Long) {
        if (saving == enabled) return
        tick(nowMs)
        saving = enabled
        for (value in entries.values) {
            if (enabled && value.samples > 0) {
                value.frozen = mean(value)
                value.colors = List(3) { value.frozen!! }
            }
            if (!enabled) value.frozen = null
            value.from = value.colors
            value.target = value.colors
        }
    }

    fun tick(nowMs: Long) { if (!saving) entries.values.forEach { advance(it, nowMs) } }
    fun moving(): Boolean = !saving && entries.values.any { it.colors != it.target }
    fun freeze(nowMs: Long) {
        tick(nowMs)
        for (value in entries.values) { value.target = value.colors; value.from = value.colors }
    }

    private fun entry(id: String, colors: List<Int>): Entry = entries.getOrPut(id) {
        Entry(colors).also {
            if (entries.size >= capacity) entries.remove(entries.keys.first())
        }
    }
    private fun mean(value: Entry): Int = argb(value.mean.map { it.roundToInt() })
    private fun advance(value: Entry, nowMs: Long) {
        if (value.colors == value.target) return
        val t = ((nowMs - value.started) / 900f).coerceIn(0f, 1f)
        val fraction = t * t * (3f - 2f * t)
        value.colors = value.target.mapIndexed { i, target ->
            val start = value.from[i]
            argb((0..2).map { c ->
                val shift = 16 - 8 * c
                val a = (start ushr shift) and 255
                (a + (((target ushr shift) and 255) - a) * fraction).roundToInt()
            })
        }
    }
}

private fun argb(channels: List<Int>): Int = (0xff shl 24) or
    (channels[0].coerceIn(0, 255) shl 16) or (channels[1].coerceIn(0, 255) shl 8) or channels[2].coerceIn(0, 255)

internal fun averageVideoColor(pixels: List<Int>): Int? {
    val valid = pixels.take(576).filter { (it ushr 24) >= 128 }
    if (valid.isEmpty()) return null
    return argb((0..2).map { i -> valid.sumOf { (it ushr (16 - 8 * i)) and 255 } / valid.size })
}
