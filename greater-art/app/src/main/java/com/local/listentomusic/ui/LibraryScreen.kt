package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.local.listentomusic.LibraryStatus
import com.local.listentomusic.LibraryUiState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.PlayHistoryEntry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.MiniWindowMetrics
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.ui.components.LiquidMetalSurface
import com.local.listentomusic.ui.components.GAButton
import com.local.listentomusic.ui.components.GACard
import com.local.listentomusic.ui.components.GAChip
import com.local.listentomusic.ui.components.GATypography
import com.local.listentomusic.ui.design.DesignTokens
import com.local.listentomusic.ui.design.SemanticColors
import kotlin.math.abs
import kotlinx.coroutines.launch

// Aligned chrome translucency: top bar, search filter, playlist and Nodes buttons
// all sit half-transparent over the app background (video wallpaper / metal).
private const val CHROME_ALPHA = DesignTokens.Spacing.sm.toFloat() / 16f  // 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    appName: String,
    state: LibraryUiState,
    preferences: UserPreferences,
    playHistory: List<PlayHistoryEntry>,
    currentPath: String?,
    contentPadding: PaddingValues,
    onGrantStorageAccess: () -> Unit,
    onRefresh: () -> Unit,
    onPlayHistoryEnabled: (Boolean) -> Unit,
    onClearPlayHistory: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistAndSeed: (String, String?, String?) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onRemoveFromPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteFile: suspend (MediaFile) -> String?,
    onShareMedia: (MediaFile) -> Unit,
    onShareCurrentList: () -> Unit,
    onShareSelectedFiles: (List<MediaFile>) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onOpenSettings: () -> Unit,
    onStackTogether: (List<MediaFile>) -> Boolean,
    onPlay: (MediaFile) -> Unit,
    onEditDisplay: (MediaFile) -> Unit,
    onCreateRule: () -> Unit,
    onAddSelected: (String, List<String>) -> Unit,
    onCreateSelected: (String, List<String>) -> Unit,
) {
    val language = preferences.appLanguage
    val activePlaylist = preferences.playlists.firstOrNull { it.id == preferences.activePlaylistId }
    val favouritesActive = preferences.activePlaylistId == com.local.listentomusic.data.FAVOURITES_PLAYLIST_ID
    var sortMenuOpen by remember { mutableStateOf(false) }
    var playlistMenuOpen by remember { mutableStateOf(false) }
    var createPlaylistOpen by remember { mutableStateOf(false) }
    var createSeedPath by remember { mutableStateOf<String?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var seedKeyword by remember { mutableStateOf("") }
    var songListFile by remember { mutableStateOf<MediaFile?>(null) }
    var openRowActionsPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<MediaFile?>(null) }
    var deleteStep by remember { mutableStateOf(0) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val actionScope = rememberCoroutineScope()
    var selected by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectionMenu by remember { mutableStateOf(false) }
    var selectionNameDialog by remember { mutableStateOf(false) }
    var selectionName by remember { mutableStateOf("") }
    var historyOpen by remember { mutableStateOf(false) }
    var burnHistoryConfirm by remember { mutableStateOf(false) }
    var shareSelectedConfirm by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.padding(contentPadding).inspectElement("LIBRARY_SCREEN", "Scrollable local media library"),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
                    TopAppBar(
                        modifier = Modifier
                            .inspectElement("LIBRARY_TOP_BAR", "App title, settings, refresh, and sort")
                            .height(DesignTokens.TouchTarget.lg),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md)
                            ) {
                                val markShape = RoundedCornerShape(DesignTokens.CornerRadius.sm)
                                LiquidMetalSurface(
                                    modifier = Modifier
                                        .size(DesignTokens.TouchTarget.min)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, markShape),
                                    shape = markShape,
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(com.local.listentomusic.R.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.xs),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                Spacer(Modifier.width(DesignTokens.Spacing.md))
                                Column {
                                    GATitle(text = appName, variant = GATitleVariant.H5)
                                    GABody(
                                        text = uiText(language, "${state.files.size} files • offline", "${state.files.size} 個檔案 • 離線"),
                                        variant = GABodyVariant.SMALL,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            GAButton(variant = GAButtonVariant.ICON, size = GAButtonSize.MEDIUM, onClick = onOpenSettings, modifier = Modifier.inspectElement("SETTINGS_BUTTON", "Opens Greater Art settings")) {
                                Icon(Icons.Rounded.Settings, uiText(language, "Settings", "設定"), Modifier.size(20.dp))
                            }
                            GAButton(variant = GAButtonVariant.ICON, size = GAButtonSize.MEDIUM, onClick = { historyOpen = true }, modifier = Modifier.inspectElement("PLAY_HISTORY_BUTTON", "Opens optional local playback history")) {
                                Icon(
                                    Icons.Rounded.History,
                                    uiText(language, "Play history", "播放紀錄"),
                                    Modifier.size(20.dp),
                                    tint = if (preferences.playHistoryEnabled) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            GAButton(variant = GAButtonVariant.ICON, size = GAButtonSize.MEDIUM, onClick = onRefresh, modifier = Modifier.inspectElement("REFRESH_LIBRARY_BUTTON", "Rescans Download for supported media")) {
                                Icon(Icons.Rounded.Refresh, uiText(language, "Scan again", "重新掃描"), Modifier.size(20.dp))
                            }
                            if (activePlaylist == null) Box {
                                GAButton(variant = GAButtonVariant.ICON, size = GAButtonSize.MEDIUM, onClick = { sortMenuOpen = true }, modifier = Modifier.inspectElement("SORT_BUTTON", "Opens Library order choices")) {
                                    Icon(Icons.AutoMirrored.Rounded.Sort, uiText(language, "Sort", "排序"), Modifier.size(20.dp))
                                }
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
                            containerColor = MaterialTheme.colorScheme.background.copy(alpha = CHROME_ALPHA),
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    )
                },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (selected.isNotEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected.size}", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { selected = (selected + state.files.map { it.path }).distinct() }) { Text(uiText(language, "Select matches", "選取搜尋結果")) }
                    TextButton(onClick = { selected = emptyList() }) { Text(uiText(language, "Clear", "清除")) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = selected.size in 2..com.local.listentomusic.playback.StackPlayback.MAX_TRACKS,
                    onClick = {
                        if (onStackTogether(state.files.filter { it.path in selected })) selected = emptyList()
                    }, modifier = Modifier.inspectElement("STACK_TOGETHER_BUTTON", "Play 2–8 selected files simultaneously")) {
                        Text(uiText(language, "Stack together", "一齊播放"))
                    }
                    Box {
                    TextButton(onClick = { selectionMenu = true }) { Text(uiText(language, "Add to…", "加入…")) }
                    DropdownMenu(selectionMenu, { selectionMenu = false }) {
                        preferences.playlists.filter { it.rule == null }.forEach { playlist ->
                            DropdownMenuItem(text = { Text(playlist.name) }, onClick = { onAddSelected(playlist.id, selected); selected = emptyList(); selectionMenu = false })
                        }
                        DropdownMenuItem(text = { Text(uiText(language, "Create playlist", "建立播放清單")) }, onClick = { selectionMenu = false; selectionNameDialog = true })
                        DropdownMenuItem(
                            text = { Text(uiText(language, "Share selected media files", "分享已選媒體檔案")) },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            onClick = { selectionMenu = false; shareSelectedConfirm = true },
                        )
                    }
                    }
                }
            }
            val searchInteraction = remember { MutableInteractionSource() }
                        val searchFocused by searchInteraction.collectIsFocusedAsState()
                        val searchScale by animateFloatAsState(if (searchFocused) 1.012f else 1f, label = "library-filter-scale")
                        val searchGlow by animateColorAsState(
                            if (searchFocused) MaterialTheme.colorScheme.secondary.copy(alpha = .72f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f),
                            label = "library-filter-glow",
                        )
                        LiquidMetalSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.md, vertical = DesignTokens.Spacing.xs)
                                .graphicsLayer { scaleX = searchScale; scaleY = searchScale }
                                .border(1.dp, searchGlow, RoundedCornerShape(DesignTokens.CornerRadius.lg))
                                .inspectElement("LIBRARY_FILTER", "Animated local filename search and clear control"),
                            shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
                            baseColor = MaterialTheme.colorScheme.background.copy(alpha = CHROME_ALPHA),
                        ) {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                interactionSource = searchInteraction,
                                shape = RoundedCornerShape(DesignTokens.CornerRadius.lg),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = if (state.query.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { onQueryChange("") }) {
                                            Icon(Icons.Rounded.Clear, uiText(language, "Clear filter", "清除篩選"))
                                        }
                                    }
                                } else null,
                                placeholder = { Text(uiText(language, "Filter library", "篩選音樂庫")) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                ),
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.md, vertical = DesignTokens.Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) {
                                GAButton(
                                    variant = GAButtonVariant.OUTLINED,
                                    size = GAButtonSize.MEDIUM,
                                    onClick = { playlistMenuOpen = true },
                                    modifier = Modifier.inspectElement("PLAYLIST_BUTTON", "Selects or manages a playlist"),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, null)
                                        Spacer(Modifier.width(DesignTokens.Spacing.sm))
                                        Text(
                                            when {
                                                favouritesActive -> uiText(language, "Favorites", "我的最愛")
                                                activePlaylist != null -> activePlaylist.name
                                                else -> uiText(language, "All songs", "所有歌曲")
                                            },
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                else activePlaylist != null && state.files.isNotEmpty(),
                        )
                        DropdownMenuItem(
                            text = { Text(uiText(language, "Create playlist", "建立播放清單")) },
                            onClick = { playlistMenuOpen = false; createSeedPath = null; createPlaylistOpen = true },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                        )
                        DropdownMenuItem(
                            text = { Text(when {
                                favouritesActive -> uiText(language, "Share Favorites", "分享我的最愛")
                                activePlaylist == null -> uiText(language, "Share current Library list", "分享目前音樂庫清單")
                                else -> uiText(language, "Share playlist", "分享播放清單")
                            }) },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            enabled = state.files.isNotEmpty(),
                            onClick = { playlistMenuOpen = false; onShareCurrentList() },
                        )
                        activePlaylist?.let { pl ->
                            DropdownMenuItem(
                                text = { Text(uiText(language, "Delete list", "刪除清單")) },
                                onClick = { playlistMenuOpen = false; onDeletePlaylist(pl.id) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            )
                        }
                    }
                }
                if (activePlaylist != null) {
                    Text(
                        uiText(language, "Hold + drag to reorder", "長按拖曳排序"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            when (state.status) {
                LibraryStatus.NEEDS_PERMISSION -> MessageState(
                    uiText(language, "Allow local file access", "允許本機檔案存取"),
                    uiText(language, "Android will open settings. Enable all-files access, then return here.", "Android 將開啟設定。請允許存取所有檔案，然後返回。"),
                    uiText(language, "Open settings", "開啟設定"),
                    onGrantStorageAccess,
                )
                LibraryStatus.SCANNING -> Box(Modifier.fillMaxSize().inspectElement("LIBRARY_LOADING", "Initial local media scan"), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LiquidMetalSurface(
                            modifier = Modifier.size(92.dp),
                            shape = RoundedCornerShape(26.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                androidx.compose.ui.res.painterResource(com.local.listentomusic.R.drawable.ic_launcher_foreground),
                                null,
                                Modifier.size(62.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Text(uiText(language, "Opening your library", "正在開啟音樂庫"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(Modifier.width(112.dp).height(2.dp), color = MaterialTheme.colorScheme.secondary)
                        Text(uiText(language, "Reading Download locally", "正在本機讀取 Download"),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LibraryStatus.FOLDER_MISSING -> MessageState(
                    uiText(language, "Download folder unavailable", "無法使用 Download 資料夾"),
                    uiText(language, "The app could not open:\n${state.targetPath}", "應用程式無法開啟：\n${state.targetPath}"),
                    uiText(language, "Scan again", "重新掃描"),
                    onRefresh,
                )
                LibraryStatus.CANNOT_READ -> MessageState(
                    uiText(language, "Folder access was lost", "資料夾存取權已失效"),
                    uiText(language, "Grant file access again, then return to the app.", "請重新授予檔案存取權，然後返回應用程式。"),
                    uiText(language, "Open settings", "開啟設定"),
                    onGrantStorageAccess,
                )
                LibraryStatus.READY -> if (state.files.isEmpty()) {
                    MessageState(
                        if (state.query.isBlank()) uiText(language, "No media files yet", "尚未找到媒體檔案") else uiText(language, "No matches", "沒有相符項目"),
                        if (state.query.isBlank()) {
                            if (favouritesActive) uiText(language, "No favorites yet. Add them with the ⋮ button.", "尚未有我的最愛。使用 ⋮ 按鈕加入歌曲。")
                            else if (activePlaylist != null) uiText(language, "This playlist is empty. Add songs with the ⋮ button.", "此播放清單是空的。使用 ⋮ 按鈕加入歌曲。")
                            else uiText(language, "No supported media was found under:\n${state.targetPath}", "在以下位置找不到支援的媒體：\n${state.targetPath}")
                        } else uiText(language, "Try a different search.", "請嘗試其他搜尋字詞。"),
                        uiText(language, "Scan again", "重新掃描"),
                        onRefresh,
                    )
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
                            if (scrolling) openRowActionsPath = null
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().inspectElement("LIBRARY_LIST", "Virtualized ordered media rows"),
            contentPadding = PaddingValues(bottom = if (currentPath == null) 2.dp else 56.dp),
                    ) {
                        itemsIndexed(
                            state.files,
                            key = { _, item -> item.id },
                            contentType = { _, item -> item.kind },
                        ) { index, item ->
                            MediaFileRow(
                                file = item,
                                isCurrent = item.path == currentPath,
                                isSelected = item.path in selected,
                                index = index,
                                itemCount = state.files.size,
                                dragEnabled = !favouritesActive && selected.isEmpty() && activePlaylist?.rule == null && state.query.isBlank() && (activePlaylist != null || state.sortMode == SortMode.CUSTOM),
                                onMove = onMoveItem,
                                onLoadThumbnail = onLoadThumbnail,
                                rowSize = preferences.libraryRowSize,
                                showThumbnails = preferences.showThumbnails,
                                showFileDetails = preferences.showFileDetails,
                                language = language,
                                actionsOpen = openRowActionsPath == item.path,
                                onPlay = { if (selected.isEmpty()) onPlay(item) else selected = if (item.path in selected) selected - item.path else selected + item.path },
                                onToggleActions = {
                                    openRowActionsPath = if (openRowActionsPath == item.path) null else item.path
                                },
                                onAddToList = {
                                    openRowActionsPath = null
                                    songListFile = item
                                },
                                onDelete = {
                                    openRowActionsPath = null
                                    deleteCandidate = item
                                    deleteStep = 1
                                },
                                moreDescription = uiText(language, "Actions", "操作"),
                            )
                            HorizontalDivider(
                                Modifier.padding(start = 26.dp + if (preferences.showThumbnails)
                                    preferences.libraryRowSize.thumbnailWidth + preferences.libraryRowSize.textSpacing else 0.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }

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

    if (selectionNameDialog) AlertDialog(onDismissRequest = { selectionNameDialog = false }, title = { Text(uiText(language, "Create playlist", "建立播放清單")) },
        text = { OutlinedTextField(selectionName, { selectionName = it.take(60) }, singleLine = true) },
        confirmButton = { TextButton(enabled = selectionName.isNotBlank(), onClick = { onCreateSelected(selectionName, selected); selected = emptyList(); selectionName = ""; selectionNameDialog = false }) { Text(uiText(language, "Create", "建立")) } },
        dismissButton = { TextButton(onClick = { selectionNameDialog = false }) { Text(uiText(language, "Cancel", "取消")) } })
    if (shareSelectedConfirm) {
        val files = state.files.filter { it.path in selected }
        val total = files.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        AlertDialog(
            onDismissRequest = { shareSelectedConfirm = false },
            title = { Text(uiText(language, "Share original media files?", "分享原始媒體檔案？")) },
            text = { Text(shareBatchMessage(language, files.size, formatBytes(total))) },
            confirmButton = { TextButton(enabled = files.isNotEmpty(), onClick = {
                shareSelectedConfirm = false; onShareSelectedFiles(files)
            }) { Text(uiText(language, "Share files", "分享檔案")) } },
            dismissButton = { TextButton(onClick = { shareSelectedConfirm = false }) { Text(uiText(language, "Cancel", "取消")) } },
        )
    }
    if (createPlaylistOpen) CreatePlaylistDialog(
        language = language,
        name = playlistName,
        onName = { playlistName = it.take(60) },
        seedPath = createSeedPath,
        keyword = seedKeyword,
        onKeyword = { seedKeyword = it },
        onDismiss = { createPlaylistOpen = false; playlistName = ""; seedKeyword = ""; createSeedPath = null },
        onCreate = {
            onCreatePlaylistAndSeed(playlistName, createSeedPath, null)
            createPlaylistOpen = false; playlistName = ""; seedKeyword = ""; createSeedPath = null
        },
        onCreateWithKeyword = {
            onCreatePlaylistAndSeed(playlistName, createSeedPath, seedKeyword.ifBlank { null })
            createPlaylistOpen = false; playlistName = ""; seedKeyword = ""; createSeedPath = null
        },
    )
    songListFile?.let { file ->
        AlertDialog(
            onDismissRequest = { songListFile = null },
            title = { Text(uiText(language, "Song list", "歌曲清單")) },
            text = {
                Column {
                    TextButton(onClick = { songListFile = null; onEditDisplay(file) }) { Text(uiText(language, "Local title & cover", "本機標題與封面")) }
                    val favourite = file.path in preferences.favouritePaths
                    TextButton(onClick = { onToggleFavourite(file.path); songListFile = null }) {
                        Icon(if (favourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null)
                        Text("  ${uiText(language, if (favourite) "Remove from Favorites" else "Add to Favorites", if (favourite) "從我的最愛移除" else "加入我的最愛")}")
                    }
                    TextButton(onClick = { songListFile = null; onShareMedia(file) }) {
                        Icon(Icons.Rounded.Share, null)
                        Text("  ${uiText(language, "Share media file", "分享媒體檔案")}")
                    }
                    TextButton(onClick = { selected = (selected + file.path).distinct(); songListFile = null }) { Text(uiText(language, "Select multiple", "選取多首")) }
                    TextButton(onClick = { songListFile = null; onCreateRule() }) { Text(uiText(language, "Rule-based playlist", "規則播放清單")) }
                    if (activePlaylist != null && activePlaylist.rule == null && file.path in activePlaylist.paths) {
                        TextButton(onClick = { onRemoveFromPlaylist(file.path); songListFile = null }) {
                            Text(uiText(language, "Remove from ${activePlaylist.name}", "從「${activePlaylist.name}」移除"))
                        }
                    }
                    preferences.playlists.filter { it.rule == null }.forEach { playlist ->
                        val added = file.path in playlist.paths
                        TextButton(
                            enabled = !added,
                            onClick = { onAddToPlaylist(playlist.id, file.path); songListFile = null },
                        ) { Text(if (added) "✓  ${playlist.name}" else playlist.name) }
                    }
                    TextButton(onClick = { songListFile = null; createSeedPath = file.path; createPlaylistOpen = true }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null)
                        Text("  ${uiText(language, "Create playlist", "建立播放清單")}")
                    }
                    TextButton(
                        onClick = { songListFile = null; deleteCandidate = file; deleteStep = 1 },
                        modifier = Modifier.inspectElement("DELETE_FILE_ACTION", "Starts three-step deletion confirmation for ${file.name}"),
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Text("  ${uiText(language, "Delete original file", "刪除原始檔案")}", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { songListFile = null }) { Text(uiText(language, "Done", "完成")) } },
        )
    }
    deleteCandidate?.let { file ->
        val title = when (deleteStep) {
            1 -> uiText(language, "Delete this file?", "要刪除此檔案嗎？")
            2 -> uiText(language, "This removes the original", "這會刪除原始檔案")
            else -> uiText(language, "Final confirmation", "最後確認")
        }
        val message = when (deleteStep) {
            1 -> file.name
            2 -> uiText(language,
                "This is not playlist removal. The media file itself will be permanently removed from Download.",
                "這不是從播放清單移除。Download 裡的媒體原始檔案將永久刪除。")
            else -> "${file.name}\n\n${file.sourcePath}\n\n${uiText(language, "There is no undo inside Greater Art.", "Greater Art 內無法復原。")}"
        }
        AlertDialog(
            modifier = Modifier.inspectElement("DELETE_CONFIRM_$deleteStep", "Deletion confirmation $deleteStep of 3 for ${file.name}"),
            onDismissRequest = { if (!deleting) { deleteCandidate = null; deleteStep = 0 } },
            title = { Text(title, color = if (deleteStep == 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
            text = { Text(message) },
            dismissButton = { TextButton(enabled = !deleting, onClick = { deleteCandidate = null; deleteStep = 0 }) { Text(uiText(language, "Cancel", "取消")) } },
            confirmButton = {
                if (deleteStep < 3) TextButton(onClick = { deleteStep += 1 }) {
                    Text(if (deleteStep == 1) uiText(language, "Continue", "繼續") else uiText(language, "I understand", "我明白"))
                } else Button(
                    enabled = !deleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    onClick = {
                        deleting = true
                        actionScope.launch {
                            val error = onDeleteFile(file)
                            deleting = false; deleteCandidate = null; deleteStep = 0
                            deleteError = error
                        }
                    },
                    modifier = Modifier.inspectElement("DELETE_FILE_FINAL_BUTTON", "Permanently deletes ${file.name}"),
                ) { Text(if (deleting) uiText(language, "Deleting…", "正在刪除…") else uiText(language, "DELETE FILE", "刪除檔案")) }
            },
        )
    }
    deleteError?.let { error ->
        AlertDialog(onDismissRequest = { deleteError = null }, title = { Text(uiText(language, "File was not deleted", "檔案未刪除")) },
            text = { Text(error) }, confirmButton = { TextButton(onClick = { deleteError = null }) { Text("OK") } })
    }
}

@Composable
private fun CreatePlaylistDialog(
    language: com.local.listentomusic.data.AppLanguage,
    name: String,
    onName: (String) -> Unit,
    seedPath: String?,
    keyword: String,
    onKeyword: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onCreateWithKeyword: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText(language, "Create playlist", "建立播放清單")) },
        text = {
            Column {
                OutlinedTextField(
                    name, onName, singleLine = true,
                    label = { Text(uiText(language, "Playlist name", "播放清單名稱")) },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    keyword, onKeyword, singleLine = true,
                    label = { Text(uiText(language, "Add all songs containing (optional)", "加入含此關鍵字的所有歌曲（選填）")) },
                    placeholder = { Text(uiText(language, "e.g. example", "例如：example")) },
                )
                if (seedPath != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        uiText(language, "Also adds the song you opened.", "同時加入你開啟的歌曲。"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = if (keyword.isBlank()) onCreate else onCreateWithKeyword,
            ) { Text(if (keyword.isBlank()) uiText(language, "Create", "建立") else uiText(language, "Create + add matches", "建立並加入")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } },
    )
}

@Composable
private fun MediaFileRow(
    file: MediaFile,
    isCurrent: Boolean,
    isSelected: Boolean,
    index: Int,
    itemCount: Int,
    dragEnabled: Boolean,
    onMove: (Int, Int) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    rowSize: LibraryRowSize,
    showThumbnails: Boolean,
    showFileDetails: Boolean,
    language: AppLanguage,
    actionsOpen: Boolean,
    onPlay: () -> Unit,
    onToggleActions: () -> Unit,
    onAddToList: () -> Unit,
    onDelete: () -> Unit,
    moreDescription: String,
) {
    var accumulatedDrag by remember(index, file.path) { mutableFloatStateOf(0f) }
    val thumbnail by produceState<Bitmap?>(null, file.path, "${file.sizeBytes}:${file.modifiedMs}:$showThumbnails:${file.coverUri}") {
        value = if (showThumbnails) onLoadThumbnail(file) else null
    }
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val rowColor by animateColorAsState(
        if (isCurrent || isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(190),
        label = "library-row-active",
    )
    val tapOffset by animateFloatAsState(
        if (pressed) 4f else 0f,
        animationSpec = androidx.compose.animation.core.tween(120),
        label = "library-row-press",
    )
    val dragModifier = if (dragEnabled) Modifier.pointerInput(index, itemCount) {
        val threshold = 54.dp.toPx()
        detectDragGesturesAfterLongPress(
            onDragStart = { accumulatedDrag = 0f },
            onDragEnd = { accumulatedDrag = 0f },
            onDragCancel = { accumulatedDrag = 0f },
            onDrag = { change, amount ->
                change.consume()
                accumulatedDrag += amount.y
                if (abs(accumulatedDrag) >= threshold) {
                    val target = if (accumulatedDrag > 0) index + 1 else index - 1
                    if (target in 0 until itemCount) onMove(index, target)
                    accumulatedDrag = 0f
                }
            },
        )
    } else Modifier
    val revealProgress by animateFloatAsState(
        targetValue = if (actionsOpen) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "library-row-actions-reveal",
    )
    val density = LocalDensity.current
    val addActionSize = DesignTokens.TouchTarget.min
        val deleteActionSize = DesignTokens.TouchTarget.min
    val actionWidth = addActionSize + deleteActionSize
    val actionWidthPx = with(density) { actionWidth.toPx() }
    Box(
        Modifier.fillMaxWidth().clip(androidx.compose.ui.graphics.RectangleShape)
            .inspectElement("LIBRARY_MEDIA_ROW", file.name),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).width(actionWidth)
                .drawWithContent content@{
                    // Keep the action underlay fixed. Only draw the portion physically
                    // uncovered by the translated foreground; no width/height animation.
                    clipRect(left = size.width * (1f - revealProgress)) {
                        this@content.drawContent()
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onAddToList,
                modifier = Modifier.size(addActionSize).inspectElement("LIBRARY_ADD_TO_LIST", "Add ${file.name} to a song list"),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    uiText(language, "Add to list", "加入列表"),
                    Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(deleteActionSize).inspectElement("LIBRARY_DELETE_INLINE", "Delete ${file.name}"),
            ) {
                Icon(Icons.Rounded.Delete, uiText(language, "Delete", "刪除"), Modifier.size(18.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .graphicsLayer { translationX = tapOffset.dp.toPx() - actionWidthPx * revealProgress }
                .background(rowColor)
                .clickable(
                    interactionSource = pressSource,
                    indication = androidx.compose.material3.ripple(),
                    onClick = { if (actionsOpen) onToggleActions() else onPlay() },
                )
                .then(if (!actionsOpen) dragModifier else Modifier)
                .then(if (rowSize == LibraryRowSize.SMALL) Modifier.heightIn(min = com.local.listentomusic.model.CompactPlayerMetrics.HEIGHT_DP.dp) else Modifier)
                .padding(horizontal = DesignTokens.Spacing.md, vertical = rowSize.verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Reserve the same 12 dp for every row so artwork and titles never jump
            // sideways when the active song changes.
            Box(Modifier.width(12.dp), contentAlignment = Alignment.CenterStart) {
                if (isCurrent) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) {
                        Box(Modifier.width(2.dp).height(18.dp).background(MaterialTheme.colorScheme.secondary))
                    }
                }
            }
            if (showThumbnails) {
                MediaThumbnail(file, thumbnail, rowSize)
                Spacer(Modifier.width(rowSize.textSpacing))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                GATitle(
                                    text = file.name.substringBeforeLast('.', file.name),
                                    variant = when (rowSize) {
                                        LibraryRowSize.SMALL -> GATitleVariant.H6
                                        LibraryRowSize.MEDIUM -> GATitleVariant.H5
                                        LibraryRowSize.LARGE -> GATitleVariant.H4
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (showFileDetails) {
                                    Spacer(Modifier.height(if (rowSize == LibraryRowSize.SMALL) DesignTokens.Spacing.xs else DesignTokens.Spacing.sm))
                                    GACaption(
                                        text = buildString {
                                            append(file.name.substringAfterLast('.', "media").uppercase())
                                            append("  \u2022  ${formatBytes(file.sizeBytes)}")
                                            if (file.durationMs > 0) append("  \u2022  ${formatDuration(file.durationMs)}")
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
            if (dragEnabled) Icon(Icons.Rounded.DragHandle, "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        GAButton(
                            variant = GAButtonVariant.ICON,
                            size = GAButtonSize.MEDIUM,
                            onClick = onToggleActions,
                            modifier = Modifier.size(DesignTokens.TouchTarget.min).inspectElement("MEDIA_MORE_BUTTON", "Actions for ${file.name}"),
                        ) {
                            Icon(Icons.Rounded.MoreVert, moreDescription)
                        }
        }
    }
}

// Hoisted so the brush is not reallocated for every row during scrolling.
private val thumbnailBrush @androidx.compose.runtime.Composable get() =
    Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primaryContainer))

@Composable
private fun MediaThumbnail(file: MediaFile, bitmap: Bitmap?, rowSize: LibraryRowSize) {
    val shape = androidx.compose.ui.graphics.RectangleShape
    val density = LocalDensity.current
    val width = if (rowSize == LibraryRowSize.SMALL) with(density) { MiniWindowMetrics.widthPx(this.density).toDp() } else rowSize.thumbnailWidth
    val height = if (rowSize == LibraryRowSize.SMALL) with(density) { MiniWindowMetrics.heightPx(this.density).toDp() } else rowSize.thumbnailHeight
    GAMediaRowThumbnail(
        file = file,
        thumbnail = bitmap,
        modifier = Modifier,
        size = if (rowSize == LibraryRowSize.SMALL) with(density) { MiniWindowMetrics.widthPx(this.density).toDp() } else rowSize.thumbnailWidth,
        cornerRadius = DesignTokens.CornerRadius.md,
    )
}

@Composable
private fun MessageState(title: String, body: String, buttonText: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}

internal fun formatDuration(milliseconds: Long): String {
    if (milliseconds <= 0L) return "--:--"
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun shareBatchMessage(language: AppLanguage, count: Int, size: String): String = when (language) {
    AppLanguage.TRADITIONAL_CHINESE -> "$count 個檔案 • $size。接收應用程式可能拒絕過大的批次。"
    AppLanguage.CANTONESE -> "$count 個檔案 • $size。一次過太多檔，對面個 App 可能唔收。"
    AppLanguage.JAPANESE -> "$count ファイル • $size。大量のファイルは受信先アプリで拒否される場合があります。"
    AppLanguage.GERMAN -> "$count Dateien • $size. Empfangende Apps können sehr große Stapel ablehnen."
    AppLanguage.FRENCH -> "$count fichiers • $size. Les applications de destination peuvent refuser un lot très volumineux."
    AppLanguage.ENGLISH -> "$count files • $size. Receiving apps may reject a very large batch."
}

private val LibraryRowSize.thumbnailWidth get() = when (this) { LibraryRowSize.SMALL -> MiniWindowMetrics.WIDTH_DP.dp; LibraryRowSize.MEDIUM -> 120.dp; LibraryRowSize.LARGE -> 148.dp }
private val LibraryRowSize.thumbnailHeight get() = when (this) { LibraryRowSize.SMALL -> MiniWindowMetrics.HEIGHT_DP.dp; LibraryRowSize.MEDIUM -> 70.dp; LibraryRowSize.LARGE -> 92.dp }
private val LibraryRowSize.verticalPadding get() = when (this) { LibraryRowSize.SMALL -> 2.75.dp; LibraryRowSize.MEDIUM -> 6.dp; LibraryRowSize.LARGE -> 8.dp }
private val LibraryRowSize.textSpacing get() = when (this) { LibraryRowSize.SMALL -> 10.dp; LibraryRowSize.MEDIUM -> 12.dp; LibraryRowSize.LARGE -> 14.dp }
private val LibraryRowSize.accentHeight get() = when (this) { LibraryRowSize.SMALL -> 34.dp; LibraryRowSize.MEDIUM -> 46.dp; LibraryRowSize.LARGE -> 58.dp }
private val LibraryRowSize.playIconSize get() = when (this) { LibraryRowSize.SMALL -> 14.dp; LibraryRowSize.MEDIUM -> 17.dp; LibraryRowSize.LARGE -> 20.dp }
private val LibraryRowSize.placeholderIconSize get() = when (this) { LibraryRowSize.SMALL -> 22.dp; LibraryRowSize.MEDIUM -> 27.dp; LibraryRowSize.LARGE -> 32.dp }
