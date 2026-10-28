package com.local.listentomusic.playback

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.BuildConfig
import com.local.listentomusic.MainActivity
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/** Transport/underrun evidence, not acoustic or video-surface acceptance. */
class PcmTransportDeviceTest {
    @Test fun slowPrefillFastPitchSeekAndLongerSurvivorReachExactEof(): Unit = runBlocking {
        exercise(8000, 2) { engine, source, restores ->
            val slots = listOf(StackSlot(source.copy(path = "${source.path}#primary", clipEndMs = 500)),
                StackSlot(source.copy(path = "${source.path}#long")))
            engine.parameters(.25f, 1f)
            assertTrue(engine.start(slots, slots.first().file.path))
            engine.setLoop(false)
            await(engine) { it.frame >= 8000 }
            assertFalse("Zero-length replacement must retain the working Stack",
                engine.start(slots.map { it.copy(offsetMs = 30000, alignmentOffsetUs = null) }, slots.first().file.path))
            assertTrue(restores.isEmpty())
            assertTrue(runCatching { engine.levels(slots.map { it.copy(volume = Float.NaN) }) }.isFailure)
            engine.levels(slots)
            assertTrue("Primary EOF cut off the longer take", engine.stats.value.active)
            assertEquals(16000L, engine.stats.value.durationFrames)
            engine.pause()
            val paused = engine.stats.value.frame
            engine.parameters(2f, 1.5f)
            delay(100)
            assertEquals(paused, engine.stats.value.frame)
            engine.seek(1000)
            assertEquals(8000L, engine.stats.value.frame)
            engine.play()
            await(engine) { it.frame > 10000 }
            await(engine) { !it.active }
            assertEquals(16000L, engine.stats.value.frame)
            assertEquals(listOf(2000L), restores)
            Log.i("PcmTransportDevice", "survivor EOF ${engine.stats.value}")
        }
    }

    @Test fun eightNative192kLanesKeepFloatTempoPitchAndDynamicTransport(): Unit = runBlocking {
        exercise(192000, 12) { engine, source, restores ->
            val slots = List(8) { index -> StackSlot(source.copy(path = "${source.path}#$index"),
                alignmentOffsetUs = index * .37 * 1_000_000 / 192000) }
            engine.parameters(1.25f, 1.5f)
            assertTrue(engine.start(slots, slots.first().file.path))
            engine.setLoop(false)
            await(engine) { it.frame >= 19200 }
            repeat(6) {
                delay(500)
                val stats = engine.stats.value
                assertNull(stats.error)
                assertEquals(192000, stats.outputRate)
                assertEquals("native tempo $stats", 0, stats.underruns)
                assertEquals(1.25f, stats.speed, 0f)
                assertEquals(1.5f, stats.pitch, 0f)
                Log.i("PcmTransportDevice", "native tempo $stats")
            }
            engine.pause()
            val paused = engine.stats.value.frame
            engine.parameters(2f, 1f)
            assertEquals(paused, engine.stats.value.frame)
            engine.seek(4000)
            engine.play()
            await(engine) { it.frame > 192000L * 5 }
            assertEquals(0, engine.stats.value.underruns)
            Log.i("PcmTransportDevice", "native dynamic ${engine.stats.value}")
            engine.onOutputInterrupted()
            await(engine) { !it.playing }
            val interrupted = engine.stats.value.frame
            delay(100)
            assertEquals(interrupted, engine.stats.value.frame)
            engine.stop()
            assertEquals(1, restores.size)
        }
    }

    private suspend fun await(engine: SharedClockPcmPrototype, condition: (PcmPrototypeStats) -> Boolean) {
        withTimeout(15000) {
            while (!condition(engine.stats.value)) {
                assertNull(engine.stats.value.error)
                delay(20)
            }
        }
    }

    private suspend fun exercise(rate: Int, seconds: Int,
        test: suspend (SharedClockPcmPrototype, MediaFile, MutableList<Long>) -> Unit) {
        assumeTrue("Requires explicit debug prototype build", BuildConfig.DEBUG && BuildConfig.STACK_PCM_PROTOTYPE)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = AppPreferences(context)
        val original = preferences.current()
        val source = File("/sdcard/Download", "ga-transport-${UUID.randomUUID()}.wav")
        preferences.setResumePlayback(false)
        preferences.setFloatingWindowMode(FloatingWindowMode.NONE)
        preferences.setBackgroundMode(AppBackgroundMode.DEFAULT)
        val foreground = ActivityScenario.launch(MainActivity::class.java)
        val connection = withContext(Dispatchers.Main) { PlaybackConnection.acquire(context) }
        var engine: SharedClockPcmPrototype? = null
        try {
            val controller = withContext(Dispatchers.IO) { connection.value.get(10, TimeUnit.SECONDS) }
            val volume = withContext(Dispatchers.Main) { controller.volume }
            val restores = mutableListOf<Long>()
            val host = object : PcmPrototypeHost {
                override suspend fun acquireExclusiveAudio(): Boolean = withContext(Dispatchers.Main) {
                    StackPlayback.stop(); ParallelPlayback.stopAll()
                    controller.stop(); controller.volume = 0f
                    true
                }
                override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) {
                    withContext(Dispatchers.Main) { controller.volume = volume }
                    assertNull(reason)
                    restores += positionMs
                }
                override fun onClock(positionMs: Long, playing: Boolean) = Unit
            }
            withContext(Dispatchers.IO) {
                val size = rate * seconds * 8
                val bytes = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
                bytes.put("RIFF".toByteArray()).putInt(size + 36).put("WAVEfmt ".toByteArray())
                    .putInt(16).putShort(3).putShort(2).putInt(rate).putInt(rate * 8)
                    .putShort(8).putShort(32).put("data".toByteArray()).putInt(size)
                repeat(rate * seconds) { frame ->
                    bytes.putFloat((sin(2 * PI * 440 * frame / rate) * 1e-7).toFloat())
                    bytes.putFloat((sin(2 * PI * 880 * frame / rate) * 5e-8).toFloat())
                }
                source.writeBytes(bytes.array())
            }
            val candidate = SharedClockPcmPrototype(context, host).also { engine = it }
            val file = MediaFile(source.canonicalPath, source.name, seconds * 1000L, source.length(),
                source.lastModified(), MediaKind.AUDIO)
            val began = SystemClock.elapsedRealtime()
            test(candidate, file, restores)
            Log.i("PcmTransportDevice", "transport completed rate=$rate elapsedMs=${SystemClock.elapsedRealtime() - began}")
        } finally {
            engine?.release()
            withContext(Dispatchers.Main) { connection.close() }
            foreground.close()
            context.stopService(android.content.Intent(context, PlaybackService::class.java))
            instrumentation.waitForIdleSync()
            preferences.setResumePlayback(original.resumePlayback)
            preferences.setFloatingWindowMode(original.floatingWindowMode)
            preferences.setBackgroundMode(original.backgroundMode)
            preferences.savePlayback(original.lastPath, original.lastPositionMs, original.playbackSpeed, original.repeatMode)
            source.delete()
        }
    }
}
