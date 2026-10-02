package com.local.listentomusic.playback

import com.local.listentomusic.data.PlayHistoryEntry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StackRecommendTest {
    private fun file(
        path: String,
        name: String,
        artist: String = "",
        album: String = "",
        duration: Long = 180_000L,
    ) = MediaFile(
        path = path,
        name = name,
        durationMs = duration,
        sizeBytes = 1L,
        modifiedMs = 0L,
        kind = MediaKind.AUDIO,
        artist = artist,
        album = album,
    )

    @Test fun recommendsSameFolderAndMetadata() {
        val seed = file("/music/live/set-a.mp3", "set-a.mp3", "Band", "Live")
        val related = file("/music/live/set-b.mp3", "set-b.mp3", "Band", "Live")
        val unrelated = file("/other/unrelated.mp3", "zzzz.mp3", "Nobody", "Other", 420_000L)
        val rec = recommendStackTracks(listOf(seed), listOf(seed, unrelated, related), limit = 4)
        assertEquals(related.path, rec.first().file.path)
        assertTrue(rec.first().reasons.contains(StackRecommendationReason.SAME_FOLDER))
        assertTrue(rec.first().reasons.contains(StackRecommendationReason.SAME_ARTIST))
    }

    @Test fun localHistoryCanRecommendARecentNeighbor() {
        val seed = file("/a/one.mp3", "one.mp3")
        val neighbor = file("/x/history-neighbor.mp3", "totally-different.mp3", duration = 600_000L)
        val history = listOf(PlayHistoryEntry(seed.path, 3_000L), PlayHistoryEntry(neighbor.path, 2_000L))
        val rec = recommendStackTracks(listOf(seed), listOf(seed, neighbor), history, limit = 4)
        assertEquals(neighbor.path, rec.single().file.path)
        assertTrue(rec.single().reasons.contains(StackRecommendationReason.PLAY_HISTORY))
    }

    @Test fun excludesSeedsAndRespectsLimit() {
        val seed = file("/a/one.mp3", "one.mp3", artist = "Same")
        val library = (1..10).map { file("/a/track$it.mp3", "track$it.mp3", artist = "Same") }
        val rec = recommendStackTracks(listOf(seed), library + seed, limit = 3)
        assertEquals(3, rec.size)
        assertTrue(rec.none { it.file.path == seed.path })
    }
}
