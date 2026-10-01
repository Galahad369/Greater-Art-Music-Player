package com.local.listentomusic.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

// Semantic color roles for consistent UI
object SemanticColors {
    // Primary interactive elements
    val primaryAction = MaterialTheme.colorScheme.primary
    val onPrimaryAction = MaterialTheme.colorScheme.onPrimary

    // Secondary interactive elements
    val secondaryAction = MaterialTheme.colorScheme.secondary
    val onSecondaryAction = MaterialTheme.colorScheme.onSecondary

    // Surface and background
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainerHighest

    // Overlays and scrims
    val scrim = Color(0x80000000) // 50% black
    val scrimLight = Color(0x40000000) // 25% black
    val scrimHeavy = Color(0xCC000000) // 80% black

    // Interactive states
    val interactivePrimary = MaterialTheme.colorScheme.primary
    val onInteractivePrimary = MaterialTheme.colorScheme.onPrimary
    val interactiveSecondary = MaterialTheme.colorScheme.secondaryContainer
    val onInteractiveSecondary = MaterialTheme.colorScheme.onSecondaryContainer

    // Borders and dividers
    val border = MaterialTheme.colorScheme.outline
    val borderVariant = MaterialTheme.colorScheme.outlineVariant
    val divider = MaterialTheme.colorScheme.outlineVariant

    // Error and warning
    val error = MaterialTheme.colorScheme.error
    val onError = MaterialTheme.colorScheme.onError
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    // Overlay backgrounds
    val overlayBackground = Color.Black.copy(alpha = 0.5f)
    val overlayBackgroundLight = Color.Black.copy(alpha = 0.3f)
    val overlayBackgroundHeavy = Color.Black.copy(alpha = 0.7f)

    // Status colors
    val success = Color(0xFF4CAF50)
    val warning = Color(0xFFFF9800)
    val info = Color(0xFF2196F3)

    // Interactive states
    val pressedOverlay = Color.White.copy(alpha = 0.12f)
    val hoverOverlay = Color.White.copy(alpha = 0.08f)
    val focusOverlay = Color.White.copy(alpha = 0.12f)
    val dragOverlay = Color.White.copy(alpha = 0.16f)
}