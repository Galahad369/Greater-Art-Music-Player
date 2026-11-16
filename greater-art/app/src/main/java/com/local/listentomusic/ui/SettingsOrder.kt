package com.local.listentomusic.ui

import com.local.listentomusic.data.ColorTheme

/** Order presentation only; never change persisted enum names or the user's selection. */
internal fun <T> defaultFirst(values: List<T>, default: T): List<T> =
    values.filter { it == default } + values.filterNot { it == default }

internal fun colorThemeOptions(): List<ColorTheme> =
    listOf(ColorTheme.VOID, ColorTheme.VIDEO) +
        ColorTheme.entries.filterNot { it == ColorTheme.VOID || it == ColorTheme.VIDEO }
