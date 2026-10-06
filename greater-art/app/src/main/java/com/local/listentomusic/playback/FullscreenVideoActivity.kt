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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.NowPlayingScreen
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import com.local.listentomusic.ui.theme.GreaterArtTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A real Activity is required to rotate video; a system overlay cannot request orientation. */
class FullscreenVideoActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var returnDestination = "EXPANDED"
    private var returnDispatched = false
    private var handoffCompletionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.useSessionPresentationOnly()
        enforceImmersiveSystemBars()
        setContent {
            val playback by viewModel.playback.collectAsState()
            val queue by viewModel.queue.collectAsState()
            val controller by viewModel.controller.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val sleepTimer by viewModel.sleepTimer.collectAsState()
            GreaterArtTheme(settings.themeMode, settings.colorTheme, settings.appFont, settings.silianRail) {
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
                    onLockTemporaryDoubleSpeed = viewModel::lockTemporaryDoubleSpeed,
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
        enforceImmersiveSystemBars()
        // Fullscreen is a real Activity, not a system overlay. Track it explicitly so
        // a transient ON_PAUSE (shade/dialog/OEM transition) cannot hand the primary
        // surface back to the hidden Mini Window while this Activity still owns video.
        VideoSurfaceOwner.setFullscreenActivityVisible(true)
        VideoSurfaceOwner.setActivityForeground(true)
        VideoSurfaceOwner.setPresentation(nowPlayingVisible = true, pictureInPicture = false)
        completeFullscreenHandoff()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enforceImmersiveSystemBars()
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
        handoffCompletionJob?.cancel()
        // Any terminal destruction must release fullscreen suppression. Android/OEM
        // recreation is the only case where ownership should intentionally survive.
        if (!isChangingConfigurations) dispatchReturn()
        super.onDestroy()
    }

    private fun enforceImmersiveSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Clear the service's fullscreen suppression before Android tears this Activity down. */
    private fun finishTo(destination: String) {
        returnDestination = destination
        dispatchReturn()
        finish()
    }

    private fun completeFullscreenHandoff() {
        handoffCompletionJob?.cancel()
        handoffCompletionJob = lifecycleScope.launch {
            // The launch handoff only bridges the old overlay surface to this Activity.
            // Once the fullscreen PlayerView has produced a frame, explicit fullscreen
            // ownership keeps NOW_PLAYING authoritative without leaving a stale handoff.
            withTimeoutOrNull(1_500L) {
                VideoSurfaceOwner.state.first { it.owner == "NOW_PLAYING" && it.firstFrame }
            }
            if (!returnDispatched) VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
        }
    }

    private fun dispatchReturn() {
        if (returnDispatched) return
        returnDispatched = true
        handoffCompletionJob?.cancel()
        VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
        VideoSurfaceOwner.setFullscreenActivityVisible(false)
        val delivered = runCatching {
            startService(Intent(this, MiniWindowOverlayService::class.java).apply {
                action = MiniWindowOverlayService.ACTION_FULLSCREEN_RETURN
                putExtra(MiniWindowOverlayService.EXTRA_FULLSCREEN_DESTINATION, returnDestination)
            })
        }.isSuccess
        if (!delivered) {
            // Allow onDestroy() to retry instead of permanently suppressing the player.
            returnDispatched = false
            VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
        }
    }
}
