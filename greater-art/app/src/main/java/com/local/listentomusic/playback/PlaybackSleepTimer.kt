package com.local.listentomusic.playback

import android.os.SystemClock
import com.local.listentomusic.SleepTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-process sleep timer owned by the playback service rather than an Activity ViewModel. */
internal object PlaybackSleepTimer {
    private val mutable = MutableStateFlow(SleepTimerState())
    val state = mutable.asStateFlow()
    // Only the schedule triggers the service countdown; UI remaining-time updates do not.
    private val target = MutableStateFlow<Long?>(null)
    val deadline = target.asStateFlow()

    fun set(minutes: Long) {
        require(minutes == -1L || minutes >= 0L)
        if (minutes == 0L) { cancel(); return }
        if (minutes == -1L) {
            target.value = null
            mutable.value = SleepTimerState(active = true, endOfTrack = true)
        } else {
            val duration = minutes.coerceAtMost(Long.MAX_VALUE / 60_000L) * 60_000L
            val now = SystemClock.elapsedRealtime()
            target.value = now + duration
            mutable.value = SleepTimerState(active = true, remainingMs = duration)
        }
    }

    fun cancel() {
        target.value = null
        mutable.value = SleepTimerState()
    }

    /** A new track reached automatically, repeated, or the final track ended. */
    fun finishAtTrackEnd(): Boolean {
        if (!mutable.value.active || !mutable.value.endOfTrack) return false
        cancel()
        return true
    }

    /** Returns true once when a deadline expires, using monotonic time. */
    fun tick(): Boolean {
        val until = target.value ?: return false
        val remaining = (until - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        if (remaining == 0L) {
            cancel()
            return true
        }
        mutable.value = mutable.value.copy(remainingMs = remaining)
        return false
    }
}
