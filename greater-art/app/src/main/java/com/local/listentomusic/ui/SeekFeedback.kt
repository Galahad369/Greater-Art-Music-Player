package com.local.listentomusic.ui

/** Left / right third of the stage; middle is inert (YouTube-style). */
internal enum class SeekSide { LEFT, RIGHT }

internal fun seekSideForX(x: Float, width: Float): SeekSide? = when {
    width <= 0f -> null
    x < width * .35f -> SeekSide.LEFT
    x > width * .65f -> SeekSide.RIGHT
    else -> null
}

/** Side seek zones only. The middle 30% is deliberately inert on double tap. */
internal fun doubleTapSeekDelta(x: Float, width: Float, seekOffsetMs: Long): Long? =
    when (seekSideForX(x, width)) {
        SeekSide.LEFT -> -seekOffsetMs
        SeekSide.RIGHT -> seekOffsetMs
        null -> null
    }

/** Same-side double-tap only — left then right must not seek. */
internal const val SIDE_DOUBLE_TAP_MS = 400L

internal fun sideDoubleTapSeeks(
    side: SeekSide?,
    nowMs: Long,
    lastSide: SeekSide?,
    lastTapMs: Long,
): Boolean = side != null && side == lastSide && (nowMs - lastTapMs) in 1 until SIDE_DOUBLE_TAP_MS

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

/**
 * YouTube-mobile-style side seek flash: dim circle, pulsing chevrons, seconds label.
 * Accumulates rapid same-side seeks (e.g. 10s → 20s).
 */
@Composable
internal fun SeekFeedback(deltaMs: Long, event: Long, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var accumulated by remember { mutableLongStateOf(0L) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(event) {
        if (event == 0L) return@LaunchedEffect
        accumulated = if (visible && (accumulated < 0) == (deltaMs < 0)) accumulated + deltaMs else deltaMs
        visible = true
        appear.snapTo(0f)
        appear.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        delay(650)
        appear.animateTo(0f, tween(220))
        visible = false
    }
    if (!visible && appear.value <= 0.01f) return

    val backwards = accumulated < 0
    val seconds = abs(accumulated) / 1000
    val pulse = rememberInfiniteTransition(label = "seekChevrons")
    val chevronPhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Restart),
        label = "chevronPhase",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(140.dp)
            .graphicsLayer { alpha = appear.value }
            .semantics {
                contentDescription = if (backwards) {
                    "Rewound $seconds seconds"
                } else {
                    "Skipped forward $seconds seconds"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = min(size.width, size.height) * 0.55f
            val cx = if (backwards) size.width * 0.15f else size.width * 0.85f
            val cy = size.height * 0.5f
            drawCircle(
                color = Color.Black.copy(alpha = 0.42f),
                radius = r,
                center = Offset(cx, cy),
            )
            val chevronColor = Color.White
            val step = 18.dp.toPx()
            val baseX = cx + if (backwards) step * 0.6f else -step * 0.6f
            val dir = if (backwards) -1f else 1f
            for (i in 0..2) {
                val phase = ((chevronPhase + i / 3f) % 1f)
                val a = 0.25f + 0.75f * (1f - abs(phase - 0.5f) * 2f)
                val x = baseX + dir * i * step * 0.55f
                val path = Path().apply {
                    val h = 10.dp.toPx()
                    val w = 8.dp.toPx()
                    moveTo(x - dir * w, cy - h)
                    lineTo(x, cy)
                    lineTo(x - dir * w, cy + h)
                }
                drawPath(path, chevronColor.copy(alpha = a), style = Stroke(width = 2.5.dp.toPx()))
            }
        }
        Text(
            text = "${seconds}s",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
