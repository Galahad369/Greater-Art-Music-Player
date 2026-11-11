package com.local.listentomusic.playback

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util

@OptIn(UnstableApi::class)
internal fun playbackRequested(player: Player): Boolean =
    StackPlayback.state.value.takeIf { it.active }?.playing ?: !Util.shouldShowPlayButton(player, false)

/** A buffering player is still playing by intent. A tap cancels that request. */
@OptIn(UnstableApi::class)
internal fun togglePlayback(player: Player) {
    QueueStartOrder.advance()
    val stack = StackPlayback.state.value
    if (!stack.active) Util.handlePlayPauseButtonAction(player, false)
    else if (stack.playing) StackPlayback.pause()
    else StackPlayback.play()
}
