package com.local.listentomusic.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.local.listentomusic.ui.theme.GaAlpha
import com.local.listentomusic.ui.theme.GaControl
import com.local.listentomusic.ui.theme.GaRadius
import com.local.listentomusic.ui.theme.GaSpacing

/** Only the plate is translucent. Foreground and action colors stay fully opaque. */
@Composable
internal fun ThemedNotice(
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Snackbar's fixed elevation shadows the image underneath its translucent
    // plate. Keep the existing SnackbarHost timing, but render an unshadowed plate.
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(GaRadius.control),
        color = colors.surfaceContainerHigh.copy(alpha = GaAlpha.notice),
        contentColor = colors.onSurface,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = GaSpacing.lg, end = GaSpacing.sm,
            top = GaSpacing.xs, bottom = GaSpacing.xs).heightIn(min = GaControl.touchTarget),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(end = GaSpacing.sm)) {
                ProvideTextStyle(MaterialTheme.typography.bodySmall, content)
            }
            CompositionLocalProvider(LocalContentColor provides colors.secondary) {
                action?.invoke()
                dismissAction?.invoke()
            }
        }
    }
}
