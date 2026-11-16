package com.local.listentomusic.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.local.listentomusic.ui.components.VideoAmbientColors
import com.local.listentomusic.ui.components.analyzeAmbient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One palette for Compose and native compact hosts. All mutation is on main. */
internal object VideoThemeColors {
    private val cache = VideoThemePalette()
    private val mutable = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val consumers = mutableSetOf<Any>()
    private var collector: Job? = null
    private var animation: Job? = null
    private var started = false
    val requested: Boolean get() = consumers.isNotEmpty()

    fun colors(mediaId: String): List<Int> = cache.colors(mediaId)

    fun retain(context: Context, consumer: Any) {
        if (!consumers.add(consumer)) return
        if (!started) {
            started = true
            val app = context.applicationContext
            val power = app.getSystemService(PowerManager::class.java)
            fun updatePower() {
                cache.powerSaving(power.isPowerSaveMode, SystemClock.uptimeMillis())
                publish()
            }
            ContextCompat.registerReceiver(app, object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) { updatePower() }
            }, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            updatePower()
        }
        if (collector == null) collector = scope.launch {
            VideoAmbientColors.state.collect { sample ->
                if (sample.mediaId.isBlank() || sample.pixels.isEmpty()) return@collect
                val signal = withContext(Dispatchers.Default) { analyzeAmbient(sample.pixels, emptyList()) }
                val average = averageVideoColor(sample.pixels) ?: return@collect
                // A black transition is not a new scene palette. Keep the last readable accent.
                if (signal.brightness < .025f) return@collect
                cache.sample(sample.mediaId, signal.colors, average, sample.capturedAtMs, SystemClock.uptimeMillis())
                publish()
                animate()
            }
        }
    }

    fun release(consumer: Any) {
        consumers.remove(consumer)
        if (requested) return
        collector?.cancel()
        collector = null
        animation?.cancel()
        animation = null
        cache.freeze(SystemClock.uptimeMillis())
        publish()
    }

    fun artwork(mediaId: String, palette: List<Int>) {
        cache.seedArtwork(mediaId, palette)
        publish()
    }

    private fun publish() { mutable.value = cache.snapshot() }
    private fun animate() {
        if (!cache.moving() || animation?.isActive == true) return
        animation = scope.launch {
            // Bound theme invalidation to 10Hz, not a whole MaterialTheme update each frame.
            // Sleeping/paused/frozen palettes have no periodic animation worker.
            while (isActive && requested && cache.moving()) {
                delay(100)
                cache.tick(SystemClock.uptimeMillis())
                publish()
            }
        }
    }
}
