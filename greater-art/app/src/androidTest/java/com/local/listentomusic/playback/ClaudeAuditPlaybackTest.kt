package com.local.listentomusic.playback

import android.content.ComponentName
import android.content.Intent
import android.os.Environment
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Owned silent PCM fixtures exercise real decoders/commands, not acoustic alignment. */
class ClaudeAuditPlaybackTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var root: File
    private lateinit var main: ExoPlayer
    private lateinit var coordinator: StackPlaybackCoordinator
    private lateinit var scope: CoroutineScope
    private var controller: MediaController? = null
    private var foreground: androidx.test.core.app.ActivityScenario<com.local.listentomusic.MainActivity>? = null
    private var originalPreferences: com.local.listentomusic.data.UserPreferences? = null

    @Before fun fixtures() {
        root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GA-Audit-${UUID.randomUUID()}")
        check(root.mkdirs())
        runBlocking {
            val prefs = AppPreferences(context)
            originalPreferences = prefs.current()
            prefs.setScopedMediaTreeUri(null)
            prefs.setExcludedFolders(emptyList())
            prefs.setActivePlaylist(null)
            prefs.setResumePlayback(false)
            prefs.setFloatingWindowMode(com.local.listentomusic.data.FloatingWindowMode.NONE)
            prefs.setBackgroundMode(com.local.listentomusic.data.AppBackgroundMode.DEFAULT)
            prefs.setDeveloperMode(false)
        }
    }

    @After fun cleanup() {
        instrumentation.runOnMainSync {
            PlaybackSleepTimer.cancel()
            if (::coordinator.isInitialized) { coordinator.release(); main.release(); scope.cancel() }
            else StackPlayback.stop()
            controller?.release()
        }
        foreground?.close()
        foreground = null
        context.stopService(Intent(context, MiniWindowOverlayService::class.java))
        context.stopService(Intent(context, PlaybackService::class.java))
        instrumentation.waitForIdleSync()
        originalPreferences?.let { old -> runBlocking {
            val prefs = AppPreferences(context)
            prefs.setScopedMediaTreeUri(old.scopedMediaTreeUri)
            prefs.setExcludedFolders(old.excludedFolders)
            prefs.setActivePlaylist(old.activePlaylistId)
            prefs.setResumePlayback(old.resumePlayback)
            prefs.setFloatingWindowMode(old.floatingWindowMode)
            prefs.setBackgroundMode(old.backgroundMode)
            prefs.setDeveloperMode(old.developerMode)
            prefs.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
        } }
        root.deleteRecursively() // only this test's UUID-owned synthetic files
    }

    private fun wave(name: String, durationMs: Long): MediaFile {
        val bytes = (durationMs * 48_000 / 1000 * 2).toInt()
        val data = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(48_000).putInt(96_000)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(bytes)
        val file = File(root, "$name.wav").apply { writeBytes(data.array()) }
        return MediaFile(file.path, file.name, durationMs, file.length(), file.lastModified(), MediaKind.AUDIO)
    }

    private fun direct(factory: ((android.content.Context) -> ExoPlayer)? = null) = instrumentation.runOnMainSync {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        main = ExoPlayer.Builder(context).build()
        coordinator = if (factory == null) StackPlaybackCoordinator(context, main, scope) { }
            else StackPlaybackCoordinator(context, main, scope, factory) { }
        main.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) { coordinator.onMainPlayChanged(ready) }
            override fun onIsPlayingChanged(playing: Boolean) { coordinator.onMainIsPlayingChanged(playing) }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) coordinator.onMainReady()
                if (state == Player.STATE_ENDED) coordinator.onPrimaryEnded()
            }
        })
    }

    private fun await(message: String, condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 15_000
        while (!condition() && SystemClock.elapsedRealtime() < end) Thread.sleep(50)
        assertTrue("$message: ${StackPlayback.state.value}", condition())
    }

    private fun connect(): MediaController {
        // Android 15+ rejects a background-only audio-focus request. Exercise
        // the real user entry point, not a service started behind the launcher.
        foreground = androidx.test.core.app.ActivityScenario.launch(
            Intent(context, com.local.listentomusic.MainActivity::class.java))
        val result = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .buildAsync().get(15, TimeUnit.SECONDS)
        controller = result
        return result
    }

    @Test fun pauseDuringGatePreventsDelayedStart() {
        val files = listOf(wave("one", 5000), wave("two", 5000))
        direct()
        instrumentation.runOnMainSync {
            assertTrue(coordinator.start(files))
            assertTrue(StackPlayback.state.value.synchronizing)
            coordinator.onMainPlayChanged(false)
            assertTrue("Internal parked callback is not a user Pause", StackPlayback.state.value.playing)
            coordinator.pause()
        }
        Thread.sleep(STACK_START_GATE_TIMEOUT_MS + 250)
        instrumentation.runOnMainSync {
            assertFalse(StackPlayback.state.value.playing)
            assertFalse(StackPlayback.state.value.synchronizing)
            assertFalse(main.playWhenReady)
            assertEquals(0, StackPlayback.state.value.runningTracks)
        }
    }

    @Test fun pausedStackSeekStaysPausedThenResumesAllVoices() {
        val files = listOf(wave("one", 8000), wave("two", 8000))
        direct()
        instrumentation.runOnMainSync { assertTrue(coordinator.start(files)) }
        await("two voices start") { StackPlayback.state.value.runningTracks == 2 }
        instrumentation.runOnMainSync { coordinator.pause() }
        Thread.sleep(600)
        instrumentation.runOnMainSync {
            coordinator.seek(1000)
            assertFalse(StackPlayback.state.value.playing)
            assertFalse(main.playWhenReady)
        }
        Thread.sleep(300)
        instrumentation.runOnMainSync {
            assertFalse(main.playWhenReady)
            coordinator.play()
        }
        await("both voices resume after idle seek") { StackPlayback.state.value.runningTracks == 2 }
        instrumentation.runOnMainSync { assertTrue(main.isPlaying) }
    }

    @Test fun allocationFailureKeepsTheWorkingStackAndPrimary() {
        val old = listOf(wave("one", 5000), wave("two", 5000))
        val replacement = listOf(wave("three", 5000), wave("four", 5000))
        var fail = false
        direct { if (fail) error("Synthetic companion allocation failure") else ExoPlayer.Builder(it).build() }
        instrumentation.runOnMainSync { assertTrue(coordinator.start(old)) }
        await("two voices start") { StackPlayback.state.value.runningTracks == 2 }
        instrumentation.runOnMainSync {
            fail = true
            assertFalse(coordinator.start(replacement))
            assertEquals(old.map { it.path }, StackPlayback.state.value.slots.map { it.file.path })
            assertEquals(old.first().path, main.currentMediaItem?.mediaId)
            assertTrue(StackPlayback.state.value.playing)
        }
        await("old voices survive failed replacement") { StackPlayback.state.value.runningTracks == 2 }
    }

    @Test fun shorterPrimaryPromotesWithoutRewindingAndStopsAtTheSurvivingEof() {
        val files = listOf(wave("short", 800), wave("long", 2400))
        direct()
        instrumentation.runOnMainSync { assertTrue(coordinator.start(files)); coordinator.setLoop(false) }
        await("long take promoted") { StackPlayback.state.value.primaryPath == files[1].path }
        instrumentation.runOnMainSync { assertTrue("promotion must not replay intro", main.currentPosition >= 600) }
        await("non-looping stack ends") { !StackPlayback.state.value.playing }
        instrumentation.runOnMainSync {
            assertTrue("Survivor stopped early: ${StackPlayback.state.value}; player=${main.playbackState}/${main.currentPosition}/${main.playWhenReady}",
                StackPlayback.state.value.positionMs >= 2200)
        }
    }

    @Test fun removingPrimaryFromTwoTracksContinuesOrdinaryPlayback() {
        val files = listOf(wave("one", 5000), wave("two", 5000))
        direct()
        instrumentation.runOnMainSync { assertTrue(coordinator.start(files)) }
        await("two voices start") { StackPlayback.state.value.runningTracks == 2 }
        instrumentation.runOnMainSync {
            coordinator.remove(files[0].path)
            assertFalse(coordinator.active)
            assertEquals(files[1].path, main.currentMediaItem?.mediaId)
            assertTrue(main.playWhenReady)
        }
        var playing = false
        await("surviving ordinary player plays") { instrumentation.runOnMainSync { playing = main.isPlaying }; playing }
    }

    @Test fun retiredMediaCallbackCannotClearTheCurrentStack() {
        val files = listOf(wave("current", 5000), wave("companion", 5000))
        direct()
        instrumentation.runOnMainSync {
            assertTrue(coordinator.start(files))
            coordinator.onMainMediaChanged(null)
            coordinator.onMainMediaChanged("retired-media")
            assertTrue(coordinator.active)
            assertEquals(files.first().path, StackPlayback.state.value.primaryPath)
            main.setMediaItem(wave("replacement", 5000).toMediaItem())
            coordinator.onMainMediaChanged(main.currentMediaItem?.mediaId)
            assertFalse("A real replacement still exits Stack", coordinator.active)
        }
    }

    @Test fun activityReconnectCannotAppendOrdinaryLibraryItemsToAStack() {
        val files = listOf(wave("stack-primary", 15_000), wave("stack-take", 15_000))
        val remote = connect()
        instrumentation.runOnMainSync { assertTrue(StackPlayback.start(files)) }
        await("Stack running before Activity reconnect") { StackPlayback.state.value.runningTracks == 2 }
        foreground?.close()
        foreground = androidx.test.core.app.ActivityScenario.launch(
            Intent(context, com.local.listentomusic.MainActivity::class.java))
        lateinit var vm: com.local.listentomusic.MainViewModel
        foreground!!.onActivity { vm = androidx.lifecycle.ViewModelProvider(it)[com.local.listentomusic.MainViewModel::class.java] }
        await("reconnected Library has scanned the owned takes") {
            vm.controller.value != null && vm.library.value.files.any { it.path == files.first().path }
        }
        instrumentation.runOnMainSync {
            assertTrue(StackPlayback.state.value.active)
            assertEquals(1, remote.mediaItemCount)
            assertEquals(files.first().path, remote.currentMediaItem?.mediaId)
        }
    }

    @Test fun aLateControllerAppendCannotExpandTheActiveStackQueue() {
        val files = listOf(wave("primary", 15_000), wave("take", 15_000))
        val ordinary = wave("late-library-item", 15_000)
        val remote = connect()
        instrumentation.runOnMainSync { assertTrue(StackPlayback.start(files)) }
        await("Stack is ready") { StackPlayback.state.value.runningTracks == 2 }
        instrumentation.runOnMainSync { remote.addMediaItems(listOf(ordinary.toMediaItem())) }
        var queueSize = -1
        await("server acknowledges that Stack owns only its primary") {
            instrumentation.runOnMainSync { queueSize = remote.mediaItemCount }
            queueSize == 1
        }
        instrumentation.runOnMainSync {
            assertTrue(StackPlayback.state.value.active)
            assertEquals(files.first().path, remote.currentMediaItem?.mediaId)
        }
    }

    @Test fun mediaControllerPauseWhileAlreadyParkedCancelsTheGate() {
        val files = listOf(wave("one", 5000), wave("two", 5000))
        val remote = connect()
        instrumentation.runOnMainSync {
            assertTrue(StackPlayback.start(files))
            assertTrue(StackPlayback.state.value.synchronizing)
            remote.pause()
        }
        Thread.sleep(STACK_START_GATE_TIMEOUT_MS + 500)
        assertFalse(StackPlayback.state.value.playing)
        assertEquals(0, StackPlayback.state.value.runningTracks)
    }

    @Test fun endOfTrackSleepStopsStackBeforePromotingItsLongerCompanion() {
        val files = listOf(wave("short", 1000), wave("long", 4000))
        connect()
        instrumentation.runOnMainSync { PlaybackSleepTimer.set(-1); assertTrue(StackPlayback.start(files)) }
        await("end timer fires") { !PlaybackSleepTimer.state.value.active }
        // The timer flow is cleared before the same main-loop callback pauses
        // players. Observe after that callback, not halfway through its updates.
        instrumentation.runOnMainSync {
            assertFalse(StackPlayback.state.value.playing)
            assertEquals(files[0].path, StackPlayback.state.value.primaryPath)
        }
        await("all singers paused") { StackPlayback.state.value.runningTracks == 0 }
    }

    @Test fun corruptBackupCannotPartiallyApplyItsEarlierValidFields(): Unit = runBlocking {
        val prefs = AppPreferences(context)
        val original = prefs.exportBackup()
        val before = prefs.current()
        val bad = org.json.JSONObject(original).put("show_thumbnails", "not-a-boolean")
            .put("theme_mode", "LIGHT").toString()
        try {
            var rejected = false
            try { prefs.restoreBackup(bad) } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            assertEquals(before, prefs.current())
        } finally { prefs.restoreBackup(original) }
    }

    @Test fun realContentQueuePlaysAndRevokedOrDeletedContentIsRejected() {
        val source = wave("uri", 5000)
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "GA-Audit-${UUID.randomUUID()}.wav")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/GreaterArtAudit/")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { it.write(File(source.path).readBytes()) }
            resolver.update(uri, android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
            val item = source.copy(path = uri.toString(), sourcePath = uri.toString()).toMediaItem()
            assertTrue(readableInternalMediaItem(context, item))
            val remote = connect()
            instrumentation.runOnMainSync { remote.setMediaItem(item); remote.prepare(); remote.play() }
            var playing = false
            await("URI admitted and decoded by the real session") {
                instrumentation.runOnMainSync { playing = remote.isPlaying && remote.currentMediaItem?.mediaId == uri.toString() }
                playing
            }
            resolver.delete(uri, null, null)
            assertFalse(readableInternalMediaItem(context, item))
        } finally { resolver.delete(uri, null, null) }
    }

    @Test fun countdownSurvivesActivityClosureButClearsOnServiceDestruction() {
        connect()
        instrumentation.runOnMainSync { PlaybackSleepTimer.set(5) }
        foreground?.close()
        foreground = null
        Thread.sleep(1200)
        assertTrue(PlaybackSleepTimer.state.value.active)
        assertTrue(PlaybackSleepTimer.state.value.remainingMs in 1 until 300_000L)
        context.stopService(Intent(context, MiniWindowOverlayService::class.java))
        instrumentation.runOnMainSync { controller?.release(); controller = null }
        context.stopService(Intent(context, PlaybackService::class.java))
        await("service destruction clears its in-process timer") { !PlaybackSleepTimer.state.value.active }
    }

    @Test fun endTimerStopsRepeatOneWithoutChangingTheRepeatPreference() {
        val file = wave("repeat", 600)
        val remote = connect()
        instrumentation.runOnMainSync {
            remote.repeatMode = Player.REPEAT_MODE_ONE
            remote.setMediaItem(file.toMediaItem()); remote.prepare()
            PlaybackSleepTimer.set(-1)
            remote.play()
        }
        await("Repeat One reaches end timer") { !PlaybackSleepTimer.state.value.active }
        instrumentation.runOnMainSync { assertFalse(remote.playWhenReady); assertEquals(Player.REPEAT_MODE_ONE, remote.repeatMode) }
    }

    @Test fun endTimerStopsOnTheFinalPlaylistItem() {
        val files = listOf(wave("first", 500), wave("last", 700))
        val remote = connect()
        instrumentation.runOnMainSync {
            remote.repeatMode = Player.REPEAT_MODE_OFF
            remote.setMediaItems(files.map { it.toMediaItem() }, 1, 0)
            remote.prepare(); PlaybackSleepTimer.set(-1); remote.play()
        }
        await("final item reaches end timer") { !PlaybackSleepTimer.state.value.active }
        instrumentation.runOnMainSync {
            assertFalse(remote.playWhenReady)
            assertEquals(files.last().path, remote.currentMediaItem?.mediaId)
        }
    }
}
