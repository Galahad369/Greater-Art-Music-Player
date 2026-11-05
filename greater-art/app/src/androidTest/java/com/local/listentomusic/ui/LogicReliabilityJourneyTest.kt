package com.local.listentomusic.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.DocumentsContract
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.media3.common.Player
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.LibraryStatus
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.SavedStack
import com.local.listentomusic.data.SavedStackTrack
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackService
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Actual app buttons, on-device storage/provider calls, and a real playback service. */
class LogicReliabilityJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrument get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrument.targetContext

    @Test fun tappingSelectedSongAfterEarlierFileDisappearsStartsThatSong() = journey { root, prefs, open ->
        val gone = wave(root, "GA01 missing")
        val selected = wave(root, "GA02 selected")
        val other = wave(root, "GA03 other")
        val id = prefs.createPlaylistWithPaths(root.name, listOf(gone.path, selected.path, other.path))
        try {
            prefs.setActivePlaylist(id)
            val model = open()
            await("Fixture playlist visible") { model.library.value.files.map { it.path } == listOf(gone.path, selected.path, other.path) }
            onMain { model.stopLibraryObservation() }
            assertTrue("Test fixture move failed", gone.renameTo(File(root, "moved-away.tmp")))
            compose.onNodeWithText("GA02 selected", substring = true).performClick()
            await("Service started a fixture") {
                onMain { model.controller.value?.let { it.playbackState == Player.STATE_READY &&
                    it.currentMediaItem?.mediaId in listOf(selected.path, other.path) } == true }
            }
            assertEquals("WRONG_SONG: earlier unreadable entry must not move the tap", selected.path,
                onMain { model.controller.value?.currentMediaItem?.mediaId })
        } finally { prefs.deletePlaylist(id) }
    }

    @Test fun removeReferenceThenSaveAndReopenMix() = journey { root, prefs, open ->
        val primary = wave(root, "GA01 reference")
        val ahead = wave(root, "GA02 ahead")
        val behind = wave(root, "GA03 behind")
        val label = root.name + "-seed"
        val savedName = root.name + "-reopen"
        val seed = SavedStack(root.name, label, listOf(SavedStackTrack(primary.path),
            SavedStackTrack(ahead.path, offsetMs = 30_000),
            SavedStackTrack(behind.path, offsetMs = -30_000)), primary.path)
        prefs.saveStack(seed)
        try {
            val model = open()
            await("Three test tracks scanned") { model.stackFiles.value.count { it.sourcePath.startsWith(root.path + "/") } == 3 }
            compose.onNodeWithContentDescription("Stack").performClick()
            compose.onNodeWithContentDescription("Saved Stacks").performClick()
            compose.onNodeWithText(label).performClick()
            compose.onAllNodesWithContentDescription("Remove track")[0].performClick()
            compose.onNodeWithContentDescription("Save Stack").performClick()
            compose.onNode(hasText("Stack name (optional)")).performTextInput(savedName)
            compose.onNodeWithText("Save", substring = false).performClick()
            await("STACK_SAVE_LOST: saved draft should decode and reappear", 5_000L) {
                runBlocking { prefs.current().savedStacks.any { it.name == savedName } }
            }
            val stored = prefs.current().savedStacks.single { it.name == savedName }
            assertEquals(listOf(ahead.path, behind.path), stored.tracks.map { it.path })
            compose.onNodeWithContentDescription("Saved Stacks").performClick()
            compose.onNodeWithText(savedName).performClick()
        } finally {
            prefs.current().savedStacks.filter { it.id == seed.id || it.name == savedName }.forEach { prefs.deleteStack(it.id) }
        }
    }

    @Test fun expiredFolderGrantRecoveryOpensPickerAndReloadsTheFolder() = journey { root, prefs, open ->
        val item = wave(root, "GA scoped recovery")
        val tree = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Download/${root.name}")
        val resolver = context.contentResolver
        assertTrue("Fixture must start without a grant", resolver.persistedUriPermissions.none { it.uri == tree })
        prefs.setScopedMediaTreeUri(tree.toString())
        try {
            val model = open()
            await("Revoked folder recognized") { model.library.value.status == LibraryStatus.CANNOT_READ }
            val recovery = compose.onNode(hasText("Choose folder again") or hasText("Open settings"))
            recovery.performClick()
            await("FOLDER_RECOVERY: open Android's folder picker, not All Files Access settings", 30_000L) {
                instrument.uiAutomation.rootInActiveWindow?.packageName?.contains("documentsui") == true
            }
            await("Owned fixture folder shown") { clickNativeText(root.name) || nativeContains(root.name) }
            await("Tap actual Use this folder button") { clickNativeText("Use this folder") }
            await("Confirm Android folder grant") { clickNativeText("Allow") }
            await("Granted folder is actually readable and visible") {
                resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission } &&
                    model.library.value.files.any { it.name == item.name }
            }
        } finally {
            val grant = resolver.persistedUriPermissions.firstOrNull { it.uri == tree && it.isReadPermission }
            if (grant != null) resolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // If a baseline assertion failed in the system picker/settings, return to the app.
            val activePackage = instrument.uiAutomation.rootInActiveWindow?.packageName?.toString().orEmpty()
            if ("documentsui" in activePackage || activePackage == "com.android.settings") {
                val command = instrument.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK")
                ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
            }
        }
    }

    @Test fun damagedBackupLeavesSavedDataIntact(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("logicReliabilityBackup") == "true")
        val prefs = AppPreferences(context)
        val id = UUID.randomUUID().toString()
        val path = "/storage/emulated/0/Download/GA-QA-$id.wav"
        val seed = SavedStack(id, "GA-QA backup", listOf(SavedStackTrack(path)), path)
        prefs.saveStack(seed)
        val original = prefs.exportBackup()
        val invalid = JSONObject(original).put("saved_stacks_v1", "invalid-record").toString()
        try {
            assertTrue("A damaged Stack payload must reject the backup", runCatching { prefs.restoreBackup(invalid) }.isFailure)
            assertEquals(JSONObject(original).toString(), JSONObject(prefs.exportBackup()).toString())
            assertEquals(seed, prefs.current().savedStacks.single { it.id == id })
        } finally {
            if (prefs.exportBackup() != original) prefs.restoreBackup(original)
            prefs.deleteStack(id)
        }
    }

    private fun nativeNode(text: String): AccessibilityNodeInfo? {
        val root = instrument.uiAutomation.rootInActiveWindow ?: return null
        val nodes = ArrayDeque(listOf(root))
        while (nodes.isNotEmpty()) {
            val node = nodes.removeFirst()
            if (node.isVisibleToUser && node.text?.toString()?.equals(text, ignoreCase = true) == true) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(nodes::addLast)
        }
        return null
    }

    private fun nativeContains(text: String) = nativeNode(text) != null
    private fun clickNativeText(text: String): Boolean {
        var node = nativeNode(text)
        while (node != null && !node.isClickable) node = node.parent
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun journey(test: suspend (File, AppPreferences, () -> MainViewModel) -> Unit): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("logicReliabilityJourney") == "true")
        assertTrue("All Files Access is required only for owned fixtures on the QA emulator", Environment.isExternalStorageManager())
        val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GA-QA-${UUID.randomUUID().toString().take(8)}")
        assertTrue("Use a new owned fixture folder", root.mkdir())
        val prefs = AppPreferences(context)
        val old = prefs.current()
        var scenario: ActivityScenario<MainActivity>? = null
        var model: MainViewModel? = null
        try {
            prefs.setAppLanguage(AppLanguage.ENGLISH)
            prefs.setScopedMediaTreeUri(null)
            prefs.setActivePlaylist(null)
            prefs.setFloatingWindowMode(FloatingWindowMode.NONE)
            prefs.setBackgroundMode(AppBackgroundMode.DEFAULT)
            prefs.setResumePlayback(false)
            prefs.setAutoPictureInPicture(false)
            prefs.setBluetoothOnlyListening(false)
            test(root, prefs) {
                val current = ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
                scenario = current
                current.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
                requireNotNull(model)
            }
        } finally {
            onMain { model?.controller?.value?.takeIf { it.isConnected }?.let { it.pause(); it.clearMediaItems() } }
            if (scenario?.state?.isAtLeast(Lifecycle.State.CREATED) == true) scenario?.onActivity { it.finish() }
            scenario?.close()
            context.stopService(Intent(context, MiniWindowOverlayService::class.java))
            context.stopService(Intent(context, PlaybackService::class.java))
            instrument.waitForIdleSync()
            prefs.setAppLanguage(old.appLanguage)
            prefs.setScopedMediaTreeUri(old.scopedMediaTreeUri)
            prefs.setActivePlaylist(old.activePlaylistId)
            prefs.setFloatingWindowMode(old.floatingWindowMode)
            prefs.setBackgroundMode(old.backgroundMode)
            prefs.setResumePlayback(old.resumePlayback)
            prefs.setAutoPictureInPicture(old.autoPictureInPicture)
            prefs.setBluetoothOnlyListening(old.bluetoothOnlyListening)
            prefs.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
            root.listFiles()?.forEach { if (it.isFile) it.delete() }
            root.delete()
        }
    }

    private fun wave(root: File, title: String): File {
        val bytes = 8000 * 2 * 40
        val wav = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(bytes).array()
        return File(root, "$title.wav").apply { writeBytes(wav) }
    }

    private fun await(message: String, timeout: Long = 15_000L, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < until) {
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError(message)
    }

    private fun <T> onMain(block: () -> T): T = FutureTask(block).let { task ->
        instrument.runOnMainSync(task)
        task.get(10, TimeUnit.SECONDS)
    }
}
