package com.local.listentomusic.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/** Tiny cached-art sampling on a worker. Never performed by the animation draw loop. */
internal fun adaptiveArtworkPixels(artwork: Bitmap?): List<Int> {
    if (artwork == null || artwork.isRecycled || artwork.width <= 0 || artwork.height <= 0) return emptyList()
    val readable = if (artwork.config == Bitmap.Config.HARDWARE)
        artwork.copy(Bitmap.Config.ARGB_8888, false) else artwork
    return try {
        if (readable == null) emptyList() else List(144) { index ->
            readable.getPixel(index % 12 * readable.width / 12, index / 12 * readable.height / 12)
        }
    } finally { if (readable !== artwork) readable?.recycle() }
}

@Composable
private fun ambientSystemConstraints(): Pair<Boolean, Boolean> {
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var reduced by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(power.isPowerSaveMode) }
    DisposableEffect(context, power) {
        fun refresh() {
            reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= 0f
            saving = power.isPowerSaveMode
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        refresh()
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return reduced to saving
}

/** Four bounded gradient passes. Reads frame state only in draw; no blur, Surface or decoder. */
@Composable
internal fun Modifier.adaptiveAmbientBackdrop(
    artwork: Bitmap?, mediaId: String?, preferences: UserPreferences,
    visible: Boolean = true, scrolling: Boolean = false,
    extraDim: Float = 0f,
): Modifier {
    val theme = MaterialTheme.colorScheme
    val light = theme.background.luminance() > .5f
    val fallback = remember(theme.background, theme.primary, theme.secondary) {
        listOf(lerp(theme.background, theme.primary, .12f).toArgb(),
            lerp(theme.background, theme.secondary, .08f).toArgb(), theme.background.toArgb())
    }
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val (reduced, saving) = ambientSystemConstraints()
    val active = visible && resumed
    val artPixels by produceState<List<Int>>(emptyList(), artwork, mediaId, active) {
        value = emptyList()
        if (active) value = withContext(Dispatchers.Default) {
            runCatching { adaptiveArtworkPixels(artwork) }.getOrDefault(emptyList())
        }
    }
    val inputActive = active && !scrolling && !saving
    val signal by produceState(SharedAdaptiveAmbient.cache.snapshot(mediaId.orEmpty(), fallback),
        artPixels, mediaId, inputActive, fallback) {
        value = SharedAdaptiveAmbient.cache.snapshot(mediaId.orEmpty(), fallback)
        suspend fun refreshSignal() {
            val video = VideoAmbientColors.state.value
            value = withContext(Dispatchers.Default) {
                val pixels = ambientInputPixels(mediaId, video.mediaId, video.pixels, artPixels)
                SharedAdaptiveAmbient.cache.update(mediaId.orEmpty(), pixels, fallback, SystemClock.uptimeMillis())
            }
        }
        if (inputActive) {
            refreshSignal()
            while (isActive) {
                // Mark subsequent sampling as continuous decorative work so Compose
                // test clocks can settle finite palette transitions instead of chasing
                // a new 1.9s fade every simulated 900ms forever.
                withInfiniteAnimationFrameNanos { }
                delay(900L)
                refreshSignal()
            }
        }
    }
    val layers = ambientLayers(signal, preferences.adaptiveIntensity,
        preferences.adaptiveReactToVideo, preferences.adaptiveMotion)
    val fade = if (reduced) snap<Color>() else tween(1600)
    val base = theme.background
    // Intensity cannot bypass contrast: layer tint is capped before drawing.
    val tint = if (light) .10f else .38f
    val top = animateColorAsState(lerp(base, Color(signal.colors[0]), tint * ambientUnit(preferences.adaptiveIntensity)), fade, label = "adaptive-top")
    val middle = animateColorAsState(lerp(base, Color(signal.colors[1]), tint * .7f * ambientUnit(preferences.adaptiveIntensity)), fade, label = "adaptive-middle")
    val bottom = animateColorAsState(lerp(base, Color(signal.colors[2]), tint * .35f * ambientUnit(preferences.adaptiveIntensity)), fade, label = "adaptive-bottom")
    val glow = animateColorAsState(Color(signal.colors[0]), fade, label = "adaptive-glow")
    val lightBand = animateColorAsState(Color(signal.colors[1]), fade, label = "adaptive-light-band")
    val flowing = animateColorAsState(Color(signal.colors[2]), fade, label = "adaptive-flow-color")
    val strengthSpec = if (reduced) snap<Float>() else tween(1900)
    val aurora = animateFloatAsState(layers.aurora, strengthSpec, label = "adaptive-aurora")
    val bloom = animateFloatAsState(layers.bloom, strengthSpec, label = "adaptive-bloom")
    val flow = animateFloatAsState(layers.flow, strengthSpec, label = "adaptive-flow")
    val movement = animateFloatAsState(layers.motion, strengthSpec, label = "adaptive-motion")
    val phase = remember { mutableStateOf(SystemClock.uptimeMillis() / 45000.0) }
    val animate = ambientShouldAnimate(visible, resumed, scrolling, reduced, saving, preferences.adaptiveMotion)
    LaunchedEffect(animate) {
        if (animate) while (isActive) withInfiniteAnimationFrameNanos {
            // Global monotonic phase keeps geometry continuous across navigation/orientation.
            phase.value = SystemClock.uptimeMillis() / 45000.0
        }
    }
    return drawBehind {
        if (size.width <= 0f || size.height <= 0f) return@drawBehind
        val p = phase.value
        val m = movement.value
        val width = size.width; val height = size.height
        // Classic base is always drawn.
        drawRect(Brush.verticalGradient(listOf(top.value, middle.value, bottom.value)))
        // Aurora: broad crossed lights, deterministic low-frequency movement.
        val center = Offset(width * (.5f + sin(p).toFloat() * .19f * m), height * (.30f + cos(p * .73).toFloat() * .12f * m))
        val radius = maxOf(width, height) * .92f
        drawRect(Brush.radialGradient(listOf(lightBand.value.copy(alpha = aurora.value), Color.Transparent), center, radius))
        // Bloom: localized glow tracks smoothed brightness/chroma, not HDR or beats.
        drawRect(Brush.radialGradient(listOf(glow.value.copy(alpha = bloom.value), Color.Transparent),
            Offset(width * (.68f + cos(p * .61).toFloat() * .10f * m), height * .56f), radius * .58f))
        // Flow: evolving diagonal gradient, independent from the Aurora geometry.
        drawRect(Brush.linearGradient(listOf(Color.Transparent, flowing.value.copy(alpha = flow.value), Color.Transparent),
            Offset(-width * .25f, height * (.2f + sin(p * .43).toFloat() * .15f * m)),
            Offset(width * 1.25f, height * .88f)))
        // Fixed contrast veil remains independent of user intensity and background dimming.
        drawRect((if (light) Color.White else Color.Black).copy(alpha = .12f + ambientUnit(extraDim, 0f) * .88f))
    }
}
