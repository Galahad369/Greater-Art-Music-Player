package com.local.listentomusic.playback

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/** Admit the original DOWN before a detector can consume movement over buttons. */
internal fun Modifier.expandedPlayerPullInput(
    enabled: Boolean,
    density: Float,
    onPull: (Float) -> Unit,
    onPullEnd: (Float, Float) -> Unit,
    onPullCancel: () -> Unit,
): Modifier = pointerInput(enabled, density, onPull, onPullEnd, onPullCancel) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.position.y !in 0f..(52f * density) ||
            !expandedPullHandleContains(down.position.x, size.width.toFloat(), density)) return@awaitEachGesture
        var firstDelta = 0f
        val slop = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
            firstDelta = overSlop
            change.consume()
        } ?: return@awaitEachGesture
        val started = SystemClock.uptimeMillis()
        var travel = 0f
        var total = 0f
        var activated = false
        var finished = false
        fun pull(delta: Float) {
            total = (total + delta).coerceAtLeast(0f)
            if (!activated) {
                travel = (travel + delta).coerceAtLeast(0f)
                val activation = expandedPullActivationPx(density)
                if (travel >= activation) {
                    activated = true
                    val overflow = travel - activation
                    if (overflow > 0f) onPull(expandedPullResistedDelta(overflow))
                }
            } else onPull(expandedPullResistedDelta(delta))
        }
        try {
            pull(firstDelta)
            val released = verticalDrag(slop.id) { change ->
                pull(change.positionChange().y)
                change.consume()
            }
            finished = true
            if (released && activated) {
                val elapsed = (SystemClock.uptimeMillis() - started).coerceAtLeast(1)
                onPullEnd(total, total * 1000f / elapsed)
            } else onPullCancel()
        } finally {
            if (!finished) onPullCancel()
        }
    }
}
