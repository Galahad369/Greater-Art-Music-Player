package com.local.listentomusic.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.local.listentomusic.BuildConfig

/** Debug-only, event-driven diagnostics. Never log filenames, URIs, or metadata. */
internal fun ExoPlayer.installVideoDiagnostics(role: String) {
    val identity = "$role:${System.identityHashCode(this)}"
    val primary = role == "PRIMARY"
    fun report(event: String) { if (BuildConfig.DEBUG) Log.d("GreaterArtVideo", "$identity $event position=$currentPosition " +
        if (primary) com.local.listentomusic.ui.components.VideoSurfaceOwner.describe() else "owner=$role") }
    report("created")
    addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) { report("state=$state") }
        override fun onIsPlayingChanged(isPlaying: Boolean) { report("playing=$isPlaying") }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { report("ready=$playWhenReady reason=$reason") }
        override fun onVideoSizeChanged(videoSize: VideoSize) { report("size=${videoSize.width}x${videoSize.height}") }
        override fun onRenderedFirstFrame() { report("firstFrame") }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            report("mediaTransition reason=$reason")
        }
        override fun onPlayerError(error: PlaybackException) { report("error=${error.errorCodeName}") }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            report("position=${newPosition.positionMs} reason=$reason")
        }
    })
    addAnalyticsListener(object : AnalyticsListener {
        override fun onMediaItemTransition(
            eventTime: AnalyticsListener.EventTime,
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int,
        ) {
            if (primary) com.local.listentomusic.ui.components.VideoSurfaceOwner.mediaChanged(
                eventTime.realtimeMs,
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
            )
            report("analyticsMediaTransition reason=$reason realtimeMs=${eventTime.realtimeMs}")
        }
        override fun onVideoEnabled(eventTime: AnalyticsListener.EventTime, decoderCounters: androidx.media3.exoplayer.DecoderCounters) { report("videoEnabled") }
        override fun onVideoDisabled(eventTime: AnalyticsListener.EventTime, decoderCounters: androidx.media3.exoplayer.DecoderCounters) { report("videoDisabled") }
        override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
            if (primary) com.local.listentomusic.ui.components.VideoSurfaceOwner.firstFrame(output, renderTimeMs)
            report("rendererFirstFrame renderTimeMs=$renderTimeMs")
        }
        override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String,
            initializedTimestampMs: Long, initializationDurationMs: Long) {
            if (primary) com.local.listentomusic.ui.components.VideoSurfaceOwner.decoder(decoderName)
            report("decoder=$decoderName initMs=$initializationDurationMs")
        }
        override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
            if (primary) com.local.listentomusic.ui.components.VideoSurfaceOwner.codecError(videoCodecError.javaClass.simpleName)
            report("codecError=${videoCodecError.javaClass.simpleName}")
        }
        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            if (primary) com.local.listentomusic.ui.components.VideoSurfaceOwner.dropped(droppedFrames)
            report("dropped=$droppedFrames elapsedMs=$elapsedMs")
        }
        override fun onPlayerReleased(eventTime: AnalyticsListener.EventTime) { report("released") }
    })
}
