package com.local.listentomusic

import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.model.MediaKind
import java.nio.file.Files
import java.nio.file.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaScannerTest {
    private lateinit var root: Path

    @Before
    fun createLibrary() {
        root = Files.createTempDirectory("greater-art-scanner")
    }

    @After
    fun removeLibrary() {
        root.toFile().deleteRecursively()
    }

    @Test
    fun recursivelyFindsSupportedMediaAndSkipsOtherFiles() {
        val nested = Files.createDirectories(root.resolve("album/deep"))
        Files.write(root.resolve("top-level.MP3"), byteArrayOf(1, 2, 3))
        Files.write(nested.resolve("lossless.FLAC"), byteArrayOf(4, 5, 6))
        Files.write(nested.resolve("video.MP4"), byteArrayOf(7, 8, 9))
        Files.write(nested.resolve("notes.txt"), byteArrayOf(10))

        val found = MediaScanner.scanFolder(root.toFile())
        val names = found.map { it.name }.toSet()

        assertEquals(setOf("top-level.MP3", "lossless.FLAC", "video.MP4"), names)
        assertFalse("notes.txt" in names)
        assertEquals(MediaKind.VIDEO, found.single { it.name == "video.MP4" }.kind)
        assertTrue(found.filterNot { it.name == "video.MP4" }.all { it.kind == MediaKind.AUDIO })
    }

    @Test
    fun ignoresTrashedPendingAppleDoubleAndHiddenCueFiles() {
        Files.write(root.resolve("keep.mp3"), byteArrayOf(1))
        Files.write(root.resolve("my.song.v2.mp3"), byteArrayOf(2))
        Files.write(root.resolve(".trashed-1790000000-gone.mp3"), byteArrayOf(3))
        Files.write(root.resolve(".pending-1790000000-partial.flac"), byteArrayOf(4))
        Files.write(root.resolve("._keep.mp3"), byteArrayOf(5))
        Files.write(root.resolve(".hidden-album.cue"), byteArrayOf(6))

        val names = MediaScanner.scanFolder(root.toFile()).map { it.name }.toSet()

        assertEquals(setOf("keep.mp3", "my.song.v2.mp3"), names)
    }

    @Test
    fun keepsTheRequestedHiResExtensionsDiscoverable() {
        assertTrue(
            setOf("flac", "alac", "ape", "dsf", "dff", "aiff", "opus")
                .all(MediaScanner.supportedExtensions::contains),
        )
    }
}
