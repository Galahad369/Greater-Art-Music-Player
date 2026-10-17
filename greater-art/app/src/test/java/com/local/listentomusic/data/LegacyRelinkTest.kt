package com.local.listentomusic.data

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRelinkTest {
    @Test fun cueSegmentsCannotCaptureWholeFileReferences() {
        val whole = scoped("primary:Music/disc.flac")
        val segment = whole.copy(path = whole.id + "#ga-cue=1000:2000")
        val id = "/sdcard/Music/disc.flac"
        assertEquals(whole, relinkLegacyMediaIds(listOf(id), listOf(segment, whole), volumes).relinked[id])
        assertTrue(relinkLegacyMediaIds(listOf(id), listOf(segment), volumes).relinked.isEmpty())
        assertEquals(segment, relinkLegacyMediaIds(listOf(segment.id), listOf(segment), volumes).relinked[segment.id])
    }

    @Test fun duplicateListingsAreNotCaseAmbiguityButDifferentDocumentsAre() {
        val file = scoped("primary:Music/Song.MP3")
        val id = "/sdcard/Music/song.mp3"
        assertEquals(file, relinkLegacyMediaIds(listOf(id), listOf(file, file), volumes).relinked[id])
        val twin = scoped("primary:Music/SONG.MP3")
        assertTrue(relinkLegacyMediaIds(listOf(id), listOf(file, file, twin), volumes).relinked.isEmpty())
    }

    @Test fun alternatePercentEncodingOfScopedIdentityMatchesSameDocument() {
        val file = scoped("primary:Music/a.mp3")
        val alternative = file.id.replace("%3A", "%3a").replace("a.mp3", "%61.mp3")
        assertEquals(file, relinkLegacyMediaIds(listOf(alternative), listOf(file), volumes).relinked[alternative])
    }

    private val volumes = listOf(
        StorageVolumeRoot("primary", listOf("/storage/emulated/0", "/sdcard")),
        StorageVolumeRoot("1A2B-3C4D", listOf("/storage/1A2B-3C4D")),
    )

    private fun scoped(documentId: String, authority: String = EXTERNAL_STORAGE_AUTHORITY) = MediaFile(
        path = stableDocumentKey(authority, documentId),
        name = documentId.substringAfterLast('/'),
        durationMs = 0, sizeBytes = 1, modifiedMs = 1, kind = MediaKind.AUDIO,
        sourcePath = "content://$authority/tree/primary%3AMusic/document/${encodeUriComponent(documentId)}",
    )

    @Test
    fun relinksStoredPathsToTheDocumentsThatNowRepresentThem() {
        val a = scoped("primary:Music/周杰倫/晴天 (live).flac")
        val b = scoped("1A2B-3C4D:Albums/b.mp3")
        val result = relinkLegacyMediaIds(
            listOf("/storage/emulated/0/Music/周杰倫/晴天 (live).flac", "/sdcard/Music/周杰倫/晴天 (live).flac", "/storage/1A2B-3C4D/Albums/b.mp3"),
            listOf(a, b), volumes,
        )
        assertEquals(mapOf(
            "/storage/emulated/0/Music/周杰倫/晴天 (live).flac" to a,
            "/sdcard/Music/周杰倫/晴天 (live).flac" to a,
            "/storage/1A2B-3C4D/Albums/b.mp3" to b,
        ), result.relinked)
        assertTrue(result.unresolved.isEmpty())
    }

    @Test
    fun reportsWhyAnIdCouldNotBeRelinked() {
        val result = relinkLegacyMediaIds(
            listOf("/data/data/app/files/x.mp3", "/storage/emulated/0/Music/gone.mp3", "", "/storage/emulated/0/Music/../../x.mp3", "relative.mp3"),
            listOf(scoped("primary:Music/here.mp3")), volumes,
        )
        assertTrue(result.relinked.isEmpty())
        assertEquals(RelinkFailure.NOT_A_KNOWN_VOLUME_PATH, result.unresolved["/data/data/app/files/x.mp3"])
        assertEquals(RelinkFailure.NOT_IN_SCOPED_LIBRARY, result.unresolved["/storage/emulated/0/Music/gone.mp3"])
        assertEquals(RelinkFailure.UNSUPPORTED_ID, result.unresolved[""])
        assertEquals(RelinkFailure.NOT_A_KNOWN_VOLUME_PATH, result.unresolved["/storage/emulated/0/Music/../../x.mp3"])
        assertEquals(RelinkFailure.UNSUPPORTED_ID, result.unresolved["relative.mp3"])
    }

    @Test
    fun caseInsensitiveFallbackIsUsedOnlyWhenUnambiguous() {
        val only = scoped("primary:Music/Song.MP3")
        assertEquals(only, relinkLegacyMediaIds(listOf("/storage/emulated/0/Music/song.mp3"), listOf(only), volumes).relinked.values.single())
        // Two different documents that differ only by case are never merged.
        val twins = listOf(scoped("primary:Music/Song.mp3"), scoped("primary:Music/SONG.mp3"))
        val result = relinkLegacyMediaIds(listOf("/storage/emulated/0/Music/song.mp3"), twins, volumes)
        assertEquals(RelinkFailure.NOT_IN_SCOPED_LIBRARY, result.unresolved["/storage/emulated/0/Music/song.mp3"])
        // An exact match wins over any case-folded one.
        assertEquals(twins[1], relinkLegacyMediaIds(listOf("/storage/emulated/0/Music/SONG.mp3"), twins, volumes).relinked.values.single())
    }

    @Test
    fun scopedIdentitiesAndCueSegmentsAreHandledHonestly() {
        val file = scoped("primary:Music/disc.flac")
        val alreadyScoped = file.id
        val cueLegacy = "/storage/emulated/0/Music/disc.flac#ga-cue=61000:243500"
        val result = relinkLegacyMediaIds(listOf(alreadyScoped, "content://other/document/zzz", cueLegacy), listOf(file), volumes)
        assertEquals(file, result.relinked[alreadyScoped])
        assertEquals(RelinkFailure.NOT_IN_SCOPED_LIBRARY, result.unresolved["content://other/document/zzz"])
        assertEquals(RelinkFailure.CUE_TRACK_NOT_EXPANDED, result.unresolved[cueLegacy])
        // If discovery ever lists the expanded cue track under the migrated id, it relinks.
        val track = file.copy(path = migrateLegacyMediaKey(cueLegacy, volumes)!!)
        assertEquals(track, relinkLegacyMediaIds(listOf(cueLegacy), listOf(file, track), volumes).relinked[cueLegacy])
    }

    @Test
    fun duplicatesCollapseAndOrderIsStable() {
        val a = scoped("primary:Music/a.mp3"); val b = scoped("primary:Music/b.mp3")
        val ids = listOf("/storage/emulated/0/Music/b.mp3", "/storage/emulated/0/Music/a.mp3", "/storage/emulated/0/Music/b.mp3")
        assertEquals(listOf(ids[0], ids[1]), relinkLegacyMediaIds(ids, listOf(a, b), volumes).relinked.keys.toList())
    }
}
