package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.playback.StackSlot
import com.local.listentomusic.ui.components.LiquidMetalSurface

/** Temporary multi-track listening, sharing Library's wallpaper and visual language. */
@Composable
fun StackScreen(
    files: List<MediaFile>, language: AppLanguage, contentPadding: PaddingValues,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
) {
    val session by StackPlayback.state.collectAsState()
    var stagedPaths by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val staged = remember(files, stagedPaths) { stagedPaths.mapNotNull { path -> files.firstOrNull { it.path == path } } }
    var pickerOpen by remember { mutableStateOf(false) }
    var expandedPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scrubPosition by remember { mutableStateOf<Long?>(null) }
    val displayed = if (session.active) session.slots else staged.map(::StackSlot)
    val canAdd = displayed.size < StackPlayback.MAX_TRACKS

    Column(Modifier.fillMaxSize().inspectElement("STACK_SCREEN", "Simultaneous local playback")
        .padding(contentPadding).consumeWindowInsets(contentPadding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .62f)) {
                Text("${displayed.size} / ${StackPlayback.MAX_TRACKS}", Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canAdd) FilledTonalButton(onClick = { pickerOpen = true },
                modifier = Modifier.inspectElement("STACK_ADD_BUTTON", "Choose a local media file to add")) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(uiText(language, "Add track", "加入歌曲"))
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).inspectElement("STACK_TRACK_LIST", "Up to eight simultaneous tracks"),
            contentPadding = PaddingValues(bottom = 12.dp)) {
            items(displayed, key = { it.file.path }) { slot ->
                val isPrimary = session.active && slot.file.path == session.primaryPath || !session.active && slot.file.path == staged.firstOrNull()?.path
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = .48f))) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { expandedPath = if (expandedPath == slot.file.path) null else slot.file.path }
                        .inspectElement("STACK_TRACK_ROW", slot.file.name).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        QueueThumbnail(slot.file, onLoadThumbnail)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(slot.file.name.substringBeforeLast('.'), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge, fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal)
                            Text(if (slot.error != null) uiText(language, "Playback unavailable", "無法播放") else if (isPrimary) uiText(language, "Primary visual", "主要畫面")
                                else if (session.active && slot.resolvedDurationMs > 0 && session.positionMs >= slot.resolvedDurationMs)
                                    uiText(language, "Ended", "已播完") else slot.file.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (slot.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isPrimary) Icon(Icons.Rounded.Star, uiText(language, "Primary visual", "主要畫面"), tint = MaterialTheme.colorScheme.secondary)
                        IconButton(onClick = {
                            if (session.active) StackPlayback.remove(slot.file.path)
                            else stagedPaths = stagedPaths - slot.file.path
                        }, modifier = Modifier.inspectElement("STACK_REMOVE_BUTTON", slot.file.name)) {
                            Icon(Icons.Rounded.Close, uiText(language, "Remove track", "移除歌曲"))
                        }
                    }
                    if (expandedPath == slot.file.path) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (session.active) {
                                TextButton(onClick = { StackPlayback.setPrimary(slot.file.path) },
                                    enabled = !isPrimary && (slot.resolvedDurationMs <= 0L || session.positionMs < slot.resolvedDurationMs)) {
                                    Text(uiText(language, "Make primary", "設為主要"))
                                }
                                TextButton(onClick = { StackPlayback.toggleMute(slot.file.path) }) {
                                    Text(uiText(language, if (slot.muted) "Unmute" else "Mute", if (slot.muted) "取消靜音" else "靜音"))
                                }
                                TextButton(onClick = { StackPlayback.toggleSolo(slot.file.path) }) {
                                    Text(uiText(language, if (slot.solo) "Unsolo" else "Solo", if (slot.solo) "取消獨奏" else "獨奏"))
                                }
                            }
                        }
                        if (session.active) Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${(slot.volume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                            Slider(slot.volume, { StackPlayback.setVolume(slot.file.path, it) },
                                modifier = Modifier.weight(1f).padding(start = 12.dp).inspectElement("STACK_TRACK_VOLUME", slot.file.name))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
                }
            }
            if (displayed.size < 2) item {
                repeat(2 - displayed.size) { index ->
                    Surface(
                        onClick = { pickerOpen = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                            .inspectElement("STACK_EMPTY_SLOT", "Tap to choose a local track"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)),
                    ) {
                        Row(Modifier.height(62.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(40.dp), RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Rounded.Add, null, Modifier.padding(9.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(uiText(language, "Add a track", "加入歌曲"), fontWeight = FontWeight.SemiBold)
                                Text(uiText(language, "Choose from your Library", "從音樂庫選擇"),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        error?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = .82f))
            .padding(horizontal = 16.dp, vertical = 8.dp).inspectElement("STACK_MASTER_CONTROLS", "Shared seek and play/pause")) {
            val duration = if (session.active) session.durationMs else staged.maxOfOrNull { it.durationMs } ?: 0L
            val position = scrubPosition ?: session.positionMs
            if (duration > 0L) {
                Slider(position.coerceIn(0L, duration).toFloat(), { scrubPosition = it.toLong() },
                    onValueChangeFinished = { scrubPosition?.let(StackPlayback::seek); scrubPosition = null },
                    valueRange = 0f..duration.toFloat(), enabled = session.active,
                    modifier = Modifier.fillMaxWidth().inspectElement("STACK_MASTER_TIMELINE", "Seeks all active tracks"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stackTime(position), style = MaterialTheme.typography.labelMedium)
                if (session.active) {
                    IconButton(onClick = { if (session.playing) StackPlayback.pause() else StackPlayback.play() },
                        modifier = Modifier.size(56.dp).inspectElement("STACK_MASTER_PLAY", "Play or pause every track")) {
                        Icon(if (session.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            uiText(language, if (session.playing) "Pause Stack" else "Play Stack", if (session.playing) "暫停疊播" else "播放疊播"), Modifier.size(32.dp))
                    }
                    TextButton(onClick = { StackPlayback.stop() }) { Text(uiText(language, "Stop", "停止")) }
                } else {
                    Button(enabled = staged.size >= 2, onClick = {
                        if (StackPlayback.start(staged)) {
                            stagedPaths = emptyList()
                            error = null
                        } else error = uiText(language, "Could not start these files on this device", "這部裝置無法播放這些檔案")
                    }, modifier = Modifier.inspectElement("STACK_START_BUTTON", "Starts selected tracks together")) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Text(uiText(language, "Play together", "一齊播放"))
                    }
                }
                Text(stackTime(duration), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    if (pickerOpen) StackLibraryPicker(
        files = files,
        excludedPaths = displayed.map { it.file.path }.toSet(),
        language = language,
        onLoadThumbnail = onLoadThumbnail,
        onDismiss = { pickerOpen = false },
        onPick = { file ->
            val added = if (session.active) StackPlayback.add(file) else { stagedPaths = stagedPaths + file.path; true }
            error = if (added) null else uiText(language, "Could not add this track", "無法加入這首歌曲")
            if (added) pickerOpen = false
        },
    )
}

@Composable
private fun StackLibraryPicker(
    files: List<MediaFile>, excludedPaths: Set<String>, language: AppLanguage,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?, onDismiss: () -> Unit, onPick: (MediaFile) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val choices = remember(files, excludedPaths, query) {
        files.filter { it.path !in excludedPaths && (query.isBlank() || it.name.contains(query, true) || it.artist.contains(query, true)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(.96f),
        title = {
            Column {
                Text(uiText(language, "Choose from Library", "從音樂庫選擇"), fontWeight = FontWeight.Bold)
                Text(uiText(language, "${choices.size} available", "${choices.size} 首可用"),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column {
                LiquidMetalSurface(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    OutlinedTextField(
                        query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = if (query.isNotEmpty()) {{ IconButton({ query = "" }) { Icon(Icons.Rounded.Clear, null) } }} else null,
                        placeholder = { Text(uiText(language, "Filter library", "篩選音樂庫")) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(min = 260.dp, max = 470.dp)) {
                    items(choices, key = { it.path }) { file ->
                        ListItem(
                            headlineContent = { Text(file.name.substringBeforeLast('.'), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(listOf(file.artist.takeIf(String::isNotBlank), stackTime(file.durationMs)).filterNotNull().joinToString(" • "),
                                    maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            leadingContent = { QueueThumbnail(file, onLoadThumbnail) },
                            modifier = Modifier.fillMaxWidth().clickable { onPick(file) }
                                .inspectElement("STACK_PICKER_ROW", file.name),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Close", "關閉")) } },
    )
}

private fun stackTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
