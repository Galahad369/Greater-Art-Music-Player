package com.local.listentomusic.ui

import org.junit.Assert.*
import org.junit.Test

class DocumentReadGrantTest {
    @Test fun successfulCallWithoutGrantIsNotSuccess() {
        assertFalse(retainDocumentReadGrant({}, { false }))
    }
    @Test fun existingGrantSurvivesProviderTakeFailure() {
        assertTrue(retainDocumentReadGrant({ error("provider") }, { true }))
    }
    @Test fun failedReleaseWithRemainingGrantIsNotRevoked() {
        assertFalse(releaseDocumentReadGrant({ error("provider") }, { true }))
    }
    @Test fun alreadyAbsentGrantCanBeCleared() {
        assertTrue(releaseDocumentReadGrant({ error("already absent") }, { false }))
    }
    @Test fun unreadableGrantStateFailsClosed() {
        assertFalse(retainDocumentReadGrant({}, { error("query") }))
        assertFalse(releaseDocumentReadGrant({}, { error("query") }))
    }
}
