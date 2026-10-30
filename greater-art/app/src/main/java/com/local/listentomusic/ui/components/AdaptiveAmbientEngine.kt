package com.local.listentomusic.ui.components

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import com.local.listentomusic.data.AmbientMotion

internal data class AmbientSignal(
    val colors: List<Int>, val brightness: Float, val saturation: Float, val activity: Float = 0f,
)
internal data class AmbientLayers(
    val classic: Float = 1f, val aurora: Float, val bloom: Float, val flow: Float, val motion: Float,
)
internal fun ambientInputPixels(mediaId: String?, videoId: String, videoPixels: List<Int>,
    artworkPixels: List<Int>): List<Int> =
    if (mediaId != null && mediaId == videoId && videoPixels.isNotEmpty()) videoPixels else artworkPixels
internal fun ambientUnit(value: Float, fallback: Float = .5f) =
    if (value.isFinite()) value.coerceIn(0f, 1f) else fallback

/** Sparse RGB samples describe color variation, not HDR, camera motion, or musical beats. */
internal fun analyzeAmbient(pixels: List<Int>, fallback: List<Int>): AmbientSignal {
    val valid = pixels.take(576).filter { (it ushr 24) >= 128 }
    val neutral = fallback.take(3).ifEmpty { listOf(0xff10151b.toInt()) }.let {
        List(3) { index -> it[index % it.size] }
    }
    if (valid.isEmpty()) return AmbientSignal(neutral, .15f, 0f)
    fun channels(pixel: Int) = floatArrayOf(
        ((pixel ushr 16) and 255) / 255f, ((pixel ushr 8) and 255) / 255f, (pixel and 255) / 255f)
    var brightness = 0f; var saturation = 0f
    val colored = mutableListOf<Int>()
    for (pixel in valid) {
        val rgb = channels(pixel)
        val high = rgb.max(); val low = rgb.min()
        brightness += .2126f * rgb[0] + .7152f * rgb[1] + .0722f * rgb[2]
        val chroma = if (high > 0f) (high - low) / high else 0f
        saturation += chroma
        if (high >= .12f && chroma >= .16f) colored += pixel
    }
    brightness /= valid.size; saturation /= valid.size
    if (brightness < .025f) return AmbientSignal(neutral, brightness, 0f)
    val histogram = colored.ifEmpty { valid }.groupBy {
        ((it ushr 20) and 15) * 256 + ((it ushr 12) and 15) * 16 + ((it ushr 4) and 15)
    }.entries.sortedWith(compareByDescending<Map.Entry<Int, List<Int>>> { it.value.size }.thenBy { it.key })
    val palette = histogram.take(3).map { entry ->
        val red = entry.value.sumOf { (it ushr 16) and 255 } / entry.value.size
        val green = entry.value.sumOf { (it ushr 8) and 255 } / entry.value.size
        val blue = entry.value.sumOf { it and 255 } / entry.value.size
        (0xff shl 24) or (red shl 16) or (green shl 8) or blue
    }
    return AmbientSignal(List(3) { palette[it % palette.size] }, brightness, saturation)
}

/** Per-presentation state, no retained bitmap. New media clears scene history immediately. */
internal class AdaptiveAmbientEngine {
    private var id: String? = null
    private var previous: AmbientSignal? = null
    private var timestamp = 0L
    fun update(mediaId: String, pixels: List<Int>, fallback: List<Int>, nowMs: Long): AmbientSignal {
        val raw = analyzeAmbient(pixels, fallback)
        val old = previous.takeIf { id == mediaId }
        val elapsed = (nowMs - timestamp).coerceIn(0, 5000)
        val mix = (1.0 - exp(-elapsed / 1900.0)).toFloat()
        val difference = if (old == null) 0f else raw.colors.zip(old.colors).map { (a, b) ->
            (abs(((a ushr 16) and 255) - ((b ushr 16) and 255)) +
                abs(((a ushr 8) and 255) - ((b ushr 8) and 255)) +
                abs((a and 255) - (b and 255))) / 765f
        }.average().toFloat()
        val next = if (old == null) raw else raw.copy(
            brightness = old.brightness + (raw.brightness - old.brightness) * mix,
            saturation = old.saturation + (raw.saturation - old.saturation) * mix,
            activity = old.activity + (difference - old.activity) * mix,
        )
        id = mediaId; previous = next; timestamp = nowMs
        return next
    }
}

/** Two small numeric snapshots preserve palette continuity between Activity/overlay hosts. */
internal class AdaptiveAmbientCache {
    private data class Key(val mediaId: String, val fallback: List<Int>)
    private data class Entry(val engine: AdaptiveAmbientEngine, var signal: AmbientSignal)
    private val entries = LinkedHashMap<Key, Entry>(2, .75f, true)
    @Synchronized fun snapshot(mediaId: String, fallback: List<Int>): AmbientSignal =
        entries[Key(mediaId, fallback.take(3))]?.signal ?: analyzeAmbient(emptyList(), fallback)
    @Synchronized fun update(mediaId: String, pixels: List<Int>, fallback: List<Int>, nowMs: Long): AmbientSignal {
        val key = Key(mediaId, fallback.take(3))
        val entry = entries.getOrPut(key) { Entry(AdaptiveAmbientEngine(), analyzeAmbient(emptyList(), fallback)) }
        entry.signal = entry.engine.update(mediaId, pixels, fallback, nowMs)
        while (entries.size > 2) entries.remove(entries.keys.first())
        return entry.signal
    }
    val size: Int @Synchronized get() = entries.size
}
internal object SharedAdaptiveAmbient { val cache = AdaptiveAmbientCache() }

internal fun ambientLayers(signal: AmbientSignal, intensity: Float, react: Boolean,
    motion: AmbientMotion): AmbientLayers {
    val amount = ambientUnit(intensity)
    val brightness = if (react) ambientUnit(signal.brightness, .15f) else .35f
    val saturation = if (react) ambientUnit(signal.saturation, 0f) else .35f
    val activity = if (react) ambientUnit(signal.activity, 0f) else 0f
    return AmbientLayers(
        aurora = amount * (.18f + saturation * .22f),
        bloom = amount * (.08f + brightness * .26f * (.35f + saturation * .65f)),
        flow = amount * (.10f + activity * .20f),
        motion = when (motion) {
            AmbientMotion.OFF -> 0f
            AmbientMotion.SUBTLE -> .55f
            AmbientMotion.NORMAL -> 1f
        } * (1f + min(activity, .4f) * .3f),
    )
}

internal fun ambientShouldAnimate(visible: Boolean, resumed: Boolean, scrolling: Boolean,
    reducedMotion: Boolean, powerSave: Boolean, motion: AmbientMotion): Boolean =
    visible && resumed && !scrolling && !reducedMotion && !powerSave && motion != AmbientMotion.OFF
