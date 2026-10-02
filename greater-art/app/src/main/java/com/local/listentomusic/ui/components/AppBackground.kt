package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.playback.installVideoDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import java.io.File
import kotlin.math.abs
import kotlin.math.max

internal fun shouldAttachVideoBackground(
    visible: Boolean,
    primaryIsVideo: Boolean,
    primaryFrameReady: Boolean,
): Boolean = visible && (!primaryIsVideo || primaryFrameReady)

internal fun shouldMirrorPrimaryPlayback(
    lifecycleActive: Boolean,
    primaryIsPlaying: Boolean,
): Boolean = lifecycleActive && primaryIsPlaying

@Composable
fun AppBackground(
    preferences: UserPreferences,
    currentPath: String?,
    isVideo: Boolean,
    controller: MediaController?,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    allowVideoBackground: Boolean = true,
    listScrolling: Boolean = false,
    horizontalPosition: (() -> Float)? = null,
) {
    val mode = preferences.backgroundMode
    val currentVideoUri = currentPath
        ?.takeIf { isVideo }
        ?.let { Uri.fromFile(File(it)) }
    var primaryFrameReady by remember(controller, currentPath, isVideo) { mutableStateOf(!isVideo) }

    // A video wallpaper is a secondary consumer. Let the real MediaController render first
    // so Library/Now Playing gets the primary decoder and surface before we allocate another.
    DisposableEffect(controller, currentPath, isVideo) {
        val primary = controller
        primaryFrameReady = !isVideo
        if (!isVideo || primary == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    primaryFrameReady = true
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    primaryFrameReady = false
                }
            }
            primary.addListener(listener)
            onDispose { primary.removeListener(listener) }
        }
    }

    // onRenderedFirstFrame can race listener registration during session reconnection. Give
    // the foreground view a short head start, then accept an already-ready matching video.
    LaunchedEffect(controller, currentPath, isVideo, visible, primaryFrameReady) {
        val primary = controller ?: return@LaunchedEffect
        if (!visible || !isVideo || primaryFrameReady) return@LaunchedEffect
        delay(PRIMARY_VIDEO_HEAD_START_MS)
        if (
            primary.currentMediaItem?.mediaId == currentPath &&
            primary.playbackState == Player.STATE_READY &&
            primary.videoSize.width > 0 &&
            primary.videoSize.height > 0
        ) {
            primaryFrameReady = true
        }
    }

    val attachVideoBackground = allowVideoBackground && shouldAttachVideoBackground(
        visible = visible,
        primaryIsVideo = isVideo,
        primaryFrameReady = primaryFrameReady,
    )

    // YouTube-style: during list fling drop the live wallpaper surface (keep the player
    // instance) and show a static base so Compose scroll is not compositing video frames.
    // Short settle delay avoids attach thrash on brief isScrollInProgress flickers.
    var videoSurfaceActive by remember { mutableStateOf(true) }
    LaunchedEffect(listScrolling) {
        if (listScrolling) {
            videoSurfaceActive = false
        } else {
            delay(VIDEO_SURFACE_SETTLE_MS)
            videoSurfaceActive = true
        }
    }
    val liveVideoSurface = attachVideoBackground && videoSurfaceActive && !listScrolling

    Box(modifier.fillMaxSize().graphicsLayer()) {
        // Stack can already own eight decoders. When decorative video is budgeted out,
        // retain a static backdrop instead of allocating another ExoPlayer/PlayerView.
        // Also paint the static base while the surface is detached for fling.
        val videoFallback = mode == AppBackgroundMode.CUSTOM_VIDEO &&
            (preferences.customBackgroundVideoUri == null || !attachVideoBackground) ||
            mode == AppBackgroundMode.CURRENT_VIDEO &&
            (currentVideoUri == null || !attachVideoBackground)
        val videoMode = mode == AppBackgroundMode.CUSTOM_VIDEO || mode == AppBackgroundMode.CURRENT_VIDEO
        if (visible && (mode == AppBackgroundMode.DEFAULT || videoFallback || (videoMode && !liveVideoSurface))) {
            DefaultMetalBackground()
        } else if (visible) {
            Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background))
        }
        when (mode) {
            AppBackgroundMode.DEFAULT -> Unit
            AppBackgroundMode.CUSTOM_IMAGE -> preferences.customBackgroundImageUri
                ?.let(Uri::parse)
                ?.let { BackgroundImage(it, preferences.backgroundScaleMode) }
            AppBackgroundMode.CUSTOM_VIDEO -> if (attachVideoBackground) {
                preferences.customBackgroundVideoUri
                    ?.let(Uri::parse)
                    ?.let {
                        BackgroundVideo(
                            source = it,
                            shouldPlay = liveVideoSurface,
                            surfaceActive = liveVideoSurface,
                            scaleMode = preferences.backgroundScaleMode,
                        )
                    }
            }
            // Independent muted output never participates in VideoSurfaceOwner.
            // Let Media3 choose a decoder with fallback; software is not presumed faster.
            AppBackgroundMode.CURRENT_VIDEO -> if (currentVideoUri != null && attachVideoBackground) {
                BackgroundVideo(
                    source = currentVideoUri,
                    shouldPlay = liveVideoSurface,
                    surfaceActive = liveVideoSurface,
                    syncController = controller,
                    scaleMode = preferences.backgroundScaleMode,
                    horizontalPosition = if (liveVideoSurface) horizontalPosition else null,
                )
            }
        }
        val isLight = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f
        val dim = if (mode == AppBackgroundMode.DEFAULT) 0.08f else preferences.backgroundDim
        val veil = if (mode == AppBackgroundMode.DEFAULT && isLight) Color.White else Color.Black
        Box(Modifier.matchParentSize().background(veil.copy(alpha = dim)))
    }
}

@Composable
private fun DefaultMetalBackground() {
    val isLight = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f
    LiquidMetalSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RectangleShape,
        baseColor = if (isLight) Color(0xFFF2F4F2) else Color(0xFF080A09),
        accentColor = if (isLight) Color(0xFF94BFB5) else Color(0xFF72D7C0),
    ) {
        // Quiet technical grid inspired by editorial motion graphics. It stays
        // subordinate to content and costs no per-frame layout work.
        Canvas(Modifier.matchParentSize()) {
            val line = if (isLight) Color.Black.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.045f)
            repeat(6) { index ->
                val x = size.width * index / 5f
                drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }
            repeat(9) { index ->
                val y = size.height * index / 8f
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    if (isLight) listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color(0xFFD7DFDC).copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.70f),
                    ) else listOf(
                        Color(0xFF07100E).copy(alpha = 0.70f),
                        Color(0xFF0B0D0C).copy(alpha = 0.34f),
                        Color.Black.copy(alpha = 0.58f),
                    ),
                ),
            ),
        )
    }
}

@Composable
private fun BackgroundImage(source: Uri, scaleMode: BackgroundScaleMode) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = source) {
        // Do not leave the previous image visible if replacement decoding fails.
        value = null
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(context.contentResolver, source)
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = when (scaleMode) {
                BackgroundScaleMode.FIT -> ContentScale.Fit
                BackgroundScaleMode.STRETCH -> ContentScale.FillBounds
                BackgroundScaleMode.CROP -> ContentScale.Crop
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun BackgroundVideo(
    source: Uri,
    shouldPlay: Boolean,
    surfaceActive: Boolean = true,
    scaleMode: BackgroundScaleMode = BackgroundScaleMode.CROP,
    syncController: MediaController? = null,
    horizontalPosition: (() -> Float)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var videoView by remember { mutableStateOf<PlayerView?>(null) }
    var lifecycleActive by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    val backgroundPlayer = remember(source) {
        val renderersFactory = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setLoadControl(androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(3_000, 10_000, 100, 250).setTargetBufferBytes(16 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false).build()).build().apply {
            installVideoDiagnostics(if (syncController != null) "CURRENT_VIDEO_BACKGROUND" else "CUSTOM_VIDEO_BACKGROUND")
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            setMediaItem(MediaItem.fromUri(source))
            prepare()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> lifecycleActive = true
                Lifecycle.Event.ON_STOP -> lifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(backgroundPlayer) {
        onDispose { backgroundPlayer.release() }
    }

    DisposableEffect(backgroundPlayer, shouldPlay, lifecycleActive, syncController) {
        val primary = syncController
        fun mirror() {
            backgroundPlayer.playWhenReady = shouldPlay && shouldMirrorPrimaryPlayback(lifecycleActive, primary?.isPlaying ?: true)
            primary?.let {
                if (abs(backgroundPlayer.playbackParameters.speed - it.playbackParameters.speed) > .001f)
                    backgroundPlayer.setPlaybackSpeed(it.playbackParameters.speed)
            }
        }
        fun align() {
            primary?.let {
                if (shouldResyncBackground(backgroundPlayer.currentPosition, it.currentPosition, false))
                    backgroundPlayer.seekTo(it.currentPosition.coerceAtLeast(0L))
            }
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { mirror() }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                align()
            }
        }
        primary?.addListener(listener)
        align()
        mirror()
        onDispose { primary?.removeListener(listener) }
    }

    // Pause, speed and user seeks are event-driven above. Correct natural drift only
    // occasionally; polling-and-seeking every 250 ms can continuously flush the decoder.
    LaunchedEffect(backgroundPlayer, shouldPlay, lifecycleActive, syncController) {
        val primary = syncController ?: return@LaunchedEffect
        if (!shouldPlay || !lifecycleActive) return@LaunchedEffect
        while (true) {
            delay(BACKGROUND_SYNC_INTERVAL_MS)
            if (backgroundPlayer.playbackState == Player.STATE_READY &&
                shouldResyncBackground(backgroundPlayer.currentPosition, primary.currentPosition, primary.isPlaying)) {
                backgroundPlayer.seekTo(primary.currentPosition.coerceAtLeast(0L))
            }
        }
    }

    // The pager owns this presentation-only position. Updating the already-attached
    // content frame does not rebuild the player, seek, or recompose the page tree.
    LaunchedEffect(videoView, horizontalPosition, scaleMode) {
        val view = videoView ?: return@LaunchedEffect
        val frame = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_content_frame)
            ?: return@LaunchedEffect
        fun move(position: Float) {
            frame.translationX = backgroundCropTranslationX(frame.width, view.width, scaleMode, position)
        }
        val layoutListener = android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            move(horizontalPosition?.invoke() ?: .5f)
        }
        view.addOnLayoutChangeListener(layoutListener)
        frame.addOnLayoutChangeListener(layoutListener)
        try {
            snapshotFlow { horizontalPosition?.invoke() ?: .5f }.collect(::move)
        } finally {
            frame.removeOnLayoutChangeListener(layoutListener)
            view.removeOnLayoutChangeListener(layoutListener)
            frame.translationX = 0f
        }
    }

    AndroidView(
        factory = { viewContext ->
            (android.view.LayoutInflater.from(viewContext).inflate(com.local.listentomusic.R.layout.background_video, android.widget.FrameLayout(viewContext), false) as PlayerView).apply {
                useController = false
                this.resizeMode = when (scaleMode) {
                    BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
                setKeepContentOnPlayerReset(true)
                // Surface starts attached; fling path detaches via update without releasing the player.
                player = backgroundPlayer
                visibility = android.view.View.VISIBLE
                videoView = this
            }
        },
        update = {
            it.resizeMode = when (scaleMode) {
                BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            // Detach surface during list fling so GPU is not compositing video under LazyColumn.
            // Keep the ExoPlayer instance prepared so settle does not re-allocate a decoder.
            if (surfaceActive) {
                if (it.player !== backgroundPlayer) it.player = backgroundPlayer
                it.visibility = android.view.View.VISIBLE
            } else {
                it.player = null
                it.visibility = android.view.View.GONE
            }
            if (videoView !== it) videoView = it
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/** PlayerView centers its enlarged ZOOM content frame; shift only within valid overflow. */
internal fun backgroundCropTranslationX(
    contentWidth: Int,
    viewportWidth: Int,
    scaleMode: BackgroundScaleMode,
    horizontalPosition: Float,
): Float {
    if (scaleMode != BackgroundScaleMode.CROP) return 0f
    val overflow = (contentWidth - viewportWidth).coerceAtLeast(0)
    return overflow * (.5f - horizontalPosition.coerceIn(0f, 1f))
}

private fun decodeSampledBitmap(
    resolver: android.content.ContentResolver,
    source: Uri,
): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val longestSide = max(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (longestSide / sampleSize > MAX_BACKGROUND_PIXELS) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

private const val PRIMARY_VIDEO_HEAD_START_MS = 300L
/** After fling ends, wait a frame or two before re-attaching the wallpaper surface. */
private const val VIDEO_SURFACE_SETTLE_MS = 80L
private const val BACKGROUND_SYNC_INTERVAL_MS = 5_000L
private const val PLAYING_SYNC_TOLERANCE_MS = 2_000L
private const val PAUSED_SYNC_TOLERANCE_MS = 80L
private const val MAX_BACKGROUND_PIXELS = 1_600

internal fun shouldResyncBackground(position: Long, target: Long, playing: Boolean): Boolean =
    abs(position - target) > if (playing) PLAYING_SYNC_TOLERANCE_MS else PAUSED_SYNC_TOLERANCE_MS
