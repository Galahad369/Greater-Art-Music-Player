package com.local.listentomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.listentomusic.ui.design.DesignTokens
import androidx.compose.ui.unit.dp

enum class GAButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT,
    ICON,
    ICON_TOGGLE,
    OVERLAY
}

enum class GAButtonSize {
    SMALL(32.dp),
    MEDIUM(40.dp),
    LARGE(48.dp),
    XL(56.dp);

    val size: androidx.compose.ui.unit.Dp
    constructor(size: androidx.compose.ui.unit.Dp) {
        this.size = size
    }
}

@Composable
fun GAButton(
    variant: GAButtonVariant = GAButtonVariant.PRIMARY,
    size: GAButtonSize = GAButtonSize.MEDIUM,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    when (variant) {
        GAButtonVariant.PRIMARY -> {
            androidx.compose.material3.Button(
                onClick = onClick,
                modifier = modifier.size(size.size),
                enabled = enabled,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.CornerRadius.md)
            ) {
                content()
            }
        }
        GAButtonVariant.SECONDARY -> {
            androidx.compose.material3.Button(
                onClick = onClick,
                modifier = modifier.size(size.size),
                enabled = enabled,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.CornerRadius.md)
            ) {
                content()
            }
        }
        GAButtonVariant.OUTLINED -> {
            androidx.compose.material3.OutlinedButton(
                onClick = onClick,
                modifier = modifier.size(size.size),
                enabled = enabled,
                colors = androidx.compose.material3.OutlinedButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    outlineColor = MaterialTheme.colorScheme.outline,
                    disabledOutlineColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.CornerRadius.md)
            ) {
                content()
            }
        }
        GAButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                enabled = enabled,
                colors = androidx.compose.material3.TextButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                content()
            }
        }
        GAButtonVariant.ICON -> {
            androidx.compose.material3.IconButton(
                onClick = onClick,
                modifier = modifier.size(size.size),
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                content()
            }
        }
        GAButtonVariant.ICON_TOGGLE -> {
            androidx.compose.material3.IconButton(
                onClick = onClick,
                modifier = modifier.size(size.size),
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                content()
            }
        }
        GAButtonVariant.OVERLAY -> {
            androidx.compose.material3.IconButton(
                onClick = onClick,
                modifier = modifier.size(size.size),
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.CornerRadius.full)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.CornerRadius.full)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun GAIconButton(
    icon: androidx.compose.ui.res.Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    size: GAButtonSize = GAButtonSize.MEDIUM,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    GAButton(
        variant = GAButtonVariant.ICON,
        size = size,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        androidx.compose.material3.Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}