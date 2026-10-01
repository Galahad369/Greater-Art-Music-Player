package com.local.listentomusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
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
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.R
private data class AppColorPalette(val light: ColorScheme, val dark: ColorScheme)

private val forestPalette = AppColorPalette(
    light = lightColorScheme(
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
    ),
    dark = darkColorScheme(
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
    ),
)

private val slatePalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF3B556D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E5F2),
        onPrimaryContainer = Color(0xFF122231),
        secondary = Color(0xFF4D6579),
        tertiary = Color(0xFF6C5D7A),
        background = Color(0xFFF3F4F6),
        surface = Color(0xFFF8F9FB),
        surfaceVariant = Color(0xFFE1E5EB),
        onSurface = Color(0xFF161B21),
        onSurfaceVariant = Color(0xFF49515B),
        inverseSurface = Color(0xFF252B33),
        inverseOnSurface = Color(0xFFF0F1F4),
        inversePrimary = Color(0xFFA5C8E6),
        outline = Color(0xFF717A84),
        outlineVariant = Color(0xFFC1C8D1),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFA5C8E6),
        onPrimary = Color(0xFF062237),
        primaryContainer = Color(0xFF22374A),
        onPrimaryContainer = Color(0xFFD6E5F2),
        secondary = Color(0xFFB3C8DA),
        tertiary = Color(0xFFD2BCE4),
        background = Color(0xFF0B0F14),
        surface = Color(0xFF161B22),
        surfaceVariant = Color(0xFF232A33),
        onSurface = Color(0xFFE8EDF3),
        onSurfaceVariant = Color(0xFFA9B2BD),
        inverseSurface = Color(0xFFE8EDF3),
        inverseOnSurface = Color(0xFF171C23),
        inversePrimary = Color(0xFF3B556D),
        outline = Color(0xFF86909A),
        outlineVariant = Color(0xFF3A434E),
    ),
)

private val amberPalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF86531E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDDBE),
        onPrimaryContainer = Color(0xFF2D1600),
        secondary = Color(0xFF6F5A44),
        tertiary = Color(0xFF7C552F),
        background = Color(0xFFF7F3EE),
        surface = Color(0xFFFCF8F2),
        surfaceVariant = Color(0xFFE9E0D6),
        onSurface = Color(0xFF231A12),
        onSurfaceVariant = Color(0xFF5C534A),
        inverseSurface = Color(0xFF382E25),
        inverseOnSurface = Color(0xFFFDEEDD),
        inversePrimary = Color(0xFFFFB770),
        outline = Color(0xFF8F8378),
        outlineVariant = Color(0xFFD3C6BA),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFFFB770),
        onPrimary = Color(0xFF4A2800),
        primaryContainer = Color(0xFF663D10),
        onPrimaryContainer = Color(0xFFFFDDBE),
        secondary = Color(0xFFD8C2AA),
        tertiary = Color(0xFFEDBE8F),
        background = Color(0xFF120E0A),
        surface = Color(0xFF1D1712),
        surfaceVariant = Color(0xFF2A221B),
        onSurface = Color(0xFFF2E2D2),
        onSurfaceVariant = Color(0xFFC0B0A0),
        inverseSurface = Color(0xFFF2E2D2),
        inverseOnSurface = Color(0xFF251B14),
        inversePrimary = Color(0xFF86531E),
        outline = Color(0xFF9D8F82),
        outlineVariant = Color(0xFF443830),
    ),
)

private val indigoPalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF4457A8),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDDE2FF),
        onPrimaryContainer = Color(0xFF0F1A4D),
        secondary = Color(0xFF515D88),
        tertiary = Color(0xFF6D4E8A),
        background = Color(0xFFF2F3F9),
        surface = Color(0xFFF8F8FC),
        surfaceVariant = Color(0xFFE2E5F2),
        onSurface = Color(0xFF151827),
        onSurfaceVariant = Color(0xFF4A4F63),
        inverseSurface = Color(0xFF2A2E3E),
        inverseOnSurface = Color(0xFFEDEFFF),
        inversePrimary = Color(0xFFBAC3FF),
        outline = Color(0xFF73788E),
        outlineVariant = Color(0xFFC4C9DB),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFBAC3FF),
        onPrimary = Color(0xFF102163),
        primaryContainer = Color(0xFF2D3A7E),
        onPrimaryContainer = Color(0xFFDDE2FF),
        secondary = Color(0xFFC0C7EF),
        tertiary = Color(0xFFDAB9F9),
        background = Color(0xFF090B14),
        surface = Color(0xFF15192A),
        surfaceVariant = Color(0xFF22283A),
        onSurface = Color(0xFFE9ECFF),
        onSurfaceVariant = Color(0xFFADB3CB),
        inverseSurface = Color(0xFFE9ECFF),
        inverseOnSurface = Color(0xFF14192B),
        inversePrimary = Color(0xFF4457A8),
        outline = Color(0xFF8990A8),
        outlineVariant = Color(0xFF3B4256),
    ),
)

private val rosePalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF8E4E61),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3A1020),
        secondary = Color(0xFF7A5966),
        tertiary = Color(0xFF84515A),
        background = Color(0xFFF8F1F3),
        surface = Color(0xFFFEF7F8),
        surfaceVariant = Color(0xFFECDDE1),
        onSurface = Color(0xFF26191D),
        onSurfaceVariant = Color(0xFF5E4D52),
        inverseSurface = Color(0xFF3B2C30),
        inverseOnSurface = Color(0xFFFDEDEE),
        inversePrimary = Color(0xFFFFB2C7),
        outline = Color(0xFF917E84),
        outlineVariant = Color(0xFFD8C8CD),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFFFB2C7),
        onPrimary = Color(0xFF561D31),
        primaryContainer = Color(0xFF713549),
        onPrimaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFFEBC0CE),
        tertiary = Color(0xFFF0BCC4),
        background = Color(0xFF140C0F),
        surface = Color(0xFF201519),
        surfaceVariant = Color(0xFF2E2025),
        onSurface = Color(0xFFFCECEF),
        onSurfaceVariant = Color(0xFFC8AEB6),
        inverseSurface = Color(0xFFFCECEF),
        inverseOnSurface = Color(0xFF27181D),
        inversePrimary = Color(0xFF8E4E61),
        outline = Color(0xFFA69097),
        outlineVariant = Color(0xFF48363C),
    ),
)

private val monochromePalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF2C2C2C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE6E6E6),
        onPrimaryContainer = Color(0xFF111111),
        secondary = Color(0xFF505050),
        tertiary = Color(0xFF666666),
        background = Color(0xFFF4F4F4),
        surface = Color(0xFFFAFAFA),
        surfaceVariant = Color(0xFFE4E4E4),
        onSurface = Color(0xFF101010),
        onSurfaceVariant = Color(0xFF4F4F4F),
        inverseSurface = Color(0xFF1E1E1E),
        inverseOnSurface = Color(0xFFF1F1F1),
        inversePrimary = Color(0xFFC8C8C8),
        outline = Color(0xFF7B7B7B),
        outlineVariant = Color(0xFFCACACA),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFE0E0E0),
        onPrimary = Color(0xFF1A1A1A),
        primaryContainer = Color(0xFF393939),
        onPrimaryContainer = Color(0xFFF2F2F2),
        secondary = Color(0xFFBDBDBD),
        tertiary = Color(0xFFA8A8A8),
        background = Color(0xFF070707),
        surface = Color(0xFF141414),
        surfaceVariant = Color(0xFF222222),
        onSurface = Color(0xFFF2F2F2),
        onSurfaceVariant = Color(0xFFC2C2C2),
        inverseSurface = Color(0xFFF2F2F2),
        inverseOnSurface = Color(0xFF171717),
        inversePrimary = Color(0xFF3F3F3F),
        outline = Color(0xFF8B8B8B),
        outlineVariant = Color(0xFF383838),
    ),
)

internal val LightColors = forestPalette.light
internal val DarkColors = forestPalette.dark

internal fun appColorScheme(theme: ColorTheme, dark: Boolean): ColorScheme {
    val palette = when (theme) {
        ColorTheme.FOREST -> forestPalette
        ColorTheme.SLATE -> slatePalette
        ColorTheme.AMBER -> amberPalette
        ColorTheme.INDIGO -> indigoPalette
        ColorTheme.ROSE -> rosePalette
        ColorTheme.MONOCHROME -> monochromePalette
    }
    return if (dark) palette.dark else palette.light
}

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
    colorTheme: ColorTheme = ColorTheme.FOREST,
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
        colorScheme = appColorScheme(colorTheme, dark),
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
