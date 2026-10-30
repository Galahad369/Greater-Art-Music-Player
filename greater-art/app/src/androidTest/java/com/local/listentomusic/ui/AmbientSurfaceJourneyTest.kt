package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.data.AmbientMotion
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackService
import com.local.listentomusic.playback.PlayerWindowVisibility
import com.local.listentomusic.ui.components.VideoAmbientColors
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Real Library/navigation and WindowManager overlay, with an explicit owned fixture. */
class AmbientSurfaceJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun bothAmbientModesKeepRealVideoAcrossAllMobileSurfaces(): Unit = runBlocking {
        assumeTrue("Opt in to real fixture screenshots with reflectedAmbientJourney=true",
            InstrumentationRegistry.getArguments().getString("reflectedAmbientJourney") == "true")
        val context = instrumentation.targetContext
        val fixture = File(InstrumentationRegistry.getArguments().getString("reflectedAmbientJourneyPath")
            ?: "/sdcard/Download/ビターチョコデコレーションsyudouselfcover.mp4")
        assertTrue("Provision the explicit local cover-song fixture before this opted-in test", fixture.isFile)
        assertTrue("Grant Display over other apps before this opted-in test", Settings.canDrawOverlays(context))
        val duration = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(fixture.path)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally { retriever.release() }
        }
        assertTrue("The real video requires at least30s of playback headroom", duration >= 30_000L)
        val preferences = AppPreferences(context)
        val old = preferences.current()
        var scenario: ActivityScenario<MainActivity>? = null
        var controller: MediaController? = null
        try {
            preferences.setAppLanguage(AppLanguage.ENGLISH)
            preferences.setColorTheme(ColorTheme.FOREST)
            preferences.setThemeMode(ThemeMode.DARK)
            preferences.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
            preferences.setBackgroundMode(AppBackgroundMode.AMBIENT)
            preferences.setBackgroundDim(.12f)
            preferences.setAdaptiveIntensity(.9f)
            preferences.setAdaptiveReactToVideo(true)
            preferences.setAdaptiveMotion(AmbientMotion.NORMAL)
            preferences.setDeveloperMode(false)
            preferences.setResumePlayback(false)
            preferences.setAutoPictureInPicture(false)
            preferences.setScopedMediaTreeUri(null)
            scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            val activity = scenario
            lateinit var model: MainViewModel
            activity.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
            await("controller and test preferences") {
                model.controller.value != null && model.settings.value.let {
                    it.appLanguage == AppLanguage.ENGLISH && it.floatingWindowMode == FloatingWindowMode.MINI_WINDOW &&
                        it.backgroundMode == AppBackgroundMode.AMBIENT && !it.developerMode && !it.resumePlayback
                }
            }
            val player = requireNotNull(model.controller.value)
            controller = player
            onMain {
                model.play(MediaFile(fixture.path, fixture.name, duration, fixture.length(), fixture.lastModified(), MediaKind.VIDEO))
                player.repeatMode = Player.REPEAT_MODE_ONE
            }
            await("actual video, dock and shared colour sample") {
                onMain {
                    player.isPlaying && player.playbackState == Player.STATE_READY &&
                        player.currentMediaItem?.mediaId == fixture.path && player.videoSize.width > 0 && player.videoSize.height > 0 &&
                        PlayerWindowVisibility.dockedVisible.value && MiniWindowOverlayService.destinationReady.value &&
                        VideoAmbientColors.state.value.let { it.mediaId == fixture.path && it.pixels.isNotEmpty() }
                }
            }
            val baseline = onMain {
                VideoIdentity(player.currentMediaItem?.localConfiguration?.uri.toString(), player.videoSize.width, player.videoSize.height)
            }
            onMain { player.seekTo(minOf(60_000L, duration / 3)) }
            await("cover-song scene after the fixture seek") { onMain { player.isPlaying && player.playbackState == Player.STATE_READY } }
            fun assertContinuity() = onMain {
                assertSame("Presentation changed the shared controller", player, model.controller.value)
                assertEquals("Presentation changed current media", fixture.path, player.currentMediaItem?.mediaId)
                assertEquals("Presentation changed current media URI", baseline.uri, player.currentMediaItem?.localConfiguration?.uri.toString())
                assertEquals("Presentation reduced source width", baseline.width, player.videoSize.width)
                assertEquals("Presentation reduced source height", baseline.height, player.videoSize.height)
                assertTrue("Playback must continue across backgrounds and pages", player.isPlaying)
            }
            for (mode in listOf(AppBackgroundMode.AMBIENT, AppBackgroundMode.ADAPTIVE_DYNAMIC)) {
                preferences.setBackgroundMode(mode)
                await("selected ambient mode") { model.settings.value.backgroundMode == mode }
                navigateLibrary()
                for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                    preferences.setThemeMode(theme)
                    await("selected theme") { model.settings.value.themeMode == theme }
                    settlePresentation()
                    delay(2_200)
                    settlePresentation()
                    assertContinuity()
                    capture("$mode-library-$theme")
                }
                compose.onNodeWithContentDescription("Nodes").assertIsDisplayed().performClick()
                settlePresentation()
                delay(2_200)
                settlePresentation()
                assertContinuity()
                capture("$mode-nodes-DARK")
                compose.onNodeWithContentDescription("Stack").assertIsDisplayed().performClick()
                settlePresentation()
                delay(2_200)
                settlePresentation()
                assertContinuity()
                capture("$mode-stack-DARK")
                navigateLibrary()
                await("Library dock before expansion") { PlayerWindowVisibility.dockedVisible.value }
                onMain {
                    ContextCompat.startForegroundService(context,
                        Intent(context, MiniWindowOverlayService::class.java).setAction(MiniWindowOverlayService.ACTION_EXPAND))
                }
                await("real expanded overlay with a rendered frame") {
                    PlayerWindowVisibility.expandedShowing.value && MiniWindowOverlayService.destinationReady.value &&
                        VideoSurfaceOwner.state.value.firstFrame
                }
                delay(2_200)
                settlePresentation()
                assertContinuity()
                capture("$mode-now-playing-DARK")
                compose.onNodeWithContentDescription("Home").assertIsDisplayed().performClick()
                await("Library restored from actual floating player") {
                    !PlayerWindowVisibility.expandedShowing.value && PlayerWindowVisibility.libraryShowing.value
                }
            }
        } finally {
            try {
                onMain { controller?.takeIf { it.isConnected }?.let { it.pause(); it.clearMediaItems() } }
                context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                scenario?.close()
                context.stopService(Intent(context, PlaybackService::class.java))
                instrumentation.waitForIdleSync()
                if (controller != null) await("test playback state cleared before preference restoration") {
                    runBlocking { preferences.current().lastPath == null }
                }
            } finally {
                preferences.setAppLanguage(old.appLanguage)
                preferences.setColorTheme(old.colorTheme)
                preferences.setThemeMode(old.themeMode)
                preferences.setFloatingWindowMode(old.floatingWindowMode)
                preferences.setBackgroundMode(old.backgroundMode)
                preferences.setBackgroundDim(old.backgroundDim)
                preferences.setAdaptiveIntensity(old.adaptiveIntensity)
                preferences.setAdaptiveReactToVideo(old.adaptiveReactToVideo)
                preferences.setAdaptiveMotion(old.adaptiveMotion)
                preferences.setDeveloperMode(old.developerMode)
                preferences.setResumePlayback(old.resumePlayback)
                preferences.setAutoPictureInPicture(old.autoPictureInPicture)
                preferences.setScopedMediaTreeUri(old.scopedMediaTreeUri)
                preferences.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
            }
        }
    }

    private data class VideoIdentity(val uri: String, val width: Int, val height: Int)

    private fun navigateLibrary() {
        compose.onAllNodesWithContentDescription("All songs")[0].assertIsDisplayed().performClick()
        settlePresentation()
    }

    private fun settlePresentation() {
        // Wall-clock delay advances video, not Compose's controlled animation
        // clock. Finish pager/theme/palette transitions before native captures.
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_200)
        compose.waitForIdle()
    }

    private suspend fun await(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 30_000L
        while (!condition()) {
            assertTrue("Timed out waiting for $label", SystemClock.elapsedRealtime() < deadline)
            delay(75)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val task = FutureTask(block)
        instrumentation.runOnMainSync(task)
        return task.get(5, TimeUnit.SECONDS)
    }

    private fun capture(label: String) {
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val context = instrumentation.targetContext
        val target = File(requireNotNull(context.getExternalFilesDir(null)), "ambient-journey-$label-${UUID.randomUUID()}.png")
        try {
            target.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            Log.i("AmbientSurfaceJourney", "SCREENSHOT ${target.absolutePath}")
        } finally { image.recycle() }
    }
}
