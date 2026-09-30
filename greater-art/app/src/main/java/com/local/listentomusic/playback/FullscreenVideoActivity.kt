package com.local.listentomusic.playback

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.NowPlayingScreen
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import com.local.listentomusic.ui.theme.GreaterArtTheme

/** A real Activity is required to rotate video; a system overlay cannot request orientation. */
class FullscreenVideoActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var returnDestination = "EXPANDED"
    private var returnDispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.useSessionPresentationOnly()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val playback by viewModel.playback.collectAsState()
            val queue by viewModel.queue.collectAsState()
            val controller by viewModel.controller.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val sleepTimer by viewModel.sleepTimer.collectAsState()
            GreaterArtTheme(settings.themeMode, settings.appFont, settings.silianRail) {
                NowPlayingScreen(
                    playback = playback,
                    artwork = null,
                    queue = queue,
                    lyrics = null,
                    showFileDetails = settings.showFileDetails,
                    editableQueue = settings.editableQueue,
                    blackDiscMode = settings.blackDiscMode,
                    language = settings.appLanguage,
                    controller = controller,
                    contentPadding = PaddingValues(0.dp),
                    isPictureInPicture = false,
                    onVideoBoundsChanged = {},
                    onPictureInPicture = { finishTo("DETACHED") },
                    onHome = {
                        returnDestination = "DOCKED"
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        })
                        finishTo("DOCKED")
                    },
                    onClose = { finishTo("DETACHED") },
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
                    onShareCurrentMedia = {},
                    onShareQueue = {},
                    initialFullscreen = true,
                    forceLandscapeFullscreen = true,
                    onFullscreenChanged = { if (!it) finishTo("EXPANDED") },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        VideoSurfaceOwner.setActivityForeground(true)
        VideoSurfaceOwner.setPresentation(nowPlayingVisible = true, pictureInPicture = false)
    }

    override fun onPause() {
        VideoSurfaceOwner.setActivityForeground(false)
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        finishTo("DETACHED")
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) dispatchReturn()
        super.onDestroy()
    }

    /** Clear the service's fullscreen suppression before Android tears this Activity down. */
    private fun finishTo(destination: String) {
        returnDestination = destination
        dispatchReturn()
        finish()
    }

    private fun dispatchReturn() {
        if (returnDispatched) return
        returnDispatched = true
        VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
        startService(Intent(this, MiniWindowOverlayService::class.java).apply {
            action = MiniWindowOverlayService.ACTION_FULLSCREEN_RETURN
            putExtra(MiniWindowOverlayService.EXTRA_FULLSCREEN_DESTINATION, returnDestination)
        })
    }
}
