package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.*
import org.junit.Test

class StackPlaybackTest {
    private fun file(index: Int, duration: Long = 1000L) = MediaFile(
        "/Download/$index.mp3", "$index.mp3", duration, 100, 1, MediaKind.AUDIO,
    )

    @Test fun primaryTrackDefinesTheLoopBoundaryEvenWhenACompanionIsLonger() {
        val slots = listOf(StackSlot(file(1, 1000)), StackSlot(file(2, 4000)))
        assertEquals(1000L, stackDuration(slots))
        assertEquals(4000L, stackDuration(slots, slots[1].file.path))
        assertEquals(0L, stackDuration(emptyList()))
    }

    @Test fun capAndDuplicatesAreEnforcedInDomain() {
        val seven = (0 until 7).map { StackSlot(file(it)) }
        assertTrue(canAddStackTrack(seven, file(7)))
        assertFalse(canAddStackTrack(seven + StackSlot(file(7)), file(8)))
        assertFalse(canAddStackTrack(seven, file(0)))
    }

    @Test fun muteSoloAndVolumeUseOnlyAudibleTracksForHeadroom() {
        val slot = StackSlot(file(1), volume = .6f)
        assertEquals(.3f, stackAudibleVolume(slot, false, 2), .0001f)
        assertEquals(.6f, stackAudibleVolume(slot, false, 1), .0001f)
        assertEquals(0f, stackAudibleVolume(slot.copy(muted = true), false, 1), 0f)
        assertEquals(0f, stackAudibleVolume(slot, true, 1), 0f)
        assertEquals(.6f, stackAudibleVolume(slot.copy(solo = true), true, 1), .0001f)

        val muted = slot.copy(muted = true)
        val failed = StackSlot(file(2), error = "Playback unavailable")
        assertEquals(1, stackAudibleTrackCount(listOf(slot, muted, failed)))
        assertEquals(1, stackAudibleTrackCount(listOf(slot.copy(solo = true), StackSlot(file(3)))))
    }

    @Test fun primaryPlayerClockWinsOverSyntheticFallback() {
        assertEquals(1250L, stackMasterPosition(1250L, 4200L, true))
        assertEquals(4200L, stackMasterPosition(1250L, 4200L, false))
    }

    @Test fun driftCorrectionOnlyHandlesSevereDesync() {
        assertFalse(shouldCorrectStackVoice(4500L, 5000L))
        assertTrue(shouldCorrectStackVoice(4300L, 5000L))
    }

    @Test fun stackLoopRestartsOnlyAtSessionEnd() {
        assertFalse(shouldRestartStack(loopEnabled = false, playing = true, positionMs = 5000L, durationMs = 5000L))
        assertFalse(shouldRestartStack(loopEnabled = true, playing = false, positionMs = 5000L, durationMs = 5000L))
        assertFalse(shouldRestartStack(loopEnabled = true, playing = true, positionMs = 4999L, durationMs = 5000L))
        assertTrue(shouldRestartStack(loopEnabled = true, playing = true, positionMs = 5000L, durationMs = 5000L))
    }

    @Test fun temporaryStackRepeatOffNeverOverwritesUserRepeatPreference() {
        assertEquals(1, persistedRepeatMode(stackActive = true, repeatBeforeStack = 1, currentRepeat = 0))
        assertEquals(2, persistedRepeatMode(stackActive = false, repeatBeforeStack = 1, currentRepeat = 2))
    }
}
