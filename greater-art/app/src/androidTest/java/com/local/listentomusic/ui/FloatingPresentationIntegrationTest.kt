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
import com.local.listentomusic.playback.FullscreenVideoActivity
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Real native PiP + background policy with the same owned Ambient scene fixture. */
class FloatingPresentationIntegrationTest {
    @Test fun nativePipAndNoFloatingKeepTheSharedVideoPlaying(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("ambientVideoFixture") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File("/sdcard/Download/GA-Ambient-Test/ga-ambient-scenes.mp4")
        assumeTrue(file.isFile)
        val prefs = AppPreferences(context)
        val old = prefs.current()
        prefs.setScopedMediaTreeUri(null)
        prefs.setBackgroundMode(AppBackgroundMode.ADAPTIVE_DYNAMIC)
        prefs.setFloatingWindowMode(FloatingWindowMode.NONE)
        val main = ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        var fullscreen: FullscreenVideoActivity? = null
        val monitor = instrumentation.addMonitor(FullscreenVideoActivity::class.java.name, null, false)
        lateinit var vm: MainViewModel
        try {
            main.onActivity { vm = ViewModelProvider(it)[MainViewModel::class.java] }
            await { vm.controller.value != null }
            main.onActivity { vm.play(MediaFile(file.path, file.name, 24000, file.length(), file.lastModified(), MediaKind.VIDEO)) }
            await { vm.playback.value.isPlaying && vm.playback.value.isVideo }
            val player = requireNotNull(vm.controller.value)
            shell("input keyevent 3")
            Thread.sleep(1000)
            assertTrue(vm.playback.value.isPlaying)
            assertFalse(VideoSurfaceOwner.expandedOverlayActive)
            assertFalse(VideoSurfaceOwner.fullscreenActivityActive)
            prefs.setFloatingWindowMode(FloatingWindowMode.FOLLOW_VIDEO)
            instrumentation.runOnMainSync {
                VideoSurfaceOwner.beginHandoff("NOW_PLAYING")
                VideoSurfaceOwner.setFullscreenActivityVisible(true)
            }
            // ActivityScenario forcibly adds CLEAR_TASK, even with explicit flags.
            // Monitor a genuine app launch to retain the Library task/connection.
            context.startActivity(Intent(context, FullscreenVideoActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(FullscreenVideoActivity.EXTRA_START_PIP, true))
            fullscreen = monitor.waitForActivityWithTimeout(10000) as? FullscreenVideoActivity
            assertNotNull("Native player Activity did not launch", fullscreen)
            var pip = false
            await { instrumentation.runOnMainSync { pip = fullscreen!!.isInPictureInPictureMode }; pip }
            assertSame(player, vm.controller.value)
            assertTrue(vm.playback.value.isPlaying)
            instrumentation.runOnMainSync {
                assertEquals(1280, player.videoSize.width)
                assertEquals(720, player.videoSize.height)
            }
            assertTrue(VideoSurfaceOwner.fullscreenActivityActive)
            android.util.Log.i("GA-FloatingIntegration", "No Floating Home keeps audio; real native PiP active; shared primary1280x720 preserved")
        } finally {
            instrumentation.runOnMainSync { fullscreen?.finish() }
            instrumentation.removeMonitor(monitor)
            instrumentation.runOnMainSync { vm.controller.value?.pause() }
            context.stopService(Intent(context, MiniWindowOverlayService::class.java))
            main.close()
            prefs.setScopedMediaTreeUri(old.scopedMediaTreeUri)
            prefs.setBackgroundMode(old.backgroundMode)
            prefs.setFloatingWindowMode(old.floatingWindowMode)
        }
    }
    private fun await(check: () -> Boolean) {
        val end = android.os.SystemClock.elapsedRealtime() + 20000
        while (!check() && android.os.SystemClock.elapsedRealtime() < end) Thread.sleep(100)
        assertTrue("Presentation transition timed out", check())
    }
    private fun shell(command: String) {
        val fd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }
}
