package com.local.listentomusic.playback

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/** Real Media3 transport checks. Position assertions are not emitted-audio acceptance. */
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class)
class StackTransportClockTest {
    @Test fun playingSeekIntoLongerTakeKeepsTheRequestedTimeline() = seekIntoSurvivor(true)

    @Test fun pausedSeekIntoLongerTakeKeepsTheRequestedTimeline() = seekIntoSurvivor(false)

    @Test fun rejectedTailPromotionDoesNotSwallowTheSeekOrClampSavedMaps() {
        val files = listOf(tone(10), tone(50), tone(50))
        try {
            exercise(files.mapIndexed { index, file -> media(file, if (index == 0) 10000 else 50000) }) { coordinator, _ ->
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                await("initial three players") { !it.synchronizing && it.runningTracks == 3 }
                instrumentation.runOnMainSync {
                    coordinator.pause()
                    coordinator.setLoop(false)
                    coordinator.setOffsets(mapOf(files[1].canonicalPath to -20000L, files[2].canonicalPath to 20000L))
                }
                val maps = StackPlayback.state.value.slots.map { it.offsetMs to it.alignmentOffsetUs }
                instrumentation.runOnMainSync { coordinator.seek(15000) }
                // Promoting the entered+20s take would rebase the-20s take to
                // unsupported-40s. Preserve the original maps and use the old
                // primary's bounded seek rather than silently dropping the action.
                await("rejected rebase still performs the bounded seek") { it.positionMs >= 9900 }
                val afterSeek = StackPlayback.state.value
                assertEquals(files[0].canonicalPath, afterSeek.primaryPath)
                assertEquals(maps, afterSeek.slots.map { it.offsetMs to it.alignmentOffsetUs })
                assertFalse(afterSeek.playing)
            }
        } finally { files.forEach { it.delete() } }
    }

    private fun seekIntoSurvivor(resume: Boolean) {
        val files = listOf(tone(10), tone(20))
        try {
            exercise(files.map { media(it, if (it == files[0]) 10000 else 20000) }) { coordinator, _ ->
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                await("initial pair") { !it.synchronizing && it.runningTracks == 2 }
                instrumentation.runOnMainSync {
                    if (!resume) coordinator.pause()
                    coordinator.seek(15000)
                }
                await("longer survivor selected after the seek") {
                    it.primaryPath == files[1].canonicalPath && !it.synchronizing
                }
                // Do not wait until a wrong10s seek eventually plays forward to15s.
                // Assert the first ready survivor's target, independently of readiness.
                val afterSeek = StackPlayback.state.value
                assertTrue("Requested15s fell back to the shorter primary EOF: $afterSeek",
                    afterSeek.positionMs in 14900..17000)
                assertEquals(resume, afterSeek.playing)
                if (!resume) {
                    val paused = StackPlayback.state.value.positionMs
                    Thread.sleep(250)
                    assertFalse(StackPlayback.state.value.playing)
                    assertEquals(paused, StackPlayback.state.value.positionMs)
                }
            }
        } finally { files.forEach { it.delete() } }
    }

    @Test fun traceIndependentCodedPlayersWithoutChangingCorrectionPolicy() {
        val folder = File("/sdcard/Download/GreaterArt-Output-Code-QA-20261113")
        val files = (1..6).map { index ->
            val source = File(folder, "QA Stack Code %02d.wav".format(index))
            assertTrue("Missing owned frozen coded fixture", source.isFile && source.length() > 44)
            media(source, 30000)
        }
        exercise(files, trace = true) { coordinator, main ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            await("all six coded players ready") { !it.synchronizing && it.runningTracks == 6 }
            instrumentation.runOnMainSync { coordinator.pause(); coordinator.seek(0); coordinator.play() }
            await("all six resumed at zero") { !it.synchronizing && it.runningTracks == 6 }
            Log.i(TAG, "OUTPUT_READY elapsedMs=${SystemClock.elapsedRealtime()}")
            repeat(270) {
                Thread.sleep(100)
                instrumentation.runOnMainSync {
                    val state = StackPlayback.state.value
                    Log.i(TAG, "sample elapsedMs=${SystemClock.elapsedRealtime()} main=${main.currentPosition}" +
                        " mainPlaying=${main.isPlaying} gate=${state.synchronizing} ${coordinator.voiceDiagnostics()}")
                }
            }
            Log.i(TAG, "OUTPUT_FINISHED elapsedMs=${SystemClock.elapsedRealtime()}; raw scorer decides output timing")
        }
    }

    private fun exercise(files: List<MediaFile>, trace: Boolean = false,
        check: (StackPlaybackCoordinator, ExoPlayer) -> Unit) = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val clockBackend = InstrumentationRegistry.getArguments().getString("gaClockBackend", "sonic")
        require(clockBackend in setOf("sonic", "native", "unity", "static"))
        val nativeRate = trace && clockBackend == "native"
        val unitRate = trace && clockBackend == "unity"
        val staticScheduling = trace && clockBackend == "static"
        fun renderers(owner: android.content.Context) = DefaultRenderersFactory(owner)
            .setEnableDecoderFallback(true).setEnableAudioOutputPlaybackParameters(nativeRate)
        val preferences = AppPreferences(context)
        val before = preferences.current()
        var activity: ActivityScenario<MainActivity>? = null
        var connection: AutoCloseable? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var main: ExoPlayer
        lateinit var coordinator: StackPlaybackCoordinator
        var mainCreated = false
        var coordinatorCreated = false
        var ordinal = 1
        val underruns = IntArray(files.size)
        fun observe(player: ExoPlayer, index: Int) {
            if (!trace) return
            player.addListener(object : Player.Listener {
                override fun onPlaybackParametersChanged(parameters: PlaybackParameters) {
                    Log.i(TAG, "rate elapsedMs=${SystemClock.elapsedRealtime()} voice=$index speed=${parameters.speed} pitch=${parameters.pitch}")
                }
                override fun onPositionDiscontinuity(old: Player.PositionInfo, next: Player.PositionInfo, reason: Int) {
                    Log.i(TAG, "position elapsedMs=${SystemClock.elapsedRealtime()} voice=$index reason=$reason old=${old.positionMs} next=${next.positionMs}")
                }
                override fun onPlaybackStateChanged(state: Int) {
                    Log.i(TAG, "state elapsedMs=${SystemClock.elapsedRealtime()} voice=$index state=$state position=${player.currentPosition}")
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    Log.i(TAG, "playing elapsedMs=${SystemClock.elapsedRealtime()} voice=$index playing=$playing position=${player.currentPosition}")
                }
            })
            player.addAnalyticsListener(object : AnalyticsListener {
                override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime, bufferSize: Int,
                    bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                    underruns[index - 1]++
                    Log.i(TAG, "underrun elapsedMs=${SystemClock.elapsedRealtime()} voice=$index bufferMs=$bufferSizeMs sinceFeedMs=$elapsedSinceLastFeedMs")
                }
            })
        }
        try {
            preferences.setResumePlayback(false)
            preferences.setFloatingWindowMode(FloatingWindowMode.NONE)
            activity = ActivityScenario.launch(MainActivity::class.java)
            val lease = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
            connection = lease
            val controller = withContext(Dispatchers.IO) { lease.value.get(10, TimeUnit.SECONDS) }
            instrumentation.runOnMainSync {
                StackPlayback.stop()
                ParallelPlayback.stopAll()
                controller.stop()
                // Mirror PlaybackService's primary construction for this controlled
                // coordinator probe. This is still not an optimized/UI acceptance run.
                main = ExoPlayer.Builder(context, renderers(context))
                    .experimentalSetDynamicSchedulingEnabled(!staticScheduling)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(context,
                        DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)))
                    .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(10000, 50000, 100, 250)
                        .setTargetBufferBytes(96 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false)
                        .setBackBuffer(5000, true).build())
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
                    .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build()
                mainCreated = true
                main.skipSilenceEnabled = false
                main.repeatMode = Player.REPEAT_MODE_ONE
                observe(main, 1)
                coordinator = StackPlaybackCoordinator(context, main, scope, companionFactory = { owner ->
                    val player = if (!nativeRate && !staticScheduling) newStackAudioPlayer(owner) else
                        ExoPlayer.Builder(owner, renderers(owner))
                            .experimentalSetDynamicSchedulingEnabled(!staticScheduling)
                            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1500, 6000, 100, 200)
                                .setTargetBufferBytes(2 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).build())
                            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
                            .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build()
                    player.also { observe(it, ++ordinal) }
                }, setRateParameters = { player, requested ->
                    if (unitRate) Log.i(TAG, "wantedRate elapsedMs=${SystemClock.elapsedRealtime()}" +
                        " session=${player.audioSessionId} speed=${requested.speed} pitch=${requested.pitch} applied=1.0")
                    player.playbackParameters = if (unitRate) PlaybackParameters.DEFAULT else requested
                }) { coordinator.applyVolumes(1f) }
                coordinatorCreated = true
                // Forward the same primary event categories as PlaybackService.
                main.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) coordinator.onMainReady()
                        else if (state == Player.STATE_ENDED) coordinator.onPrimaryEnded()
                    }
                    override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) = coordinator.onMainPlayChanged(ready)
                    override fun onIsPlayingChanged(playing: Boolean) = coordinator.onMainIsPlayingChanged(playing)
                    override fun onPlaybackParametersChanged(parameters: PlaybackParameters) = coordinator.onMainSpeedChanged()
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) = coordinator.onMainMediaChanged(item?.mediaId)
                    override fun onPositionDiscontinuity(old: Player.PositionInfo, next: Player.PositionInfo, reason: Int) {
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) coordinator.onMainSeek()
                    }
                })
                assertTrue(coordinator.start(files))
                coordinator.setLoop(false)
                if (trace) Log.i(TAG, "backend=$clockBackend; maps=" + StackPlayback.state.value.slots.mapIndexed { index, slot ->
                    "${index + 1}:offsetUs=${slot.alignmentOffsetUs ?: slot.offsetMs * 1000.0},scale=${slot.alignmentScale}"
                }.joinToString(";"))
            }
            check(coordinator, main)
        } finally {
            try {
                instrumentation.runOnMainSync {
                    if (coordinatorCreated) coordinator.release()
                    if (mainCreated) main.release()
                    scope.cancel()
                    if (trace) Log.i(TAG, "underrunCounts=${underruns.joinToString()}")
                }
                withContext(Dispatchers.Main) { connection?.close() }
                activity?.close()
                context.stopService(Intent(context, PlaybackService::class.java))
                instrumentation.waitForIdleSync()
            } finally {
                preferences.setResumePlayback(before.resumePlayback)
                preferences.setFloatingWindowMode(before.floatingWindowMode)
                preferences.savePlayback(before.lastPath, before.lastPositionMs, before.playbackSpeed, before.repeatMode)
            }
        }
    }

    private fun await(label: String, condition: (StackSession) -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 12000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition(StackPlayback.state.value)) return
            Thread.sleep(50)
        }
        fail("$label: ${StackPlayback.state.value}")
    }

    private fun media(source: File, durationMs: Long) = MediaFile(source.canonicalPath, source.name,
        durationMs, source.length(), source.lastModified(), MediaKind.AUDIO)

    private fun tone(seconds: Int): File {
        val rate = 48000
        val frames = rate * seconds
        val size = frames * 4
        val data = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(size + 36).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(2).putInt(rate).putInt(rate * 4)
            .putShort(4).putShort(16).put("data".toByteArray()).putInt(size)
        repeat(frames) { frame ->
            val sample = (sin(2 * PI * 441 * frame / rate) * 1500).toInt().toShort()
            data.putShort(sample).putShort(sample)
        }
        return File("/sdcard/Download", "ga-seek-${UUID.randomUUID()}.wav").also { it.writeBytes(data.array()) }
    }

    companion object { private const val TAG = "GaLegacyClock" }
}
