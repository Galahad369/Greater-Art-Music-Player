package com.local.listentomusic.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.local.listentomusic.ui.theme.GaControl

@Composable
internal fun PlaybackSpeedButton(
    onClick: () -> Unit,
    onBeginHold: (() -> Boolean)?,
    onEndHold: () -> Unit,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    var press by remember { mutableStateOf<PressInteraction.Press?>(null) }
    var ownsHold by remember { mutableStateOf(false) }
    val endHold by rememberUpdatedState(onEndHold)
    val pressedChange by rememberUpdatedState(onPressedChange)

    fun finishPress() {
        if (ownsHold) {
            ownsHold = false
            endHold()
        }
        if (press != null) {
            press = null
            pressedChange(false)
        }
    }

    LaunchedEffect(interactions) {
        interactions.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    finishPress()
                    press = interaction
                    pressedChange(true)
                }
                is PressInteraction.Release -> if (interaction.press == press) finishPress()
                is PressInteraction.Cancel -> if (interaction.press == press) finishPress()
            }
        }
    }
    DisposableEffect(interactions) { onDispose { finishPress() } }

    Box(
        modifier = modifier.size(GaControl.touchTarget).clip(CircleShape).combinedClickable(
            interactionSource = interactions,
            indication = ripple(bounded = false, radius = GaControl.touchTarget / 2),
            role = Role.Button,
            onClick = onClick,
            onLongClick = onBeginHold?.let { begin ->
                {
                    // A semantics action has no matching physical release.
                    if (press == null) onClick()
                    else if (!ownsHold) ownsHold = begin()
                }
            },
        ),
        contentAlignment = Alignment.Center,
    ) { content() }
}
