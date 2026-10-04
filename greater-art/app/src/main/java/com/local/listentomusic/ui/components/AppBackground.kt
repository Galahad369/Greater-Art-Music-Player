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
import kotlin.math.max

internal fun shouldAttachVideoBackground(
    visible: Boolean,
    primaryIsVideo: Boolean,
    primaryFrameReady: Boolean,
): Boolean = visible && (!primaryIsVideo || primaryFrameReady)

internal fun shouldUsePrimaryVideoBackground(
    visible: Boolean,
    allowVideoBackground: Boolean,
    isVideo: Boolean,
    controllerAvailable: Boolean,
): Boolean = visible && allowVideoBackground && isVideo && controllerAvailable

internal fun shouldClaimCurrentVideoBackground(
    usePrimaryVideoBackground: Boolean,
    surfaceActive: Boolean,
): Boolean = usePrimaryVideoBackground && surfaceActive

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

    val attachCustomVideoBackground =
        mode == AppBackgroundMode.CUSTOM_VIDEO &&
            allowVideoBackground &&
            shouldAttachVideoBackground(
                visible = visible,
                primaryIsVideo = isVideo,
                primaryFrameReady = primaryFrameReady,
            )
    val usePrimaryVideoBackground =
        mode == AppBackgroundMode.CURRENT_VIDEO &&
            currentVideoUri != null &&
            controller != null &&
            shouldUsePrimaryVideoBackground(
                visible = visible,
                allowVideoBackground = allowVideoBackground,
                isVideo = isVideo,
                controllerAvailable = true,
            )

    // During list fling drop only the presentation surface. CURRENT_VIDEO shares the
    // real player, so this cannot create/release a second decoder or a second timeline.
    var videoSurfaceActive by remember { mutableStateOf(true) }
    LaunchedEffect(listScrolling) {
        if (listScrolling) {
            videoSurfaceActive = false
        } else {
            delay(VIDEO_SURFACE_SETTLE_MS)
            videoSurfaceActive = true
        }
    }
    val liveVideoSurface =
        videoSurfaceActive && !listScrolling && (attachCustomVideoBackground || usePrimaryVideoBackground)

    Box(modifier.fillMaxSize().graphicsLayer()) {
        val ambient = artworkBackdrop(null,
            androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > .5f, currentPath)
        val videoFallback = mode == AppBackgroundMode.CUSTOM_VIDEO &&
            (preferences.customBackgroundVideoUri == null || !attachCustomVideoBackground) ||
            mode == AppBackgroundMode.CURRENT_VIDEO &&
            !usePrimaryVideoBackground
        val videoMode = mode == AppBackgroundMode.CUSTOM_VIDEO || mode == AppBackgroundMode.CURRENT_VIDEO
        if (visible && (mode == AppBackgroundMode.DEFAULT || videoFallback || (videoMode && !liveVideoSurface))) {
            if (mode == AppBackgroundMode.CURRENT_VIDEO) Box(Modifier.fillMaxSize().background(ambient))
            else DefaultMetalBackground()
        } else if (visible) {
            Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background))
        }
        when (mode) {
            AppBackgroundMode.DEFAULT -> Unit
            AppBackgroundMode.CUSTOM_IMAGE -> preferences.customBackgroundImageUri
                ?.let(Uri::parse)
                ?.let { BackgroundImage(it, preferences.backgroundScaleMode) }
            AppBackgroundMode.CUSTOM_VIDEO -> if (attachCustomVideoBackground) {
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
            AppBackgroundMode.CURRENT_VIDEO -> if (usePrimaryVideoBackground && controller != null) {
                PrimaryVideoBackground(
                    controller = controller,
                    surfaceActive = liveVideoSurface,
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
    val palette = androidx.compose.material3.MaterialTheme.colorScheme
    LiquidMetalSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RectangleShape,
        baseColor = palette.background,
        accentColor = palette.primary,
    ) {
        // One static themed gradient: no animation work behind scrolling or video.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(
                        palette.primaryContainer.copy(alpha = 0.30f),
                        palette.surface.copy(alpha = 0.55f),
                        palette.background.copy(alpha = 0.85f),
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
                BackgroundScaleMode.STRETCH -> ContentScale.Crop
                BackgroundScaleMode.CROP -> ContentScale.Crop
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PrimaryVideoBackground(
    controller: MediaController,
    surfaceActive: Boolean,
    scaleMode: BackgroundScaleMode,
    horizontalPosition: (() -> Float)?,
) {
    var videoView by remember { mutableStateOf<PlayerView?>(null) }

    // CURRENT_VIDEO is a presentation lease on the real player. There is no secondary
    // ExoPlayer, no duplicate decode, and therefore no decoder-to-decoder drift.
    DisposableEffect(controller) {
        onDispose {
            // Relinquish expected ownership before removing the candidate so reconcile can
            // switch directly to Library/Now Playing without a no-surface interval.
            VideoSurfaceOwner.setCurrentVideoBackgroundActive(false)
            videoView?.let(VideoSurfaceOwner::detach)
        }
    }

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
            (android.view.LayoutInflater.from(viewContext)
                .inflate(com.local.listentomusic.R.layout.background_video, android.widget.FrameLayout(viewContext), false) as PlayerView)
                .apply {
                    useController = false
                    tag = "BACKGROUND"
                    resizeMode = when (scaleMode) {
                        BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    setKeepContentOnPlayerReset(true)
                    videoView = this
                }
        },
        update = { view ->
            view.resizeMode = when (scaleMode) {
                BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            if (surfaceActive) {
                view.visibility = android.view.View.VISIBLE
                // Register the candidate first. Claiming BACKGROUND before registration can
                // make expectedOwner point at a missing surface and blank the player.
                VideoSurfaceOwner.attachBackground(controller, view)
                VideoSurfaceOwner.setCurrentVideoBackgroundActive(
                    shouldClaimCurrentVideoBackground(
                        usePrimaryVideoBackground = true,
                        surfaceActive = surfaceActive,
                    ),
                )
            } else {
                // During fling/hidden states give ownership back before detaching the
                // background candidate. This keeps the primary surface continuously owned.
                VideoSurfaceOwner.setCurrentVideoBackgroundActive(false)
                VideoSurfaceOwner.detach(view)
                view.visibility = android.view.View.GONE
            }
            if (videoView !== view) videoView = view
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun BackgroundVideo(
    source: Uri,
    shouldPlay: Boolean,
    surfaceActive: Boolean = true,
    scaleMode: BackgroundScaleMode = BackgroundScaleMode.CROP,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleActive by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    val backgroundPlayer = remember(source) {
        val renderersFactory = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        WALLPAPER_MIN_BUFFER_MS,
                        WALLPAPER_MAX_BUFFER_MS,
                        WALLPAPER_PLAYBACK_BUFFER_MS,
                        WALLPAPER_REBUFFER_MS,
                    )
                    .setTargetBufferBytes(WALLPAPER_TARGET_BUFFER_BYTES)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build(),
            ).build().apply {
                installVideoDiagnostics("CUSTOM_VIDEO_BACKGROUND")
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
    LaunchedEffect(backgroundPlayer, shouldPlay, lifecycleActive) {
        backgroundPlayer.playWhenReady = shouldPlay && lifecycleActive
    }

    AndroidView(
        factory = { viewContext ->
            (android.view.LayoutInflater.from(viewContext)
                .inflate(com.local.listentomusic.R.layout.background_video, android.widget.FrameLayout(viewContext), false) as PlayerView)
                .apply {
                    useController = false
                    resizeMode = when (scaleMode) {
                        BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    setKeepContentOnPlayerReset(true)
                    player = backgroundPlayer
                    visibility = android.view.View.VISIBLE
                }
        },
        update = { view ->
            view.resizeMode = when (scaleMode) {
                BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            if (surfaceActive) {
                if (view.player !== backgroundPlayer) view.player = backgroundPlayer
                view.visibility = android.view.View.VISIBLE
            } else {
                view.player = null
                view.visibility = android.view.View.GONE
            }
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

private const val WALLPAPER_MIN_BUFFER_MS = 500
private const val WALLPAPER_MAX_BUFFER_MS = 2_000
private const val WALLPAPER_PLAYBACK_BUFFER_MS = 100
private const val WALLPAPER_REBUFFER_MS = 200
private const val WALLPAPER_TARGET_BUFFER_BYTES = 2 * 1024 * 1024
private const val PRIMARY_VIDEO_HEAD_START_MS = 300L
/** After fling ends, wait a frame or two before re-attaching the wallpaper surface. */
private const val VIDEO_SURFACE_SETTLE_MS = 80L
private const val MAX_BACKGROUND_PIXELS = 1_600
