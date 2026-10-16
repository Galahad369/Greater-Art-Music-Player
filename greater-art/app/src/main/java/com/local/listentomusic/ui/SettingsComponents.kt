package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.*
import com.local.listentomusic.ui.components.GaDivider
import com.local.listentomusic.ui.theme.GaSpacing
import com.local.listentomusic.ui.theme.GaControl

internal val LocalSettingsLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

internal fun settingsSizeLabel(language: AppLanguage, size: LibraryRowSize): String =
    uiText(language, size.label, when (size) {
        LibraryRowSize.SMALL -> "小"; LibraryRowSize.MEDIUM -> "中"; LibraryRowSize.LARGE -> "大"
    })

/** Stable keys survive language and configuration changes; translated titles are not identity. */
internal enum class SettingsSection(val icon: ImageVector) {
    APPEARANCE(Icons.Rounded.Palette), BACKGROUND(Icons.Rounded.Wallpaper),
    PLAYBACK(Icons.Rounded.PlayCircle), LIBRARY(Icons.Rounded.LibraryMusic),
    FOLDERS(Icons.Rounded.FolderOpen), DATA(Icons.Rounded.PrivacyTip), DEVELOPER(Icons.Rounded.Code);

    fun title(language: AppLanguage): String = when (this) {
        APPEARANCE -> uiText(language, "Language & appearance", "語言與外觀")
        BACKGROUND -> uiText(language, "Background", "背景")
        PLAYBACK -> uiText(language, "Playback", "播放")
        LIBRARY -> uiText(language, "Library & lists", "音樂庫與清單")
        FOLDERS -> when (language) {
            AppLanguage.ENGLISH -> "Media folders"
            AppLanguage.TRADITIONAL_CHINESE, AppLanguage.CANTONESE -> "媒體資料夾"
            AppLanguage.JAPANESE -> "メディアフォルダー"
            AppLanguage.GERMAN -> "Medienordner"
            AppLanguage.FRENCH -> "Dossiers multimédias"
        }
        DATA -> uiText(language, "Privacy & data", "私隱與資料")
        DEVELOPER -> uiText(language, "Developer", "開發者")
    }
}

@Composable
internal fun SettingsCategoryRow(section: SettingsSection, preferences: UserPreferences,
    playback: PlaybackUiState, onClick: () -> Unit) {
    val language = preferences.appLanguage
    val summary = when (section) {
        SettingsSection.APPEARANCE -> "${preferences.appLanguage.label} · ${uiText(language, preferences.themeMode.label,
            when (preferences.themeMode) { ThemeMode.SYSTEM -> "系統"; ThemeMode.LIGHT -> "淺色"; ThemeMode.DARK -> "深色" })}"
        SettingsSection.BACKGROUND -> when (preferences.backgroundMode) {
            AppBackgroundMode.DEFAULT -> uiText(language, "Liquid metal", "液態金屬")
            AppBackgroundMode.CURRENT_VIDEO -> uiText(language, "Now-playing video", "播放中影片")
            AppBackgroundMode.CUSTOM_IMAGE -> uiText(language, "Custom image", "自訂圖片")
            AppBackgroundMode.CUSTOM_VIDEO -> uiText(language, "Custom background video", "自訂背景影片")
            AppBackgroundMode.AMBIENT -> classicAmbientLabel(language)
            AppBackgroundMode.ADAPTIVE_DYNAMIC -> adaptiveLabel(language)
        }
        SettingsSection.PLAYBACK -> "${speedLabel(playback.speed)} · ${floatingModeLabel(language, preferences.floatingWindowMode)}"
        SettingsSection.LIBRARY -> uiText(language, "Create playlist", "建立播放清單") + " · " +
            uiText(language, "Library row size", "音樂庫列大小")
        SettingsSection.FOLDERS -> if (preferences.scopedMediaTreeUri == null)
            uiText(language, "No folder selected", "尚未選擇資料夾") else scopedSelectedFolderText(language)
        SettingsSection.DATA -> uiText(language, "Offline by design", "離線設計")
        SettingsSection.DEVELOPER -> uiText(language, "Developer diagnostics", "開發者診斷") + " · " +
            if (preferences.developerMode) uiText(language, "On", "開啟") else uiText(language, "Off", "關閉")
    }
    ListItem(
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(role = Role.Button, onClick = onClick)
            .inspectElement("SETTINGS_CATEGORY_${section.name}", section.title(language)),
        headlineContent = { Text(section.title(language), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(section.icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
    GaDivider(Modifier.padding(start = 56.dp, end = GaSpacing.lg))
}

/** One readable current value per row, not a permanently expanded wall of choice chips. */
@Composable
internal fun <T> ChoiceSetting(title: String, description: String, values: List<T>, selected: T,
    label: (T) -> String, onSelect: (T) -> Unit, swatch: (T) -> Color? = { null },
    language: AppLanguage = LocalSettingsLanguage.current) {
    var open by rememberSaveable { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { open = true }
            .semantics { stateDescription = label(selected) }.inspectElement("SETTING_CHOICE", title),
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(GaSpacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    swatch(selected)?.let { Box(Modifier.size(14.dp).background(it, CircleShape)) }
                    Text(label(selected), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    GaDivider(Modifier.padding(horizontal = GaSpacing.lg))
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(values) { value ->
                    ListItem(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .selectable(value == selected, role = Role.RadioButton, onClick = {
                                open = false
                                onSelect(value)
                            }).inspectElement("SETTING_OPTION", "$title: ${label(value)}"),
                        headlineContent = { Text(label(value), maxLines = 3, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { RadioButton(value == selected, onClick = null) },
                        trailingContent = swatch(value)?.let { colour ->
                            { Box(Modifier.size(20.dp).background(colour, CircleShape)) }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(uiText(language, "Cancel", "取消")) } },
    )
}

@Composable
internal fun SettingsPlaylistRow(playlist: LocalPlaylist, language: AppLanguage,
    onPlay: () -> Unit, onShare: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val playable = playlist.paths.isNotEmpty() || playlist.rule != null || playlist.stackKeyword.isNotBlank()
    Column(Modifier.fillMaxWidth().inspectElement("SETTINGS_PLAYLIST", playlist.name)) {
        ListItem(
            headlineContent = { Text(playlist.name, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                val count = uiText(language, "${playlist.paths.size} songs", "${playlist.paths.size} 首歌曲")
                Text(if (playlist.stackKeyword.isBlank()) count
                    else "$count · ${uiText(language, "Stack", "疊播")}: ${playlist.stackKeyword}",
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg),
            horizontalArrangement = Arrangement.End) {
            IconButton(onPlay, modifier = Modifier.size(GaControl.touchTarget), enabled = playable) {
                Icon(Icons.Rounded.PlayArrow, "${uiText(language, "Play", "播放")}: ${playlist.name}")
            }
            IconButton(onShare, modifier = Modifier.size(GaControl.touchTarget), enabled = playable) {
                Icon(Icons.Rounded.Share, "${uiText(language, "Share playlist", "分享播放清單")}: ${playlist.name}")
            }
            IconButton(onRename, modifier = Modifier.size(GaControl.touchTarget)) {
                Icon(Icons.Rounded.Edit, "${uiText(language, "Rename", "重新命名")}: ${playlist.name}")
            }
            IconButton(onDelete, modifier = Modifier.size(GaControl.touchTarget)) {
                Icon(Icons.Rounded.Delete, "${uiText(language, "Delete", "刪除")}: ${playlist.name}")
            }
        }
        GaDivider(Modifier.padding(horizontal = GaSpacing.lg))
    }
}
