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
    private fun fixture(rate: Int, channels: Int, seconds: Int = 2): File {
        val file = File("/sdcard/Download", "ga-pcm-test-${UUID.randomUUID()}.wav")
        val frames = rate * seconds
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

    @Test fun nativeFloatStereoPreservesSubPcm16Samples(): Unit = runBlocking {
        val rate = 96_000
        val frames = rate / 10
        val bytes = ByteBuffer.allocate(44 + frames * 8).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + frames * 8).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(3).putShort(2).putInt(rate).putInt(rate * 8)
            .putShort(8).putShort(32).put("data".toByteArray()).putInt(frames * 8)
        fun sample(frame: Int, channel: Int): Float =
            (if (channel == 0) 1f else -1f) * (0.00001f + frame % 17 * 0.0000001f)
        repeat(frames) { frame -> repeat(2) { channel -> bytes.putFloat(sample(frame, channel)) } }
        val file = File("/sdcard/Download", "ga-float-precision-${UUID.randomUUID()}.wav")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            file.writeBytes(bytes.array())
            val source = MediaFile(file.canonicalPath, file.name, 100, file.length(),
                file.lastModified(), MediaKind.AUDIO)
            val spec = pcmSpec(source)
            assertEquals(rate, spec.rate)
            assertEquals(2, spec.channels)
            val stream = StackPcmStream(scope, source, spec, 0)
            try {
                val block = withTimeout(2_000) { stream.blocks.receive() }
                android.util.Log.i("PcmPrototypeTest", "float decoded start=${block.start} first=${block.samples.take(6)}")
                assertEquals(0L, block.start)
                repeat(256) { frame -> repeat(2) { channel ->
                    assertEquals("Float precision/channel lost at $frame/$channel",
                        sample(frame, channel), block.samples[frame * 2 + channel], 0.000000001f)
                } }
            } finally { stream.close() }
        } finally { scope.cancel(); file.delete() }
    }

    @Test fun native192kStereo24BitKeepsEveryBitAcrossClipAndSeek(): Unit = runBlocking {
        val rate = 192_000
        val frames = 19_013
        val data = ByteBuffer.allocate(44 + frames * 6).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + frames * 6).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(2).putInt(rate).putInt(rate * 6)
            .putShort(6).putShort(24).put("data".toByteArray()).putInt(frames * 6)
        fun value(frame: Int, channel: Int) = if (frame % 13 == 0) channel * 2 + 1 else
            (frame * 179 + channel * 137) % 16_777_216 - 8_388_608
        repeat(frames) { frame -> repeat(2) { channel ->
            val sample = value(frame, channel)
            data.put(sample.toByte()).put((sample shr 8).toByte()).put((sample shr 16).toByte())
        } }
        val file = File("/sdcard/Download", "ga-pcm24-${UUID.randomUUID()}.wav")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            file.writeBytes(data.array())
            val full = MediaFile(file.canonicalPath, file.name, 100, file.length(), file.lastModified(), MediaKind.AUDIO)
            assertEquals(frames.toLong(), pcmSpec(full).durationFrames)
            val clip = full.copy(clipStartMs = 20, clipEndMs = 70)
            val spec = pcmSpec(clip)
            assertEquals(rate, spec.rate); assertEquals(2, spec.channels)
            assertEquals(9600L, spec.durationFrames)
            val stream = StackPcmStream(scope, clip, spec, 3000)
            try {
                val lane = PcmLane(stream) { fail("Native WAV unexpectedly starved") }
                lane.prepare(3000, 3255)
                for (frame in 3000..3255) for (channel in 0..1) {
                    assertEquals("24-bit seek/channel mismatch $frame/$channel",
                        value(3840 + frame, channel) / 8388608f, lane.sample(frame.toLong(), channel), 0f)
                }
            } finally { withTimeout(2000) { stream.close() } }
        } finally { scope.cancel(); file.delete() }
    }

    @Test fun unsupportedInteger32IsRefusedBeforeAudioLease(): Unit = runBlocking {
        val data = ByteBuffer.allocate(44 + 400).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(436).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(96_000).putInt(384_000)
            .putShort(4).putShort(32).put("data".toByteArray()).putInt(400)
        repeat(100) { data.putInt(it * 65537 + 1) }
        val file = File("/sdcard/Download", "ga-pcm32-${UUID.randomUUID()}.wav")
        try {
            file.writeBytes(data.array())
            val source = MediaFile(file.canonicalPath, file.name, 1, file.length(), file.lastModified(), MediaKind.AUDIO)
            assertTrue(runCatching { pcmSpec(source) }.exceptionOrNull()?.message.orEmpty().contains("precision"))
            val host = object : PcmPrototypeHost {
                override suspend fun acquireExclusiveAudio(): Boolean { fail("Unsupported PCM32 acquired output"); return false }
                override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) { fail("Unsupported PCM32 changed legacy") }
                override fun onClock(positionMs: Long, playing: Boolean) { fail("Unsupported PCM32 changed clock") }
            }
            val engine = SharedClockPcmPrototype(InstrumentationRegistry.getInstrumentation().targetContext, host)
            try { assertFalse(engine.start(listOf(StackSlot(source)), source.path)) } finally { engine.release() }
        } finally { file.delete() }
    }

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
                    // Decode-ahead is bounded by sample rate, never by recording length.
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

    @Test fun eightVoicesStayContinuousAtNative48kAnd192k(): Unit = runBlocking {
        assumeTrue("Requires explicit debug prototype build", BuildConfig.STACK_PCM_PROTOTYPE)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = com.local.listentomusic.data.AppPreferences(context)
        val original = preferences.current()
        preferences.setResumePlayback(false)
        preferences.setFloatingWindowMode(com.local.listentomusic.data.FloatingWindowMode.NONE)
        val foreground = androidx.test.core.app.ActivityScenario.launch(com.local.listentomusic.MainActivity::class.java)
        val connection = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
        var engine: SharedClockPcmPrototype? = null
        val files = mutableListOf<File>()
        try {
            val controller = withContext(Dispatchers.IO) { connection.value.get(10, TimeUnit.SECONDS) }
            val volume = withContext(Dispatchers.Main) { controller.volume }
            val host = object : PcmPrototypeHost {
                override suspend fun acquireExclusiveAudio(): Boolean = withContext(Dispatchers.Main) {
                    StackPlayback.stop(); ParallelPlayback.stopAll(); controller.stop(); controller.volume = 0f
                    true
                }
                override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) {
                    withContext(Dispatchers.Main) { controller.volume = volume }
                }
                override fun onClock(positionMs: Long, playing: Boolean) = Unit
            }
            val candidate = SharedClockPcmPrototype(context, host)
            engine = candidate
            for (rate in listOf(48_000, 192_000)) {
                val source = withContext(Dispatchers.IO) { fixture(rate, 2, 32).also(files::add) }
                val file = MediaFile(source.canonicalPath, source.name, 32_000, source.length(), source.lastModified(), MediaKind.AUDIO)
                val fractional = InstrumentationRegistry.getArguments().getString("fractionalOffsets") == "true"
                val tracks = List(8) { StackSlot(file.copy(path = "${source.canonicalPath}#$it"),
                    alignmentOffsetUs = if (fractional) it * .37 * 1_000_000 / rate else null) }
                assertTrue(candidate.start(tracks, tracks[0].file.path))
                candidate.setLoop(false)
                repeat(20) { second ->
                    delay(1000)
                    val stats = candidate.stats.value
                    assertNull(stats.error)
                    assertEquals(rate, stats.outputRate)
                    assertEquals("rate=$rate second=$second stats=$stats", 0, stats.underruns)
                    if (second % 5 == 0) android.util.Log.i("PcmPrototypeTest", "sustained rate=$rate second=${second + 1} $stats")
                }
                assertTrue("Shared clock stalled: ${candidate.stats.value}", candidate.stats.value.frame >= rate * 18L)
                android.util.Log.i("PcmPrototypeTest", "sustained final rate=$rate ${candidate.stats.value}")
                withTimeout(2000) { candidate.stop() }
            }
        } finally {
            engine?.release()
            withContext(Dispatchers.Main) { connection.close() }
            foreground.close()
            context.stopService(android.content.Intent(context, PlaybackService::class.java))
            instrumentation.waitForIdleSync()
            preferences.setResumePlayback(original.resumePlayback)
            preferences.setFloatingWindowMode(original.floatingWindowMode)
            preferences.savePlayback(original.lastPath, original.lastPositionMs, original.playbackSpeed, original.repeatMode)
            files.forEach { it.delete() }
        }
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
