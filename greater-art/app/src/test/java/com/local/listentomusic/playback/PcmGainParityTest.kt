package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmGainParityTest {
    @Test fun partialZeroMuteAndSoloLevelsMatchProductionPolicy() {
        val originals = List(6) { StackSlot(MediaFile("$it", "$it", 1000, 0, 0, MediaKind.AUDIO), volume = .5f) }
        val cases = listOf(originals,
            originals.mapIndexed { index, slot -> slot.copy(volume = if (index == 0) 0f else .7f) },
            originals.mapIndexed { index, slot -> slot.copy(muted = index % 2 == 0) },
            originals.mapIndexed { index, slot -> slot.copy(solo = index == 2 || index == 3, muted = index == 2) },
            originals.map { it.copy(muted = true, solo = true) })
        for (slots in cases) {
            val count = stackAudibleTrackCount(slots)
            val solo = slots.any { it.solo && !it.muted }
            val expected = slots.sumOf { stackAudibleVolume(it, solo, count).toDouble() * .75 }
            val voices = slots.map { PcmVoice(48000, gain = it.volume, muted = it.muted, solo = it.solo) }
            val actual = mixPcmBlock(0, 48000, voices, 1) { _, _, _ -> .75f }
            assertEquals("slots=$slots", expected.toFloat(), actual[0], .0000002f)
        }
    }
}
