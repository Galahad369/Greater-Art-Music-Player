package com.local.listentomusic

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.data.ScanResult
import com.local.listentomusic.data.ThumbnailRepository
import com.local.listentomusic.data.WaveformRepository
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.PlayHistoryEntry
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.LocalLyrics
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.model.loadLocalLyrics
import com.local.listentomusic.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

internal enum class CycleMode { OFF, ONE, ALL, RANDOM }

internal fun resolveCycleMode(repeatMode: Int, random: Boolean): CycleMode = when {
    random -> CycleMode.RANDOM
    repeatMode == Player.REPEAT_MODE_ONE -> CycleMode.ONE
    repeatMode == Player.REPEAT_MODE_ALL -> CycleMode.ALL
    else -> CycleMode.OFF
}

internal fun nextCycleMode(current: CycleMode): CycleMode = when (current) {
    CycleMode.OFF -> CycleMode.ONE
    CycleMode.ONE -> CycleMode.ALL
    CycleMode.ALL -> CycleMode.RANDOM
    CycleMode.RANDOM -> CycleMode.OFF
}

// Sleep timer: minute targets. -1L = "stop at the end of the current track".
val sleepTimerOptions = listOf(5L, 10L, 15L, 30L, 60L, -1L)

data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    val endOfTrack: Boolean = false,
)

enum class LibraryStatus { NEEDS_PERMISSION, SCANNING, READY, FOLDER_MISSING, CANNOT_READ }

data class LibraryUiState(
    val status: LibraryStatus = LibraryStatus.SCANNING,
    val files: List<MediaFile> = emptyList(),
    val sortMode: SortMode = SortMode.NAME_ASC,
    val query: String = "",
    val targetPath: String = MediaScanner.targetFolder().absolutePath,
)

data class PlaybackUiState(
    val connected: Boolean = false,
    val currentPath: String? = null,
    val currentQueueIndex: Int = -1,
    val title: String = "Nothing playing",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_ONE,
    val shuffleEnabled: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val videoAspectRatio: Float = 16f / 9f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoFrameRendered: Boolean = false,
    val errorMessage: String? = null,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val showSleepControl: Boolean = false,
    val showAbRepeat: Boolean = false,
    val stackCount: Int = 0,
) {
    val hasMedia: Boolean get() = currentPath != null
    val isVideo: Boolean
        get() = currentPath?.substringAfterLast('.', "")?.lowercase(Locale.ROOT) in VIDEO_EXTENSIONS

    private companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi")
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var presentationOnly = false
    /** A floating presentation reads the session queue; it must not rescan Download. */
    fun useSessionPresentationOnly() {
        presentationOnly = true
        scanJob?.cancel()
        waveformWarmupJob?.cancel()
        waveformAheadJob?.cancel()
    }
    private val preferences = AppPreferences(application)
    private val thumbnailRepository = ThumbnailRepository(application)
    private val waveformRepository = WaveformRepository(application)
    val thumbnailStats = thumbnailRepository.stats
    val waveformDiagnostics = waveformRepository.diagnostics
    private var userPreferences = UserPreferences()
    private var preferencesLoaded = false
    private var scannedFiles: List<MediaFile> = emptyList()
    private var orderedFiles: List<MediaFile> = emptyList()
    private val metadataIndex = com.local.listentomusic.data.MetadataIndex(application)
    private var metadataJob: Job? = null
    private var sortingJob: Job? = null
    private var syncQueueAfterSort = false
    private var undoAction: (suspend () -> Unit)? = null
    private var undoJob: Job? = null
    val undoMessage = MutableStateFlow<String?>(null)
    val indexStatus = MutableStateFlow("Basic filenames")
    private val libraryObserver = com.local.listentomusic.data.LibraryObserver(application) { rescan() }
    fun startLibraryObservation() { libraryObserver.start() }
    fun stopLibraryObservation() { libraryObserver.stop() }

    private val _library = MutableStateFlow(LibraryUiState())
    val library: StateFlow<LibraryUiState> = _library.asStateFlow()
    private val _stackFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val stackFiles: StateFlow<List<MediaFile>> = _stackFiles.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaFile>>(emptyList())
    val queue: StateFlow<List<MediaFile>> = _queue.asStateFlow()

    private val graphRepository = com.local.listentomusic.data.GraphRepository(application)
    val graph = MutableStateFlow<com.local.listentomusic.graph.LibraryGraph?>(null)
    val graphLoading = MutableStateFlow(false)
    val graphError = MutableStateFlow<String?>(null)
    private var graphRequested = false
    private var graphJob: Job? = null
    private var graphGeneration = 0

    fun requestGraph() {
        graphRequested = true
        graphJob?.cancel()
        val generation = ++graphGeneration
        // Physical files only, independent of playlists, search and display-title overrides.
        val files = scannedFiles
        graphJob = viewModelScope.launch {
            graphLoading.value = true
            graphError.value = null
            try {
                val input = withContext(Dispatchers.Default) { files.distinctBy { it.sourcePath }.map {
                    com.local.listentomusic.graph.GraphInput(it.sourcePath, File(it.sourcePath).name)
                } }
                graph.value = graphRepository.load(input)
            }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { graphError.value = "Could not prepare the graph. Your library and playback are unchanged." }
            finally { if (generation == graphGeneration) graphLoading.value = false }
        }
    }

    fun playGraphNode(path: String) {
        val file = scannedFiles.firstOrNull { it.path == path } ?: scannedFiles.firstOrNull { it.sourcePath == path }
            ?.copy(path = path, name = File(path).name, clipStartMs = 0, clipEndMs = null)
        file?.let(::play)
    }

    private val _playback = MutableStateFlow(PlaybackUiState())
    val playback: StateFlow<PlaybackUiState> = _playback.asStateFlow()

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()
    private var sleepTimerJob: Job? = null
    private var sleepTimerEndsAt: Long = 0L
    // Single-job wakeup. The exact remainingMs is computed at fire time,
    // so we never have to chase a moving target. End-of-track uses a flag, not a clock.

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _settings = MutableStateFlow(UserPreferences())
    val settings: StateFlow<UserPreferences> = _settings.asStateFlow()
    private val _playHistory = MutableStateFlow<List<PlayHistoryEntry>>(emptyList())
    val playHistory: StateFlow<List<PlayHistoryEntry>> = _playHistory.asStateFlow()

    private var controllerLease: com.local.listentomusic.playback.SharedPlaybackResource.Lease<ListenableFuture<MediaController>>? = null
    private var tickerJob: Job? = null
        private var scanJob: Job? = null
        private var durationProbeJob: Job? = null
        private var waveformWarmupJob: Job? = null
        private var waveformAheadJob: Job? = null
        private var durationProbePath: String? = null
        private val probedDurations = mutableMapOf<String, Long>()
    private var pendingPlay: MediaFile? = null
    private var expandRestoredQueue = false
        private var lastPlaybackError: String? = null
        private var videoFrameRendered = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishPlayback(player)
            if (events.contains(Player.EVENT_TIMELINE_CHANGED) || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) syncPlaybackQueue()
            if (!presentationOnly && player.isPlaying && (events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION))) {
                player.currentMediaItem?.mediaId?.takeIf(String::isNotBlank)?.let { path ->
                    viewModelScope.launch { preferences.recordPlayed(path) }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            lastPlaybackError = "This file could not be decoded on this device. Trying the next item."
            publishPlayback(_controller.value ?: return)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) lastPlaybackError = null
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) videoFrameRendered = false
                    // Sleep timer in "end of track" mode fires when the next item lands.
                    val timer = _sleepTimer.value
                    if (timer.active && timer.endOfTrack) cancelSleepTimer()
                }

        override fun onRenderedFirstFrame() {
            val diagnostics = com.local.listentomusic.playback.PlaybackDiagnostics
            if (diagnostics.requestedAtMs > 0 && diagnostics.firstFrameDelayMs == null) diagnostics.firstFrameDelayMs = android.os.SystemClock.elapsedRealtime() - diagnostics.requestedAtMs
            videoFrameRendered = true
            _controller.value?.let(::publishPlayback)
        }
    }

    init {
        viewModelScope.launch {
            com.local.listentomusic.playback.StackPlayback.state.collect {
                _controller.value?.let(::publishPlayback)
            }
        }
        viewModelScope.launch { preferences.playHistory.collect { _playHistory.value = it } }
        viewModelScope.launch {
            preferences.values.collect {
                val firstPreferences = !preferencesLoaded
                preferencesLoaded = true
                val searchChanged = userPreferences.extendedSearch != it.extendedSearch
                val libraryChanged = userPreferences.sortMode != it.sortMode || userPreferences.customOrder != it.customOrder ||
                    userPreferences.playlists != it.playlists || userPreferences.activePlaylistId != it.activePlaylistId ||
                    userPreferences.localOverrides != it.localOverrides ||
                    userPreferences.favouritePaths != it.favouritePaths || searchChanged
                userPreferences = it
                _settings.value = it
                if (!it.showAbRepeat) com.local.listentomusic.playback.PracticeLoop.clear()
                _controller.value?.let(::publishPlayback)
                if (!presentationOnly) {
                    if (libraryChanged) applySortingAndFilter()
                    if (searchChanged) refreshMetadata()
                    if (firstPreferences) rescan()
                }
            }
        }
        connectController()
        // Also runs on service reconnection, not only on a new media-item event.
        waveformWarmupJob = viewModelScope.launch {
            combine(
                _queue,
                _library.map { it.files }.distinctUntilChanged(),
                _playback.map { it.currentPath }.distinctUntilChanged(),
                com.local.listentomusic.playback.PlayerWindowVisibility.expandedShowing,
                com.local.listentomusic.playback.StackPlayback.state.map { it.active }.distinctUntilChanged(),
            ) { queue, library, path, expanded, stackActive ->
                // Expanded Now Playing and Stack own the latency budget. Stack may
                // already run eight decoders, so background future-track MediaCodec
                // waveform work must not compete with active playback.
                if (expanded || stackActive) emptyList()
                else com.local.listentomusic.model.waveformWarmupPaths(queue, library, path)
            }.distinctUntilChanged()
                .collectLatest { upcoming ->
                    if (upcoming.isEmpty()) return@collectLatest
                    delay(700)
                    // One producer: future-track decoding cannot jump ahead of the current track.
                    upcoming.forEach {
                        loadWaveform(it)
                        delay(300)
                    }
                }
        }
    }

    fun rescan() {
        if (!preferencesLoaded) return
        if (!hasStorageAccess()) {
            _stackFiles.value = emptyList()
            _library.value = _library.value.copy(status = LibraryStatus.NEEDS_PERMISSION)
            return
        }
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            if (_library.value.files.isEmpty()) _library.value = _library.value.copy(status = LibraryStatus.SCANNING)
            when (val result = MediaScanner.scan(userPreferences.excludedFolders.toSet())) {
                is ScanResult.Success -> {
                    scannedFiles = result.files
                    if (graphRequested) requestGraph()
                    refreshMetadata()
                    applySortingAndFilter()
                    _library.value = _library.value.copy(status = LibraryStatus.READY)
                }
                is ScanResult.FolderMissing -> {
                    scannedFiles = emptyList()
                    if (graphRequested) requestGraph()
                    applySortingAndFilter()
                    _library.value = _library.value.copy(
                        status = LibraryStatus.FOLDER_MISSING,
                        targetPath = result.path,
                    )
                }
                is ScanResult.PermissionMissing -> {
                    scannedFiles = emptyList()
                    if (graphRequested) requestGraph()
                    applySortingAndFilter()
                    _library.value = _library.value.copy(
                        status = LibraryStatus.CANNOT_READ,
                        targetPath = result.path,
                    )
                }
            }
        }
    }

    fun setQuery(query: String) {
        _library.value = _library.value.copy(query = query)
        applySortingAndFilter()
    }

    fun setSortMode(mode: SortMode) {
        if (mode == userPreferences.sortMode) return
        viewModelScope.launch {
            syncQueueAfterSort = true
            if (mode == SortMode.CUSTOM && userPreferences.customOrder.isEmpty()) {
                preferences.setCustomOrder(orderedFiles.map { it.path })
            } else {
                preferences.setSortMode(mode)
            }
        }
    }

    fun moveCustomItem(fromIndex: Int, toIndex: Int) {
        if (_library.value.query.isNotBlank()) return
        if (fromIndex !in orderedFiles.indices || toIndex !in orderedFiles.indices || fromIndex == toIndex) return
        userPreferences.activePlaylistId?.let { playlistId ->
            viewModelScope.launch { preferences.movePlaylistItem(playlistId, fromIndex, toIndex) }
            return
        }
        if (userPreferences.sortMode != SortMode.CUSTOM) return
        val moved = orderedFiles.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        orderedFiles = moved
        _library.value = _library.value.copy(files = moved)
        synchronizeExistingQueueOrder(moved)
        viewModelScope.launch { preferences.setCustomOrder(moved.map { it.path }) }
    }

    fun play(file: MediaFile) {
        val player = _controller.value
        if (player == null) {
            // A tap can arrive while the MediaSession connection is still starting.
            // Keep it instead of silently dropping the user's request.
            pendingPlay = file
            return
        }
        playNow(player, file)
    }

    private fun playNow(player: Player, file: MediaFile) {
        val queue = com.local.listentomusic.model.browsingQueue(file, orderedFiles, scannedFiles)
        val index = queue.indexOfFirst { it.path == file.path }.coerceAtLeast(0)
        val resumeAt = if (userPreferences.resumePlayback && file.path == userPreferences.lastPath) {
            userPreferences.lastPositionMs
        } else 0L
        startNormalQueue(player, queue, index, resumeAt)
    }

    /** Every normal queue replacement exits Stack first and shares one startup path. */
    private fun startNormalQueue(player: Player, queue: List<MediaFile>, index: Int, positionMs: Long) {
        if (queue.isEmpty()) return
        com.local.listentomusic.playback.StackPlayback.stop()
        expandRestoredQueue = false
        com.local.listentomusic.playback.PlaybackDiagnostics.requestedAtMs = android.os.SystemClock.elapsedRealtime()
        com.local.listentomusic.playback.PlaybackDiagnostics.firstFrameDelayMs = null
        lastPlaybackError = null
        player.setMediaItems(queue.map(MediaFile::toMediaItem), index.coerceIn(queue.indices), positionMs.coerceAtLeast(0L))
        player.playWhenReady = true
        player.prepare()
    }

    suspend fun loadThumbnail(file: MediaFile): Bitmap? = thumbnailRepository.load(file)
    suspend fun loadWaveform(path: String): FloatArray? {
        val file = scannedFiles.firstOrNull { it.path == path } ?: _queue.value.firstOrNull { it.path == path }
        val source = file?.sourcePath ?: com.local.listentomusic.model.sourceMediaPath(path)
        // Stat the physical source even before scanning finishes. Never cache under 0/0.
        val identity = withContext(Dispatchers.IO) { File(source).let { it.length() to it.lastModified() } }
        val peaks = waveformRepository.load(source, identity.first, identity.second) ?: return null
        if (file == null || file.clipStartMs == 0L && file.clipEndMs == null) return peaks
        return withContext(Dispatchers.IO) {
            val reader = MediaMetadataRetriever()
            val duration = try { reader.setDataSource(source); reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0 }
                catch (_: Exception) { 0L } finally { runCatching { reader.release() } }
            if (duration <= 0) null else {
                val start = (file.clipStartMs.toDouble() / duration * peaks.size).toInt().coerceIn(0, peaks.size)
                val end = ((file.clipEndMs ?: duration).toDouble() / duration * peaks.size).toInt().coerceIn(start, peaks.size)
                peaks.copyOfRange(start, end).takeIf { it.isNotEmpty() }
            }
        }
    }

    fun playQueueItem(index: Int) {
        val player = _controller.value ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekToDefaultPosition(index)
        player.play()
    }

    suspend fun loadCurrentArtwork(path: String?): Bitmap? {
        val file = scannedFiles.firstOrNull { it.path == path } ?: _queue.value.firstOrNull { it.path == path } ?: return null
        val override = userPreferences.localOverrides[file.path]
        return thumbnailRepository.load(file.copy(coverUri = override?.coverUri.orEmpty()))
    }

    suspend fun loadLyrics(path: String?): LocalLyrics? = withContext(Dispatchers.IO) {
        val file = scannedFiles.firstOrNull { it.path == path }
        val loaded = loadLocalLyrics(file?.sourcePath ?: path?.let { com.local.listentomusic.model.sourceMediaPath(it) })
        if (file == null || file.clipStartMs == 0L && file.clipEndMs == null) loaded else loaded?.copy(lines = loaded.lines
            .filter { it.timeMs >= file.clipStartMs && (file.clipEndMs == null || it.timeMs < file.clipEndMs) }
            .map { line -> line.copy(timeMs = line.timeMs - file.clipStartMs, words = line.words.map { it.copy(timeMs = it.timeMs - file.clipStartMs) }) })
    }

    fun togglePlayPause() {
        val stack = com.local.listentomusic.playback.StackPlayback.state.value
        if (stack.active) {
            if (stack.playing) com.local.listentomusic.playback.StackPlayback.pause()
            else com.local.listentomusic.playback.StackPlayback.play()
            return
        }
        _controller.value?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekBy(deltaMs: Long) {
        val stack = com.local.listentomusic.playback.StackPlayback.state.value
        if (stack.active) { com.local.listentomusic.playback.StackPlayback.seek(stack.positionMs + deltaMs); return }
        _controller.value?.let { seekTo(it.currentPosition + deltaMs) }
    }

    fun seekTo(positionMs: Long) {
        if (com.local.listentomusic.playback.StackPlayback.state.value.active) {
            com.local.listentomusic.playback.StackPlayback.seek(positionMs)
            return
        }
        _controller.value?.seekTo(positionMs.coerceAtLeast(0L))
        _controller.value?.let(::publishPlayback)
    }

    fun next() {
        if (com.local.listentomusic.playback.StackPlayback.state.value.active) return
        _controller.value?.seekToNextMediaItem()
    }

    fun previous() {
        if (com.local.listentomusic.playback.StackPlayback.state.value.active) {
            com.local.listentomusic.playback.StackPlayback.seek(0L)
            return
        }
        _controller.value?.let {
            if (it.currentPosition > 4_000) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun setSpeed(speed: Float) {
        _controller.value?.playbackParameters = PlaybackParameters(speed)
        _controller.value?.let(::publishPlayback)
    }

    fun beginTemporaryDoubleSpeed(): Boolean =
        com.local.listentomusic.playback.TemporaryPlaybackSpeed.begin()

    fun endTemporaryDoubleSpeed() =
        com.local.listentomusic.playback.TemporaryPlaybackSpeed.end()

    fun setPlaybackCycle(mode: Int, random: Boolean) {
        if (com.local.listentomusic.playback.StackPlayback.state.value.active) return
        _controller.value?.let {
            it.shuffleModeEnabled = random
            it.repeatMode = if (random) Player.REPEAT_MODE_ALL else mode
            publishPlayback(it)
        }
    }

    fun setLibraryRowSize(value: LibraryRowSize) = updatePreference { preferences.setLibraryRowSize(value) }
    fun setThemeMode(value: ThemeMode) = updatePreference { preferences.setThemeMode(value) }
    fun setColorTheme(value: com.local.listentomusic.data.ColorTheme) = updatePreference { preferences.setColorTheme(value) }
    fun setShowThumbnails(value: Boolean) = updatePreference { preferences.setShowThumbnails(value) }
    fun setShowFileDetails(value: Boolean) = updatePreference { preferences.setShowFileDetails(value) }
    fun setResumePlayback(value: Boolean) = updatePreference { preferences.setResumePlayback(value) }
    fun setAutoPictureInPicture(value: Boolean) = updatePreference { preferences.setAutoPictureInPicture(value) }
    fun setFloatingWindowMode(value: FloatingWindowMode) =
        updatePreference { preferences.setFloatingWindowMode(value) }
    fun setAppLanguage(value: AppLanguage) = updatePreference { preferences.setAppLanguage(value) }
    fun setBackgroundMode(value: AppBackgroundMode) =
            updatePreference { preferences.setBackgroundMode(value) }
        fun setBackgroundScaleMode(value: BackgroundScaleMode) =
            updatePreference { preferences.setBackgroundScaleMode(value) }
        fun setCustomBackgroundImage(uri: String?) = updatePreference {
        preferences.setCustomBackgroundImageUri(uri)
        if (uri != null) preferences.setBackgroundMode(AppBackgroundMode.CUSTOM_IMAGE)
    }
    fun setCustomBackgroundVideo(uri: String?) = updatePreference {
        preferences.setCustomBackgroundVideoUri(uri)
        if (uri != null) preferences.setBackgroundMode(AppBackgroundMode.CUSTOM_VIDEO)
    }
    fun setBackgroundDim(value: Float) = updatePreference { preferences.setBackgroundDim(value) }
    fun setSeekOffset(value: Long) = updatePreference { preferences.setSeekOffsetMs(value) }
    fun setAppFont(value: com.local.listentomusic.data.AppFont) =
        updatePreference { preferences.setAppFont(value) }
    fun setDeveloperMode(value: Boolean) = updatePreference { preferences.setDeveloperMode(value) }
    fun setEditableQueue(value: Boolean) = updatePreference { preferences.setEditableQueue(value) }
    fun setShowSleepControl(value: Boolean) = updatePreference { preferences.setShowSleepControl(value) }
    fun setShowAbRepeat(value: Boolean) = updatePreference { preferences.setShowAbRepeat(value) }
    fun setExtendedSearch(value: Boolean) = updatePreference { preferences.setExtendedSearch(value) }
    fun setLocalOverride(path: String, title: String, cover: String) = updatePreference {
        preferences.setLocalOverride(path, com.local.listentomusic.model.LocalOverride(title.trim().take(300), cover))
    }
    fun createRulePlaylist(name: String, rule: com.local.listentomusic.model.PlaylistRule) = updatePreference {
        if (name.isNotBlank()) preferences.setActivePlaylist(preferences.createRulePlaylist(name.take(60), rule))
    }
    fun undoLastEdit() {
        val action = undoAction ?: return
        undoAction = null; undoMessage.value = null; undoJob?.cancel()
        viewModelScope.launch { action() }
    }
    private fun offerUndo(message: String, action: suspend () -> Unit) {
        undoJob?.cancel(); undoAction = action; undoMessage.value = message
        undoJob = viewModelScope.launch { delay(8_000); undoAction = null; undoMessage.value = null }
    }
    private fun refreshMetadata() {
        metadataJob?.cancel()
        if (!userPreferences.extendedSearch) { indexStatus.value = "Basic filenames"; return }
        val snapshot = scannedFiles
        metadataJob = viewModelScope.launch {
            indexStatus.value = "Indexing ${snapshot.size} files (cached tags reused)"
            delay(800)
            metadataIndex.enrich(snapshot) { batch -> withContext(Dispatchers.Main) {
                scannedFiles = batch; applySortingAndFilter()
            } }
            indexStatus.value = "Search index ready: ${snapshot.size} files"
        }
    }
    fun setReplayGainEnabled(value: Boolean) = updatePreference { preferences.setReplayGainEnabled(value) }
    fun setBlackDiscMode(value: Boolean) = updatePreference { preferences.setBlackDiscMode(value) }
    fun setPlayHistoryEnabled(value: Boolean) = updatePreference { preferences.setPlayHistoryEnabled(value) }
    fun toggleFavourite(path: String) = updatePreference { preferences.toggleFavourite(path) }
    fun clearPlayHistory() = updatePreference { preferences.clearPlayHistory() }
    fun setJokeAdsEnabled(value: Boolean) = updatePreference { preferences.setJokeAdsEnabled(value) }
    fun setFolderExcluded(folder: String, excluded: Boolean) = updatePreference {
        val next = userPreferences.excludedFolders.toMutableSet().apply {
            if (excluded) add(folder) else remove(folder)
        }
        preferences.setExcludedFolders(next.toList())
        userPreferences = preferences.current()
        scanJob?.cancel()
        scanJob = null
        rescan()
    }
    fun setActivePlaylist(id: String?) = updatePreference { preferences.setActivePlaylist(id) }

    fun backupSettings(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val result = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(preferences.exportBackup()) }
        }
        withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, if (result.isSuccess) "Backup saved" else "Could not save backup", android.widget.Toast.LENGTH_LONG).show() }
    }

    fun restoreSettings(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val result = runCatching {
            val bytes = context.contentResolver.openInputStream(uri)!!.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 5_000_000) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= 5_000_000)
            preferences.restoreBackup(bytes.toString(Charsets.UTF_8))
        }
        withContext(Dispatchers.Main) {
            if (result.isSuccess) { userPreferences = preferences.current(); scanJob?.cancel(); scanJob = null; rescan() }
            android.widget.Toast.makeText(context, if (result.isSuccess) "Settings and playlists restored" else "Invalid or unreadable backup", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun importM3u(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val raw = runCatching { resolver.openInputStream(uri)?.bufferedReader()?.use { input ->
                val data = StringBuilder(); val chars = CharArray(4096)
                while (true) { val count = input.read(chars); if (count < 0) break; data.append(chars, 0, count); require(data.length <= 5_000_000) }
                data.lines()
            } }.getOrNull() ?: return@launch
            val byPath = scannedFiles.associateBy { File(it.path).canonicalPath }
            val byName = scannedFiles.groupBy { File(it.path).name.lowercase(Locale.ROOT) }
            val paths = com.local.listentomusic.model.parseM3u(raw).asSequence()
                .mapNotNull { entry ->
                    val decoded = Uri.decode(entry.path.removePrefix("file://"))
                    if (entry.start != null) {
                        val candidates = scannedFiles.filter { it.clipStartMs == entry.start && it.clipEndMs == entry.end }
                        return@mapNotNull (candidates.firstOrNull { it.sourcePath == decoded }
                            ?: candidates.filter { File(it.sourcePath).name == File(decoded).name }.singleOrNull())?.path
                    }
                    runCatching { File(decoded).canonicalPath }.getOrNull()?.let(byPath::get)?.path
                        ?: byName[File(decoded).name.lowercase(Locale.ROOT)]?.singleOrNull()?.path
                }.distinct().toList()
            if (paths.isEmpty()) return@launch
            val displayName = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0).substringBeforeLast('.') else null
                }
            }.getOrNull().orEmpty().ifBlank { "Imported playlist" }
            val id = preferences.createPlaylistWithPaths(displayName.take(60), paths)
            preferences.setActivePlaylist(id)
        }
    }

    fun exportActiveM3u(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = com.local.listentomusic.model.exportM3u(orderedFiles)
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?.bufferedWriter()?.use { it.write(lines) }
            }
        }
    }
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = preferences.createPlaylist(name)
            preferences.setActivePlaylist(id)
        }
    }
    fun addAllToPlaylist(id: String, paths: List<String>) {
        if (paths.isEmpty()) return
        updatePreference { preferences.addAllToPlaylist(id, paths) }
    }
    fun createPlaylistAndSeed(name: String, seedPath: String?, keyword: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = preferences.createPlaylist(name)
            val toAdd = buildList {
                seedPath?.let { add(it) }
                keyword?.takeIf { it.isNotBlank() }?.let { kw ->
                    val upper = kw.uppercase(Locale.ROOT)
                    orderedFiles.filter { it.path.uppercase(Locale.ROOT).contains(upper) || it.name.uppercase(Locale.ROOT).contains(upper) }
                        .forEach { add(it.path) }
                }
            }.distinct()
            if (toAdd.isNotEmpty()) preferences.addAllToPlaylist(id, toAdd)
            preferences.setActivePlaylist(id)
        }
    }
    fun playPlaylist(id: String) {
        val player = _controller.value ?: return
        val byPath = scannedFiles.associateBy { it.path }
        val queue = if (id == com.local.listentomusic.data.FAVOURITES_PLAYLIST_ID) {
            userPreferences.favouritePaths.mapNotNull(byPath::get)
        } else {
            val playlist = userPreferences.playlists.firstOrNull { it.id == id } ?: return
            playlist.rule?.let { rule -> scannedFiles.filter { rule.matches(it, MediaScanner.targetFolder().path) } }
                ?: com.local.listentomusic.model.expandStackKeyword(
                    explicitPaths = playlist.paths,
                    library = scannedFiles,
                    keyword = playlist.stackKeyword,
                )
        }
        startNormalQueue(player, queue, 0, 0L)
    }

    fun filesForPlaylist(id: String): List<MediaFile> {
        val decorated = scannedFiles.map { file -> userPreferences.localOverrides[file.path]?.let { override ->
            file.copy(name = override.title.ifBlank { file.name }, coverUri = override.coverUri)
        } ?: file }
        val byPath = decorated.associateBy(MediaFile::path)
        if (id == com.local.listentomusic.data.FAVOURITES_PLAYLIST_ID) {
            return userPreferences.favouritePaths.mapNotNull(byPath::get)
        }
        val playlist = userPreferences.playlists.firstOrNull { it.id == id } ?: return emptyList()
        return playlist.rule?.let { rule -> decorated.filter { rule.matches(it, MediaScanner.targetFolder().path) } }
            ?: com.local.listentomusic.model.expandStackKeyword(
                explicitPaths = playlist.paths,
                library = decorated,
                keyword = playlist.stackKeyword,
            )
    }
    fun renamePlaylist(id: String, name: String) {
        if (name.isBlank()) return
        updatePreference { preferences.renamePlaylist(id, name) }
    }
    fun deletePlaylist(id: String) = updatePreference {
        val old = userPreferences.playlists.firstOrNull { it.id == id } ?: return@updatePreference
        preferences.deletePlaylist(id)
        offerUndo("Playlist removed") { preferences.restorePlaylist(old) }
    }
    fun createSelectionPlaylist(name: String, paths: List<String>) = updatePreference {
        if (name.isNotBlank()) preferences.setActivePlaylist(preferences.createPlaylistWithPaths(name.take(60), paths))
    }
    fun saveStackAsPlaylist(name: String, keyword: String, paths: List<String>) {
        val clean = paths.distinct()
        val cleanKeyword = keyword.trim().take(80)
        val resolvedName = name.trim().take(60).ifBlank {
            cleanKeyword.takeIf(String::isNotBlank)?.let { "Stack · ${it.take(42)}" }.orEmpty()
        }
        if (resolvedName.isBlank() || clean.isEmpty()) return
        viewModelScope.launch {
            preferences.createPlaylistWithPaths(resolvedName, clean, stackKeyword = cleanKeyword)
        }
    }
    fun addToPlaylist(id: String, path: String) = updatePreference { preferences.addToPlaylist(id, path) }
    fun removeFromActivePlaylist(path: String) {
        val id = userPreferences.activePlaylistId ?: return
        val old = userPreferences.playlists.firstOrNull { it.id == id } ?: return
        if (old.rule != null) return
        updatePreference {
            preferences.removeFromPlaylist(id, path)
            offerUndo("Song removed from playlist") { preferences.restorePlaylistItem(id, path, old.paths.indexOf(path)) }
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            thumbnailRepository.clear()
            waveformRepository.clear()
        }
    }

    fun resetAppSettings() {
        viewModelScope.launch {
            preferences.resetAppSettings()
            _controller.value?.let {
                it.playbackParameters = PlaybackParameters(1f)
                it.repeatMode = Player.REPEAT_MODE_ONE
                it.shuffleModeEnabled = false
                publishPlayback(it)
            }
        }
    }

    fun cycleRepeatMode() {
        _controller.value?.let { player ->
            val current = resolveCycleMode(player.repeatMode, player.shuffleModeEnabled)
            val next = nextCycleMode(current)
            applyCycleMode(player, next)
            publishPlayback(player)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = _controller.value ?: return
        if (fromIndex !in 0 until player.mediaItemCount || toIndex !in 0 until player.mediaItemCount) return
        player.moveMediaItem(fromIndex, toIndex)
        // Listener callbacks may already have updated the queue synchronously.
        // Read the player rather than applying the move twice to the UI snapshot.
        syncPlaybackQueue()
    }

    fun removeQueueItem(index: Int) {
        val player = _controller.value ?: return
        if (index !in _queue.value.indices || player.mediaItemCount <= 1) return
        val removed = player.getMediaItemAt(index)
        player.removeMediaItem(index)
        syncPlaybackQueue()
        offerUndo("Song removed from queue") {
            if (_controller.value === player) {
                // Duplicates are valid queue entries. Restore the exact removed
                // occurrence even when another item has the same mediaId.
                player.addMediaItem(index.coerceIn(0, player.mediaItemCount), removed)
                syncPlaybackQueue()
            }
        }
    }

    // One control owns the full cycle: Off → One → All → Random → Off.
    private fun applyCycleMode(player: Player, mode: CycleMode) {
        when (mode) {
            CycleMode.OFF -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            CycleMode.ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            CycleMode.ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            CycleMode.RANDOM -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }
        }
    }

    // -1L minute target = pause at the end of the current track.
    fun setSleepTimer(minutes: Long) {
        sleepTimerJob?.cancel()
        if (minutes == 0L) {
            cancelSleepTimer()
            return
        }
        if (minutes == -1L) {
            _sleepTimer.value = SleepTimerState(active = true, endOfTrack = true)
            return
        }
        sleepTimerEndsAt = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimer.value = SleepTimerState(active = true, remainingMs = minutes * 60_000L)
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = (sleepTimerEndsAt - System.currentTimeMillis()).coerceAtLeast(0L)
                _sleepTimer.value = _sleepTimer.value.copy(remainingMs = remaining)
                if (remaining == 0L) break
                delay(1_000L)
            }
            _controller.value?.pause()
            _sleepTimer.value = SleepTimerState()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimer.value = SleepTimerState()
    }

    private fun connectController() {
        val context = getApplication<Application>()
        val lease = com.local.listentomusic.playback.PlaybackConnection.acquire(context)
        controllerLease = lease
        val future = lease.value
        future.addListener({
            if (controllerLease !== lease) return@addListener
            runCatching { future.get() }.onSuccess { mediaController ->
                _controller.value = mediaController
                mediaController.addListener(playerListener)
                expandRestoredQueue = mediaController.mediaItemCount == 1
                if (expandRestoredQueue && orderedFiles.isNotEmpty()) applySortingAndFilter()
                syncPlaybackQueue()
                publishPlayback(mediaController)
                startTicker()
                pendingPlay?.let { requested ->
                    pendingPlay = null
                    playNow(mediaController, requested)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            // Labels need only two updates per second. The old 4 Hz root-state churn
            // made long library scrolling visibly hitch on mid-range phones.
            while (true) {
                _controller.value?.let(::publishPlayback)
                delay(500L)
            }
        }
    }

    private fun publishPlayback(player: Player) {
        val stack = com.local.listentomusic.playback.StackPlayback.state.value.takeIf { it.active }
        val path = stack?.primaryPath ?: player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() }
        val timelineDuration = if (
            !player.currentTimeline.isEmpty &&
            player.currentMediaItemIndex in 0 until player.currentTimeline.windowCount
        ) {
            player.currentTimeline.getWindow(
                player.currentMediaItemIndex,
                androidx.media3.common.Timeline.Window(),
            ).durationMs
        } else {
            C.TIME_UNSET
        }
        val duration = resolveDurationMs(
            player.duration,
            player.contentDuration,
            timelineDuration,
            path?.let(probedDurations::get) ?: C.TIME_UNSET,
        )
        if (duration == 0L && path != null) probeDuration(path)
        val videoSize = player.videoSize
        val videoAspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
            videoSize.width.toFloat() / videoSize.height.toFloat()
        } else {
            16f / 9f
        }
        // Match the UI cadence so player callbacks do not emit redundant state.
        val quantizedPosition = ((stack?.positionMs ?: player.currentPosition) / 500L) * 500L
        val next = PlaybackUiState(
            connected = true,
            currentPath = path,
            currentQueueIndex = if (stack == null && player.currentMediaItemIndex in 0 until player.mediaItemCount) {
                player.currentMediaItemIndex
            } else -1,
            title = orderedFiles.firstOrNull { it.path == path }?.name ?: player.mediaMetadata.title?.toString()
                ?: player.currentMediaItem?.mediaId?.substringAfterLast('/')
                ?: "Nothing playing",
            isPlaying = stack?.playing ?: player.isPlaying,
            positionMs = quantizedPosition.coerceAtLeast(0L),
            durationMs = stack?.durationMs ?: duration,
            speed = player.playbackParameters.speed,
            repeatMode = player.repeatMode,
            shuffleEnabled = player.shuffleModeEnabled,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem() || player.currentPosition > 0,
            videoAspectRatio = videoAspectRatio,
            videoWidth = videoSize.width,
            videoHeight = videoSize.height,
            videoFrameRendered = videoFrameRendered,
            errorMessage = lastPlaybackError,
            appLanguage = userPreferences.appLanguage,
            showSleepControl = userPreferences.showSleepControl,
            showAbRepeat = userPreferences.showAbRepeat,
            stackCount = stack?.slots?.size ?: 0,
        )
        // Skip identical emits. Every StateFlow update triggers a
        // recomposition storm across every screen that reads `playback`.
        if (next == _playback.value) return
        _playback.value = next
    }

    private fun applySortingAndFilter() {
        sortingJob?.cancel()
        val prefs = userPreferences
        val raw = scannedFiles
        val query = _library.value.query.trim()
        sortingJob = viewModelScope.launch {
        val (all, ordered, shown) = withContext(Dispatchers.Default) {
        val decorated = raw.map { file -> prefs.localOverrides[file.path]?.let { override ->
            file.copy(name = override.title.ifBlank { file.name }, coverUri = override.coverUri)
        } ?: file }
        val names = Comparator<MediaFile> { a, b -> com.local.listentomusic.model.naturalNames.compare(a.name, b.name) }
        val sortedLibrary = when (prefs.sortMode) {
            SortMode.CUSTOM -> {
                val rank = prefs.customOrder.withIndex().associate { it.value to it.index }
                decorated.sortedWith(
                    compareBy<MediaFile> { rank[it.path] ?: Int.MAX_VALUE }
                        .then(names)
                )
            }
            SortMode.NAME_ASC -> decorated.sortedWith(names)
            SortMode.NAME_DESC -> decorated.sortedWith(names.reversed())
            SortMode.DATE_DESC -> decorated.sortedWith(compareByDescending<MediaFile> { it.modifiedMs }.then(names))
            SortMode.DATE_ASC -> decorated.sortedWith(compareBy<MediaFile> { it.modifiedMs }.then(names))
        }
        val ordered = if (prefs.activePlaylistId == com.local.listentomusic.data.FAVOURITES_PLAYLIST_ID) {
            sortedLibrary.filter { it.path in prefs.favouritePaths }
        } else prefs.activePlaylistId
            ?.let { id -> prefs.playlists.firstOrNull { it.id == id } }
            ?.let { playlist ->
                val byPath = decorated.associateBy(MediaFile::path)
                playlist.rule?.let { rule -> sortedLibrary.filter { rule.matches(it, MediaScanner.targetFolder().path) } }
                    ?: com.local.listentomusic.model.expandStackKeyword(
                        explicitPaths = playlist.paths,
                        library = sortedLibrary,
                        keyword = playlist.stackKeyword,
                    )
            }
            ?: sortedLibrary
        val normalized = com.local.listentomusic.model.searchText(query)
        val shown = if (query.isBlank()) ordered else ordered.filter {
            com.local.listentomusic.model.searchText(it.name).contains(normalized) ||
                (prefs.extendedSearch && it.searchExtras.contains(normalized))
        }
        Triple(sortedLibrary, ordered, shown)
        }
        _stackFiles.value = all
        orderedFiles = ordered
        if (syncQueueAfterSort) {
            syncQueueAfterSort = false
            synchronizeExistingQueueOrder(ordered)
        }
        // A cold service restore contains only the remembered item. Append the library
        // once without restarting it. Explicit one-song playlists/queue edits stay intact.
        _controller.value?.let { player ->
            if (expandRestoredQueue && ordered.isNotEmpty()) {
                expandRestoredQueue = false
                if (player.mediaItemCount == 1 && prefs.activePlaylistId == null) {
                    val current = player.currentMediaItem?.mediaId
                    player.addMediaItems(ordered.filter { it.path != current }.map(MediaFile::toMediaItem))
                }
            }
        }
        syncPlaybackQueue()
        _library.value = _library.value.copy(
            files = shown,
            sortMode = prefs.sortMode,
        )
        }
    }

    private fun syncPlaybackQueue() {
        val player = _controller.value
        if (player == null || player.mediaItemCount == 0) { _queue.value = orderedFiles; return }
        val byId = (_queue.value + scannedFiles + orderedFiles).associateBy { it.path }
        // Session is authoritative. A scan race must never drop playable queue entries.
        val session = (0 until player.mediaItemCount).map(player::getMediaItemAt)
        _queue.value = com.local.listentomusic.model.reconcileSessionQueue(session.map { it.mediaId }, byId) { index ->
            com.local.listentomusic.model.mediaFileFromSession(session[index])
        }
    }

    /** Deletes one physical media file only. The caller owns the explicit confirmation UI. */
    suspend fun deleteMediaFile(file: MediaFile): String? {
        val candidate = withContext(Dispatchers.IO) { runCatching { File(file.sourcePath).canonicalFile }.getOrNull() }
            ?: return "Android could not resolve this file. Nothing was deleted."
        val failure = withContext(Dispatchers.IO) {
            when {
                !MediaScanner.isInsideTarget(candidate) -> "Safety check blocked deletion outside Download."
                candidate.exists() && !candidate.isFile -> "The selected item is not a file. Nothing was deleted."
                !candidate.exists() -> null
                !candidate.delete() -> "Android refused to delete the file. Check file access and try again."
                else -> null
            }
        }
        if (failure != null) return failure

        // Remove every cue/queue entry backed by the deleted physical file. Media3 then
        // advances safely if it was the current item; an empty queue is explicitly stopped.
        _controller.value?.let { player ->
            val removed = (0 until player.mediaItemCount).filter { index ->
                com.local.listentomusic.model.sourceMediaPath(player.getMediaItemAt(index).mediaId) == candidate.path
            }
            removed.asReversed().forEach(player::removeMediaItem)
            if (player.mediaItemCount == 0) { player.stop(); player.clearMediaItems() }
        }
        scannedFiles = scannedFiles.filterNot { runCatching { File(it.sourcePath).canonicalPath == candidate.path }.getOrDefault(false) }
        orderedFiles = orderedFiles.filterNot { runCatching { File(it.sourcePath).canonicalPath == candidate.path }.getOrDefault(false) }
        _queue.value = _queue.value.filterNot { runCatching { File(it.sourcePath).canonicalPath == candidate.path }.getOrDefault(false) }
        graph.value = null
        applySortingAndFilter()
        scanJob?.cancel()
        scanJob = null
        rescan()
        return null
    }

    private fun synchronizeExistingQueueOrder(ordered: List<MediaFile>) {
        val player = _controller.value ?: return
        if (player.mediaItemCount == 0 || ordered.isEmpty()) return
        val sessionIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        if (!com.local.listentomusic.model.containsSameMedia(sessionIds, ordered)) return
        val current = player.currentMediaItem?.mediaId ?: return
        val index = ordered.indexOfFirst { it.path == current }
        if (index < 0 || sessionIds == ordered.map(MediaFile::path)) return
        val position = player.currentPosition.coerceAtLeast(0L)
        val playWhenReady = player.playWhenReady
        player.setMediaItems(ordered.map(MediaFile::toMediaItem), index, position)
        player.prepare()
        player.playWhenReady = playWhenReady
        syncPlaybackQueue()
    }

    fun setGraphOptions(options: com.local.listentomusic.graph.GraphOptions) = viewModelScope.launch { preferences.setGraphOptions(options) }

    fun refreshPlaybackSession() {
        syncPlaybackQueue()
        _controller.value?.let(::publishPlayback)
    }

    private fun hasStorageAccess(): Boolean {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updatePreference(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun probeDuration(path: String) {
        if (durationProbePath == path || probedDurations.containsKey(path)) return
        durationProbeJob?.cancel()
        durationProbePath = path
        durationProbeJob = viewModelScope.launch {
            val duration = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                        retriever.setDataSource(com.local.listentomusic.model.sourceMediaPath(path))
                        val full = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        val clip = com.local.listentomusic.model.cueBounds(path)
                        if (clip == null) full else full?.let { ((clip.second ?: it) - clip.first).coerceAtLeast(0) }
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }
            }
            if (duration != null && duration > 0L) probedDurations[path] = duration
            durationProbePath = null
            _controller.value?.let(::publishPlayback)
        }
    }

    override fun onCleared() {
        libraryObserver.stop()
        tickerJob?.cancel()
        scanJob?.cancel()
        durationProbeJob?.cancel()
        waveformWarmupJob?.cancel()
        sleepTimerJob?.cancel()
        _controller.value?.removeListener(playerListener)
        controllerLease?.close()
        controllerLease = null
        _controller.value = null
        super.onCleared()
    }

}

internal fun resolveDurationMs(vararg candidates: Long): Long =
    candidates.firstOrNull { it > 0L && it != C.TIME_UNSET } ?: 0L
