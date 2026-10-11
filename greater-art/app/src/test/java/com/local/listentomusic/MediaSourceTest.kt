package com.local.listentomusic

import com.local.listentomusic.model.*
import org.junit.Assert.*
import org.junit.Test

class MediaSourceTest {
    @Test fun legacyUnicodePathsAreLossless() {
        val id = "/storage/emulated/0/Download/孤独毒毒 # take.flac"
        val result = migrateLegacyMediaId(id) as LegacyMediaMigration.Migrated
        assertEquals(id, result.reference.id)
        assertEquals(MediaSource.LocalFile(id), result.reference.source)
    }

    @Test fun cueIdentityIsSeparateFromPhysicalLocation() {
        val result = migrateLegacyMediaId("/Download/album.flac#ga-cue=1200:3400")
            as LegacyMediaMigration.Migrated
        assertEquals("/Download/album.flac", result.reference.source.value)
        assertEquals(1200L, result.reference.startMs)
        assertEquals(3400L, result.reference.endMs)
        assertEquals("/Download/album.flac#ga-cue=1200:3400", result.reference.id)
    }

    @Test fun contentLocationsStayOpaqueAndUnchanged() {
        val value = "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2F%E6%AD%8C.mp3"
        assertEquals(MediaSource.ContentUri(value), MediaSource.parse(value))
        assertEquals(value, (migrateLegacyMediaId(value) as LegacyMediaMigration.Migrated).reference.id)
    }

    @Test fun unsupportedRecordsAreRetained() {
        listOf("https://example.com/song.mp3", "relative.mp3", "file:///Download/song.mp3",
            "content:opaque", "content:///song", "content://user@provider/song",
            "content://provider:12/song", "content://provider/song#fragment", "/Download/a\u0000.mp3")
            .forEach { value ->
                assertNull(value, MediaSource.parse(value))
                assertEquals(LegacyMediaMigration.Unsupported(value), migrateLegacyMediaId(value))
            }
    }

    @Test fun clipBoundsAreValidated() {
        val source = MediaSource.LocalFile("/Download/song.mp3")
        assertThrows(IllegalArgumentException::class.java) { MediaReference("id", source, -1) }
        assertThrows(IllegalArgumentException::class.java) { MediaReference("id", source, 10, 10) }
    }
}
