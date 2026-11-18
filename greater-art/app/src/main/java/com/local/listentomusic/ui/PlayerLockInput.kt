package com.local.listentomusic.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Block controls in the parent's initial pass, without covering the queue's scroll target. */
@Composable
internal fun Modifier.playerLockInput(
    locked: Boolean,
    queueBounds: Rect?,
    onLockedTap: (Offset) -> Unit,
): Modifier {
    val onTap by rememberUpdatedState(onLockedTap)
    return if (!locked) this else pointerInput(queueBounds) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val queueGesture = queueBounds?.contains(down.position) == true
            if (!queueGesture) down.consume()
            var moved = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }) moved = true
                if (!queueGesture) event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
            if (!moved) onTap(down.position)
        }
    }
}
