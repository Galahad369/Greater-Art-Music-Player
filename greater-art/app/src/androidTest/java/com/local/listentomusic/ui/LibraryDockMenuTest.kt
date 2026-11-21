package com.local.listentomusic.ui

import android.content.Intent
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackConnection
import com.local.listentomusic.playback.PlaybackService
import com.local.listentomusic.playback.PlayerWindowVisibility
import com.local.listentomusic.playback.SharedPlaybackResource
import com.local.listentomusic.ui.components.CompactPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

/**
 * Long Library menus against the real MediaSession and WindowManager dock.
 * Does not mock focus, popup placement, touch input, or the dock visibility policy.
 * All inserted playlists and audio are UUID-owned and removed in finally.
 */
@SdkSuppress(minSdkVersion = 29)
class LibraryDockMenuTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun longPlaylistAndSelectionMenusKeepBottomActionsTappableOverRealDock(): Unit = runBlocking {
        assertTrue("Grant Display over other apps for the dock integration test",
            Settings.canDrawOverlays(context))
        val preferences = AppPreferences(context)
        val old = preferences.current()
        val prefix = "GA dock-menu " + UUID.randomUUID().toString().take(8)
        val fixture = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ga-dock-menu-" + UUID.randomUUID() + ".wav")
        val testPlaylistIds = mutableListOf<String>()
        var activity: ActivityScenario<MainActivity>? = null
        var connection: SharedPlaybackResource.Lease<ListenableFuture<MediaController>>? = null
        var controller: MediaController? = null
        try {
            withContext(Dispatchers.IO) { writeAudioFixture(fixture) }
            preferences.setAppLanguage(AppLanguage.ENGLISH)
            preferences.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
            preferences.setMiniWindowSize(LibraryRowSize.LARGE)
            preferences.setBackgroundMode(AppBackgroundMode.DEFAULT)
            preferences.setResumePlayback(false)
            preferences.setAutoPictureInPicture(false)
            preferences.setScopedMediaTreeUri(null)
            testPlaylistIds += preferences.createPlaylistWithPaths(prefix + " 01", listOf(fixture.path))
            preferences.setActivePlaylist(testPlaylistIds.first())

            val opened = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            activity = opened
            lateinit var viewModel: MainViewModel
            opened.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            await("Library initialized with the test playlist") {
                viewModel.controller.value != null &&
                    viewModel.settings.value.activePlaylistId == testPlaylistIds.first() &&
                    viewModel.settings.value.appLanguage == AppLanguage.ENGLISH
            }
            onMain { viewModel.rescan() }
            await("UUID audio indexed and visible in the active playlist") {
                viewModel.library.value.files.any { it.path == fixture.path }
            }

            val lease = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
            connection = lease
            val player = withContext(Dispatchers.IO) { lease.value.get(10, TimeUnit.SECONDS) }
            controller = player
            onMain {
                assertEquals("One current session controller", player, viewModel.controller.value)
                viewModel.play(MediaFile(fixture.path, fixture.name, 60_000L,
                    fixture.length(), fixture.lastModified(), MediaKind.AUDIO))
                player.repeatMode = Player.REPEAT_MODE_ONE
            }
            await("playing audio with the real LARGE dock") {
                onMain {
                    player.isPlaying && player.playbackState == Player.STATE_READY &&
                        player.currentMediaItem?.mediaId == fixture.path &&
                        PlayerWindowVisibility.dockedVisible.value &&
                        MiniWindowOverlayService.modeSnapshot.value?.name == "DOCKED" &&
                        MiniWindowOverlayService.destinationReady.value &&
                        dockWindow()?.let { it.alpha == 1f && !it.untouchable } == true
                }
            }
            val dockBefore = onMain { dockWindow() }
            assertNotNull("A real TYPE_APPLICATION_OVERLAY dock is required", dockBefore)

            // Enter selection using the existing short song-list dialog before
            // growing the playlist count; this test targets DropdownMenu, not
            // the separate multi-button AlertDialog's height behavior.
            compose.onNodeWithContentDescription("Actions").performClick()
            compose.onNodeWithContentDescription("Add to list").performClick()
            compose.onNodeWithText("Select multiple").performClick()
            compose.onNodeWithText("Add to…").assertIsDisplayed()

            repeat(11) { index ->
                testPlaylistIds += preferences.createPlaylist(prefix + " " + (index + 2).toString().padStart(2, '0'))
            }
            await("12 temporary playlists rendered") {
                viewModel.settings.value.playlists.count { it.name.startsWith(prefix) } == 12
            }

            // Material3's scrollable popup must expose its last action even with
            // the system overlay window in the same display.
            compose.onNodeWithText("Add to…").performClick()
            reportDock("selection popup")
            compose.onNodeWithText("Share selected media files").performScrollTo()
                .assertIsDisplayed().performClick()
            compose.onNodeWithText("Share original media files?").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            awaitDock("selection popup dismissed")
            compose.onNodeWithText("Clear").performClick()

            compose.onNodeWithContentDescription("Playlists").performClick()
            reportDock("playlist popup")
            compose.onNodeWithText("Delete list").performScrollTo()
                .assertIsDisplayed().performClick()
            compose.onNodeWithText("Delete playlist?").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            awaitDock("playlist popup dismissed")

            val dockAfter = onMain { dockWindow() }
            assertNotNull(dockAfter)
            assertEquals("Popup interaction must not recreate the dock",
                dockBefore?.identity, dockAfter?.identity)
            onMain {
                assertEquals("Popup must retain the same audio", fixture.path,
                    player.currentMediaItem?.mediaId)
                assertTrue("Popup must not interrupt playback", player.isPlaying)
                assertEquals(Player.STATE_READY, player.playbackState)
            }
        } finally {
            try {
                onMain { controller?.takeIf { it.isConnected }?.let { it.pause(); it.clearMediaItems() } }
                context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                activity?.close()
                onMain { connection?.close() }
                context.stopService(Intent(context, PlaybackService::class.java))
                instrumentation.waitForIdleSync()
            } finally {
                // Never clear arbitrary user playlists, saved media or Android grants.
                testPlaylistIds.asReversed().forEach { preferences.deletePlaylist(it) }
                preferences.setActivePlaylist(old.activePlaylistId)
                preferences.setAppLanguage(old.appLanguage)
                preferences.setFloatingWindowMode(old.floatingWindowMode)
                preferences.setMiniWindowSize(old.miniWindowSize)
                preferences.setBackgroundMode(old.backgroundMode)
                preferences.setResumePlayback(old.resumePlayback)
                preferences.setAutoPictureInPicture(old.autoPictureInPicture)
                preferences.setScopedMediaTreeUri(old.scopedMediaTreeUri)
                preferences.savePlayback(old.lastPath, old.lastPositionMs,
                    old.playbackSpeed, old.repeatMode)
                withContext(Dispatchers.IO) {
                    assertTrue("Only the UUID-owned audio fixture may be removed",
                        !fixture.exists() || fixture.delete())
                }
            }
        }
    }

    private data class DockWindow(val identity: Int, val alpha: Float, val untouchable: Boolean)

    private fun dockWindow(): DockWindow? {
        val root = WindowInspector.getGlobalWindowViews().singleOrNull { view ->
            (view.layoutParams as? WindowManager.LayoutParams)?.type ==
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY && containsCompactPlayer(view)
        } ?: return null
        val params = root.layoutParams as WindowManager.LayoutParams
        return DockWindow(System.identityHashCode(root), params.alpha,
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
    }

    private fun containsCompactPlayer(view: View): Boolean {
        if (view is CompactPlayerView) return true
        if (view !is ViewGroup) return false
        for (index in 0 until view.childCount) {
            if (containsCompactPlayer(view.getChildAt(index))) return true
        }
        return false
    }

    private fun reportDock(context: String) {
        println("LIBRARY_DOCK_MENU: " + context + " dock=" + onMain { dockWindow() } +
            " visibleState=" + PlayerWindowVisibility.dockedVisible.value)
    }

    private suspend fun awaitDock(context: String) {
        await(context) {
            onMain {
                PlayerWindowVisibility.dockedVisible.value &&
                    dockWindow()?.let { it.alpha == 1f && !it.untouchable } == true
            }
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val task = FutureTask(block)
        instrumentation.runOnMainSync(task)
        return task.get(5, TimeUnit.SECONDS)
    }

    private suspend fun await(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 30_000L
        while (!condition()) {
            assertTrue("Timed out waiting for " + label, SystemClock.elapsedRealtime() < deadline)
            delay(40)
        }
    }

    private fun writeAudioFixture(file: File) {
        val sampleRate = 48_000
        val dataBytes = sampleRate * 60 * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(dataBytes + 36)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII)); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray(Charsets.US_ASCII)); putInt(dataBytes)
        }.array()
        val oneSecond = ByteBuffer.allocate(sampleRate * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            repeat(sampleRate) { sample ->
                putShort((sin(2 * PI * 440 * sample / sampleRate) * 500).toInt().toShort())
            }
        }.array()
        file.outputStream().buffered().use { output ->
            output.write(header)
            repeat(60) { output.write(oneSecond) }
        }
    }
}
