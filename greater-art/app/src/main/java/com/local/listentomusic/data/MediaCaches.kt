package com.local.listentomusic.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex

/** Presentation-only ViewModels reuse the same LRU, in-flight requests and decoder budget. */
object MediaCaches {
    private var thumbnails: ThumbnailRepository? = null
    private var waveforms: WaveformRepository? = null
    @Synchronized fun thumbnails(context: Context): ThumbnailRepository = thumbnails
        ?: ThumbnailRepository(context.applicationContext).also { thumbnails = it }
    @Synchronized fun waveforms(context: Context): WaveformRepository = waveforms
        ?: WaveformRepository(context.applicationContext).also { waveforms = it }
}

/** Only offline analysis is serialized; the real Media3 decoder never takes this lock. */
object OfflineAnalysisBudget { val mutex = Mutex() }
