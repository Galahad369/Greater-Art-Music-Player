package com.local.listentomusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Restrained metal: graphite, silver and a narrow teal reflection. A fixed
 * reflection avoids continuous redraws behind scrolling lists or live video.
 */
@Composable
fun LiquidMetalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    baseColor: Color? = null,
    accentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = baseColor ?: MaterialTheme.colorScheme.surface
    val teal = accentColor ?: MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.clip(shape).background(base),
        contentAlignment = contentAlignment,
    ) {
        // matchParentSize is deliberately non-measuring. fillMaxSize here made
        // this decorative canvas claim every available pixel when the surface
        // was used inside Scaffold.bottomBar, expanding the mini player over
        // the whole library.
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.11f),
                        teal.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Black.copy(alpha = 0.14f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
        }
        // Transparent/decorative containers must not inherit an arbitrary Activity
        // content color. Metal surfaces are dark by design, so keep controls legible.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}
