package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode

internal fun floatingModeLabel(language: AppLanguage, mode: FloatingWindowMode): String {
    val labels = when (language) {
        AppLanguage.ENGLISH -> listOf("Mini Window", "Android PiP", "No Floating")
        AppLanguage.TRADITIONAL_CHINESE -> listOf("迷你視窗", "Android 子母畫面", "無浮動視窗")
        AppLanguage.CANTONESE -> listOf("迷你視窗", "Android 子母畫面", "唔開浮動視窗")
        AppLanguage.JAPANESE -> listOf("ミニウィンドウ", "Android ピクチャーインピクチャー", "フローティングなし")
        AppLanguage.GERMAN -> listOf("Minifenster", "Android Bild-in-Bild", "Kein schwebendes Fenster")
        AppLanguage.FRENCH -> listOf("Mini-fenêtre", "Image dans l’image Android", "Sans fenêtre flottante")
    }
    return labels[when (mode) {
        FloatingWindowMode.MINI_WINDOW -> 0
        FloatingWindowMode.COMPACT, FloatingWindowMode.FOLLOW_VIDEO -> 1
        FloatingWindowMode.NONE -> 2
    }]
}

internal fun floatingModeDescription(language: AppLanguage, mode: FloatingWindowMode): String {
    val label = floatingModeLabel(language, mode)
    return when (language) {
        AppLanguage.ENGLISH -> "Floating mode: $label. Tap to change."
        AppLanguage.TRADITIONAL_CHINESE -> "浮動模式：$label，輕觸切換。"
        AppLanguage.CANTONESE -> "浮動模式：$label，撳一下切換。"
        AppLanguage.JAPANESE -> "フローティングモード：$label。タップして変更。"
        AppLanguage.GERMAN -> "Fenstermodus: $label. Zum Wechseln tippen."
        AppLanguage.FRENCH -> "Mode de fenêtre : $label. Touchez pour changer."
    }
}
