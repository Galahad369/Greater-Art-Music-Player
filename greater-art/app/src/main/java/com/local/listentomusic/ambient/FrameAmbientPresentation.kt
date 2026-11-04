package com.local.listentomusic.ambient

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.ui.PlayerView
import com.local.listentomusic.R
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay

/** The primary service player calls this listener on its playback thread. */
@OptIn(UnstableApi::class)
internal object FrameAmbientPlayback : VideoFrameMetadataListener {
    private val active = AtomicReference<FrameAmbientView?>()

    fun attach(view: FrameAmbientView) { active.set(view) }
    fun detach(view: FrameAmbientView) { active.compareAndSet(view, null) }

    override fun onVideoFrameAboutToBeRendered(
        presentationTimeUs: Long, releaseTimeNs: Long, format: Format, mediaFormat: MediaFormat?,
    ) {
        val view = active.get() ?: return
        if (!FrameAmbientFormatPolicy.accepts(format)) {
            view.refusePresentation()
            return
        }
        view.sourceFrame(presentationTimeUs, releaseTimeNs,
            format.width * format.pixelWidthHeightRatio / format.height)
    }
}

/** No built-in PlayerView surface. This target uses the existing surface lease. */
@OptIn(UnstableApi::class)
internal class FrameAmbientPlayerView(context: Context, attrs: AttributeSet) : PlayerView(context, attrs) {
    private var input: Surface? = null
    private var stage: Rect? = null
    private var intensity = 0f
    var onFailure: () -> Unit = {}
    val renderer = FrameAmbientView(context, object : FrameAmbientView.Listener {
        override fun surfaceReady(surface: Surface) {
            val previous = input
            input = surface
            player?.let { current ->
                if (previous != null) current.clearVideoSurface(previous)
                attachInput(current, surface)
            }
        }

        override fun failed(reason: String) { onFailure() }
    })

    init {
        setBackgroundColor(Color.TRANSPARENT)
        setShutterBackgroundColor(Color.TRANSPARENT)
        useController = false
        addView(renderer, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun getVideoSurfaceView(): View = renderer

    override fun setPlayer(player: Player?) {
        val previous = this.player
        if (previous === player) return
        FrameAmbientPlayback.detach(renderer)
        input?.let { previous?.clearVideoSurface(it) }
        super.setPlayer(player)
        if (player != null) input?.let { attachInput(player, it) }
    }

    private fun attachInput(player: Player, surface: Surface) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) {
            onFailure()
            return
        }
        FrameAmbientPlayback.attach(renderer)
        player.setVideoSurface(surface)
    }

    fun placeVideo(bounds: Rect?, light: Float) {
        if (bounds == stage && light == intensity) return
        stage = bounds
        intensity = light
        updateStage()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) updateStage()
    }

    private fun updateStage() {
        val bounds = stage ?: return
        val origin = IntArray(2)
        getLocationInWindow(origin)
        renderer.setStage(bounds.left - origin[0], bounds.top - origin[1], bounds.width(), bounds.height(), intensity)
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun hasFrameAmbientTrack(player: Player?, mediaId: String?): Boolean {
    var allowed by remember(player, mediaId) { mutableStateOf(false) }
    DisposableEffect(player, mediaId) {
        fun update() {
            allowed = player?.let { current ->
                current.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE) &&
                    current.currentMediaItem?.mediaId == mediaId && mediaId != null &&
                    current.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.any { group ->
                        (0 until group.length).any { group.isTrackSelected(it) &&
                            FrameAmbientFormatPolicy.accepts(group.getTrackFormat(it)) }
                    }
            } ?: false
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) update()
            }
        }
        player?.addListener(listener)
        update()
        onDispose { player?.removeListener(listener) }
    }
    return allowed
}

@Composable
@OptIn(UnstableApi::class)
internal fun FrameAmbientPresentation(
    player: Player, overlay: Boolean, stage: Rect?, intensity: Float,
    onFailure: () -> Unit, modifier: Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var target by remember { mutableStateOf<FrameAmbientPlayerView?>(null) }
    val failure by rememberUpdatedState(onFailure)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // If the driver cannot produce a frame, return to the normal video path.
    LaunchedEffect(target, resumed, player) {
        val view = target ?: return@LaunchedEffect
        if (resumed) {
            delay(3_000)
            if (player.isPlaying && view.player === player && view.renderer.renderedFrames() == 0) failure()
        }
    }
    AndroidView(
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.frame_ambient_video, FrameLayout(context), false)
                as FrameAmbientPlayerView).apply {
                tag = "NOW_PLAYING"
                this.onFailure = { failure() }
                target = this
            }
        },
        update = { view ->
            view.placeVideo(stage, intensity)
            if (resumed) VideoSurfaceOwner.attach(player, view, overlay)
            else VideoSurfaceOwner.detach(view)
        },
        onRelease = { view ->
            VideoSurfaceOwner.detach(view)
            view.renderer.close()
            if (target === view) target = null
        },
        modifier = modifier,
    )
}
