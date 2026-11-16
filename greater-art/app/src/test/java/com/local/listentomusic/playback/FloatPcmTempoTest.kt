package com.local.listentomusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
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
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class FloatPcmTempoTest {
    @Test fun bulkInputIsByteIdenticalToTheOriginalScalarTempoAdapter() {
        for (rate in listOf(48000, 96000, 192000)) for (speed in listOf(.25f, 1.25f, 3f))
            for (pitch in listOf(1f, 1.5f)) {
                val reference = SonicAudioProcessor()
                reference.setSpeed(speed); reference.setPitch(pitch)
                val format = reference.configure(AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_FLOAT))
                assertEquals(C.ENCODING_PCM_FLOAT, format.encoding)
                reference.flush(AudioProcessor.StreamMetadata.DEFAULT)
                FloatPcmTempo(rate, speed, pitch).use { stage ->
                    val expected = ByteArrayOutputStream()
                    val actual = ByteArrayOutputStream()
                    val bytes = ByteBuffer.allocateDirect(PCM_BLOCK_FRAMES * 8).order(ByteOrder.nativeOrder())
                    val samples = FloatArray(PCM_BLOCK_FRAMES * 2)
                    fun append(target: ByteArrayOutputStream, buffer: ByteBuffer) {
                        val packet = ByteArray(buffer.remaining())
                        buffer.get(packet); target.write(packet)
                    }
                    var first = 0
                    val total = rate / 10 + 137
                    while (first < total) {
                        val frames = minOf(PCM_BLOCK_FRAMES, total - first)
                        repeat(frames) { index ->
                            samples[index * 2] = (sin((first + index) * .137) * 1e-7 + index % 31 * 1e-11).toFloat()
                            samples[index * 2 + 1] = (sin((first + index) * .271) * -5e-8).toFloat()
                        }
                        append(actual, stage.queue(samples, frames))
                        // Frozen pre-change scalar adapter, independent of the
                        // production bulk view/cursor implementation.
                        bytes.clear()
                        for (index in 0 until frames * 2) bytes.putFloat(samples[index])
                        bytes.flip()
                        reference.queueInput(bytes)
                        assertFalse(bytes.hasRemaining())
                        append(expected, reference.output)
                        first += frames
                    }
                    stage.end(); reference.queueEndOfStream()
                    var drains = 0
                    while (!stage.ended || !reference.isEnded) {
                        assertTrue(++drains < 100)
                        if (!stage.ended) append(actual, stage.drain())
                        if (!reference.isEnded) append(expected, reference.output)
                    }
                    assertArrayEquals("rate=$rate speed=$speed pitch=$pitch", expected.toByteArray(), actual.toByteArray())
                }
                reference.reset()
            }
    }

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
