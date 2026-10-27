package com.local.listentomusic.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform

/** First viewport is immediate; motion updates coalesce, with a <=50ms trailing idle delivery. */
internal fun Flow<Triple<Boolean, Int, Int>>.pacedThumbnailViewports(): Flow<Triple<Boolean, Int, Int>> =
    conflate().transform { viewport ->
        emit(viewport)
        if (viewport.first) delay(50L)
    }
