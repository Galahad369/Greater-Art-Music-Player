package com.local.listentomusic.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.VideoAmbientColors
import com.local.listentomusic.ui.components.adaptiveArtworkPixels
import com.local.listentomusic.ui.components.analyzeAmbient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

internal fun artworkThemePalette(artwork: Bitmap?): List<Int> {
    val pixels = try { adaptiveArtworkPixels(artwork) } catch (_: RuntimeException) { emptyList() }
    return analyzeAmbient(pixels, emptyList()).takeIf { it.saturation >= .12f }?.colors.orEmpty()
}

/** Reuse cached current artwork or the existing sparse video samples; no new decoder/PixelCopy loop. */
@Composable
internal fun MediaAdaptiveTheme(preferences: UserPreferences, mediaId: String?, artwork: Bitmap?,
    content: @Composable () -> Unit) {
    val followsVideo = preferences.colorTheme == ColorTheme.VIDEO
    val enabled = mediaThemeEnabled(preferences, mediaId)
    val useVideo = followsVideo || preferences.adaptiveReactToVideo
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val videoPalette by remember(mediaId) {
        VideoThemeColors.state.map { it[mediaId].orEmpty() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = VideoThemeColors.colors(mediaId.orEmpty()))
    DisposableEffect(enabled, followsVideo, owner) {
        val consumer = Any()
        fun update() {
            if (enabled && followsVideo && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                VideoThemeColors.retain(context, consumer)
            else VideoThemeColors.release(consumer)
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        owner.lifecycle.addObserver(observer)
        update()
        onDispose { owner.lifecycle.removeObserver(observer); VideoThemeColors.release(consumer) }
    }
    var palette by remember(mediaId, artwork, enabled, useVideo, followsVideo) {
        mutableStateOf<List<Int>>(emptyList())
    }
    LaunchedEffect(mediaId, artwork, enabled, useVideo, followsVideo, owner) {
        if (!enabled) return@LaunchedEffect
        val fromArtwork = withContext(Dispatchers.Default) {
            artworkThemePalette(artwork)
        }
        if (fromArtwork.isNotEmpty()) {
            if (followsVideo) {
                VideoThemeColors.artwork(mediaId.orEmpty(), fromArtwork)
                return@LaunchedEffect
            }
            palette = fromArtwork
            return@LaunchedEffect
        }
        if (followsVideo) return@LaunchedEffect
        if (!useVideo) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (palette.isNotEmpty() && !followsVideo) return@repeatOnLifecycle
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
        val intensity = if (followsVideo) 1f else preferences.adaptiveIntensity
        val appliedPalette = if (followsVideo) videoPalette else palette
        val colors = remember(base, appliedPalette, intensity, preferences.colorTheme) {
            artworkAccentScheme(base, appliedPalette, intensity, preferences.colorTheme)
        }
        MaterialTheme(colorScheme = colors, content = content)
    }
}
