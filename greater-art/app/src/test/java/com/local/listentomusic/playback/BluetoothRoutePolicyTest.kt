package com.local.listentomusic.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothRoutePolicyTest {
    @Test fun replacingBluetoothOutputDoesNotPauseDuringHandoff() {
        assertFalse(shouldPauseForBluetoothOutputLoss(true, true, false))
        assertFalse(shouldPauseForBluetoothOutputLoss(true, true, true))
    }

    @Test fun lastBluetoothOutputRemovalPausesPlayback() {
        assertTrue(shouldPauseForBluetoothOutputLoss(true, false, false))
        assertTrue(shouldPauseForBluetoothOutputLoss(true, false, true))
    }

    @Test fun privateModeRequiresBluetoothOutputEvenWithoutRemovalEvent() {
        assertTrue(shouldPauseForBluetoothOutputLoss(false, false, true))
        assertFalse(shouldPauseForBluetoothOutputLoss(false, true, true))
        assertFalse(shouldPauseForBluetoothOutputLoss(false, false, false))
    }
}
