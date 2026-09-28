package com.local.listentomusic.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRestorePolicyTest {
    @Test fun untouchedLiveServiceMayRestoreTheSavedItem() {
        assertTrue(shouldRestoreSavedSession(serviceAlive = true, mediaItemCount = 0))
    }

    @Test fun delayedRestoreCannotReplaceAUserSelectedQueue() {
        assertFalse(shouldRestoreSavedSession(serviceAlive = true, mediaItemCount = 1))
        assertFalse(shouldRestoreSavedSession(serviceAlive = true, mediaItemCount = 209))
    }

    @Test fun destroyedServiceCannotRestore() {
        assertFalse(shouldRestoreSavedSession(serviceAlive = false, mediaItemCount = 0))
    }
}
