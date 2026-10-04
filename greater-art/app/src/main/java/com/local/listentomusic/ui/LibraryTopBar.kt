package com.local.listentomusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.LibraryUiState
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.PlayHistoryEntry
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.ui.components.GaIconAction
import com.local.listentomusic.ui.components.LiquidMetalSurface
import com.local.listentomusic.ui.theme.GaRadius
import com.local.listentomusic.ui.theme.GaSpacing
import com.local.listentomusic.ui.theme.gaChromeColor

/** Common actions for all three Library-family pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryTopBar(
    appName: String,
    state: LibraryUiState,
    preferences: UserPreferences,
    playHistory: List<PlayHistoryEntry>,
    onRefresh: () -> Unit,
    onPlayHistoryEnabled: (Boolean) -> Unit,
    onClearPlayHistory: () -> Unit,
    onSortChange: (SortMode) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val language = preferences.appLanguage
    val activePlaylist = preferences.playlists.firstOrNull { it.id == preferences.activePlaylistId }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var burnHistoryConfirm by remember { mutableStateOf(false) }
            TopAppBar(
                modifier = Modifier.inspectElement("LIBRARY_TOP_BAR", "App title, settings, refresh, and sort"),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val markShape = RoundedCornerShape(GaRadius.compact)
                        LiquidMetalSurface(
                            modifier = Modifier.size(40.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, markShape),
                            shape = markShape,
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(com.local.listentomusic.R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(3.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(Modifier.width(GaSpacing.md))
                        Column {
                            Text(appName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                uiText(language, "${state.files.size} files • offline", "${state.files.size} 個檔案 • 離線"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    GaIconAction(
                        icon = Icons.Rounded.Settings,
                        contentDescription = uiText(language, "Settings", "設定"),
                        onClick = onOpenSettings,
                        modifier = Modifier.inspectElement("SETTINGS_BUTTON", "Opens Greater Art settings"),
                    )
                    GaIconAction(
                        icon = Icons.Rounded.History,
                        contentDescription = uiText(language, "Play history", "播放紀錄"),
                        onClick = { historyOpen = true },
                        tint = if (preferences.playHistoryEnabled) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.inspectElement("PLAY_HISTORY_BUTTON", "Opens optional local playback history"),
                    )
                    GaIconAction(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = uiText(language, "Scan again", "重新掃描"),
                        onClick = onRefresh,
                        modifier = Modifier.inspectElement("REFRESH_LIBRARY_BUTTON", "Rescans Download for supported media"),
                    )
                    if (activePlaylist == null) Box {
                        GaIconAction(
                            icon = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = uiText(language, "Sort", "排序"),
                            onClick = { sortMenuOpen = true },
                            modifier = Modifier.inspectElement("SORT_BUTTON", "Opens Library order choices"),
                        )
                        DropdownMenu(sortMenuOpen, { sortMenuOpen = false }) {
                            SortMode.entries.forEach { mode ->
                                val label = when (mode) {
                                    SortMode.CUSTOM -> uiText(language, "Custom order", "自訂排序")
                                    SortMode.NAME_ASC -> uiText(language, "Name A–Z", "名稱 A–Z")
                                    SortMode.NAME_DESC -> uiText(language, "Name Z–A", "名稱 Z–A")
                                    SortMode.DATE_DESC -> uiText(language, "Newest first", "最新優先")
                                    SortMode.DATE_ASC -> uiText(language, "Oldest first", "最舊優先")
                                }
                                DropdownMenuItem(
                                    text = { Text(if (mode == state.sortMode) "✓  $label" else label) },
                                    onClick = { sortMenuOpen = false; onSortChange(mode) },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = gaChromeColor(),
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
    if (historyOpen) {
        val dateFormat = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT) }
        AlertDialog(
            onDismissRequest = { historyOpen = false },
            icon = { Icon(Icons.Rounded.History, null) },
            title = { Text(uiText(language, "Play history", "播放紀錄")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(uiText(language, "Record locally", "只在本機記錄"), fontWeight = FontWeight.SemiBold)
                            Text(
                                uiText(language, "Disabled by default. History is never exported in settings backups.", "預設關閉，播放紀錄永遠不會匯出到設定備份。"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(preferences.playHistoryEnabled, onPlayHistoryEnabled)
                    }
                    if (playHistory.isEmpty()) {
                        Text(uiText(language, "No local play history", "尚無本機播放紀錄"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 370.dp)) {
                            items(playHistory, key = { "${it.playedAtEpochMs}:${it.path}" }) { entry ->
                                val file = state.files.firstOrNull { it.path == entry.path || it.sourcePath == entry.path }
                                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text(
                                        file?.let { com.local.listentomusic.model.mediaTitle(it.name, it.path) }
                                            ?: entry.path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.'),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        dateFormat.format(java.util.Date(entry.playedAtEpochMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (playHistory.isNotEmpty()) TextButton(onClick = { burnHistoryConfirm = true }) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(5.dp))
                    Text(uiText(language, "Burn history", "燒毀紀錄"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { historyOpen = false }) { Text(uiText(language, "Close", "關閉")) } },
        )
    }
    if (burnHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { burnHistoryConfirm = false },
            icon = { Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(uiText(language, "Burn all play history?", "燒毀所有播放紀錄？")) },
            text = { Text(uiText(language, "This permanently removes the local record. Media files and playlists are unchanged.", "這會永久移除本機紀錄，媒體檔案與播放清單不會變更。")) },
            confirmButton = { TextButton(onClick = { onClearPlayHistory(); burnHistoryConfirm = false }) {
                Text(uiText(language, "Burn history", "燒毀紀錄"), color = MaterialTheme.colorScheme.error)
            } },
            dismissButton = { TextButton(onClick = { burnHistoryConfirm = false }) { Text(uiText(language, "Cancel", "取消")) } },
        )
    }
}
