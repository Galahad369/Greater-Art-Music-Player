package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Live compositor colors with cached-art fallback; never creates a video decoder. */
@Composable
internal fun Modifier.ambientBackdrop(artwork: Bitmap?, light: Boolean, currentPath: String? = null): Modifier {
    val video by VideoAmbientColors.state.collectAsStateWithLifecycle()
    val colors by produceState(artworkGradientColors(intArrayOf(), light), artwork, light) {
        value = withContext(Dispatchers.Default) {
            val pixels = runCatching {
                if (artwork == null || artwork.isRecycled) return@runCatching intArrayOf()
                val readable = if (artwork.config == Bitmap.Config.HARDWARE)
                    artwork.copy(Bitmap.Config.ARGB_8888, false) else artwork
                try {
                    if (readable == null) intArrayOf() else IntArray(144) { index ->
                        readable.getPixel(index % 12 * readable.width / 12, index / 12 * readable.height / 12)
                    }
                } finally { if (readable !== artwork) readable?.recycle() }
            }.getOrDefault(intArrayOf())
            artworkGradientColors(pixels, light)
        }
    }
    val live = remember(video, currentPath, light) {
        if (currentPath != null && video.mediaId == currentPath && video.pixels.isNotEmpty())
            artworkGradientColors(video.pixels.toIntArray(), light) else null
    }
    val target = live ?: colors
    val top = animateColorAsState(target.first(), tween(700), label = "ambient-top")
    val middle = animateColorAsState(target[target.size / 2], tween(700), label = "ambient-middle")
    val bottom = animateColorAsState(target.last(), tween(700), label = "ambient-bottom")
    // Read animation state during drawing, not composition: a color fade must not
    // recompose the queue, controls and native video host on every display frame.
    return drawBehind { drawRect(Brush.verticalGradient(listOf(top.value, middle.value, bottom.value))) }
}

internal fun artworkGradientColors(pixels: IntArray, light: Boolean): List<Color> {
    val base = if (light) Color(0xFFF6F7F6) else Color(0xFF090D10)
    val usable = pixels.filter { (it ushr 24) >= 128 }
    if (usable.isEmpty()) return listOf(base, base)
    // Quantized RGB histogram gives a representative color, not a grey average.
    // Ignore black letterboxing and near-neutral shadows when real color exists.
    val colorful = usable.filter {
        val channels = listOf((it shr 16) and 255, (it shr 8) and 255, it and 255)
        channels.max() >= 64 && channels.max() - channels.min() >= 32
    }
    val dominant = colorful.ifEmpty { usable }.groupBy { ((it shr 20) and 15) * 256 + ((it shr 12) and 15) * 16 + ((it shr 4) and 15) }
        .maxBy { it.value.size }.value
    val tint = Color(
        red = dominant.map { (it shr 16) and 255 }.average().toFloat() / 255f,
        green = dominant.map { (it shr 8) and 255 }.average().toFloat() / 255f,
        blue = dominant.map { it and 255 }.average().toFloat() / 255f,
    )
    // Conservative tint strengths keep both light and dark foregrounds readable.
    val alternate = colorful.filter { pixel ->
        val r = ((pixel shr 16) and 255) / 255f
        val g = ((pixel shr 8) and 255) / 255f
        val b = (pixel and 255) / 255f
        kotlin.math.abs(r - tint.red) + kotlin.math.abs(g - tint.green) + kotlin.math.abs(b - tint.blue) > .55f
    }.ifEmpty { dominant }
    val second = Color(alternate.map { (it shr 16) and 255 }.average().toFloat() / 255f,
        alternate.map { (it shr 8) and 255 }.average().toFloat() / 255f,
        alternate.map { it and 255 }.average().toFloat() / 255f)
    return listOf(lerp(base, tint, if (light) .18f else .42f),
        lerp(base, second, if (light) .10f else .24f), base)
}
