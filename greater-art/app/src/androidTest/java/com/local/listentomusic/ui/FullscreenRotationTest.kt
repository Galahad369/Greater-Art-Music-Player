package com.local.listentomusic.ui

import android.content.Intent
import kotlin.math.abs
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.FullscreenVideoActivity
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real Activity/configuration changes and retained video, not a simulated viewport. */
class FullscreenRotationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun portraitEntryAndExplicitRoundTripKeepPlayerAndSurface(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixture = File("/sdcard/Download/GA-Ambient-Test/ga-ambient-scenes.mp4")
        assertTrue("Install the owned 1280x720 Ambient scene fixture before this integration test", fixture.isFile)
        val preferences = AppPreferences(context)
        val old = preferences.current()
        preferences.setAppLanguage(AppLanguage.ENGLISH)
        preferences.setFloatingWindowMode(FloatingWindowMode.NONE)
        val main = ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        val monitor = instrumentation.addMonitor(FullscreenVideoActivity::class.java.name, null, false)
        var fullscreen: FullscreenVideoActivity? = null
        var viewModel: MainViewModel? = null
        try {
            main.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            val vm = requireNotNull(viewModel)
            compose.waitUntil(20_000) { vm.controller.value != null }
            main.onActivity {
                vm.play(MediaFile(fixture.path, fixture.name, 24_000L, fixture.length(),
                    fixture.lastModified(), MediaKind.VIDEO))
            }
            compose.waitUntil(20_000) { vm.playback.value.isPlaying && vm.playback.value.isVideo }
            main.onActivity {
                vm.controller.value?.pause()
                VideoSurfaceOwner.beginHandoff("NOW_PLAYING")
                VideoSurfaceOwner.setFullscreenActivityVisible(true)
            }
            context.startActivity(Intent(context, FullscreenVideoActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
            fullscreen = monitor.waitForActivityWithTimeout(10_000) as? FullscreenVideoActivity
            val activity = requireNotNull(fullscreen)
            compose.waitUntil(20_000) {
                activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
                    VideoSurfaceOwner.state.value.owner == "NOW_PLAYING" &&
                    VideoSurfaceOwner.state.value.firstFrame
            }
            val lease = VideoSurfaceOwner.state.value
            capture("portrait")
            assertFullscreenToolbarAlignment("Rotate to landscape")
            assertToolbarInsideScreenSafeRegion(activity)
            compose.onNodeWithContentDescription("Rotate to landscape").assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
            compose.waitUntil(20_000) {
                activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            capture("landscape")
            assertFullscreenToolbarAlignment("Rotate to portrait")
            assertToolbarInsideScreenSafeRegion(activity)
            compose.onNodeWithContentDescription("Rotate to portrait").assertIsDisplayed().performClick()
            compose.waitUntil(20_000) {
                activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            }
            compose.onNodeWithContentDescription("Rotate to landscape").assertIsDisplayed()
            instrumentation.runOnMainSync {
                assertFalse(activity.isDestroyed)
                assertEquals(lease.player, VideoSurfaceOwner.state.value.player)
                assertEquals(lease.view, VideoSurfaceOwner.state.value.view)
                assertEquals(1280, vm.controller.value!!.videoSize.width)
                assertEquals(720, vm.controller.value!!.videoSize.height)
            }
        } finally {
            instrumentation.runOnMainSync { fullscreen?.finish(); viewModel?.controller?.value?.pause() }
            instrumentation.removeMonitor(monitor)
            context.stopService(Intent(context, MiniWindowOverlayService::class.java))
            main.close()
            preferences.setAppLanguage(old.appLanguage)
            preferences.setFloatingWindowMode(old.floatingWindowMode)
        }
    }

    /** Measured on the real fullscreen Activity, with rotate/lock in separate layers. */
    private fun assertFullscreenToolbarAlignment(rotationDescription: String) {
        val rotate = compose.onNodeWithContentDescription(rotationDescription).fetchSemanticsNode().boundsInRoot
        val lock = compose.onNodeWithContentDescription("Lock player controls").fetchSemanticsNode().boundsInRoot
        assertTrue("Rotate and Lock touch targets must share their vertical center: $rotate vs $lock",
            abs(rotate.center.y - lock.center.y) <= 2f)
        assertEquals("Rotate and Lock must have the same touch target height",
            rotate.height, lock.height, 2f)
        assertEquals("Rotate and Lock must have the same touch target width",
            rotate.width, lock.width, 2f)
    }

    /** Verify the high-level toolbar controls remain inside the Android safe cutout. */
    private fun assertToolbarInsideScreenSafeRegion(activity: FullscreenVideoActivity) {
        val windowInsets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
        val safe = windowInsets?.getInsets(
            androidx.core.view.WindowInsetsCompat.Type.displayCutout()
        ) ?: androidx.core.graphics.Insets.NONE
        val width = activity.window.decorView.width.toFloat()
        val rotate = compose.onNodeWithContentDescription(
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                "Rotate to portrait" else "Rotate to landscape",
        ).fetchSemanticsNode().boundsInRoot
        assertTrue("Rotate control must not overlap the left display cutout: $rotate vs $safe",
            rotate.left + 1f >= safe.left)
        assertTrue("Rotate control must not overlap the right display cutout: $rotate vs $safe",
            rotate.right <= width - safe.right + 1f)
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(instrumentation.targetContext.getExternalFilesDir(null), "fullscreen-$name.png")
                .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }
}
