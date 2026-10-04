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
        primary = Color(0xFF2D6A57),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCBE9DD),
        onPrimaryContainer = Color(0xFF0B2A20),
        secondary = Color(0xFF4F6F64),
        tertiary = Color(0xFF796849),
        background = Color(0xFFF7F8F6),
        surface = Color(0xFFFFFDFC),
        surfaceVariant = Color(0xFFE8ECE8),
        onSurface = Color(0xFF171A18),
        onSurfaceVariant = Color(0xFF5B625E),
        inverseSurface = Color(0xFF2C312E),
        inverseOnSurface = Color(0xFFF0F2EF),
        inversePrimary = Color(0xFF83D4B9),
        outline = Color(0xFF747C77),
        outlineVariant = Color(0xFFC9D0CB),
    ),
    dark = darkColorScheme(
        primary = Color(0xFF83D4B9),
        onPrimary = Color(0xFF0A382B),
        primaryContainer = Color(0xFF174C3C),
        onPrimaryContainer = Color(0xFFCBE9DD),
        secondary = Color(0xFFA8CDBF),
        tertiary = Color(0xFFD1C09B),
        background = Color(0xFF0D100F),
        surface = Color(0xFF151918),
        surfaceVariant = Color(0xFF222826),
        onSurface = Color(0xFFE9EEEB),
        onSurfaceVariant = Color(0xFFB7C0BA),
        inverseSurface = Color(0xFFE9EEEB),
        inverseOnSurface = Color(0xFF252A27),
        inversePrimary = Color(0xFF2D6A57),
        outline = Color(0xFF818B85),
        outlineVariant = Color(0xFF38413D),
    ),
)

private val slatePalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF475E73),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCE7F0),
        onPrimaryContainer = Color(0xFF132838),
        secondary = Color(0xFF5E6E7C),
        tertiary = Color(0xFF71657B),
        background = Color(0xFFF7F8FA),
        surface = Color(0xFFFCFDFE),
        surfaceVariant = Color(0xFFE8EBEF),
        onSurface = Color(0xFF171A1E),
        onSurfaceVariant = Color(0xFF59616A),
        inverseSurface = Color(0xFF2C3035),
        inverseOnSurface = Color(0xFFF0F2F4),
        inversePrimary = Color(0xFFAEC9DF),
        outline = Color(0xFF747D86),
        outlineVariant = Color(0xFFC9CFD5),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFAEC9DF),
        onPrimary = Color(0xFF153144),
        primaryContainer = Color(0xFF2A465B),
        onPrimaryContainer = Color(0xFFDCE7F0),
        secondary = Color(0xFFBCC9D4),
        tertiary = Color(0xFFD0C1D8),
        background = Color(0xFF0E1115),
        surface = Color(0xFF171B20),
        surfaceVariant = Color(0xFF242A31),
        onSurface = Color(0xFFE9EDF1),
        onSurfaceVariant = Color(0xFFB8C0C8),
        inverseSurface = Color(0xFFE9EDF1),
        inverseOnSurface = Color(0xFF262B30),
        inversePrimary = Color(0xFF475E73),
        outline = Color(0xFF858F99),
        outlineVariant = Color(0xFF3A424B),
    ),
)

private val amberPalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF8A561D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF7DFC2),
        onPrimaryContainer = Color(0xFF321B04),
        secondary = Color(0xFF745F4A),
        tertiary = Color(0xFF84603A),
        background = Color(0xFFF9F7F4),
        surface = Color(0xFFFFFDFC),
        surfaceVariant = Color(0xFFEDE8E1),
        onSurface = Color(0xFF1D1A16),
        onSurfaceVariant = Color(0xFF645D55),
        inverseSurface = Color(0xFF322E29),
        inverseOnSurface = Color(0xFFF4F0EA),
        inversePrimary = Color(0xFFF2BA78),
        outline = Color(0xFF80776E),
        outlineVariant = Color(0xFFD4CCC2),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFF2BA78),
        onPrimary = Color(0xFF4A2A00),
        primaryContainer = Color(0xFF634112),
        onPrimaryContainer = Color(0xFFF7DFC2),
        secondary = Color(0xFFD7C4AF),
        tertiary = Color(0xFFE2BD91),
        background = Color(0xFF11100E),
        surface = Color(0xFF1B1916),
        surfaceVariant = Color(0xFF29251F),
        onSurface = Color(0xFFF0ECE7),
        onSurfaceVariant = Color(0xFFC6BDB3),
        inverseSurface = Color(0xFFF0ECE7),
        inverseOnSurface = Color(0xFF29251F),
        inversePrimary = Color(0xFF8A561D),
        outline = Color(0xFF91877C),
        outlineVariant = Color(0xFF443D35),
    ),
)

private val indigoPalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF4B5EA7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0E5FF),
        onPrimaryContainer = Color(0xFF172456),
        secondary = Color(0xFF5D668D),
        tertiary = Color(0xFF705980),
        background = Color(0xFFF7F7FA),
        surface = Color(0xFFFCFCFF),
        surfaceVariant = Color(0xFFE9E9F0),
        onSurface = Color(0xFF191A20),
        onSurfaceVariant = Color(0xFF5E5F6A),
        inverseSurface = Color(0xFF2E3037),
        inverseOnSurface = Color(0xFFF1F0F5),
        inversePrimary = Color(0xFFBBC5FF),
        outline = Color(0xFF797A86),
        outlineVariant = Color(0xFFCECED8),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFBBC5FF),
        onPrimary = Color(0xFF1A2A65),
        primaryContainer = Color(0xFF334681),
        onPrimaryContainer = Color(0xFFE0E5FF),
        secondary = Color(0xFFC5C9E7),
        tertiary = Color(0xFFD8BFE2),
        background = Color(0xFF0F1015),
        surface = Color(0xFF181A21),
        surfaceVariant = Color(0xFF252832),
        onSurface = Color(0xFFEBEBF1),
        onSurfaceVariant = Color(0xFFBDBDC8),
        inverseSurface = Color(0xFFEBEBF1),
        inverseOnSurface = Color(0xFF282A31),
        inversePrimary = Color(0xFF4B5EA7),
        outline = Color(0xFF898A97),
        outlineVariant = Color(0xFF3D404C),
    ),
)

private val rosePalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF965166),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFADDE4),
        onPrimaryContainer = Color(0xFF351722),
        secondary = Color(0xFF745D66),
        tertiary = Color(0xFF83585C),
        background = Color(0xFFF9F7F8),
        surface = Color(0xFFFFFCFD),
        surfaceVariant = Color(0xFFEDE8EA),
        onSurface = Color(0xFF1E191B),
        onSurfaceVariant = Color(0xFF655B5F),
        inverseSurface = Color(0xFF332E30),
        inverseOnSurface = Color(0xFFF4EFF1),
        inversePrimary = Color(0xFFEBB3C2),
        outline = Color(0xFF81777B),
        outlineVariant = Color(0xFFD5CBCD),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFEBB3C2),
        onPrimary = Color(0xFF4F1D2B),
        primaryContainer = Color(0xFF6B3546),
        onPrimaryContainer = Color(0xFFFADDE4),
        secondary = Color(0xFFD4C0C7),
        tertiary = Color(0xFFE0BABD),
        background = Color(0xFF110F10),
        surface = Color(0xFF1B1819),
        surfaceVariant = Color(0xFF292426),
        onSurface = Color(0xFFF0EAEC),
        onSurfaceVariant = Color(0xFFC5B9BD),
        inverseSurface = Color(0xFFF0EAEC),
        inverseOnSurface = Color(0xFF2A2527),
        inversePrimary = Color(0xFF965166),
        outline = Color(0xFF91858A),
        outlineVariant = Color(0xFF443B3E),
    ),
)

private val monochromePalette = AppColorPalette(
    light = lightColorScheme(
        primary = Color(0xFF35383B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE4E6E8),
        onPrimaryContainer = Color(0xFF191B1D),
        secondary = Color(0xFF5D6266),
        tertiary = Color(0xFF6C7074),
        background = Color(0xFFF8F8F8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE9EAEB),
        onSurface = Color(0xFF171819),
        onSurfaceVariant = Color(0xFF5F6265),
        inverseSurface = Color(0xFF2E3032),
        inverseOnSurface = Color(0xFFF2F2F2),
        inversePrimary = Color(0xFFC7CACD),
        outline = Color(0xFF7A7E81),
        outlineVariant = Color(0xFFCDD0D2),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFD8DADC),
        onPrimary = Color(0xFF202224),
        primaryContainer = Color(0xFF383B3E),
        onPrimaryContainer = Color(0xFFF0F1F2),
        secondary = Color(0xFFBEC2C5),
        tertiary = Color(0xFFAEB2B5),
        background = Color(0xFF0E0F10),
        surface = Color(0xFF18191A),
        surfaceVariant = Color(0xFF26282A),
        onSurface = Color(0xFFEDEEEF),
        onSurfaceVariant = Color(0xFFBEC1C3),
        inverseSurface = Color(0xFFEDEEEF),
        inverseOnSurface = Color(0xFF27292B),
        inversePrimary = Color(0xFF4F5356),
        outline = Color(0xFF898D90),
        outlineVariant = Color(0xFF3D4042),
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
    val colors = if (dark) palette.dark else palette.light
    // Material's default secondary containers are lavender even in Forest/Amber.
    // Derive every control surface from the selected palette instead.
    return colors.copy(
        secondaryContainer = androidx.compose.ui.graphics.lerp(colors.surfaceVariant, colors.primary, .12f),
        onSecondaryContainer = colors.onSurface,
        tertiaryContainer = androidx.compose.ui.graphics.lerp(colors.surfaceVariant, colors.tertiary, .12f),
        onTertiaryContainer = colors.onSurface,
        surfaceTint = colors.primary,
    )
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
