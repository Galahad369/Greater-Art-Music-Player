package com.local.listentomusic.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.FullscreenVideoActivity
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlayerWindowMode
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenServiceHandoffTest {
    @Test fun fullscreenKeepsTheExpandedHostForADirectReturn(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixture = File("/sdcard/Download/GreaterArt-Alignment-QA-20261111/GA-Live-Theme-32-loop.mp4")
        assertTrue("Install the owned video fixture first", fixture.isFile)
        val prefs = AppPreferences(context)
        val original = prefs.current()
        prefs.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
        prefs.setResumePlayback(false)
        val automation = instrumentation.uiAutomation
        val originalFlags = automation.serviceInfo.flags
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val main = ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        val monitor = instrumentation.addMonitor(FullscreenVideoActivity::class.java.name, null, false)
        var vm: MainViewModel? = null
        var fullscreen: FullscreenVideoActivity? = null
        try {
            main.onActivity { vm = ViewModelProvider(it)[MainViewModel::class.java] }
            val model = requireNotNull(vm)
            await { model.controller.value != null }
            main.onActivity {
                model.play(MediaFile(fixture.path, fixture.name, 72_000L, fixture.length(), fixture.lastModified(), MediaKind.VIDEO))
                model.seekTo(0L)
            }
            await { model.playback.value.isVideo && model.playback.value.playRequested }
            context.startService(Intent(context, MiniWindowOverlayService::class.java).setAction(MiniWindowOverlayService.ACTION_EXPAND))
            await { MiniWindowOverlayService.modeSnapshot.value == PlayerWindowMode.EXPANDED && VideoSurfaceOwner.state.value.firstFrame }
            val playerIdentity = VideoSurfaceOwner.state.value.player
            var fullscreenRequested = false
            await {
                if (!fullscreenRequested) fullscreenRequested = clickInWindows("Fullscreen")
                fullscreenRequested
            }
            fullscreen = monitor.waitForActivityWithTimeout(10_000L) as? FullscreenVideoActivity
            assertTrue("The fullscreen button did not open the real Activity", fullscreen != null)
            await { VideoSurfaceOwner.state.value.owner == "NOW_PLAYING" && VideoSurfaceOwner.state.value.firstFrame }
            instrumentation.runOnMainSync {
                assertEquals("Fullscreen must retain the expanded host, not tear it down into Detached", PlayerWindowMode.EXPANDED, MiniWindowOverlayService.modeSnapshot.value)
                assertEquals(playerIdentity, VideoSurfaceOwner.state.value.player)
                assertEquals(1280, model.controller.value!!.videoSize.width)
                assertEquals(720, model.controller.value!!.videoSize.height)
                assertTrue(model.controller.value!!.isPlaying)
                fullscreen!!.onBackPressedDispatcher.onBackPressed()
            }
            await { !VideoSurfaceOwner.fullscreenActivityActive && VideoSurfaceOwner.state.value.owner == "MINI_WINDOW" && VideoSurfaceOwner.state.value.firstFrame }
            assertEquals(PlayerWindowMode.EXPANDED, MiniWindowOverlayService.modeSnapshot.value)
            assertEquals(playerIdentity, VideoSurfaceOwner.state.value.player)
        } finally {
            instrumentation.runOnMainSync { fullscreen?.finish(); vm?.controller?.value?.pause() }
            instrumentation.removeMonitor(monitor)
            context.stopService(Intent(context, MiniWindowOverlayService::class.java))
            main.close()
            prefs.setFloatingWindowMode(original.floatingWindowMode)
            prefs.setResumePlayback(original.resumePlayback)
            automation.serviceInfo = automation.serviceInfo.apply { flags = originalFlags }
        }
    }

    private fun clickInWindows(label: String): Boolean {
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.contentDescription?.toString() == label) return node
            for (index in 0 until node.childCount) node.getChild(index)?.let { child -> find(child)?.let { return it } }
            return null
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val node = automation.windows.mapNotNull { it.root }.firstNotNullOfOrNull(::find) ?: return false
        var action = node
        while (!action.isClickable) action = action.parent ?: return false
        return action.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun await(check: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 20_000L
        while (!check() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100L)
        assertTrue("Fullscreen handoff did not reach the expected state", check())
    }
}
