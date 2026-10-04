package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume

data class VideoAmbientSample(val mediaId: String = "", val pixels: List<Int> = emptyList())

/** Read the displayed frame sparsely; never decode a proxy or constrain playback. */
object VideoAmbientColors {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(VideoAmbientSample())
    val state = mutable.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (isActive) {
                val candidate = VideoSurfaceOwner.ambientSource()
                if (candidate != null && !com.local.listentomusic.ui.ListScrollBudget.scrolling.value) {
                    val (id, view) = candidate
                    val pixels = try { copyPixels(view) } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: RuntimeException) { null }
                    if (pixels != null && VideoSurfaceOwner.ambientSource()?.first == id)
                        mutable.value = VideoAmbientSample(id, pixels.toList())
                }
                delay(900L)
            }
        }
    }
    private suspend fun copyPixels(view: View): IntArray? {
        if (!view.isShown || view.width == 0 || view.height == 0) return null
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { continuation ->
            var finished = false
            fun finish(success: Boolean) {
                if (finished) return
                finished = true
                val pixels = if (success) IntArray(576).also { bitmap.getPixels(it, 0, 24, 0, 0, 24, 24) } else null
                bitmap.recycle()
                if (continuation.isActive) continuation.resume(pixels)
            }
            try {
                when (view) {
                    is SurfaceView -> if (view.holder.surface.isValid)
                        PixelCopy.request(view, bitmap, { finish(it == PixelCopy.SUCCESS) }, handler)
                        else finish(false)
                    is TextureView -> finish(view.getBitmap(bitmap) != null)
                    else -> finish(false)
                }
            } catch (_: RuntimeException) { finish(false) }
            // PixelCopy owns the destination until its callback; cancellation must
            // not recycle a buffer while the compositor is writing to it.
        }
    }
}
