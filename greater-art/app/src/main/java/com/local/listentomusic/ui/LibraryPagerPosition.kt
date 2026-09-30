package com.local.listentomusic.ui

/** Continuous pager position: Stack = 0, Library = 1, Nodes = 2. */
internal fun libraryPagerNavigationPosition(page: Int, offsetFraction: Float): Float =
    (page + offsetFraction).coerceIn(0f, 2f)

/** Stack | Library | Nodes sample one continuous source-video crop. */
internal fun libraryPagerBackgroundPosition(page: Int, offsetFraction: Float): Float =
    libraryPagerNavigationPosition(page, offsetFraction) / 2f
