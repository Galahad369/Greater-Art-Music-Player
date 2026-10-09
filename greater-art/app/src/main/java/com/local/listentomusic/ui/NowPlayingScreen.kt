package com.local.listentomusic.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.playback.stackTransportUsesLoopIcon
import com.local.listentomusic.playback.stackTransportUsesRepeatOneIcon
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.LocalLyrics
import com.local.listentomusic.sleepTimerOptions
import com.local.listentomusic.ui.components.ambientBackdrop
import com.local.listentomusic.ui.theme.GaControl
import com.local.listentomusic.ui.theme.GaMotion
import com.local.listentomusic.ui.theme.GaRadius
import com.local.listentomusic.ui.theme.GaSpacing
import com.local.listentomusic.ui.theme.GaVideoOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal val playbackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
internal const val HOLD_2X_ACTIVATION_MS = 700L
internal const val HOLD_2X_LOCK_DISTANCE_DP = 72f
internal const val NOW_PLAYING_AMBIENT_BOTTOM_BLEND = 0.62f
internal const val NOW_PLAYING_AMBIENT_PANEL_ALPHA = 0.58f
internal const val NOW_PLAYING_AMBIENT_ROW_ALPHA = 0.42f
internal const val NOW_PLAYING_AMBIENT_OUTLINE_ALPHA = 0.34f
internal const val NOW_PLAYING_ART_STAGE_ALPHA = 0.32f

internal fun nowPlayingMetadataLine(file: MediaFile?): String =
    file?.let {
        listOf(it.artist.trim(), it.album.trim())
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" · ")
    }.orEmpty()

internal fun shouldLockHeldDoubleSpeed(dragAfterHoldPx: Float, thresholdPx: Float): Boolean =
    thresholdPx > 0f && dragAfterHoldPx >= thresholdPx
internal fun isDoubleSpeed(speed: Float): Boolean = kotlin.math.abs(speed - 2f) <= 0.01f

internal fun playerLockLocalOffset(
    rootBounds: androidx.compose.ui.geometry.Rect?,
    anchorBounds: androidx.compose.ui.geometry.Rect?,
): IntOffset? {
    if (rootBounds == null || anchorBounds == null) return null
    return IntOffset(
        (anchorBounds.left - rootBounds.left).roundToInt(),
        (anchorBounds.top - rootBounds.top).roundToInt(),
    )
}

private val LocalSystemPlayer = androidx.compose.runtime.compositionLocalOf { false }
@Composable private fun playerStatusInsets() = if (LocalSystemPlayer.current) WindowInsets(0) else WindowInsets.statusBars
@Composable private fun playerNavigationInsets() = if (LocalSystemPlayer.current) WindowInsets(0) else WindowInsets.navigationBars

internal enum class SeekSide { LEFT, RIGHT }

/** Side seek zones only. The middle 30% is deliberately inert. */
internal fun seekSideForX(x: Float, width: Float): SeekSide? = when {
    width <= 0f -> null
    x < width * .35f -> SeekSide.LEFT
    x > width * .65f -> SeekSide.RIGHT
    else -> null
}

internal fun doubleTapSeekDelta(x: Float, width: Float, seekOffsetMs: Long): Long? = when (seekSideForX(x, width)) {
    SeekSide.LEFT -> -seekOffsetMs
    SeekSide.RIGHT -> seekOffsetMs
    null -> null
}

internal const val SIDE_DOUBLE_TAP_MS = 400L

/** Two quick taps seek only when both land on the same active side zone. */
internal fun sideDoubleTapSeeks(
    side: SeekSide?,
    nowMs: Long,
    lastSide: SeekSide?,
    lastTapMs: Long,
): Boolean {
    val elapsedMs = nowMs - lastTapMs
    return side != null && side == lastSide && elapsedMs in 1L..SIDE_DOUBLE_TAP_MS
}

@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    queue: List<MediaFile>,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    blackDiscMode: Boolean,
    language: AppLanguage,
    controller: MediaController?,
    contentPadding: PaddingValues,
    isPictureInPicture: Boolean,
    onVideoBoundsChanged: (Rect) -> Unit,
    onPictureInPicture: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    seekOffsetMs: Long = 5_000L,
    onSeekBy: (Long) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onLoadWaveform: suspend (String) -> FloatArray?,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onBeginTemporaryDoubleSpeed: () -> Boolean,
    onEndTemporaryDoubleSpeed: () -> Unit,
    onLockTemporaryDoubleSpeed: () -> Boolean,
    isFavourite: Boolean,
    onToggleFavourite: (String) -> Unit,
    onShareCurrentMedia: () -> Unit,
    onShareQueue: () -> Unit,
    onAddQueueItemToList: (MediaFile) -> Unit = {},
    systemOverlay: Boolean = false,
    initialFullscreen: Boolean = false,
    forceLandscapeFullscreen: Boolean = false,
    onFullscreenChanged: (Boolean) -> Unit = {},
    sharedVideoView: PlayerView? = null,
    onSharedVideoReleased: () -> Unit = {},
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalSystemPlayer provides systemOverlay) {
    var fullscreen by rememberSaveable { mutableStateOf(initialFullscreen) }
    var controlsLocked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(fullscreen) { onFullscreenChanged(fullscreen) }

    if (isPictureInPicture && playback.isVideo) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoSurface(playback.currentPath, controller, onVideoBoundsChanged, Modifier.fillMaxSize(),
                sharedVideoView, onSharedVideoReleased)
        }
        return@CompositionLocalProvider
    }

    // Shared list state for queue scrolling (used by locate button)
        val queueListState = rememberLazyListState()
        var locateTrigger by remember { mutableStateOf(0) }

        // The queue composable owns filtering, so it also owns the mapping from
        // MediaSession index to visible list index. This callback only requests locate.
        val onLocateCurrent = {
            if (!queueListState.isScrollInProgress) locateTrigger++
        }

        var playerRootBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

        val backdrop = Modifier.ambientBackdrop(
        artwork,
        MaterialTheme.colorScheme.background.luminance() > .5f,
        playback.currentPath,
        bottomBlend = NOW_PLAYING_AMBIENT_BOTTOM_BLEND,
    )
    BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
                .padding(PaddingValues(horizontal = 0.dp, vertical = contentPadding.calculateTopPadding()))
                .onGloballyPositioned { playerRootBounds = it.boundsInWindow() }
                .inspectElement("NOW_PLAYING_SCREEN", "Current artwork or video, queue, timeline, and transport controls")
                .then(backdrop),
        ) {
        val landscape = maxWidth > maxHeight
        val portraitVideoHeight = minOf(maxWidth / playback.videoAspectRatio.coerceIn(0.75f, 2.25f), maxHeight * 0.34f)
        val immersiveVideo = playback.isVideo && (fullscreen || landscape)
        var lockAnchorBounds by remember(playback.isVideo, immersiveVideo) {
            mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
        }
        val reportLockAnchor: (androidx.compose.ui.geometry.Rect) -> Unit = { lockAnchorBounds = it }

        FullscreenEffect(enabled = fullscreen || (playback.isVideo && landscape),
            forceLandscape = forceLandscapeFullscreen)
        BackHandler(enabled = fullscreen) { fullscreen = false }
        BackHandler(enabled = controlsLocked) { controlsLocked = false }

        Box(Modifier.fillMaxSize().then(if (controlsLocked) Modifier.clearAndSetSemantics { } else Modifier)) {
        if (playback.isVideo) {
            val (pageBg, pageInsets) = if (immersiveVideo) {
                Color.Black to WindowInsets(0)
            } else {
                // Portrait playback: gutters use the theme so light mode does not
                // turn the whole page into a black slab. The video stage itself
                // keeps its black backdrop below for letterboxing the frame.
                Color.Transparent to playerStatusInsets()
            }
            val videoPageModifier = Modifier.fillMaxSize().background(pageBg)
                .windowInsetsPadding(pageInsets)
            Column(videoPageModifier) {
                if (!immersiveVideo) {
                                    NowPlayingTopBar(
                                        language = language,
                                        onPictureInPicture = onPictureInPicture,
                                        onHome = onHome,
                                        onFullscreen = { fullscreen = true },
                                        onClose = onClose,
                                        onLocateCurrent = onLocateCurrent,
                                        onLockAnchorBoundsChanged = reportLockAnchor,
                                    )
                                }
                                VideoPlayerStage(
                    playback = playback,
                    controller = controller,
                    sharedVideoView = sharedVideoView,
                    onSharedVideoReleased = onSharedVideoReleased,
                    immersive = immersiveVideo,
                    onVideoBoundsChanged = onVideoBoundsChanged,
                    onHome = onHome,
                    onClose = onClose,
                    onPictureInPicture = onPictureInPicture,
                    onFullscreen = { fullscreen = !fullscreen },
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    seekOffsetMs = seekOffsetMs,
                    onSeekBy = onSeekBy,
                    onSpeed = onSpeed,
                    onBeginTemporaryDoubleSpeed = onBeginTemporaryDoubleSpeed,
                    onEndTemporaryDoubleSpeed = onEndTemporaryDoubleSpeed,
                    onLockTemporaryDoubleSpeed = onLockTemporaryDoubleSpeed,
                    onLockAnchorBoundsChanged = reportLockAnchor,
                    modifier = if (immersiveVideo) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth()
                            .height(portraitVideoHeight)
                    },
                )
                if (!immersiveVideo) {
                                    SecondaryControls(
                                        playback = playback.copy(appLanguage = language),
                                        queue = queue,
                                        lyrics = lyrics,
                                        showFileDetails = showFileDetails,
                                        editableQueue = editableQueue,
                                        language = language,
                                        onSpeed = onSpeed,
                                        onRepeat = onRepeat,
                                        onPrevious = onPrevious,
                                        onTogglePlay = onTogglePlay,
                                        onNext = onNext,
                                        onSleepTimer = onSleepTimer,
                                        sleepTimer = sleepTimer,
                                        onPlayQueueItem = onPlayQueueItem,
                                        onLoadThumbnail = onLoadThumbnail,
                                        onSeek = onSeek,
                                        onMoveQueueItem = onMoveQueueItem,
                                        onRemoveQueueItem = onRemoveQueueItem,
                                        onShareQueue = onShareQueue,
                                        onAddQueueItemToList = onAddQueueItemToList,
                                        isFavourite = isFavourite,
                                        onToggleFavourite = { playback.currentPath?.let(onToggleFavourite) },
                                        onShareCurrentMedia = onShareCurrentMedia,
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        queueListState = queueListState,
                                        locateTrigger = locateTrigger,
                                        onLocateCurrent = onLocateCurrent,
                                    )
                                }
            }
        } else {
            AudioPlayer(
                            playback = playback.copy(appLanguage = language),
                            artwork = artwork,
                            queue = queue,
                            lyrics = lyrics,
                            showFileDetails = showFileDetails,
                            editableQueue = editableQueue,
                            blackDiscMode = blackDiscMode,
                            language = language,
                            onTogglePlay = onTogglePlay,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onSeek = onSeek,
                            onSpeed = onSpeed,
                            onRepeat = onRepeat,
                            onSleepTimer = onSleepTimer,
                            sleepTimer = sleepTimer,
                            seekOffsetMs = seekOffsetMs,
                            onSeekBy = onSeekBy,
                            onPlayQueueItem = onPlayQueueItem,
                            onLoadThumbnail = onLoadThumbnail,
                            onLoadWaveform = onLoadWaveform,
                            onMoveQueueItem = onMoveQueueItem,
                            onRemoveQueueItem = onRemoveQueueItem,
                            onShareQueue = onShareQueue,
                            onAddQueueItemToList = onAddQueueItemToList,
                            isFavourite = isFavourite,
                            onToggleFavourite = { playback.currentPath?.let(onToggleFavourite) },
                            onShareCurrentMedia = onShareCurrentMedia,
                            queueListState = queueListState,
                            locateTrigger = locateTrigger,
                            onLocateCurrent = onLocateCurrent,
                            onLockAnchorBoundsChanged = reportLockAnchor,
                        )
        }
        }
        if (controlsLocked) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            })
        }
        playerLockLocalOffset(playerRootBounds, lockAnchorBounds)?.let { lockOffset ->
            IconButton(
                onClick = { controlsLocked = !controlsLocked },
                modifier = Modifier
                    .offset { lockOffset }
                    .size(GaControl.touchTarget)
                    .inspectElement(
                        "PLAYER_LOCK_BUTTON",
                        if (controlsLocked) "Unlock Now Playing controls" else "Lock Now Playing controls",
                    ),
            ) {
                Icon(
                    if (controlsLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    uiText(language, if (controlsLocked) "Unlock player controls" else "Lock player controls",
                        if (controlsLocked) "解鎖播放器控制" else "鎖定播放器控制"),
                    tint = if (playback.isVideo && immersiveVideo) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}
}

@Composable
private fun VideoPlayerStage(
    playback: PlaybackUiState,
    controller: MediaController?,
    sharedVideoView: PlayerView?,
    onSharedVideoReleased: () -> Unit,
    immersive: Boolean,
    onVideoBoundsChanged: (Rect) -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
    onPictureInPicture: () -> Unit,
    onFullscreen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    seekOffsetMs: Long = 5_000L,
    onSeekBy: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onBeginTemporaryDoubleSpeed: () -> Boolean,
    onEndTemporaryDoubleSpeed: () -> Unit,
    onLockTemporaryDoubleSpeed: () -> Boolean,
    onLockAnchorBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    modifier: Modifier,
    onLocateCurrent: (() -> Unit)? = null,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var seekFeedback by remember { mutableStateOf(0L to 0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    val maximum = if (hasDuration) playback.durationMs.toFloat() else 1f
    val targetPosition = if (hasDuration) playback.positionMs.toFloat() else 0f
    val position = if (seeking) seekPosition else if (immersive && controlsVisible) {
        animateFloatAsState(targetPosition, tween(if (playback.isPlaying) 450 else 0, easing = LinearEasing),
            label = "timeline-position").value
    } else targetPosition
    val currentPlayback by androidx.compose.runtime.rememberUpdatedState(playback)
    val haptics = LocalHapticFeedback.current
    val lockThresholdPx = with(LocalDensity.current) { HOLD_2X_LOCK_DISTANCE_DP.dp.toPx() }
    var temporaryDoubleSpeed by remember { mutableStateOf(false) }
    var unlockDoubleSpeedArmed by remember { mutableStateOf(false) }

    // Zoom state for fullscreen pinch-to-zoom
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffset by remember { mutableStateOf(Offset.Zero) }
    var lastViewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(controlsVisible, playback.isPlaying, playback.currentPath) {
        if (controlsVisible && playback.isPlaying) {
            delay(2_500)
            controlsVisible = false
        }
    }

    // Reset zoom when exiting immersive mode
    LaunchedEffect(immersive) {
        controlsVisible = true
        if (!immersive) {
            videoScale = 1f
            videoOffset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                if (size != lastViewportSize) {
                    lastViewportSize = size
                    controlsVisible = true
                }
            }
            .inspectElement("VIDEO_STAGE", "Side double-tap seeks; pinch to zoom in fullscreen")
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // The video itself is transformed, but gesture ownership lives on the
        // transparent layer above PlayerView. PlayerView/native AndroidView can
        // otherwise consume touches before Compose's transform detector sees them.
        Box(
            Modifier.graphicsLayer {
                scaleX = videoScale
                scaleY = videoScale
                translationX = videoOffset.x
                translationY = videoOffset.y
            }
        ) {
            VideoSurface(playback.currentPath, controller, onVideoBoundsChanged, Modifier.fillMaxSize(),
                sharedVideoView, onSharedVideoReleased)
        }
        // PlayerView is a native AndroidView and can consume touches before a
        // parent detector sees them. This transparent layer is the single input
        // surface for fullscreen gestures. It only consumes the transform after a
        // second pointer appears, so ordinary one-finger tap/drag controls remain.
        Box(Modifier.fillMaxSize().pointerInput(immersive) {
            if (immersive) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var multiTouch = false
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        multiTouch = multiTouch || pressedCount > 1
                        if (multiTouch) {
                            if (pressedCount > 1) {
                                val oldScale = videoScale
                                val newScale = (oldScale * event.calculateZoom()).coerceIn(1f, 4f)
                                val twoFingerPan = event.calculatePan()
                                val maxX = size.width * (newScale - 1f) / 2f
                                val maxY = size.height * (newScale - 1f) / 2f
                                videoOffset = if (newScale <= 1.001f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        (videoOffset.x + twoFingerPan.x).coerceIn(-maxX, maxX),
                                        (videoOffset.y + twoFingerPan.y).coerceIn(-maxY, maxY),
                                    )
                                }
                                videoScale = newScale
                            }
                            // Keep the rest of a pinch away from tap, seek and
                            // one-finger vertical-drag handling until every finger lifts.
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
        }.pointerInput(seekOffsetMs, playback.currentPath, lockThresholdPx) {
            coroutineScope {
                val timerScope = this
                var lastSeekSide: SeekSide? = null
                var lastSeekTapMs = 0L
                awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pressStartedMs = android.os.SystemClock.uptimeMillis()
                val startPosition = down.position
                val startedAtDoubleSpeed = isDoubleSpeed(currentPlayback.speed)
                var currentY = startPosition.y
                var activationY = startPosition.y
                var maxMovement = 0f
                var multiTouch = false
                var holdActivated = false
                var lockedThisGesture = false
                var unlockArmed = false

                fun tryLockCurrentHold() {
                    if (!holdActivated || lockedThisGesture || multiTouch) return
                    if (!shouldLockHeldDoubleSpeed(currentY - activationY, lockThresholdPx)) return
                    if (onLockTemporaryDoubleSpeed()) {
                        lockedThisGesture = true
                        temporaryDoubleSpeed = false
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                val activation = timerScope.launch {
                    delay(HOLD_2X_ACTIVATION_MS)
                        if (multiTouch) return@launch
                        if (startedAtDoubleSpeed) {
                            unlockArmed = true
                            unlockDoubleSpeedArmed = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (currentPlayback.isPlaying && onBeginTemporaryDoubleSpeed()) {
                            holdActivated = true
                            activationY = currentY
                            temporaryDoubleSpeed = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    var anyPressed = true
                    while (anyPressed) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) multiTouch = true
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change != null) {
                            currentY = change.position.y
                            val dx = kotlin.math.abs(change.position.x - startPosition.x)
                            val dy = kotlin.math.abs(change.position.y - startPosition.y)
                            maxMovement = maxOf(maxMovement, dx, dy)
                            if (!multiTouch && dy > 4f) controlsVisible = true
                            tryLockCurrentHold()
                            if (!multiTouch && (holdActivated || unlockArmed || dy > viewConfiguration.touchSlop)) {
                                change.consume()
                            }
                        }
                        anyPressed = event.changes.any { it.pressed }
                    }
                activation.cancel()

                val heldMs = android.os.SystemClock.uptimeMillis() - pressStartedMs
                unlockDoubleSpeedArmed = false

                if (multiTouch) {
                    if (holdActivated && !lockedThisGesture) {
                        onEndTemporaryDoubleSpeed()
                        temporaryDoubleSpeed = false
                    }
                    return@awaitEachGesture
                }

                if (startedAtDoubleSpeed && unlockArmed) {
                    onSpeed(1f)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    lastSeekSide = null
                    lastSeekTapMs = 0L
                    return@awaitEachGesture
                }

                if (holdActivated) {
                    if (!lockedThisGesture) {
                        onEndTemporaryDoubleSpeed()
                        temporaryDoubleSpeed = false
                    }
                    lastSeekSide = null
                    lastSeekTapMs = 0L
                    return@awaitEachGesture
                }

                if (heldMs < HOLD_2X_ACTIVATION_MS && maxMovement <= viewConfiguration.touchSlop) {
                    val nowMs = android.os.SystemClock.uptimeMillis()
                    val side = seekSideForX(startPosition.x, size.width.toFloat())
                    if (sideDoubleTapSeeks(side, nowMs, lastSeekSide, lastSeekTapMs)) {
                        val delta = when (side) {
                            SeekSide.LEFT -> -seekOffsetMs
                            SeekSide.RIGHT -> seekOffsetMs
                            null -> return@awaitEachGesture
                        }
                        onSeekBy(delta)
                        seekFeedback = delta to nowMs
                        lastSeekSide = null
                        lastSeekTapMs = 0L
                    } else {
                        lastSeekSide = side
                        lastSeekTapMs = nowMs
                    }
                } else {
                    lastSeekSide = null
                    lastSeekTapMs = 0L
                }
                }
            }
        })
        val speedGestureLabel = when {
            unlockDoubleSpeedArmed -> uiText(playback.appLanguage, "Release for 1×", "放開回到 1×")
            temporaryDoubleSpeed -> uiText(playback.appLanguage, "2× · Pull down to lock", "2× · 下拉鎖定")
            isDoubleSpeed(playback.speed) -> uiText(playback.appLanguage, "2× · Locked", "2× · 已鎖定")
            else -> null
        }
        AnimatedVisibility(
            visible = speedGestureLabel != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        ) {
            Text(
                speedGestureLabel.orEmpty(),
                color = GaVideoOverlay.foreground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(GaVideoOverlay.scrim)
                    .padding(horizontal = GaSpacing.md, vertical = GaSpacing.sm),
            )
        }
        SeekFeedback(seekFeedback.first, seekFeedback.second, Modifier.align(if (seekFeedback.first < 0) Alignment.CenterStart else Alignment.CenterEnd))

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (immersive) {
                                    NowPlayingTopBar(
                                        language = playback.appLanguage,
                                        onPictureInPicture = onPictureInPicture,
                                        onHome = onHome,
                                        onFullscreen = onFullscreen,
                                        onClose = onClose,
                                        overlay = true,
                                        fullscreen = true,
                                        onLocateCurrent = onLocateCurrent,
                                        onLockAnchorBoundsChanged = onLockAnchorBoundsChanged,
                                        modifier = Modifier.align(Alignment.TopCenter)
                                            .windowInsetsPadding(playerStatusInsets()),
                                    )
                                }

                if (immersive) Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(playerNavigationInsets()).padding(bottom = 64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayIconButton(onClick = onPrevious, enabled = playback.hasPrevious) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = GaVideoOverlay.foreground, modifier = Modifier.size(GaControl.prominentIcon))
                    }
                    // Keep the center play control available whenever controls are visible.
                    Box(
                        modifier = Modifier.size(GaControl.hero).clip(CircleShape)
                            .background(GaVideoOverlay.playSurface).clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (playback.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(GaControl.heroIcon),
                            tint = GaVideoOverlay.foreground,
                        )
                    }
                    OverlayIconButton(onClick = onNext, enabled = playback.hasNext) {
                        Icon(Icons.Rounded.SkipNext, "Next", tint = GaVideoOverlay.foreground, modifier = Modifier.size(GaControl.prominentIcon))
                    }
                }

                val timelineModifier = if (immersive) {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(playerNavigationInsets())
                } else {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                }
                if (immersive) Column(
                    modifier = timelineModifier.padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    CompactSlider(
                        value = position.coerceIn(0f, maximum),
                        onValueChange = { seeking = true; seekPosition = it },
                        onValueChangeFinished = { onSeek(seekPosition.toLong()); seeking = false },
                        valueRange = 0f..maximum,
                        enabled = hasDuration,
                        activeColor = GaVideoOverlay.foreground,
                        inactiveColor = GaVideoOverlay.inactiveTrack,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatDuration(if (seeking) position.toLong() else playback.positionMs),
                            color = GaVideoOverlay.foreground,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(formatDuration(playback.durationMs), color = GaVideoOverlay.foreground, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlayer(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    queue: List<MediaFile>,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    blackDiscMode: Boolean,
    language: AppLanguage,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    seekOffsetMs: Long = 5_000L,
    onSeekBy: (Long) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onLoadWaveform: suspend (String) -> FloatArray?,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onShareQueue: () -> Unit,
    onAddQueueItemToList: (MediaFile) -> Unit,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onShareCurrentMedia: () -> Unit,
    queueListState: androidx.compose.foundation.lazy.LazyListState,
    locateTrigger: Int,
    onLocateCurrent: () -> Unit,
    onLockAnchorBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
) {
    var waveformLoading by remember(playback.currentPath) { mutableStateOf(true) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val waveform by produceState<FloatArray?>(null, playback.currentPath) {
        value = null
        // Playback and artwork get the first frame; stale requests are cancelled by
        // produceState when the user skips rapidly.
        value = playback.currentPath?.let { onLoadWaveform(it) }
        waveformLoading = false
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(playerStatusInsets())
            .windowInsetsPadding(playerNavigationInsets()),
    ) {
        val artSize = minOf(maxWidth * 0.72f, maxHeight * 0.30f)
        val artworkShape = if (blackDiscMode) CircleShape else RoundedCornerShape(GaRadius.panel)
        var seekFeedback by remember { mutableStateOf(0L to 0L) }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .size(artSize)
                .shadow(4.dp, artworkShape, clip = false)
                .clip(artworkShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = NOW_PLAYING_ART_STAGE_ALPHA))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = NOW_PLAYING_AMBIENT_OUTLINE_ALPHA),
                    artworkShape,
                )
                .pointerInput(seekOffsetMs, playback.currentPath) {
                    var lastSeekSide: SeekSide? = null
                    var lastSeekTapMs = 0L
                    detectTapGestures(
                        onTap = { offset ->
                            val side = seekSideForX(offset.x, size.width.toFloat())
                            val nowMs = android.os.SystemClock.uptimeMillis()
                            if (sideDoubleTapSeeks(side, nowMs, lastSeekSide, lastSeekTapMs)) {
                                val delta = when (side) {
                                    SeekSide.LEFT -> -seekOffsetMs
                                    SeekSide.RIGHT -> seekOffsetMs
                                    null -> return@detectTapGestures
                                }
                                onSeekBy(delta)
                                seekFeedback = delta to nowMs
                                lastSeekSide = null
                                lastSeekTapMs = 0L
                            } else {
                                lastSeekSide = side
                                lastSeekTapMs = nowMs
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (blackDiscMode) {
                BlackDiscArtwork(
                    artwork = artwork,
                    isPlaying = playback.isPlaying,
                    mediaPath = playback.currentPath,
                    language = language,
                    modifier = Modifier.fillMaxSize().padding(5.dp),
                )
            } else if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = uiText(language, "Album artwork", "專輯封面"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                        .clip(if (blackDiscMode) CircleShape else RoundedCornerShape(GaRadius.chrome)),
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(116.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            SeekFeedback(seekFeedback.first, seekFeedback.second, Modifier.align(if (seekFeedback.first < 0) Alignment.CenterStart else Alignment.CenterEnd))
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CurrentMediaHeader(
                playback = playback,
                mediaFile = queue.getOrNull(playback.currentQueueIndex)
                    ?.takeIf { it.path == playback.currentPath }
                    ?: queue.firstOrNull { it.path == playback.currentPath },
                isFavourite = isFavourite,
                onToggleFavourite = onToggleFavourite,
                onShareCurrentMedia = onShareCurrentMedia,
                onShareQueue = onShareQueue,
                canShareQueue = queue.isNotEmpty(),
                headline = true,
                onSearch = { searchOpen = !searchOpen },
            )
            if (playback.showAbRepeat || playback.showSleepControl) {
                Spacer(Modifier.height(4.dp))
                SecondaryControlRow(playback, onSleepTimer, sleepTimer)
            }
            PlaybackError(playback.errorMessage)
            Spacer(Modifier.height(10.dp))
            NowPlayingQueue(
                searchOpen = searchOpen,
                onCloseSearch = { searchOpen = false },
                queue = queue,
                lyrics = lyrics,
                showFileDetails = showFileDetails,
                editableQueue = editableQueue,
                positionMs = if (lyrics == null) 0L else playback.positionMs,
                currentQueueIndex = playback.currentQueueIndex,
                                language = language,
                                onPlay = onPlayQueueItem,
                                onSeek = onSeek,
                                onLoadThumbnail = onLoadThumbnail,
                                onMoveQueueItem = onMoveQueueItem,
                                onRemoveQueueItem = onRemoveQueueItem,
                                onAddQueueItemToList = onAddQueueItemToList,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                queueListState = queueListState,
                                locateTrigger = locateTrigger,
                            )
            WaveformTimeline(playback, waveform, onSeek, language, waveformLoading, artwork)
            PlayerBottomControls(playback, onRepeat, onPrevious, onTogglePlay, onNext, onSpeed)
        }
        }

        Spacer(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = GaSpacing.md, top = GaSpacing.xs)
                .size(GaControl.touchTarget)
                .onGloballyPositioned { onLockAnchorBoundsChanged(it.boundsInWindow()) }
                .inspectElement("PLAYER_LOCK_ANCHOR", "Measured audio lock slot"),
        )
    }
}

@Composable
private fun BlackDiscArtwork(
    artwork: Bitmap?,
    isPlaying: Boolean,
    mediaPath: String?,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val rotation = remember(mediaPath) { Animatable(0f) }
    val spindleColor = MaterialTheme.colorScheme.secondary.copy(alpha = .72f)
    LaunchedEffect(mediaPath, isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 14_000, easing = LinearEasing),
            )
            if (rotation.value >= 3600f) rotation.snapTo(rotation.value % 360f)
        }
    }
    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(Color(0xFF070808), radius)
            listOf(.94f, .87f, .79f, .70f, .61f, .52f).forEachIndexed { index, scale ->
                drawCircle(
                    color = if (index % 2 == 0) Color.White.copy(alpha = .11f) else Color.Black.copy(alpha = .72f),
                    radius = radius * scale,
                    style = Stroke(width = (1f + index * .18f).dp.toPx()),
                )
            }
            drawCircle(Color(0xFF171A1A), radius * .23f)
            drawCircle(spindleColor, radius * .055f)
        }
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = uiText(language, "Album artwork", "專輯封面"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(.36f).clip(CircleShape),
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFFCAD0CB),
                modifier = Modifier.fillMaxSize(.16f),
            )
        }
    }
}

@Composable
private fun SecondaryControls(
    playback: PlaybackUiState,
    queue: List<MediaFile>,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    language: AppLanguage,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    onPlayQueueItem: (Int) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onSeek: (Long) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onShareQueue: () -> Unit,
    onAddQueueItemToList: (MediaFile) -> Unit,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onShareCurrentMedia: () -> Unit,
    modifier: Modifier,
    queueListState: androidx.compose.foundation.lazy.LazyListState,
    locateTrigger: Int,
    onLocateCurrent: () -> Unit,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .windowInsetsPadding(playerNavigationInsets())
            // Keep the same ambient backdrop visible through the lower player region.
            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        val chromeShape = RoundedCornerShape(GaRadius.chrome)
        Box(
            Modifier.fillMaxWidth()
                .clip(chromeShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = NOW_PLAYING_AMBIENT_PANEL_ALPHA))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = NOW_PLAYING_AMBIENT_OUTLINE_ALPHA),
                    chromeShape,
                )
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
        CurrentMediaHeader(
            playback = playback,
            mediaFile = queue.getOrNull(playback.currentQueueIndex)
                ?.takeIf { it.path == playback.currentPath }
                ?: queue.firstOrNull { it.path == playback.currentPath },
            isFavourite = isFavourite,
            onToggleFavourite = onToggleFavourite,
            onShareCurrentMedia = onShareCurrentMedia,
            onShareQueue = onShareQueue,
            canShareQueue = queue.isNotEmpty(),
            headline = false,
            onSearch = { searchOpen = !searchOpen },
        )
        }
        if (playback.showAbRepeat || playback.showSleepControl) {
            Spacer(Modifier.height(4.dp))
            SecondaryControlRow(playback, onSleepTimer, sleepTimer)
        }
        PlaybackError(playback.errorMessage)
        Spacer(Modifier.height(6.dp))
        NowPlayingQueue(
                    searchOpen = searchOpen,
                    onCloseSearch = { searchOpen = false },
                    queue = queue,
                    lyrics = lyrics,
                    showFileDetails = showFileDetails,
                    editableQueue = editableQueue,
                    positionMs = if (lyrics == null) 0L else playback.positionMs,
                    currentQueueIndex = playback.currentQueueIndex,
                    language = language,
                    onPlay = onPlayQueueItem,
                    onSeek = onSeek,
                    onLoadThumbnail = onLoadThumbnail,
                    onMoveQueueItem = onMoveQueueItem,
                    onRemoveQueueItem = onRemoveQueueItem,
                    onAddQueueItemToList = onAddQueueItemToList,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    queueListState = queueListState,
                    locateTrigger = locateTrigger,
                )
        Column(
            Modifier.fillMaxWidth()
                .clip(chromeShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = NOW_PLAYING_AMBIENT_PANEL_ALPHA))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = NOW_PLAYING_AMBIENT_OUTLINE_ALPHA),
                    chromeShape,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Timeline(playback, onSeek)
            PlayerBottomControls(playback, onRepeat, onPrevious, onTogglePlay, onNext, onSpeed)
        }
    }
}

@Composable
private fun NowPlayingQueue(
    searchOpen: Boolean,
    onCloseSearch: () -> Unit,
    queue: List<MediaFile>,
    currentQueueIndex: Int,
    language: AppLanguage,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    positionMs: Long,
    onPlay: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onAddQueueItemToList: (MediaFile) -> Unit,
    modifier: Modifier = Modifier,
    queueListState: androidx.compose.foundation.lazy.LazyListState,
    locateTrigger: Int,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            searchFocus.requestFocus()
            keyboard?.show()
        } else {
            query = ""
            keyboard?.hide()
        }
    }
    BackHandler(enabled = searchOpen) { onCloseSearch() }
    val stack by com.local.listentomusic.playback.StackPlayback.state.collectAsState()
    val stackRows = remember(stack.slots, query) { stack.slots.filter { query.isBlank() || it.file.name.contains(query.trim(), true) } }
    val queueEntries = remember(queue) { com.local.listentomusic.model.queueEntries(queue) }
    val visibleQueue = remember(queueEntries, query) {
        val normalized = query.trim()
        queueEntries.filter { entry ->
            val file = entry.file
            normalized.isBlank() ||
                file.name.contains(normalized, ignoreCase = true) ||
                file.artist.contains(normalized, ignoreCase = true) ||
                file.album.contains(normalized, ignoreCase = true)
        }
    }
    val listState = queueListState
    var openActionsKey by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleIndex = remember(visibleQueue, currentQueueIndex, stackRows, stack.primaryPath, stack.active) {
        if (stack.active) stackRows.indexOfFirst { it.file.path == stack.primaryPath }
        else visibleQueue.indexOfFirst { it.index == currentQueueIndex }
    }
    LaunchedEffect(currentQueueIndex, visibleIndex) {
        if (visibleIndex >= 0 && !listState.isScrollInProgress) listState.scrollToItem(visibleIndex)
    }
    LaunchedEffect(locateTrigger) {
        if (locateTrigger > 0 && visibleIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(visibleIndex)
        }
    }
    LaunchedEffect(listState) {
        try {
            snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
                ListScrollBudget.set("now_playing_queue", scrolling)
                if (scrolling) openActionsKey = null
            }
        } finally {
            ListScrollBudget.set("now_playing_queue", false)
        }
    }
    Column(modifier.inspectElement("NOW_PLAYING_QUEUE", "Ordered playback queue and optional synchronized lyrics")) {
        if (searchOpen) Row(Modifier.fillMaxWidth().inspectElement("QUEUE_SEARCH_FIELD", "Filters the current playback queue"), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).height(52.dp).focusRequester(searchFocus),
                    singleLine = true,
                    placeholder = { Text(uiText(language, "Search current queue", "搜尋目前播放佇列")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { if (query.isNotEmpty()) query = "" else onCloseSearch() }) {
                            Icon(Icons.Rounded.Clear, uiText(language, "Clear", "清除"))
                        }
                    },
                )
        }
        if (lyrics != null) {
            SyncedLyricsPanel(
                lyrics = lyrics,
                positionMs = positionMs,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth().height(118.dp),
            )
            Spacer(Modifier.height(6.dp))
        }
        if (stack.active) {
            // Mix rows share the flexible list budget, never the fixed identity
            // header. Eight voices must not push seek or transport off screen.
            LazyColumn(Modifier.fillMaxSize().inspectElement("NOW_PLAYING_STACK_ROWS", "Scrollable simultaneous mix"),
                state = listState, contentPadding = PaddingValues(vertical = 4.dp)) {
                items(stackRows, key = { it.file.path }) { slot ->
                    val primary = slot.file.path == stack.primaryPath
                    val available = slot.error == null && (slot.resolvedDurationMs <= 0 ||
                        com.local.listentomusic.playback.stackVoiceTarget(stack.positionMs, slot.offsetMs) < slot.resolvedDurationMs)
                    Row(Modifier.fillMaxWidth().background(if (primary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
                        else Color.Transparent).clickable(enabled = available && !primary) {
                            com.local.listentomusic.playback.StackPlayback.setPrimary(slot.file.path)
                        }.inspectElement("NOW_PLAYING_STACK_SUBROW", "Select primary video: ${slot.file.name}")
                        .padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        QueueThumbnail(slot.file, onLoadThumbnail) { listState.isScrollInProgress }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(slot.file.name.substringBeforeLast('.'), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium, fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal)
                            val status = when {
                                slot.error != null -> uiText(language, "Playback unavailable", "無法播放")
                                primary -> uiText(language, "Primary visual", "主要畫面")
                                slot.muted -> uiText(language, "Muted", "已靜音")
                                slot.solo -> uiText(language, "Solo", "獨奏")
                                else -> "${(slot.volume * 100).toInt()}% · " + java.lang.String.format(java.util.Locale.ROOT, "%+.2fs", slot.offsetMs / 1000.0)
                            }
                            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (primary) Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        } else if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    uiText(language, "No songs in this list", "這個列表沒有歌曲"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().inspectElement("NOW_PLAYING_QUEUE_LIST", "Scrollable playback queue"),
                state = listState,
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(visibleQueue, key = { entry -> entry.stableKey }, contentType = { "queue-song" }) { entry ->
                    val index = entry.index
                    val file = entry.file
                    val selected = index == currentQueueIndex
                    val actionsOpen = openActionsKey == entry.stableKey
                    val revealProgress by animateFloatAsState(
                        targetValue = if (actionsOpen) 1f else 0f,
                        animationSpec = tween(durationMillis = GaMotion.standardMs),
                        label = "queue-actions-reveal",
                    )
                    val density = LocalDensity.current
                    val actionSize = 48.dp
                    val actionWidth = actionSize
                    val actionWidthPx = with(density) { actionWidth.toPx() }
                    val rowShape = RoundedCornerShape(GaRadius.control)
                    Box(
                        Modifier.fillMaxWidth().clip(rowShape)
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
                                rowShape,
                            )
                            .inspectElement("NOW_PLAYING_QUEUE_ROW", file.name),
                    ) {
                        IconButton(
                            onClick = {
                                openActionsKey = null
                                onAddQueueItemToList(file)
                            },
                            modifier = Modifier.align(Alignment.CenterEnd).size(actionSize)
                                .drawWithContent content@{
                                    // The underlay never moves. Reveal only the strip exposed
                                    // by the translated foreground so transparent rows cannot
                                    // leak the action through before it is uncovered.
                                    clipRect(left = size.width * (1f - revealProgress)) {
                                        this@content.drawContent()
                                    }
                                }
                                .inspectElement("QUEUE_ADD_TO_LIST", "Add ${file.name} to a song list"),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistAdd,
                                uiText(language, "Add to list", "加入列表"),
                                Modifier.size(22.dp),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .graphicsLayer { translationX = -actionWidthPx * revealProgress }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = NOW_PLAYING_AMBIENT_ROW_ALPHA),
                                )
                                .clickable {
                                    if (actionsOpen) openActionsKey = null else onPlay(index)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            QueueThumbnail(
                                file = file,
                                onLoadThumbnail = onLoadThumbnail,
                                isScrolling = { listState.isScrollInProgress },
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    com.local.listentomusic.model.mediaTitle(file.name, file.sourcePath),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                )
                                if (showFileDetails) {
                                    Text(
                                        queueDetails(file),
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (selected) {
                                Box(
                                    Modifier.size(7.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary),
                                )
                            }
                            IconButton(
                                onClick = { openActionsKey = if (actionsOpen) null else entry.stableKey },
                                modifier = Modifier.size(GaControl.touchTarget).inspectElement("QUEUE_MORE_BUTTON", "Actions for ${file.name}"),
                            ) {
                                Icon(Icons.Rounded.MoreVert, uiText(language, "Actions", "操作"))
                            }
                            if (editableQueue) {
                                IconButton(onClick = { onMoveQueueItem(index, index - 1) }, enabled = index > 0) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, uiText(language, "Move up", "上移"))
                                }
                                IconButton(onClick = { onMoveQueueItem(index, index + 1) }, enabled = index < queue.lastIndex) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, uiText(language, "Move down", "下移"))
                                }
                                IconButton(onClick = { onRemoveQueueItem(index) }, enabled = queue.size > 1) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, uiText(language, "Remove", "移除"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsPanel(
    lyrics: LocalLyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSynced = lyrics.lines.any { it.timeMs > 0L }
    val activeIndex = if (isSynced) lyrics.lines.indexOfLast { it.timeMs <= positionMs } else -1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex.coerceAtLeast(0))
    LaunchedEffect(activeIndex, lyrics.sourcePath) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex, scrollOffset = -24)
    }
    LazyColumn(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(
            lyrics.lines,
            key = { index, line -> "${line.timeMs}:$index" },
        ) { index, line ->
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    if (line.words.isEmpty()) append(line.text) else line.words.forEach { word ->
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = if (index == activeIndex && word.timeMs <= positionMs)
                            MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)))
                        append(word.text); pop()
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable(enabled = isSynced) { onSeek(line.timeMs) }
                    .padding(vertical = 5.dp),
                color = if (index == activeIndex) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f),
                style = if (index == activeIndex) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium,
                fontWeight = if (index == activeIndex) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun queueDetails(file: MediaFile): String = buildList {
    file.sourcePath.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.uppercase()?.let(::add)
    file.durationMs.takeIf { it > 0L }?.let { add(formatDuration(it)) }
    file.sizeBytes.takeIf { it > 0L }?.let { add(formatBytes(it)) }
}.joinToString("  •  ")

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

@Composable
internal fun QueueThumbnail(
    file: MediaFile,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    isScrolling: () -> Boolean = { false },
) {
    var thumbnail by remember(file.path, file.sizeBytes, file.modifiedMs, file.coverUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file.path, file.sizeBytes, file.modifiedMs, file.coverUri) {
        // ListScrollBudget gates cache misses centrally, not RAM hits in each row.
        if (thumbnail != null) return@LaunchedEffect
        thumbnail = onLoadThumbnail(file)
    }
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                if (file.kind == MediaKind.VIDEO) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun Timeline(playback: PlaybackUiState, onSeek: (Long) -> Unit) {
    var seeking by remember(playback.currentPath) { mutableStateOf(false) }
    var seekPosition by remember(playback.currentPath) { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    val maximum = if (hasDuration) playback.durationMs.toFloat() else 1f
    val position = if (seeking) seekPosition else if (hasDuration) playback.positionMs.toFloat() else 0f
    CompactSlider(
        value = position.coerceIn(0f, maximum),
        onValueChange = {
            seeking = true
            val range = com.local.listentomusic.playback.PracticeLoop.state.value
            val markers = if (playback.showAbRepeat && range.path == playback.currentPath) listOfNotNull(range.start, range.end) else emptyList()
            seekPosition = snapPracticePosition(it.toLong(), playback.durationMs, markers).toFloat()
        },
        onValueChangeFinished = { onSeek(seekPosition.toLong()); seeking = false },
        valueRange = 0f..maximum,
        enabled = hasDuration,
        activeColor = MaterialTheme.colorScheme.secondary,
        inactiveColor = MaterialTheme.colorScheme.outlineVariant,
    )
    PracticeMarkers(playback)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ScrubReadout(null, formatDuration(if (seeking) position.toLong() else playback.positionMs), seeking)
        Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveformTimeline(
    playback: PlaybackUiState,
    waveform: FloatArray?,
    onSeek: (Long) -> Unit,
    language: AppLanguage,
    loading: Boolean,
    artwork: Bitmap?,
) {
    var seeking by remember(playback.currentPath) { mutableStateOf(false) }
    var seekFraction by remember(playback.currentPath) { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    val playbackFraction = if (hasDuration) {
        playback.positionMs.toDouble().div(playback.durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(playbackFraction, androidx.compose.animation.core.tween(if (playback.isPlaying) 450 else 0), label = "wave-progress")
    val fraction = if (seeking) seekFraction else animatedProgress
    val active = MaterialTheme.colorScheme.secondary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val thumbScale = animateFloatAsState(if (seeking) 1.22f else 1f, androidx.compose.animation.core.tween(130), label = "wave-thumb")
    val displayPeaks = remember(waveform) {
        waveform?.takeIf { it.isNotEmpty() }?.let { raw ->
            val groups = minOf(60, raw.size)
            val peaks = FloatArray(groups) { g ->
                val first = g * raw.size / groups
                val last = ((g + 1) * raw.size / groups).coerceAtMost(raw.size)
                (first until last).maxOfOrNull { raw[it].takeIf(Float::isFinite) ?: 0f } ?: 0f
            }
            peaks.map { it.coerceIn(0f, 1f) }.toFloatArray()
        }
    }

    Slider(
            value = fraction,
            onValueChange = {
                seeking = true
                val range = com.local.listentomusic.playback.PracticeLoop.state.value
                val markers = if (playback.showAbRepeat && range.path == playback.currentPath) listOfNotNull(range.start, range.end) else emptyList()
                val position = snapPracticePosition((it * playback.durationMs).toLong(), playback.durationMs, markers)
                seekFraction = if (hasDuration) position.toFloat() / playback.durationMs else 0f
            },
            onValueChangeFinished = {
                if (hasDuration) {
                    onSeek((playback.durationMs.toDouble() * seekFraction).toLong())
                }
                seeking = false
            },
            valueRange = 0f..1f,
            enabled = hasDuration,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            thumb = {
                Box(
                    Modifier.width(4.dp).height(28.dp)
                        .graphicsLayer { scaleY = thumbScale.value }
                        .clip(RoundedCornerShape(999.dp))
                        .background(active),
                )
            },
        track = {
            AnimatedWaveformBars(displayPeaks, fraction, active, inactive, playback.isPlaying, Modifier.fillMaxWidth().height(44.dp))
        },
    )
    PracticeMarkers(playback)
    if (waveform == null) Text(
        if (loading) uiText(language, "Waveform is being prepared; seeking is ready", "正在準備波形，仍可拖曳播放位置")
        else uiText(language, "Waveform unavailable; seeking still works", "無法讀取波形，仍可拖曳播放位置"),
        style = MaterialTheme.typography.labelSmall, color = inactive,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ScrubReadout(artwork,
            formatDuration(
                if (seeking && hasDuration) {
                    (playback.durationMs.toDouble() * seekFraction).toLong()
                } else {
                    playback.positionMs
                },
            ),
            seeking,
        )
        Text(
            if (hasDuration) formatDuration(playback.durationMs) else uiText(language, "Loading duration…", "正在讀取長度…"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val range = valueRange.endInclusive - valueRange.start
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val dragged by interactions.collectIsDraggedAsState()
    val thumbScale = animateFloatAsState(if (pressed || dragged) 1.22f else 1f, androidx.compose.animation.core.tween(130), label="video-seek-thumb")
    val fraction = if (range > 0f) {
        ((value - valueRange.start) / range).coerceIn(0f, 1f)
    } else 0f
    // Spring-animate the fill so dragging feels fluid rather than stepped.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = androidx.compose.animation.core.tween(100),
        label = "sliderFill",
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        interactionSource = interactions,
        modifier = modifier.fillMaxWidth().height(48.dp).inspectElement("PLAYBACK_TIMELINE", "Drag to seek playback"),
        thumb = { _ ->
            Box(
                Modifier.size(18.dp)
                    .graphicsLayer { scaleX = thumbScale.value; scaleY = thumbScale.value }
                    .clip(CircleShape)
                    .background(activeColor)
                    .border(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), CircleShape),
            )
        },
        track = { _ ->
            Box(
                Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(999.dp))
                    .background(inactiveColor.copy(alpha = 0.34f))
            ) {
                Box(
                    Modifier.fillMaxWidth(animatedFraction).fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(activeColor),
                )
            }
        },
    )
}

@Composable
private fun PlayerBottomControls(
    playback: PlaybackUiState,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSpeed: (Float) -> Unit,
) {
    var speedMenuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.secondary
    val cycleLabel = when {
        playback.stackCount > 0 && playback.repeatMode == Player.REPEAT_MODE_ALL ->
            uiText(playback.appLanguage, "Loop", "循環")
        playback.stackCount > 0 -> uiText(playback.appLanguage, "Off", "關閉")
        playback.shuffleEnabled -> uiText(playback.appLanguage, "Random", "隨機")
        playback.repeatMode == Player.REPEAT_MODE_ONE -> uiText(playback.appLanguage, "One", "單曲")
        playback.repeatMode == Player.REPEAT_MODE_ALL -> uiText(playback.appLanguage, "All", "全部")
        else -> uiText(playback.appLanguage, "Off", "關閉")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            .inspectElement("TRANSPORT_CONTROLS", "Repeat/random, previous, play/pause, next, and speed"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
                    onClick = onRepeat,
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .height(48.dp)
                        .padding(horizontal = 4.dp)
                        .inspectElement(
                            "REPEAT_BUTTON",
                            if (playback.stackCount > 0) "Loop whole Stack: $cycleLabel" else cycleLabel,
                        ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        val stackLoop = stackTransportUsesLoopIcon(playback.stackCount)
                        val stackLoopOn = stackLoop && playback.repeatMode == Player.REPEAT_MODE_ALL
                        Icon(
                            when {
                                stackTransportUsesRepeatOneIcon(playback.stackCount, stackLoopOn) -> Icons.Rounded.RepeatOne
                                stackLoop -> Icons.Rounded.Repeat
                                playback.shuffleEnabled -> Icons.Rounded.Shuffle
                                playback.repeatMode == Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            uiText(
                                playback.appLanguage,
                                if (stackLoop) if (stackLoopOn) "Loop" else "Loop off" else "Repeat mode",
                                if (stackLoop) if (stackLoopOn) "循環" else "不循環" else "重複模式",
                            ),
                            Modifier.size(26.dp),
                            tint = if (stackLoop && !stackLoopOn) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                        )
                        Text(cycleLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
        IconButton(onClick = onPrevious, enabled = playback.hasPrevious || playback.positionMs > 4_000L,
            modifier = Modifier.size(GaControl.touchTarget).inspectElement("PREVIOUS_BUTTON", "Previous media or restart current")) {
            Icon(Icons.Rounded.SkipPrevious, uiText(playback.appLanguage, "Previous", "上一首"), modifier = Modifier.size(GaControl.prominentIcon))
        }
        Box(
            modifier = Modifier.size(GaControl.hero)
                .clip(CircleShape)
                .background(accent)
                .border(1.dp, MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.18f), CircleShape)
                .inspectElement("PLAY_PAUSE_BUTTON", if (playback.isPlaying) "Pause" else "Play")
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onTogglePlay,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playback.isPlaying) uiText(playback.appLanguage, "Pause", "暫停") else uiText(playback.appLanguage, "Play", "播放"),
                modifier = Modifier.size(GaControl.heroIcon),
                tint = MaterialTheme.colorScheme.onSecondary,
            )
        }
        IconButton(onClick = onNext, enabled = playback.hasNext, modifier = Modifier.size(GaControl.touchTarget).inspectElement("NEXT_BUTTON", "Next media")) {
            Icon(Icons.Rounded.SkipNext, uiText(playback.appLanguage, "Next", "下一首"), modifier = Modifier.size(GaControl.prominentIcon))
        }
        Box {
                    IconButton(onClick = { speedMenuOpen = true }, modifier = Modifier.size(48.dp).inspectElement("SPEED_BUTTON", speedLabel(playback.speed))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SpeedDialIcon(playback.speed, accent)
                            Text(speedLabel(playback.speed), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
            DropdownMenu(speedMenuOpen, { speedMenuOpen = false }) {
                playbackSpeeds.forEach { speed -> DropdownMenuItem(
                    text = { Text(if (speed == playback.speed) "✓  ${speedLabel(speed)}" else speedLabel(speed)) },
                    onClick = { speedMenuOpen = false; onSpeed(speed) },
                ) }
            }
        }
    }
}

@Composable
private fun CurrentMediaHeader(
    playback: PlaybackUiState,
    mediaFile: MediaFile?,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onShareCurrentMedia: () -> Unit,
    onShareQueue: () -> Unit,
    canShareQueue: Boolean,
    headline: Boolean,
    onSearch: () -> Unit,
) {
    var shareMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // The primary keeps the ordinary Now Playing header. Stack context is
        // represented only by the compact subordinate rows below it.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 4.dp)) {
                Text(
                    com.local.listentomusic.model.mediaTitle(playback.title, playback.currentPath),
                    style = if (headline) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().inspectElement("CURRENT_MEDIA_TITLE", playback.title)
                        .padding(start = if (headline) 0.dp else 2.dp)
                        .basicMarquee(iterations = 1, initialDelayMillis = 1_200),
                )
                val metadataLine = nowPlayingMetadataLine(mediaFile)
                if (metadataLine.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        metadataLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(start = if (headline) 0.dp else 2.dp),
                    )
                }
            }
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.inspectElement("FAVOURITE_BUTTON", "Stores the current file in the local Favorites list"),
            ) {
                Icon(
                    if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    uiText(playback.appLanguage, if (isFavourite) "Remove from Favorites" else "Add to Favorites", if (isFavourite) "從我的最愛移除" else "加入我的最愛"),
                    tint = if (isFavourite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onSearch, modifier = Modifier.inspectElement("QUEUE_SEARCH_BUTTON", "Search the current list")) {
                Icon(Icons.Rounded.Search, uiText(playback.appLanguage, "Search current queue", "搜尋目前播放佇列"), Modifier.size(28.dp))
            }
            Box {
                IconButton(
                    onClick = { shareMenuOpen = true },
                    modifier = Modifier.inspectElement("SHARE_BUTTON", "Choose the current file or an M3U8 queue"),
                ) {
                    Icon(Icons.Rounded.Share, uiText(playback.appLanguage, "Share", "分享"), Modifier.size(28.dp))
                }
                DropdownMenu(shareMenuOpen, { shareMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(uiText(playback.appLanguage, "Current media file", "目前媒體檔案")) },
                        enabled = playback.currentPath != null,
                        onClick = { shareMenuOpen = false; onShareCurrentMedia() },
                    )
                    DropdownMenuItem(
                        text = { Text(uiText(playback.appLanguage, "Queue as M3U8", "將播放佇列分享為 M3U8")) },
                        enabled = canShareQueue,
                        onClick = { shareMenuOpen = false; onShareQueue() },
                    )
                }
            }
        }

    }
}

@Composable
private fun SecondaryControlRow(
    playback: PlaybackUiState,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
) {
    val practice by com.local.listentomusic.playback.PracticeLoop.state.collectAsState()
    var sleepMenuOpen by remember { mutableStateOf(false) }
    val outline = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.secondary
    val controlColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    )
    val sleepLabel = when {
        sleepTimer.endOfTrack -> uiText(playback.appLanguage, "End", "播完")
        sleepTimer.active -> formatSleepRemaining(sleepTimer.remainingMs)
        else -> uiText(playback.appLanguage, "Sleep", "睡眠")
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (playback.showAbRepeat) Button(
            onClick = { com.local.listentomusic.playback.PracticeLoop.mark(playback.currentPath, playback.positionMs) },
            modifier = Modifier.weight(1f).height(GaControl.touchTarget), colors = controlColors,
            shape = RoundedCornerShape(GaRadius.control), contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(when { practice.end != null -> "A–B ×"; practice.start != null -> "Set B"; else -> "Set A" }, style = MaterialTheme.typography.labelMedium)
        }
        if (playback.showSleepControl) Box(Modifier.weight(1f)) {
            Button(
                onClick = { sleepMenuOpen = true },
                modifier = Modifier.fillMaxWidth().height(GaControl.touchTarget),
                colors = controlColors,
                shape = RoundedCornerShape(GaRadius.control),
                contentPadding = PaddingValues(horizontal = 7.dp),
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    null,
                    tint = if (sleepTimer.active) activeColor else outline,
                    modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(sleepLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
            DropdownMenu(expanded = sleepMenuOpen, onDismissRequest = { sleepMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(uiText(playback.appLanguage, "Off", "關閉")) },
                    onClick = { sleepMenuOpen = false; onSleepTimer(0L) },
                )
                sleepTimerOptions.forEach { minutes ->
                    DropdownMenuItem(
                        text = {
                            val text = when (minutes) {
                                -1L -> uiText(playback.appLanguage, "End of track", "播完這首")
                                else -> uiText(playback.appLanguage, "$minutes min", "$minutes 分鐘")
                            }
                            Text(text)
                        },
                        onClick = { sleepMenuOpen = false; onSleepTimer(minutes) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeMarkers(playback: PlaybackUiState) {
    val range by com.local.listentomusic.playback.PracticeLoop.state.collectAsState()
    if (!playback.showAbRepeat || range.path != playback.currentPath || range.start == null || playback.durationMs <= 0) return
    val color = MaterialTheme.colorScheme.secondary
    val markerTextSize = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.fontSize.toPx() }
    Canvas(Modifier.fillMaxWidth().height(18.dp).padding(horizontal = 10.dp)) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textSize = markerTextSize
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        listOf("A" to range.start, "B" to range.end).forEach { (label, time) ->
            if (time != null) {
                val x = (time.toDouble() / playback.durationMs).toFloat().coerceIn(0f, 1f) * size.width
                drawLine(color, Offset(x, 0f), Offset(x, 5.dp.toPx()), 2.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(label, (x - paint.measureText(label) / 2).coerceIn(0f, (size.width - paint.measureText(label)).coerceAtLeast(0f)), size.height - 1.dp.toPx(), paint)
            }
        }
    }
}

private fun formatSleepRemaining(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun PlaybackError(message: String?) {
    if (message != null) {
        Spacer(Modifier.height(14.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NowPlayingTopBar(
    language: AppLanguage,
    onPictureInPicture: () -> Unit,
    onHome: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
    fullscreen: Boolean = false,
    onLocateCurrent: (() -> Unit)? = null,
    onLockAnchorBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit = {},
) {
    val foreground = if (overlay) GaVideoOverlay.foreground else MaterialTheme.colorScheme.onSurface
    val background = if (overlay) GaVideoOverlay.scrim
        else MaterialTheme.colorScheme.surface.copy(alpha = NOW_PLAYING_AMBIENT_PANEL_ALPHA)
    val topBarShape = RoundedCornerShape(GaRadius.chrome)
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(52.dp)
            .clip(topBarShape)
            .background(background)
            .border(
                1.dp,
                if (overlay) Color.White.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = NOW_PLAYING_AMBIENT_OUTLINE_ALPHA),
                topBarShape,
            )
            .padding(horizontal = GaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPictureInPicture, modifier = Modifier.size(GaControl.touchTarget).inspectElement("FLOATING_PLAYER_BUTTON", "Opens the selected floating-player mode")) {
            Icon(
                Icons.Rounded.PictureInPictureAlt,
                uiText(language, "Open floating player", "開啟浮動播放器"),
                tint = foreground,
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(onClick = onHome, modifier = Modifier.size(GaControl.touchTarget).inspectElement("HOME_BUTTON", "Returns to Library")) {
            Icon(Icons.Rounded.Home, uiText(language, "Home", "首頁"), Modifier.size(30.dp), tint = foreground)
        }
        onLocateCurrent?.let { locateAction ->
            IconButton(onClick = locateAction, modifier = Modifier.size(GaControl.touchTarget).inspectElement("LOCATE_CURRENT_BUTTON", "Scroll to currently playing song in queue")) {
                Icon(Icons.Rounded.QueueMusic, uiText(language, "Locate current song", "定位當前播放"), Modifier.size(30.dp), tint = foreground)
            }
        }
        Spacer(Modifier.weight(1f))
        // The button itself stays above the global input blocker, but its position
        // comes from this real toolbar slot. Padding/action-count changes therefore
        // move it automatically instead of requiring another magic offset.
        Spacer(
            Modifier
                .size(GaControl.touchTarget)
                .onGloballyPositioned { onLockAnchorBoundsChanged(it.boundsInWindow()) }
                .inspectElement("PLAYER_LOCK_ANCHOR", "Measured video top-bar lock slot"),
        )
        IconButton(onClick = onFullscreen, modifier = Modifier.size(GaControl.touchTarget).inspectElement("FULLSCREEN_BUTTON", if (fullscreen) "Exit fullscreen" else "Enter fullscreen")) {
            Icon(
                if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                if (fullscreen) uiText(language, "Exit fullscreen", "離開全螢幕")
                    else uiText(language, "Fullscreen", "全螢幕"),
                tint = foreground,
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(GaControl.touchTarget).inspectElement("CLOSE_PLAYER_BUTTON", "Closes Now Playing without stopping playback")) {
            Icon(Icons.Rounded.Close, uiText(language, "Close player", "關閉播放器"), Modifier.size(30.dp), tint = foreground)
        }
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(GaControl.touchTarget).clip(CircleShape)
            .background(GaVideoOverlay.control),
        content = content,
    )
}

@Composable
internal fun VideoSurface(
    mediaKey: String?,
    controller: MediaController?,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
    sharedVideoView: PlayerView? = null,
    onSharedVideoReleased: () -> Unit = {},
) {
    val surfaceLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var surfaceActive by remember(surfaceLifecycle) { mutableStateOf(surfaceLifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }
    val activity = LocalContext.current.findActivity()
    DisposableEffect(surfaceLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) surfaceActive = true
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) surfaceActive = activity?.isInPictureInPictureMode == true
        }
        surfaceLifecycle.addObserver(observer)
        onDispose { surfaceLifecycle.removeObserver(observer) }
    }
    // Keep the same PlayerView across media transitions. Keying this view by path
    // created a fresh surface after Media3 had already rendered the new first frame,
    // producing generation N+1 / last-frame generation N false alarms.
    key(controller, sharedVideoView) {
        AndroidView(
        factory = { context ->
            (sharedVideoView ?: PlayerView(context)).apply {
                (parent as? ViewGroup)?.removeView(this)
                tag = if (sharedVideoView == null) "NOW_PLAYING" else "MINI_WINDOW"
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setKeepContentOnPlayerReset(true)
                if (surfaceActive) com.local.listentomusic.ui.components.VideoSurfaceOwner.attach(
                    controller, this, overlay = sharedVideoView != null)
            }
        },
        update = { view ->
            if (surfaceActive || activity?.isInPictureInPictureMode == true)
                com.local.listentomusic.ui.components.VideoSurfaceOwner.attach(controller, view,
                    overlay = sharedVideoView != null)
            else if (sharedVideoView == null) com.local.listentomusic.ui.components.VideoSurfaceOwner.detach(view)
        },
        onRelease = { view ->
            if (sharedVideoView == null) com.local.listentomusic.ui.components.VideoSurfaceOwner.detach(view)
            else onSharedVideoReleased()
        },
        modifier = modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            onBoundsChanged(
                Rect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                )
            )
        },
        )
    }
}

internal fun shouldRehideImmersiveBars(
    fullscreen: Boolean,
    statusBarsVisible: Boolean,
    navigationBarsVisible: Boolean,
): Boolean = fullscreen && (statusBarsVisible || navigationBarsVisible)

internal fun immersiveCutoutModeForSdk(sdkInt: Int): Int? = when {
    sdkInt >= Build.VERSION_CODES.R -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    sdkInt >= Build.VERSION_CODES.P -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    else -> null
}

@RequiresApi(Build.VERSION_CODES.P)
private fun windowCutoutMode(window: Window): Int =
    window.attributes.layoutInDisplayCutoutMode

@RequiresApi(Build.VERSION_CODES.P)
private fun setWindowCutoutMode(window: Window, mode: Int) {
    val attributes = window.attributes
    if (attributes.layoutInDisplayCutoutMode != mode) {
        attributes.layoutInDisplayCutoutMode = mode
        window.attributes = attributes
    }
}

/**
 * Apply one consistent edge-to-edge contract for landscape Now Playing and the
 * dedicated fullscreen Activity. System edge gestures remain Android-owned, but
 * the app window itself may render through status/navigation/cutout safe regions.
 */
internal fun enforceImmersiveWindow(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        immersiveCutoutModeForSdk(Build.VERSION.SDK_INT)?.let { mode ->
            setWindowCutoutMode(window, mode)
        }
    }
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun FullscreenEffect(enabled: Boolean, forceLandscape: Boolean = false) {
    val activity = LocalContext.current.findActivity() ?: return
    val systemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val orientation = LocalConfiguration.current.orientation

    // MainActivity handles orientation itself via configChanges, so the Activity is not
    // recreated. Reassert the fullscreen contract after the landscape/portrait relayout
    // instead of relying only on the original enter-fullscreen call.
    LaunchedEffect(activity, enabled, orientation) {
        if (enabled) {
            enforceImmersiveWindow(activity.window)
            ViewCompat.requestApplyInsets(activity.window.decorView)
        }
    }

    DisposableEffect(activity, enabled, systemDark, forceLandscape) {
        val window = activity.window
        val decorView = window.decorView
        val insets = WindowCompat.getInsetsController(window, decorView)
        val previousCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowCutoutMode(window)
        } else null

        fun enforceImmersiveBars() = enforceImmersiveWindow(window)

        val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (enabled && hasFocus) enforceImmersiveBars()
        }

        if (enabled) {
            activity.requestedOrientation = if (forceLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            enforceImmersiveBars()
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
            ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, applied ->
                val statusVisible = applied.isVisible(WindowInsetsCompat.Type.statusBars())
                val navigationVisible = applied.isVisible(WindowInsetsCompat.Type.navigationBars())
                if (shouldRehideImmersiveBars(true, statusVisible, navigationVisible)) {
                    view.post {
                        if (activity.hasWindowFocus()) enforceImmersiveBars()
                    }
                }
                applied
            }
        } else {
            insets.isAppearanceLightStatusBars = !systemDark
            insets.isAppearanceLightNavigationBars = !systemDark
        }

        onDispose {
            if (enabled) {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                }
                ViewCompat.setOnApplyWindowInsetsListener(decorView, null)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                if (previousCutoutMode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setWindowCutoutMode(window, previousCutoutMode)
                }
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
            insets.isAppearanceLightStatusBars = !systemDark
            insets.isAppearanceLightNavigationBars = !systemDark
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "$speed×"
