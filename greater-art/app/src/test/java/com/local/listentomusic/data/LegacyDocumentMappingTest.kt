package com.local.listentomusic.data

import com.local.listentomusic.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyDocumentMappingTest {
    @Test fun literalUnicodeDecodesWithoutSurrogateReplacement() {
        assertEquals("primary:Music/🎵中文.flac", decodeUriComponent("primary:Music/🎵中文.flac"))
        assertEquals("🎵+%", decodeUriComponent("🎵+%25"))
        assertNull(decodeUriComponent("\uD800"))
        assertNull(decodeUriComponent("%ＦＦ"))
    }

    @Test fun nonCanonicalOrDecoratedUrisAreNotDocumentIdentities() {
        for (key in listOf("content://provider/document/a?query=1", "content://provider/document/a#fragment",
            "content://user@provider/document/a", "content://provider:12/document/a",
            "content://provider/document/a/b", "content://provider/document/a#ga-cue=broken")) {
            assertNull(key, parseStableDocumentKey(key))
        }
        assertEquals(DocumentRef("provider", "🎵"), parseStableDocumentKey("content://provider/document/🎵"))
        assertNull(documentIdForLegacyPath("/sdcard/Music/\uD800.mp3", volumes))
    }

    private val volumes = listOf(
        StorageVolumeRoot("primary", listOf("/storage/emulated/0", "/sdcard", "/storage/self/primary")),
        StorageVolumeRoot("1A2B-3C4D", listOf("/storage/1A2B-3C4D")),
    )

    @Test
    fun encodingMatchesAndroidUriEncode() {
        // Values produced by android.net.Uri.encode / DocumentsContract.
        assertEquals("primary%3AMusic%2Fa%20b.mp3", encodeUriComponent("primary:Music/a b.mp3"))
        assertEquals("a_b-c!d.e~f'g(h)i*j", encodeUriComponent("a_b-c!d.e~f'g(h)i*j"))
        assertEquals("100%25%20%23%2B%26%3D", encodeUriComponent("100% #+&="))
        assertEquals("%E5%91%A8%E6%9D%B0%E5%80%AB", encodeUriComponent("周杰倫"))
    }

    @Test
    fun decodingInvertsEncodingForAwkwardNames() {
        for (name in listOf("primary:Music/周杰倫 - 晴天.flac", "a%b", "x+y", "tab\tname", "emoji 🎵.mp3", "", "primary:")) {
            assertEquals(name, decodeUriComponent(encodeUriComponent(name)))
        }
        assertNull(decodeUriComponent("%"))
        assertNull(decodeUriComponent("%4"))
        assertNull(decodeUriComponent("%zz"))
        assertNull(decodeUriComponent("%FF%FF"))   // invalid UTF-8
    }

    @Test
    fun stableKeyMatchesWhatDocumentsContractBuilds() {
        // content://authority/document/<Uri.encode(id)>, the same string ScopedMediaDiscovery uses as MediaFile.id.
        val key = stableDocumentKey(EXTERNAL_STORAGE_AUTHORITY, "primary:Music/Rock/a.mp3")
        assertEquals("content://com.android.externalstorage.documents/document/primary%3AMusic%2FRock%2Fa.mp3", key)
        assertTrue(MediaSource.parse(key) is MediaSource.ContentUri)
        // A tree-qualified URI is a location, never an identity.
        assertNull(parseStableDocumentKey("content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2Fa.mp3"))
    }

    @Test
    fun keysParseBackIncludingCueSuffixes() {
        val id = "primary:Music/周杰倫/晴天 (live).flac"
        val key = stableDocumentKey("com.example.provider", id)
        assertEquals(DocumentRef("com.example.provider", id), parseStableDocumentKey(key))
        assertEquals(DocumentRef("com.example.provider", id), parseStableDocumentKey("$key#ga-cue=1500:98000"))
        assertNull(parseStableDocumentKey("/storage/emulated/0/Music/a.mp3"))
        assertNull(parseStableDocumentKey("content://authority/document/"))
        assertNull(parseStableDocumentKey("content://authority/other/abc"))
        assertNull(parseStableDocumentKey("content://authority"))
    }

    @Test
    fun legacyPathsMapToExternalStorageDocumentIds() {
        assertEquals("primary:Download/a.mp3", documentIdForLegacyPath("/storage/emulated/0/Download/a.mp3", volumes))
        assertEquals("primary:Download/a.mp3", documentIdForLegacyPath("/sdcard/Download/a.mp3", volumes))
        assertEquals("primary:Download/a.mp3", documentIdForLegacyPath("/storage/self/primary//Download///a.mp3/", volumes))
        assertEquals("primary:", documentIdForLegacyPath("/storage/emulated/0", volumes))
        assertEquals("1A2B-3C4D:Music/b.flac", documentIdForLegacyPath("/storage/1A2B-3C4D/Music/b.flac", volumes))
        assertNull(documentIdForLegacyPath("/data/data/app/files/a.mp3", volumes))
        assertNull("sibling with a shared prefix is not inside the root", documentIdForLegacyPath("/storage/emulated/01/a.mp3", volumes))
        assertNull(documentIdForLegacyPath("/storage/emulated/0/Download/../../etc/passwd", volumes))
        assertNull(documentIdForLegacyPath("relative/path.mp3", volumes))
    }

    @Test
    fun longestVolumeRootWins() {
        val nested = listOf(
            StorageVolumeRoot("primary", listOf("/storage/emulated/0")),
            StorageVolumeRoot("usb", listOf("/storage/emulated/0/mnt/usb")),
        )
        assertEquals("usb:song.mp3", documentIdForLegacyPath("/storage/emulated/0/mnt/usb/song.mp3", nested))
        assertEquals("primary:mnt/other/song.mp3", documentIdForLegacyPath("/storage/emulated/0/mnt/other/song.mp3", nested))
    }

    @Test
    fun documentIdsMapBackToTheCanonicalPath() {
        assertEquals("/storage/emulated/0/Download/a.mp3", legacyPathForDocumentId("primary:Download/a.mp3", volumes))
        assertEquals("/storage/emulated/0", legacyPathForDocumentId("primary:", volumes))
        assertEquals("/storage/1A2B-3C4D/Music/b.flac", legacyPathForDocumentId("1A2B-3C4D:Music/b.flac", volumes))
        assertNull(legacyPathForDocumentId("unknown:Music/b.flac", volumes))
        assertNull(legacyPathForDocumentId("primary:Music/../../x", volumes))
        assertNull(legacyPathForDocumentId("no-colon", volumes))
    }

    @Test
    fun storedLegacyKeysMigrateAndRoundTrip() {
        val legacy = "/storage/emulated/0/Download/周杰倫/晴天.flac"
        val migrated = migrateLegacyMediaKey(legacy, volumes)!!
        assertEquals(DocumentRef(EXTERNAL_STORAGE_AUTHORITY, "primary:Download/周杰倫/晴天.flac"), parseStableDocumentKey(migrated))
        assertEquals(legacy, legacyPathForStableKey(migrated, volumes))
        // A cue track keeps its suffix through migration.
        val cue = "$legacy#ga-cue=61000:243500"
        val migratedCue = migrateLegacyMediaKey(cue, volumes)!!
        assertTrue(migratedCue.endsWith("#ga-cue=61000:243500"))
        assertEquals(cue, legacyPathForStableKey(migratedCue, volumes))
        assertNull(migrateLegacyMediaKey("/data/x.mp3", volumes))
        assertNull(legacyPathForStableKey(stableDocumentKey("com.other.provider", "primary:a.mp3"), volumes))
    }

    @Test
    fun relativePathInsideATree() {
        assertEquals("", externalStorageRelativePath("primary:Music", "primary:Music"))
        assertEquals("Rock/a.mp3", externalStorageRelativePath("primary:Music/Rock/a.mp3", "primary:Music"))
        assertEquals("Music/a.mp3", externalStorageRelativePath("primary:Music/a.mp3", "primary:"))
        assertNull(externalStorageRelativePath("primary:Music2/a.mp3", "primary:Music"))
        assertNull(externalStorageRelativePath("primary:Other/a.mp3", "primary:Music"))
        assertNull(externalStorageRelativePath("primary:Music/../Private/a.mp3", "primary:Music"))
        assertNull(externalStorageRelativePath("primary:Music/./a.mp3", "primary:Music"))
    }
}
