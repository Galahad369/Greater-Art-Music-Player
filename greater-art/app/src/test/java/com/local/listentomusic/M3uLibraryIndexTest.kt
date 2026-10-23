package com.local.listentomusic

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.M3uLibraryIndex
import org.junit.Assert.*
import org.junit.Test

class M3uLibraryIndexTest {
    private fun media(id: String, source: String = id, start: Long = 0, end: Long? = null) =
        MediaFile(id, id, 20_000, 100, 1, MediaKind.AUDIO,
            sourcePath = source, clipStartMs = start, clipEndMs = end)

    @Test fun contentIdAndSourceUseTheSameFirstMatchAsTheLegacyScan() {
        val uri = "content://media/external/audio/42"
        val files = listOf(media("clip-first", uri), media(uri), media("clip-later", uri))
        val index = M3uLibraryIndex(files)
        for (reference in listOf(uri, "clip-first", "clip-later", "content://missing")) {
            assertEquals(files.firstOrNull { it.id == reference || it.sourcePath == reference }, index.content(reference))
        }
    }

    @Test fun exactClipSourceWinsOverDuplicateBasenames() {
        val first = media("one", "/a/音楽.flac", 1000, 2000)
        val second = media("two", "/b/音楽.flac", 1000, 2000)
        val index = M3uLibraryIndex(listOf(first, second))
        assertEquals(second, index.clipped(second.sourcePath, 1000, 2000))
        assertNull(index.clipped("/moved/音楽.flac", 1000, 2000))
    }

    @Test fun clipRangesAndOpenEndsStayDistinct() {
        val first = media("bounded", "/a/song.flac", 1000, 2000)
        val open = media("open", "/a/song.flac", 1000, null)
        val index = M3uLibraryIndex(listOf(first, open))
        assertEquals(open, index.clipped("/moved/song.flac", 1000, null))
        assertEquals(first, index.clipped("/moved/song.flac", 1000, 2000))
        assertNull(index.clipped(first.sourcePath, 2000, 3000))
    }
}
