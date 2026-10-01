package com.local.listentomusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.listentomusic.data.AppFont
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.R
internal val LightColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color(0xFFF5FFFB),
    primaryContainer = Color(0xFFDDF8F1),
    onPrimaryContainer = Color(0xFF10221E),
    secondary = Color(0xFF267B69),
    tertiary = Color(0xFF6C7E1C),
    background = Color(0xFFF5F5F1),
    surface = Color(0xFFFBFBF7),
    surfaceVariant = Color(0xFFE5EAE5),
    onSurface = Color(0xFF111513),
    onSurfaceVariant = Color(0xFF666D68),
    inverseSurface = Color(0xFF181C1B),
    inverseOnSurface = Color(0xFFF3F5F0),
    inversePrimary = Color(0xFF8BE9D3),
    outline = Color(0xFF8D9690),
    outlineVariant = Color(0xFFD7DDD8),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF8BE9D3),
    onPrimary = Color(0xFF07130F),
    primaryContainer = Color(0xFF183A32),
    onPrimaryContainer = Color(0xFFDDF8F1),
    secondary = Color(0xFF72D7C0),
    tertiary = Color(0xFFB8D6A1),
    background = Color(0xFF080A09),
    surface = Color(0xFF151918),
    surfaceVariant = Color(0xFF202624),
    onSurface = Color(0xFFF3F5F0),
    onSurfaceVariant = Color(0xFF9CA39D),
    inverseSurface = Color(0xFFF3F5F0),
    inverseOnSurface = Color(0xFF101311),
    inversePrimary = Color(0xFF267B69),
    outline = Color(0xFF65706A),
    outlineVariant = Color(0xFF323A37),
)

private val RoundedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun GreaterArtTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    appFont: AppFont = AppFont.SYSTEM,
    silianRail: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = RoundedShapes,
        typography = typographyFor(if (silianRail) AppFont.SILIAN_RAIL else appFont, silianRail),
        content = content,
    )
}

private fun typographyFor(font: AppFont, smallCaps: Boolean): Typography {
    val family = when (font) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.SANS_SERIF -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONOSPACE -> FontFamily.Monospace
        AppFont.CURSIVE -> FontFamily.Cursive
        AppFont.INTER -> FontFamily(Font(R.font.inter))
        AppFont.NUNITO -> FontFamily(Font(R.font.nunito))
        AppFont.OSWALD -> FontFamily(Font(R.font.oswald))
        AppFont.SILIAN_RAIL -> FontFamily(Font(R.font.garamond))
        AppFont.PLAYFAIR_DISPLAY -> FontFamily(Font(R.font.playfair_display))
        AppFont.ROBOTO_SLAB -> FontFamily(Font(R.font.roboto_slab))
        AppFont.SOURCE_CODE_PRO -> FontFamily(Font(R.font.source_code_pro))
    }
    val base = Typography()
    val features = if (smallCaps) "\"smcp\"" else null
    return base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold,
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold,
        ),
        displaySmall = base.displaySmall.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 16.sp, lineHeight = 24.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 14.sp, lineHeight = 20.sp,
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 12.sp, lineHeight = 17.sp,
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium,
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = family, fontFeatureSettings = features,
            fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium,
        ),
    )
}
