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
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qualifies the real existing dependency, not a DSP stub; no production integration yet. */
class FloatTempoCandidateTest {
    private fun processed(rate: Int, speed: Float, pitch: Float, amplitude: Float): FloatArray {
        val processor = SonicAudioProcessor()
        try {
            processor.setSpeed(speed); processor.setPitch(pitch)
            val format = processor.configure(AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_FLOAT))
            assertEquals(rate, format.sampleRate); assertEquals(2, format.channelCount)
            assertEquals(C.ENCODING_PCM_FLOAT, format.encoding)
            assertTrue(processor.isActive)
            processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
            val output = ByteArrayOutputStream()
            fun drain() {
                val buffer = processor.output
                if (buffer.hasRemaining()) {
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes); output.write(bytes)
                }
            }
            val input = ByteBuffer.allocateDirect(1024 * 8).order(ByteOrder.nativeOrder())
            var next = 0
            while (next < rate) {
                input.clear()
                val frames = minOf(1024, rate - next)
                repeat(frames) { frame ->
                    input.putFloat((sin(2 * PI * 440 * (next + frame) / rate) * amplitude).toFloat())
                    input.putFloat((sin(2 * PI * 880 * (next + frame) / rate) * amplitude * .5).toFloat())
                }
                input.flip(); processor.queueInput(input)
                assertEquals(0, input.remaining()); drain(); next += frames
            }
            processor.queueEndOfStream()
            var attempts = 0
            while (!processor.isEnded) { assertTrue(++attempts < 100); drain() }
            val data = ByteBuffer.wrap(output.toByteArray()).order(ByteOrder.nativeOrder())
            return FloatArray(data.remaining() / 4) { data.float }
        } finally { processor.reset() }
    }

    private fun frequency(samples: FloatArray, channel: Int, rate: Int): Double {
        val frames = samples.size / 2
        val first = rate / 10
        val last = frames - rate / 10
        var rising = 0
        for (frame in first + 1 until last) if (samples[(frame - 1) * 2 + channel] <= 0 && samples[frame * 2 + channel] > 0) rising++
        return rising * rate.toDouble() / (last - first)
    }

    @Test fun floatTempoPreservesNativeRateStereoAndQuietSamples() {
        for (rate in listOf(48000, 96000, 192000)) {
            val output = processed(rate, 1.25f, 1f, .0000001f)
            assertTrue("tempo length rate=$rate frames=${output.size / 2}", abs(output.size / 2 - (rate / 1.25).roundToInt()) <= 2)
            assertTrue(output.all { it.isFinite() })
            assertTrue("PCM16 quantization destroyed quiet signal", output.any { abs(it) > .00000001f })
            assertEquals(440.0, frequency(output, 0, rate), 3.0)
            assertEquals(880.0, frequency(output, 1, rate), 3.0)
        }
    }

    @Test fun pitchAndTinyAlignmentScaleAreIndependentFromTempo() {
        val pitch = processed(96000, 1f, 1.5f, .2f)
        assertTrue(abs(pitch.size / 2 - 96000) <= 2)
        assertEquals(660.0, frequency(pitch, 0, 96000), 3.0)
        assertEquals(1320.0, frequency(pitch, 1, 96000), 3.0)
        for (speed in listOf(.999f, 1.001f)) {
            val output = processed(96000, speed, 1f, .2f)
            assertTrue(abs(output.size / 2 - (96000 / speed).roundToInt()) <= 2)
            assertEquals(440.0, frequency(output, 0, 96000), 3.0)
        }
    }
}
