package com.local.listentomusic.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Explicit device fixture: six user-supplied takes in Download; never fetches media. */
@RunWith(AndroidJUnit4::class)
class StackSixTakeTest {
    @Test fun sixLocalTakesStartSeekResumeAndLoopWithoutLosingVoices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter {
            it.isFile && it.extension == "mp4" && it.name.contains("孤独毒毒")
        }.sortedBy { it.name }.take(6)
        assumeTrue("Requires six local 孤独毒毒 fixture videos", sources.size == 6)
        val files = sources.map { MediaFile(it.canonicalPath, it.name, 0, it.length(), it.lastModified(), MediaKind.VIDEO) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var main: ExoPlayer
        lateinit var coordinator: StackPlaybackCoordinator
        instrumentation.runOnMainSync {
            main = ExoPlayer.Builder(context).build()
            coordinator = StackPlaybackCoordinator(context, main, scope) { }
            assertTrue(coordinator.start(files))
        }
        fun await(label: String, timeoutMs: Long = 15_000, condition: (StackSession) -> Boolean) {
            val until = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < until) {
                if (condition(StackPlayback.state.value)) return
                Thread.sleep(100)
            }
            fail("$label: ${StackPlayback.state.value}")
        }
        try {
            await("all six started") { !it.synchronizing && it.runningTracks == 6 && it.positionMs > 500 }
            instrumentation.runOnMainSync { coordinator.pause() }
            await("all paused") { !it.playing && it.runningTracks == 0 }
            instrumentation.runOnMainSync { coordinator.seek(30_000); coordinator.play() }
            await("seek/resume") { !it.synchronizing && it.runningTracks == 6 && it.positionMs >= 30_000 }
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 8_000) {
                val state = StackPlayback.state.value
                assertEquals("lost a voice during steady playback", 6, state.runningTracks)
                assertFalse("unnecessary group gate", state.synchronizing)
                Thread.sleep(250)
            }
            val duration = StackPlayback.state.value.durationMs
            assertTrue(duration > 30_000)
            instrumentation.runOnMainSync { coordinator.seek(duration - 700) }
            await("primary-boundary whole Stack loop", 20_000) {
                !it.synchronizing && it.runningTracks == 6 && it.positionMs in 1..10_000
            }
        } finally {
            instrumentation.runOnMainSync { coordinator.release(); main.release(); scope.cancel() }
        }
    }
}
