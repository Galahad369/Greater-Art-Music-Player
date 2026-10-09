package com.local.listentomusic.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.BuildConfig
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.*

@RunWith(AndroidJUnit4::class)
class PcmPrototypeTest {
    private fun fixture(rate: Int, channels: Int): File {
        val file = File("/sdcard/Download", "ga-pcm-test-${UUID.randomUUID()}.wav")
        val frames = rate * 2
        val size = frames * channels * 2
        val bytes = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()); bytes.putInt(size + 36); bytes.put("WAVEfmt ".toByteArray())
        bytes.putInt(16); bytes.putShort(1); bytes.putShort(channels.toShort()); bytes.putInt(rate)
        bytes.putInt(rate * channels * 2); bytes.putShort((channels * 2).toShort()); bytes.putShort(16)
        bytes.put("data".toByteArray()); bytes.putInt(size)
        repeat(frames) { frame -> repeat(channels) { channel ->
            bytes.putShort((sin(2 * PI * 1000 * frame / rate) * .2 * (channel + 1) * 32767).toInt().toShort())
        } }
        file.writeBytes(bytes.array())
        return file
    }
    private fun media(file: File) = MediaFile(file.canonicalPath, file.name, 1000, file.length(),
        file.lastModified(), MediaKind.AUDIO, sourcePath = file.canonicalPath, clipStartMs = 500, clipEndMs = 1500)

    @Test fun nativeMonoStereoClipsAndCancellationRemainBounded() = runBlocking<Unit> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val created = mutableListOf<File>()
        try {
            for ((rate, channels) in listOf(44100 to 1, 48000 to 2)) {
                val source = fixture(rate, channels).also(created::add)
                val file = media(source)
                val spec = pcmSpec(file)
                assertEquals(rate, spec.rate); assertEquals(channels, spec.channels)
                assertEquals(rate.toLong(), spec.durationFrames)
                val stream = StackPcmStream(scope, file, spec, 0)
                try {
                    val lane = PcmLane(stream) { }
                    lane.prepare(0, 255)
                    assertEquals(.2 * sin(2 * PI * 1000 * 100 / rate), lane.sample(100, 0).toDouble(), .0001)
                    if (channels == 2) assertEquals(lane.sample(100, 0) * 2, lane.sample(100, 1), .0001f)
                    // Decoder is allowed to fill only four queued chunks while consumer stalls.
                    delay(300)
                    withTimeout(2000) { stream.close() }
                } finally { stream.close() }
            }
        } finally { scope.cancel(); created.forEach { it.delete() } }
    }

    @Test fun disabledGateNeverAcquiresOrChangesLegacyOutput() = runBlocking<Unit> {
        assumeTrue("Run with default-off build", !BuildConfig.STACK_PCM_PROTOTYPE)
        var acquired = false
        val host = object : PcmPrototypeHost {
            override suspend fun acquireExclusiveAudio(): Boolean { acquired = true; return true }
            override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) { fail("gate changed legacy") }
            override fun onClock(positionMs: Long, playing: Boolean) { fail("gate changed video") }
        }
        val engine = SharedClockPcmPrototype(InstrumentationRegistry.getInstrumentation().targetContext, host)
        try { assertFalse(engine.start(emptyList(), "none")); assertFalse(acquired) }
        finally { engine.release() }
    }

    @Test fun explicitPrototypeRunsEightVoicesPauseSeekLoopAndSolo() = runBlocking<Unit> {
        assumeTrue("Requires explicit -PstackPcmPrototype=true debug build", BuildConfig.STACK_PCM_PROTOTYPE)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        val connection = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
        val controller = withContext(Dispatchers.IO) { connection.value.get(10, TimeUnit.SECONDS) }
        val originalVolume = withContext(Dispatchers.Main) { controller.volume }
        val source = fixture(48000, 2)
        var restores = 0
        val host = object : PcmPrototypeHost {
            override suspend fun acquireExclusiveAudio(): Boolean = withContext(Dispatchers.Main) {
                ParallelPlayback.stopAll()
                controller.pause()
                controller.volume = 0f
                !StackPlayback.state.value.active && ParallelPlayback.layers.value.isEmpty()
            }
            override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) {
                withContext(Dispatchers.Main) { controller.volume = originalVolume }
                restores++
            }
            override fun onClock(positionMs: Long, playing: Boolean) { }
        }
        val engine = SharedClockPcmPrototype(context, host)
        val tracks = List(8) { i -> StackSlot(media(source).copy(path = "${source.canonicalPath}#$i")) }
        suspend fun await(condition: () -> Boolean) {
            withTimeout(10000) { while (!condition()) { check(engine.stats.value.error == null) { engine.stats.value.toString() }; delay(20) } }
        }
        try {
            assertFalse(engine.start(tracks.map { it.copy(alignmentScale = 1.001) }, tracks[0].file.path))
            assertEquals(0, restores)
            assertTrue("Prototype could not get foreground audio focus: ${engine.stats.value}", engine.start(tracks, tracks[0].file.path))
            await { engine.stats.value.frame > 4800 }
            android.util.Log.i("PcmPrototypeTest", "eight-voice steady=${engine.stats.value}")
            engine.pause()
            val paused = engine.stats.value.frame
            delay(100); assertEquals(paused, engine.stats.value.frame)
            engine.seek(500); assertEquals(24000L, engine.stats.value.frame)
            engine.levels(tracks.mapIndexed { i, slot -> slot.copy(solo = i == 3, muted = i == 1) })
            engine.play(); await { engine.stats.value.frame > 30000 }
            engine.onOutputInterrupted()
            await { !engine.stats.value.playing }
            engine.play(); await { engine.stats.value.playing }
            engine.seek(900)
            await { engine.stats.value.frame < 10000 && engine.stats.value.playing }
            android.util.Log.i("PcmPrototypeTest", "eight-voice stats=${engine.stats.value}; heapBytes=${Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()}")
            engine.stop(); assertEquals(1, restores)
            val takes = File("/sdcard/Download").listFiles().orEmpty().filter {
                it.extension == "mp4" && it.name.contains("孤独毒毒")
            }.sortedBy { it.name }.take(6)
            if (takes.size == 6) {
                val realTracks = takes.map { StackSlot(MediaFile(it.canonicalPath, it.name, 2000,
                    it.length(), it.lastModified(), MediaKind.VIDEO, clipStartMs = 30_000, clipEndMs = 32_000)) }
                assertTrue(engine.start(realTracks, realTracks[0].file.path))
                await { engine.stats.value.frame > engine.stats.value.outputRate / 2 }
                android.util.Log.i("PcmPrototypeTest", "six-compressed steady=${engine.stats.value}; heapBytes=${Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()}")
                engine.pause()
                engine.seek(1000)
                engine.play(); await { engine.stats.value.frame > engine.stats.value.outputRate * 1.25 }
                engine.stop(); assertEquals(2, restores)
            }
        } finally {
            engine.release(); source.delete()
            withContext(Dispatchers.Main) { controller.volume = originalVolume; connection.close(); activity.finish() }
        }
    }
}
