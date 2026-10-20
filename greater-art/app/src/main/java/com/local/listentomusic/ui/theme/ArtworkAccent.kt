package com.local.listentomusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.local.listentomusic.data.ColorTheme

internal fun accentContrast(a: Color, b: Color): Float =
    (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

/** Move only as far toward black/white as the existing foreground needs. */
private fun readableAccent(candidate: Color, foreground: Color): Color {
    if (accentContrast(candidate, foreground) >= 4.5f) return candidate
    val endpoint = if (accentContrast(Color.Black, foreground) > accentContrast(Color.White, foreground))
        Color.Black else Color.White
    var low = 0f; var high = 1f
    repeat(16) {
        val mid = (low + high) / 2f
        if (accentContrast(lerp(candidate, endpoint, mid), foreground) >= 4.5f) high = mid else low = mid
    }
    return lerp(candidate, endpoint, high)
}

/** Only three accent roles change; surface, text, error and OLED black roles stay intact. */
internal fun artworkAccentScheme(base: ColorScheme, palette: List<Int>, intensity: Float,
    theme: ColorTheme): ColorScheme {
    if (theme == ColorTheme.HIGH_CONTRAST || theme == ColorTheme.MONOCHROME ||
        !intensity.isFinite() || intensity <= 0f) return base
    val colors = palette.filter { (it ushr 24) >= 128 }.take(3)
    if (colors.isEmpty()) return base
    val amount = (if (theme == ColorTheme.VOID) .18f else .45f) * intensity.coerceAtMost(1f)
    return base.copy(
        primary = readableAccent(lerp(base.primary, Color(colors[0]), amount), base.onPrimary),
        secondary = readableAccent(lerp(base.secondary, Color(colors.getOrElse(1) { colors[0] }), amount * .65f), base.onSecondary),
        inversePrimary = readableAccent(lerp(base.inversePrimary, Color(colors[0]), amount), base.inverseSurface),
    )
}
