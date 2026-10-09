package com.local.listentomusic.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex

/** Presentation-only ViewModels reuse the same LRU, in-flight requests and decoder budget. */
object MediaCaches {
    private var thumbnails: ThumbnailRepository? = null
    private var waveforms: WaveformRepository? = null
    private var videoPlayback = false
    @Synchronized fun thumbnails(context: Context): ThumbnailRepository = thumbnails
        ?: ThumbnailRepository(context.applicationContext).also { thumbnails = it; it.setVideoPlayback(videoPlayback) }
    @Synchronized fun waveforms(context: Context): WaveformRepository = waveforms
        ?: WaveformRepository(context.applicationContext).also { waveforms = it }

    /** Do not instantiate caches just because Android is asking an idle process to trim. */
    @Synchronized fun trimMemory(level: Int) {
        thumbnails?.trimMemory(level)
    }

    @Synchronized fun setVideoPlayback(active: Boolean) {
        videoPlayback = active
        thumbnails?.setVideoPlayback(active)
    }
}

/** Only offline analysis is serialized; the real Media3 decoder never takes this lock. */
object OfflineAnalysisBudget { val mutex = Mutex() }
