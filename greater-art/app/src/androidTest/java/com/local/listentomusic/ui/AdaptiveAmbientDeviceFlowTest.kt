package com.local.listentomusic.ui

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.*
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackService
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.ui.components.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Explicit owned fixture, no download and no arbitrary user media mutation. */
class AdaptiveAmbientDeviceFlowTest {
    @Test fun realPrimaryVideoFeedsAmbientWithoutReplacingItsPlayer(): Unit = runBlocking {
        assumeTrue("Explicit owned scene fixture opt-in",
            InstrumentationRegistry.getArguments().getString("ambientVideoFixture") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File("/sdcard/Download/GA-Ambient-Test/ga-ambient-scenes.mp4")
        assumeTrue("Generated red/blue/green 24s video required", source.isFile)
        val preferences = AppPreferences(context)
        val old = preferences.current()
        var scenario: ActivityScenario<MainActivity>? = null
        var ownedActivity: MainActivity? = null
        var controller: MediaController? = null
        val companion = File(source.parentFile, "ga-ambient-companion-test.mp4")
        var createdCompanion = false
        try {
            preferences.setScopedMediaTreeUri(null)
            preferences.setBackgroundMode(AppBackgroundMode.AMBIENT)
            preferences.setAdaptiveMotion(AmbientMotion.SUBTLE)
            preferences.setResumePlayback(false)
            preferences.setAutoPictureInPicture(false)
            scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            val viewModel = onMain {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().single()
                ownedActivity = activity
                ViewModelProvider(activity)[MainViewModel::class.java]
            }
            await { viewModel.controller.value != null }
            onMain {
                viewModel.play(MediaFile(source.path, source.name, 24000, source.length(), source.lastModified(), MediaKind.VIDEO))
            }
            await { VideoAmbientColors.state.value.mediaId == source.path }
            val player = onMain { requireNotNull(viewModel.controller.value) }
            controller = player
            val playerIdentity = System.identityHashCode(player)
            onMain { player.seekTo(1000); player.play() }
            await { hasColor(source.path, red = true) }
            val warm = VideoAmbientColors.state.value.pixels
            val lease = VideoSurfaceOwner.state.value
            resetFrames()
            Thread.sleep(1800)
            frames("classic-gfx.txt")
            resetFrames()
            preferences.setBackgroundMode(AppBackgroundMode.ADAPTIVE_DYNAMIC)
            Thread.sleep(1800)
            assertEquals("Ambient selection must not transfer the primary surface", lease.generation,
                VideoSurfaceOwner.state.value.generation)
            screenshot("adaptive-library-warm.png")
            onMain { player.seekTo(10000); player.play() }
            await { hasColor(source.path, red = false) }
            val cool = VideoAmbientColors.state.value.pixels
            Thread.sleep(1800)
            screenshot("adaptive-library-cool.png")
            frames("adaptive-gfx.txt")
            assertNotEquals(warm, cool)
            onMain {
                assertSame(player, viewModel.controller.value)
                assertEquals(playerIdentity, System.identityHashCode(player))
                assertEquals(1280, player.videoSize.width)
                assertEquals(720, player.videoSize.height)
                player.pause()
            }
            val paused = VideoAmbientColors.state.value
            Thread.sleep(1100)
            assertEquals(paused, VideoAmbientColors.state.value)
            android.util.Log.i("GA-AmbientDevice", "primary1280x720 red-to-blue compositor colors passed; Classic-to-Adaptive lease/player unchanged; pause holds")
            require(!companion.exists()) { "Owned companion fixture already exists; refusing overwrite" }
            source.copyTo(companion)
            createdCompanion = true
            onMain {
                assertTrue(StackPlayback.start(listOf(source, companion).map {
                    MediaFile(it.path, it.name, 24000, it.length(), it.lastModified(), MediaKind.VIDEO)
                }))
            }
            await { StackPlayback.state.value.runningTracks == 2 }
            assertTrue(StackPlayback.state.value.slots.none { it.error != null })
            Thread.sleep(1500)
            onMain {
                assertSame(player, viewModel.controller.value)
                assertEquals(1280, player.videoSize.width)
                assertEquals(720, player.videoSize.height)
            }
            screenshot("adaptive-stack-active.png")
            frames("adaptive-stack-gfx.txt")
            android.util.Log.i("GA-AmbientDevice", "Adaptive background with two video-backed Stack voices: running=2 errors=0 primary1280x720")
        } finally {
            try {
                try {
                    onMain {
                        StackPlayback.stop()
                        controller?.takeIf { it.isConnected }?.let { it.pause(); it.clearMediaItems() }
                    }
                } finally {
                    context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                    try {
                        // close() also waits for idle. Finish the animated host first.
                        onMain { ownedActivity?.finish() }
                        ownedActivity?.let { activity -> await { onMain { activity.isDestroyed } } }
                    } finally {
                        try { scenario?.close() }
                        finally { context.stopService(Intent(context, PlaybackService::class.java)) }
                    }
                }
                val cleanupPlayer = controller
                if (cleanupPlayer != null) await {
                    !StackPlayback.state.value.active && onMain { !cleanupPlayer.isConnected } &&
                        runBlocking { preferences.current().lastPath == null }
                }
            } finally {
                try {
                    preferences.setScopedMediaTreeUri(old.scopedMediaTreeUri)
                    preferences.setBackgroundMode(old.backgroundMode)
                    preferences.setAdaptiveMotion(old.adaptiveMotion)
                    preferences.setResumePlayback(old.resumePlayback)
                    preferences.setAutoPictureInPicture(old.autoPictureInPicture)
                    preferences.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
                } finally {
                    if (createdCompanion) check(companion.delete() || !companion.exists())
                }
            }
        }
    }

    private fun <T> onMain(block: () -> T): T {
        // onActivity waits for global idle; live Stack/ambient frames need not idle.
        val task = FutureTask(block)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
        return task.get(5, TimeUnit.SECONDS)
    }

    private fun hasColor(id: String, red: Boolean): Boolean {
        val sample = VideoAmbientColors.state.value
        if (sample.mediaId != id || sample.pixels.isEmpty()) return false
        val palette = analyzeAmbient(sample.pixels, listOf(0xff10151b.toInt())).colors.first()
        val r = (palette ushr 16) and 255; val b = palette and 255
        return if (red) r > b + 60 else b > r + 60
    }
    private fun await(condition: () -> Boolean) {
        val until = android.os.SystemClock.elapsedRealtime() + 15000
        while (!condition() && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(100)
        assertTrue("Timed out waiting for actual player/compositor evidence", condition())
    }
    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "ambient-evidence").apply { mkdirs() }
        try { File(folder, name).outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
        finally { image.recycle() }
    }
    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
    private fun resetFrames() { shell("dumpsys gfxinfo com.local.listentomusic reset") }
    private fun frames(name: String) {
        val summary = shell("dumpsys gfxinfo com.local.listentomusic").lineSequence().filter {
            it.contains("Total frames") || it.contains("Janky frames") || it.contains("percentile") ||
                it.contains("Number Missed") || it.contains("Number High")
        }.joinToString("\n") + "\nheapUsedBytes=" +
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "ambient-evidence").apply { mkdirs() }
        File(folder, name).writeText(summary)
        android.util.Log.i("GA-AmbientFrames", "$name: $summary")
    }
}
