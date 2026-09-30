package com.local.listentomusic.ui

/** Stack | Library | Nodes sample one continuous source-video crop. */
internal fun libraryPagerBackgroundPosition(page: Int, offsetFraction: Float): Float =
    ((page + offsetFraction) / 2f).coerceIn(0f, 1f)
