package com.local.listentomusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal const val LIBRARY_BACKGROUND_REVEAL_MAX_DP = 240f
internal const val LIBRARY_BACKGROUND_REVEAL_SNAP_THRESHOLD = 0.35f
internal const val LIBRARY_BACKGROUND_LIGHT_SCRIM_ALPHA = 0.94f

internal fun libraryBackgroundRevealTarget(fraction: Float): Float =
    if (fraction.coerceIn(0f, 1f) >= LIBRARY_BACKGROUND_REVEAL_SNAP_THRESHOLD) 1f else 0f

internal fun libraryBackgroundContentAlpha(lightPalette: Boolean): Float =
    if (lightPalette) LIBRARY_BACKGROUND_LIGHT_SCRIM_ALPHA else 0f

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

    internal suspend fun settle() {
        animation.snapTo(fraction)
        animation.animateTo(
            targetValue = libraryBackgroundRevealTarget(fraction),
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy,
            ),
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
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val maxRevealPx = with(density) { LIBRARY_BACKGROUND_REVEAL_MAX_DP.dp.toPx() }
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
                reveal.settle()
                return if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (reveal.fraction > 0f || available.y > 0f) {
                    reveal.settle()
                    return if (available.y > 0f) Velocity(0f, available.y) else Velocity.Zero
                }
                return Velocity.Zero
            }
        }
    }

    Column(modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        // Intentionally empty: no Surface, scrim, color, or placeholder may cover AppBackground.
        Box(
            Modifier
                .fillMaxWidth()
                .height((LIBRARY_BACKGROUND_REVEAL_MAX_DP * reveal.fraction).dp)
                .inspectElement(
                    "LIBRARY_BACKGROUND_REVEAL",
                    "Pure app-background reveal band; pull down to expand, pull up to collapse",
                ),
        )

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.background.copy(
                alpha = libraryBackgroundContentAlpha(lightPalette),
            ),
            contentColor = MaterialTheme.colorScheme.onBackground,
            tonalElevation = 0.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().weight(1f), content = content)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .pointerInput(reveal, maxRevealPx) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    reveal.dragBy(dragAmount, maxRevealPx)
                                },
                                onDragEnd = { scope.launch { reveal.settle() } },
                                onDragCancel = { scope.launch { reveal.settle() } },
                            )
                        }
                        .inspectElement(
                            "LIBRARY_BACKGROUND_REVEAL_HANDLE",
                            "Backup drag handle for pure app-background reveal",
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.11f)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}
