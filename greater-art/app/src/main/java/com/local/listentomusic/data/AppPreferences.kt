package com.local.listentomusic.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import com.local.listentomusic.model.SortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "listen_to_music_preferences")

const val FAVOURITES_PLAYLIST_ID = "__greater_art_favourites__"

data class UserPreferences(
    val graphOptions: com.local.listentomusic.graph.GraphOptions = com.local.listentomusic.graph.GraphOptions(),
    val sortMode: SortMode = SortMode.DATE_DESC,
    val customOrder: List<String> = emptyList(),
    val lastPath: String? = null,
    val lastPositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_ONE,
    val libraryRowSize: LibraryRowSize = LibraryRowSize.SMALL,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val showThumbnails: Boolean = true,
    val showFileDetails: Boolean = false,
    val resumePlayback: Boolean = true,
    val autoPictureInPicture: Boolean = true,
    val floatingWindowMode: FloatingWindowMode = FloatingWindowMode.MINI_WINDOW,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val backgroundMode: AppBackgroundMode = AppBackgroundMode.CURRENT_VIDEO,
    val customBackgroundImageUri: String? = null,
    val customBackgroundVideoUri: String? = null,
    val backgroundDim: Float = 0.35f,
        val backgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.CROP,
        val playlists: List<LocalPlaylist> = emptyList(),
    val activePlaylistId: String? = null,
    val seekOffsetMs: Long = 5_000L,
    val appFont: AppFont = AppFont.SYSTEM,
    val developerMode: Boolean = false,
    val editableQueue: Boolean = false,
    val silianRail: Boolean = false,
    val showSleepControl: Boolean = false,
    val showAbRepeat: Boolean = false,
    val extendedSearch: Boolean = false,
    val localOverrides: Map<String, com.local.listentomusic.model.LocalOverride> = emptyMap(),
    val replayGainEnabled: Boolean = false,
    val blackDiscMode: Boolean = false,
    val playHistoryEnabled: Boolean = false,
    val favouritePaths: List<String> = emptyList(),
    val excludedFolders: List<String> = emptyList(),
    val jokeAdsEnabled: Boolean = false,
)

enum class LibraryRowSize(val label: String) { SMALL("Small"), MEDIUM("Medium"), LARGE("Large") }
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class FloatingWindowMode { COMPACT, FOLLOW_VIDEO, MINI_WINDOW }
enum class AppLanguage(val label: String) {
    ENGLISH("English"), TRADITIONAL_CHINESE("繁體中文"), JAPANESE("日本語"),
    GERMAN("Deutsch"), FRENCH("Français"), CANTONESE("廣東話"),
}
enum class AppBackgroundMode { DEFAULT, CUSTOM_IMAGE, CUSTOM_VIDEO, CURRENT_VIDEO }
enum class BackgroundScaleMode(val label: String) {
    FIT("Fit"),
    STRETCH("Stretch"),
    CROP("Cut to screen size"),
}
enum class AppFont(val label: String) {
    SYSTEM("System"), SANS_SERIF("Sans serif"), SERIF("Serif"), MONOSPACE("Monospace"),
    CURSIVE("Cursive"), INTER("Inter"), NUNITO("Nunito"), OSWALD("Oswald"),
    SILIAN_RAIL("Silian Rail"), PLAYFAIR_DISPLAY("Playfair Display"), ROBOTO_SLAB("Roboto Slab"), SOURCE_CODE_PRO("Source Code Pro")
}

data class LocalPlaylist(
    val id: String,
    val name: String,
    val paths: List<String>,
    val rule: com.local.listentomusic.model.PlaylistRule? = null,
)

data class PlayHistoryEntry(val path: String, val playedAtEpochMs: Long)

class AppPreferences(private val context: Context) {
    private object Keys {
        val graphOptions = stringPreferencesKey("graph_options_v1")
        val sortMode = stringPreferencesKey("sort_mode")
        val customOrder = stringPreferencesKey("custom_order")
        val lastPath = stringPreferencesKey("last_path")
        val lastPosition = longPreferencesKey("last_position_ms")
        val speed = floatPreferencesKey("playback_speed")
        val repeatMode = longPreferencesKey("repeat_mode")
        val libraryRowSize = stringPreferencesKey("library_row_size")
        val themeMode = stringPreferencesKey("theme_mode")
        val showThumbnails = booleanPreferencesKey("show_thumbnails")
        val showFileDetails = booleanPreferencesKey("show_file_details")
        val preloadThumbnails = booleanPreferencesKey("preload_thumbnails")
        val resumePlayback = booleanPreferencesKey("resume_playback")
        val autoPictureInPicture = booleanPreferencesKey("auto_picture_in_picture")
        val floatingWindowMode = stringPreferencesKey("floating_window_mode")
        // Distinguishes a deliberate Compact choice from the pre-1.6.1 Compact default.
        val floatingWindowDefaultV2 = booleanPreferencesKey("floating_window_default_v2")
        val appLanguage = stringPreferencesKey("app_language")
        val backgroundMode = stringPreferencesKey("background_mode")
        val customBackgroundImageUri = stringPreferencesKey("custom_background_image_uri")
        val customBackgroundVideoUri = stringPreferencesKey("custom_background_video_uri")
        val backgroundDim = floatPreferencesKey("background_dim")
                val backgroundScaleMode = stringPreferencesKey("background_scale_mode")
                val playlists = stringPreferencesKey("playlists")
        val activePlaylistId = stringPreferencesKey("active_playlist_id")
        val seekOffsetMs = longPreferencesKey("seek_offset_ms")
        val appFont = stringPreferencesKey("app_font")
        val developerMode = booleanPreferencesKey("developer_mode")
        val editableQueue = booleanPreferencesKey("editable_queue")
        val silianRail = booleanPreferencesKey("silian_rail")
        val showSleepControl = booleanPreferencesKey("show_sleep_control")
        val showAbRepeat = booleanPreferencesKey("show_ab_repeat")
        val extendedSearch = booleanPreferencesKey("extended_search")
        val localOverrides = stringPreferencesKey("local_overrides")
        val replayGainEnabled = booleanPreferencesKey("replay_gain_enabled")
        val blackDiscMode = booleanPreferencesKey("black_disc_mode")
        val playHistoryEnabled = booleanPreferencesKey("play_history_enabled")
        val playHistory = stringPreferencesKey("play_history_v1")
        val favouritePaths = stringPreferencesKey("favourite_paths_v1")
        val excludedFolders = stringPreferencesKey("excluded_folders")
        val jokeAdsEnabled = booleanPreferencesKey("joke_ads_enabled")
    }

    val values: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val savedFont = enumValueOrDefault(prefs[Keys.appFont], AppFont.SYSTEM)
        // v1.7.5 briefly stored Silian Rail in a second Boolean. Migrate that
        // state into the font picker so typography and identity cannot disagree.
        val effectiveFont = if (prefs[Keys.silianRail] == true) AppFont.SILIAN_RAIL else savedFont
        UserPreferences(
            graphOptions = com.local.listentomusic.graph.GraphOptions.decode(prefs[Keys.graphOptions]),
            sortMode = runCatching {
                SortMode.valueOf(prefs[Keys.sortMode] ?: SortMode.NAME_ASC.name)
            }.getOrDefault(SortMode.NAME_ASC),
            customOrder = decodeOrder(prefs[Keys.customOrder].orEmpty()),
            lastPath = prefs[Keys.lastPath],
            lastPositionMs = prefs[Keys.lastPosition] ?: 0L,
            playbackSpeed = prefs[Keys.speed] ?: 1f,
            repeatMode = (prefs[Keys.repeatMode] ?: Player.REPEAT_MODE_ONE.toLong()).toInt(),
            libraryRowSize = enumValueOrDefault(
                prefs[Keys.libraryRowSize],
                LibraryRowSize.SMALL,
            ),
            themeMode = enumValueOrDefault(prefs[Keys.themeMode], ThemeMode.DARK),
            showThumbnails = prefs[Keys.showThumbnails] ?: true,
            showFileDetails = prefs[Keys.showFileDetails] ?: false,
            resumePlayback = prefs[Keys.resumePlayback] ?: true,
            autoPictureInPicture = prefs[Keys.autoPictureInPicture] ?: true,
            floatingWindowMode = enumValueOrDefault(
                prefs[Keys.floatingWindowMode],
                FloatingWindowMode.MINI_WINDOW,
            ).let { savedMode ->
                // Existing installs inherited Compact without choosing it. Migrate that
                // old default once; future explicit Compact selections remain respected.
                if (prefs[Keys.floatingWindowDefaultV2] != true &&
                    savedMode == FloatingWindowMode.COMPACT
                ) FloatingWindowMode.MINI_WINDOW else savedMode
            },
            appLanguage = enumValueOrDefault(prefs[Keys.appLanguage], AppLanguage.ENGLISH),
            backgroundMode = enumValueOrDefault(
                prefs[Keys.backgroundMode],
                AppBackgroundMode.CURRENT_VIDEO,
            ),
            customBackgroundImageUri = prefs[Keys.customBackgroundImageUri],
            customBackgroundVideoUri = prefs[Keys.customBackgroundVideoUri],
            backgroundDim = (prefs[Keys.backgroundDim] ?: 0.35f).coerceIn(0.25f, 0.85f),
                        backgroundScaleMode = enumValueOrDefault(
                            prefs[Keys.backgroundScaleMode],
                            BackgroundScaleMode.CROP,
                        ),
                        playlists = decodePlaylists(prefs[Keys.playlists].orEmpty()),
            activePlaylistId = prefs[Keys.activePlaylistId],
            seekOffsetMs = prefs[Keys.seekOffsetMs] ?: 5_000L,
            appFont = effectiveFont,
            developerMode = prefs[Keys.developerMode] ?: false,
            editableQueue = prefs[Keys.editableQueue] ?: false,
            silianRail = effectiveFont == AppFont.SILIAN_RAIL,
            showSleepControl = prefs[Keys.showSleepControl] ?: false,
            showAbRepeat = prefs[Keys.showAbRepeat] ?: false,
            extendedSearch = prefs[Keys.extendedSearch] ?: false,
            localOverrides = decodeOverrides(prefs[Keys.localOverrides].orEmpty()),
            replayGainEnabled = prefs[Keys.replayGainEnabled] ?: false,
            blackDiscMode = prefs[Keys.blackDiscMode] ?: false,
            playHistoryEnabled = prefs[Keys.playHistoryEnabled] ?: false,
            favouritePaths = decodeOrder(prefs[Keys.favouritePaths].orEmpty()),
            excludedFolders = decodeOrder(prefs[Keys.excludedFolders].orEmpty()),
            jokeAdsEnabled = prefs[Keys.jokeAdsEnabled] ?: false,
        )
    }

    suspend fun current(): UserPreferences = values.first()
    val playHistory: Flow<List<PlayHistoryEntry>> = context.dataStore.data.map { prefs ->
        decodePlayHistory(prefs[Keys.playHistory].orEmpty())
    }
    suspend fun setGraphOptions(value: com.local.listentomusic.graph.GraphOptions) = edit { it[Keys.graphOptions] = value.encode() }

    // Explicit portable preference allowlist: no last-played data, private background
    // document grants, diagnostics or joke toggle cross a backup boundary.
    private val backupStrings = listOf(Keys.graphOptions, Keys.sortMode, Keys.customOrder, Keys.libraryRowSize,
        Keys.themeMode, Keys.floatingWindowMode, Keys.appLanguage, Keys.appFont,
        Keys.playlists, Keys.activePlaylistId, Keys.excludedFolders, Keys.favouritePaths)
    private val backupBooleans = listOf(Keys.showThumbnails, Keys.showFileDetails,
        Keys.resumePlayback, Keys.autoPictureInPicture,
        Keys.editableQueue, Keys.showSleepControl, Keys.showAbRepeat, Keys.extendedSearch,
        Keys.replayGainEnabled, Keys.blackDiscMode)

    suspend fun exportBackup(): String {
        val prefs = context.dataStore.data.first()
        val json = org.json.JSONObject().put("format", "GreaterArtSettings").put("version", 1)
        backupStrings.forEach { key -> prefs[key]?.let { json.put(key.name, it) } }
        backupBooleans.forEach { key -> prefs[key]?.let { json.put(key.name, it) } }
        json.put(Keys.speed.name, prefs[Keys.speed] ?: 1f)
        json.put(Keys.repeatMode.name, prefs[Keys.repeatMode] ?: 1L)
        json.put(Keys.seekOffsetMs.name, prefs[Keys.seekOffsetMs] ?: 5_000L)
        return json.toString(2)
    }

    suspend fun restoreBackup(text: String) {
        require(text.length <= 5_000_000) { "Backup is too large" }
        val json = org.json.JSONObject(text)
        require(json.optString("format") == "GreaterArtSettings" && json.optInt("version") == 1) { "Not a supported Greater Art backup" }
        val speed = json.optDouble(Keys.speed.name, 1.0)
        require(speed.isFinite() && speed in 0.25..3.0)
        context.dataStore.edit { prefs ->
            backupStrings.forEach { key ->
                if (json.has(key.name)) {
                    val value = json.getString(key.name)
                    require(value.length <= 4_000_000)
                    prefs[key] = value
                } else prefs.remove(key)
            }
            backupBooleans.forEach { key ->
                if (json.has(key.name)) prefs[key] = json.getBoolean(key.name) else prefs.remove(key)
            }
            prefs[Keys.speed] = speed.toFloat()
            prefs[Keys.repeatMode] = json.optLong(Keys.repeatMode.name, 1).coerceIn(0, 2)
            prefs[Keys.seekOffsetMs] = json.optLong(Keys.seekOffsetMs.name, 5_000).coerceIn(1_000, 60_000)
            prefs[Keys.floatingWindowDefaultV2] = true
            // Bulk thumbnail preload was retired; discard legacy backup/device state.
            prefs.remove(Keys.preloadThumbnails)
            prefs.remove(Keys.silianRail)
        }
    }

    suspend fun setSortMode(mode: SortMode) {
        context.dataStore.edit { it[Keys.sortMode] = mode.name }
    }

    suspend fun setCustomOrder(paths: List<String>) {
        context.dataStore.edit {
            it[Keys.customOrder] = StoredPathListCodec.encode(paths)
            it[Keys.sortMode] = SortMode.CUSTOM.name
        }
    }

    suspend fun savePlayback(path: String?, positionMs: Long, speed: Float, repeatMode: Int) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.lastPath) else prefs[Keys.lastPath] = path
            prefs[Keys.lastPosition] = positionMs.coerceAtLeast(0L)
            prefs[Keys.speed] = speed
            prefs[Keys.repeatMode] = repeatMode.toLong()
        }
    }

    suspend fun setLibraryRowSize(value: LibraryRowSize) = edit { it[Keys.libraryRowSize] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.themeMode] = value.name }
    suspend fun setShowThumbnails(value: Boolean) = edit { it[Keys.showThumbnails] = value }
    suspend fun setShowFileDetails(value: Boolean) = edit { it[Keys.showFileDetails] = value }
    suspend fun setResumePlayback(value: Boolean) = edit { it[Keys.resumePlayback] = value }
    suspend fun setAutoPictureInPicture(value: Boolean) = edit { it[Keys.autoPictureInPicture] = value }
    suspend fun setFloatingWindowMode(value: FloatingWindowMode) = edit {
        it[Keys.floatingWindowMode] = value.name
        it[Keys.floatingWindowDefaultV2] = true
    }
    suspend fun setAppLanguage(value: AppLanguage) = edit { it[Keys.appLanguage] = value.name }
    suspend fun setBackgroundMode(value: AppBackgroundMode) =
        edit { it[Keys.backgroundMode] = value.name }
    suspend fun setCustomBackgroundImageUri(value: String?) = edit { prefs ->
        if (value == null) prefs.remove(Keys.customBackgroundImageUri)
        else prefs[Keys.customBackgroundImageUri] = value
    }
    suspend fun setCustomBackgroundVideoUri(value: String?) = edit { prefs ->
        if (value == null) prefs.remove(Keys.customBackgroundVideoUri)
        else prefs[Keys.customBackgroundVideoUri] = value
    }
    suspend fun setBackgroundDim(value: Float) =
            edit { it[Keys.backgroundDim] = value.coerceIn(0.25f, 0.85f) }
        suspend fun setBackgroundScaleMode(value: BackgroundScaleMode) =
            edit { it[Keys.backgroundScaleMode] = value.name }
        suspend fun setSeekOffsetMs(value: Long) = edit { it[Keys.seekOffsetMs] = value }
    suspend fun setAppFont(value: AppFont) = edit {
        it[Keys.appFont] = value.name
        // One source of truth: this old key is migration-only.
        it.remove(Keys.silianRail)
    }
    suspend fun setDeveloperMode(value: Boolean) = edit { it[Keys.developerMode] = value }
    suspend fun setEditableQueue(value: Boolean) = edit { it[Keys.editableQueue] = value }
    suspend fun setShowSleepControl(value: Boolean) = edit { it[Keys.showSleepControl] = value }
    suspend fun setShowAbRepeat(value: Boolean) = edit { it[Keys.showAbRepeat] = value }
    suspend fun setExtendedSearch(value: Boolean) = edit { it[Keys.extendedSearch] = value }
    suspend fun setLocalOverride(path: String, value: com.local.listentomusic.model.LocalOverride?) = edit { prefs ->
        val entries = decodeOverrides(prefs[Keys.localOverrides].orEmpty()).toMutableMap()
        if (value == null || (value.title.isBlank() && value.coverUri.isBlank())) entries.remove(path) else entries[path] = value
        val json = org.json.JSONObject()
        entries.forEach { (key, item) -> json.put(key, org.json.JSONObject().put("title", item.title).put("cover", item.coverUri)) }
        prefs[Keys.localOverrides] = json.toString()
    }

    suspend fun createRulePlaylist(name: String, rule: com.local.listentomusic.model.PlaylistRule): String {
        val id = UUID.randomUUID().toString()
        updatePlaylists { it + LocalPlaylist(id, name.trim(), emptyList(), rule) }
        return id
    }

    suspend fun restorePlaylist(playlist: LocalPlaylist) = updatePlaylists { current ->
        if (current.any { it.id == playlist.id }) current else current + playlist
    }
    suspend fun restorePlaylistItem(id: String, path: String, index: Int) = updatePlaylists { current ->
        current.map { playlist ->
            if (playlist.id != id || path in playlist.paths || playlist.rule != null) playlist else
                playlist.copy(paths = playlist.paths.toMutableList().apply { add(index.coerceIn(0, size), path) })
        }
    }
    suspend fun setReplayGainEnabled(value: Boolean) = edit { it[Keys.replayGainEnabled] = value }
    suspend fun setBlackDiscMode(value: Boolean) = edit { it[Keys.blackDiscMode] = value }
    suspend fun setPlayHistoryEnabled(value: Boolean) = edit { it[Keys.playHistoryEnabled] = value }
    suspend fun toggleFavourite(path: String) = edit { prefs ->
        val values = decodeOrder(prefs[Keys.favouritePaths].orEmpty()).toMutableList()
        if (!values.remove(path)) values.add(path)
        prefs[Keys.favouritePaths] = StoredPathListCodec.encode(values)
    }

    suspend fun recordPlayed(path: String, atEpochMs: Long = System.currentTimeMillis()) {
        if (path.isBlank()) return
        context.dataStore.edit { prefs ->
            if (prefs[Keys.playHistoryEnabled] != true) return@edit
            val existing = decodePlayHistory(prefs[Keys.playHistory].orEmpty())
            val newest = existing.firstOrNull()
            if (newest?.path == path && atEpochMs - newest.playedAtEpochMs < 30_000L) return@edit
            prefs[Keys.playHistory] = encodePlayHistory(
                (listOf(PlayHistoryEntry(path, atEpochMs)) + existing).take(500),
            )
        }
    }

    suspend fun clearPlayHistory() = edit { it.remove(Keys.playHistory) }
    suspend fun setExcludedFolders(value: List<String>) = edit { prefs ->
        prefs[Keys.excludedFolders] = StoredPathListCodec.encode(value.distinct().sorted())
    }
    suspend fun setJokeAdsEnabled(value: Boolean) = edit { it[Keys.jokeAdsEnabled] = value }
    suspend fun setActivePlaylist(id: String?) = edit { prefs ->
        if (id == null) prefs.remove(Keys.activePlaylistId) else prefs[Keys.activePlaylistId] = id
    }

    suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        updatePlaylists { current -> current + LocalPlaylist(id, name.trim(), emptyList()) }
        return id
    }

    suspend fun createPlaylistWithPaths(name: String, paths: List<String>): String {
        val id = UUID.randomUUID().toString()
        updatePlaylists { current -> current + LocalPlaylist(id, name.trim(), paths.distinct()) }
        return id
    }

    suspend fun renamePlaylist(id: String, name: String) = updatePlaylists { playlists ->
        playlists.map { if (it.id == id) it.copy(name = name.trim()) else it }
    }

    suspend fun deletePlaylist(id: String) {
        context.dataStore.edit { prefs ->
            val updated = decodePlaylists(prefs[Keys.playlists].orEmpty()).filterNot { it.id == id }
            prefs[Keys.playlists] = encodePlaylists(updated)
            if (prefs[Keys.activePlaylistId] == id) prefs.remove(Keys.activePlaylistId)
        }
    }

    suspend fun addToPlaylist(id: String, path: String) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id != id || path in playlist.paths) playlist
            else playlist.copy(paths = playlist.paths + path)
        }
    }

    suspend fun addAllToPlaylist(id: String, paths: List<String>) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id != id || paths.isEmpty()) playlist
            else playlist.copy(paths = (playlist.paths + paths).distinct())
        }
    }

    suspend fun removeFromPlaylist(id: String, path: String) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id == id) playlist.copy(paths = playlist.paths.filterNot { it == path }) else playlist
        }
    }

    suspend fun movePlaylistItem(id: String, fromIndex: Int, toIndex: Int) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id != id || fromIndex !in playlist.paths.indices || toIndex !in playlist.paths.indices) {
                playlist
            } else {
                playlist.copy(paths = playlist.paths.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                })
            }
        }
    }

    suspend fun resetAppSettings() {
        context.dataStore.edit {
            it.remove(Keys.graphOptions)
            it.remove(Keys.libraryRowSize)
            it.remove(Keys.themeMode)
            it.remove(Keys.showThumbnails)
            it.remove(Keys.showFileDetails)
            it.remove(Keys.preloadThumbnails)
            it.remove(Keys.resumePlayback)
            it.remove(Keys.autoPictureInPicture)
            it.remove(Keys.floatingWindowMode)
            it.remove(Keys.floatingWindowDefaultV2)
            it.remove(Keys.appLanguage)
            it.remove(Keys.backgroundMode)
                        it.remove(Keys.customBackgroundImageUri)
                        it.remove(Keys.customBackgroundVideoUri)
                        it.remove(Keys.backgroundDim)
                        it.remove(Keys.backgroundScaleMode)
                        it.remove(Keys.seekOffsetMs)
            it.remove(Keys.appFont)
            it.remove(Keys.developerMode)
            it.remove(Keys.editableQueue)
            it.remove(Keys.silianRail)
            it.remove(Keys.showSleepControl)
            it.remove(Keys.showAbRepeat)
            it.remove(Keys.extendedSearch)
            it.remove(Keys.replayGainEnabled)
            it.remove(Keys.blackDiscMode)
            it.remove(Keys.playHistoryEnabled)
            it.remove(Keys.playHistory)
            it.remove(Keys.excludedFolders)
            it.remove(Keys.jokeAdsEnabled)
            it[Keys.speed] = 1f
            it[Keys.repeatMode] = Player.REPEAT_MODE_ONE.toLong()
        }
    }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private fun decodeOrder(encoded: String): List<String> = StoredPathListCodec.decode(encoded)

    private suspend fun updatePlaylists(transform: (List<LocalPlaylist>) -> List<LocalPlaylist>) {
        context.dataStore.edit { prefs ->
            val current = decodePlaylists(prefs[Keys.playlists].orEmpty())
            prefs[Keys.playlists] = encodePlaylists(transform(current))
        }
    }

    private fun encodePlaylists(playlists: List<LocalPlaylist>): String = playlists.joinToString("\n") { playlist ->
        listOf(
            encode(playlist.id),
            encode(playlist.name),
            playlist.paths.joinToString(",", transform = ::encode),
            playlist.rule?.let { rule -> encode(org.json.JSONObject().put("folder", rule.folder).put("extension", rule.extension).put("text", rule.text).toString()) }.orEmpty(),
        ).joinToString("|")
    }

    private fun decodePlaylists(encoded: String): List<LocalPlaylist> = encoded.lineSequence().mapNotNull { line ->
        val parts = line.split('|', limit = 4)
        if (parts.size < 3) return@mapNotNull null
        val id = decode(parts[0]) ?: return@mapNotNull null
        val name = decode(parts[1])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val paths = if (parts[2].isBlank()) emptyList() else parts[2].split(',').mapNotNull(::decode)
        val rule = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.let(::decode)?.let { raw -> runCatching {
            val json = org.json.JSONObject(raw)
            com.local.listentomusic.model.PlaylistRule(json.optString("folder"), json.optString("extension"), json.optString("text"))
        }.getOrNull() }
        LocalPlaylist(id, name, paths.distinct(), rule)
    }.toList()

    private fun encodePlayHistory(entries: List<PlayHistoryEntry>): String = entries.joinToString("\n") { entry ->
        "${entry.playedAtEpochMs}|${encode(entry.path)}"
    }

    private fun decodePlayHistory(encoded: String): List<PlayHistoryEntry> = encoded
        .lineSequence()
        .take(500)
        .mapNotNull { line ->
            val separator = line.indexOf('|')
            if (separator <= 0) return@mapNotNull null
            val epoch = line.substring(0, separator).toLongOrNull()?.takeIf { it > 0L } ?: return@mapNotNull null
            val path = decode(line.substring(separator + 1))?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            PlayHistoryEntry(path, epoch)
        }
        .toList()

    private fun encode(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun decodeOverrides(raw: String): Map<String, com.local.listentomusic.model.LocalOverride> = runCatching {
        val json = org.json.JSONObject(raw)
        json.keys().asSequence().associateWith { key ->
            val value = json.getJSONObject(key)
            com.local.listentomusic.model.LocalOverride(value.optString("title"), value.optString("cover"))
        }
    }.getOrDefault(emptyMap())

    private fun decode(value: String): String? = runCatching {
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE).toString(Charsets.UTF_8)
    }.getOrNull()

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value ?: fallback.name) }.getOrDefault(fallback)
}
