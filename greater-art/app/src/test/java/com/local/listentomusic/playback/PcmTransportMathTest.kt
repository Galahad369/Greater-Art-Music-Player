package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmTransportMathTest {
    private fun slot(name: String, offsetUs: Double = 0.0, scale: Double = 1.0) =
        StackSlot(MediaFile(name, name, 0, 0, 0, MediaKind.AUDIO),
            alignmentOffsetUs = offsetUs, alignmentScale = scale)

    @Test fun longerCompanionAndDelayedEntryKeepTheirWholeNativeTails() {
        val slots = listOf(slot("primary"), slot("long", -250_000.0).copy(muted = true))
        assertEquals(108000L, pcmSessionEndFrames(slots, listOf(PcmSpec(48000, 2, 48000),
            PcmSpec(96000, 2, 192000)), 48000))
        assertEquals(48000L, pcmSessionEndFrames(slots.mapIndexed { index, slot ->
            if (index == 1) slot.copy(error = "unreadable") else slot },
            listOf(PcmSpec(48000, 2, 48000), PcmSpec(96000, 2, 192000)), 48000))
    }

    @Test fun nativeOddTailAndScaledMappingAreNotRoundedToMilliseconds() {
        assertEquals(19013L, pcmSessionEndFrames(listOf(slot("native")), listOf(PcmSpec(192000, 2, 19013)), 192000))
        val slots = listOf(slot("scaled", 100_000.0, 1.001))
        assertEquals(43157L, pcmSessionEndFrames(slots, listOf(PcmSpec(44100, 1, 44100)), 48000))
        assertEquals(0L, pcmSessionEndFrames(listOf(slot("ended", 2_000_000.0)),
            listOf(PcmSpec(48000, 2, 48000)), 48000))
    }

    @Test fun tempoClockUsesHardwareFramesAndPreservesWrap() {
        val clock = PcmFrameClock(24000, 1.25)
        assertEquals(25250L, clock.position(1000))
        assertEquals(26500L, clock.position(2000))
        assertEquals(26500L, clock.position(2000))
        val fast = PcmFrameClock(0, 2.0)
        assertEquals(0xfffffff0L * 2, fast.position(-16))
        assertEquals(0x100000014L * 2, fast.position(20))
    }
}
