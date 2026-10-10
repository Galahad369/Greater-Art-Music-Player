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
    @Test fun adaptiveSyncReportsOneMinuteOfSixRealTakes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter {
            it.isFile && it.extension == "mp4" && it.name.contains("孤独毒毒")
        }.sortedBy { it.name }.take(6)
        assumeTrue("Requires six local takes", sources.size == 6)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var main: ExoPlayer
        lateinit var coordinator: StackPlaybackCoordinator
        instrumentation.runOnMainSync {
            main = ExoPlayer.Builder(context).build()
            coordinator = StackPlaybackCoordinator(context, main, scope) { }
            assertTrue(coordinator.start(sources.map { fixture(it) }))
        }
        try {
            val readyUntil = android.os.SystemClock.elapsedRealtime() + 15_000
            while (StackPlayback.state.value.runningTracks != 6 && android.os.SystemClock.elapsedRealtime() < readyUntil) Thread.sleep(100)
            assertEquals(6, StackPlayback.state.value.runningTracks)
            repeat(60) { second ->
                Thread.sleep(1_000)
                instrumentation.runOnMainSync {
                    assertEquals("voice lost at $second", 6, StackPlayback.state.value.runningTracks)
                    android.util.Log.i("GreaterArtAdaptiveSync", "second=${second + 1} ${coordinator.voiceDiagnostics()}")
                }
            }
        } finally {
            instrumentation.runOnMainSync { coordinator.release(); main.release(); scope.cancel() }
        }
    }
    @Test fun decoderPressureRetiresTilesAndRetriesTheSamePrimaryWithoutLosingSingers() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter {
            it.isFile && it.extension == "mp4" && it.name.contains("孤独毒毒")
        }.sortedBy { it.name }.take(6)
        assumeTrue("Requires six local fixture takes", sources.size == 6)
        val files = sources.map { fixture(it) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var main: ExoPlayer
        lateinit var coordinator: StackPlaybackCoordinator
        lateinit var view: androidx.media3.ui.PlayerView
        instrumentation.runOnMainSync {
            main = ExoPlayer.Builder(context).build()
            coordinator = StackPlaybackCoordinator(context, main, scope) { }
            assertTrue(coordinator.start(files))
        }
        fun awaitSix() {
            val deadline = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < deadline) {
                val state = StackPlayback.state.value
                if (!state.synchronizing && state.runningTracks == 6) return
                Thread.sleep(100)
            }
            fail("six singers did not recover: ${StackPlayback.state.value}")
        }
        try {
            awaitSix()
            instrumentation.runOnMainSync {
                view = androidx.media3.ui.PlayerView(context)
                coordinator.attachVideo(files[1].path, Any(), view)
                coordinator.onPrimaryError(androidx.media3.common.PlaybackException(
                    "Synthetic decoder pressure", null, androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
                assertNull("decorative codec must be retired before retry", view.player)
                assertEquals(files[0].path, StackPlayback.state.value.primaryPath)
            }
            awaitSix()
            val recovered = StackPlayback.state.value
            assertEquals(files[0].path, recovered.primaryPath)
            assertTrue(recovered.slots.all { it.error == null })
            assertTrue(recovered.slots[1].videoUnavailable)
            instrumentation.runOnMainSync {
                // Restart while an old recovery timer is pending. Its cancellation
                // must not become an error or pause the newly requested mix.
                assertTrue(coordinator.start(files))
                coordinator.attachVideo(files[1].path, Any(), view)
                coordinator.onPrimaryError(androidx.media3.common.PlaybackException(
                    "Synthetic decoder pressure", null, androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
                assertTrue(coordinator.start(files))
            }
            awaitSix()
            assertTrue(StackPlayback.state.value.playing)
        } finally {
            instrumentation.runOnMainSync { coordinator.release(); main.release(); scope.cancel() }
        }
    }

    @Test fun stoppedDecorativeVideoCannotStopOrRegateSixAudioVoices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter {
            it.isFile && it.extension == "mp4" && it.name.contains("孤独毒毒")
        }.sortedBy { it.name }.take(6)
        assumeTrue("Requires six local fixture takes", sources.size == 6)
        val files = sources.map { fixture(it) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var main: ExoPlayer
        lateinit var coordinator: StackPlaybackCoordinator
        lateinit var view: androidx.media3.ui.PlayerView
        val owner = Any()
        instrumentation.runOnMainSync {
            main = ExoPlayer.Builder(context).build()
            coordinator = StackPlaybackCoordinator(context, main, scope) { }
            assertTrue(coordinator.start(files))
        }
        try {
            val deadline = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < deadline && StackPlayback.state.value.runningTracks != 6) Thread.sleep(100)
            assertEquals(6, StackPlayback.state.value.runningTracks)
            instrumentation.runOnMainSync {
                view = androidx.media3.ui.PlayerView(context)
                coordinator.attachVideo(files[1].path, owner, view)
                val preview = view.player!!
                assertNotSame(main, preview)
                assertTrue(preview.trackSelectionParameters.disabledTrackTypes.contains(androidx.media3.common.C.TRACK_TYPE_AUDIO))
                assertFalse(preview.trackSelectionParameters.disabledTrackTypes.contains(androidx.media3.common.C.TRACK_TYPE_VIDEO))
                // Simulate a stopped/failed visual lane; it must never control a singer.
                preview.stop()
                coordinator.attachVideo(files[1].path, Any(), null)
                assertSame("stale owner detached the new lease", preview, view.player)
            }
            repeat(12) {
                val state = StackPlayback.state.value
                assertEquals("video stopped an audio voice", 6, state.runningTracks)
                assertFalse("decorative video incorrectly gated audio", state.synchronizing)
                Thread.sleep(250)
            }
            instrumentation.runOnMainSync {
                coordinator.attachVideo(files[1].path, owner, null)
                assertNull(view.player)
            }
        } finally {
            instrumentation.runOnMainSync { coordinator.release(); main.release(); scope.cancel() }
        }
    }

    @Test fun localFeatureCacheSurvivesTruncationAndRepositoriesAreShared() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter {
            it.isFile && it.extension == "mp4" && it.name.contains("孤独毒毒")
        }.sortedBy { it.name }.take(2)
        assumeTrue("Requires two local fixture takes", sources.size == 2)
        val files = sources.map { fixture(it) }
        assertSame(com.local.listentomusic.data.MediaCaches.thumbnails(context), com.local.listentomusic.data.MediaCaches.thumbnails(context))
        assertSame(com.local.listentomusic.data.MediaCaches.waveforms(context), com.local.listentomusic.data.MediaCaches.waveforms(context))
        val aligner = StackAudioAlign(context)
        val first = aligner.estimate(files[0], files[1])
        // Different arrangements are allowed to abstain; cache recovery must be deterministic.
        val source = sources[0]
        val identity = "7|${source.canonicalPath}|${source.length()}|${source.lastModified()}|0|null|0|6000"
        val key = java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val cached = File(context.cacheDir, "stack-align-v7-instruments/$key.bin")
        assertTrue(cached.length() > 4)
        // Only generated cache data is corrupted, never the user's media.
        cached.writeBytes(byteArrayOf(0, 0, 1))
        // Evict only derived pair decisions so this assertion actually exercises descriptor recovery.
        File(context.cacheDir, "stack-align-v8-maps").listFiles().orEmpty().forEach { it.delete() }
        val recovered = aligner.estimate(files[0], files[1])
        assertEquals(first, recovered)
        assertTrue(cached.length() > 4)
        val cachedMatch = aligner.estimate(files[0], files[1])
        assertEquals(recovered, cachedMatch)
        val baseline = aligner.estimateV6(files[0], files[1])
        android.util.Log.i("GreaterArtStackTest", "v6=$baseline v7=$first; no acoustic ground truth")
        Unit
    }
    @Test fun sixLocalTakesStartSeekResumeAndLoopWithoutLosingVoices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sources = File("/sdcard/Download").listFiles().orEmpty().filter {
            it.isFile && it.extension == "mp4" && it.name.contains("孤独毒毒")
        }.sortedBy { it.name }.take(6)
        assumeTrue("Requires six local 孤独毒毒 fixture videos", sources.size == 6)
        val files = sources.map { fixture(it) }
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
            val settledDrift = mutableListOf<Long>()
            while (System.currentTimeMillis() - start < 8_000) {
                val state = StackPlayback.state.value
                assertEquals("lost a voice during steady playback", 6, state.runningTracks)
                assertFalse("unnecessary group gate", state.synchronizing)
                // Rate trim deliberately converges instead of force-seeking the
                // moment AudioTrack timestamps first settle. Test convergence,
                // not instantaneous zero drift at an asynchronous seek boundary.
                android.util.Log.i("GreaterArtStackTest", "afterSeek=${System.currentTimeMillis() - start} driftMs=${state.maxDriftMs}")
                if (System.currentTimeMillis() - start >= 6_000) settledDrift += state.maxDriftMs
                Thread.sleep(250)
            }
            assertTrue("persistent large drift $settledDrift", settledDrift.all { it < 200 })
            assertTrue("rate correction failed to converge $settledDrift", settledDrift.average() < 75)
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

    private fun fixture(source: File): MediaFile {
        val metadata = android.media.MediaMetadataRetriever()
        val duration = try {
            metadata.setDataSource(source.canonicalPath)
            metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        } finally { metadata.release() }
        return MediaFile(source.canonicalPath, source.name, duration, source.length(), source.lastModified(), MediaKind.VIDEO)
    }
}
