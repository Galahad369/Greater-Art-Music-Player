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
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.R

internal fun colorSchemeFor(theme: ColorTheme, dark: Boolean) = when (theme) {
    ColorTheme.FOREST -> if (dark) ForestDark else ForestLight
    ColorTheme.SLATE -> if (dark) SlateDark else SlateLight
    ColorTheme.AMBER -> if (dark) AmberDark else AmberLight
    ColorTheme.INDIGO -> if (dark) IndigoDark else IndigoLight
    ColorTheme.ROSE -> if (dark) RoseDark else RoseLight
    ColorTheme.MONOCHROME -> if (dark) MonoDark else MonoLight
}

private val ForestLight = lightColorScheme(
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

private val ForestDark = darkColorScheme(
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

internal val LightColors = ForestLight
internal val DarkColors = ForestDark

private val SlateLight = lightColorScheme(
    primary = Color(0xFF3D5A73),
    onPrimary = Color(0xFFF4F7FA),
    primaryContainer = Color(0xFFD5E3EF),
    onPrimaryContainer = Color(0xFF0F1C28),
    secondary = Color(0xFF4A6478),
    tertiary = Color(0xFF5C6B7A),
    background = Color(0xFFF3F5F7),
    surface = Color(0xFFF8FAFB),
    surfaceVariant = Color(0xFFE2E8ED),
    onSurface = Color(0xFF12161A),
    onSurfaceVariant = Color(0xFF5A6570),
    inverseSurface = Color(0xFF1A1F24),
    inverseOnSurface = Color(0xFFEEF1F4),
    inversePrimary = Color(0xFFA8C5DC),
    outline = Color(0xFF7A8794),
    outlineVariant = Color(0xFFC9D2DA),
)

private val SlateDark = darkColorScheme(
    primary = Color(0xFFA8C5DC),
    onPrimary = Color(0xFF0C1520),
    primaryContainer = Color(0xFF243548),
    onPrimaryContainer = Color(0xFFD5E3EF),
    secondary = Color(0xFF8FAABB),
    tertiary = Color(0xFF9AADB8),
    background = Color(0xFF0A0C0E),
    surface = Color(0xFF14181C),
    surfaceVariant = Color(0xFF1E252B),
    onSurface = Color(0xFFEEF1F4),
    onSurfaceVariant = Color(0xFF9AA5B0),
    inverseSurface = Color(0xFFEEF1F4),
    inverseOnSurface = Color(0xFF12161A),
    inversePrimary = Color(0xFF3D5A73),
    outline = Color(0xFF5E6B78),
    outlineVariant = Color(0xFF2A323A),
)

private val AmberLight = lightColorScheme(
    primary = Color(0xFF8B5E2B),
    onPrimary = Color(0xFFFFF8F0),
    primaryContainer = Color(0xFFF5E4C8),
    onPrimaryContainer = Color(0xFF2A1A08),
    secondary = Color(0xFF9A6B3A),
    tertiary = Color(0xFF7A5C38),
    background = Color(0xFFF7F3ED),
    surface = Color(0xFFFCFAF6),
    surfaceVariant = Color(0xFFECE4D8),
    onSurface = Color(0xFF1A1510),
    onSurfaceVariant = Color(0xFF6B5E50),
    inverseSurface = Color(0xFF1C1712),
    inverseOnSurface = Color(0xFFF5F0E8),
    inversePrimary = Color(0xFFE0B87A),
    outline = Color(0xFF8A7A68),
    outlineVariant = Color(0xFFD6CBBC),
)

private val AmberDark = darkColorScheme(
    primary = Color(0xFFE0B87A),
    onPrimary = Color(0xFF1A1006),
    primaryContainer = Color(0xFF3D2A14),
    onPrimaryContainer = Color(0xFFF5E4C8),
    secondary = Color(0xFFC9A06A),
    tertiary = Color(0xFFB89A72),
    background = Color(0xFF0C0A08),
    surface = Color(0xFF17140F),
    surfaceVariant = Color(0xFF242018),
    onSurface = Color(0xFFF5F0E8),
    onSurfaceVariant = Color(0xFFA89888),
    inverseSurface = Color(0xFFF5F0E8),
    inverseOnSurface = Color(0xFF1A1510),
    inversePrimary = Color(0xFF8B5E2B),
    outline = Color(0xFF6E6050),
    outlineVariant = Color(0xFF342C22),
)

private val IndigoLight = lightColorScheme(
    primary = Color(0xFF3F3D8B),
    onPrimary = Color(0xFFF6F5FF),
    primaryContainer = Color(0xFFDDDBF8),
    onPrimaryContainer = Color(0xFF16153A),
    secondary = Color(0xFF5553A0),
    tertiary = Color(0xFF6B4F8C),
    background = Color(0xFFF5F4F9),
    surface = Color(0xFFFAF9FC),
    surfaceVariant = Color(0xFFE6E4F0),
    onSurface = Color(0xFF14131C),
    onSurfaceVariant = Color(0xFF5C5A70),
    inverseSurface = Color(0xFF1A1924),
    inverseOnSurface = Color(0xFFF0EEF6),
    inversePrimary = Color(0xFFB4B1EA),
    outline = Color(0xFF7A7890),
    outlineVariant = Color(0xFFCBC8DA),
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFB4B1EA),
    onPrimary = Color(0xFF12112A),
    primaryContainer = Color(0xFF2A2858),
    onPrimaryContainer = Color(0xFFDDDBF8),
    secondary = Color(0xFF9B99D0),
    tertiary = Color(0xFFB89AD4),
    background = Color(0xFF0A0910),
    surface = Color(0xFF15141E),
    surfaceVariant = Color(0xFF201F2C),
    onSurface = Color(0xFFF0EEF6),
    onSurfaceVariant = Color(0xFFA09CB8),
    inverseSurface = Color(0xFFF0EEF6),
    inverseOnSurface = Color(0xFF14131C),
    inversePrimary = Color(0xFF3F3D8B),
    outline = Color(0xFF5E5C78),
    outlineVariant = Color(0xFF2E2C40),
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFF8B4A5A),
    onPrimary = Color(0xFFFFF5F7),
    primaryContainer = Color(0xFFF8DDE3),
    onPrimaryContainer = Color(0xFF2A1218),
    secondary = Color(0xFF9A5A68),
    tertiary = Color(0xFF7A5A50),
    background = Color(0xFFF8F3F4),
    surface = Color(0xFFFCF8F9),
    surfaceVariant = Color(0xFFEDE2E5),
    onSurface = Color(0xFF1A1315),
    onSurfaceVariant = Color(0xFF6B585C),
    inverseSurface = Color(0xFF1C1517),
    inverseOnSurface = Color(0xFFF6F0F1),
    inversePrimary = Color(0xFFE0A8B4),
    outline = Color(0xFF8A7478),
    outlineVariant = Color(0xFFD6C8CB),
)

private val RoseDark = darkColorScheme(
    primary = Color(0xFFE0A8B4),
    onPrimary = Color(0xFF1A0C10),
    primaryContainer = Color(0xFF3D222A),
    onPrimaryContainer = Color(0xFFF8DDE3),
    secondary = Color(0xFFC998A4),
    tertiary = Color(0xFFB89A90),
    background = Color(0xFF0C090A),
    surface = Color(0xFF171214),
    surfaceVariant = Color(0xFF241C1F),
    onSurface = Color(0xFFF6F0F1),
    onSurfaceVariant = Color(0xFFA8989C),
    inverseSurface = Color(0xFFF6F0F1),
    inverseOnSurface = Color(0xFF1A1315),
    inversePrimary = Color(0xFF8B4A5A),
    outline = Color(0xFF6E5C60),
    outlineVariant = Color(0xFF34282C),
)

private val MonoLight = lightColorScheme(
    primary = Color(0xFF2A2A2A),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF121212),
    secondary = Color(0xFF4A4A4A),
    tertiary = Color(0xFF5A5A5A),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurface = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF5A5A5A),
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFF0F0F0),
    inversePrimary = Color(0xFFB8B8B8),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFFC8C8C8),
)

private val MonoDark = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF101010),
    primaryContainer = Color(0xFF2E2E2E),
    onPrimaryContainer = Color(0xFFE8E8E8),
    secondary = Color(0xFFB0B0B0),
    tertiary = Color(0xFFA0A0A0),
    background = Color(0xFF080808),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF9A9A9A),
    inverseSurface = Color(0xFFF0F0F0),
    inverseOnSurface = Color(0xFF111111),
    inversePrimary = Color(0xFF2A2A2A),
    outline = Color(0xFF606060),
    outlineVariant = Color(0xFF2C2C2C),
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
        colorScheme = colorSchemeFor(colorTheme, dark),
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
        displayLarge = base.displayLarge.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold),
        displayMedium = base.displayMedium.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        displaySmall = base.displaySmall.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = base.bodySmall.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = base.labelLarge.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = family, fontFeatureSettings = features, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    )
}
