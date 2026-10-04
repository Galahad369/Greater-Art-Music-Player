package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.*
import org.junit.Test

class StackPlaybackTest {
    @Test fun controlledCorrectionDoesNotFreezeTheWholeMix() {
        assertFalse(stackBufferRequiresGate(1300L, 1000L, 2000L))
        assertFalse(stackBufferRequiresGate(1299L, 1000L, 0L))
        assertFalse(stackBufferRequiresGate(5000L, 0L, 0L))
        assertTrue(stackBufferRequiresGate(2000L, 1000L, 2000L))
        assertTrue(stackBufferRequiresGate(1300L, 1000L, 0L))
    }
    @Test fun gateNeverReleasesVoicesBeforePrimaryIsReady() {
        assertFalse(stackStartGateCanOpen(false, true, true))
        assertFalse(stackStartGateCanOpen(false, false, true))
        assertFalse(stackStartGateCanOpen(true, false, false))
        assertTrue(stackStartGateCanOpen(true, true, false))
        assertTrue(stackStartGateCanOpen(true, false, true))
    }
    @Test fun primaryAcknowledgementsCannotCreateSeekFeedback() {
        val command = stackSeekPlan(1200L, 3000L, primaryEvent = false)
        assertTrue(command.seekPrimary)
        val acknowledgements = List(100) { stackSeekPlan(command.positionMs, 3000L, primaryEvent = true) }
        assertTrue(acknowledgements.all { !it.seekPrimary && it.positionMs == 1200L })
        assertEquals(3000L, stackSeekPlan(9000L, 3000L, primaryEvent = false).positionMs)
    }
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

    @Test fun eightTrackDomainBudgetKeepsAllVoicesAudibleWithHeadroom() {
        assertEquals(8, StackPlayback.MAX_TRACKS)
        val slots = (0 until StackPlayback.MAX_TRACKS).map { StackSlot(file(it), volume = 1f) }
        assertEquals(8, stackAudibleTrackCount(slots))
        assertEquals(0.125f, stackAudibleVolume(slots.first(), anySolo = false, audibleCount = 8), .0001f)
        assertFalse(canAddStackTrack(slots, file(99)))
    }

    @Test fun primaryPlayerClockWinsOverSyntheticFallback() {
        assertEquals(1250L, stackMasterPosition(1250L, 4200L, true))
        assertEquals(4200L, stackMasterPosition(1250L, 4200L, false))
    }

    @Test fun driftCorrectionUsesOffsetAwareTargetWithBoundedTolerance() {
        assertFalse(shouldCorrectStackVoice(4950L, 5000L))
        assertTrue(shouldCorrectStackVoice(4800L, 5000L))
        assertFalse(shouldCorrectStackVoice(5500L, stackVoiceTarget(5000L, 500L)))
    }

    @Test fun stackLoopIsTheDefault() {
        assertTrue(STACK_LOOP_DEFAULT)
        assertTrue(stackTransportUsesLoopIcon(2))
        assertFalse(stackTransportUsesLoopIcon(0))
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

    @Test fun nowPlayingKeepsPrimaryOutOfCompactStackRowsAndPreservesOrder() {
        val slots = listOf(StackSlot(file(1)), StackSlot(file(2)), StackSlot(file(3)))
        assertEquals(
            listOf(slots[1], slots[2]),
            stackSecondarySlots(slots, primaryPath = slots[0].file.path),
        )
        assertEquals(slots, stackSecondarySlots(slots, primaryPath = null))
    }

    @Test fun syncPrefersRateTrimAndSeeksOnlyForLargeDrift() {
        assertEquals(StackSyncAction.NONE, stackSyncAction(8.0, 10_000L))
        assertEquals(StackSyncAction.RATE, stackSyncAction(-120.0, 10_000L))
        assertEquals(StackSyncAction.SEEK, stackSyncAction(600.0, 10_000L))
        assertEquals(StackSyncAction.RATE, stackSyncAction(600.0, 500L))
    }

    @Test fun rateTrimSlowsAheadVoicesAndIsBounded() {
        assertEquals(1f, stackRateTrim(10.0), 1e-6f)
        assertEquals(0.95f, stackRateTrim(100.0), 1e-6f)
        assertEquals(1.02f, stackRateTrim(-40.0), 1e-6f)
        assertEquals(0.95f, stackRateTrim(5_000.0), 1e-6f)
        assertEquals(1.05f, stackRateTrim(-5_000.0), 1e-6f)
        assertEquals(1f, stackRateTrim(Double.NaN), 1e-6f)
    }

    @Test fun seekLeadLearnsFromResidualDrift() {
        assertEquals(210L, stackNextSeekLead(150L, -100.0))
        assertEquals(120L, stackNextSeekLead(150L, 50.0))
        assertEquals(STACK_MAX_SEEK_LEAD_MS, stackNextSeekLead(150L, -1_000.0))
        assertEquals(0L, stackNextSeekLead(20L, 500.0))
    }
}
