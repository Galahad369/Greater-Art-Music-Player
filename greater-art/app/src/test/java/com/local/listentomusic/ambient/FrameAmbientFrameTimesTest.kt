package com.local.listentomusic.ambient

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameAmbientFrameTimesTest {
    @Test fun releaseTimeReorderKeepsBothExactAdmissions() {
        val frames = FrameAmbientFrameTimes()
        frames.record(30_000, 120)
        frames.record(20_000, 160)
        assertEquals(160L, frames.lookup(20_000))
        assertEquals(120L, frames.lookup(30_000))
        assertEquals(C.TIME_UNSET, frames.lookup(29_999))
    }

    @Test fun oldAndUnknownFramesAreNotAdmitted() {
        val frames = FrameAmbientFrameTimes()
        for (time in 1L..65L) frames.record(time, time * 10)
        assertEquals(C.TIME_UNSET, frames.lookup(1))
        assertEquals(20L, frames.lookup(2))
        assertEquals(650L, frames.lookup(65))
        assertEquals(C.TIME_UNSET, frames.lookup(66))
    }
}
