package com.local.listentomusic.playback

import com.local.listentomusic.model.*
import org.junit.Assert.*
import org.junit.Test

class ClaudeAuditRegressionTest {
    @Test fun fractionalDensityToolbarKeepsWholeTouchTargets() {
        assertEquals(6, com.local.listentomusic.ui.toolbarAvailableSlots(840, 126, 84))
        assertEquals(5, com.local.listentomusic.ui.toolbarAvailableSlots(840, 126, 86))
        assertEquals(5, com.local.listentomusic.ui.toolbarAvailableSlots(890, 134, 90))
        assertEquals(0, com.local.listentomusic.ui.toolbarAvailableSlots(30, 48, 32))
        assertEquals(0, com.local.listentomusic.ui.toolbarAvailableSlots(320, 0, 32))
    }
    @Test fun everyMinuteModeExpiresOnceUsingTheInjectedMonotonicClock() {
        for (minutes in listOf(5L, 10L, 15L, 30L, 60L)) {
            var now = 12_345L
            val timer = SleepTimerClock { now }
            timer.set(minutes)
            assertEquals(minutes * 60_000L, timer.state.value.remainingMs)
            now += minutes * 60_000L - 1
            assertFalse(timer.tick())
            assertEquals(1L, timer.state.value.remainingMs)
            now++
            assertTrue(timer.tick())
            assertFalse(timer.state.value.active)
            assertFalse(timer.tick())
        }
    }

    @Test fun endModeCancelAndRestartNeverRetainAnOldDeadline() {
        var now = 100L
        val timer = SleepTimerClock { now }
        timer.set(5)
        timer.set(-1)
        now += 600_000
        assertFalse(timer.tick())
        assertTrue(timer.finishAtTrackEnd())
        assertFalse(timer.finishAtTrackEnd())
        timer.set(10)
        timer.cancel()
        assertFalse(timer.tick())
        assertNull(timer.deadline.value)
        timer.set(5)
        assertEquals(now + 300_000L, timer.deadline.value)
        assertFalse(SleepTimerClock { now }.state.value.active) // recreation does not persist a timer
    }

    @Test fun extremeTimerInputCannotWrapToAnExpiredNegativeDeadline() {
        val timer = SleepTimerClock { 123_456L }
        timer.set(Long.MAX_VALUE)
        assertTrue(timer.deadline.value!! > 123_456L)
        assertFalse(timer.tick())
    }

    @Test fun contentAdmissionMatchesGrantQualifiedUnicodeDocumentsWithoutAliasingOtherFiles() {
        val id = "content://provider/document/primary%3AMusic%2F%E9%9F%B3%E6%A8%82.mp3"
        val uri = "content://provider/tree/primary%3AMusic/document/primary%3AMusic%2F%E9%9F%B3%E6%A8%82.mp3"
        assertTrue(internalContentIdentityMatches(id, uri))
        assertFalse(internalContentIdentityMatches(id, uri.replace("provider", "other")))
        assertFalse(internalContentIdentityMatches(id, uri.replace(".mp3", "other.mp3")))
        assertFalse(internalContentIdentityMatches(id, "https://provider/video.mp3"))
        assertTrue(internalContentIdentityMatches("content://media/external/audio/media/42", "content://media/external/audio/media/42"))
        assertFalse(internalContentIdentityMatches("content://media/external/audio/media/42", "content://media/external/audio/media/43"))
    }

    @Test fun mappedEofHonorsOffsetsRatesAndUnknownDurations() {
        val slot = StackSlot(MediaFile("/Download/a.wav", "a.wav", 4000, 1, 1, MediaKind.AUDIO))
        assertEquals(5000L, stackTimelineEnd(slot.copy(offsetMs = -1000), 4000))
        assertEquals(3000L, stackTimelineEnd(slot.copy(offsetMs = 1000), 4000))
        assertEquals(4000L, stackTimelineEnd(slot.copy(alignmentScale = 1.01), 4040))
        assertEquals(0L, stackTimelineEnd(slot, 0))
        assertEquals(0L, stackTimelineEnd(slot.copy(alignmentScale = Double.NaN), 4000))
    }

    @Test fun scopedRulesUseDiscoveredRelativeNamesNotOpaqueIdsOrDisplayOverrides() {
        val file = MediaFile("content://provider/document/135", "Renamed title", 1000, 1, 1,
            MediaKind.AUDIO, sourcePath = "content://provider/tree/root/document/135", relativePath = "Live/音樂.FLAC")
        assertTrue(PlaylistRule(folder = "live", extension = "flac", text = "renamed").matches(file, "/Download"))
        assertFalse(PlaylistRule(folder = "private").matches(file, "/Download"))
        assertFalse(PlaylistRule(folder = "live").matches(file.copy(relativePath = null), "/Download"))
        assertTrue(PlaylistRule(extension = "mp3").matches(file.copy(name = "music.mp3", relativePath = null), "/Download"))
    }

    @Test fun staleFailedConnectionLeasesCannotReleaseTheReplacementEarly() {
        val released = mutableListOf<Any>()
        val pool = SharedPlaybackResource<Any> { released += it }
        val old = Any(); val next = Any()
        val a = pool.acquire { old }; val b = pool.acquire { fail("must share"); Any() }
        pool.invalidate(old)
        val c = pool.acquire { next }
        a.close(); assertTrue(released.isEmpty())
        b.close(); assertEquals(listOf(old), released)
        val d = pool.acquire { fail("replacement is still borrowed"); Any() }
        assertSame(next, d.value)
        c.close(); assertEquals(listOf(old), released)
        d.close(); assertEquals(listOf(old, next), released)
    }
}
