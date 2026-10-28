package com.local.listentomusic.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackConnection
import com.local.listentomusic.playback.PlaybackService
import com.local.listentomusic.playback.PlayerWindowVisibility
import com.local.listentomusic.playback.SharedPlaybackResource
import com.local.listentomusic.ui.components.CompactPlayerView
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/** Real IME, Activity, shared MediaSession and WindowManager dock; no visibility-policy mocks. */
@SdkSuppress(minSdkVersion = 29) // WindowInspector is the public API for the actual overlay roots.
class LibraryImeDockTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun audioSearchHidesAndRestoresDockWithoutRestartingPlayback(): Unit = runBlocking {
        val fixture = File("/sdcard/Download/ga-library-ime-${UUID.randomUUID()}.wav")
        try {
            withContext(Dispatchers.IO) { writeAudioFixture(fixture) }
            exerciseSearch(fixture, MediaKind.AUDIO, 60_000L)
        } finally {
            // Never remove a shared directory or an existing user's media fixture.
            withContext(Dispatchers.IO) {
                assertTrue("Cannot remove the test-owned UUID audio fixture", !fixture.exists() || fixture.delete())
            }
        }
    }

    @Test fun videoSearchKeepsPlayerAndPrimarySurfaceWhileDockIsHidden(): Unit = runBlocking {
        val path = InstrumentationRegistry.getArguments().getString("libraryImeVideoFixture")
            ?: "/sdcard/Download/Baka self cover.mp4"
        val fixture = File(path)
        assertTrue("Provision a real video with -e libraryImeVideoFixture <path> before this test", fixture.isFile)
        val duration = withContext(Dispatchers.IO) {
            val reader = MediaMetadataRetriever()
            try {
                reader.setDataSource(fixture.path)
                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                reader.release()
            }
        }
        assertTrue("The real video must have at least 30 seconds of playback headroom", duration >= 30_000L)
        exerciseSearch(fixture, MediaKind.VIDEO, duration)
    }

    private suspend fun exerciseSearch(fixture: File, kind: MediaKind, duration: Long) {
        assertTrue("Grant Display over other apps before running the dock integration tests", Settings.canDrawOverlays(context))
        val preferences = AppPreferences(context)
        val old = preferences.current()
        val background = InstrumentationRegistry.getArguments().getString("libraryImeBackground")
            ?.let(AppBackgroundMode::valueOf) ?: AppBackgroundMode.DEFAULT
        var activity: ActivityScenario<MainActivity>? = null
        var connection: SharedPlaybackResource.Lease<ListenableFuture<MediaController>>? = null
        var controller: MediaController? = null
        try {
            preferences.setAppLanguage(AppLanguage.ENGLISH)
            preferences.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
            preferences.setBackgroundMode(background)
            preferences.setDeveloperMode(false)
            preferences.setResumePlayback(false)
            preferences.setAutoPictureInPicture(false)
            preferences.setScopedMediaTreeUri(null)
            activity = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            val main = activity
            lateinit var viewModel: MainViewModel
            val landscape = InstrumentationRegistry.getArguments().getString("libraryImeLandscape") == "true"
            main.onActivity {
                if (landscape) it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                viewModel = ViewModelProvider(it)[MainViewModel::class.java]
            }
            await("MainActivity preference and MediaSession initialization") {
                var orientationReady = true
                if (landscape) main.onActivity { orientationReady = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
                orientationReady && viewModel.controller.value != null && viewModel.settings.value.let {
                    it.appLanguage == AppLanguage.ENGLISH && it.floatingWindowMode == FloatingWindowMode.MINI_WINDOW &&
                        it.backgroundMode == background && !it.resumePlayback && !it.developerMode
                }
            }
            val lease = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
            connection = lease
            val player = withContext(Dispatchers.IO) { lease.value.get(10, TimeUnit.SECONDS) }
            controller = player
            onMain {
                assertSame("The Activity and test must borrow the existing session controller", player, viewModel.controller.value)
                viewModel.play(MediaFile(fixture.path, fixture.name, duration, fixture.length(), fixture.lastModified(), kind))
                player.repeatMode = Player.REPEAT_MODE_ONE
            }
            await("real playback and visible dock") {
                onMain {
                    player.isPlaying && player.playbackState == Player.STATE_READY &&
                        player.currentMediaItem?.mediaId == fixture.path &&
                        PlayerWindowVisibility.libraryShowing.value && PlayerWindowVisibility.dockedVisible.value &&
                        MiniWindowOverlayService.modeSnapshot.value?.name == "DOCKED" &&
                        MiniWindowOverlayService.destinationReady.value && dockWindow()?.let { it.alpha == 1f && !it.untouchable } == true &&
                        (kind != MediaKind.VIDEO || (player.videoSize.width > 0 && player.videoSize.height > 0 &&
                            VideoSurfaceOwner.state.value.owner == "MINI_WINDOW" && VideoSurfaceOwner.state.value.firstFrame))
                }
            }
            val baseline = onMain { snapshot(player) }
            assertNotNull("The compact player must be an actual overlay window", baseline.dock)

            // Two cycles catch an IME flag which is only updated on the first inset change.
            repeat(2) { cycle ->
                val cyclePosition = onMain { player.currentPosition }
                compose.onNode(hasSetTextAction()).performClick()
                await("actual IME shown and dock made invisible/non-touchable") {
                    var visible = false
                    main.onActivity { visible = ViewCompat.getRootWindowInsets(it.window.decorView)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) == true }
                    visible && PlayerWindowVisibility.keyboardShowing.value && !PlayerWindowVisibility.dockedVisible.value &&
                        PlayerWindowVisibility.dockSurfaceReserved.value && onMain {
                            dockWindow()?.let { it.alpha == 0f && it.untouchable } == true
                        }
                }
                // InputConnection edits plus a real, visible IME. This does not simulate vendor keyboard key taps.
                val query = "ga-ime-search-$cycle"
                compose.onNode(hasSetTextAction()).performTextInput(query)
                await("typed query reaches the real Library") { viewModel.library.value.query == query }
                compose.onNodeWithContentDescription("Clear filter").performClick()
                await("Clear empties the filter without closing the IME") {
                    var imeVisible = false
                    main.onActivity { imeVisible = ViewCompat.getRootWindowInsets(it.window.decorView)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) == true }
                    viewModel.library.value.query.isEmpty() && PlayerWindowVisibility.keyboardShowing.value && imeVisible
                }
                assertContinuity(baseline, onMain { snapshot(player) }, kind)
                assertFalse("An IME must not create a detached mini window", PlayerWindowVisibility.detachedVisible.value)
                await("playback advances while the dock is hidden") {
                    onMain { player.currentPosition > cyclePosition + 100L }
                }
                if (cycle == 0) {
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                } else {
                    main.onActivity { WindowCompat.getInsetsController(it.window, it.window.decorView).hide(WindowInsetsCompat.Type.ime()) }
                }
                await("IME dismissed and same dock visible/touchable again") {
                    var hidden = false
                    main.onActivity { hidden = ViewCompat.getRootWindowInsets(it.window.decorView)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) == false }
                    hidden && !PlayerWindowVisibility.keyboardShowing.value && PlayerWindowVisibility.dockedVisible.value &&
                        onMain { dockWindow()?.let { it.alpha == 1f && !it.untouchable } == true }
                }
                main.onActivity { assertTrue("Back must keep the Library Activity in the foreground", it.hasWindowFocus()) }
                assertTrue("Back must not leave Library", PlayerWindowVisibility.libraryShowing.value)
                assertContinuity(baseline, onMain { snapshot(player) }, kind)
            }
        } finally {
            try {
                onMain { controller?.takeIf { it.isConnected }?.let { it.pause(); it.clearMediaItems() } }
                context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                activity?.close()
                onMain { connection?.close() }
                context.stopService(Intent(context, PlaybackService::class.java))
                instrumentation.waitForIdleSync()
                // The final empty-queue write is asynchronous. Observe it before restoring the saved resume state.
                if (controller != null) await("test playback state cleared") { runBlocking { preferences.current().lastPath == null } }
            } finally {
                preferences.setAppLanguage(old.appLanguage)
                preferences.setFloatingWindowMode(old.floatingWindowMode)
                preferences.setBackgroundMode(old.backgroundMode)
                preferences.setDeveloperMode(old.developerMode)
                preferences.setResumePlayback(old.resumePlayback)
                preferences.setAutoPictureInPicture(old.autoPictureInPicture)
                preferences.setScopedMediaTreeUri(old.scopedMediaTreeUri)
                preferences.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
            }
        }
    }

    private data class DockSnapshot(val identity: Int, val alpha: Float, val untouchable: Boolean)
    private data class PlaybackSnapshot(
        val controller: Int, val mediaId: String?, val position: Long, val playing: Boolean, val state: Int,
        val serviceIdentities: String, val dock: DockSnapshot?, val owner: String, val generation: Long,
        val player: Int, val view: Int,
    )

    private fun snapshot(controller: MediaController): PlaybackSnapshot {
        val surface = VideoSurfaceOwner.state.value
        return PlaybackSnapshot(System.identityHashCode(controller), controller.currentMediaItem?.mediaId,
            controller.currentPosition, controller.isPlaying, controller.playbackState,
            MiniWindowOverlayService.identities.value, dockWindow(), surface.owner, surface.generation, surface.player, surface.view)
    }

    private fun assertContinuity(before: PlaybackSnapshot, after: PlaybackSnapshot, kind: MediaKind) {
        assertEquals("MediaController changed during search", before.controller, after.controller)
        assertEquals("Current media changed during search", before.mediaId, after.mediaId)
        assertEquals("Compact controller/PlayerView was recreated", before.serviceIdentities, after.serviceIdentities)
        assertEquals("WindowManager dock was recreated", before.dock?.identity, after.dock?.identity)
        assertTrue("Playback stopped during search", after.playing)
        assertEquals("Playback was re-prepared or stalled during search", Player.STATE_READY, after.state)
        assertTrue("Playback position reset during search", after.position >= before.position)
        if (kind == MediaKind.VIDEO) {
            assertEquals("Video surface generation changed during IME visibility", before.generation, after.generation)
            assertEquals("Video surface owner changed during IME visibility", before.owner, after.owner)
            assertEquals("Primary video player changed during IME visibility", before.player, after.player)
            assertEquals("Primary video view changed during IME visibility", before.view, after.view)
        }
    }

    /** The system IME is a different process. Only this app's real overlay root is selected. */
    private fun dockWindow(): DockSnapshot? {
        val root = WindowInspector.getGlobalWindowViews().singleOrNull { root ->
            (root.layoutParams as? WindowManager.LayoutParams)?.type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY &&
                containsCompactPlayer(root)
        } ?: return null
        val params = root.layoutParams as WindowManager.LayoutParams
        return DockSnapshot(System.identityHashCode(root), params.alpha,
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
    }

    private fun containsCompactPlayer(view: View): Boolean {
        if (view is CompactPlayerView) return true
        if (view !is ViewGroup) return false
        for (index in 0 until view.childCount) if (containsCompactPlayer(view.getChildAt(index))) return true
        return false
    }

    private fun <T> onMain(block: () -> T): T {
        val task = FutureTask(block)
        instrumentation.runOnMainSync(task)
        return task.get(5, TimeUnit.SECONDS)
    }

    private suspend fun await(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000L
        while (!condition()) {
            assertTrue("Timed out waiting for $label", SystemClock.elapsedRealtime() < deadline)
            delay(40)
        }
    }

    private fun writeAudioFixture(file: File) {
        val sampleRate = 48_000
        val bytes = sampleRate * 60 * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(bytes + 36)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII)); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray(Charsets.US_ASCII)); putInt(bytes)
        }.array()
        val second = ByteBuffer.allocate(sampleRate * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            repeat(sampleRate) { sample -> putShort((sin(2 * PI * 440 * sample / sampleRate) * 500).toInt().toShort()) }
        }.array()
        file.outputStream().buffered().use { output ->
            output.write(header)
            repeat(60) { output.write(second) }
        }
    }
}
