package com.local.listentomusic.playback

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativePcmTest {
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
