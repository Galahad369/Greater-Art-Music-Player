package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.Settings
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ambient.FrameAmbientView
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackService
import com.local.listentomusic.playback.PlayerWindowVisibility
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Exercises the actual WindowManager player with the primary service decoder. */
@OptIn(UnstableApi::class)
class ProductionFrameAmbientTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun expandedSdrUsesAmbientWithEveryLibraryBackground(): Unit = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("defaultFrameAmbient") == "true")
        val sdr = File(requireNotNull(arguments.getString("frameAmbientSdrPath")))
        val context = instrumentation.targetContext
        assertTrue("Provision the generated SDR clip", sdr.isFile)
        assertTrue("Grant overlay access on the QA device", Settings.canDrawOverlays(context))
        val preferences = AppPreferences(context)
        val old = preferences.current()
        preferences.setBackgroundMode(AppBackgroundMode.DEFAULT)
        preferences.setAdaptiveReactToVideo(false)
        preferences.setAdaptiveIntensity(.6f)
        preferences.setBackgroundDim(.35f)
        preferences.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
        preferences.setScopedMediaTreeUri(null)
        preferences.setResumePlayback(false)
        preferences.setBluetoothOnlyListening(false)
        var scenario: ActivityScenario<MainActivity>? = null
        var player: Player? = null
        try {
            scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            lateinit var model: MainViewModel
            scenario.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
            await("controller and Default wallpaper") {
                model.controller.value != null && model.settings.value.backgroundMode == AppBackgroundMode.DEFAULT
            }
            val active = requireNotNull(model.controller.value)
            player = active
            onMain {
                model.play(MediaFile(sdr.path, sdr.name, 0L, sdr.length(), sdr.lastModified(), MediaKind.VIDEO))
                active.repeatMode = Player.REPEAT_MODE_ONE
            }
            await("actual dock playback") {
                PlayerWindowVisibility.dockedVisible.value && onMain {
                    active.isPlaying && active.currentMediaItem?.mediaId == sdr.path && active.videoSize.width > 0
                }
            }
            val sourceSize = onMain { active.videoSize }
            onMain {
                ContextCompat.startForegroundService(context, Intent(context, MiniWindowOverlayService::class.java)
                    .setAction(MiniWindowOverlayService.ACTION_EXPAND))
            }
            var checkedSurface: FrameAmbientView? = null
            for (mode in AppBackgroundMode.entries) {
                preferences.setBackgroundMode(mode)
                await("$mode wallpaper setting applied") { model.settings.value.backgroundMode == mode }
                val firstSurface = onMain { VideoSurfaceOwner.activeVideoSurface() as? FrameAmbientView }
                val firstFrame = onMain { firstSurface?.renderedFrames() ?: 0 }
                try {
                    await("NOW_PLAYING_AMBIENT: $mode must render new frames on the actual expanded compositor") {
                        PlayerWindowVisibility.expandedShowing.value && onMain {
                            val surface = VideoSurfaceOwner.activeVideoSurface() as? FrameAmbientView
                            if (surface == null || surface.renderedFrames() < if (surface === firstSurface) firstFrame + 8 else 8)
                                false
                            else { checkedSurface = surface; true }
                        }
                    }
                } catch (failure: AssertionError) {
                    val detail = onMain {
                        "owner=${VideoSurfaceOwner.state.value.owner} playing=${active.isPlaying} state=${active.playbackState} " +
                            "surface=${VideoSurfaceOwner.activeVideoSurface()?.javaClass?.simpleName} " +
                            "frames=${checkedSurface?.renderedFrames()} error=${checkedSurface?.failure() ?: firstSurface?.failure()}"
                    }
                    throw AssertionError("${failure.message}; $detail", failure)
                }
                onMain {
                    assertSame("Keep the shared controller", active, model.controller.value)
                    assertEquals(sdr.path, active.currentMediaItem?.mediaId)
                    assertEquals(sourceSize, active.videoSize)
                }
                android.util.Log.i("GA-FrameAmbient", "1.22.8 expanded Ambient verified: $mode")
            }
            capture("default-all-modes", context)
        } finally {
            try {
                onMain { player?.let { it.pause(); it.clearMediaItems() } }
                context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                scenario?.close()
                context.stopService(Intent(context, PlaybackService::class.java))
                instrumentation.waitForIdleSync()
            } finally {
                preferences.setBackgroundMode(old.backgroundMode)
                preferences.setAdaptiveReactToVideo(old.adaptiveReactToVideo)
                preferences.setAdaptiveIntensity(old.adaptiveIntensity)
                preferences.setBackgroundDim(old.backgroundDim)
                preferences.setFloatingWindowMode(old.floatingWindowMode)
                preferences.setScopedMediaTreeUri(old.scopedMediaTreeUri)
                preferences.setResumePlayback(old.resumePlayback)
                preferences.setBluetoothOnlyListening(old.bluetoothOnlyListening)
                preferences.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
            }
        }
    }

    @Test fun expandedSdrAndPqUseCorrectOutputAndRetainPlayer(): Unit = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("productionFrameAmbient") == "true")
        val sdr = File(requireNotNull(arguments.getString("frameAmbientSdrPath")))
        val pq = File(requireNotNull(arguments.getString("frameAmbientPqPath")))
        val context = instrumentation.targetContext
        assertTrue("Provision the generated SDR and PQ files", sdr.isFile && pq.isFile)
        assertTrue("Grant the QA app overlay access", Settings.canDrawOverlays(context))
        val preferences = AppPreferences(context)
        val old = preferences.current()
        preferences.setBackgroundMode(AppBackgroundMode.AMBIENT)
        preferences.setAdaptiveReactToVideo(true)
        preferences.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
        preferences.setResumePlayback(false)
        var scenario: ActivityScenario<MainActivity>? = null
        var player: Player? = null
        try {
            scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            lateinit var model: MainViewModel
            scenario.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
            await("primary controller and Ambient preference") {
                model.controller.value != null && model.settings.value.backgroundMode == AppBackgroundMode.AMBIENT
            }
            val active = requireNotNull(model.controller.value)
            player = active
            fun play(file: File) = onMain {
                model.play(MediaFile(file.path, file.name, 0L, file.length(), file.lastModified(), MediaKind.VIDEO))
                active.repeatMode = Player.REPEAT_MODE_ONE
            }
            play(sdr)
            await("native dock SDR first frame") {
                PlayerWindowVisibility.dockedVisible.value && onMain {
                    active.isPlaying && active.currentMediaItem?.mediaId == sdr.path &&
                        VideoSurfaceOwner.activeVideoSurface() is SurfaceView
                }
            }
            onMain {
                ContextCompat.startForegroundService(context, Intent(context, MiniWindowOverlayService::class.java)
                    .setAction(MiniWindowOverlayService.ACTION_EXPAND))
            }
            await("actual expanded SDR compositor") {
                PlayerWindowVisibility.expandedShowing.value && onMain {
                    (VideoSurfaceOwner.activeVideoSurface() as? FrameAmbientView)?.renderedFrames()?.let { it >= 8 } == true
                }
            }
            onMain {
                assertSame("Reuse the controller", active, model.controller.value)
                assertEquals(1280, active.videoSize.width)
                assertEquals(720, active.videoSize.height)
                active.pause()
                active.seekTo(4_000L)
            }
            await("paused SDR seek on the same compositor") {
                onMain {
                    !active.isPlaying && active.currentPosition in 3_950L..4_050L &&
                        VideoSurfaceOwner.activeVideoSurface() is FrameAmbientView
                }
            }
            capture("sdr", context)
            play(pq)
            await("native PQ video with a selected HDR track") {
                onMain {
                    active.isPlaying && active.currentMediaItem?.mediaId == pq.path &&
                        active.currentTracks.groups.any { group ->
                            group.type == C.TRACK_TYPE_VIDEO && (0 until group.length).any {
                                group.isTrackSelected(it) &&
                                    group.getTrackFormat(it).colorInfo?.colorTransfer == C.COLOR_TRANSFER_ST2084
                            }
                        } && VideoSurfaceOwner.activeVideoSurface().let {
                            it is SurfaceView && it !is FrameAmbientView
                        } && VideoSurfaceOwner.state.value.firstFrame
                }
            }
            onMain { assertSame("HDR fallback must retain the primary controller", active, model.controller.value) }
            capture("pq-native", context)
            android.util.Log.i("GA-FrameAmbient", "PRODUCTION_AMBIENT_SDR_AND_PQ_ROUTING_PASS")
        } finally {
            try {
                onMain { player?.let { it.pause(); it.clearMediaItems() } }
                context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                scenario?.close()
                context.stopService(Intent(context, PlaybackService::class.java))
            } finally {
                preferences.setBackgroundMode(old.backgroundMode)
                preferences.setAdaptiveReactToVideo(old.adaptiveReactToVideo)
                preferences.setFloatingWindowMode(old.floatingWindowMode)
                preferences.setResumePlayback(old.resumePlayback)
                preferences.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
            }
        }
    }

    private suspend fun await(label: String, ready: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 30_000L
        while (!ready()) {
            assertTrue("Timed out: $label", SystemClock.elapsedRealtime() < end)
            delay(75)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val task = FutureTask(block)
        instrumentation.runOnMainSync(task)
        return task.get(5L, TimeUnit.SECONDS)
    }

    private fun capture(label: String, context: android.content.Context) {
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File(requireNotNull(context.getExternalFilesDir(null)), "production-ambient-$label-${UUID.randomUUID()}.png")
        try {
            assertTrue(file.createNewFile())
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
