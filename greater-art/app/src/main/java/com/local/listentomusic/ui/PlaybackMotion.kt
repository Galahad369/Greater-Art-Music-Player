package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.local.listentomusic.ui.theme.GaMotion
import kotlin.math.*

@Composable
internal fun motionActive(playing: Boolean): Boolean {
    val owner = LocalLifecycleOwner.current
    var visible by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> visible = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return playing && visible && android.animation.ValueAnimator.areAnimatorsEnabled()
}

internal fun waveformEnvelope(peaks: FloatArray?, position: Long, duration: Long): Float {
    if (peaks == null || peaks.isEmpty() || duration <= 0) return 0f
    return peaks[((position.coerceAtLeast(0).toDouble() / duration) * peaks.size).toInt().coerceIn(peaks.indices)]
        .takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
}

internal fun snapPracticePosition(position: Long, duration: Long, markers: List<Long>): Long {
    val value = position.coerceIn(0, duration.coerceAtLeast(0))
    return markers.filter { it in 0..duration }.minByOrNull { abs(it - value) }
        ?.takeIf { abs(it - value) <= 120L } ?: value
}

/** One reveal and one optional decorative phase, not sixty composables or fake BPM data. */
@Composable
internal fun AnimatedWaveformBars(peaks: FloatArray?, fraction: Float, active: Color, inactive: Color,
    playing: Boolean, modifier: Modifier) {
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(peaks) {
        if (peaks != null && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            reveal.snapTo(0f); reveal.animateTo(1f, tween(GaMotion.emphasizedMs))
        } else reveal.snapTo(1f)
    }
    Canvas(modifier.graphicsLayer()) {
        val count = peaks?.size ?: 60
        val spacing = size.width / count
        repeat(count) { i ->
            val peak = peaks?.get(i) ?: .025f
            val entrance = (reveal.value * 1.3f - i.toFloat() / count * .3f).coerceIn(0f, 1f)
            val height = size.height * (.06f + peak * .9f * entrance)
            val x = spacing * (i + .5f)
            drawLine(if ((i + 1f) / count <= fraction) active else inactive, Offset(x, (size.height-height)/2),
                Offset(x, (size.height+height)/2), min(2.25.dp.toPx(), spacing * .55f), StrokeCap.Round)
        }
        if (peaks != null) {
            val playheadX = size.width * fraction.coerceIn(0f, 1f)
            drawLine(active.copy(alpha = if (playing) .95f else .78f), Offset(playheadX, 0f), Offset(playheadX, size.height), 1.25.dp.toPx())
            drawCircle(active, 2.5.dp.toPx(), Offset(playheadX, size.height / 2f))
        }
    }
}

@Composable
internal fun SpeedDialIcon(speed: Float, tint: Color) {
    val angle = animateFloatAsState(150f + ((speed - .25f) / 2.75f).coerceIn(0f,1f) * 240f, tween(GaMotion.standardMs), label = "speed-needle")
    Canvas(Modifier.size(27.dp).semantics { contentDescription = "Playback speed" }) {
        val c = Offset(size.width/2, size.height*.55f)
        val r = size.width*.39f
        drawArc(tint.copy(alpha=.5f), 150f, 240f, false, Offset(c.x-r,c.y-r), androidx.compose.ui.geometry.Size(r*2,r*2), style=Stroke(1.6.dp.toPx(), cap=StrokeCap.Round))
        val radians = angle.value * PI.toFloat()/180
        drawLine(tint, c, c + Offset(cos(radians),sin(radians))*r*.78f, 1.7.dp.toPx(), StrokeCap.Round)
        drawCircle(tint, 1.8.dp.toPx(), c)
    }
}

/** Cached cover, not a fabricated timestamp-specific video frame. Never decodes while scrubbing. */
@Composable
internal fun ScrubReadout(artwork: Bitmap?, time: String, seeking: Boolean) {
    val lift = animateFloatAsState(if (seeking) 1f else 0f, tween(GaMotion.quickMs), label="scrub-peel")
    Row(Modifier.height(32.dp).graphicsLayer { rotationX = (1f-lift.value)*8f; translationY=(1f-lift.value)*2f }
        .clip(RoundedCornerShape(8.dp)).background(if (seeking) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
        .padding(horizontal=6.dp), verticalAlignment=Alignment.CenterVertically) {
        if (seeking && artwork != null) {
            Image(artwork.asImageBitmap(), null, Modifier.size(26.dp).clip(RoundedCornerShape(5.dp)), contentScale=ContentScale.Crop)
            Spacer(Modifier.width(6.dp))
        }
        Text(time, color=MaterialTheme.colorScheme.secondary, style=MaterialTheme.typography.labelLarge)
    }
}
