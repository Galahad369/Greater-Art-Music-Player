package com.local.listentomusic.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Reflected light from the media palette, not a second rendering of the video. */
internal fun DrawScope.drawAmbientReflections(
    primary: Color,
    secondary: Color,
    accent: Color,
    phase: Double,
    motion: Float,
    strength: Float,
    light: Boolean,
) {
    val amount = if (strength.isFinite()) strength.coerceIn(0f, 1f) else 0f
    val width = size.width
    val height = size.height
    if (amount == 0f || !width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return

    val movement = if (motion.isFinite()) motion.coerceIn(0f, 1.25f) else 0f
    // Integer harmonics keep the wrapped phase continuous. With motion Off,
    // every position remains fixed even if the caller supplies a changing phase.
    val p = if (phase.isFinite()) phase % (2.0 * PI) else 0.0
    val sweep = sin(p).toFloat() * movement
    val drift = cos(p).toFloat() * movement
    val ripple = sin(p * 2.0 + .8).toFloat() * movement
    val span = maxOf(width, height)
    val gleam = lerp(accent, if (light) primary else Color.White, if (light) .25f else .64f)

    // Wide off-screen origins produce luminous edges without a visible glow disc.
    drawRect(Brush.radialGradient(
        listOf(primary.copy(alpha = amount * if (light) .18f else .43f), Color.Transparent),
        center = Offset(width * (-.08f + .07f * sweep), height * (.27f + .10f * drift)),
        radius = span * .67f,
    ))
    drawRect(Brush.radialGradient(
        listOf(secondary.copy(alpha = amount * if (light) .16f else .38f), Color.Transparent),
        center = Offset(width * (1.06f + .05f * drift), height * (.72f + .09f * sweep)),
        radius = span * .60f,
    ))

    val upper = height * (.19f + .06f * sweep)
    val lower = height * (.74f + .05f * ripple)
    val broad = Path().apply {
        moveTo(-width * .20f, upper - height * .14f)
        cubicTo(width * .22f, upper - height * .20f,
            width * .54f, lower - height * .10f, width * 1.20f, lower - height * .25f)
        lineTo(width * 1.20f, lower + height * .06f)
        cubicTo(width * .52f, lower + height * .19f,
            width * .19f, upper + height * .12f, -width * .20f, upper + height * .12f)
        close()
    }
    drawPath(broad, Brush.linearGradient(
        0f to Color.Transparent,
        .27f to secondary.copy(alpha = amount * if (light) .08f else .17f),
        .49f to primary.copy(alpha = amount * if (light) .18f else .36f),
        .58f to gleam.copy(alpha = amount * if (light) .10f else .23f),
        .78f to accent.copy(alpha = amount * if (light) .06f else .14f),
        1f to Color.Transparent,
        start = Offset(width * .12f, upper - height * .12f),
        end = Offset(width * .84f, lower + height * .16f),
    ))

    val ribbonY = height * (.54f + .055f * drift)
    val ribbon = Path().apply {
        moveTo(-width * .12f, ribbonY + height * .19f)
        cubicTo(width * .26f, ribbonY - height * .08f,
            width * .67f, ribbonY + height * .20f, width * 1.12f, ribbonY - height * .17f)
        lineTo(width * 1.12f, ribbonY - height * .09f)
        cubicTo(width * .65f, ribbonY + height * .28f,
            width * .27f, ribbonY - height * .015f, -width * .12f, ribbonY + height * .26f)
        close()
    }
    drawPath(ribbon, Brush.linearGradient(
        0f to Color.Transparent,
        .35f to accent.copy(alpha = amount * if (light) .10f else .24f),
        .53f to gleam.copy(alpha = amount * if (light) .13f else .29f),
        .72f to secondary.copy(alpha = amount * if (light) .07f else .16f),
        1f to Color.Transparent,
        start = Offset(0f, ribbonY + height * .24f),
        end = Offset(width, ribbonY - height * .12f),
    ))
    // Reuse the ribbon contour for a fine specular edge, not another large layer.
    drawPath(ribbon, Brush.horizontalGradient(listOf(
        Color.Transparent,
        gleam.copy(alpha = amount * if (light) .15f else .31f),
        Color.Transparent,
    )), style = Stroke(width = (minOf(width, height) * .0016f).coerceIn(.7f, 2f), cap = StrokeCap.Round))
}
