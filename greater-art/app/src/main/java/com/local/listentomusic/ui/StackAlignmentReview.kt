package com.local.listentomusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.playback.StackAlignmentMethod
import com.local.listentomusic.playback.StackAlignmentProgress
import com.local.listentomusic.playback.StackSlot
import java.math.BigDecimal
import java.math.RoundingMode

private fun signedStackOffsetText(milliseconds: Double): String =
    "${if (milliseconds >= 0.0) "+" else ""}${stackOffsetText(milliseconds)}"

private fun stackReviewRate(rate: Double): String {
    val exact = BigDecimal.valueOf(rate)
    val shown = exact.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
    return "${if (shown.compareTo(exact) == 0) "" else "≈"}${shown.toPlainString()}×"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StackAlignmentReview(
    language: AppLanguage,
    slots: List<StackSlot>,
    alignment: StackAlignmentProgress,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onManualOffset: (String) -> Unit,
    onWiderPictureSearch: () -> Unit,
) {
    val picture = alignment.method == StackAlignmentMethod.PICTURE
    val eligible = alignment.eligiblePaths.toSet()
    val reviewed = if (alignment.applied) alignment.results.filterValues { it.confident } else alignment.proposal
    val reference = slots.firstOrNull { it.file.path == alignment.primaryPath }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(uiText(language, "Review offsets", "檢視偏移"), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("STACK_ALIGNMENT_REVIEW_LIST"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (picture) uiText(language, "Picture matching aligns images. It does not guarantee matching sound.", "畫面匹配只對齊影像，不保證聲音同步。")
                            else uiText(language, "Sound matching follows the shared backing track, not the singers' phrasing.", "聲音匹配以共同伴奏為準，而非歌手的唱法。"),
                            Modifier.testTag("STACK_ALIGNMENT_METHOD"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        reference?.let {
                            Text(
                                "${uiText(language, "Reference track", "參考歌曲")}: ${it.file.name}",
                                Modifier.testTag("STACK_ALIGNMENT_REFERENCE"),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        val status = if (alignment.applied) uiText(language, "Timing applied", "已套用時間調整")
                            else uiText(language, "Suggested offsets", "建議偏移")
                        Text(
                            "$status: ${alignment.matched ?: 0}/${alignment.total}",
                            Modifier.testTag("STACK_ALIGNMENT_STATUS"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (alignment.canRetryPictureWider) {
                            TextButton(
                                onClick = onWiderPictureSearch,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("STACK_ALIGNMENT_WIDER"),
                            ) { Text(uiText(language, "Wider search (±30 s)", "擴大搜尋（±30 秒）")) }
                        }
                    }
                }
                items(slots.filter { it.file.path in eligible }, key = { it.file.path }) { slot ->
                    val result = reviewed[slot.file.path]
                    val currentMs = (slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0) / 1000.0
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(slot.file.name, style = MaterialTheme.typography.titleSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (result == null) {
                            val unfinished = alignment.quickPictureTimedOut &&
                                alignment.eligiblePaths.indexOf(slot.file.path) >= alignment.completed
                            Text(
                                "${uiText(language, "Kept unchanged", "保持不變")} · " +
                                    when {
                                        unfinished -> uiText(language, "Not completed before the quick scan time limit.", "未能在快速搜尋時限內完成。")
                                        picture -> uiText(language, "Could not establish one consistent picture offset.", "未能確立一致的畫面偏移。")
                                        else -> uiText(language, "Could not establish one consistent sound offset.", "未能確立一致的聲音偏移。")
                                    },
                                Modifier.testTag("STACK_ALIGNMENT_REASON_${slot.file.path}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val offsetLabel = if (result != null && !alignment.applied)
                            uiText(language, "Current → proposed", "目前 → 建議")
                        else uiText(language, "Offset (ms)", "偏移（毫秒）")
                        val timing = if (result == null || alignment.applied) signedStackOffsetText(currentMs)
                            else "${signedStackOffsetText(currentMs)} → ${signedStackOffsetText(result.offsetUs / 1000.0)}"
                        Text("$offsetLabel: $timing${if (result != null && !alignment.applied) " ms" else ""}", Modifier.testTag("STACK_ALIGNMENT_TIMING_${slot.file.path}"),
                            style = MaterialTheme.typography.bodyMedium)
                        if (result != null && (slot.alignmentScale != result.timeScale || alignment.applied && result.timeScale != 1.0)) {
                            val proposedRate = stackReviewRate(result.timeScale)
                            val speed = if (alignment.applied) proposedRate
                                else "${stackReviewRate(slot.alignmentScale)} → $proposedRate"
                            Text("${uiText(language, "Speed fit", "速度配合")}: $speed",
                                Modifier.testTag("STACK_ALIGNMENT_SPEED_${slot.file.path}"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(
                            onClick = { onManualOffset(slot.file.path) },
                            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                                .testTag("STACK_ALIGNMENT_MANUAL_${slot.file.path}"),
                        ) { Text(uiText(language, "Manual offset", "手動偏移")) }
                    }
                }
            }
            Text(
                when {
                    alignment.applied -> uiText(language, "Timing was applied, not verified. Listen and adjust if needed.", "已套用時間調整，但尚未驗證。請聆聽並按需要調整。")
                    alignment.proposal.isNotEmpty() -> uiText(language, "Applying timing briefly pauses the Stack. Listen to check the result.", "套用時間調整時，疊播會短暫暫停。請聆聽並檢查結果。")
                    else -> uiText(language, "Uncertain matches kept unchanged", "未能確認的匹配保持不變")
                },
                Modifier.testTag("STACK_ALIGNMENT_CAUTION"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(uiText(language, "Close", "關閉"))
                }
                if (alignment.proposal.isNotEmpty()) {
                    Button(onClick = onApply, modifier = Modifier.heightIn(min = 48.dp).testTag("STACK_ALIGNMENT_APPLY")) {
                        Text(uiText(language, "Apply timing ({count})", "套用時間調整（{count}）")
                            .replace("{count}", alignment.proposal.size.toString()))
                    }
                }
            }
        }
    }
}
