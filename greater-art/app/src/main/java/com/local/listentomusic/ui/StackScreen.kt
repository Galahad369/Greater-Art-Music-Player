package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
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
import com.local.listentomusic.data.PlayHistoryEntry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.sourceMediaPath
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.playback.StackRecommendation
import com.local.listentomusic.playback.StackRecommendationReason
import com.local.listentomusic.playback.StackSlot
import com.local.listentomusic.playback.recommendStackTracks
import com.local.listentomusic.ui.components.LiquidMetalSurface

/** Temporary multi-track listening, sharing Library's wallpaper and visual language. */
@Composable
fun StackScreen(
    files: List<MediaFile>,
    language: AppLanguage,
    contentPadding: PaddingValues,
    nowPlayingPath: String?,
    playHistory: List<PlayHistoryEntry>,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onSaveList: (String, String, List<String>) -> Unit,
) {
    val session by StackPlayback.state.collectAsState()
    var stagedPaths by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val staged = remember(files, stagedPaths) {
        stagedPaths.mapNotNull { path -> files.firstOrNull { it.path == path } }
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var recommendationsOpen by remember { mutableStateOf(false) }
    var saveOpen by remember { mutableStateOf(false) }
    var expandedPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scrubPosition by remember { mutableStateOf<Long?>(null) }

    val displayed = if (session.active) session.slots else staged.map(::StackSlot)
    val displayedPaths = remember(displayed) { displayed.map { it.file.path }.toSet() }
    val nowPlayingSource = nowPlayingPath?.let(::sourceMediaPath)
    val nowPlayingFile = remember(files, nowPlayingPath, nowPlayingSource) {
        files.firstOrNull { it.path == nowPlayingPath }
            ?: files.firstOrNull { nowPlayingSource != null && it.sourcePath == nowPlayingSource }
    }
    val nowPlayingRepresented = displayed.any { it.file.path == nowPlayingPath }
    val canAdd = displayed.size < StackPlayback.MAX_TRACKS
    val remaining = (StackPlayback.MAX_TRACKS - displayed.size).coerceAtLeast(0)

    fun addFiles(candidates: List<MediaFile>) {
        val additions = candidates.filter { it.path !in displayedPaths }
            .distinctBy { it.path }
            .take(remaining)
        if (additions.isEmpty()) return
        if (session.active) {
            var failed = false
            additions.forEach { if (!StackPlayback.add(it)) failed = true }
            error = if (failed) uiText(language, "Some tracks could not be added", "部分歌曲無法加入") else null
        } else {
            stagedPaths = (stagedPaths + additions.map { it.path }).distinct().take(StackPlayback.MAX_TRACKS)
            error = null
        }
    }

    Column(
        Modifier.fillMaxSize().inspectElement("STACK_SCREEN", "Simultaneous local playback")
            .padding(contentPadding).consumeWindowInsets(contentPadding),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .62f)) {
                Text(
                    "${displayed.size} / ${StackPlayback.MAX_TRACKS}",
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { recommendationsOpen = true },
                enabled = canAdd && (displayed.isNotEmpty() || nowPlayingFile != null),
                modifier = Modifier.inspectElement("STACK_RECOMMEND_BUTTON", "Offline local recommendations"),
            ) { Icon(Icons.Rounded.Star, uiText(language, "Recommend tracks", "推薦歌曲")) }
            IconButton(
                onClick = { saveOpen = true },
                enabled = displayed.isNotEmpty(),
                modifier = Modifier.inspectElement("STACK_SAVE_BUTTON", "Save Stack as a named playlist"),
            ) { Icon(Icons.Rounded.Save, uiText(language, "Save Stack", "儲存疊播")) }
            if (canAdd) {
                FilledTonalButton(
                    onClick = { pickerOpen = true },
                    modifier = Modifier.inspectElement("STACK_ADD_BUTTON", "Choose local media files to add"),
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text(uiText(language, "Add", "加入"))
                }
            }
        }

        val trackListState = rememberLazyListState()
        LaunchedEffect(trackListState) {
            try {
                snapshotFlow { trackListState.isScrollInProgress }.collect { scrolling ->
                    ListScrollBudget.set("stack", scrolling)
                }
            } finally {
                ListScrollBudget.set("stack", false)
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f)
                .inspectElement("STACK_TRACK_LIST", "Up to eight simultaneous tracks plus current playback context"),
            state = trackListState,
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            if (nowPlayingFile != null && !nowPlayingRepresented) {
                item(key = "stack-now-playing-context:${nowPlayingFile.path}") {
                    StackNowPlayingRow(
                        file = nowPlayingFile,
                        language = language,
                        canAdd = canAdd,
                        onLoadThumbnail = onLoadThumbnail,
                        isScrolling = { trackListState.isScrollInProgress },
                        onAdd = { addFiles(listOf(nowPlayingFile)) },
                    )
                }
            }

            items(displayed, key = { it.file.path }) { slot ->
                val path = slot.file.path
                val isPrimary = session.active && path == session.primaryPath ||
                    !session.active && path == staged.firstOrNull()?.path
                val isNowPlaying = path == nowPlayingPath
                val reached = session.active && slot.resolvedDurationMs > 0L &&
                    session.positionMs >= slot.resolvedDurationMs
                StackTrackRow(
                    slot = slot,
                    language = language,
                    sessionActive = session.active,
                    isPrimary = isPrimary,
                    isNowPlaying = isNowPlaying,
                    ended = reached,
                    expanded = expandedPath == path,
                    onLoadThumbnail = onLoadThumbnail,
                    isScrolling = { trackListState.isScrollInProgress },
                    onToggleExpanded = { expandedPath = if (expandedPath == path) null else path },
                    onRemove = {
                        if (session.active) StackPlayback.remove(path) else stagedPaths = stagedPaths - path
                    },
                )
            }

            if (displayed.size < 2) {
                item {
                    repeat(2 - displayed.size) {
                        Surface(
                            onClick = { pickerOpen = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
                                .inspectElement("STACK_EMPTY_SLOT", "Tap to choose local tracks"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .52f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                            ),
                        ) {
                            Row(
                                Modifier.height(62.dp).padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    Modifier.size(40.dp),
                                    RoundedCornerShape(9.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        null,
                                        Modifier.padding(9.dp),
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(uiText(language, "Add tracks", "加入歌曲"), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        uiText(language, "Search or select several from Library", "搜尋或從音樂庫多選"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        error?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = .82f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .inspectElement("STACK_MASTER_CONTROLS", "Shared seek and play/pause"),
        ) {
            val duration = if (session.active) session.durationMs else staged.maxOfOrNull { it.durationMs } ?: 0L
            val position = scrubPosition ?: session.positionMs
            if (duration > 0L) {
                Slider(
                    position.coerceIn(0L, duration).toFloat(),
                    { scrubPosition = it.toLong() },
                    onValueChangeFinished = {
                        scrubPosition?.let(StackPlayback::seek)
                        scrubPosition = null
                    },
                    valueRange = 0f..duration.toFloat(),
                    enabled = session.active,
                    modifier = Modifier.fillMaxWidth().inspectElement("STACK_MASTER_TIMELINE", "Seeks all active tracks"),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stackTime(position), style = MaterialTheme.typography.labelMedium)
                if (session.active) {
                    IconButton(
                        onClick = { if (session.playing) StackPlayback.pause() else StackPlayback.play() },
                        modifier = Modifier.size(56.dp).inspectElement("STACK_MASTER_PLAY", "Play or pause every track"),
                    ) {
                        Icon(
                            if (session.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            uiText(
                                language,
                                if (session.playing) "Pause Stack" else "Play Stack",
                                if (session.playing) "暫停疊播" else "播放疊播",
                            ),
                            Modifier.size(32.dp),
                        )
                    }
                    TextButton(onClick = { StackPlayback.stop() }) { Text(uiText(language, "Stop", "停止")) }
                } else {
                    Button(
                        enabled = staged.size >= 2,
                        onClick = {
                            if (StackPlayback.start(staged)) {
                                stagedPaths = emptyList()
                                error = null
                            } else {
                                error = uiText(language, "Could not start these files on this device", "這部裝置無法播放這些檔案")
                            }
                        },
                        modifier = Modifier.inspectElement("STACK_START_BUTTON", "Starts selected tracks together"),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Text(uiText(language, "Play together", "一齊播放"))
                    }
                }
                Text(stackTime(duration), style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (pickerOpen) {
        StackLibraryPicker(
            files = files,
            excludedPaths = displayedPaths,
            maxSelection = remaining,
            language = language,
            onLoadThumbnail = onLoadThumbnail,
            onDismiss = { pickerOpen = false },
            onPick = { addFiles(it); pickerOpen = false },
        )
    }

    if (recommendationsOpen) {
        val recommendationSeeds = remember(displayed, nowPlayingFile) {
            (displayed.map { it.file } + listOfNotNull(nowPlayingFile)).distinctBy { it.path }
        }
        StackRecommendationPicker(
            recommendations = remember(recommendationSeeds, files, playHistory, displayedPaths) {
                recommendStackTracks(
                    seeds = recommendationSeeds,
                    library = files.filter { it.path !in displayedPaths },
                    history = playHistory,
                    limit = 12,
                )
            },
            maxSelection = remaining,
            language = language,
            onLoadThumbnail = onLoadThumbnail,
            onDismiss = { recommendationsOpen = false },
            onPick = { addFiles(it); recommendationsOpen = false },
        )
    }

    if (saveOpen) {
        SaveStackDialog(
            language = language,
            count = displayed.size,
            onDismiss = { saveOpen = false },
            onSave = { name, keyword ->
                onSaveList(name, keyword, displayed.map { it.file.path })
                saveOpen = false
            },
        )
    }
}

@Composable
private fun StackNowPlayingRow(
    file: MediaFile,
    language: AppLanguage,
    canAdd: Boolean,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    isScrolling: () -> Boolean,
    onAdd: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .36f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
            .inspectElement("STACK_NOW_PLAYING_CONTEXT", file.name),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f)),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueThumbnail(file, onLoadThumbnail, isScrolling)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    file.name.substringBeforeLast('.'),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    uiText(language, "NOW PLAYING · not in Stack", "正在播放 · 未加入疊播"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onAdd, enabled = canAdd) { Text(uiText(language, "Add", "加入")) }
        }
    }
}

@Composable
private fun StackTrackRow(
    slot: StackSlot,
    language: AppLanguage,
    sessionActive: Boolean,
    isPrimary: Boolean,
    isNowPlaying: Boolean,
    ended: Boolean,
    expanded: Boolean,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    isScrolling: () -> Boolean,
    onToggleExpanded: () -> Unit,
    onRemove: () -> Unit,
) {
    val path = slot.file.path
    val rowColor = if (isNowPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f)
        else MaterialTheme.colorScheme.surface.copy(alpha = .48f)
    Column(Modifier.fillMaxWidth().background(rowColor)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onToggleExpanded)
                .inspectElement("STACK_TRACK_ROW", slot.file.name).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueThumbnail(slot.file, onLoadThumbnail, isScrolling)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    slot.file.name.substringBeforeLast('.'),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPrimary || isNowPlaying) FontWeight.SemiBold else FontWeight.Normal,
                )
                val status = when {
                    slot.error != null -> uiText(language, "Playback unavailable", "無法播放")
                    isNowPlaying && isPrimary -> uiText(language, "NOW PLAYING · Primary visual", "正在播放 · 主要畫面")
                    isNowPlaying -> uiText(language, "NOW PLAYING", "正在播放")
                    isPrimary -> uiText(language, "Primary visual", "主要畫面")
                    ended -> uiText(language, "Ended", "已播完")
                    else -> slot.file.artist
                }
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        slot.error != null -> MaterialTheme.colorScheme.error
                        isNowPlaying -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (isPrimary) {
                Icon(Icons.Rounded.Star, uiText(language, "Primary visual", "主要畫面"), tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onRemove, modifier = Modifier.inspectElement("STACK_REMOVE_BUTTON", slot.file.name)) {
                Icon(Icons.Rounded.Close, uiText(language, "Remove track", "移除歌曲"))
            }
        }

        if (expanded) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (sessionActive) {
                    TextButton(onClick = { StackPlayback.setPrimary(path) }, enabled = !isPrimary && !ended) {
                        Text(uiText(language, "Make primary", "設為主要"))
                    }
                    TextButton(onClick = { StackPlayback.toggleMute(path) }) {
                        Text(uiText(language, if (slot.muted) "Unmute" else "Mute", if (slot.muted) "取消靜音" else "靜音"))
                    }
                    TextButton(onClick = { StackPlayback.toggleSolo(path) }) {
                        Text(uiText(language, if (slot.solo) "Unsolo" else "Solo", if (slot.solo) "取消獨奏" else "獨奏"))
                    }
                }
            }
            if (sessionActive) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${(slot.volume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        slot.volume,
                        { StackPlayback.setVolume(path, it) },
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                            .inspectElement("STACK_TRACK_VOLUME", slot.file.name),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
    }
}

@Composable
private fun StackLibraryPicker(
    files: List<MediaFile>,
    excludedPaths: Set<String>,
    maxSelection: Int,
    language: AppLanguage,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onDismiss: () -> Unit,
    onPick: (List<MediaFile>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPaths by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val choices = remember(files, excludedPaths, query) {
        files.filter {
            it.path !in excludedPaths &&
                (query.isBlank() || it.name.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true))
        }
    }

    fun toggle(path: String) {
        selectedPaths = if (path in selectedPaths) selectedPaths - path
        else if (selectedPaths.size < maxSelection) selectedPaths + path
        else selectedPaths
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(.96f),
        title = {
            Column {
                Text(uiText(language, "Choose from Library", "從音樂庫選擇"), fontWeight = FontWeight.Bold)
                Text(
                    uiText(
                        language,
                        "${selectedPaths.size} selected · ${choices.size} matching",
                        "已選 ${selectedPaths.size} 首 · ${choices.size} 首符合",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column {
                LiquidMetalSurface(
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    OutlinedTextField(
                        query,
                        { query = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            { IconButton({ query = "" }) { Icon(Icons.Rounded.Clear, null) } }
                        } else null,
                        placeholder = { Text(uiText(language, "Type song, artist, or album", "輸入歌曲、歌手或專輯")) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = {
                            selectedPaths = choices.asSequence().map { it.path }.distinct().take(maxSelection).toList()
                        },
                        enabled = choices.isNotEmpty() && maxSelection > 0,
                    ) { Text(uiText(language, "Select visible", "選取顯示項目")) }
                    TextButton(onClick = { selectedPaths = emptyList() }, enabled = selectedPaths.isNotEmpty()) {
                        Text(uiText(language, "Clear", "清除"))
                    }
                }

                val pickerListState = rememberLazyListState()
                LaunchedEffect(pickerListState) {
                    try {
                        snapshotFlow { pickerListState.isScrollInProgress }.collect { scrolling ->
                            ListScrollBudget.set("stack_picker", scrolling)
                        }
                    } finally {
                        ListScrollBudget.set("stack_picker", false)
                    }
                }
                LazyColumn(Modifier.heightIn(min = 260.dp, max = 470.dp), state = pickerListState) {
                    items(choices, key = { it.path }) { file ->
                        val checked = file.path in selectedPaths
                        ListItem(
                            headlineContent = {
                                Text(file.name.substringBeforeLast('.'), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    listOf(file.artist.takeIf(String::isNotBlank), stackTime(file.durationMs))
                                        .filterNotNull().joinToString(" • "),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            leadingContent = {
                                QueueThumbnail(file, onLoadThumbnail) { pickerListState.isScrollInProgress }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { toggle(file.path) },
                                    enabled = checked || selectedPaths.size < maxSelection,
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { toggle(file.path) }
                                .inspectElement("STACK_PICKER_ROW", file.name),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPaths.isNotEmpty(),
                onClick = { onPick(selectedPaths.mapNotNull { path -> files.firstOrNull { it.path == path } }) },
            ) { Text(uiText(language, "Add ${selectedPaths.size}", "加入 ${selectedPaths.size} 首")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Close", "關閉")) } },
    )
}

@Composable
private fun StackRecommendationPicker(
    recommendations: List<StackRecommendation>,
    maxSelection: Int,
    language: AppLanguage,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onDismiss: () -> Unit,
    onPick: (List<MediaFile>) -> Unit,
) {
    var selectedPaths by rememberSaveable { mutableStateOf(emptyList<String>()) }
    fun toggle(path: String) {
        selectedPaths = if (path in selectedPaths) selectedPaths - path
        else if (selectedPaths.size < maxSelection) selectedPaths + path
        else selectedPaths
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(.96f),
        title = {
            Column {
                Text(uiText(language, "Offline recommendations", "離線推薦"), fontWeight = FontWeight.Bold)
                Text(
                    uiText(
                        language,
                        "Local filename, folder, metadata, duration and play-history signals only",
                        "只使用本機檔名、資料夾、媒體資料、時長及播放記錄",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            if (recommendations.isEmpty()) {
                Text(
                    uiText(
                        language,
                        "No strong local matches yet. Add a track or enable local play history to give Stack more context.",
                        "暫時沒有足夠相似的本機歌曲。先加入歌曲，或啟用本機播放記錄以提供更多線索。",
                    ),
                )
            } else {
                val listState = rememberLazyListState()
                LazyColumn(Modifier.heightIn(min = 220.dp, max = 470.dp), state = listState) {
                    items(recommendations, key = { it.file.path }) { recommendation ->
                        val file = recommendation.file
                        val checked = file.path in selectedPaths
                        ListItem(
                            headlineContent = {
                                Text(file.name.substringBeforeLast('.'), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    recommendation.reasons.joinToString(" • ") { recommendationReason(it, language) },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            leadingContent = { QueueThumbnail(file, onLoadThumbnail) { listState.isScrollInProgress } },
                            trailingContent = {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { toggle(file.path) },
                                    enabled = checked || selectedPaths.size < maxSelection,
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { toggle(file.path) }
                                .inspectElement("STACK_RECOMMENDATION_ROW", file.name),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPaths.isNotEmpty(),
                onClick = { onPick(recommendations.filter { it.file.path in selectedPaths }.map { it.file }) },
            ) { Text(uiText(language, "Add ${selectedPaths.size}", "加入 ${selectedPaths.size} 首")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Close", "關閉")) } },
    )
}

@Composable
private fun SaveStackDialog(
    language: AppLanguage,
    count: Int,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var keyword by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText(language, "Save Stack", "儲存疊播")) },
        text = {
            Column {
                Text(
                    uiText(
                        language,
                        "Save these $count tracks. An optional keyword stays attached and automatically includes matching local songs when this list is opened later.",
                        "儲存這 $count 首歌曲。可選關鍵字會保留，日後開啟此清單時會自動加入符合的本機歌曲。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    singleLine = true,
                    label = { Text(uiText(language, "Stack name (optional)", "疊播名稱（選填）")) },
                    supportingText = {
                        Text(
                            uiText(
                                language,
                                "Leave blank to use the keyword or a local Stack track-count name.",
                                "留空時會使用關鍵字，或以本機疊播歌曲數自動命名。",
                            ),
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it.take(80) },
                    singleLine = true,
                    label = { Text(uiText(language, "My Stack keyword (optional)", "我的疊播關鍵字（選填）")) },
                    placeholder = { Text(uiText(language, "e.g. live, piano, Ado", "例如：live、piano、Ado")) },
                    supportingText = {
                        Text(
                            uiText(
                                language,
                                "Matches local song names and file paths. The keyword is saved, not sent anywhere.",
                                "比對本機歌曲名稱及檔案路徑。關鍵字只會儲存在本機，不會傳送出去。",
                            ),
                        )
                    },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = count > 0,
                onClick = { onSave(name.trim(), keyword.trim()) },
                modifier = Modifier.inspectElement("STACK_SAVE_CONFIRM_BUTTON", "Creates a local playlist from the displayed Stack"),
            ) {
                Text(uiText(language, "Save", "儲存"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } },
    )
}

private fun recommendationReason(reason: StackRecommendationReason, language: AppLanguage): String =
    when (reason) {
        StackRecommendationReason.FILENAME -> uiText(language, "similar name", "相似檔名")
        StackRecommendationReason.SAME_FOLDER -> uiText(language, "same folder", "同一資料夾")
        StackRecommendationReason.SAME_ARTIST -> uiText(language, "same artist", "同一歌手")
        StackRecommendationReason.SAME_ALBUM -> uiText(language, "same album", "同一專輯")
        StackRecommendationReason.SIMILAR_DURATION -> uiText(language, "similar length", "相近時長")
        StackRecommendationReason.PLAY_HISTORY -> uiText(language, "played nearby", "播放記錄相近")
    }

private fun stackTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
