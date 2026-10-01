package com.local.listentomusic.ui.design

import androidx.compose.ui.graphics.Color

// Semantic color roles for consistent UI
// These are resolved at composition time via getters
object SemanticColors {
    // Primary interactive elements
    val primaryAction: Color
        get() = Color(0xFF176B5B)
    val onPrimaryAction: Color
        get() = Color(0xFFF5FFFB)

    // Secondary interactive elements
    val secondaryAction: Color
        get() = Color(0xFF267B69)
    val onSecondaryAction: Color
        get() = Color(0xFFDDF8F1)

    // Surface and background
    val surface: Color
        get() = Color(0xFFFBFBF7)
    val onSurface: Color
        get() = Color(0xFF111513)
    val surfaceVariant: Color
        get() = Color(0xFFE5EAE5)
    val onSurfaceVariant: Color
        get() = Color(0xFF666D68)
    val surfaceContainer: Color
        get() = Color(0xFFE5EAE5)
    val surfaceContainerHigh: Color
        get() = Color(0xFFD7DFDC)
    val surfaceContainerHighest: Color
        get() = Color(0xFFCDD6D1)

    // Overlays and scrims
    val scrim: Color
        get() = Color(0x80000000) // 50% black
    val scrimLight: Color
        get() = Color(0x40000000) // 25% black
    val scrimHeavy: Color
        get() = Color(0xCC000000) // 80% black

    // Interactive states
    val interactivePrimary: Color
        get() = Color(0xFF176B5B)
    val onInteractivePrimary: Color
        get() = Color(0xFFF5FFFB)
    val interactiveSecondary: Color
        get() = Color(0xFFDDF8F1)
    val onInteractiveSecondary: Color
        get() = Color(0xFF10221E)

    // Borders and dividers
    val border: Color
        get() = Color(0xFF8D9690)
    val borderVariant: Color
        get() = Color(0xFFD7DDD8)
    val divider: Color
        get() = Color(0xFFD7DDD8)

    // Error and warning
    val error: Color
        get() = Color(0xFFBA1A1A)
    val onError: Color
        get() = Color(0xFFFFFFFF)
    val errorContainer: Color
        get() = Color(0xFFDAD6D5)
    val onErrorContainer: Color
        get() = Color(0xFF410002)

    // Overlay backgrounds
    val overlayBackground: Color
        get() = Color(0x80000000)
    val overlayBackgroundLight: Color
        get() = Color(0x4D000000)
    val overlayBackgroundHeavy: Color
        get() = Color(0xCC000000)

    // Status colors
    val success: Color
        get() = Color(0xFF4CAF50)
    val warning: Color
        get() = Color(0xFFFF9800)
    val info: Color
        get() = Color(0xFF2196F3)

    // Interactive states
    val pressedOverlay: Color
        get() = Color(0x1FFFFFFF)
    val hoverOverlay: Color
        get() = Color(0x14FFFFFF)
    val focusOverlay: Color
        get() = Color(0x1FFFFFFF)
    val dragOverlay: Color
        get() = Color(0x29FFFFFF)
}