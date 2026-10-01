package com.local.listentomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.ui.design.DesignTokens

enum class GACardVariant {
    ELEVATED,
    FILLED,
    OUTLINED,
    OVERLAY
}

@Composable
fun GACard(
    variant: GACardVariant = GACardVariant.ELEVATED,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    elevation: androidx.compose.ui.unit.Dp = DesignTokens.Elevation.level1,
    content: @Composable () -> Unit
) {
    val cardModifier = when (variant) {
        GACardVariant.ELEVATED -> modifier
            .fillMaxWidth()
            .shadow(Shadow.elevation(elevation))
        GACardVariant.FILLED -> modifier
            .fillMaxWidth()
        GACardVariant.OUTLINED -> modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        GACardVariant.OVERLAY -> modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
    }

    val cardColors = when (variant) {
        GACardVariant.ELEVATED -> SurfaceDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        GACardVariant.FILLED -> SurfaceDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        GACardVariant.OUTLINED -> SurfaceDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        GACardVariant.OVERLAY -> SurfaceDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = cardColors,
        shadowElevation = if (variant == GACardVariant.ELEVATED) 4.dp else 0.dp,
        border = if (variant == GACardVariant.OUTLINED) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(if (enabled) Modifier else Modifier.pointerInput(Unit) {}),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    }
}

@Composable
fun GACardSimple(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = SurfaceDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shadowElevation = 2.dp
    ) {
        content()
    }
}