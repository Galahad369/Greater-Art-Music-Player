package com.local.listentomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.playback.StackSession
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.ceil
import kotlin.math.min

internal data class StackVideoTile(val path: String, val primary: Boolean, val unavailable: Boolean)

internal fun stackVideoTiles(session: StackSession): List<StackVideoTile> = session.slots
    .filter { it.file.kind == MediaKind.VIDEO && it.error == null }
    .map { StackVideoTile(it.file.path, it.file.path == session.primaryPath, it.videoUnavailable) }

internal fun shouldTileStackBackground(mode: AppBackgroundMode, scale: BackgroundScaleMode,
    visible: Boolean, foreground: Boolean, allowed: Boolean, controllerAvailable: Boolean, videoCount: Int): Boolean =
    mode == AppBackgroundMode.CURRENT_VIDEO && scale == BackgroundScaleMode.FIT &&
        visible && foreground && allowed && controllerAvailable && videoCount >= 2

/** Maximize fitted image area, including empty last-row cells, without any stretching. */
internal fun stackTileColumns(count: Int, viewportAspect: Float): Int {
    if (count < 2) return 1
    val aspect = viewportAspect.takeIf { it.isFinite() && it > 0f } ?: 1f
    return (1..min(count, 4)).maxBy { columns ->
        val rows = ceil(count.toDouble() / columns).toInt()
        val cellAspect = aspect * rows / columns
        val fit = min(cellAspect / (16f / 9f), (16f / 9f) / cellAspect)
        fit * count / (columns * rows)
    }
}

@Composable
internal fun rememberStackVideoTiles(): List<StackVideoTile> {
    // Position/drift ticks must not recompose six native views or rebuild their grid.
    val flow = remember { StackPlayback.state.map(::stackVideoTiles).distinctUntilChanged() }
    val tiles by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    return tiles
}

/** Tiles follow Stack's audio clock; controls/audio never wait for decorative video. */
@Composable
internal fun StackVideoBackground(tiles: List<StackVideoTile>, controller: MediaController) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val columns = stackTileColumns(tiles.size, maxWidth.value / maxHeight.value)
        val rows = tiles.chunked(columns)
        Column(Modifier.fillMaxSize()) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { tile ->
                        key(tile.path, tile.primary) {
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                if (tile.primary) PrimaryVideoBackground(controller, true, BackgroundScaleMode.FIT, null)
                                else if (!tile.unavailable) StackCompanionVideo(tile.path)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun StackCompanionVideo(path: String) {
    val owner = remember(path) { Any() }
    DisposableEffect(path, owner) {
        onDispose { StackPlayback.attachVideo(path, owner, null) }
    }
    AndroidView(
        factory = { context ->
            (android.view.LayoutInflater.from(context).inflate(com.local.listentomusic.R.layout.background_video,
                android.widget.FrameLayout(context), false) as PlayerView).apply {
                useController = false
                isClickable = false
                isFocusable = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setKeepContentOnPlayerReset(true)
                StackPlayback.attachVideo(path, owner, this)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
