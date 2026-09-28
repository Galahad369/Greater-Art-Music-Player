package com.local.listentomusic

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.ui.GreaterArtApp
import com.local.listentomusic.ui.uiText
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.playback.MiniWindowOverlayService
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val backgroundPlayer = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_BACKGROUND_PLAYER) moveTaskToBack(true)
        }
    }
    private val viewModel: MainViewModel by viewModels()
    private var isPictureInPicture by mutableStateOf(false)
    private var openPlayerRequest by mutableIntStateOf(0)
    private var playerScreenVisible = false
    private var libraryScreenVisible = true
    private var videoSourceRect = Rect()
    private var returnScan: kotlinx.coroutines.Job? = null
    private var miniWindowReturnJob: kotlinx.coroutines.Job? = null
    private var returningFromMiniWindow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(this, backgroundPlayer, android.content.IntentFilter(ACTION_BACKGROUND_PLAYER),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.playback.value.hasMedia && startMiniWindowIfAllowed(backgroundWhenReady = true)) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
        openPlayerRequest = savedInstanceState?.getInt(STATE_OPEN_PLAYER_REQUEST) ?: 0
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Keep the display awake only while this Activity is visible. Android still
        // honors the physical power/lock key, and no wake lock survives the Activity.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handleOpenPlayerIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.playback, viewModel.settings) { playback, settings ->
                        listOf(
                            playback.isVideo,
                            playback.isPlaying,
                            (playback.videoAspectRatio * 100).roundToInt(),
                            settings.floatingWindowMode,
                        )
                    }
                    .distinctUntilChanged()
                    .collect { updatePictureInPictureParams() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playback.map { it.hasMedia }.distinctUntilChanged().collect { hasMedia ->
                    if (hasMedia && !com.local.listentomusic.ui.components.VideoSurfaceOwner.expandedOverlayActive &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity))) {
                        ContextCompat.startForegroundService(this@MainActivity,
                            Intent(this@MainActivity, MiniWindowOverlayService::class.java)
                                .setAction(MiniWindowOverlayService.ACTION_DOCK))
                    } else if (!hasMedia) {
                        stopService(Intent(this@MainActivity, MiniWindowOverlayService::class.java))
                    }
                }
            }
        }
        setContent {
            PermissionAwareApp(
                viewModel = viewModel,
                openPlayerRequest = openPlayerRequest,
                onOpenPlayerRequestConsumed = { request ->
                    if (openPlayerRequest == request) openPlayerRequest = 0
                },
                isPictureInPicture = isPictureInPicture,
                onPlayerScreenChanged = {
                    playerScreenVisible = it
                    if (it && returningFromMiniWindow) completeMiniWindowReturn()
                    updatePictureInPictureParams()
                },
                onVideoBoundsChanged = {
                    if (videoSourceRect != it) {
                        videoSourceRect = Rect(it)
                        updatePictureInPictureParams()
                    }
                },
                onLibraryScreenChanged = {
                    libraryScreenVisible = it
                    com.local.listentomusic.playback.PlayerWindowVisibility.library(
                        it && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                },
                onEnterPictureInPicture = ::enterVideoPictureInPicture,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setActivityForeground(true)
        // A Mini Window tap is a direct continuation of Now Playing. During that
        // handoff, keep the working overlay alive until the Now Playing surface is
        // registered instead of destroying it on Activity resume.
        if (returningFromMiniWindow && playerScreenVisible) {
            // If a transient pause interrupted the handoff, resume waiting for the
            // NOW_PLAYING surface instead of leaving the overlay stuck indefinitely.
            completeMiniWindowReturn()
        }
        viewModel.refreshPlaybackSession()
        if (viewModel.playback.value.hasMedia &&
            !com.local.listentomusic.ui.components.VideoSurfaceOwner.expandedOverlayActive &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
            ContextCompat.startForegroundService(this, Intent(this, MiniWindowOverlayService::class.java)
                .setAction(MiniWindowOverlayService.ACTION_DOCK))
        }
        // Reconcile files changed while the observer was stopped, after the return transition.
        returnScan?.cancel()
        returnScan = lifecycleScope.launch {
            if (viewModel.library.value.status == LibraryStatus.READY) kotlinx.coroutines.delay(1800)
            viewModel.rescan()
        }
    }

    override fun onPause() {
        returnScan?.cancel()
        miniWindowReturnJob?.cancel()
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setActivityForeground(false)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        com.local.listentomusic.playback.PlayerWindowVisibility.app(true)
        com.local.listentomusic.playback.PlayerWindowVisibility.library(libraryScreenVisible)
    }

    override fun onStop() {
        com.local.listentomusic.playback.PlayerWindowVisibility.library(false)
        com.local.listentomusic.playback.PlayerWindowVisibility.app(false)
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(backgroundPlayer) }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenPlayerIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // A system-level Now Playing window is already the active external
        // presentation. Do not spawn a second Mini Window underneath it.
        if (com.local.listentomusic.ui.components.VideoSurfaceOwner.expandedOverlayActive) {
            if (!MiniWindowOverlayService.shareInProgress) {
                startService(Intent(this, MiniWindowOverlayService::class.java)
                    .setAction(MiniWindowOverlayService.ACTION_DETACH))
            }
            return
        }
        val playback = viewModel.playback.value
        val settings = viewModel.settings.value
        if (
            playback.hasMedia &&
            settings.autoPictureInPicture &&
            settings.floatingWindowMode == FloatingWindowMode.MINI_WINDOW
        ) {
            // The already-running window follows Library visibility onStop.
            // Do not queue a second detach intent that can race a rapid return.
            return
        }
        if (
            playerScreenVisible &&
            playback.isVideo &&
            playback.isPlaying &&
            settings.autoPictureInPicture &&
            !isInPictureInPictureMode
        ) {
            updatePictureInPictureParams()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                enterPictureInPictureMode(buildPictureInPictureParams(autoEnter = false))
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPictureInPicture = isInPictureInPictureMode
    }

    private fun updatePictureInPictureParams() {
        val playback = viewModel.playback.value
        val autoEnter = playerScreenVisible && playback.isVideo && playback.isPlaying &&
            viewModel.settings.value.autoPictureInPicture &&
            viewModel.settings.value.floatingWindowMode != FloatingWindowMode.MINI_WINDOW
        setPictureInPictureParams(buildPictureInPictureParams(autoEnter))
    }

    private fun enterVideoPictureInPicture() {
        val playback = viewModel.playback.value
        if (!playback.hasMedia || isInPictureInPictureMode) return
        if (
            !playback.isVideo ||
            viewModel.settings.value.floatingWindowMode == FloatingWindowMode.MINI_WINDOW
        ) {
            startMiniWindowIfAllowed(
                openSettingsWhenMissing = true,
                backgroundWhenReady = true,
            )
            return
        }
        if (!playerScreenVisible || !playback.isVideo) return
        updatePictureInPictureParams()
        runCatching {
            enterPictureInPictureMode(buildPictureInPictureParams(autoEnter = false))
        }
    }

    private fun startMiniWindowIfAllowed(
        openSettingsWhenMissing: Boolean = false,
        backgroundWhenReady: Boolean = false,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            if (openSettingsWhenMissing) {
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:$packageName".toUri(),
                        ),
                    )
                }
            }
            return false
        }
        return runCatching {
            ContextCompat.startForegroundService(this, Intent(this, MiniWindowOverlayService::class.java)
                .setAction(MiniWindowOverlayService.ACTION_DETACH))
            if (backgroundWhenReady) moveTaskToBack(true)
            true
        }.getOrElse {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            false
        }
    }

    private fun handleOpenPlayerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(MiniWindowOverlayService.EXTRA_OPEN_PLAYER, false) != true) return
        intent.removeExtra(MiniWindowOverlayService.EXTRA_OPEN_PLAYER)
        returningFromMiniWindow = true
        // Declare NOW_PLAYING as the foreground destination before onResume() changes
        // the Activity foreground state. This prevents a transient LIBRARY_MINI owner.
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setPresentation(
            nowPlayingVisible = true,
            pictureInPicture = false,
        )
        com.local.listentomusic.ui.components.VideoSurfaceOwner.beginHandoff("NOW_PLAYING")
        openPlayerRequest++
        playerScreenVisible = true
    }

    private fun completeMiniWindowReturn() {
        miniWindowReturnJob?.cancel()
        if (!viewModel.playback.value.isVideo) {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
            returningFromMiniWindow = false
            return
        }
        miniWindowReturnJob = lifecycleScope.launch {
            // Event-driven handoff: stop the overlay once NOW_PLAYING owns the active
            // video surface. The timeout is cleanup-only so an OEM/view failure cannot
            // leave a system overlay stuck above the foreground app.
            withTimeoutOrNull(1_500L) {
                com.local.listentomusic.ui.components.VideoSurfaceOwner.state
                    .first { it.owner == "NOW_PLAYING" && it.firstFrame }
            }
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
            returningFromMiniWindow = false
        }
    }

    private fun buildPictureInPictureParams(autoEnter: Boolean): PictureInPictureParams {
        val ratio = when (viewModel.settings.value.floatingWindowMode) {
            FloatingWindowMode.COMPACT -> 16f / 9f
            FloatingWindowMode.FOLLOW_VIDEO ->
                viewModel.playback.value.videoAspectRatio.coerceIn(0.5f, 2.0f)
            FloatingWindowMode.MINI_WINDOW -> 16f / 9f
        }
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1_000).toInt(), 1_000))
            .apply {
                if (!videoSourceRect.isEmpty) setSourceRectHint(videoSourceRect)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(autoEnter)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_OPEN_PLAYER_REQUEST, openPlayerRequest)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val ACTION_BACKGROUND_PLAYER = "com.local.listentomusic.BACKGROUND_PLAYER_TASK"
        private const val STATE_OPEN_PLAYER_REQUEST = "open_player_request"
    }
}

@Composable
private fun PermissionAwareApp(
    viewModel: MainViewModel,
    openPlayerRequest: Int,
    onOpenPlayerRequestConsumed: (Int) -> Unit,
    isPictureInPicture: Boolean,
    onPlayerScreenChanged: (Boolean) -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit,
    onLibraryScreenChanged: (Boolean) -> Unit,
    onEnterPictureInPicture: () -> Unit,
) {
    val context = LocalContext.current
    val preferences by viewModel.settings.collectAsStateWithLifecycle()
    val language = preferences.appLanguage
    var overlayGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) }
    var showOverlayPrompt by remember { mutableStateOf(true) }
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context) }
    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.rescan() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Playback still works if notification permission is declined. */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    GreaterArtApp(
        viewModel = viewModel,
        openPlayerRequest = openPlayerRequest,
        onOpenPlayerRequestConsumed = onOpenPlayerRequestConsumed,
        isPictureInPicture = isPictureInPicture,
        onPlayerScreenChanged = onPlayerScreenChanged,
        onVideoBoundsChanged = onVideoBoundsChanged,
        onLibraryScreenChanged = onLibraryScreenChanged,
        onEnterPictureInPicture = onEnterPictureInPicture,
        onGrantStorageAccess = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appSpecific = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
                runCatching { context.startActivity(appSpecific) }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacyPermissionLauncher.launch(
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                )
            }
        },
    )
    if (!overlayGranted && showOverlayPrompt) {
        GreaterArtTheme(preferences.themeMode, preferences.appFont, preferences.silianRail) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showOverlayPrompt = false },
            title = { androidx.compose.material3.Text(uiText(language, "Allow floating player", "允許浮動播放器")) },
            text = { androidx.compose.material3.Text(uiText(language, "Enable Display over other apps to use the Library mini-player and Now Playing window. You can keep listening without it.", "啟用「顯示在其他應用程式上層」即可使用音樂庫迷你播放器和正在播放視窗。未啟用時仍可繼續聆聽。")) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                overlaySettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()))
            }) { androidx.compose.material3.Text(uiText(language, "Open permission settings", "開啟權限設定")) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showOverlayPrompt = false }) {
                androidx.compose.material3.Text(uiText(language, "Not now", "暫時不用"))
            } },
        )
        }
    }
}
