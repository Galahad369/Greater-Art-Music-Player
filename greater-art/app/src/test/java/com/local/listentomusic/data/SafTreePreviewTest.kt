package com.local.listentomusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafTreePreviewTest {
    @Test fun audioAndVideoProviderTypesAreCandidates() {
        assertTrue(safMediaCandidate("track", "audio/flac"))
        assertTrue(safMediaCandidate("clip", "video/mp4"))
        assertTrue(safMediaCandidate("VOICE.M4A", "application/octet-stream"))
        assertTrue(safMediaCandidate("VIDEO.MKV", null))
        assertTrue(safMediaCandidate("audio.OPUS", null))
    }

    @Test fun nonMediaProviderDocumentsAreNotIndexed() {
        assertFalse(safMediaCandidate("readme.txt", "text/plain"))
        assertFalse(safMediaCandidate("cover.jpg", "image/jpeg"))
        assertFalse(safMediaCandidate("garbage", "application/octet-stream"))
        assertFalse(safMediaCandidate("folder", "vnd.android.document/directory"))
    }
}
