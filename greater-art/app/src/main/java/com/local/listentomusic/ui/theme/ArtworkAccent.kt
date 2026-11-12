package com.local.listentomusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.UserPreferences

internal fun mediaThemeEnabled(preferences: UserPreferences, mediaId: String?): Boolean =
    mediaId != null && (preferences.colorTheme == ColorTheme.VIDEO ||
        (preferences.backgroundMode in listOf(AppBackgroundMode.AMBIENT, AppBackgroundMode.ADAPTIVE_DYNAMIC) &&
            preferences.colorTheme !in listOf(ColorTheme.HIGH_CONTRAST, ColorTheme.MONOCHROME) &&
            preferences.adaptiveIntensity.isFinite() && preferences.adaptiveIntensity > 0f))

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

/** Change control accents only; surface, text, error and OLED black roles stay intact. */
internal fun artworkAccentScheme(base: ColorScheme, palette: List<Int>, intensity: Float,
    theme: ColorTheme): ColorScheme {
    if (theme == ColorTheme.HIGH_CONTRAST || theme == ColorTheme.MONOCHROME ||
        !intensity.isFinite() || intensity <= 0f) return base
    val colors = palette.filter { (it ushr 24) >= 128 }.take(3)
    if (colors.isEmpty()) return base
    val amount = when (theme) {
        ColorTheme.VIDEO -> 1f
        ColorTheme.VOID -> .18f
        else -> .45f
    } * intensity.coerceAtMost(1f)
    return base.copy(
        primary = readableAccent(lerp(base.primary, Color(colors[0]), amount), base.onPrimary),
        secondary = readableAccent(lerp(base.secondary, Color(colors.getOrElse(1) { colors[0] }), amount * if (theme == ColorTheme.VIDEO) 1f else .65f), base.onSecondary),
        tertiary = if (theme == ColorTheme.VIDEO)
            readableAccent(Color(colors.getOrElse(2) { colors[0] }), base.onTertiary) else base.tertiary,
        inversePrimary = readableAccent(lerp(base.inversePrimary, Color(colors[0]), amount), base.inverseSurface),
    )
}
