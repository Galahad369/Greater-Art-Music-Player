package com.local.listentomusic.playback

import kotlin.math.floor
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmWindowMixerTest {
    @Test fun contiguousWindowsKeepEveryFloatBitAndTheOriginalMixOrder() {
        val mixer = PcmWindowMixer()
        val actual = FloatArray(PCM_BLOCK_FRAMES * 2)
        val expected = FloatArray(actual.size)
        val gains = FloatArray(8)
        for (rate in listOf(8000,44100,48000,96000,192000)) {
            for (count in listOf(1,2,6,8)) for (frames in listOf(1,37,1024)) {
                for (frame in listOf(0L,123456L,192000L*3600*5)) for (variant in 0..4) {
                    val voices = List(count) { i -> PcmVoice(
                        if (variant == 4 && i == 0 && rate == 48000) 44100 else rate,
                        (i - count / 2) * (if (variant == 0) 1000000.0 else .37 * 1000000.0) / rate,
                        if (variant == 1 && i == 1) 0f else (i + 1f) / count,
                        muted = variant == 2 && i % 3 == 1,
                        solo = variant == 3 && i % 2 == 0) }
                    val starts = LongArray(count) { floor(pcmSourceFrame(frame,rate,voices[it])).toLong()-PCM_SINC_RADIUS }
                    val windows = Array(count) { i -> FloatArray((PCM_BLOCK_FRAMES+PCM_SINC_RADIUS*2+2)*2) { j ->
                        if (starts[i]+j/2 < 0) 0f else (sin((starts[i]+j/2)*.013+i*.7+j%2*.3)*
                            if (variant == 4) 1e-8 else .35).toFloat() } }
                    mixPcmWindowBlockInto(frame,rate,voices,frames,expected,gains,windows,starts)
                    mixer.mix(frame,rate,voices,frames,actual,gains,windows,starts)
                    for (index in 0 until frames*2) assertEquals(
                        "rate=$rate count=$count frames=$frames frame=$frame variant=$variant at=$index",
                        expected[index].toRawBits(),actual[index].toRawBits())
                }
            }
        }
    }
}
