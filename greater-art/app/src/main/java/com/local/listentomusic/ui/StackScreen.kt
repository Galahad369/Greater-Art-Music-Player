package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.GraphicEq
import com.local.listentomusic.playback.StackAlignmentController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.OpenInFull
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
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Delete
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
import com.local.listentomusic.data.SavedStack
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.sourceMediaPath
import com.local.listentomusic.playback.STACK_LOOP_DEFAULT
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.playback.StackRecommendation
import com.local.listentomusic.playback.StackRecommendationReason
import com.local.listentomusic.playback.StackSlot
import com.local.listentomusic.playback.recommendStackTracks
import com.local.listentomusic.ui.components.LiquidMetalSurface
import com.local.listentomusic.ui.theme.gaChromeColor
import com.local.listentomusic.ui.theme.gaDividerColor

/** Temporary multi-track listening, sharing Library's wallpaper and visual language. */
@Composable
fun StackScreen(
    files: List<MediaFile>,
    language: AppLanguage,
    contentPadding: PaddingValues,
    nowPlayingPath: String?,
    playHistory: List<PlayHistoryEntry>,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    savedStacks: List<SavedStack>,
    onSaveStack: (String, String, List<StackSlot>, String?, Boolean) -> Unit,
    onDeleteStack: (String) -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val session by StackPlayback.state.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    val alignmentState by StackAlignmentController.state.collectAsStateWithLifecycle()
    val alignment = if (alignmentState.primaryPath == session.primaryPath && alignmentState.paths == session.slots.map { it.file.path })
        alignmentState else com.local.listentomusic.playback.StackAlignmentProgress()
    val alignProgress = if (alignment.running) "${uiText(language, "Aligning by sound", "正在按聲音對齊")} ${alignment.completed}/${alignment.total}" else null
    var stagedPaths by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val staged = remember(files, stagedPaths) {
        stagedPaths.mapNotNull { path -> files.firstOrNull { it.path == path } }
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var recommendationsOpen by remember { mutableStateOf(false) }
    var saveOpen by remember { mutableStateOf(false) }
    var savedOpen by remember { mutableStateOf(false) }
    var deleteSaved by remember { mutableStateOf<SavedStack?>(null) }
    var stagedPresetRaw by rememberSaveable { mutableStateOf("") }
    val stagedPreset = remember(stagedPresetRaw) { com.local.listentomusic.data.SavedStackCodec.decode(stagedPresetRaw).firstOrNull() }
    var saveSnapshot by remember { mutableStateOf<List<StackSlot>>(emptyList()) }
    var savePrimary by remember { mutableStateOf<String?>(null) }
    var saveLoop by remember { mutableStateOf(true) }
    var expandedPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scrubPosition by remember { mutableStateOf<Long?>(null) }

    // Keep the editable mix when Stop, a failed voice, or a two-to-one removal
    // ends simultaneous playback. Previously stagedPaths was cleared at Start.
    LaunchedEffect(session.slots, session.primaryPath, session.loopEnabled) {
        if (session.active) {
            stagedPaths = session.slots.map { it.file.path }
            stagedPresetRaw = com.local.listentomusic.data.SavedStackCodec.encode(listOf(SavedStack("draft", "draft", session.slots.map {
                com.local.listentomusic.data.SavedStackTrack(it.file.path, it.volume, it.muted, it.solo, it.offsetMs)
            }, session.primaryPath ?: session.slots.first().file.path, session.loopEnabled)))
        }
    }

    val displayed = if (session.active) session.slots else staged.map { file ->
        val saved = stagedPreset?.tracks?.firstOrNull { it.path == file.path }
        StackSlot(file, saved?.volume ?: 1f, saved?.muted ?: false, saved?.solo ?: false, offsetMs = saved?.offsetMs ?: 0L)
    }
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
            Surface(shape = RoundedCornerShape(50), color = gaChromeColor()) {
                Text(
                    "${displayed.size} / ${StackPlayback.MAX_TRACKS}",
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { savedOpen = true },
                modifier = Modifier.inspectElement("STACK_SAVED_BUTTON", "Open saved simultaneous mixes"),
            ) { Icon(Icons.Rounded.FolderOpen, uiText(language, "Saved Stacks", "已儲存疊播")) }
            IconButton(
                onClick = { recommendationsOpen = true },
                enabled = canAdd && (displayed.isNotEmpty() || nowPlayingFile != null),
                modifier = Modifier.inspectElement("STACK_RECOMMEND_BUTTON", "Offline local recommendations"),
            ) { Icon(Icons.Rounded.Star, uiText(language, "Recommend tracks", "推薦歌曲")) }
            IconButton(
                onClick = {
                    saveSnapshot = displayed.toList()
                    savePrimary = if (session.active) session.primaryPath else stagedPreset?.primaryPath ?: staged.firstOrNull()?.path
                    saveLoop = if (session.active) session.loopEnabled else stagedPreset?.loopEnabled ?: true
                    saveOpen = true
                },
                enabled = displayed.isNotEmpty(),
                modifier = Modifier.inspectElement("STACK_SAVE_BUTTON", "Save this simultaneous mix, not the Library playlist"),
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
        if (session.active && session.slots.size >= 2) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = gaChromeColor(),
                border = androidx.compose.foundation.BorderStroke(1.dp, gaDividerColor()),
            ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (alignProgress != null) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(alignProgress!!, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = StackAlignmentController::cancel) { Text(uiText(language, "Cancel", "取消")) }
                } else {
                    TextButton(
                        modifier = Modifier.inspectElement("STACK_ALIGN_SOUND_BUTTON", "Offline arrangement alignment; uncertain matches are unchanged"),
                        onClick = { StackAlignmentController.start(context) },
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null)
                        Spacer(Modifier.width(6.dp))
                        Text(uiText(language, "Align by sound", "按聲音對齊"))
                    }
                }
                if (!alignment.running) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenPlayer, modifier = Modifier.inspectElement("STACK_OPEN_PLAYER", "Open the primary video without replacing the mix")) {
                        Icon(Icons.Rounded.OpenInFull, uiText(language, "Now Playing", "正在播放"), Modifier.size(26.dp))
                    }
                }
            }
            if (alignProgress != null) Text(
                uiText(language, "Playback resumes after analysis", "分析後繼續播放"),
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            alignment.matched?.let { matched ->
                Text("${uiText(language, "Aligned tracks", "已對齊歌曲")}: $matched/${alignment.total}",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                if (matched < alignment.total) Text(uiText(language, "Uncertain matches kept unchanged", "未能確認的匹配保持不變"),
                    Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall)
            }
            if (alignment.failed) Text(uiText(language, "Could not analyse these files", "無法分析這些檔案"),
                Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
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
                    !session.active && path == (stagedPreset?.primaryPath ?: staged.firstOrNull()?.path)
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
                        stagedPaths = stagedPaths - path
                        if (session.active) StackPlayback.remove(path)
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
                            color = gaChromeColor(),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, gaDividerColor(),
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
                                        tint = MaterialTheme.colorScheme.primary,
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
            Modifier.fillMaxWidth().background(gaChromeColor())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .inspectElement("STACK_MASTER_CONTROLS", "Shared seek and play/pause"),
        ) {
            val duration = if (session.active) session.durationMs else com.local.listentomusic.playback.stackDuration(displayed,
                stagedPreset?.primaryPath?.takeIf { path -> displayed.any { it.file.path == path } } ?: staged.firstOrNull()?.path)
            val position = scrubPosition ?: session.positionMs
            if (duration > 0L) {
                CompactSlider(
                    value = position.coerceIn(0L, duration).toFloat(),
                    onValueChange = { scrubPosition = it.toLong() },
                    onValueChangeFinished = {
                        scrubPosition?.let(StackPlayback::seek)
                        scrubPosition = null
                    },
                    valueRange = 0f..duration.toFloat(),
                    enabled = session.active && alignProgress == null,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.inspectElement("STACK_MASTER_TIMELINE", "Seeks all active tracks"),
                )
            }
            if (session.active) Text(
                if (session.synchronizing) uiText(language, "Synchronizing tracks…", "正在同步歌曲…")
                else uiText(language, "${session.runningTracks}/${session.slots.count { it.error == null }} tracks playing", "${session.runningTracks}/${session.slots.count { it.error == null }} 首播放中"),
                Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stackTime(position), style = MaterialTheme.typography.labelMedium)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (session.active) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            IconButton(
                                onClick = { StackPlayback.setLoop(!session.loopEnabled) },
                                modifier = Modifier.size(48.dp)
                                    .inspectElement("STACK_LOOP_BUTTON", if (session.loopEnabled) "Stack loop on" else "Stack loop off"),
                            ) {
                                Icon(
                                    Icons.Rounded.Repeat,
                                    uiText(language, "Loop whole Stack", "循環整個疊播"),
                                    Modifier.size(26.dp),
                                    tint = if (session.loopEnabled) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                )
                            }
                            IconButton(
                                onClick = { if (session.playing) StackPlayback.pause() else StackPlayback.play() },
                                enabled = alignProgress == null,
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
                        }
                    } else {
                        Button(
                            enabled = staged.size >= 2,
                            onClick = {
                                val primary = stagedPreset?.primaryPath
                                val ordered = staged.sortedBy { if (it.path == primary) 0 else 1 }
                                if (StackPlayback.start(ordered)) {
                                    displayed.forEach { slot ->
                                        StackPlayback.setVolume(slot.file.path, slot.volume)
                                        if (slot.muted) StackPlayback.toggleMute(slot.file.path)
                                        if (slot.solo) StackPlayback.toggleSolo(slot.file.path)
                                    }
                                    StackPlayback.setLoop(stagedPreset?.loopEnabled ?: STACK_LOOP_DEFAULT)
                                    StackPlayback.setOffsets(displayed.associate { it.file.path to it.offsetMs })
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

    if (savedOpen) {
        AlertDialog(
            onDismissRequest = { savedOpen = false },
            title = { Text(uiText(language, "Saved Stacks", "已儲存疊播")) },
            text = {
                if (savedStacks.isEmpty()) Text(uiText(language, "Save a mix here, separately from Library playlists.", "儲存疊播組合，與音樂庫播放清單分開。"))
                else LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(savedStacks, key = { it.id }) { saved ->
                        ListItem(
                            headlineContent = { Text(saved.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(uiText(language, "${saved.tracks.size} tracks · simultaneous", "${saved.tracks.size} 首 · 同時播放")) },
                            trailingContent = { IconButton(onClick = { deleteSaved = saved }) {
                                Icon(Icons.Rounded.Delete, uiText(language, "Delete Stack", "刪除疊播"), tint = MaterialTheme.colorScheme.error)
                            } },
                            modifier = Modifier.clickable {
                                val available = saved.tracks.mapNotNull { track -> files.firstOrNull { it.path == track.path } }
                                if (available.isEmpty()) {
                                    error = uiText(language, "Saved files are missing. Restore the files or delete this saved Stack.", "儲存的檔案已遺失，請還原檔案或刪除此疊播。")
                                } else {
                                    if (session.active) StackPlayback.stop()
                                    stagedPaths = available.map { it.path }
                                    stagedPresetRaw = com.local.listentomusic.data.SavedStackCodec.encode(listOf(saved))
                                    error = if (available.size < saved.tracks.size) uiText(language, "Some saved files are missing", "部分已儲存檔案已遺失") else null
                                }
                                savedOpen = false
                            }.inspectElement("STACK_SAVED_ROW", saved.name),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { savedOpen = false }) { Text(uiText(language, "Close", "關閉")) } },
        )
    }
    deleteSaved?.let { saved ->
        AlertDialog(
            onDismissRequest = { deleteSaved = null },
            title = { Text(uiText(language, "Delete Stack?", "刪除疊播？")) },
            text = { Text(uiText(language, "Remove ${saved.name}? Media files stay untouched.", "移除「${saved.name}」？媒體檔案不會被刪除。")) },
            confirmButton = { TextButton(onClick = { onDeleteStack(saved.id); deleteSaved = null }) {
                Text(uiText(language, "Delete", "刪除"), color = MaterialTheme.colorScheme.error)
            } },
            dismissButton = { TextButton(onClick = { deleteSaved = null }) { Text(uiText(language, "Cancel", "取消")) } },
        )
    }
    if (saveOpen) {
        SaveStackDialog(
            language = language,
            count = saveSnapshot.size,
            onDismiss = { saveOpen = false },
            onSave = { name, keyword ->
                onSaveStack(name, keyword, saveSnapshot, savePrimary, saveLoop)
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
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
            .inspectElement("STACK_NOW_PLAYING_CONTEXT", file.name),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
    val rowColor = if (isNowPlaying) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
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
                    isNowPlaying && isPrimary -> uiText(language, "Primary visual", "主要畫面")
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
                Icon(Icons.Rounded.Star, uiText(language, "Primary visual", "主要畫面"), tint = MaterialTheme.colorScheme.primary)
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
                if (!isPrimary) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { StackPlayback.setOffset(path, slot.offsetMs - 100L) }) { Text("−0.1s") }
                    Text(java.lang.String.format(java.util.Locale.ROOT, "%+.2fs", slot.offsetMs / 1000.0), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { StackPlayback.setOffset(path, slot.offsetMs + 100L) }) { Text("+0.1s") }
                    TextButton(onClick = { StackPlayback.setOffset(path, 0L) }) { Text(uiText(language, "Reset alignment", "重設對齊")) }
                }
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
        HorizontalDivider(color = gaDividerColor())
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
                        HorizontalDivider(color = gaDividerColor())
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
                        HorizontalDivider(color = gaDividerColor())
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
                        "Save this $count-track simultaneous mix, including its primary track, levels, mute, solo and loop. It stays separate from Library playlists.",
                        "儲存這 $count 首的同時播放組合，包括主歌曲、音量、靜音、獨奏與循環，與音樂庫播放清單分開。",
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
                                "A local label for this mix. It does not change its saved tracks.",
                                "此組合的本機標籤，不會改變已儲存的歌曲。",
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
                modifier = Modifier.inspectElement("STACK_SAVE_CONFIRM_BUTTON", "Saves this mix, including primary, levels, loop and alignment offsets"),
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
