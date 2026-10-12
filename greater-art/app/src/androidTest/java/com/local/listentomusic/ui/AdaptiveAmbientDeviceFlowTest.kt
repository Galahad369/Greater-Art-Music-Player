package com.local.listentomusic.ui

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.*
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.StackPlayback
import com.local.listentomusic.ui.components.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

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
        preferences.setScopedMediaTreeUri(null)
        preferences.setBackgroundMode(AppBackgroundMode.AMBIENT)
        preferences.setAdaptiveMotion(AmbientMotion.SUBTLE)
        val scenario = ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        lateinit var viewModel: MainViewModel
        val companion = File(source.parentFile, "ga-ambient-companion-test.mp4")
        var createdCompanion = false
        try {
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            await { viewModel.controller.value != null }
            scenario.onActivity {
                viewModel.play(MediaFile(source.path, source.name, 24000, source.length(), source.lastModified(), MediaKind.VIDEO))
            }
            await { VideoAmbientColors.state.value.mediaId == source.path }
            val controller = requireNotNull(viewModel.controller.value)
            val playerIdentity = System.identityHashCode(controller)
            scenario.onActivity { controller.seekTo(1000); controller.play() }
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
            scenario.onActivity { controller.seekTo(10000); controller.play() }
            await { hasColor(source.path, red = false) }
            val cool = VideoAmbientColors.state.value.pixels
            Thread.sleep(1800)
            screenshot("adaptive-library-cool.png")
            frames("adaptive-gfx.txt")
            assertNotEquals(warm, cool)
            scenario.onActivity {
                assertSame(controller, viewModel.controller.value)
                assertEquals(playerIdentity, System.identityHashCode(controller))
                assertEquals(1280, controller.videoSize.width)
                assertEquals(720, controller.videoSize.height)
                controller.pause()
            }
            val paused = VideoAmbientColors.state.value
            Thread.sleep(1100)
            assertEquals(paused, VideoAmbientColors.state.value)
            android.util.Log.i("GA-AmbientDevice", "primary1280x720 red-to-blue compositor colors passed; Classic-to-Adaptive lease/player unchanged; pause holds")
            require(!companion.exists()) { "Owned companion fixture already exists; refusing overwrite" }
            source.copyTo(companion)
            createdCompanion = true
            scenario.onActivity {
                assertTrue(StackPlayback.start(listOf(source, companion).map {
                    MediaFile(it.path, it.name, 24000, it.length(), it.lastModified(), MediaKind.VIDEO)
                }))
            }
            await { StackPlayback.state.value.runningTracks == 2 }
            assertTrue(StackPlayback.state.value.slots.none { it.error != null })
            Thread.sleep(1500)
            scenario.onActivity {
                assertSame(controller, viewModel.controller.value)
                assertEquals(1280, controller.videoSize.width)
                assertEquals(720, controller.videoSize.height)
            }
            screenshot("adaptive-stack-active.png")
            frames("adaptive-stack-gfx.txt")
            android.util.Log.i("GA-AmbientDevice", "Adaptive background with two video-backed Stack voices: running=2 errors=0 primary1280x720")
        } finally {
            scenario.onActivity { viewModel.controller.value?.pause(); StackPlayback.stop() }
            scenario.close()
            preferences.setScopedMediaTreeUri(old.scopedMediaTreeUri)
            preferences.setBackgroundMode(old.backgroundMode)
            preferences.setAdaptiveMotion(old.adaptiveMotion)
            if (createdCompanion) check(companion.delete() || !companion.exists())
        }
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
