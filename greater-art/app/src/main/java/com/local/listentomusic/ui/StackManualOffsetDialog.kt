package com.local.listentomusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.playback.STACK_MAX_OFFSET_MS
import java.math.BigDecimal
import java.math.RoundingMode

internal fun parseStackOffset(text: String): Double? = text.trim().replace('−', '-').replace(',', '.')
    .toDoubleOrNull()?.takeIf { it in -STACK_MAX_OFFSET_MS.toDouble()..STACK_MAX_OFFSET_MS.toDouble() }

internal fun stackOffsetText(milliseconds: Double): String =
    BigDecimal.valueOf(milliseconds).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

@Composable
internal fun StackManualOffsetDialog(
    language: AppLanguage,
    trackName: String,
    originalMs: Double,
    onDismiss: () -> Unit,
    onApply: (milliseconds: Double, resetRate: Boolean) -> Unit,
) {
    var input by rememberSaveable(trackName, originalMs) { mutableStateOf(stackOffsetText(originalMs)) }
    var resetRate by rememberSaveable(trackName, originalMs) { mutableStateOf(false) }
    val value = parseStackOffset(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText(language, "Manual offset", "手動偏移")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(trackName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().testTag("STACK_OFFSET_INPUT"),
                    label = { Text(uiText(language, "Offset (ms)", "偏移（毫秒）")) },
                    singleLine = true,
                    isError = value == null,
                    supportingText = {
                        Text(if (value == null) uiText(language, "Enter −30000 to +30000 ms", "請輸入 −30000 至 +30000 毫秒")
                            else uiText(language, "Positive moves this track earlier; negative moves it later.", "正數會提早此歌曲；負數會延後。"))
                    },
                )
                for (step in listOf(1, 10, 100)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            enabled = value != null,
                            onClick = {
                                input = stackOffsetText(((value ?: originalMs) + step)
                                    .coerceAtMost(STACK_MAX_OFFSET_MS.toDouble()))
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("STACK_OFFSET_EARLIER_$step"),
                        ) { Text("${uiText(language, "Earlier", "提早")} $step ms") }
                        TextButton(
                            enabled = value != null,
                            onClick = {
                                input = stackOffsetText(((value ?: originalMs) - step)
                                    .coerceAtLeast(-STACK_MAX_OFFSET_MS.toDouble()))
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("STACK_OFFSET_LATER_$step"),
                        ) { Text("${uiText(language, "Later", "延後")} $step ms") }
                    }
                }
                TextButton(onClick = { input = "0"; resetRate = true },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("STACK_OFFSET_RESET")) {
                    Text(uiText(language, "Reset alignment", "重設對齊"))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value != null, onClick = { value?.let { onApply(it, resetRate) } },
                modifier = Modifier.testTag("STACK_OFFSET_APPLY")) {
                Text(uiText(language, "Apply", "套用"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } },
    )
}
