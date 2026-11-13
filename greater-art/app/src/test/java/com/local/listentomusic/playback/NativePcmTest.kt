package com.local.listentomusic.playback

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class NativePcmTest {
    @Test fun isolatedWindowKernelMatchesDirectInlineRenderBitsAndGains() {
        for (frame in listOf(0L, 400L, 4_300_000_000L)) for (rate in listOf(48000, 192000))
            for (mixedRates in listOf(false, true)) for (selected in listOf(false, true)) {
                val voices = List(8) { index -> PcmVoice(
                    if (mixedRates && index % 2 == 1) 44100 else rate,
                    if (index % 3 == 0) 0.0 else index * .37 * 1_000_000 / rate - 40,
                    (index + 1) / 8f, muted = selected && index == 2, solo = selected && index == 3) }
                val starts = LongArray(8) { kotlin.math.floor(pcmSourceFrame(frame, rate, voices[it])).toLong() - PCM_SINC_RADIUS }
                val windows = Array(8) { voice ->
                    val last = kotlin.math.ceil(pcmSourceFrame(frame + 1023, rate, voices[voice])).toLong() + PCM_SINC_RADIUS
                    FloatArray(((last - starts[voice] + 1) * 2).toInt()) { index ->
                        kotlin.math.sin((starts[voice] + index / 2) * .137 + voice + index % 2).toFloat() * 1e-7f }
                }
                val expected = FloatArray(2048)
                val actual = FloatArray(2048)
                val expectedGains = FloatArray(8)
                val actualGains = FloatArray(8)
                mixPcmBlockInto(frame, rate, voices, 1024, expected, expectedGains) { voice, sample, channel ->
                    windows[voice][((sample - starts[voice]) * 2 + channel).toInt()]
                }
                mixPcmWindowBlockInto(frame, rate, voices, 1024, actual, actualGains, windows, starts)
                assertArrayEquals("frame=$frame rate=$rate mixed=$mixedRates solo=$selected", expected, actual, 0f)
                assertArrayEquals(expectedGains, actualGains, 0f)
                for (index in expected.indices) assertEquals(expected[index].toRawBits(), actual[index].toRawBits())
            }
    }

    @Test fun bulkFloatReadsMatchScalarBitsAndAdvanceOnlyTheRequestedSamples() {
        val samples = floatArrayOf(-1f, -0f, 0f, Float.MIN_VALUE, 1e-9f, -1e-7f, .125f, 1f)
        for (direct in listOf(false, true)) for (order in listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN)) {
            val buffer = (if (direct) ByteBuffer.allocateDirect(8 + samples.size * 4)
                else ByteBuffer.allocate(8 + samples.size * 4)).order(order)
            buffer.putInt(123)
            samples.forEach(buffer::putFloat)
            buffer.putInt(456)
            buffer.flip(); buffer.position(4)
            val values = nativePcmSamples(buffer, C.ENCODING_PCM_FLOAT, samples.size)
            assertArrayEquals(samples, values, 0f)
            for (index in samples.indices) assertEquals(samples[index].toRawBits(), values[index].toRawBits())
            assertEquals(4 + samples.size * 4, buffer.position())
            assertEquals(456, buffer.int)
        }
    }
    @Test fun bulkFloatInputStillRejectsEveryNonfiniteValue() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(1e-7f).putFloat(bad).putFloat(0f)
            buffer.flip()
            assertTrue(runCatching { nativePcmSamples(buffer, C.ENCODING_PCM_FLOAT, 3) }.isFailure)
        }
    }
    @Test fun pcm24LowBitsAndSignedExtremesSurviveExactly() {
        for (value in listOf(-8388608, -65537, -1, 0, 1, 65537, 8388607)) {
            val bytes = ByteBuffer.wrap(byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte()))
            assertEquals(value / 8388608f, nativePcmSample(bytes, C.ENCODING_PCM_24BIT), 0f)
        }
    }
    @Test fun floatIsNotQuantizedAndWiderIntegerDoesNotPretendToBeLossless() {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(.0000001f)
        bytes.flip()
        assertEquals(.0000001f, nativePcmSample(bytes, C.ENCODING_PCM_FLOAT), 0f)
        assertTrue(runCatching { nativePcmBytes(C.ENCODING_PCM_32BIT) }.isFailure)
    }
    @Test fun reusableRenderMatchesOriginalReferenceWithoutStaleTail() {
        val voices = listOf(PcmVoice(44100, 13.5), PcmVoice(48000, -4.5, .5f))
        fun sample(voice: Int, frame: Long, channel: Int) = kotlin.math.sin(frame * .07 + channel + voice).toFloat()
        val output = FloatArray(512) { 123f }
        val gains = FloatArray(8)
        for (count in listOf(256, 13, 1)) {
            val reference = mixPcmBlock(300, 48000, voices, count, ::sample)
            mixPcmBlockInto(300, 48000, voices, count, output, gains, ::sample)
            for (i in reference.indices) assertEquals(reference[i], output[i], 0f)
        }
    }
    @Test fun largerBatchesPreserveEveryFrameAndStereoSample() {
        val voices = listOf(PcmVoice(48000, 625.0, .7f), PcmVoice(44100, -12.5, .3f))
        fun sample(voice: Int, frame: Long, channel: Int) = kotlin.math.sin(frame * .013 + voice + channel * .3).toFloat()
        val whole = mixPcmBlock(400, 48000, voices, 1024, ::sample)
        repeat(4) { part ->
            val short = mixPcmBlock(400L + part * 256, 48000, voices, 256, ::sample)
            for (i in short.indices) assertEquals(whole[part * 512 + i], short[i], 0f)
        }
    }
    @Test fun cachedNativeFractionalKernelMatchesUncachedScalarAtHighFrequency() {
        for (rate in listOf(48000, 192000)) for (offset in listOf(-9.375, 3.125, 732.1)) {
            val voice = PcmVoice(rate, offset)
            fun sample(frame: Long, channel: Int) = kotlin.math.sin(frame * 2.8 + channel * .4).toFloat() * .3f
            val mixed = mixPcmBlock(400, rate, listOf(voice), 1024) { _, frame, channel -> sample(frame, channel) }
            repeat(1024) { frame -> repeat(2) { channel ->
                val position = pcmSourceFrame(400L + frame, rate, voice)
                val reference = pcmInterpolated(position) { sample(it, channel) }
                assertEquals("rate=$rate offset=$offset frame=$frame/$channel", reference,
                    mixed[frame * 2 + channel], .000001f)
            } }
        }
    }
}
