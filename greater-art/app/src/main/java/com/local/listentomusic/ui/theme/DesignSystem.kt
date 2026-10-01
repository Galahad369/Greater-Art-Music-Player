package com.local.listentomusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Small, deliberate token set for Greater Art's existing visual language.
 *
 * This is not a generic component theme. Liquid metal, media artwork and graph/video
 * surfaces keep their own character; these tokens only standardise repeated chrome,
 * spacing, interaction targets and motion.
 */
internal object GaSpacing {
    val micro = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

internal object GaControl {
    val touchTarget = 48.dp
    val icon = 22.dp
    val prominentIcon = 28.dp
    val hero = 56.dp
}

internal object GaRadius {
    val compact = 10.dp
    val control = 14.dp
    val chrome = 16.dp
    val panel = 20.dp
}

internal object GaMotion {
    const val quickMs = 120
    const val standardMs = 180
    const val emphasizedMs = 260
}

internal object GaAlpha {
    const val chrome = 0.68f
    const val divider = 0.38f
    const val secondary = 0.72f
}

/**
 * Full-screen video controls intentionally use a fixed light-on-dark palette instead
 * of theme colours: the surface underneath is arbitrary media, not a themed surface.
 */
internal object GaVideoOverlay {
    val foreground = Color(0xFFF6F7F4)
    val scrim = Color.Black.copy(alpha = 0.44f)
    val control = Color.Black.copy(alpha = 0.42f)
    val playSurface = Color.White.copy(alpha = 0.18f)
    val inactiveTrack = Color.White.copy(alpha = 0.36f)
}

@Composable
internal fun gaChromeColor(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = GaAlpha.chrome)

@Composable
internal fun gaDividerColor(): Color =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = GaAlpha.divider)
