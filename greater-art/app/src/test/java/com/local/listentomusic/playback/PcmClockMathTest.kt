package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PcmClockMathTest {
    @Test fun hardwareHeadWrapIsNotASeekOrClockReset() {
        val clock = PcmFrameClock(500)
        assertEquals(500L + 0xfffffff0L, clock.position(-16))
        assertEquals(500L + 0x100000014L, clock.position(20))
        assertEquals(500L + 0x100000014L, clock.position(20))
    }
    @Test fun eightIdenticalVoicesAreFrameLockedWithHeadroom() {
        val voices = List(8) { PcmVoice(48000) }
        val mixed = mixPcmBlock(0, 48000, voices) { _, frame, _ -> if (frame == 73L) 1f else 0f }
        assertEquals(1f, mixed[73 * 2], 0f)
        assertEquals(2, mixed.count { it != 0f })
        assertTrue(mixed.all { abs(it) <= 1f })
    }
    @Test fun positiveAndNegativeOffsetsScheduleSameInstrumentFrame() {
        val voices = listOf(PcmVoice(48000, 0.0), PcmVoice(48000, 1_000_000.0 / 48000 * 30),
            PcmVoice(48000, -1_000_000.0 / 48000 * 30))
        val mixed = mixPcmBlock(0, 48000, voices) { voice, frame, _ ->
            if (frame == listOf(90L, 120L, 60L)[voice]) 1f else 0f
        }
        assertEquals(1f, mixed[180], .00001f)
        assertEquals(0f, mixed[0], 0f)
    }
    @Test fun longTimelineUsesOneClockWithoutIntegerOverflow() {
        val frame = 192000L * 3600 * 5
        val a = PcmVoice(44100, 713250.5)
        val b = PcmVoice(48000, -550500.25)
        assertEquals(frame * 44100.0 / 192000 + 713250.5 * 44100 / 1e6, pcmSourceFrame(frame, 192000, a), 1e-6)
        assertEquals(frame * .25 - 550500.25 * 48000 / 1e6, pcmSourceFrame(frame, 192000, b), 1e-6)
    }
    @Test fun monoStereoGainMuteSoloAndFractionalOffsets() {
        val voices = listOf(PcmVoice(48000, gain = .5f), PcmVoice(48000, gain = 1f, solo = true),
            PcmVoice(48000, gain = 1f, muted = true, solo = true))
        val mixed = mixPcmBlock(100, 48000, voices) { voice, _, channel -> if (voice == 1) .25f * (channel + 1) else 1f }
        assertEquals(.25f, mixed[0], 0f); assertEquals(.5f, mixed[1], 0f)
        val fraction = pcmInterpolated(100.25) { (it * .001).toFloat() }
        assertEquals(.10025f, fraction, .00002f)
    }
    @Test fun mixed44100And48000PreserveBandlimitedSignal() {
        val voices = listOf(PcmVoice(44100), PcmVoice(48000))
        val mixed = mixPcmBlock(1200, 48000, voices) { i, frame, _ -> sin(2 * PI * 1000 * frame / voices[i].rate).toFloat() }
        var squared = 0.0
        for (i in 0 until 256) squared += (mixed[i * 2] - sin(2 * PI * 1000 * (1200 + i) / 48000)).pow(2)
        val rms = sqrt(squared / 256)
        assertTrue("resampling RMS error $rms", rms < .001)
        println("shared-clock 44.1/48k sinusoid RMS error=$rms; relative impulse scheduling error=0 frames")
    }
    @Test fun invalidInputsAndTrackLimitsAreRejected() {
        try { PcmVoice(48000, gain = Float.NaN); fail() } catch (_: IllegalArgumentException) { }
        try { mixPcmBlock(0, 48000, List(9) { PcmVoice(48000) }) { _, _, _ -> 0f }; fail() }
        catch (_: IllegalArgumentException) { }
    }
}
