package com.local.listentomusic.data

import com.local.listentomusic.model.findMatchingLrc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.Random

class SafSidecarsTest {
    @Test
    fun lyricsPreferTtmlThenLrcThenSrtForTheSameStem() {
        val names = listOf("song.srt", "song.lrc", "song.ttml", "other.lrc")
        assertEquals("song.ttml", matchLyricsSidecar("song.mp3", names))
        assertEquals("song.lrc", matchLyricsSidecar("song.mp3", names - "song.ttml"))
        assertEquals("song.srt", matchLyricsSidecar("song.mp3", listOf("song.srt", "other.lrc")))
        assertNull(matchLyricsSidecar("song.mp3", listOf("other.lrc", "song.txt", "song.mp3")))
    }

    @Test
    fun lyricsMatchCaseInsensitivelyAndByFullMediaName() {
        assertEquals("SONG.LRC", matchLyricsSidecar("Song.mp3", listOf("SONG.LRC")))
        assertEquals("song.mp3.lrc", matchLyricsSidecar("Song.MP3", listOf("song.mp3.lrc")))
        assertEquals("a.b.lrc", matchLyricsSidecar("a.b.flac", listOf("a.b.lrc")))
    }

    @Test
    fun artworkFollowsTheThumbnailRepositoryOrder() {
        // Same-stem image beats folder art; within each group the extension order is jpg, jpeg, png, webp, bmp.
        assertEquals("song.png", matchArtworkSidecar("song.mp3", listOf("cover.jpg", "song.png", "folder.png")))
        assertEquals("cover.jpg", matchArtworkSidecar("song.mp3", listOf("artwork.jpg", "cover.jpg", "front.png")))
        assertEquals("cover.jpg", matchArtworkSidecar("song.mp3", listOf("cover.png", "cover.jpg")))
        assertEquals("Folder.JPG", matchArtworkSidecar("song.mp3", listOf("Folder.JPG", "notes.txt")))
        assertNull(matchArtworkSidecar("song.mp3", listOf("song.gif", "photo.jpg", "cover.txt")))
    }

    @Test
    fun theSharedConstantsAreTheOnesThumbnailsUse() {
        assertEquals(listOf("jpg", "jpeg", "png", "webp", "bmp"), SidecarNames.IMAGE_EXTENSIONS.toList())
        assertEquals(listOf("cover", "folder", "front", "album", "artwork"), SidecarNames.FOLDER_ART_NAMES.toList())
    }

    /** The real File-based rule and the listing-based rule must agree on a real directory. */
    @Test
    fun listingMatchAgreesWithTheFileBasedLyricsRule() {
        val dir = Files.createTempDirectory("sidecars").toFile()
        try {
            val scenarios = listOf(
                "a.mp3" to listOf("a.lrc"),
                "a.mp3" to listOf("a.lrc", "a.srt"),
                "a.mp3" to listOf("a.srt", "a.ttml"),
                "a.mp3" to listOf("b.lrc"),
                "Song.mp3" to listOf("song.LRC"),          // only the case-insensitive fallback can find this on Linux
                "x.y.flac" to listOf("x.y.lrc", "x.lrc"),
                "t.m4a" to listOf("t.m4a.lrc"),
                "t.m4a" to emptyList(),
            )
            for ((media, siblings) in scenarios) {
                dir.listFiles()!!.forEach { it.deleteRecursively() }
                File(dir, media).writeText("x")
                siblings.forEach { File(dir, it).writeText("x") }
                val viaFiles = findMatchingLrc(File(dir, media).path)?.name
                val viaListing = matchLyricsSidecar(media, dir.list()!!.sorted())
                if (viaFiles == null || viaListing == null) {
                    assertEquals("media=$media siblings=$siblings", viaFiles, viaListing)
                } else {
                    // File probes preserve requested casing on Windows while the
                    // listing preserves disk casing. Compare the actual document.
                    assertTrue("media=$media siblings=$siblings",
                        Files.isSameFile(File(dir, viaFiles).toPath(), File(dir, viaListing).toPath()))
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    // The list-scanning rule the per-folder index replaced, kept as the reference it must agree with.
    private fun stem(n: String) = n.substringBeforeLast(".")
    private fun ext(n: String) = n.substringAfterLast('.', "")
    private fun naiveLyrics(media: String, sib: List<String>): String? {
        for (e in SidecarNames.LYRICS_PRIORITY) sib.firstOrNull { stem(it).equals(stem(media), true) && ext(it).equals(e, true) }?.let { return it }
        val accepted = setOf(stem(media).lowercase(Locale.ROOT), media.lowercase(Locale.ROOT))
        return sib.firstOrNull { ext(it).lowercase(Locale.ROOT) in SidecarNames.LYRICS_PRIORITY && stem(it).lowercase(Locale.ROOT) in accepted }
    }
    private fun naiveArtwork(media: String, sib: List<String>): String? {
        val candidates = buildList {
            SidecarNames.IMAGE_EXTENSIONS.forEach { add("${stem(media)}.$it") }
            SidecarNames.FOLDER_ART_NAMES.forEach { n -> SidecarNames.IMAGE_EXTENSIONS.forEach { add("$n.$it") } }
        }
        for (c in candidates) sib.firstOrNull { it.equals(c, true) }?.let { return it }
        return null
    }

    @Test
    fun theFolderIndexAgreesWithTheNaiveRuleOnRandomMessyFolders() {
        val pool = listOf("a.mp3", "A.LRC", "a.srt", "a.ttml", "b.flac", "b.lrc", "Song.mp3", "song.LRC", "song.png", "SONG.jpg", "x.y.mp3", "x.y.lrc", "x.lrc",
            "song.mp3.lrc", "Song.MP3.srt", "cover.jpg", "Folder.PNG", "FRONT.webp", "album.bmp", "artwork.jpeg", "cover.png", "notes.txt", "noext", ".hidden.lrc", "a.b.c.lrc")
        val media = pool.filter { ext(it).lowercase(Locale.ROOT) in setOf("mp3", "flac") }
        var checked = 0
        for (seed in 1L..400L) {
            val random = Random(seed)
            val siblings = pool.filter { random.nextInt(3) == 0 }.shuffled(random)
            val index = SidecarIndex(siblings)
            for (m in media) {
                assertEquals("lyrics seed=$seed media=$m siblings=$siblings", naiveLyrics(m, siblings), index.lyricsFor(m))
                assertEquals("artwork seed=$seed media=$m siblings=$siblings", naiveArtwork(m, siblings), index.artworkFor(m))
                checked++
            }
        }
        assertTrue(checked > 1000)
    }
}
