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
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Block
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
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.nextFloatingMode
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.AspectRatio
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
import com.local.listentomusic.ui.components.adaptiveAmbientBackdrop
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

/** The video speed banner is gesture feedback, not a permanent 2× status display. */
internal enum class VideoSpeedHint { RELEASE_FOR_NORMAL, PULL_TO_LOCK, LOCK_CONFIRMED }

internal fun videoSpeedHint(
    unlockArmed: Boolean, temporaryDoubleSpeed: Boolean,
    lockConfirmationVisible: Boolean, speed: Float,
): VideoSpeedHint? = when {
    unlockArmed -> VideoSpeedHint.RELEASE_FOR_NORMAL
    temporaryDoubleSpeed -> VideoSpeedHint.PULL_TO_LOCK
    lockConfirmationVisible && isDoubleSpeed(speed) -> VideoSpeedHint.LOCK_CONFIRMED
    else -> null
}

internal fun shouldShowPlayerLock(immersiveVideo: Boolean, videoControlsVisible: Boolean): Boolean =
    !immersiveVideo || videoControlsVisible

/** Middle 30% toggles chrome; side taps retain their double-tap seek behavior. */
internal fun isCenterVideoTap(x: Float, width: Float): Boolean =
    width > 0f && x.isFinite() && seekSideForX(x, width) == null

internal fun shouldAutoHideVideoChrome(
    immersive: Boolean, visible: Boolean, seeking: Boolean,
    panActive: Boolean, playerGestureActive: Boolean, temporarySpeed: Boolean,
    speedUnlockArmed: Boolean, timelineActive: Boolean = false,
): Boolean = immersive && visible && !seeking && !panActive &&
    !playerGestureActive && !temporarySpeed && !speedUnlockArmed && !timelineActive

/** Reserve the same 48dp slot per action. Drop secondary actions when safe width shrinks. */
internal data class ToolbarActionVisibility(
    val floatingLauncher: Boolean,
    val home: Boolean,
    val floatingMode: Boolean,
)
internal fun toolbarActionVisibility(
    availableWidthDp: Float, hasRotate: Boolean, hasFrameMode: Boolean = false,
): ToolbarActionVisibility {
    // 32dp horizontal chrome. Essential: Rotate, Lock, Fullscreen, Close
    // and (on immersive video) Fit/Crop. No optional button can displace them.
    val baseDp = (if (hasRotate) 224f else 176f) + if (hasFrameMode) 48f else 0f
    return ToolbarActionVisibility(
        floatingLauncher = availableWidthDp >= baseDp + 96f,
        home = availableWidthDp >= baseDp + 48f,
        floatingMode = availableWidthDp >= baseDp + 144f,
    )
}

/** Native Media3 resizing must never select FILL (stretch). The primary
 * fullscreen video is independent of the decorative wallpaper scale setting.
 * Crop preserves aspect ratio by trimming edges; Fit preserves the whole frame.
 */
internal enum class FullscreenFrameMode { FIT, CROP }
internal fun fullscreenResizeMode(mode: FullscreenFrameMode): Int = when (mode) {
    FullscreenFrameMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    FullscreenFrameMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}

internal fun toolbarAvailableSlots(widthPx: Int, targetPx: Int, chromePx: Int): Int =
    if (targetPx <= 0) 0 else ((widthPx - chromePx).coerceAtLeast(0) / targetPx)

internal fun fittedVideoPanLimit(viewport: IntSize, aspectRatio: Float, scale: Float): Offset {
    if (viewport.width <= 0 || viewport.height <= 0 || !aspectRatio.isFinite() ||
        aspectRatio <= 0f || !scale.isFinite() || scale <= 1f) return Offset.Zero
    val width = viewport.width.toFloat()
    val height = viewport.height.toFloat()
    val fitWidth = minOf(width, height * aspectRatio)
    val fitHeight = minOf(height, width / aspectRatio)
    return Offset(
        ((fitWidth * scale - width) / 2f).coerceAtLeast(0f),
        ((fitHeight * scale - height) / 2f).coerceAtLeast(0f),
    )
}

internal fun clampVideoPan(
    requested: Offset, viewport: IntSize, aspectRatio: Float, scale: Float,
): Offset {
    val limit = fittedVideoPanLimit(viewport, aspectRatio, scale)
    return Offset(
        requested.x.coerceIn(-limit.x, limit.x),
        requested.y.coerceIn(-limit.y, limit.y),
    )
}

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

private val LocalFloatingMode = androidx.compose.runtime.compositionLocalOf { FloatingWindowMode.MINI_WINDOW }
private val LocalFloatingModeChange = androidx.compose.runtime.compositionLocalOf<(FloatingWindowMode) -> Unit> { {} }

@Composable
fun NowPlayingScreen(
    floatingWindowMode: FloatingWindowMode,
    onFloatingWindowModeChange: (FloatingWindowMode) -> Unit,
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
    onFullscreenChanged: (Boolean) -> Unit = {},
    sharedVideoView: PlayerView? = null,
    onSharedVideoReleased: () -> Unit = {},
    ambientPreferences: com.local.listentomusic.data.UserPreferences? = null,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSystemPlayer provides systemOverlay,
        LocalFloatingMode provides floatingWindowMode,
        LocalFloatingModeChange provides onFloatingWindowModeChange,
    ) {
    var fullscreen by rememberSaveable { mutableStateOf(initialFullscreen) }
    var fullscreenLandscape by rememberSaveable { mutableStateOf(false) }
    // Only aspect-preserving fullscreen options. Fit defaults to whole frame;
    // Crop trims edges when the user explicitly selects screen fill.
    var fullscreenFrameMode by rememberSaveable { mutableStateOf(FullscreenFrameMode.FIT) }
    val toggleFullscreen = {
        if (!fullscreen) fullscreenLandscape = false
        fullscreen = !fullscreen
    }
    var controlsLocked by rememberSaveable { mutableStateOf(false) }
    // The lock and the fullscreen video chrome share one visibility source.
    var videoControlsVisible by rememberSaveable { mutableStateOf(true) }
    // Both fullscreen toolbar and the separately elevated Lock action use one
    // transition timeline. Lock stays above the input blocker when controls are locked.
    val videoChromeTransition = updateTransition(
        targetState = videoControlsVisible, label = "fullscreen video chrome"
    )
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

        val backdrop = if (ambientPreferences != null && ambientPreferences.backgroundMode in listOf(
                com.local.listentomusic.data.AppBackgroundMode.AMBIENT,
                com.local.listentomusic.data.AppBackgroundMode.ADAPTIVE_DYNAMIC))
            Modifier.adaptiveAmbientBackdrop(artwork, playback.currentPath, ambientPreferences,
                visible = !fullscreen,
                extraDim = ambientPreferences.backgroundDim,
                scrolling = queueListState.isScrollInProgress,
                steady = ambientPreferences.backgroundMode == com.local.listentomusic.data.AppBackgroundMode.AMBIENT) else Modifier.ambientBackdrop(
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
        val rotateFullscreen = {
            fullscreenLandscape = if (fullscreen) !fullscreenLandscape else !landscape
            fullscreen = true
        }
        // Invalidate measured window coordinates on the *requested* rotation as
        // well as the effective viewport change; never show a stale lock anchor.
        var lockAnchorBounds by remember(
            playback.isVideo, immersiveVideo, fullscreenLandscape, maxWidth, maxHeight,
        ) {
            mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
        }
        val reportLockAnchor: (androidx.compose.ui.geometry.Rect) -> Unit = { lockAnchorBounds = it }

        FullscreenEffect(enabled = fullscreen || (playback.isVideo && landscape),
            landscape = if (fullscreen) fullscreenLandscape else null)
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
                                        onFullscreen = toggleFullscreen,
                                        onClose = onClose,
                                        onLock = { controlsLocked = !controlsLocked },
                                        locked = controlsLocked,
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
                    onFullscreen = toggleFullscreen,
                    onRotate = rotateFullscreen,
                    landscape = if (fullscreen) fullscreenLandscape else landscape,
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
                    onToggleControlsLock = { controlsLocked = !controlsLocked },
                    controlsLocked = controlsLocked,
                    frameMode = fullscreenFrameMode,
                    onToggleFrameMode = {
                        fullscreenFrameMode = if (fullscreenFrameMode == FullscreenFrameMode.FIT)
                            FullscreenFrameMode.CROP else FullscreenFrameMode.FIT
                    },
                    controlsVisible = videoControlsVisible,
                    chromeTransition = videoChromeTransition,
                    onControlsVisibilityChange = { videoControlsVisible = it },
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
            // Audio uses the same presentation actions as video. Keep them outside
            // artwork/loading branches and consume safe insets only once.
            Column(Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .windowInsetsPadding(playerStatusInsets())
                .windowInsetsPadding(playerNavigationInsets())) {
                NowPlayingTopBar(
                    language = language,
                    onPictureInPicture = onPictureInPicture,
                    onHome = onHome,
                    onFullscreen = toggleFullscreen,
                    onRotate = if (fullscreen && !systemOverlay) rotateFullscreen else null,
                    landscape = fullscreenLandscape,
                    onClose = onClose,
                    fullscreen = fullscreen,
                    onLock = { controlsLocked = !controlsLocked },
                    locked = controlsLocked,
                    onLockAnchorBoundsChanged = reportLockAnchor,
                )
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
        }
        if (controlsLocked) {
            Box(Modifier.fillMaxSize().pointerInput(immersiveVideo) {
                if (immersiveVideo) {
                    // Locked input cannot reach VideoPlayerStage. A tap only reveals
                    // the unlock control; playback/seek/zoom remain blocked.
                    detectTapGestures(onTap = { position ->
                        if (isCenterVideoTap(position.x, size.width.toFloat()))
                            videoControlsVisible = !videoControlsVisible
                        else videoControlsVisible = true
                    })
                } else {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            })
        }
        if (controlsLocked) {
            playerLockLocalOffset(playerRootBounds, lockAnchorBounds)?.let { lockOffset ->
                // The *visible* Lock/Unlock glyph lives inside NowPlayingTopBar
                // with Rotate, Fullscreen and Close, so it has identical scrim,
                // fade, geometry and clipping. Only this transparent hit target
                // is elevated over the locked-player input blocker.
                videoChromeTransition.AnimatedVisibility(
                    visible = { shouldShowPlayerLock(immersiveVideo, it) },
                    modifier = Modifier.offset { lockOffset }.size(GaControl.touchTarget),
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(180)),
                ) {
                    Box(Modifier.size(GaControl.touchTarget)
                        .inspectElement("PLAYER_UNLOCK_HIT_TARGET", "Unlocks blocked player controls")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { controlsLocked = false },
                        )
                        .semantics {
                            contentDescription = uiText(language, "Unlock player controls", "解鎖播放器控制")
                        })
                }
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
    onRotate: () -> Unit,
    landscape: Boolean,
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
    onToggleControlsLock: () -> Unit,
    controlsLocked: Boolean,
    frameMode: FullscreenFrameMode,
    onToggleFrameMode: () -> Unit,
    controlsVisible: Boolean,
    chromeTransition: Transition<Boolean>,
    onControlsVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
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
    // Clear the one-shot lock confirmation on a media change; never render
    // a permanent "2× Locked" banner over the video.
    var lockConfirmationVisible by remember(playback.currentPath) { mutableStateOf(false) }
    LaunchedEffect(lockConfirmationVisible, playback.currentPath) {
        if (lockConfirmationVisible) {
            delay(1_200)
            lockConfirmationVisible = false
        }
    }
    var panGestureActive by remember { mutableStateOf(false) }
    var playerGestureActive by remember { mutableStateOf(false) }
    var timelineInteractionActive by remember { mutableStateOf(false) }
    var lastViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var toolbarWindowBounds by remember(immersive, landscape) {
        mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    }
    var stageWindowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val density = LocalDensity.current
    val currentControlsVisible by rememberUpdatedState(controlsVisible)
    val currentImmersive by rememberUpdatedState(immersive)
    val toolbarBottomPx = if (toolbarWindowBounds != null && stageWindowBounds != null)
        (toolbarWindowBounds!!.bottom - stageWindowBounds!!.top).coerceAtLeast(0f)
    else with(density) { 72.dp.toPx() }
    val safeBottomPx = with(density) { 16.dp.toPx() } +
        WindowInsets.displayCutout.getBottom(density).toFloat() +
        playerNavigationInsets().getBottom(density).toFloat()
    val canShowTimeline = lastViewportSize.height == 0 ||
        lastViewportSize.height > toolbarBottomPx + with(density) { 90.dp.toPx() } + safeBottomPx
    val canShowTransport = lastViewportSize.height == 0 ||
        lastViewportSize.height > toolbarBottomPx + with(density) { 172.dp.toPx() } + safeBottomPx

    // A zoom/pan transform is viewport-relative, not an intrinsic media setting.
    // Do not carry it into a differently sized portrait/landscape viewport.
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(playback.currentPath, playback.videoAspectRatio) {
        videoScale = 1f
        videoOffset = Offset.Zero
    }

    // Keep the full 2.5-second auto-hide timer while playing or paused, but
    // never interrupt a live slider/zoom/hold gesture. Gestures restart the timer.
    LaunchedEffect(immersive, controlsVisible, playback.currentPath,
        seeking, timelineInteractionActive, panGestureActive, playerGestureActive,
        temporaryDoubleSpeed, unlockDoubleSpeedArmed,
    ) {
        if (shouldAutoHideVideoChrome(
                immersive, controlsVisible, seeking,
                panGestureActive, playerGestureActive,
                temporaryDoubleSpeed, unlockDoubleSpeedArmed, timelineInteractionActive,
            )
        ) {
            delay(2_500)
            onControlsVisibilityChange(false)
        }
    }

    // Reset zoom when exiting immersive mode
    LaunchedEffect(immersive) {
        onControlsVisibilityChange(true)
        if (!immersive) {
            videoScale = 1f
            videoOffset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { stageWindowBounds = it.boundsInWindow() }
            .onSizeChanged { size ->
                if (size != lastViewportSize) {
                    lastViewportSize = size
                    videoScale = 1f
                    videoOffset = Offset.Zero
                    onControlsVisibilityChange(true)
                }
            }
            .inspectElement("VIDEO_STAGE", "Side double-tap seeks; pinch to zoom in fullscreen")
            .background(Color.Black)
            .clipToBounds(),
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
                sharedVideoView, onSharedVideoReleased,
                frameMode = if (immersive) frameMode else FullscreenFrameMode.FIT)
        }
        // PlayerView is a native AndroidView and can consume touches before a
        // parent detector sees them. This transparent layer is the single input
        // surface for fullscreen gestures. It only consumes the transform after a
        // second pointer appears, so ordinary one-finger tap/drag controls remain.
        Box(
            Modifier.fillMaxSize()
                .pointerInput(immersive, playback.currentPath, playback.videoAspectRatio) {
                    if (immersive) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            panGestureActive = true
                            try {
                            var multiTouch = false
                            var oneFingerPanActive = false
                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                multiTouch = multiTouch || pressed.size > 1
                                if (multiTouch) {
                                    if (pressed.size > 1) {
                                        val nextScale = (videoScale * event.calculateZoom()).coerceIn(1f, 4f)
                                        videoOffset = clampVideoPan(
                                            videoOffset + event.calculatePan(),
                                            size, playback.videoAspectRatio, nextScale,
                                        )
                                        videoScale = nextScale
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (videoScale > 1.01f) {
                                    val move = pressed.firstOrNull { it.id == down.id }
                                    if (move != null) {
                                        val distance = (move.position - down.position).getDistance()
                                        if (oneFingerPanActive || distance > viewConfiguration.touchSlop) {
                                            oneFingerPanActive = true
                                            videoOffset = clampVideoPan(
                                                videoOffset + (move.position - move.previousPosition),
                                                size, playback.videoAspectRatio, videoScale,
                                            )
                                            move.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            if (!multiTouch && !oneFingerPanActive && videoScale > 1.01f) {
                                // Zoomed taps use the same middle toggle; side taps
                                // reveal the controls, never trigger a pan/seek.
                                if (isCenterVideoTap(down.position.x, size.width.toFloat()))
                                    onControlsVisibilityChange(!currentControlsVisible)
                                else if (!currentControlsVisible) onControlsVisibilityChange(true)
                            }
                            } finally {
                                // Cancellation on a media/configuration change must
                                // not keep the 2.5s auto-hide suspended indefinitely.
                                panGestureActive = false
                            }
                        }
                    }
                }
                // Keep the recognizer installed across scale changes; dynamically
                // replacing its Modifier used to cancel gestures mid-pinch.
                .pointerInput(seekOffsetMs, playback.currentPath, lockThresholdPx) {
            coroutineScope {
                val timerScope = this
                var lastSeekSide: SeekSide? = null
                var lastSeekTapMs = 0L
                awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                playerGestureActive = true
                try {
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
                    if (!holdActivated || lockedThisGesture || multiTouch || videoScale > 1.01f) return
                    if (!shouldLockHeldDoubleSpeed(currentY - activationY, lockThresholdPx)) return
                    if (onLockTemporaryDoubleSpeed()) {
                        lockedThisGesture = true
                        temporaryDoubleSpeed = false
                        lockConfirmationVisible = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                val activation = timerScope.launch {
                    delay(HOLD_2X_ACTIVATION_MS)
                        if (multiTouch || videoScale > 1.01f) return@launch
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
                            if (!multiTouch && dy > 4f) onControlsVisibilityChange(true)
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

                if (multiTouch || videoScale > 1.01f) {
                    if (holdActivated && !lockedThisGesture) {
                        onEndTemporaryDoubleSpeed()
                        temporaryDoubleSpeed = false
                    }
                    return@awaitEachGesture
                }

                if (startedAtDoubleSpeed && unlockArmed) {
                    lockConfirmationVisible = false
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
                    // Centre tap toggles. Side taps only reveal hidden chrome and
                    // retain the same-side double-tap seeking contract.
                    val side = seekSideForX(startPosition.x, size.width.toFloat())
                    if (currentImmersive && isCenterVideoTap(startPosition.x, size.width.toFloat())) {
                        onControlsVisibilityChange(!currentControlsVisible)
                        lastSeekSide = null
                        lastSeekTapMs = 0L
                        return@awaitEachGesture
                    }
                    if (currentImmersive && !currentControlsVisible) onControlsVisibilityChange(true)
                    val nowMs = android.os.SystemClock.uptimeMillis()
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
                } finally {
                    playerGestureActive = false
                    unlockDoubleSpeedArmed = false
                    // Release an interrupted temporary 2× hold on cancellation,
                    // but never undo an explicitly locked 2× speed.
                    if (temporaryDoubleSpeed) {
                        temporaryDoubleSpeed = false
                        onEndTemporaryDoubleSpeed()
                    }
                }
                }
            }
        })
        val speedGestureLabel = when (videoSpeedHint(
            unlockDoubleSpeedArmed, temporaryDoubleSpeed, lockConfirmationVisible, playback.speed
        )) {
            VideoSpeedHint.RELEASE_FOR_NORMAL ->
                uiText(playback.appLanguage, "Release for 1×", "放開回到 1×")
            VideoSpeedHint.PULL_TO_LOCK ->
                uiText(playback.appLanguage, "2× · Pull down to lock", "2× · 下拉鎖定")
            VideoSpeedHint.LOCK_CONFIRMED ->
                uiText(playback.appLanguage, "2× · Locked", "2× · 已鎖定")
            null -> null
        }
        AnimatedVisibility(
            visible = speedGestureLabel != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.TopCenter)
                .offset { IntOffset(0, (toolbarBottomPx + with(density) { 8.dp.toPx() }).roundToInt()) },
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
        SeekFeedback(
            seekFeedback.first,
            seekFeedback.second,
            Modifier.align(if (seekFeedback.first < 0) Alignment.CenterStart else Alignment.CenterEnd)
                // Side-seek feedback must avoid landscape camera/display cutouts.
                .then(if (immersive) Modifier.windowInsetsPadding(WindowInsets.displayCutout) else Modifier),
        )

        chromeTransition.AnimatedVisibility(
            visible = { it },
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
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
                                        speed = playback.speed,
                                        onSpeed = onSpeed,
                                        onRotate = onRotate,
                                        landscape = landscape,
                                        onLock = onToggleControlsLock,
                                        locked = controlsLocked,
                                        frameMode = frameMode,
                                        onToggleFrameMode = onToggleFrameMode,
                                        onLockAnchorBoundsChanged = onLockAnchorBoundsChanged,
                                        onTopBarBoundsChanged = { toolbarWindowBounds = it },
                                        modifier = Modifier.align(Alignment.TopCenter)
                                            // The video itself stays edge-to-edge; chrome must
                                            // avoid notches/camera cutouts on both orientations.
                                            .windowInsetsPadding(WindowInsets.displayCutout)
                                            .windowInsetsPadding(playerStatusInsets()),
                                    )
                                }

                if (immersive && canShowTransport) Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .windowInsetsPadding(playerNavigationInsets()).padding(bottom = 64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayIconButton(onClick = onPrevious, enabled = playback.hasPrevious) {
                        Icon(Icons.Rounded.SkipPrevious, uiText(playback.appLanguage, "Previous", "上一首"), tint = GaVideoOverlay.foreground, modifier = Modifier.size(GaControl.prominentIcon))
                    }
                    // Keep the center play control available whenever controls are visible.
                    Box(
                        modifier = Modifier.size(GaControl.hero).clip(CircleShape)
                            .background(GaVideoOverlay.playSurface).clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            uiText(playback.appLanguage, if (playback.isPlaying) "Pause" else "Play", if (playback.isPlaying) "暫停" else "播放"),
                            modifier = Modifier.size(GaControl.heroIcon),
                            tint = GaVideoOverlay.foreground,
                        )
                    }
                    OverlayIconButton(onClick = onNext, enabled = playback.hasNext) {
                        Icon(Icons.Rounded.SkipNext, uiText(playback.appLanguage, "Next", "下一首"), tint = GaVideoOverlay.foreground, modifier = Modifier.size(GaControl.prominentIcon))
                    }
                }

                val timelineModifier = if (immersive) {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .windowInsetsPadding(playerNavigationInsets())
                } else {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                }
                if (immersive && canShowTimeline) Column(
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
                        onInteractionActiveChange = { timelineInteractionActive = it },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatPlaybackTime(if (seeking) position.toLong() else playback.positionMs),
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
    modifier: Modifier,
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
        modifier = modifier,
    ) {
        val compactHeight = maxWidth > maxHeight
        val artSize = if (compactHeight) minOf(maxWidth * 0.25f, maxHeight * 0.65f)
            else minOf(maxWidth * 0.72f, maxHeight * 0.30f)
        val artworkShape = if (blackDiscMode) CircleShape else RoundedCornerShape(GaRadius.panel)
        var seekFeedback by remember { mutableStateOf(0L to 0L) }
        val artworkContent: @Composable () -> Unit = {
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
        }
        val playbackContent: @Composable (Modifier) -> Unit = { bodyModifier ->
        Column(
            modifier = bodyModifier
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 0.dp),
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
                                onLocateCurrent = onLocateCurrent,
                            )
            WaveformTimeline(playback, waveform, onSeek, language, waveformLoading, artwork)
            PlayerBottomControls(playback, onRepeat, onPrevious, onTogglePlay, onNext, onSpeed)
        }
        }
        // Short landscape windows put artwork beside the controls instead of
        // spending the transport's fixed height on another vertical section.
        if (compactHeight) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                artworkContent()
                playbackContent(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                artworkContent()
                playbackContent(Modifier.fillMaxWidth().weight(1f))
            }
        }
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
        modifier = modifier.testTag("BLACK_DISC_ARTWORK").graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            // The dark vinyl must remain distinguishable from the Default and
            // Void dark backgrounds, even with no embedded album artwork.
            drawCircle(Color(0xFF909A96), radius)
            drawCircle(Color(0xFF070808), radius * .975f)
            drawCircle(Color(0xFF222827), radius * .92f, style = Stroke(width = 2.dp.toPx()))
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
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    onLocateCurrent = onLocateCurrent,
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
    onLocateCurrent: () -> Unit,
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
    var handledLocateTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(locateTrigger, visibleIndex) {
        // Consume each Locate request exactly once, even after the filter changes.
        if (locateTrigger > handledLocateTrigger && visibleIndex >= 0) {
            handledLocateTrigger = locateTrigger
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
    Column(modifier.testTag("NOW_PLAYING_QUEUE").inspectElement("NOW_PLAYING_QUEUE", "Ordered playback queue and optional synchronized lyrics")) {
        if (searchOpen) Row(
            Modifier.fillMaxWidth().inspectElement("QUEUE_SEARCH_FIELD", "Locate | Search current queue | Clear/Close"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dedicated targets on opposite ends minimize accidental Locate/Close taps.
            IconButton(
                onClick = { query = ""; onLocateCurrent() },
                enabled = stack.active || currentQueueIndex in queue.indices,
                modifier = Modifier.size(GaControl.touchTarget)
                    .inspectElement("LOCATE_CURRENT_BUTTON", "Reveal and locate the playing song"),
            ) {
                Icon(Icons.Rounded.MyLocation, uiText(language, "Locate current song", "定位當前播放"))
            }
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).height(52.dp).focusRequester(searchFocus),
                singleLine = true,
                placeholder = { Text(uiText(language, "Search current queue", "搜尋目前播放佇列")) },
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { if (query.isNotEmpty()) query = "" else onCloseSearch() },
                modifier = Modifier.size(GaControl.touchTarget)
                    .inspectElement("QUEUE_SEARCH_CLEAR_CLOSE", "Clear the filter or close search"),
            ) {
                Icon(
                    Icons.Rounded.Clear,
                    uiText(language, if (query.isNotEmpty()) "Clear search" else "Close search",
                        if (query.isNotEmpty()) "清除搜尋" else "關閉搜尋"),
                )
            }
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
            val isActiveLine = index == activeIndex
            val wordPosition = if (isActiveLine) positionMs else 0L
            val spokenColor = MaterialTheme.colorScheme.secondary
            val pendingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
            // Only the active line depends on playback position; other lines reuse their text.
            val lineText = remember(line, isActiveLine, wordPosition, spokenColor, pendingColor) {
                androidx.compose.ui.text.buildAnnotatedString {
                    if (line.words.isEmpty()) append(line.text) else line.words.forEach { word ->
                        pushStyle(androidx.compose.ui.text.SpanStyle(
                            color = if (isActiveLine && word.timeMs <= wordPosition) spokenColor else pendingColor))
                        append(word.text); pop()
                    }
                }
            }
            Text(
                text = lineText,
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
        ScrubReadout(null, formatPlaybackTime(if (seeking) position.toLong() else playback.positionMs), seeking)
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
            formatPlaybackTime(
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
    onInteractionActiveChange: (Boolean) -> Unit = {},
) {
    val range = valueRange.endInclusive - valueRange.start
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val dragged by interactions.collectIsDraggedAsState()
    val activeChanged by rememberUpdatedState(onInteractionActiveChange)
    LaunchedEffect(pressed, dragged) { activeChanged(pressed || dragged) }
    DisposableEffect(Unit) { onDispose { activeChanged(false) } }
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
            .testTag("TRANSPORT_CONTROLS")
            .inspectElement("TRANSPORT_CONTROLS", "Repeat/random, previous, play/pause, next, and speed"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
                    onClick = onRepeat,
                    modifier = Modifier.size(GaControl.touchTarget)
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
                            Modifier.size(GaControl.icon).testTag("PLAYER_TRANSPORT_ICON"),
                            tint = if (stackLoop && !stackLoopOn) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                        )
                        Text(cycleLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
        IconButton(onClick = onPrevious, enabled = playback.hasPrevious || playback.positionMs > 4_000L,
            modifier = Modifier.size(GaControl.touchTarget).inspectElement("PREVIOUS_BUTTON", "Previous media or restart current")) {
            Icon(Icons.Rounded.SkipPrevious, uiText(playback.appLanguage, "Previous", "上一首"), modifier = Modifier.size(GaControl.icon).testTag("PLAYER_TRANSPORT_ICON"))
        }
        Box(
            modifier = Modifier.size(GaControl.touchTarget)
                .clip(CircleShape)
                .background(accent)
                .border(1.dp, MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.18f), CircleShape)
                .inspectElement("PLAY_PAUSE_BUTTON", if (playback.isPlaying) "Pause" else "Play")
                .testTag("PLAY_PAUSE_BUTTON")
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
                modifier = Modifier.size(GaControl.prominentIcon),
                tint = MaterialTheme.colorScheme.onSecondary,
            )
        }
        IconButton(onClick = onNext, enabled = playback.hasNext, modifier = Modifier.size(GaControl.touchTarget).inspectElement("NEXT_BUTTON", "Next media")) {
            Icon(Icons.Rounded.SkipNext, uiText(playback.appLanguage, "Next", "下一首"), modifier = Modifier.size(GaControl.icon).testTag("PLAYER_TRANSPORT_ICON"))
        }
        Box {
                    IconButton(onClick = { speedMenuOpen = true }, modifier = Modifier.size(48.dp).inspectElement("SPEED_BUTTON", speedLabel(playback.speed))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SpeedDialIcon(playback.speed, accent, Modifier.size(GaControl.icon))
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().testTag("CURRENT_MEDIA_TITLE").inspectElement("CURRENT_MEDIA_TITLE", playback.title)
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
                modifier = Modifier.size(GaControl.touchTarget).inspectElement("FAVOURITE_BUTTON", "Stores the current file in the local Favorites list"),
            ) {
                Icon(
                    if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    uiText(playback.appLanguage, if (isFavourite) "Remove from Favorites" else "Add to Favorites", if (isFavourite) "從我的最愛移除" else "加入我的最愛"),
                    tint = if (isFavourite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(GaControl.icon).testTag("PLAYER_HEADER_ACTION_ICON"),
                )
            }
            IconButton(onClick = onSearch, modifier = Modifier.size(GaControl.touchTarget).inspectElement("QUEUE_SEARCH_BUTTON", "Search the current list")) {
                Icon(Icons.Rounded.Search, uiText(playback.appLanguage, "Search current queue", "搜尋目前播放佇列"), Modifier.size(GaControl.icon).testTag("PLAYER_HEADER_ACTION_ICON"))
            }
            Box {
                IconButton(
                    onClick = { shareMenuOpen = true },
                    modifier = Modifier.size(GaControl.touchTarget).inspectElement("SHARE_BUTTON", "Choose the current file or an M3U8 queue"),
                ) {
                    Icon(Icons.Rounded.Share, uiText(playback.appLanguage, "Share", "分享"), Modifier.size(GaControl.icon).testTag("PLAYER_HEADER_ACTION_ICON"))
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
    return formatPlaybackTime(ms)
}

@Composable
private fun PlaybackError(message: String?) {
    if (message != null) {
        Spacer(Modifier.height(14.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * One visual specification for every Now Playing toolbar action, including the
 * input-safe Lock action. Its caller owns placement and chrome visibility; it
 * never creates a separate background or an independent animation clock.
 */
@Composable
private fun PlayerToolbarAction(
    onClick: () -> Unit,
    glyph: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    foreground: Color,
    inspectorId: String,
    inspectorDescription: String,
    modifier: Modifier = Modifier,
    rotationBadgeBackground: Color? = null,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(GaControl.touchTarget)
            .inspectElement(inspectorId, inspectorDescription),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Icon(glyph, description, modifier = Modifier.size(GaControl.icon), tint = foreground)
            if (rotationBadgeBackground != null) {
                Icon(
                    Icons.AutoMirrored.Rounded.RotateRight, null,
                    modifier = Modifier.size(14.dp).align(Alignment.TopEnd)
                        .background(rotationBadgeBackground, CircleShape),
                    tint = foreground,
                )
            }
        }
    }
}

@Composable
internal fun NowPlayingTopBar(
    language: AppLanguage,
    onPictureInPicture: () -> Unit,
    onHome: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
    fullscreen: Boolean = false,
    speed: Float = 1f,
    onSpeed: ((Float) -> Unit)? = null,
    onLock: (() -> Unit)? = null,
    locked: Boolean = false,
    frameMode: FullscreenFrameMode? = null,
    onToggleFrameMode: (() -> Unit)? = null,
    onRotate: (() -> Unit)? = null,
    landscape: Boolean = false,
    onLockAnchorBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onTopBarBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit = {},
) {
    val foreground = if (overlay) GaVideoOverlay.foreground else MaterialTheme.colorScheme.onSurface
    val background = if (overlay) GaVideoOverlay.scrim
        else MaterialTheme.colorScheme.surface.copy(alpha = NOW_PLAYING_AMBIENT_PANEL_ALPHA)
    val foregroundColor = foreground
    val backgroundColor = background
    val topBarShape = RoundedCornerShape(GaRadius.chrome)
    BoxWithConstraints(modifier.fillMaxWidth()
        .onGloballyPositioned { onTopBarBoundsChanged(it.boundsInWindow()) }) {
        // Keep 48dp targets at every width. Under 224dp, provide two compact
        // rows rather than squeezing/clipping Rotate, Lock, Fullscreen and Close.
        val density = LocalDensity.current
        // Round the combined inset once. At 420dpi, independently rounded
        // 12dp + 4dp pads steal two pixels from six exact 48dp targets.
        val outerPx = with(density) { 12.dp.roundToPx() }
        val insetPx = with(density) { 16.dp.roundToPx() }
        val innerPadding = with(density) { (insetPx - outerPx).toDp() }
        val slots = toolbarAvailableSlots(constraints.maxWidth,
            with(density) { GaControl.touchTarget.roundToPx() }, insetPx * 2)
        val canToggleFrame = frameMode != null && onToggleFrameMode != null
        val hasSpeed = fullscreen && onSpeed != null
        val actions = toolbarActionVisibility(32f + (slots - if (hasSpeed) 1 else 0) * 48f, onRotate != null, canToggleFrame)
        val essentialSlots = (if (onRotate != null) 4 else 3) + (if (canToggleFrame) 1 else 0) + (if (hasSpeed) 1 else 0)
        val compact = slots < essentialSlots
        val oneColumn = slots < 2
        val chrome = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(topBarShape)
            .background(backgroundColor)
            .border(
                1.dp,
                if (overlay) Color.White.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = NOW_PLAYING_AMBIENT_OUTLINE_ALPHA),
                topBarShape,
            )
            .padding(horizontal = innerPadding)

        val speedButton: @Composable () -> Unit = {
            if (hasSpeed) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(GaControl.touchTarget)
                        .inspectElement("FULLSCREEN_SPEED_BUTTON", speedLabel(speed))) {
                        Text(speedLabel(speed), color = foregroundColor,
                            style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        playbackSpeeds.forEach { choice ->
                            DropdownMenuItem(text = { Text(if (choice == speed) "✓  ${speedLabel(choice)}" else speedLabel(choice)) },
                                onClick = { menuOpen = false; onSpeed(choice) })
                        }
                    }
                }
            }
        }
        val rotateButton: @Composable () -> Unit = {
            if (onRotate != null) PlayerToolbarAction(
                onClick = onRotate,
                glyph = if (landscape) Icons.Rounded.StayCurrentPortrait else Icons.Rounded.StayCurrentLandscape,
                description = fullscreenRotationLabel(language, landscape),
                foreground = foregroundColor,
                inspectorId = "ROTATE_SCREEN_BUTTON",
                inspectorDescription = fullscreenRotationLabel(language, landscape),
                rotationBadgeBackground = backgroundColor,
            )
        }
        val lockSlot: @Composable () -> Unit = {
            val anchored = Modifier.onGloballyPositioned {
                onLockAnchorBoundsChanged(it.boundsInWindow())
            }
            if (onLock == null) {
                // Component-only toolbar previews can reserve this slot without
                // enabling a non-functional Lock action.
                Spacer(anchored.size(GaControl.touchTarget))
            } else {
                PlayerToolbarAction(
                    onClick = onLock,
                    glyph = if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    description = uiText(language,
                        if (locked) "Unlock player controls" else "Lock player controls",
                        if (locked) "解鎖播放器控制" else "鎖定播放器控制"),
                    foreground = foregroundColor,
                    inspectorId = "PLAYER_LOCK_BUTTON",
                    inspectorDescription = if (locked)
                        "Unlock Now Playing controls" else "Lock Now Playing controls",
                    modifier = anchored,
                )
            }
        }
        val frameModeButton: @Composable () -> Unit = {
            if (canToggleFrame) PlayerToolbarAction(
                onClick = onToggleFrameMode!!,
                glyph = if (frameMode == FullscreenFrameMode.FIT) Icons.Rounded.Crop
                    else Icons.Rounded.AspectRatio,
                description = if (frameMode == FullscreenFrameMode.FIT)
                    uiText(language, "Crop video to screen", "裁切影片填滿螢幕")
                    else uiText(language, "Fit whole video", "完整顯示影片"),
                foreground = foregroundColor,
                inspectorId = "FULLSCREEN_FRAME_MODE_BUTTON",
                inspectorDescription = if (frameMode == FullscreenFrameMode.FIT)
                    "Crop video to screen (no stretch)" else "Fit whole video (no stretch)",
            )
        }
        val fullscreenButton: @Composable () -> Unit = {
            PlayerToolbarAction(
                onClick = onFullscreen,
                glyph = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                description = if (fullscreen) uiText(language, "Exit fullscreen", "離開全螢幕")
                    else uiText(language, "Fullscreen", "全螢幕"),
                foreground = foregroundColor,
                inspectorId = "FULLSCREEN_BUTTON",
                inspectorDescription = if (fullscreen) "Exit fullscreen" else "Enter fullscreen",
            )
        }
        val closeButton: @Composable () -> Unit = {
            PlayerToolbarAction(
                onClick = onClose,
                glyph = Icons.Rounded.Close,
                description = uiText(language, "Close player", "關閉播放器"),
                foreground = foregroundColor,
                inspectorId = "CLOSE_PLAYER_BUTTON",
                inspectorDescription = "Closes Now Playing without stopping playback",
            )
        }
        if (compact) {
            Column(chrome, horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasSpeed) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { speedButton() }
                if (oneColumn) {
                    rotateButton()
                    lockSlot()
                    fullscreenButton()
                    closeButton()
                    frameModeButton()
                } else {
                    Row(Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        rotateButton()
                        lockSlot()
                    }
                    Row(Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        fullscreenButton()
                        closeButton()
                    }
                    if (canToggleFrame) Row(Modifier.height(52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        frameModeButton()
                    }
                }
            }
        } else {
            Row(chrome.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                speedButton()
                if (actions.floatingLauncher) PlayerToolbarAction(
                    onClick = onPictureInPicture,
                    glyph = Icons.Rounded.PictureInPictureAlt,
                    description = uiText(language, "Open floating player", "開啟浮動播放器"),
                    foreground = foregroundColor,
                    inspectorId = "FLOATING_PLAYER_BUTTON",
                    inspectorDescription = "Opens the selected floating-player mode",
                )
                if (actions.home) PlayerToolbarAction(
                    onClick = onHome,
                    glyph = Icons.Rounded.Home,
                    description = uiText(language, "Home", "首頁"),
                    foreground = foregroundColor,
                    inspectorId = "HOME_BUTTON",
                    inspectorDescription = "Returns to Library",
                )
                val selectedMode = LocalFloatingMode.current
                val onModeChange = LocalFloatingModeChange.current
                if (actions.floatingMode) PlayerToolbarAction(
                    onClick = { onModeChange(selectedMode.nextFloatingMode()) },
                    glyph = when (selectedMode) {
                        FloatingWindowMode.MINI_WINDOW -> Icons.Rounded.PictureInPictureAlt
                        FloatingWindowMode.FOLLOW_VIDEO, FloatingWindowMode.COMPACT -> Icons.Rounded.OndemandVideo
                        FloatingWindowMode.NONE -> Icons.Rounded.Block
                    },
                    description = floatingModeDescription(language, selectedMode),
                    foreground = foregroundColor,
                    inspectorId = "FLOATING_MODE_BUTTON",
                    inspectorDescription = "Switch floating playback mode: ${floatingModeLabel(language, selectedMode)}",
                )
                Spacer(Modifier.weight(1f))
                rotateButton()
                lockSlot()
                frameModeButton()
                fullscreenButton()
                closeButton()
            }
        }
    }
}

internal fun fullscreenRotationLabel(language: AppLanguage, landscape: Boolean): String = when (language) {
    AppLanguage.ENGLISH -> if (landscape) "Rotate to portrait" else "Rotate to landscape"
    AppLanguage.TRADITIONAL_CHINESE -> if (landscape) "旋轉至直向" else "旋轉至橫向"
    AppLanguage.CANTONESE -> if (landscape) "轉返直向" else "轉做橫向"
    AppLanguage.JAPANESE -> if (landscape) "縦向きに回転" else "横向きに回転"
    AppLanguage.GERMAN -> if (landscape) "Ins Hochformat drehen" else "Ins Querformat drehen"
    AppLanguage.FRENCH -> if (landscape) "Passer en portrait" else "Passer en paysage"
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

@OptIn(UnstableApi::class)
@Composable
internal fun VideoSurface(
    mediaKey: String?,
    controller: MediaController?,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
    sharedVideoView: PlayerView? = null,
    onSharedVideoReleased: () -> Unit = {},
    frameMode: FullscreenFrameMode = FullscreenFrameMode.FIT,
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
                // Media3 API 34 Compose/SurfaceView synchronization workaround.
                // Preserve the owned PlayerView and player identity.
                if (Build.VERSION.SDK_INT == 34) setEnableComposeSurfaceSyncWorkaround(true)
                // Explicitly forbid resizeMode=FILL: Media3 Fit or Zoom (crop)
                // always preserves source/pixel aspect ratio.
                resizeMode = fullscreenResizeMode(frameMode)
                setKeepContentOnPlayerReset(true)
                if (surfaceActive) com.local.listentomusic.ui.components.VideoSurfaceOwner.attach(
                    controller, this, overlay = sharedVideoView != null)
            }
        },
        update = { view ->
            // A borrowed/reparented PlayerView can retain a previous owner mode.
            // Restore the fullscreen aspect policy on every Compose update.
            val requiredResize = fullscreenResizeMode(frameMode)
            if (view.resizeMode != requiredResize) view.resizeMode = requiredResize
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
private fun FullscreenEffect(enabled: Boolean, landscape: Boolean? = null) {
    // A WindowManager presentation delegates fullscreen to its real Activity;
    // it must not rotate an incidental Activity supplied through a wrapped context.
    if (LocalSystemPlayer.current) return
    val activity = LocalContext.current.findActivity() ?: return
    val systemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val orientation = LocalConfiguration.current.orientation

    // MainActivity handles orientation itself via configChanges, so the Activity is not
    // recreated. Reassert the fullscreen contract after the landscape/portrait relayout
    // instead of relying only on the original enter-fullscreen call.
    LaunchedEffect(activity, enabled, orientation, landscape) {
        if (enabled) {
            activity.requestedOrientation = when (landscape) {
                true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                false -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                null -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
            enforceImmersiveWindow(activity.window)
            ViewCompat.requestApplyInsets(activity.window.decorView)
        }
    }

    val latestSystemDark by rememberUpdatedState(systemDark)
    DisposableEffect(activity, enabled) {
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
            insets.isAppearanceLightStatusBars = !latestSystemDark
            insets.isAppearanceLightNavigationBars = !latestSystemDark
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
            insets.isAppearanceLightStatusBars = !latestSystemDark
            insets.isAppearanceLightNavigationBars = !latestSystemDark
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
