package com.local.listentomusic.ui

import kotlin.math.ceil

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.local.listentomusic.BuildConfig
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppFont
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.LocalPlaylist
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.components.GaChromeSurface
import com.local.listentomusic.ui.components.GaDivider
import com.local.listentomusic.ui.components.GaIconAction
import com.local.listentomusic.ui.components.GaSectionHeader
import com.local.listentomusic.ui.theme.GaSpacing
import com.local.listentomusic.ui.theme.gaChromeColor

private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f)
private val seekOffsets = listOf(1000L, 2000L, 3000L, 5000L, 10000L, 20000L, 30000L, 60000L)
private val backgroundDims = listOf(0.35f, 0.50f, 0.65f, 0.80f)

private data class PlaybackCycleChoice(
    val repeatMode: Int,
    val random: Boolean,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appName: String,
    preferences: UserPreferences,
    playback: PlaybackUiState,
    onBack: () -> Unit,
    onRowSize: (LibraryRowSize) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onShowThumbnails: (Boolean) -> Unit,
    onShowFileDetails: (Boolean) -> Unit,
    onResumePlayback: (Boolean) -> Unit,
    onAutoPictureInPicture: (Boolean) -> Unit,
    onFloatingWindowMode: (FloatingWindowMode) -> Unit,
    onAppLanguage: (AppLanguage) -> Unit,
    onAppFont: (AppFont) -> Unit,
    onDeveloperMode: (Boolean) -> Unit,
    onEditableQueue: (Boolean) -> Unit,
    onImportM3u: () -> Unit,
    onExportM3u: () -> Unit,
    onBackgroundMode: (AppBackgroundMode) -> Unit,
        onBackgroundScaleMode: (BackgroundScaleMode) -> Unit,
        onChooseBackgroundImage: () -> Unit,
    onChooseBackgroundVideo: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onClearBackgroundVideo: () -> Unit,
    onBackgroundDim: (Float) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistAndSeed: (String, String?, String?) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onSharePlaylist: (LocalPlaylist) -> Unit,
    onSpeed: (Float) -> Unit,
    onPlaybackCycle: (Int, Boolean) -> Unit,
    onClearThumbnailCache: () -> Unit,
    onRescan: () -> Unit,
    onReset: () -> Unit,
    onSeekOffset: (Long) -> Unit,
    onJokeAdsEnabled: (Boolean) -> Unit,
    onShowSleepControl: (Boolean) -> Unit,
    onShowAbRepeat: (Boolean) -> Unit,
    onExtendedSearch: (Boolean) -> Unit,
    onFolderExcluded: (String, Boolean) -> Unit,
    onReplayGainEnabled: (Boolean) -> Unit,
    onBlackDiscMode: (Boolean) -> Unit,
    onPlayHistoryEnabled: (Boolean) -> Unit,
    onEqualizer: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDuplicates: () -> Unit,
) {
    val language = preferences.appLanguage
    var cacheCleared by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    var editPlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var deletePlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var resetConfirmOpen by remember { mutableStateOf(false) }
    var folderDraft by remember { mutableStateOf("") }
    val repeatModes = listOf(
        PlaybackCycleChoice(Player.REPEAT_MODE_ONE, false, uiText(language, "One", "單曲")),
        PlaybackCycleChoice(Player.REPEAT_MODE_ALL, false, uiText(language, "All", "全部")),
        PlaybackCycleChoice(Player.REPEAT_MODE_ALL, true, uiText(language, "Random", "隨機")),
        PlaybackCycleChoice(Player.REPEAT_MODE_OFF, false, uiText(language, "Off", "關閉")),
    )

    Scaffold(
        modifier = Modifier.inspectElement("SETTINGS_SCREEN", "Local preferences; no account or network"),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.inspectElement("SETTINGS_TOP_BAR", "Back button and local app version"),
                title = {
                    Column {
                        Text(uiText(language, "Settings", "設定"), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "$appName ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    GaIconAction(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = uiText(language, "Back", "返回"),
                        onClick = onBack,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = gaChromeColor()),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).inspectElement("SETTINGS_LIST", "Scrollable preference controls"), contentPadding = PaddingValues(bottom = GaSpacing.xxl)) {
            item(key = "section_lang_appearance") {
                SectionTitle(uiText(language, "Language & appearance", "語言與外觀"))
                ChoiceSetting(
                    uiText(language, "Language", "語言"),
                    uiText(language, "English is the default. Changes apply immediately.", "變更會立即套用。預設語言為英文。"),
                    AppLanguage.entries,
                    preferences.appLanguage,
                    { it.label },
                    onAppLanguage,
                )
                ChoiceSetting(
                    uiText(language, "Theme", "主題"),
                    uiText(language, "Follow Android or keep one appearance.", "跟隨 Android，或固定使用淺色／深色外觀。"),
                    defaultFirst(ThemeMode.entries, ThemeMode.DARK),
                    preferences.themeMode,
                    {
                        when (it) {
                            ThemeMode.SYSTEM -> uiText(language, "System", "系統")
                            ThemeMode.LIGHT -> uiText(language, "Light", "淺色")
                            ThemeMode.DARK -> uiText(language, "Dark", "深色")
                        }
                    },
                    onThemeMode,
                )
                ChoiceSetting(
                    uiText(language, "Text style", "文字字型"),
                    uiText(language, "Silian Rail is the final reversible font choice.", "Silian Rail 是最後一個可隨時切換的字型選項。"),
                    AppFont.entries.filterNot { it == AppFont.SILIAN_RAIL } + AppFont.SILIAN_RAIL,
                    preferences.appFont,
                    { uiText(language, it.label, when (it) {
                        AppFont.SYSTEM -> "系統"
                        AppFont.SANS_SERIF -> "無襯線"
                        AppFont.SERIF -> "襯線"
                        AppFont.MONOSPACE -> "等寬"
                        AppFont.CURSIVE -> "手寫"
                        else -> it.label
                    }) },
                    onAppFont,
                )
                ChoiceSetting(
                    uiText(language, "App background", "應用程式背景"),
                    uiText(language, "Video wallpaper follows the current track across pages. Audio uses liquid metal. Wallpaper is always muted.", "影片背景會跟隨目前曲目並跨頁播放，純音訊使用液態金屬，背景永遠靜音。"),
                    defaultFirst(AppBackgroundMode.entries, AppBackgroundMode.CURRENT_VIDEO),
                    preferences.backgroundMode,
                    {
                        when (it) {
                            AppBackgroundMode.DEFAULT -> uiText(language, "Liquid metal", "液態金屬")
                            AppBackgroundMode.CUSTOM_IMAGE -> uiText(language, "Image", "圖片")
                            AppBackgroundMode.CUSTOM_VIDEO -> uiText(language, "Silent MP4", "靜音 MP4")
                            AppBackgroundMode.CURRENT_VIDEO -> uiText(language, "Now-playing video", "播放中影片")
                        }
                    },
                    onBackgroundMode,
                                    )
                                    ChoiceSetting(
                                        uiText(language, "Background fit", "背景適配"),
                                        uiText(language, "Choose how custom images and videos fill the screen. Cut to screen size is the default.", "選擇自訂圖片與影片如何填滿螢幕。預設為裁切至螢幕大小。"),
                                        defaultFirst(BackgroundScaleMode.entries, BackgroundScaleMode.CROP),
                                        preferences.backgroundScaleMode,
                                        { uiText(language, it.label, when (it) {
                                            BackgroundScaleMode.FIT -> "完整顯示"
                                            BackgroundScaleMode.STRETCH -> "拉伸"
                                            BackgroundScaleMode.CROP -> "裁切填滿"
                                        }) },
                                        onBackgroundScaleMode,
                                    )
                                    when (preferences.backgroundMode) {
                    AppBackgroundMode.CUSTOM_IMAGE -> BackgroundFileSetting(
                        title = uiText(language, "Custom image", "自訂圖片"),
                        selected = preferences.customBackgroundImageUri != null,
                        chooseLabel = if (preferences.customBackgroundImageUri != null) {
                            uiText(language, "Change image", "更換圖片")
                        } else uiText(language, "Choose image", "選擇圖片"),
                        clearLabel = uiText(language, "Remove", "移除"),
                        onChoose = onChooseBackgroundImage,
                        onClear = onClearBackgroundImage,
                        language = language,
                    )
                    AppBackgroundMode.CUSTOM_VIDEO -> BackgroundFileSetting(
                        title = uiText(language, "Custom background video", "自訂背景影片"),
                        selected = preferences.customBackgroundVideoUri != null,
                        chooseLabel = if (preferences.customBackgroundVideoUri != null) {
                            uiText(language, "Change MP4", "更換 MP4")
                        } else uiText(language, "Choose MP4", "選擇 MP4"),
                        clearLabel = uiText(language, "Remove", "移除"),
                        onChoose = onChooseBackgroundVideo,
                        onClear = onClearBackgroundVideo,
                        language = language,
                    )
                    AppBackgroundMode.CURRENT_VIDEO -> Text(
                        uiText(language, "When the current track is a video, a muted synchronized copy appears behind the interface. Audio tracks fall back to liquid metal.", "目前曲目為影片時，介面後方會顯示同步的靜音副本；播放純音訊時則回復液態金屬背景。"),
                        modifier = Modifier.padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppBackgroundMode.DEFAULT -> Unit
                }
                if (preferences.backgroundMode != AppBackgroundMode.DEFAULT) {
                    DimSliderSetting(
                        uiText(language, "Background dimming", "背景暗度"),
                        uiText(language, "Darkens custom media so titles and controls remain readable.", "調暗自訂媒體，讓標題與控制按鈕保持清晰。"),
                        preferences.backgroundDim,
                        onBackgroundDim,
                    )
                }
                ChoiceSetting(
                    uiText(language, "Library row size", "音樂庫列大小"),
                    uiText(language, "Changes the full row and thumbnail. Small is the default.", "調整完整列與縮圖大小。預設為小。"),
                    LibraryRowSize.entries,
                    preferences.libraryRowSize,
                    {
                        when (it) {
                            LibraryRowSize.SMALL -> uiText(language, "Small", "小")
                            LibraryRowSize.MEDIUM -> uiText(language, "Medium", "中")
                            LibraryRowSize.LARGE -> uiText(language, "Large", "大")
                        }
                    },
                    onRowSize,
                )
                SwitchSetting(uiText(language, "Show thumbnails", "顯示縮圖"), uiText(language, "Turn off previews for the densest list.", "關閉預覽以顯示最緊密的清單。"), preferences.showThumbnails, onShowThumbnails)
                SwitchSetting(uiText(language, "Show file details", "顯示檔案詳情"), uiText(language, "Display format and file size below the title.", "在標題下顯示格式與檔案大小。"), preferences.showFileDetails, onShowFileDetails)
            }
            item(key = "section_playback") {
                SectionTitle(uiText(language, "Playback", "播放"))
                SwitchSetting(
                    uiText(language, "ReplayGain", "ReplayGain"),
                    uiText(language, "Use track gain tags with peak protection. Untagged files play unchanged; boosting needs a peak tag and device support.", "使用曲目增益標籤及峰值保護。沒有標籤時保持原音量，增強音量需要峰值標籤和裝置支援。"),
                    preferences.replayGainEnabled,
                    onReplayGainEnabled,
                )
                SwitchSetting(
                    uiText(language, "Black disc mode", "黑膠唱片模式"),
                    uiText(language, "Spin a black vinyl display for audio tracks. Off by default.", "音訊曲目顯示平滑旋轉的黑膠唱片，預設關閉。"),
                    preferences.blackDiscMode,
                    onBlackDiscMode,
                )
                SwitchSetting(
                    uiText(language, "Offline play history", "離線播放紀錄"),
                    uiText(language, "Off by default. When enabled, played tracks and times stay only on this device and are never included in backups.", "預設關閉。啟用後，播放曲目與時間只會留在此裝置，且永遠不會加入備份。"),
                    preferences.playHistoryEnabled,
                    onPlayHistoryEnabled,
                )
                TextButton(onClick = onEqualizer, modifier = Modifier.padding(horizontal = 16.dp)) { Text(uiText(language, "Open system equalizer", "開啟系統等化器")) }
                SwitchSetting(uiText(language, "Show sleep timer", "顯示睡眠計時器"), uiText(language, "Optional player control. Hidden by default.", "選用播放控制，預設隱藏。"), preferences.showSleepControl, onShowSleepControl)
                SwitchSetting(uiText(language, "A–B practice controls", "A–B 練習控制"), uiText(language, "Mark a section to repeat. Turning this off clears the markers.", "標記要重複的段落，關閉時會清除標記。"), preferences.showAbRepeat, onShowAbRepeat)
                SwitchSetting(uiText(language, "Extended local search", "進階本機搜尋"), uiText(language, "Search artist, album and lyrics. Builds a local cache in the background; off by default.", "搜尋歌手、專輯及歌詞，在背景建立本機索引，預設關閉。"), preferences.extendedSearch, onExtendedSearch)
                ChoiceSetting(uiText(language, "Playback speed", "播放速度"), uiText(language, "Applied immediately and remembered locally.", "立即套用並儲存在本機。"), defaultFirst(speeds, 1f), playback.speed, { "${it}×" }, onSpeed)
                ChoiceSetting(
                    uiText(language, "Repeat", "循環"),
                    uiText(language, "Repeat one remains the default after reset.", "重設後仍以單曲循環為預設。"),
                    repeatModes,
                    repeatModes.first {
                        it.random == playback.shuffleEnabled &&
                            (it.random || it.repeatMode == playback.repeatMode)
                    },
                    { it.label },
                    { onPlaybackCycle(it.repeatMode, it.random) },
                )
                ChoiceSetting(
                    uiText(language, "Jump back / forward", "快退 / 快進"),
                    uiText(language, "How far the skip buttons move playback.", "快退快進按鈕一次移動的時間。"),
                    defaultFirst(seekOffsets, 5_000L),
                    preferences.seekOffsetMs,
                    { if (it >= 60_000L) "1m" else "${it / 1000}s" },
                    onSeekOffset,
                )
                SwitchSetting(uiText(language, "Resume last position", "接續上次位置"), uiText(language, "Continue the last file where you stopped.", "從上次停止的位置繼續播放。"), preferences.resumePlayback, onResumePlayback)
                SwitchSetting(uiText(language, "Automatic floating playback", "自動浮動播放"), uiText(language, "Keep playing in the selected floating mode when leaving the app.", "離開應用程式時，以所選浮動模式繼續播放。"), preferences.autoPictureInPicture, onAutoPictureInPicture)
                ChoiceSetting(
                    uiText(language, "Floating window shape", "浮動視窗形狀"),
                    uiText(language, "Mini window is the tiniest option. Compact and Follow video use Android's resizable picture-in-picture.", "「迷你視窗」尺寸最小；「精簡」與「跟隨影片」使用 Android 可縮放子母畫面。"),
                    defaultFirst(FloatingWindowMode.entries, FloatingWindowMode.MINI_WINDOW),
                    preferences.floatingWindowMode,
                    { if (it == FloatingWindowMode.COMPACT) uiText(language, "Compact", "精簡")
                      else if (it == FloatingWindowMode.FOLLOW_VIDEO) uiText(language, "Follow video", "跟隨影片")
                      else uiText(language, "Mini window", "迷你視窗") },
                    onFloatingWindowMode,
                )
            }
            item(key = "section_playlists") {
                SectionTitle(uiText(language, "Song lists", "歌曲清單"))
                SwitchSetting(
                    uiText(language, "Editable play queue", "可編輯播放佇列"),
                    uiText(language, "Show queue editing controls on the player. Off by default to keep playback clean.", "在播放器顯示佇列編輯控制。預設關閉，保持介面簡潔。"),
                    preferences.editableQueue,
                    onEditableQueue,
                )
                Text(
                    uiText(language, "Create local playlists, then add songs with the ⋮ button in the library.", "建立本機播放清單，然後使用音樂庫中的 ⋮ 按鈕加入歌曲。"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preferences.playlists.forEach { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(uiText(language, "${playlist.paths.size} songs", "${playlist.paths.size} 首歌曲")) },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { onPlayPlaylist(playlist.id) },
                                    enabled = playlist.paths.isNotEmpty() || playlist.rule != null,
                                ) { Icon(Icons.Rounded.PlayArrow, uiText(language, "Play", "播放")) }
                                IconButton(onClick = { onSharePlaylist(playlist) }, enabled = playlist.paths.isNotEmpty() || playlist.rule != null) {
                                    Icon(Icons.Rounded.Share, uiText(language, "Share playlist", "分享播放清單"))
                                }
                                IconButton(onClick = { playlistName = playlist.name; editPlaylist = playlist }) { Icon(Icons.Rounded.Edit, uiText(language, "Rename", "重新命名")) }
                                IconButton(onClick = { deletePlaylist = playlist }) { Icon(Icons.Rounded.Delete, uiText(language, "Delete", "刪除")) }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                OutlinedButton(onClick = { playlistName = ""; createOpen = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm)) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  ${uiText(language, "Create playlist", "建立播放清單")}")
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm),
                ) {
                    OutlinedButton(onClick = onImportM3u, modifier = Modifier.weight(1f)) {
                        Text(uiText(language, "Import M3U", "匯入 M3U"))
                    }
                    OutlinedButton(onClick = onExportM3u, modifier = Modifier.weight(1f)) {
                        Text(uiText(language, "Export list", "匯出清單"))
                    }
                }
            }
            item(key = "section_library") {
                SectionTitle(uiText(language, "Library & cache", "音樂庫與快取"))
                TextButton(onClick = onDuplicates, modifier = Modifier.padding(horizontal = 16.dp)) { Text(uiText(language, "Find duplicate files", "尋找重複檔案")) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm)) {
                    OutlinedButton(onClick = onBackup, modifier = Modifier.weight(1f)) { Text(uiText(language, "Back up settings", "備份設定")) }
                    OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) { Text(uiText(language, "Restore backup", "還原備份")) }
                }
                Text(uiText(language, "Excluded Download folders", "排除的 Download 資料夾"), Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
                OutlinedTextField(folderDraft, { folderDraft = it }, label = { Text(uiText(language, "Folder / subfolder", "資料夾 / 子資料夾")) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
                TextButton(onClick = {
                    val path = folderDraft.trim().replace('\\', '/').trim('/')
                    if (path.isNotBlank() && path.split('/').none { it == ".." || it == "." } && ':' !in path) {
                        onFolderExcluded(path, true); folderDraft = ""
                    }
                }, enabled = folderDraft.isNotBlank(), modifier = Modifier.padding(horizontal = 16.dp)) { Text(uiText(language, "Exclude folder", "排除此資料夾")) }
                preferences.excludedFolders.forEach { folder ->
                    SwitchSetting(folder, uiText(language, "Turn off to include again. Files remain untouched.", "關閉後重新加入，不會更改檔案。"), true, { onFolderExcluded(folder, false) })
                }
                ActionCard(Icons.Rounded.Cached, uiText(language, "Scan Download again", "重新掃描 Download"), uiText(language, "Refresh the recursive local media index.", "重新整理遞迴本機媒體索引。"), uiText(language, "Rescan", "重新掃描")) { onRescan(); onBack() }
                ActionCard(
                    Icons.Rounded.DeleteSweep,
                    uiText(language, "Thumbnail cache", "縮圖快取"),
                    if (cacheCleared) uiText(language, "Cache cleared. Previews will be recreated when needed.", "快取已清除，預覽會在需要時重新建立。") else uiText(language, "Remove generated previews without touching media files.", "移除產生的預覽，不會動到媒體檔案。"),
                    uiText(language, "Clear cache", "清除快取"),
                ) { onClearThumbnailCache(); cacheCleared = true }

            }
            item(key = "section_privacy") {
                SectionTitle(uiText(language, "Privacy", "私隱"))
                GaChromeSurface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm),
                    contentPadding = PaddingValues(GaSpacing.lg),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(GaSpacing.md)) {
                        Icon(Icons.Rounded.PrivacyTip, null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text(
                                uiText(language, "Offline by design", "離線設計"),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(GaSpacing.xs))
                            Text(
                                uiText(language, "No Internet permission, ads, analytics, account, telemetry, or cloud library. Everything stays on this device.", "沒有網絡權限、廣告、分析、帳戶、遙測或雲端音樂庫。所有資料都留在本機。"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SwitchSetting(
                    uiText(language, "Developer diagnostics", "開發者診斷"),
                    uiText(language, "Adds a local DEV panel with live screen, player, queue and permission details. Nothing is transmitted.", "加入本機 DEV 面板，顯示畫面、播放器、佇列及權限資料；不會傳送任何內容。"),
                    preferences.developerMode,
                    onDeveloperMode,
                )
                Spacer(Modifier.height(12.dp))
                SwitchSetting(
                                    uiText(language, "Toggle Ads On", "開啟廣告"),
                                    uiText(language, "Optional parody: loud colours, wobbling buttons and a five-second skip. Buttons open a Rickroll in your browser.", "可選惡搞：高飽和配色、晃動按鈕與五秒跳過。按鈕會在瀏覽器開啟 Rickroll。"),
                                    preferences.jokeAdsEnabled,
                                    onJokeAdsEnabled,
                                )
                OutlinedButton(
                    onClick = { resetConfirmOpen = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.RestartAlt, null)
                    Text("  ${uiText(language, "Reset app settings", "重設應用程式設定")}")
                }
                Text(uiText(language, "Your playlists, library order, and media files are not changed.", "播放清單、音樂庫排序與媒體檔案不會被更改。"), modifier = Modifier.fillMaxWidth().padding(top = GaSpacing.sm), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
                        }
    }

    if (createOpen) NameDialog(language, null, playlistName, { playlistName = it.take(60) }, { createOpen = false }, { onCreatePlaylistAndSeed(playlistName, null, null); createOpen = false })
    editPlaylist?.let { playlist ->
        NameDialog(language, playlist, playlistName, { playlistName = it.take(60) }, { editPlaylist = null }, { onRenamePlaylist(playlist.id, playlistName); editPlaylist = null })
    }
    deletePlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletePlaylist = null },
            title = { Text(uiText(language, "Delete playlist?", "刪除播放清單？")) },
            text = { Text(uiText(language, "${playlist.name} will be removed. Media files stay untouched.", "將移除「${playlist.name}」，媒體檔案不會被刪除。")) },
            confirmButton = { TextButton(onClick = { onDeletePlaylist(playlist.id); deletePlaylist = null }) { Text(uiText(language, "Delete", "刪除")) } },
            dismissButton = { TextButton(onClick = { deletePlaylist = null }) { Text(uiText(language, "Cancel", "取消")) } },
        )
    }
    if (resetConfirmOpen) {
        AlertDialog(
            onDismissRequest = { resetConfirmOpen = false },
            icon = { Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(uiText(language, "Reset every setting?", "重設所有設定？"), color = MaterialTheme.colorScheme.error) },
            text = { Text(uiText(language, "This restores Mini window, dark theme, Repeat One and every other preference. Playlists and media files stay safe.", "這會還原迷你視窗、深色主題、單曲循環及所有偏好。播放清單與媒體檔案不受影響。")) },
            confirmButton = {
                Button(
                    onClick = { onReset(); cacheCleared = false; resetConfirmOpen = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(uiText(language, "Yes, reset settings", "確認重設設定")) }
            },
            dismissButton = { TextButton(onClick = { resetConfirmOpen = false }) { Text(uiText(language, "Keep settings", "保留設定")) } },
        )
    }
}

@Composable
private fun NameDialog(language: AppLanguage, playlist: LocalPlaylist?, name: String, onName: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (playlist == null) uiText(language, "Create playlist", "建立播放清單") else uiText(language, "Rename playlist", "重新命名播放清單")) },
        text = { OutlinedTextField(name, onName, singleLine = true, label = { Text(uiText(language, "Playlist name", "播放清單名稱")) }) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = onConfirm) { Text(uiText(language, "Save", "儲存")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } },
    )
}

@Composable private fun SectionTitle(text: String) = GaSectionHeader(text)

@Composable
private fun SwitchSetting(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        modifier = Modifier.inspectElement("SETTING_SWITCH", title),
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { Switch(checked, onChecked, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    GaDivider()
}

@Composable
private fun BackgroundFileSetting(
    title: String,
    selected: Boolean,
    chooseLabel: String,
    clearLabel: String,
    onChoose: () -> Unit,
    onClear: () -> Unit,
    language: AppLanguage,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
            if (selected) uiText(language, "File selected — access is saved locally.", "已選擇檔案，存取權限只儲存在本機。")
            else uiText(language, "No file selected; the default background is used.", "尚未選擇檔案，將使用預設背景。"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.padding(top = GaSpacing.sm), horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm)) {
            OutlinedButton(onClick = onChoose) { Text(chooseLabel) }
            if (selected) TextButton(onClick = onClear) { Text(clearLabel) }
        }
    }
    GaDivider()
}

@Composable
private fun <T> ChoiceSetting(title: String, description: String, values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().inspectElement("SETTING_CHOICE", title).padding(horizontal = GaSpacing.lg, vertical = GaSpacing.md)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = GaSpacing.sm), horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm)) {
            values.forEach { value -> FilterChip(value == selected, { onSelect(value) }, { Text(label(value)) },
                modifier = Modifier.inspectElement("SETTING_OPTION", "$title: ${label(value)}")) }
        }
    }
    GaDivider()
}

@Composable
private fun DimSliderSetting(title: String, description: String, value: Float, onValue: (Float) -> Unit) {
    var inputOpen by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().inspectElement("SETTING_SLIDER", title).padding(horizontal = GaSpacing.lg, vertical = GaSpacing.md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${(value * 100).toInt()}%",
                modifier = Modifier.padding(start = GaSpacing.md, top = GaSpacing.micro).clickable { inputText = "${(value * 100).toInt()}"; inputOpen = true },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = 0.25f..0.85f,
            modifier = Modifier.fillMaxWidth().padding(top = GaSpacing.xs),
        )
    }
    if (inputOpen) AlertDialog(
        onDismissRequest = { inputOpen = false },
        title = { Text("Background dimming %") },
        text = { OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text("Enter % (25-85)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        ) },
        confirmButton = { TextButton(onClick = {
            val pct = inputText.toFloatOrNull() ?: value * 100
            val rounded = kotlin.math.ceil(pct.coerceIn(25f, 85f) / 5f) * 5f / 100f
            onValue(rounded.coerceIn(0.25f, 0.85f))
            inputOpen = false
        }) { Text("Set") } },
    )
    GaDivider()
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, button: String, onClick: () -> Unit) {
    GaChromeSurface(
        modifier = Modifier.fillMaxWidth().inspectElement("SETTING_ACTION", title)
            .padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm),
        contentPadding = PaddingValues(GaSpacing.lg),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GaSpacing.md)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick, Modifier.padding(top = GaSpacing.sm)) { Text(button) }
            }
        }
    }
}
