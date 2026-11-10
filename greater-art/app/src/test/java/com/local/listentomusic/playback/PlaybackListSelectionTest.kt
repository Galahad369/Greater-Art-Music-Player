package com.local.listentomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackListSelectionTest {
    private val playingMediaId = "content://media/external/audio/media/41"

    @Test fun retappingPlayingLibrarySongKeepsPlaybackPosition() {
        var positionMs = 23_000L
        playListSelection(playingMediaId, playingMediaId, playRequested = true) {
            positionMs = 0L
        }
        assertEquals("ACTIVE_LIST_RETAP: the playing Library row must not restart playback", 23_000L, positionMs)
    }

    @Test fun retappingPlayingQueueEntryDoesNotDispatchPlayback() {
        var requests = 0
        playListSelection(4, 4, playRequested = true) { requests++ }
        assertEquals("ACTIVE_LIST_RETAP: the playing queue row must not seek or play again", 0, requests)
    }

    @Test fun anotherMediaIdOrQueueOccurrenceStillDispatchesPlayback() {
        var requests = 0
        playListSelection("content://media/external/audio/media/42", playingMediaId, playRequested = true) { requests++ }
        playListSelection(5, 4, playRequested = true) { requests++ }
        assertEquals(2, requests)
    }

    @Test fun aPausedEntryOrAnUnknownCurrentEntryCanStillBeSelected() {
        var requests = 0
        playListSelection(playingMediaId, playingMediaId, playRequested = false) { requests++ }
        playListSelection(4, 4, playRequested = false) { requests++ }
        playListSelection(playingMediaId, null, playRequested = false) { requests++ }
        assertEquals(3, requests)
    }
}
