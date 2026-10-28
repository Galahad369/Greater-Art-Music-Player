package com.local.listentomusic.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.BuildConfig
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** App-output timing only. The mono PCM16 capture does not qualify playback fidelity. */
@RunWith(AndroidJUnit4::class)
class StackOutputProbeTest {
    @Test fun playSixZeroOffsetProbeVoices() = probe("production")
    @Test fun playSingleMixedCalibration() = probe("calibration")
    @Test fun playSixSharedClockProbeVoices() = probe("shared")

    private fun probe(mode: String): Unit = runBlocking {
        if (mode == "shared") assumeTrue("Requires explicit debug prototype build",
            BuildConfig.DEBUG && BuildConfig.STACK_PCM_PROTOTYPE)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val arguments = InstrumentationRegistry.getArguments()
        val captureName = arguments.getString("probeCaptureName")
        require(captureName == null || captureName.matches(Regex("[a-z0-9_-]{1,40}\\.wav")))
        val durationMs = arguments.getString("probeDurationMs")?.toLong() ?: 60_000L
        require(durationMs in 10_000L..60_000L)
        val files = (if (mode == "calibration") listOf("calibration.wav") else
            (0..5).map { "voice_$it.wav" }).map { name ->
            val source = File("/sdcard/Download/GreaterArtSyncProbe", name)
            assertTrue("Missing generated probe $source", source.isFile)
            MediaFile(source.canonicalPath, source.name, 60_000, source.length(),
                source.lastModified(), MediaKind.AUDIO)
        }
        val preferences = AppPreferences(context)
        val original = preferences.current()
        val captureStarted = CompletableDeferred<Unit>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getStringExtra("name") == captureName) captureStarted.complete(Unit)
            }
        }
        // Only the test APK sends this; registration exists solely during instrumentation.
        if (captureName != null) ContextCompat.registerReceiver(context, receiver,
            IntentFilter("com.local.listentomusic.test.STACK_CAPTURE_STARTED"), ContextCompat.RECEIVER_EXPORTED)
        try {
            preferences.setResumePlayback(false)
            preferences.setFloatingWindowMode(FloatingWindowMode.NONE)
            preferences.setBackgroundMode(AppBackgroundMode.DEFAULT)
            preferences.setDeveloperMode(false)
            preferences.savePlayback(original.lastPath, original.lastPositionMs, 1f, Player.REPEAT_MODE_OFF)
            val foreground = androidx.test.core.app.ActivityScenario.launch(com.local.listentomusic.MainActivity::class.java)
            val connection = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
            try {
                val controller = withContext(Dispatchers.IO) { connection.value.get(10, TimeUnit.SECONDS) }
                val volume = withContext(Dispatchers.Main) { controller.volume }
                var engine: SharedClockPcmPrototype? = null
                try {
                    withContext(Dispatchers.Main) {
                        ParallelPlayback.stopAll()
                        controller.stop()
                        controller.setPlaybackSpeed(1f)
                        controller.repeatMode = Player.REPEAT_MODE_OFF
                        controller.volume = if (mode == "shared") 0f else 1f
                        assertTrue("An earlier Stack is still active", !StackPlayback.state.value.active)
                        assertTrue("An earlier parallel voice is still active", ParallelPlayback.layers.value.isEmpty())
                    }
                    delay(2_000)
                    Log.i("GreaterArtOutputProbe", "READY mode=$mode capture=$captureName prototype=${BuildConfig.STACK_PCM_PROTOTYPE}")
                    if (captureName != null) withTimeout(120_000) { captureStarted.await() }
                    Log.i("GreaterArtOutputProbe", "COMMAND mode=$mode elapsedNs=${SystemClock.elapsedRealtimeNanos()}")
                    if (mode == "shared") {
                        val host = object : PcmPrototypeHost {
                            override suspend fun acquireExclusiveAudio(): Boolean = withContext(Dispatchers.Main) {
                                ParallelPlayback.stopAll()
                                controller.stop()
                                controller.volume = 0f
                                !StackPlayback.state.value.active && ParallelPlayback.layers.value.isEmpty()
                            }
                            override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) {
                                withContext(Dispatchers.Main) { controller.volume = volume }
                            }
                            override fun onClock(positionMs: Long, playing: Boolean) = Unit
                        }
                        val candidate = SharedClockPcmPrototype(context, host)
                        engine = candidate
                        assertTrue("Shared output could not start", candidate.start(files.map { StackSlot(it) }, files.first().path))
                        candidate.setLoop(false)
                    } else withContext(Dispatchers.Main) {
                        if (mode == "production") {
                            // Use the actual service primary, focus and buffers, not a hand-built approximation.
                            assertTrue("Production Stack could not start", StackPlayback.start(files))
                            StackPlayback.setLoop(false)
                        } else {
                            controller.setMediaItem(files.single().toMediaItem())
                            controller.prepare()
                            controller.play()
                        }
                    }
                    repeat((durationMs / 1_000).toInt()) { second ->
                        delay(1_000)
                        if (mode == "shared") {
                            val stats = requireNotNull(engine).stats.value
                            assertNull("shared second=${second + 1}", stats.error)
                            if (second % 5 == 0) Log.i("GreaterArtOutputProbe", "shared second=${second + 1} $stats")
                        } else if (mode == "production") {
                            val state = StackPlayback.state.value
                            assertEquals("Voice lost at second=${second + 1}", 6, state.slots.size)
                            assertTrue("Voice failed at second=${second + 1}", state.slots.all { it.error == null })
                            if (second % 5 == 0) Log.i("GreaterArtOutputProbe", "production second=${second + 1} running=${state.runningTracks} driftMs=${state.maxDriftMs}")
                        }
                    }
                    Log.i("GreaterArtOutputProbe", "DONE mode=$mode durationMs=$durationMs shared=${engine?.stats?.value}")
                } finally {
                    engine?.release()
                    withContext(Dispatchers.Main) {
                        StackPlayback.stop()
                        ParallelPlayback.stopAll()
                        controller.stop()
                        controller.volume = volume
                    }
                }
            } finally {
                withContext(Dispatchers.Main) { connection.close() }
                foreground.close()
                context.stopService(Intent(context, MiniWindowOverlayService::class.java))
                context.stopService(Intent(context, PlaybackService::class.java))
                instrumentation.waitForIdleSync()
            }
        } finally {
            if (captureName != null) context.unregisterReceiver(receiver)
            preferences.setResumePlayback(original.resumePlayback)
            preferences.setFloatingWindowMode(original.floatingWindowMode)
            preferences.setBackgroundMode(original.backgroundMode)
            preferences.setDeveloperMode(original.developerMode)
            preferences.savePlayback(original.lastPath, original.lastPositionMs, original.playbackSpeed, original.repeatMode)
        }
    }
}
