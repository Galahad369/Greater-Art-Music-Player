package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StackRecommendTest {
    private fun file(path: String, name: String, artist: String = "", duration: Long = 180_000L) = MediaFile(
        path = path,
        name = name,
        durationMs = duration,
        sizeBytes = 1L,
        modifiedMs = 0L,
        kind = MediaKind.AUDIO,
        artist = artist,
    )

    @Test fun recommendsSameFolderAndSimilarName() {
        val seed = file("/music/live/set-a.mp3", "set-a.mp3", "Band")
        val library = listOf(
            seed,
            file("/music/live/set-b.mp3", "set-b.mp3", "Band"),
            file("/other/unrelated.mp3", "zzzz.mp3", "Nobody"),
        )
        val rec = recommendStackTracks(listOf(seed), library, limit = 4)
        assertTrue(rec.any { it.path.endsWith("set-b.mp3") })
    }

    @Test fun excludesSeedsAndRespectsLimit() {
        val seed = file("/a/one.mp3", "one.mp3")
        val library = (1..10).map { file("/a/track$it.mp3", "track$it.mp3", "Same") }
        val rec = recommendStackTracks(listOf(seed), library + seed, limit = 3)
        assertEquals(3, rec.size)
        assertTrue(rec.none { it.path == seed.path })
    }
}
