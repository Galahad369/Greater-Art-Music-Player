package com.local.listentomusic.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackPictureFailureTest {
    @Test fun onlyAnIdleWidePictureTimeoutOffersAWideRetry() {
        val progress = StackAlignmentProgress(method = StackAlignmentMethod.PICTURE, total = 1,
            matched = 0, widePictureSearch = true, pictureFailures = mapOf("take" to StackPictureFailure.DECODE_TIME_LIMIT))
        assertTrue(progress.canRetryFailedPictureScan)
        assertTrue(progress.copy(pictureFailures = mapOf("take" to StackPictureFailure.WAIT_TIME_LIMIT)).canRetryFailedPictureScan)
        assertFalse(progress.copy(running = true).canRetryFailedPictureScan)
        assertFalse(progress.copy(method = StackAlignmentMethod.SOUND).canRetryFailedPictureScan)
        assertFalse(progress.copy(pictureFailures = emptyMap()).canRetryFailedPictureScan)
        assertFalse(progress.copy(pictureFailures = mapOf("take" to StackPictureFailure.UNSUPPORTED)).canRetryFailedPictureScan)
    }
}
