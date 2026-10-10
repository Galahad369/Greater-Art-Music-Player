package com.local.listentomusic.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.Settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.components.AppBackground
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.LocalLyrics
import com.local.listentomusic.ui.theme.GreaterArtTheme

private enum class Screen { LIBRARY, NOW_PLAYING, SETTINGS }

@Composable
@android.annotation.SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun GreaterArtApp(
    viewModel: MainViewModel,
    openPlayerRequest: Int,
    onOpenPlayerRequestConsumed: (Int) -> Unit,
    isPictureInPicture: Boolean,
    onPlayerScreenChanged: (Boolean) -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit,
    onLibraryScreenChanged: (Boolean) -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onGrantStorageAccess: () -> Unit,
) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val controller by viewModel.controller.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playHistory by viewModel.playHistory.collectAsStateWithLifecycle()
    val scopedPreview by viewModel.scopedPreviewStatus.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val expandedPlayerVisible by com.local.listentomusic.playback.PlayerWindowVisibility.expandedShowing.collectAsStateWithLifecycle()
    val listScrolling by ListScrollBudget.scrolling.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { viewModel.backupSettings(it) } }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.restoreSettings(it) } }
    var restoreConfirm by rememberSaveable { mutableStateOf(false) }
    var showDuplicates by rememberSaveable { mutableStateOf(false) }
    val imageBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val previousUri = settings.customBackgroundImageUri
        val grantPersisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        viewModel.setCustomBackgroundImage(uri.toString())
        if (grantPersisted && previousUri != null && previousUri != uri.toString()) runCatching {
            context.contentResolver.releasePersistableUriPermission(
                previousUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    val videoBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val previousUri = settings.customBackgroundVideoUri
        val grantPersisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        viewModel.setCustomBackgroundVideo(uri.toString())
        if (grantPersisted && previousUri != null && previousUri != uri.toString()) runCatching {
            context.contentResolver.releasePersistableUriPermission(
                previousUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    val scopedTreePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (persisted) {
            val prior = settings.scopedMediaTreeUri
            viewModel.setScopedMediaTree(uri.toString())
            if (prior != null && prior != uri.toString()) runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    prior.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            android.widget.Toast.makeText(context, "Could not retain folder permission. Please retry.",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val m3uImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importM3u)
    }
    val m3uExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri -> uri?.let(viewModel::exportActiveM3u) }
    val lyrics by produceState<LocalLyrics?>(initialValue = null, key1 = playback.currentPath) {
        value = viewModel.loadLyrics(playback.currentPath)
    }
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }
    LaunchedEffect(screen) { onLibraryScreenChanged(screen == Screen.LIBRARY) }
    val libraryPager = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val graphLoading by viewModel.graphLoading.collectAsStateWithLifecycle()
    val graphError by viewModel.graphError.collectAsStateWithLifecycle()
    val wallpaperPan = rememberWallpaperPanState()
    val wallpaperSourceKey = when (settings.backgroundMode) {
        AppBackgroundMode.DEFAULT -> "default"
        AppBackgroundMode.CUSTOM_IMAGE -> "image:${settings.customBackgroundImageUri.orEmpty()}"
        AppBackgroundMode.CUSTOM_VIDEO -> "video:${settings.customBackgroundVideoUri.orEmpty()}"
        AppBackgroundMode.CURRENT_VIDEO -> "current:${playback.currentPath.orEmpty()}"
    }
    LaunchedEffect(wallpaperSourceKey) { wallpaperPan.center() }
    val backgroundHorizontalPosition = androidx.compose.runtime.remember(wallpaperPan, screen) {
        { if (screen == Screen.LIBRARY) wallpaperPan.position else 0.5f }
    }
    val navigationScope = rememberCoroutineScope()
    val libraryBackgroundReveal = rememberLibraryBackgroundRevealState()
    // Preserve the original Stack | All songs | Nodes wallpaper motion while the sheet
    // is closed. As soon as reveal starts, freeze that framing so vertical reveal cannot
    // move the wallpaper; at full reveal horizontal gestures own WallpaperPanState.
    androidx.compose.runtime.SideEffect {
        if (screen == Screen.LIBRARY && shouldSyncWallpaperPanToPager(libraryBackgroundReveal.fraction)) {
            wallpaperPan.setPosition(
                libraryPagerBackgroundPosition(
                    libraryPager.currentPage,
                    libraryPager.currentPageOffsetFraction,
                ),
            )
        }
    }
    LaunchedEffect(libraryPager.currentPage) { if (libraryPager.currentPage == 2) viewModel.requestGraph() }
    var editDisplay by remember { mutableStateOf<com.local.listentomusic.model.MediaFile?>(null) }
    var createRule by remember { mutableStateOf(false) }
    var pendingNowPlayingOpen by remember { mutableStateOf(false) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (pendingNowPlayingOpen && Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MiniWindowOverlayService::class.java).setAction(MiniWindowOverlayService.ACTION_EXPAND),
            )
        }
        pendingNowPlayingOpen = false
    }
    val openNowPlayingOverlay: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MiniWindowOverlayService::class.java).setAction(MiniWindowOverlayService.ACTION_EXPAND),
            )
        } else {
            pendingNowPlayingOpen = true
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                ),
            )
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) { viewModel.startLibraryObservation() }
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) { viewModel.stopLibraryObservation() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); viewModel.stopLibraryObservation() }
    }
    var jokeDismissed by rememberSaveable { mutableStateOf(false) }
    val inspector = remember { UiInspectorState() }

    LaunchedEffect(openPlayerRequest) {
        if (openPlayerRequest > 0) {
            openNowPlayingOverlay()
            onOpenPlayerRequestConsumed(openPlayerRequest)
        }
    }

    LaunchedEffect(Unit) { onPlayerScreenChanged(false) }

    BackHandler(enabled = screen != Screen.LIBRARY || libraryPager.currentPage != 1) {
        if (screen != Screen.LIBRARY) screen = Screen.LIBRARY
        else navigationScope.launch { libraryPager.animateScrollToPage(1) }
    }

    val appName = if (settings.silianRail) "PIERCE&PIERCE" else "Greater Art"
    val stackPage = screen == Screen.LIBRARY && libraryPager.currentPage == 0
    val dockedPlayerVisible by com.local.listentomusic.playback.PlayerWindowVisibility.dockedVisible
        .collectAsStateWithLifecycle()
    val libraryRevealActive =
        screen == Screen.LIBRARY && libraryBackgroundReveal.fraction > 0.001f
    LaunchedEffect(libraryRevealActive) {
        com.local.listentomusic.playback.PlayerWindowVisibility.libraryBackgroundReveal(libraryRevealActive)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            com.local.listentomusic.playback.PlayerWindowVisibility.libraryBackgroundReveal(false)
        }
    }
    androidx.compose.runtime.DisposableEffect(stackPage) {
        com.local.listentomusic.playback.PlayerWindowVisibility.stackTransport(stackPage)
        onDispose { com.local.listentomusic.playback.PlayerWindowVisibility.stackTransport(false) }
    }
    GreaterArtTheme(
        themeMode = settings.themeMode,
        colorTheme = settings.colorTheme,
        appFont = settings.appFont,
        silianRail = settings.silianRail,
    ) {
        val lightPalette = MaterialTheme.colorScheme.background.luminance() > 0.5f
        androidx.compose.runtime.SideEffect {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.setPresentation(
                nowPlayingVisible = false,
                pictureInPicture = isPictureInPicture,
            )
        }
        LaunchedEffect(lightPalette) {
            context.findActivity()?.let { activity ->
                androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = lightPalette
                    isAppearanceLightNavigationBars = lightPalette
                }
            }
        }
        UiInspectorHost(enabled = settings.developerMode, state = inspector) {
        val libraryWallpaperDimAlpha =
            if (settings.backgroundMode == AppBackgroundMode.DEFAULT) 0.08f else settings.backgroundDim
        val libraryWallpaperDimColor =
            if (settings.backgroundMode == AppBackgroundMode.DEFAULT && lightPalette) Color.White else Color.Black
        Box(modifier = Modifier.fillMaxSize().inspectElement("APP_VIEWPORT", "Greater Art root viewport")) {
            // The expanded player fully covers MainActivity. Do not leave the independent
            // CURRENT_VIDEO/CUSTOM_VIDEO wallpaper decoder running underneath it: fast queue
            // flings then compete with two video decoders plus Compose/GPU work.
            AppBackground(
                preferences = settings,
                currentPath = playback.currentPath,
                isVideo = playback.isVideo,
                controller = controller,
                visible = !expandedPlayerVisible,
                // CUSTOM_VIDEO still yields its independent decorative decoder while
                // Stack is active. CURRENT_VIDEO is now the primary player's own surface,
                // so it adds no decoder and can remain available.
                allowVideoBackground =
                    settings.backgroundMode == com.local.listentomusic.data.AppBackgroundMode.CURRENT_VIDEO ||
                        playback.stackCount == 0,
                // The Library dock is a user-facing playback surface. When it is visible,
                // give it the single primary video surface and let CURRENT_VIDEO wallpaper
                // fall back to ambient instead of forcing the dock to artwork.
                allowPrimaryVideoBackground = !dockedPlayerVisible,
                listScrolling = listScrolling,
                horizontalPosition = backgroundHorizontalPosition,
                // Library reveal must expose the raw wallpaper. Its normal dim is
                // reproduced only inside the translated Library content layer.
                dimAlphaOverride = if (screen == Screen.LIBRARY) 0f else null,
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                // Library must stay transparent so its pull-down reveal can expose the
                // real AppBackground. Non-Library screens retain the light-palette wash.
                color = when {
                    screen == Screen.LIBRARY -> Color.Transparent
                    lightPalette -> MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                    else -> Color.Transparent
                },
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
            // Surface propagates its full-screen minimum constraints. Transient
            // feedback must not inherit them and cover the library with a snackbar.
            Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (playback.isVideo) {
                        // A shared decoder cannot display two outgoing/incoming
                        // surfaces at once. Switch ownership without an overlap.
                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    } else {
                    val spring = spring<IntOffset>(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy,
                    )
                    // A restrained horizontal push keeps screen changes spatially clear.
                    if (targetState > initialState) {
                        slideInHorizontally(spring, initialOffsetX = { it }) + fadeIn() togetherWith
                            slideOutHorizontally(spring, targetOffsetX = { -it / 3 }) + fadeOut()
                    } else {
                        slideInHorizontally(spring, initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                            slideOutHorizontally(spring, targetOffsetX = { it }) + fadeOut()
                    }
                    }
                },
                label = "screen",
            ) { scr ->
                when (scr) {
                Screen.LIBRARY -> Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        if (playback.hasMedia && !stackPage) {
                            Spacer(Modifier.fillMaxWidth().height(com.local.listentomusic.model.MiniWindowMetrics.HEIGHT_DP.dp))
                        }
                    },
                ) { dockPadding ->
                    LibraryFamilyWithBackgroundReveal(
                        reveal = libraryBackgroundReveal,
                        lightPalette = lightPalette,
                        wallpaperDimAlpha = libraryWallpaperDimAlpha,
                        wallpaperDimColor = libraryWallpaperDimColor,
                        wallpaperPan = wallpaperPan,
                        backgroundScaleMode = settings.backgroundScaleMode,
                        // Keep the reveal/pure-wallpaper gesture surface full-screen.
                        // Only the translated foreground respects the Mini Window dock.
                        modifier = Modifier.fillMaxSize(),
                        foregroundModifier = Modifier
                            .padding(dockPadding)
                            .consumeWindowInsets(dockPadding),
                    ) {
                        LibraryTopBar(appName, library, settings, playHistory, viewModel::rescan,
                            viewModel::setPlayHistoryEnabled, viewModel::clearPlayHistory,
                            viewModel::setSortMode, { screen = Screen.SETTINGS })
                        LibraryFamilyNavigationBar(
                            currentPage = libraryPager.currentPage,
                            pageOffsetFraction = libraryPager.currentPageOffsetFraction,
                            language = settings.appLanguage,
                            onPage = { page -> navigationScope.launch { libraryPager.animateScrollToPage(page) } },
                        )
                        HorizontalPager(
                            state = libraryPager,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            key = { when (it) { 0 -> "STACK"; 2 -> "NODES"; else -> "LIBRARY" } },
                        ) { page ->
                                                                            if (page == 0) {
                                                                                val stackFiles by viewModel.stackFiles.collectAsStateWithLifecycle()
                                                                                StackScreen(
                                                                                    files = stackFiles,
                                                                                    language = settings.appLanguage,
                                                                                    contentPadding = PaddingValues(0.dp),
                                                                                    nowPlayingPath = playback.currentPath,
                                                                                    playHistory = playHistory,
                                                                                    onLoadThumbnail = viewModel::loadThumbnail,
                                                                                    savedStacks = settings.savedStacks,
                                                                                    onSaveStack = viewModel::saveStack,
                                                                                    onDeleteStack = viewModel::deleteStack,
                                                                                    onOpenPlayer = openNowPlayingOverlay,
                                                                                )
                                                                            } else if (page == 2) {
                                                                                NodesScreen(
                                                                                    graph = graph,
                                                                                    loading = graphLoading,
                                                                                    error = graphError,
                                                                                    currentPath = playback.currentPath,
                                                                                    contentPadding = PaddingValues(0.dp),
                                                                                    onRetry = viewModel::requestGraph,
                                                                                    onPlay = viewModel::playGraphNode,
                                                                                    options = settings.graphOptions,
                                                                                    onOptions = viewModel::setGraphOptions,
                                                                                )
                                                                            } else {
                                            LibraryScreen(
                        appName = appName,
                        showTopBar = false,
                        onThumbnailViewport = viewModel::thumbnailViewport,
                        state = library,
                        preferences = settings,
                        playHistory = playHistory,
                        currentPath = playback.currentPath,
                        // The outer Scaffold's bottom inset is the mini-player height.
                        // Applying it to the whole Library created a permanent dead band.
                        // The list now draws behind the player and keeps only a scroll-end inset.
                        contentPadding = PaddingValues(0.dp),
                        onGrantStorageAccess = onGrantStorageAccess,
                        onRefresh = viewModel::rescan,
                        onPlayHistoryEnabled = viewModel::setPlayHistoryEnabled,
                        onClearPlayHistory = viewModel::clearPlayHistory,
                        onQueryChange = viewModel::setQuery,
                        onSortChange = viewModel::setSortMode,
                        onMoveItem = viewModel::moveCustomItem,
                        onSelectPlaylist = viewModel::setActivePlaylist,
                        onCreatePlaylist = viewModel::createPlaylist,
                        onCreatePlaylistAndSeed = viewModel::createPlaylistAndSeed,
                        onPlayPlaylist = viewModel::playPlaylist,
                        onAddToPlaylist = viewModel::addToPlaylist,
                        onRemoveFromPlaylist = viewModel::removeFromActivePlaylist,
                        onDeletePlaylist = viewModel::deletePlaylist,
                        onDeleteFile = viewModel::deleteMediaFile,
                        onShareMedia = { file ->
                            AndroidShare.media(context, file, uiText(settings.appLanguage, "Share media file", "分享媒體檔案")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this file", "無法分享此檔案"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onShareCurrentList = {
                            navigationScope.launch {
                                val label = when (settings.activePlaylistId) {
                                    com.local.listentomusic.data.FAVOURITES_PLAYLIST_ID -> uiText(settings.appLanguage, "Favorites", "我的最愛")
                                    else -> settings.playlists.firstOrNull { it.id == settings.activePlaylistId }?.name
                                        ?: uiText(settings.appLanguage, "Current Library list", "目前音樂庫清單")
                                }
                                AndroidShare.list(context, label, library.files, uiText(settings.appLanguage, "Share current Library list", "分享目前音樂庫清單")).onFailure {
                                    android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this list", "無法分享此清單"), android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onShareSelectedFiles = { files ->
                            AndroidShare.mediaFiles(context, files, uiText(settings.appLanguage, "Share files", "分享檔案")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share these files", "無法分享這些檔案"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onToggleFavourite = viewModel::toggleFavourite,
                        onLoadThumbnail = viewModel::loadThumbnail,
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onStackTogether = { files ->
                            val started = com.local.listentomusic.playback.StackPlayback.start(files)
                            if (started) navigationScope.launch { libraryPager.animateScrollToPage(0) }
                            started
                        },
                        onEditDisplay = { editDisplay = it },
                        onCreateRule = { createRule = true },
                        onAddSelected = viewModel::addAllToPlaylist,
                        onCreateSelected = viewModel::createSelectionPlaylist,
                        onPlay = {
                            viewModel.play(it)
                        },
                    )
                                                                            }
                        }
                    }
                }
                Screen.NOW_PLAYING -> Unit // Legacy saved enum; playback now lives in the overlay.
                Screen.SETTINGS -> SettingsScreen(
                    appName = appName,
                    preferences = settings,
                    playback = playback,
                    dockInset = if (playback.hasMedia) com.local.listentomusic.model.MiniWindowMetrics.HEIGHT_DP.dp else 0.dp,
                    onBack = { screen = Screen.LIBRARY },
                    onRowSize = viewModel::setLibraryRowSize,
                    onThemeMode = viewModel::setThemeMode,
                    onColorTheme = viewModel::setColorTheme,
                    onShowThumbnails = viewModel::setShowThumbnails,
                    onShowFileDetails = viewModel::setShowFileDetails,
                    onResumePlayback = viewModel::setResumePlayback,
                    onAutoPictureInPicture = viewModel::setAutoPictureInPicture,
                    onFloatingWindowMode = viewModel::setFloatingWindowMode,
                    onAppLanguage = viewModel::setAppLanguage,
                    onAppFont = viewModel::setAppFont,
                    onDeveloperMode = viewModel::setDeveloperMode,
                    onEditableQueue = viewModel::setEditableQueue,
                    onImportM3u = { m3uImporter.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain")) },
                    onExportM3u = { m3uExporter.launch("Greater-Art-playlist.m3u8") },
                    onBackgroundMode = { mode ->
                                            when {
                                                mode == AppBackgroundMode.CUSTOM_IMAGE &&
                                                    (settings.customBackgroundImageUri == null ||
                                                        settings.backgroundMode == AppBackgroundMode.CUSTOM_IMAGE) ->
                                                    imageBackgroundPicker.launch(arrayOf("image/*"))
                                                mode == AppBackgroundMode.CUSTOM_VIDEO &&
                                                    (settings.customBackgroundVideoUri == null ||
                                                        settings.backgroundMode == AppBackgroundMode.CUSTOM_VIDEO) ->
                                                    videoBackgroundPicker.launch(arrayOf("video/mp4"))
                                                else -> viewModel.setBackgroundMode(mode)
                                            }
                                        },
                                        onBackgroundScaleMode = viewModel::setBackgroundScaleMode,
                                        onChooseBackgroundImage = {
                        imageBackgroundPicker.launch(arrayOf("image/*"))
                    },
                    onChooseBackgroundVideo = {
                        videoBackgroundPicker.launch(arrayOf("video/mp4"))
                    },
                    onClearBackgroundImage = { viewModel.setCustomBackgroundImage(null) },
                    onClearBackgroundVideo = { viewModel.setCustomBackgroundVideo(null) },
                    onBackgroundDim = viewModel::setBackgroundDim,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onCreatePlaylistAndSeed = viewModel::createPlaylistAndSeed,
                    onPlayPlaylist = viewModel::playPlaylist,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onSharePlaylist = { playlist ->
                        navigationScope.launch {
                            AndroidShare.list(context, playlist.name, viewModel.filesForPlaylist(playlist.id), uiText(settings.appLanguage, "Share playlist", "分享播放清單")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this list", "無法分享此清單"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onSpeed = viewModel::setSpeed,
                    onPlaybackCycle = viewModel::setPlaybackCycle,
                    onClearThumbnailCache = viewModel::clearThumbnailCache,
                    onRescan = viewModel::rescan,
                    onReset = viewModel::resetAppSettings,
                    onSeekOffset = viewModel::setSeekOffset,
                    onJokeAdsEnabled = viewModel::setJokeAdsEnabled,
                    onShowSleepControl = viewModel::setShowSleepControl,
                    onShowAbRepeat = viewModel::setShowAbRepeat,
                    onExtendedSearch = viewModel::setExtendedSearch,
                    onFolderExcluded = viewModel::setFolderExcluded,
                    scopedPreview = scopedPreview,
                    onChooseScopedFolder = { scopedTreePicker.launch(null) },
                    onPreviewScopedFolder = viewModel::previewScopedMediaTree,
                    onClearScopedFolder = {
                        settings.scopedMediaTreeUri?.let { prior ->
                            runCatching {
                                context.contentResolver.releasePersistableUriPermission(
                                    prior.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                        viewModel.setScopedMediaTree(null)
                    },
                    onReplayGainEnabled = viewModel::setReplayGainEnabled,
                    onBlackDiscMode = viewModel::setBlackDiscMode,
                    onPlayHistoryEnabled = viewModel::setPlayHistoryEnabled,
                    onBackup = { backupPicker.launch("Greater-Art-settings.json") },
                    onRestore = { restoreConfirm = true },
                    onDuplicates = { showDuplicates = true },
                    onEqualizer = {
                        val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                            .putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, controller?.audioSessionId ?: 0)
                            .putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            .putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                        runCatching { context.startActivity(intent) }.onFailure {
                            android.widget.Toast.makeText(context, "No system equalizer is installed on this device.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
            }
            val undoMessage by viewModel.undoMessage.collectAsStateWithLifecycle()
            undoMessage?.let { message ->
                androidx.compose.material3.Snackbar(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(bottom =
                        (if (playback.hasMedia) com.local.listentomusic.model.MiniWindowMetrics.HEIGHT_DP.dp else 0.dp) +
                            (if (screen == Screen.LIBRARY && libraryPager.currentPage == 0) 120.dp else 0.dp)
                    ), action = {
                    androidx.compose.material3.TextButton(onClick = viewModel::undoLastEdit) { androidx.compose.material3.Text(uiText(settings.appLanguage, "Undo", "復原")) }
                }) { androidx.compose.material3.Text(message) }
            }
            if (settings.developerMode) {
                DiagnosticsLayer {
                // Counter updates invalidate this child composition, not the whole
                // Library/player. DEV must not turn a cache load into app-root churn.
                val thumbnailStats by viewModel.thumbnailStats.collectAsStateWithLifecycle()
                val waveformDiagnostics by viewModel.waveformDiagnostics.collectAsStateWithLifecycle()
                val engineReport by com.local.listentomusic.playback.PlaybackDiagnostics.report.collectAsStateWithLifecycle()
                val localFailures by com.local.listentomusic.diagnostics.CrashReports.latest.collectAsStateWithLifecycle()
                val stackSession by com.local.listentomusic.playback.StackPlayback.state.collectAsStateWithLifecycle()
                val windowMode by MiniWindowOverlayService.modeSnapshot.collectAsStateWithLifecycle()
                val windowIdentities by MiniWindowOverlayService.identities.collectAsStateWithLifecycle()
                val indexStatus by viewModel.indexStatus.collectAsStateWithLifecycle()
                val viewport = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
                val density = androidx.compose.ui.platform.LocalDensity.current
                val regions = inspector.regions.values.sortedBy { it.order }.map { region ->
                    "${region.label} · ${region.bounds.width.toInt()}×${region.bounds.height.toInt()}px" +
                        region.detail.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                }
                val surface by com.local.listentomusic.ui.components.VideoSurfaceOwner.state.collectAsStateWithLifecycle()
                val warnings = surface.warnings(
                    com.local.listentomusic.ui.components.VideoSurfaceOwner.expectedOwner,
                    playback.isVideo && playback.isPlaying && controller?.playbackState == androidx.media3.common.Player.STATE_READY &&
                        (com.local.listentomusic.ui.components.VideoSurfaceOwner.systemOverlayActive || screen == Screen.LIBRARY), android.os.SystemClock.elapsedRealtime(),
                    playback.videoFrameRendered,
                ) + listOfNotNull(if (playback.errorMessage != null) "PLAYBACK_ERROR" else null,
                    if (waveformDiagnostics.error != null) "WAVEFORM_ERROR" else null)
                val warning = warnings.isNotEmpty()
                DeveloperDiagnostics(
                    report = buildString {
                        appendLine("version=${com.local.listentomusic.BuildConfig.VERSION_NAME}")
                        appendLine(localFailures)
                        // This inspector belongs to MainActivity. A system-player
                        // ownership flag can outlive its visible window and must not
                        // relabel the page the user is actually inspecting.
                        appendLine("screen=${if (screen == Screen.LIBRARY) when (libraryPager.currentPage) { 0 -> "STACK"; 2 -> "NODES"; else -> "LIBRARY" } else screen.name}")
                        appendLine("systemPlayerOverlay=${com.local.listentomusic.ui.components.VideoSurfaceOwner.systemOverlayActive}")
                        appendLine("playerWindowMode=${windowMode ?: "none"} playerWindowService=${windowMode != null} $windowIdentities")
                        appendLine("media=${playback.currentPath?.let { com.local.listentomusic.model.sourceMediaPath(it).substringAfterLast('.') } ?: "none"} (paths omitted)")
                        appendLine("playing=${playback.isPlaying} video=${playback.isVideo}")
                        appendLine("position=${playback.positionMs} duration=${playback.durationMs}")
                        appendLine("playerState=${controller?.playbackState ?: -1} buffered=${controller?.bufferedPosition ?: 0L}")
                        appendLine("video=${playback.videoWidth}x${playback.videoHeight} controllerMediaFirstFrame=${playback.videoFrameRendered}")
                        appendLine(com.local.listentomusic.ui.components.VideoSurfaceOwner.describe())
                        appendLine("firstFrameAttribution=renderer timestamp after transfer; not a screen-capture proof")
                        appendLine("queue=${queue.size} library=${library.files.size}")
                        appendLine("stack=${stackSession.slots.size} primarySelected=${stackSession.primaryPath != null}")
                        appendLine("libraryPager=${libraryPager.currentPage} offset=${libraryPager.currentPageOffsetFraction} navPosition=${libraryPagerNavigationPosition(libraryPager.currentPage, libraryPager.currentPageOffsetFraction)} backgroundCrop=$backgroundHorizontalPosition")
                        appendLine("backgroundDim=${settings.backgroundDim} libraryRevealUndimmed=true dimLayer=translatedLibraryContent")
                        appendLine("repeat=${playback.repeatMode} random=${playback.shuffleEnabled}")
                        appendLine("floating=${settings.floatingWindowMode} auto=${settings.autoPictureInPicture}")
                        appendLine("background=${settings.backgroundMode} theme=${settings.themeMode}/${settings.colorTheme}")
                        appendLine("thumbs=memory:${thumbnailStats.memoryHits} disk:${thumbnailStats.diskHits} made:${thumbnailStats.generated} failed:${thumbnailStats.failed} noCoverOrUnsupported:${thumbnailStats.missingArtwork} active:${thumbnailStats.inFlight}")
                        appendLine("thumbQueue=${thumbnailStats.queued} shared=${thumbnailStats.sharedRequests} cancelled=${thumbnailStats.cancelled} rejected=${thumbnailStats.rejected} peak=${thumbnailStats.peakPending} artReuse=${thumbnailStats.artworkReuses}")
                        appendLine("thumbRamKb=${thumbnailStats.memoryKb}/${thumbnailStats.memoryBudgetKb} peak=${thumbnailStats.peakMemoryKb}")
                        appendLine("waveform=${waveformDiagnostics.status}")
                        appendLine("waveformError=${waveformDiagnostics.error ?: "none"}")
                        val storageGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                            Environment.isExternalStorageManager()
                        appendLine("storage=$storageGranted overlay=${Settings.canDrawOverlays(context)}")
                        appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} api=${Build.VERSION.SDK_INT}")
                        appendLine("warnings=$warnings")
                        appendLine("\nAUDIO ENGINE")
                        appendLine(engineReport)
                        appendLine("\nVIEWPORT / PERFORMANCE")
                        appendLine("viewportPx=${viewport.width}x${viewport.height} density=${density.density} fontScale=${density.fontScale}")
                        appendLine("heapUsedMiB=${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_048_576} heapLimitMiB=${Runtime.getRuntime().maxMemory() / 1_048_576}")
                        appendLine("lastTapToFirstFrameMs=${com.local.listentomusic.playback.PlaybackDiagnostics.firstFrameDelayMs ?: "not reported"} (last tap; independent of current surface)")
                        appendLine("$indexStatus")
                        appendLine("abControls=${settings.showAbRepeat} extendedSearch=${settings.extendedSearch}")
                        appendLine("displayOverrides=${settings.localOverrides.size} rulePlaylists=${settings.playlists.count { it.rule != null }}")
                        appendLine("No logs, file paths or listening history are uploaded.")
                    },
                    regions = regions,
                    warning = warning,
                    inspector = inspector,
                    modifier = Modifier,
                    badgeAlignment = if (screen == Screen.LIBRARY) Alignment.TopStart else Alignment.TopEnd,
                )
                }
            }
            if (settings.jokeAdsEnabled && !jokeDismissed && !isPictureInPicture) {
                FakeAdInterstitial(
                    onSkip = { jokeDismissed = true },
                    onOpenLink = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                            "https://www.youtube.com/watch?v=dQw4w9WgXcQ".toUri())) }
                    },
                )
            }
            if (restoreConfirm) androidx.compose.material3.AlertDialog(
                onDismissRequest = { restoreConfirm = false },
                title = { androidx.compose.material3.Text(uiText(settings.appLanguage, "Restore settings and playlists?", "還原設定與播放清單？")) },
                text = { androidx.compose.material3.Text(uiText(settings.appLanguage, "The selected backup replaces portable settings and playlists. Media files remain unchanged.", "所選備份將取代可攜式設定與播放清單，不會更改媒體檔案。")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { restoreConfirm = false; restorePicker.launch(arrayOf("application/json", "text/plain")) }) { androidx.compose.material3.Text(uiText(settings.appLanguage, "Choose backup", "選擇備份")) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { restoreConfirm = false }) { androidx.compose.material3.Text(uiText(settings.appLanguage, "Cancel", "取消")) } },
            )
            if (showDuplicates) DuplicateDialog(library.files, playback.currentPath,
                onDismiss = { showDuplicates = false }, onChanged = viewModel::rescan)
            editDisplay?.let { file -> DisplayOverrideDialog(file, settings.localOverrides[file.path], settings.appLanguage,
                viewModel::loadThumbnail, { title, cover -> viewModel.setLocalOverride(file.path, title, cover) }, { editDisplay = null }) }
            if (createRule) RulePlaylistDialog(settings.appLanguage, viewModel::createRulePlaylist, { createRule = false })
        }
        }
        }
        }
    }
}

@Composable
private fun DiagnosticsLayer(content: @Composable () -> Unit) {
    content()
}
