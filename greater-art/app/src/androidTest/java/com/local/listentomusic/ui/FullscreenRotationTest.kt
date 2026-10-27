package com.local.listentomusic.ui

import android.content.Intent
import kotlin.math.abs
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
        // Prior fixture tests can leave a saved position near EOF. A forward
        // seek must have real headroom, not be silently clamped at the duration.
        preferences.setResumePlayback(false)
        preferences.setSeekOffsetMs(5_000L)
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
            // Paused playback also auto-hides in 1.21.54+. Verify idle hide
            // before the center-tap reveal/hide/reveal sequence.
            compose.waitUntil(7_000) {
                compose.onAllNodesWithContentDescription("Rotate to landscape")
                    .fetchSemanticsNodes().isEmpty()
            }
            compose.onRoot().performTouchInput { click(center) }
            compose.onNodeWithContentDescription("Rotate to landscape").assertIsDisplayed()
            compose.onRoot().performTouchInput { click(center) }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Rotate to landscape")
                    .fetchSemanticsNodes().isEmpty()
            }
            compose.onRoot().performTouchInput { click(center) }
            compose.onNodeWithContentDescription("Rotate to landscape").assertIsDisplayed()
            fun position(): Long {
                var value = 0L
                instrumentation.runOnMainSync { value = vm.controller.value!!.currentPosition }
                return value
            }
            val beforeSeek = position()
            var beforeSpeed = 1f
            instrumentation.runOnMainSync { beforeSpeed = vm.controller.value!!.playbackParameters.speed }
            compose.onRoot().performTouchInput { doubleClick(Offset(width * .82f, height * .5f)) }
            compose.waitUntil(5_000) { abs(position() - (beforeSeek + 5_000L)) < 150L }
            compose.onRoot().performTouchInput { doubleClick(Offset(width * .18f, height * .5f)) }
            compose.waitUntil(5_000) { abs(position() - beforeSeek) < 150L }
            compose.onNodeWithContentDescription("Crop video to screen").assertIsDisplayed().performClick()
            compose.waitForIdle()
            fun videoResizeMode(): Int? {
                fun find(view: android.view.View): androidx.media3.ui.PlayerView? {
                    if (view is androidx.media3.ui.PlayerView && view.tag == "NOW_PLAYING") return view
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) {
                        find(view.getChildAt(i))?.let { return it }
                    }
                    return null
                }
                return find(activity.window.decorView)?.resizeMode
            }
            compose.runOnIdle {
                assertEquals(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM, videoResizeMode())
            }
            compose.onNodeWithContentDescription("Fit whole video").assertIsDisplayed().performClick()
            compose.runOnIdle {
                assertEquals(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, videoResizeMode())
            }
            compose.onNodeWithContentDescription("Crop video to screen").assertIsDisplayed()
            compose.onNodeWithContentDescription("Lock player controls").performClick()
            compose.onNodeWithContentDescription("Unlock player controls").assertIsDisplayed()
            val lockedPosition = position()
            compose.onRoot().performTouchInput { doubleClick(Offset(width * .82f, height * .5f)) }
            compose.runOnIdle { assertEquals(lockedPosition, vm.controller.value!!.currentPosition) }
            compose.onNodeWithContentDescription("Unlock player controls").performClick()
            // Pinch followed by one-finger pan must not become a side seek/2x hold.
            compose.onRoot().performTouchInput {
                down(0, center - Offset(60f, 0f)); down(1, center + Offset(60f, 0f))
                for (step in 1..10) {
                    moveTo(0, center - Offset(60f + step * 14f, 0f))
                    moveTo(1, center + Offset(60f + step * 14f, 0f))
                }
                up(0); up(1)
            }
            compose.onRoot().performTouchInput { swipe(center, center + Offset(100f, 20f), 300) }
            compose.runOnIdle {
                assertEquals(lockedPosition, vm.controller.value!!.currentPosition)
                assertEquals(beforeSpeed, vm.controller.value!!.playbackParameters.speed, .001f)
            }
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
            // Material light/dark changes must not dispose the orientation owner
            // or hand the working video surface away during fullscreen.
            for (theme in listOf(com.local.listentomusic.data.ThemeMode.LIGHT, com.local.listentomusic.data.ThemeMode.DARK)) {
                preferences.setThemeMode(theme)
                compose.waitForIdle()
                kotlinx.coroutines.delay(250)
                instrumentation.runOnMainSync {
                    assertEquals(Configuration.ORIENTATION_LANDSCAPE, activity.resources.configuration.orientation)
                    assertEquals(lease.player, VideoSurfaceOwner.state.value.player)
                    assertEquals(lease.view, VideoSurfaceOwner.state.value.view)
                }
            }
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
            preferences.setResumePlayback(old.resumePlayback)
            preferences.setSeekOffsetMs(old.seekOffsetMs)
            preferences.setThemeMode(old.themeMode)
        }
    }

    /** All actions use one visual component; Lock alone is elevated above input blocking. */
    private fun assertFullscreenToolbarAlignment(rotationDescription: String) {
        val descriptions = listOf(rotationDescription, "Lock player controls", "Exit fullscreen", "Close player")
        val bounds = descriptions.map { name ->
            name to compose.onNodeWithContentDescription(name).fetchSemanticsNode().boundsInRoot
        }
        val (firstName, first) = bounds.first()
        bounds.drop(1).forEach { (name, button) ->
            assertTrue("$name and $firstName must share their vertical center: $button vs $first",
                abs(button.center.y - first.center.y) <= 2f)
            assertEquals("$name must match $firstName height", first.height, button.height, 2f)
            assertEquals("$name must match $firstName width", first.width, button.width, 2f)
        }
    }

    private fun assertToolbarInsideScreenSafeRegion(activity: FullscreenVideoActivity) {
        val safe = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            ?: androidx.core.graphics.Insets.NONE
        val rotate = compose.onNodeWithContentDescription(
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                "Rotate to portrait" else "Rotate to landscape",
        ).fetchSemanticsNode().boundsInRoot
        assertTrue("Rotate overlaps left cutout: $rotate vs $safe", rotate.left + 1f >= safe.left)
        assertTrue("Rotate overlaps right cutout: $rotate vs $safe",
            rotate.right <= activity.window.decorView.width - safe.right + 1f)
    }

    private fun capture(name: String) {
        // Configuration changes precede window relayout. Do not capture the old
        // portrait composition merely because Android has reported landscape.
        val wantLandscape = name == "landscape"
        compose.waitUntil(10_000) {
            val bounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
            (bounds.width > bounds.height) == wantLandscape
        }
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // The system rotation animation can still transform a correctly sized
        // Compose root. With this paused fixture, require stable compositor pixels
        // rather than mistaking a tilted transition frame for the final layout.
        var previous: android.graphics.Bitmap? = null
        var stableSamples = 0
        var sampledAt = 0L
        try {
            compose.waitUntil(7_000) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - sampledAt < 100L) false else {
                    sampledAt = now
                    val next = instrumentation.uiAutomation.takeScreenshot()
                    if (next == null) {
                        // Rotation can briefly leave the compositor without a
                        // capture buffer. Still require three real stable frames.
                        stableSamples = 0
                        return@waitUntil false
                    }
                    stableSamples = if (previous?.sameAs(next) == true) stableSamples + 1 else 0
                    previous?.recycle()
                    previous = next
                    stableSamples >= 3
                }
            }
            File(instrumentation.targetContext.getExternalFilesDir(null), "fullscreen-$name.png")
                .outputStream().use { requireNotNull(previous).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally { previous?.recycle() }
    }
}

/** Component-only tests need a hosting Activity; the real-Activity test must not create one. */
class FullscreenToolbarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullscreenSpeedIsVisibleOnLeftAndChangesPlayback() {
        var selected = 1f
        compose.setContent {
            Box(Modifier.width(360.dp)) {
                NowPlayingTopBar(AppLanguage.ENGLISH, {}, {}, {}, {}, fullscreen = true,
                    onRotate = {}, speed = 1f, onSpeed = { selected = it })
            }
        }
        val speed = compose.onNodeWithText("1×").assertIsDisplayed()
        val speedBounds = speed.fetchSemanticsNode().boundsInRoot
        val rotateBounds = compose.onNodeWithContentDescription("Rotate to landscape")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Speed belongs on the left", speedBounds.right <= rotateBounds.left)
        speed.performClick()
        compose.onNodeWithText("1.5×").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1.5f, selected, 0f) }
    }

    @Test fun narrowFullscreenKeepsSpeedAndEssentialActions() {
        compose.setContent {
            Box(Modifier.width(160.dp)) {
                NowPlayingTopBar(AppLanguage.ENGLISH, {}, {}, {}, {}, fullscreen = true,
                    onRotate = {}, speed = 1.25f, onSpeed = {})
            }
        }
        compose.onNodeWithText("1.25×").assertIsDisplayed().performClick()
        compose.onNodeWithText("2×").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Rotate to landscape").assertIsDisplayed()
        compose.onNodeWithContentDescription("Exit fullscreen").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close player").assertIsDisplayed()
    }

    /** No video fixture required: the narrow toolbar must wrap, never clip buttons. */
    @Test fun narrowFullscreenToolbarRetainsRotateExitAndClose() {
        compose.setContent {
            Box(Modifier.width(160.dp)) {
                NowPlayingTopBar(
                    language = AppLanguage.ENGLISH, onPictureInPicture = {},
                    onHome = {}, onFullscreen = {}, onClose = {},
                    fullscreen = true, onRotate = {},
                )
            }
        }
        val rotate = compose.onNodeWithContentDescription("Rotate to landscape")
            .assertIsDisplayed().assertWidthIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        val exit = compose.onNodeWithContentDescription("Exit fullscreen")
            .assertIsDisplayed().assertWidthIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription("Close player")
            .assertIsDisplayed().assertWidthIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        assertTrue("Compact toolbar must wrap essential buttons below the first row",
            rotate.bottom <= exit.top + 2f)
        assertTrue("Exit and Close share the second row", abs(exit.center.y - close.center.y) <= 2f)
        assertTrue("Compact controls may not exceed 160dp safe area",
            maxOf(rotate.right, exit.right, close.right) <=
                160f * InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density + 2f)
    }

    /** No video fixture required: ultra-narrow toolbar stacks 48dp targets. */
    @Test fun ultraNarrowFullscreenToolbarStacksActions() {
        compose.setContent {
            Box(Modifier.width(110.dp)) {
                NowPlayingTopBar(
                    language = AppLanguage.ENGLISH, onPictureInPicture = {},
                    onHome = {}, onFullscreen = {}, onClose = {},
                    fullscreen = true, onRotate = {},
                )
            }
        }
        val rotate = compose.onNodeWithContentDescription("Rotate to landscape")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val exit = compose.onNodeWithContentDescription("Exit fullscreen")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription("Close player")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Ultra-narrow toolbar must not place actions side-by-side", rotate.bottom <= exit.top)
        assertTrue("Ultra-narrow toolbar must preserve action order", exit.bottom <= close.top)
    }

}
