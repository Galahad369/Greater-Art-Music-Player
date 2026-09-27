package com.local.listentomusic.playback

import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class PlaybackService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var preferences: AppPreferences
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var saveJob: Job? = null
    private var speedBeforeTemporaryHold: Float? = null
    private var retriedPath: String? = null
    private var gainEnabled = false
    private var enhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var widgetArtwork: android.graphics.Bitmap? = null
    private var widgetJob: Job? = null
    private val layers = mutableListOf<LayerPlayer>()
    private var focusHeld = false
    private val focusRequest by lazy {
        android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                if (change < 0) { focusHeld = false; pauseEveryPlayer() }
            }.build()
    }
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (::player.isInitialized && removedDevices.any(::isBluetoothOutput)) {
                // Never spill private playback through the phone speaker after a
                // Bluetooth headset/speaker disappears.
                pauseEveryPlayer()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(applicationContext)
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,
                50_000,
                100,
                250,
            )
            // Local 4K/hi-res files can be huge. Keep a useful read-ahead without
            // letting one item grow the player buffer until the process is killed.
            .setTargetBufferBytes(96 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(5_000, true)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false, // One shared audio-focus owner for all ten possible voices.
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.installVideoDiagnostics("PRIMARY")
        player.skipSilenceEnabled = false
        ParallelPlayback.addCommand = ::addLayer
        ParallelPlayback.stopCommand = {
            pauseEveryPlayer()
            layers.toList().forEach { removeLayer(it.id) }
            player.stop()
        }
        ParallelPlayback.removeCommand = ::removeLayer
        ParallelPlayback.toggleCommand = { id -> layers.firstOrNull { it.id == id }?.player?.let { if (it.playWhenReady) it.pause() else it.play() } }
        ParallelPlayback.volumeCommand = { id, level -> layers.firstOrNull { it.id == id }?.level = level; applyMixLevels(); publishLayers() }
        TemporaryPlaybackSpeed.beginCommand = {
            val current = player.playbackParameters.speed
            if (!player.isPlaying || current >= 2f || speedBeforeTemporaryHold != null) false
            else {
                speedBeforeTemporaryHold = current
                player.playbackParameters = androidx.media3.common.PlaybackParameters(2f)
                true
            }
        }
        TemporaryPlaybackSpeed.endCommand = {
            speedBeforeTemporaryHold?.let { previous ->
                player.playbackParameters = androidx.media3.common.PlaybackParameters(previous)
                speedBeforeTemporaryHold = null
            }
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(mainLooper))
        // ponytail: async prefs load; runBlocking here froze main and ANR'd the first play tap.
        serviceScope.launch {
            val saved = runCatching { preferences.current() }.getOrDefault(UserPreferences())
            if (mediaSession == null) return@launch // service already destroyed
            restoreLastSession(saved)
        }

        mediaSession = MediaLibrarySession.Builder(this, player, LocalLibraryCallback(this, serviceScope)).build()
        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) ensureFocus()
                else if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) pauseEveryPlayer()
            }
            override fun onEvents(player: Player, events: Player.Events) {
                PlaybackWidget.update(this@PlaybackService, player.mediaMetadata.title?.toString() ?: "Greater Art", player.isPlaying, widgetArtwork)
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { applyGain(); publishDiagnostics() }
            override fun onAudioSessionIdChanged(audioSessionId: Int) { applyGain() }
            override fun onIsPlayingChanged(isPlaying: Boolean) = scheduleSave()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                retriedPath = null
                widgetArtwork = null
                widgetJob?.cancel()
                val path = mediaItem?.mediaId
                if (path != null && android.appwidget.AppWidgetManager.getInstance(this@PlaybackService)
                    .getAppWidgetIds(android.content.ComponentName(this@PlaybackService, PlaybackWidget::class.java)).isNotEmpty()) {
                    widgetJob = serviceScope.launch {
                        val file = File(com.local.listentomusic.model.sourceMediaPath(path))
                        val item = com.local.listentomusic.model.MediaFile(path, file.name, 0, file.length(), file.lastModified(),
                            if (file.extension.lowercase() in MediaScanner.videoExtensions) com.local.listentomusic.model.MediaKind.VIDEO else com.local.listentomusic.model.MediaKind.AUDIO, sourcePath = file.path,
                            coverUri = preferences.current().localOverrides[path]?.coverUri.orEmpty())
                        widgetArtwork = com.local.listentomusic.data.ThumbnailRepository(this@PlaybackService).load(item)
                        PlaybackWidget.update(this@PlaybackService, player.mediaMetadata.title?.toString() ?: "Greater Art", player.isPlaying, widgetArtwork)
                    }
                }
                enhancer?.release()
                enhancer = null
                player.volume = mixLevel(1f, layers.size)
                scheduleSave()
            }
            override fun onPlaybackStateChanged(playbackState: Int) { scheduleSave(); publishDiagnostics() }
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) = scheduleSave()
            override fun onRepeatModeChanged(repeatMode: Int) = scheduleSave()
            override fun onPlayerError(error: PlaybackException) {
                val failedPath = player.currentMediaItem?.mediaId
                if (!failedPath.isNullOrBlank() && retriedPath != failedPath) {
                    // One retry covers transient decoder/audio-route failures without looping forever.
                    retriedPath = failedPath
                    val retryPosition = player.currentPosition.coerceAtLeast(0L)
                    player.prepare()
                    player.seekTo(retryPosition)
                    player.playWhenReady = true
                } else if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
                    // A corrupt or unsupported item must not strand the rest of the queue.
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        serviceScope.launch {
            preferences.values.collect { savedPreferences ->
                if (gainEnabled != savedPreferences.replayGainEnabled) {
                    gainEnabled = savedPreferences.replayGainEnabled
                    applyGain()
                }
            }
        }
        serviceScope.launch {
            while (isActive) {
                val range = PracticeLoop.state.value
                if (range.path != null && range.path != player.currentMediaItem?.mediaId) PracticeLoop.clear()
                else if (player.isPlaying && range.start != null && range.end != null && player.currentPosition >= range.end) player.seekTo(range.start)
                delay(50)
            }
        }
        serviceScope.launch {
            while (isActive) {
                delay(5_000)
                if (player.currentMediaItem != null) saveNow()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveNow()
        // Do not stop: MediaSessionService keeps active audio available in the notification.
    }

    override fun onDestroy() {
        TemporaryPlaybackSpeed.end()
        TemporaryPlaybackSpeed.detach()
        ParallelPlayback.detach()
        layers.toList().forEach { layer -> removeSession(layer.session); layer.session.release(); layer.player.release() }
        layers.clear()
        audioManager.abandonAudioFocusRequest(focusRequest)
        enhancer?.release()
        enhancer = null
        saveJob?.cancel()
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        // Player is main-thread-only; read state here, then only the disk write runs on IO.
        val savedPath = player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() }
        val savedPosition = player.currentPosition
        val savedSpeed = speedBeforeTemporaryHold ?: player.playbackParameters.speed
        val savedRepeat = player.repeatMode
        runBlocking(Dispatchers.IO) {
            preferences.savePlayback(
                path = savedPath,
                positionMs = savedPosition,
                speed = savedSpeed,
                repeatMode = savedRepeat,
            )
        }
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun restoreLastSession(saved: UserPreferences) {
        player.playbackParameters = androidx.media3.common.PlaybackParameters(saved.playbackSpeed)
        player.repeatMode = saved.repeatMode
        val path = saved.lastPath ?: return
        val file = File(com.local.listentomusic.model.sourceMediaPath(path))
        if (!file.isFile || !file.canRead() || !MediaScanner.isInsideTarget(file)) return

        val item = MediaItem.Builder()
            .setMediaId(path)
            .setUri(android.net.Uri.fromFile(file))
            .setClippingConfiguration(clipConfiguration(path))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(file.name).build())
            .build()
        player.setMediaItem(item, if (saved.resumePlayback) saved.lastPositionMs else 0L)
        player.prepare()
        player.playWhenReady = false
    }

    private fun applyGain() {
        enhancer?.release()
        enhancer = null
        player.volume = 1f
        if (!gainEnabled) { applyMixLevels(); return }
        val gain = replayGainDb(player.currentTracks)
        if (gain <= 0) player.volume = Math.pow(10.0, gain / 20.0).toFloat()
        else if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            runCatching {
                android.media.audiofx.LoudnessEnhancer(player.audioSessionId).also {
                    enhancer = it
                    it.setTargetGain((gain * 100).toInt())
                    it.enabled = true
                }
            }.onFailure { enhancer?.release(); enhancer = null }
        }
        player.volume /= (layers.size + 1).toFloat()
    }

    private fun ensureFocus() {
        if (focusHeld) return
        focusHeld = runCatching { audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED }.getOrDefault(false)
        if (!focusHeld) pauseEveryPlayer()
    }

    private fun pauseEveryPlayer() {
        if (::player.isInitialized) player.pause()
        layers.forEach { it.player.pause() }
        publishLayers()
    }

    private fun publishLayers() {
        ParallelPlayback.publish(layers.map { MixLayer(it.id, it.title, it.player.playWhenReady, it.level, it.player.currentMediaItem?.mediaId.orEmpty()) })
        publishDiagnostics()
    }

    private fun publishDiagnostics() {
        if (!::player.isInitialized) return
        PlaybackDiagnostics.report.value = buildString {
            val audio = player.audioFormat
            appendLine("audio mime=${audio?.sampleMimeType ?: "unknown"} codec=${audio?.codecs ?: "unknown"}")
            appendLine("source sampleRate=${audio?.sampleRate ?: -1} channels=${audio?.channelCount ?: -1} pcmEncoding=${audio?.pcmEncoding ?: -1} bitrate=${audio?.bitrate ?: -1}")
            appendLine("audioSession=${player.audioSessionId} speed=${player.playbackParameters.speed} focusHeld=$focusHeld")
            appendLine("voices=${layers.size + 1}/10 extraPlaying=${layers.count { it.player.isPlaying }}")
            appendLine("mainGain=${player.volume} extraBufferLimit=2MiB/voice")
            appendLine("actual DAC format / bit-perfect output: not observable here")
            appendLine("availableOutputs=" + getSystemService(android.media.AudioManager::class.java).getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).joinToString { "type:${it.type} channels:${it.channelCounts.joinToString()}" })
        }
    }

    private fun clipConfiguration(path: String): MediaItem.ClippingConfiguration {
        val clip = com.local.listentomusic.model.cueBounds(path)
        return MediaItem.ClippingConfiguration.Builder().apply {
            clip?.let { setStartPositionMs(it.first); it.second?.let { end -> setEndPositionMs(end) } }
        }.build()
    }

    private fun applyMixLevels() {
        if (!gainEnabled) player.volume = mixLevel(1f, layers.size)
        layers.forEach { it.player.volume = mixLevel(it.level, layers.size) }
    }

    private fun addLayer(path: String) {
        if (layers.size >= ParallelPlayback.MAX_EXTRA_LAYERS) return
        val file = File(com.local.listentomusic.model.sourceMediaPath(path))
        if (!MediaScanner.isInsideTarget(file) || !file.isFile || file.extension.lowercase() !in MediaScanner.supportedExtensions) return
        var extra: ExoPlayer? = null
        var session: MediaSession? = null
        runCatching {
            val id = java.util.UUID.randomUUID().toString()
            val engine = ExoPlayer.Builder(this, DefaultRenderersFactory(this).setEnableDecoderFallback(true))
                .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1_000, 5_000, 100, 200)
                    .setTargetBufferBytes(2 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).build())
                .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
                .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build()
            extra = engine
            engine.trackSelectionParameters = engine.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
            engine.repeatMode = Player.REPEAT_MODE_ONE
            val mediaSession = MediaSession.Builder(this, engine).setId("mix-$id").build()
            session = mediaSession
            layers.add(LayerPlayer(id, file.nameWithoutExtension, engine, mediaSession))
            addSession(mediaSession)
            engine.addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) ensureFocus()
                    else if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) pauseEveryPlayer()
                    publishLayers()
                }
                override fun onPlayerError(error: PlaybackException) {
                    Handler(mainLooper).post { removeLayer(id) }
                    android.widget.Toast.makeText(this@PlaybackService, "Could not play this layer on this device", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
            applyGain(); applyMixLevels()
            engine.setMediaItem(MediaItem.Builder().setMediaId(path).setUri(android.net.Uri.fromFile(file))
                .setClippingConfiguration(clipConfiguration(path))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(file.nameWithoutExtension).build()).build())
            engine.prepare(); engine.play()
            publishLayers()
        }.onFailure {
            layers.removeAll { it.player === extra }
            session?.let { removeSession(it); it.release() }
            extra?.release()
            applyGain(); applyMixLevels(); publishLayers()
            android.widget.Toast.makeText(this, "This device cannot open another layer", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeLayer(id: String) {
        val layer = layers.firstOrNull { it.id == id } ?: return
        layers.remove(layer)
        removeSession(layer.session); layer.session.release(); layer.player.release()
        applyGain(); applyMixLevels(); publishLayers()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = serviceScope.launch {
            delay(400)
            saveNow()
        }
    }

    private fun saveNow() {
        if (!::player.isInitialized) return
        val path = player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() }
        val position = player.currentPosition
        val speed = speedBeforeTemporaryHold ?: player.playbackParameters.speed
        val repeat = player.repeatMode
        serviceScope.launch(Dispatchers.IO) {
            preferences.savePlayback(path, position, speed, repeat)
        }
    }

    private fun isBluetoothOutput(device: AudioDeviceInfo): Boolean = when (device.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID -> device.isSink
        else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.isSink &&
            device.type in setOf(AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER)
    }
}
