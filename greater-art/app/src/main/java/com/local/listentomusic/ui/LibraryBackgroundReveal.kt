package com.local.listentomusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.BackgroundScaleMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val LIBRARY_BACKGROUND_REVEAL_MIN_DP = 240f
internal const val LIBRARY_BACKGROUND_REVEAL_VIEWPORT_FRACTION = 1f
internal const val LIBRARY_BACKGROUND_REVEAL_VIEWPORT_CAP_FRACTION = 1f
internal const val LIBRARY_BACKGROUND_REVEAL_SNAP_THRESHOLD = 0.18f
internal const val LIBRARY_BACKGROUND_LIGHT_SCRIM_ALPHA = 0.94f

internal fun libraryBackgroundRevealTarget(fraction: Float): Float =
    if (fraction.coerceIn(0f, 1f) >= LIBRARY_BACKGROUND_REVEAL_SNAP_THRESHOLD) 1f else 0f

internal fun libraryBackgroundContentAlpha(lightPalette: Boolean): Float =
    if (lightPalette) LIBRARY_BACKGROUND_LIGHT_SCRIM_ALPHA else 0f

internal fun libraryBackgroundRevealOffsetPx(fraction: Float, maxPx: Float): Float =
    fraction.coerceIn(0f, 1f) * maxPx.coerceAtLeast(0f)

internal fun libraryBackgroundRecoveryTarget(totalDragPx: Float, fraction: Float): Float = when {
    totalDragPx < 0f -> 0f
    totalDragPx > 0f -> 1f
    else -> libraryBackgroundRevealTarget(fraction)
}

internal fun libraryBackgroundFlingTarget(velocityY: Float, fraction: Float): Float = when {
    velocityY < -1f -> 0f
    velocityY > 1f -> 1f
    else -> libraryBackgroundRevealTarget(fraction)
}

internal fun libraryBackgroundRevealMaxPx(viewportHeightPx: Float, _minRevealPx: Float): Float =
    viewportHeightPx.coerceAtLeast(0f)

internal enum class WallpaperGestureAxis { HORIZONTAL, VERTICAL }

internal fun wallpaperGestureAxis(totalX: Float, totalY: Float, touchSlop: Float): WallpaperGestureAxis? {
    if (maxOf(abs(totalX), abs(totalY)) < touchSlop.coerceAtLeast(0f)) return null
    return if (abs(totalX) > abs(totalY)) WallpaperGestureAxis.HORIZONTAL else WallpaperGestureAxis.VERTICAL
}

@Stable
internal class WallpaperPanState {
    var position by mutableFloatStateOf(0.5f)
        private set

    internal fun dragBy(deltaPx: Float, viewportWidthPx: Float, scaleMode: BackgroundScaleMode) {
        if (viewportWidthPx <= 0f || deltaPx == 0f) return
        val signedDelta = if (scaleMode == BackgroundScaleMode.CROP) -deltaPx else deltaPx
        position = (position + signedDelta / viewportWidthPx).coerceIn(0f, 1f)
    }

    internal fun center() { position = 0.5f }
}

@Composable
internal fun rememberWallpaperPanState(): WallpaperPanState = remember { WallpaperPanState() }

@Stable
internal class LibraryBackgroundRevealState {
    private val animation = Animatable(0f)

    var fraction by mutableFloatStateOf(0f)
        private set

    internal fun dragBy(deltaPx: Float, maxPx: Float): Float {
        if (maxPx <= 0f || deltaPx == 0f) return 0f
        val before = fraction
        fraction = (before + deltaPx / maxPx).coerceIn(0f, 1f)
        return (fraction - before) * maxPx
    }

    internal suspend fun settle(
        target: Float = libraryBackgroundRevealTarget(fraction),
        velocityPxPerSecond: Float = 0f,
        maxPx: Float = 1f,
    ) {
        animation.snapTo(fraction)
        val initialVelocity = if (maxPx > 0f) velocityPxPerSecond / maxPx else 0f
        animation.animateTo(
            targetValue = target.coerceIn(0f, 1f),
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy,
            ),
            initialVelocity = initialVelocity,
        ) {
            fraction = value.coerceIn(0f, 1f)
        }
    }

    internal suspend fun toggle() {
        animation.snapTo(fraction)
        animation.animateTo(
            targetValue = if (fraction >= 0.5f) 0f else 1f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy,
            ),
        ) {
            fraction = value.coerceIn(0f, 1f)
        }
    }
}

@Composable
internal fun rememberLibraryBackgroundRevealState(): LibraryBackgroundRevealState =
    remember { LibraryBackgroundRevealState() }

@Composable
internal fun LibraryFamilyWithBackgroundReveal(
    reveal: LibraryBackgroundRevealState,
    lightPalette: Boolean,
    wallpaperDimAlpha: Float,
    wallpaperDimColor: Color,
    wallpaperPan: WallpaperPanState,
    backgroundScaleMode: BackgroundScaleMode,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val minRevealPx = with(density) { LIBRARY_BACKGROUND_REVEAL_MIN_DP.dp.toPx() }
        val maxRevealPx = libraryBackgroundRevealMaxPx(viewportHeightPx, minRevealPx)
        val revealOffsetPx = libraryBackgroundRevealOffsetPx(reveal.fraction, maxRevealPx)
        val revealHeight = with(density) { revealOffsetPx.toDp() }
        val scope = rememberCoroutineScope()

        val nestedScrollConnection = remember(reveal, maxRevealPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y < 0f && reveal.fraction > 0f) {
                        return Offset(0f, reveal.dragBy(available.y, maxRevealPx))
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y > 0f) {
                        return Offset(0f, reveal.dragBy(available.y, maxRevealPx))
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (reveal.fraction <= 0f) return Velocity.Zero
                    val target = libraryBackgroundFlingTarget(available.y, reveal.fraction)
                    reveal.settle(target, available.y, maxRevealPx)
                    return if (available.y != 0f) Velocity(0f, available.y) else Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (reveal.fraction > 0f || available.y > 0f) {
                        val target = libraryBackgroundFlingTarget(available.y, reveal.fraction)
                        reveal.settle(target, available.y, maxRevealPx)
                        return if (available.y != 0f) Velocity(0f, available.y) else Velocity.Zero
                    }
                    return Velocity.Zero
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        ) {
            // This is deliberately empty. The Library wallpaper underneath is forced
            // to zero dim, so the exposed band is the raw AppBackground only.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(revealHeight)
                    .pointerInput(reveal, wallpaperPan, maxRevealPx, viewportWidthPx, backgroundScaleMode) {
                        var totalX = 0f
                        var totalY = 0f
                        var axis: WallpaperGestureAxis? = null
                        var horizontalAllowed = false
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f; totalY = 0f; axis = null
                                horizontalAllowed = reveal.fraction >= 0.995f
                            },
                            onDrag = { change, dragAmount ->
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                if (axis == null) {
                                    axis = if (horizontalAllowed) {
                                        wallpaperGestureAxis(totalX, totalY, viewConfiguration.touchSlop)
                                    } else if (abs(totalY) >= viewConfiguration.touchSlop) {
                                        WallpaperGestureAxis.VERTICAL
                                    } else null
                                }
                                when (axis) {
                                    WallpaperGestureAxis.HORIZONTAL -> {
                                        change.consume()
                                        wallpaperPan.dragBy(dragAmount.x, viewportWidthPx, backgroundScaleMode)
                                    }
                                    WallpaperGestureAxis.VERTICAL -> {
                                        change.consume()
                                        reveal.dragBy(dragAmount.y, maxRevealPx)
                                    }
                                    null -> Unit
                                }
                            },
                            onDragEnd = {
                                if (axis == WallpaperGestureAxis.VERTICAL) {
                                    val target = libraryBackgroundRecoveryTarget(totalY, reveal.fraction)
                                    scope.launch { reveal.settle(target) }
                                }
                            },
                            onDragCancel = {
                                if (axis == WallpaperGestureAxis.VERTICAL) scope.launch { reveal.settle() }
                            },
                        )
                    }
                    .inspectElement(
                        "LIBRARY_BACKGROUND_REVEAL",
                        "Undimmed app-background reveal; drag up here to recover the Library sheet",
                    ),
            )

            // All normal Library readability treatment moves with the content instead
            // of living on the wallpaper. Pulling down therefore exposes no dim/scrim,
            // title, settings affordance, navigation, dock chrome, or drag handle.
            Box(
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, revealOffsetPx.roundToInt()) },
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            wallpaperDimColor.copy(alpha = wallpaperDimAlpha.coerceIn(0f, 1f)),
                        ),
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(
                        alpha = libraryBackgroundContentAlpha(lightPalette),
                    ),
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    tonalElevation = 0.dp,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxWidth().weight(1f), content = content)
                    }
                }

                // This grab zone travels with the Library sheet. At full reveal it is
                // completely outside the clipped viewport, leaving only raw wallpaper.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.46f)
                        .height(38.dp)
                        .pointerInput(reveal, maxRevealPx) {
                            var totalDragPx = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalDragPx = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragPx += dragAmount
                                    reveal.dragBy(dragAmount, maxRevealPx)
                                },
                                onDragEnd = {
                                    val target = libraryBackgroundRecoveryTarget(totalDragPx, reveal.fraction)
                                    scope.launch { reveal.settle(target) }
                                },
                                onDragCancel = { scope.launch { reveal.settle() } },
                            )
                        }
                        .inspectElement(
                            "LIBRARY_BACKGROUND_REVEAL_HANDLE",
                            "Pull down for full pure wallpaper; handle leaves the viewport",
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.24f)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                                RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}
