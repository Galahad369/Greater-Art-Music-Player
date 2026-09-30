package com.local.listentomusic.playback

import android.content.Intent
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.media3.ui.PlayerView
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.model.mediaFileFromSession
import com.local.listentomusic.ui.AndroidShare
import com.local.listentomusic.ui.DeveloperDiagnostics
import com.local.listentomusic.ui.NowPlayingScreen
import com.local.listentomusic.ui.UiInspectorHost
import com.local.listentomusic.ui.UiInspectorState
import com.local.listentomusic.ui.inspectElement
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.ui.uiText
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Expanded chrome for the persistent WindowManager player. The video view is borrowed, never created here. */
@Composable
internal fun PlayerWindowExpandedContent(
    viewModel: MainViewModel,
    videoView: PlayerView?,
    scope: CoroutineScope,
    onVideoReleased: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
    onShrink: () -> Unit,
    onFullscreen: (Boolean) -> Unit,
    onPull: (Float) -> Unit,
    onPullEnd: () -> Unit,
    onPullCancel: () -> Unit,
    onShare: (Intent) -> Unit,
    onShareFailure: (String) -> Unit,
) {
    val playback by viewModel.playback.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val library by viewModel.library.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val controller by viewModel.controller.collectAsState()
    val sleepTimer by viewModel.sleepTimer.collectAsState()
    val inspector = remember { UiInspectorState() }
    var fullscreen by remember { mutableStateOf(false) }
    var addToListFile by remember { mutableStateOf<com.local.listentomusic.model.MediaFile?>(null) }
    val artwork by produceState<android.graphics.Bitmap?>(null, playback.currentPath,
        settings.localOverrides[playback.currentPath]) {
        value = viewModel.loadCurrentArtwork(playback.currentPath)
    }
    val lyrics by produceState<com.local.listentomusic.model.LocalLyrics?>(null, playback.currentPath) {
        value = viewModel.loadLyrics(playback.currentPath)
    }

    GreaterArtTheme(settings.themeMode, settings.appFont, settings.silianRail) {
        UiInspectorHost(settings.developerMode, inspector) {
            Surface(color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        NowPlayingScreen(
                            playback = playback,
                            artwork = artwork,
                            queue = queue,
                            lyrics = lyrics,
                            showFileDetails = settings.showFileDetails,
                            editableQueue = settings.editableQueue,
                            blackDiscMode = settings.blackDiscMode,
                            language = settings.appLanguage,
                            controller = controller,
                            contentPadding = PaddingValues(0.dp),
                            isPictureInPicture = false,
                            onVideoBoundsChanged = { _: Rect -> },
                            onPictureInPicture = onShrink,
                            onHome = onHome,
                            onClose = onClose,
                            onTogglePlay = viewModel::togglePlayPause,
                            onPrevious = viewModel::previous,
                            onNext = viewModel::next,
                            onSeek = viewModel::seekTo,
                            onSpeed = viewModel::setSpeed,
                            onRepeat = viewModel::cycleRepeatMode,
                            onSleepTimer = viewModel::setSleepTimer,
                            sleepTimer = sleepTimer,
                            seekOffsetMs = settings.seekOffsetMs,
                            onSeekBy = viewModel::seekBy,
                            onPlayQueueItem = viewModel::playQueueItem,
                            onLoadThumbnail = viewModel::loadThumbnail,
                            onLoadWaveform = viewModel::loadWaveform,
                            onMoveQueueItem = viewModel::moveQueueItem,
                            onRemoveQueueItem = viewModel::removeQueueItem,
                            onBeginTemporaryDoubleSpeed = viewModel::beginTemporaryDoubleSpeed,
                            onEndTemporaryDoubleSpeed = viewModel::endTemporaryDoubleSpeed,
                            isFavourite = playback.currentPath in settings.favouritePaths,
                            onToggleFavourite = viewModel::toggleFavourite,
                            onShareCurrentMedia = {
                                val media = queue.firstOrNull { it.path == playback.currentPath }
                                    ?: library.files.firstOrNull { it.path == playback.currentPath }
                                    ?: controller?.currentMediaItem?.let(::mediaFileFromSession)
                                if (media == null) onShareFailure(uiText(settings.appLanguage,
                                    "Media unavailable", "媒體不可用"))
                                else AndroidShare.mediaChooser(viewModel.getApplication(), media,
                                    uiText(settings.appLanguage, "Share media file", "分享媒體檔案"))
                                    .onSuccess(onShare).onFailure { onShareFailure(it.message.orEmpty()) }
                            },
                            onAddQueueItemToList = { addToListFile = it },
                            onShareQueue = {
                                scope.launch {
                                    AndroidShare.listChooser(viewModel.getApplication(),
                                        uiText(settings.appLanguage, "Current queue", "目前播放佇列"), queue,
                                        uiText(settings.appLanguage, "Share current queue", "分享目前播放佇列"))
                                        .onSuccess(onShare).onFailure { onShareFailure(it.message.orEmpty()) }
                                }
                            },
                            systemOverlay = true,
                            onFullscreenChanged = { fullscreen = it; onFullscreen(it) },
                            sharedVideoView = videoView,
                            onSharedVideoReleased = onVideoReleased,
                        )
                    }
                }
            }
            addToListFile?.let { file ->
                val manualLists = settings.playlists.filter { it.rule == null }
                val favourite = file.path in settings.favouritePaths
                AlertDialog(
                    onDismissRequest = { addToListFile = null },
                    title = { Text(uiText(settings.appLanguage, "Add to list", "加入列表")) },
                    text = {
                        Column {
                            TextButton(
                                enabled = !favourite,
                                onClick = {
                                    viewModel.toggleFavourite(file.path)
                                    addToListFile = null
                                },
                            ) {
                                Text(if (favourite)
                                    uiText(settings.appLanguage, "✓ Favorites", "✓ 我的最愛")
                                else uiText(settings.appLanguage, "Favorites", "我的最愛"))
                            }
                            manualLists.forEach { playlist ->
                                val added = file.path in playlist.paths
                                TextButton(
                                    enabled = !added,
                                    onClick = {
                                        viewModel.addToPlaylist(playlist.id, file.path)
                                        addToListFile = null
                                    },
                                ) {
                                    Text(if (added) "✓  ${playlist.name}" else playlist.name)
                                }
                            }
                            if (manualLists.isEmpty()) {
                                Text(
                                    uiText(settings.appLanguage,
                                        "No manual playlists yet. Favorites is still available above.",
                                        "尚未有手動播放清單；你仍可加入上方的我的最愛。"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { addToListFile = null }) {
                            Text(uiText(settings.appLanguage, "Done", "完成"))
                        }
                    },
                )
            }
            if (settings.developerMode) {
                val engine by PlaybackDiagnostics.report.collectAsState()
                DeveloperDiagnostics(
                    report = "version=${com.local.listentomusic.BuildConfig.VERSION_NAME}\nscreen=NOW_PLAYING\n" +
                        "playing=${playback.isPlaying} playerWindowMode=EXPANDED\n" +
                        "controllerId=${System.identityHashCode(controller)} playerViewId=${System.identityHashCode(videoView)}\n" +
                        "position=${playback.positionMs} duration=${playback.durationMs} queue=${queue.size}\n" +
                        VideoSurfaceOwner.describe() + "\nAUDIO ENGINE\n" + engine,
                    regions = inspector.regions.values.map { "${it.label}: ${it.detail}" },
                    warning = playback.errorMessage != null,
                    inspector = inspector,
                    systemOverlay = true,
                )
            }
        }
    }
}
