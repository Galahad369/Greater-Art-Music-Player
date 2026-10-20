package com.local.listentomusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** Shared by the three player presentations; only the final borrower releases it. */
internal object PlaybackConnection {
    private val pool = SharedPlaybackResource<ListenableFuture<MediaController>>(MediaController::releaseFuture)

    @MainThread
    fun acquire(context: Context): SharedPlaybackResource.Lease<ListenableFuture<MediaController>> {
        fun create(): ListenableFuture<MediaController> {
            val app = context.applicationContext
            return MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
        }
        val lease = pool.acquire(::create)
        if (!lease.value.isDone || runCatching { lease.value.get() }.isSuccess) return lease
        // Another presentation may still own the failed future. Its lease must
        // not pin every later user-requested reconnect to the same failure.
        pool.invalidate(lease.value)
        lease.close()
        return pool.acquire(::create)
    }
}

/** Main-thread lease ownership, independent of Android so lifetime rules are testable. */
internal class SharedPlaybackResource<T>(private val release: (T) -> Unit) {
    private class Entry<T>(val value: T, var borrowers: Int = 0)
    private var current: Entry<T>? = null

    fun acquire(create: () -> T): Lease<T> {
        val entry = current ?: Entry(create()).also { current = it }
        entry.borrowers++
        return Lease(entry.value) {
            entry.borrowers--
            if (entry.borrowers == 0) {
                if (current === entry) current = null
                release(entry.value)
            }
        }
    }

    fun invalidate(value: T) { if (current?.value === value) current = null }

    class Lease<T>(val value: T, private val onClose: () -> Unit) : AutoCloseable {
        var closed = false
            private set
        override fun close() {
            if (closed) return
            closed = true
            onClose()
        }
    }
}
