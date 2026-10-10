package com.local.listentomusic.playback

import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.BuildConfig
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File

/** Run alongside the separate consented capture test service. No position-based sync claim. */
class StackOutputProbeTest {
    @Test fun playSixZeroOffsetProbeVoices() {
        play(true)
    }
    @Test fun playSingleMixedCalibration() {
        play(false)
    }
    @Test fun playSixSharedClockProbeVoices(): Unit = runBlocking {
        assumeTrue("Requires explicit debug prototype build", BuildConfig.STACK_PCM_PROTOTYPE)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        val tracks = (0..5).map { index ->
            val file = File("/sdcard/Download/GreaterArtSyncProbe/voice_$index.wav")
            assertTrue(file.isFile)
            StackSlot(MediaFile(file.canonicalPath, file.name, 60000, file.length(), file.lastModified(), MediaKind.AUDIO))
        }
        val connection = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
        val controller = withContext(Dispatchers.IO) { connection.value.get(10, java.util.concurrent.TimeUnit.SECONDS) }
        val volume = withContext(Dispatchers.Main) { controller.volume }
        val host = object : PcmPrototypeHost {
            override suspend fun acquireExclusiveAudio(): Boolean = withContext(Dispatchers.Main) {
                ParallelPlayback.stopAll(); controller.pause(); controller.volume = 0f
                !StackPlayback.state.value.active && ParallelPlayback.layers.value.isEmpty()
            }
            override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) {
                withContext(Dispatchers.Main) { controller.volume = volume }
            }
            override fun onClock(positionMs: Long, playing: Boolean) { }
        }
        val engine = SharedClockPcmPrototype(context, host)
        try {
            assertTrue("Shared-clock probe could not start", engine.start(tracks, tracks.first().file.path))
            delay(60000)
            assertNull(engine.stats.value.error)
            android.util.Log.i("GreaterArtOutputProbe", "sharedClock=${engine.stats.value}")
        } finally {
            engine.release()
            withContext(Dispatchers.Main) { controller.volume = volume; connection.close(); activity.finish() }
        }
    }
    private fun play(stack: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File("/sdcard/Download/GreaterArtSyncProbe")
        val files = (if (stack) (0..5).map { File(directory, "voice_$it.wav") } else listOf(File(directory, "calibration.wav")))
            .map { file ->
                assertTrue("Missing generated probe $file", file.isFile)
                MediaFile(file.canonicalPath, file.name, 60000, file.length(), file.lastModified(), MediaKind.AUDIO)
            }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var player: ExoPlayer
        var coordinator: StackPlaybackCoordinator? = null
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(context).build()
            if (stack) {
                coordinator = StackPlaybackCoordinator(context, player, scope) { }
                assertTrue(coordinator!!.start(files))
            } else { player.setMediaItem(files.single().toMediaItem()); player.prepare(); player.play() }
        }
        try { Thread.sleep(60000) }
        finally { instrumentation.runOnMainSync { coordinator?.release(); player.release(); scope.cancel() } }
    }
}
