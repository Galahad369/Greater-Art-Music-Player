package com.local.listentomusic.playback

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.usesAndroidPip
import com.local.listentomusic.ui.NowPlayingScreen
import com.local.listentomusic.ui.enforceImmersiveWindow
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
    private var pipVisible by mutableStateOf(false)
    private var pipTransitionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.useSessionPresentationOnly()
        if (intent.getBooleanExtra(EXTRA_START_PIP, false)) returnDestination = "DETACHED"
        // Fullscreen is a separate Activity; its own window must stay awake.
        // This applies to paused playback and is released when fullscreen closes.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enforceImmersiveSystemBars()
        setContent {
            val playback by viewModel.playback.collectAsState()
            val queue by viewModel.queue.collectAsState()
            val controller by viewModel.controller.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val artwork by produceState<android.graphics.Bitmap?>(null, playback.currentPath,
                settings.backgroundMode, settings.localOverrides[playback.currentPath]) {
                value = null
                if (settings.backgroundMode == com.local.listentomusic.data.AppBackgroundMode.ADAPTIVE_DYNAMIC)
                    value = viewModel.loadCurrentArtwork(playback.currentPath)
            }
            val sleepTimer by viewModel.sleepTimer.collectAsState()
            GreaterArtTheme(settings.themeMode, settings.colorTheme, settings.appFont, settings.silianRail) {
                NowPlayingScreen(
                    floatingWindowMode = settings.floatingWindowMode,
                    onFloatingWindowModeChange = viewModel::setFloatingWindowMode,
                    playback = playback,
                    artwork = artwork,
                    ambientPreferences = settings,
                    queue = queue,
                    lyrics = null,
                    showFileDetails = settings.showFileDetails,
                    editableQueue = settings.editableQueue,
                    blackDiscMode = settings.blackDiscMode,
                    language = settings.appLanguage,
                    controller = controller,
                    contentPadding = PaddingValues(0.dp),
                    isPictureInPicture = pipVisible,
                    onVideoBoundsChanged = {},
                    onPictureInPicture = {
                        if (settings.floatingWindowMode.usesAndroidPip() && playback.isVideo) enterNativePip()
                        else finishTo("DETACHED")
                    },
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
                    onFullscreenChanged = { if (!it) finishTo("EXPANDED") },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInPictureInPictureMode) enforceImmersiveSystemBars()
        // Fullscreen is a real Activity, not a system overlay. Track it explicitly so
        // a transient ON_PAUSE (shade/dialog/OEM transition) cannot hand the primary
        // surface back to the hidden Mini Window while this Activity still owns video.
        VideoSurfaceOwner.setFullscreenActivityVisible(true)
        VideoSurfaceOwner.setActivityForeground(true)
        VideoSurfaceOwner.setPresentation(nowPlayingVisible = true, pictureInPicture = false)
        completeFullscreenHandoff()
        if (intent.getBooleanExtra(EXTRA_START_PIP, false)) {
            intent.removeExtra(EXTRA_START_PIP)
            // A fresh presentation-only ViewModel connects asynchronously. Entering
            // from the first onResume can see no media and silently lose the request.
            lifecycleScope.launch {
                val ready = withTimeoutOrNull(8_000L) {
                    viewModel.playback.first { it.isVideo }
                    VideoSurfaceOwner.state.first { it.owner == "NOW_PLAYING" && it.firstFrame }
                } != null
                if (!isFinishing && !isInPictureInPictureMode) {
                    if (ready) enterNativePip() else finishTo("DETACHED")
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipVisible = isInPictureInPictureMode
        pipTransitionRequested = isInPictureInPictureMode
        if (isInPictureInPictureMode) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        VideoSurfaceOwner.setPresentation(nowPlayingVisible = true, pictureInPicture = isInPictureInPictureMode)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isInPictureInPictureMode) enforceImmersiveSystemBars()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // This Activity handles orientation/configuration changes without recreation.
        // Re-apply after the new landscape geometry has been attached so OEM window
        // managers cannot leave a status-bar/cutout strip reserved on the rotated edge.
        window.decorView.post {
            if (!isInPictureInPictureMode) enforceImmersiveSystemBars()
            ViewCompat.requestApplyInsets(window.decorView)
        }
    }

    override fun onPause() {
        VideoSurfaceOwner.setActivityForeground(false)
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pipTransitionRequested || isInPictureInPictureMode) return
        val settings = viewModel.settings.value
        if (settings.autoPictureInPicture && settings.floatingWindowMode.usesAndroidPip() &&
            viewModel.playback.value.isVideo && viewModel.playback.value.isPlaying) {
            enterNativePip()
        } else {
            finishTo("DETACHED")
        }
    }

    override fun onDestroy() {
        handoffCompletionJob?.cancel()
        // Any terminal destruction must release fullscreen suppression. Android/OEM
        // recreation is the only case where ownership should intentionally survive.
        if (!isChangingConfigurations) dispatchReturn()
        super.onDestroy()
    }

    private fun enforceImmersiveSystemBars() {
        enforceImmersiveWindow(window)
    }

    private fun enterNativePip() {
        if (isInPictureInPictureMode || pipTransitionRequested || !viewModel.playback.value.isVideo) return
        pipTransitionRequested = true
        returnDestination = "DETACHED"
        val ratio = viewModel.playback.value.videoAspectRatio.coerceIn(.5f, 2f)
        val accepted = runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational((ratio * 1_000).toInt(), 1_000))
                    .build()
            )
        }.getOrDefault(false)
        if (!accepted) {
            pipTransitionRequested = false
            finishTo("DETACHED")
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
        // Native PiP / background-only exit must never resurrect a detached overlay.
        if (returnDestination == "DETACHED" &&
            viewModel.settings.value.floatingWindowMode != FloatingWindowMode.MINI_WINDOW) {
            stopService(Intent(this, MiniWindowOverlayService::class.java))
            VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            return
        }
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
    companion object {
        const val EXTRA_START_PIP = "com.local.listentomusic.player.START_PIP"
    }

}
