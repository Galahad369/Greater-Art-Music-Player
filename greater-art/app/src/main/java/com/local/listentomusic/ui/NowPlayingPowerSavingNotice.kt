package com.local.listentomusic.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.ui.theme.GaControl

internal const val POWER_SAVING_AMBIENT_MESSAGE =
    "Power saving makes Ambient less vivid. Video quality is unchanged."

/** Inform once per Power saving interval in this player visit, not on every song. */
@Composable
internal fun NowPlayingPowerSavingNotice(
    powerSaving: Boolean,
    visible: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val host = remember { SnackbarHostState() }
    var announced by rememberSaveable(powerSaving) { mutableStateOf(false) }
    val message = uiText(language, POWER_SAVING_AMBIENT_MESSAGE, "省電模式會減弱氛圍燈光效果，但影片畫質不變。")
    LaunchedEffect(powerSaving, visible) {
        if (!powerSaving || !visible) {
            host.currentSnackbarData?.dismiss()
        } else if (!announced) {
            announced = true
            host.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Long)
        }
    }
    SnackbarHost(hostState = host, modifier = modifier) { data ->
        ThemedNotice(
            modifier = Modifier.testTag("NOW_PLAYING_POWER_SAVING_NOTICE"),
            dismissAction = {
                IconButton(
                    onClick = data::dismiss,
                    modifier = Modifier.size(GaControl.touchTarget).testTag("DISMISS_POWER_SAVING_NOTICE"),
                ) {
                    Icon(Icons.Rounded.Close,
                        contentDescription = uiText(language, "Dismiss power saving notice", "關閉省電提示"),
                        modifier = Modifier.size(GaControl.icon))
                }
            },
        ) {
            Text(data.visuals.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
