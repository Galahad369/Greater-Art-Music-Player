package com.local.listentomusic.playback

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatPcmTempoTest {
    @Test fun unityIsAnExplicitBypass() {
        FloatPcmTempo(192000, 1f, 1f).use {
            assertFalse(it.active)
            assertEquals(1.0, it.clockSpeed, 0.0)
        }
    }

    @Test fun actualRenderStagePreservesQuietStereoAtNativeRatesAndDrainsItsTail() {
        for (rate in listOf(48000, 96000, 192000)) for (speed in listOf(.25f, 1.25f, 3f)) {
            FloatPcmTempo(rate, speed, 1f).use { stage ->
                assertTrue(stage.active)
                val encoded = ByteArrayOutputStream()
                fun append(buffer: ByteBuffer) {
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes); encoded.write(bytes)
                }
                val input = FloatArray(PCM_BLOCK_FRAMES * 2)
                var start = 0
                while (start < rate) {
                    val count = minOf(PCM_BLOCK_FRAMES, rate - start)
                    repeat(count) { i ->
                        input[2 * i] = (sin(2 * PI * 440 * (start + i) / rate) * 1e-7).toFloat()
                        input[2 * i + 1] = (sin(2 * PI * 880 * (start + i) / rate) * 5e-8).toFloat()
                    }
                    append(stage.queue(input, count)); start += count
                }
                stage.end()
                var drains = 0
                while (!stage.ended) { assertTrue(++drains < 100); append(stage.drain()) }
                val bytes = ByteBuffer.wrap(encoded.toByteArray()).order(ByteOrder.nativeOrder())
                val samples = FloatArray(bytes.remaining() / 4) { bytes.float }
                val frames = samples.size / 2
                assertTrue("rate=$rate speed=$speed frames=$frames", abs(frames - (rate / speed).roundToInt()) <= 2)
                assertTrue(samples.all { it.isFinite() })
                for (channel in 0..1) {
                    var crossings = 0
                    val first = rate / 20
                    val last = frames - rate / 20
                    for (i in first + 1 until last)
                        if (samples[(i - 1) * 2 + channel] <= 0 && samples[i * 2 + channel] > 0) crossings++
                    assertEquals(if (channel == 0) 440.0 else 880.0,
                        crossings * rate.toDouble() / (last - first), 5.0)
                }
            }
        }
    }
}
