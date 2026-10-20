package com.local.listentomusic.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.VideoAmbientColors
import com.local.listentomusic.ui.components.adaptiveArtworkPixels
import com.local.listentomusic.ui.components.analyzeAmbient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Reuse cached current artwork or the existing sparse video samples; no new decoder/PixelCopy loop. */
@Composable
internal fun MediaAdaptiveTheme(preferences: UserPreferences, mediaId: String?, artwork: Bitmap?,
    content: @Composable () -> Unit) {
    val enabled = mediaId != null && preferences.backgroundMode in
        listOf(AppBackgroundMode.AMBIENT, AppBackgroundMode.ADAPTIVE_DYNAMIC) &&
        preferences.colorTheme !in listOf(ColorTheme.HIGH_CONTRAST, ColorTheme.MONOCHROME) &&
        preferences.adaptiveIntensity.isFinite() && preferences.adaptiveIntensity > 0f
    val owner = LocalLifecycleOwner.current
    var palette by remember(mediaId, artwork, enabled, preferences.adaptiveReactToVideo) {
        mutableStateOf<List<Int>>(emptyList())
    }
    LaunchedEffect(mediaId, artwork, enabled, preferences.adaptiveReactToVideo, owner) {
        if (!enabled) return@LaunchedEffect
        val fromArtwork = withContext(Dispatchers.Default) {
            val pixels = try { adaptiveArtworkPixels(artwork) } catch (_: RuntimeException) { emptyList() }
            analyzeAmbient(pixels, emptyList()).takeIf { it.saturation >= .12f }?.colors.orEmpty()
        }
        if (fromArtwork.isNotEmpty()) { palette = fromArtwork; return@LaunchedEffect }
        if (!preferences.adaptiveReactToVideo) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (palette.isNotEmpty()) return@repeatOnLifecycle
            // Latch one coherent palette per media/presentation. Do not recolor the
            // entire Compose tree every frame or let a previous video's colors leak.
            palette = VideoAmbientColors.state.map { sample ->
                if (sample.mediaId != mediaId || sample.pixels.isEmpty()) emptyList() else
                    withContext(Dispatchers.Default) {
                        analyzeAmbient(sample.pixels, emptyList()).takeIf { it.saturation >= .12f }?.colors.orEmpty()
                    }
            }.first { it.isNotEmpty() }
        }
    }
    GreaterArtTheme(preferences.themeMode, preferences.colorTheme, preferences.appFont, preferences.silianRail) {
        val base = MaterialTheme.colorScheme
        val colors = remember(base, palette, preferences.adaptiveIntensity, preferences.colorTheme) {
            artworkAccentScheme(base, palette, preferences.adaptiveIntensity, preferences.colorTheme)
        }
        MaterialTheme(colorScheme = colors, content = content)
    }
}
