package com.local.listentomusic.data

import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Random

/** Scoped folder exclusions and URI sidecar selection (stage C remainder), plus parity with the real File scanner. */
class ScopedMediaDiscoveryFoldersTest {
    @Test fun sidecarsRejectNonContentLocations() = runBlocking {
        val access = Access(mapOf("root" to listOf(file("song", "song.mp3"),
            file("lyrics", "song.lrc").copy(uri = "file:///private/song.lrc"),
            file("art", "cover.jpg").copy(uri = "https://example.invalid/cover.jpg"))))
        val result = ScopedMediaDiscovery(access).scan(listOf("content://tree"), false)
        assertEquals(ScopedSidecars(), result.records.single().sidecars)
    }

    private fun doc(id: String) = "content://provider/document/${encodeUriComponent(id)}"
    private fun uri(id: String) = "content://provider/tree/t/document/${encodeUriComponent(id)}"
    private fun file(id: String, name: String, mime: String = "application/octet-stream", size: Long = 1) =
        ScopedRow(id, doc(id), uri(id), name, mime, size = size)
    private fun folder(id: String, name: String) = ScopedRow(id, "", "", name, null, directory = true)

    private class Access(val children: Map<String, List<ScopedRow>>) : ScopedDiscoveryAccess {
        var visited = mutableListOf<String>()
        override suspend fun mediaStore(limit: Int) = ScopedListing(emptyList())
        override fun treeRoot(tree: String) = "root"
        override fun hasReadGrant(tree: String) = true
        override suspend fun children(tree: String, documentId: String, limit: Int): ScopedListing {
            visited += documentId
            return ScopedListing(children[documentId].orEmpty().take(limit))
        }
    }

    private val tree = mapOf(
        "root" to listOf(file("a", "a.mp3"), folder("rock", "Rock"), folder("priv", "Private")),
        "rock" to listOf(file("b", "b.flac"), folder("live", "Live")),
        "live" to listOf(file("c", "c.mp3")),
        "priv" to listOf(file("s", "secret.mp3"), folder("deep", "deep")),
        "deep" to listOf(file("d", "also.mp3")),
    )

    private fun names(excluded: Set<String>): List<String> = runBlocking {
        ScopedMediaDiscovery(Access(tree)).scan(listOf("content://tree"), false, excludedFolders = excluded).records.map { it.media.name }.sorted()
    }

    @Test
    fun excludedFoldersPruneTheirWholeSubtreeLikeTheFileScanner() {
        val all = listOf("a.mp3", "also.mp3", "b.flac", "c.mp3", "secret.mp3")
        assertEquals(all, names(emptySet()))
        assertEquals(listOf("a.mp3", "b.flac", "c.mp3"), names(setOf("Private")))
        assertEquals(listOf("a.mp3", "also.mp3", "b.flac", "secret.mp3"), names(setOf("Rock/Live")))
        assertEquals(all, names(setOf("Priv")))            // a name prefix is not a folder match
        assertEquals(all, names(setOf("deep")))            // only paths relative to the granted root count
        assertEquals(listOf("a.mp3", "b.flac", "c.mp3", "secret.mp3"), names(setOf("Private/deep")))
        assertEquals(listOf("a.mp3", "b.flac", "c.mp3"), names(setOf("\\Private\\")))   // slash-normalised
        assertEquals(all, names(setOf("", "  /", "/")))    // blanks exclude nothing
    }

    @Test
    fun exclusionsIgnoreCaseAndAmbiguousTransportStreamDocuments() {
        assertEquals(listOf("a.mp3", "b.flac", "c.mp3"), names(setOf("pRiVaTe")))
        assertEquals(listOf("a.mp3", "also.mp3", "b.flac", "secret.mp3"), names(setOf("rOcK/lIvE")))
        assertNull(scopedMediaKind("script.ts", "application/typescript"))
        assertNull(scopedMediaKind("unknown.ts", "application/octet-stream"))
        assertEquals(MediaKind.VIDEO, scopedMediaKind("movie.ts", "video/mp2t"))
    }

    @Test
    fun excludedFoldersAreNeverQueried() = runBlocking {
        val access = Access(tree)
        ScopedMediaDiscovery(access).scan(listOf("content://tree"), false, excludedFolders = setOf("Private"))
        assertTrue("excluded folders must not cost a provider round trip", "priv" !in access.visited && "deep" !in access.visited)
        assertEquals(listOf("root", "rock", "live"), access.visited)
    }

    @Test
    fun exclusionDoesNotReportAPartialResult() = runBlocking {
        val result = ScopedMediaDiscovery(Access(tree)).scan(listOf("content://tree"), false, excludedFolders = setOf("Private"))
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun sidecarsComeFromTheSameFolderListingAndAreGrantQualifiedUris() = runBlocking {
        val access = Access(mapOf("root" to listOf(
            file("m1", "song.mp3"), file("l1", "song.lrc"), file("l2", "song.srt"), file("art", "Cover.JPG"),
            file("m2", "other.flac"), file("nm", "notes.txt"), folder("sub", "sub"),
        ), "sub" to listOf(file("m3", "song.mp3"))))
        val records = ScopedMediaDiscovery(access).scan(listOf("content://tree"), false).records.associateBy { it.media.name + "@" + it.media.id }
        val song = records.getValue("song.mp3@" + doc("m1")).sidecars
        assertEquals(uri("l1"), song.lyricsUri)           // lrc beats srt
        assertEquals(uri("art"), song.artworkUri)         // folder art, matched case-insensitively
        val other = records.getValue("other.flac@" + doc("m2")).sidecars
        assertNull(other.lyricsUri); assertEquals(uri("art"), other.artworkUri)
        // A file in another folder never borrows this folder's sidecars.
        assertEquals(ScopedSidecars(), records.getValue("song.mp3@" + doc("m3")).sidecars)
    }

    @Test
    fun mediaStoreRecordsHaveNoSidecars() = runBlocking {
        val access = object : ScopedDiscoveryAccess {
            override suspend fun mediaStore(limit: Int) = ScopedListing(listOf(file("x", "x.mp3", "audio/mpeg")))
            override fun treeRoot(tree: String) = "root"
            override fun hasReadGrant(tree: String) = true
            override suspend fun children(tree: String, documentId: String, limit: Int) = ScopedListing(emptyList())
        }
        assertEquals(ScopedSidecars(), ScopedMediaDiscovery(access).scan(emptyList()).records.single().sidecars)
    }

    // ---- parity with the real File scanner ------------------------------------------------------------------------------

    private fun mirror(root: File): Map<String, List<ScopedRow>> {
        val out = HashMap<String, List<ScopedRow>>()
        fun walk(dir: File, dirId: String) {
            out[dirId] = dir.listFiles()!!.map { f ->
                val id = if (dirId == "root") f.name else "$dirId/${f.name}"
                if (f.isDirectory) { walk(f, id); folder(id, f.name) } else file(id, f.name, size = f.length())
            }.shuffled(Random(dirId.hashCode().toLong()))
        }
        walk(root, "root")
        return out
    }

    private fun randomTree(root: File, random: Random) {
        val fileNames = listOf("s1.mp3", "Clip.MP4", "track.FLAC", "notes.txt", "cover.jpg", ".trashed-9-x.mp3", "._y.m4a", "album.cue",
            "x.song.v2.ogg", "noext", "voice.aac", "movie.MKV", "lossless.ALAC", "image.png", "a.lrc", "old.wma", "UPPER.MP3")
        val folderNames = listOf("A", "B", "c d", "周杰倫", ".hidden")
        fun fill(dir: File, depth: Int) {
            fileNames.filter { random.nextInt(3) == 0 }.forEach { File(dir, it).writeText("x") }
            if (depth < 3) folderNames.filter { random.nextInt(3) == 0 }.forEach { name -> File(dir, name).also { it.mkdirs(); fill(it, depth + 1) } }
        }
        fill(root, 0)
    }

    @Test
    fun discoveryMatchesTheRealFileScannerOnRandomTreesIncludingExclusions() {
        var compared = 0
        for (seed in 1L..40L) {
            val random = Random(seed)
            val root = Files.createTempDirectory("scoped$seed").toFile()
            try {
                randomTree(root, random)
                val folders = root.walkTopDown().filter { it.isDirectory && it != root }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
                val excluded = if (folders.isNotEmpty() && random.nextBoolean()) setOf(folders[random.nextInt(folders.size)]) else emptySet()
                val viaFiles = MediaScanner.scanFolder(root, excluded)
                    .map { Triple(File(it.sourcePath).relativeTo(root).invariantSeparatorsPath, it.kind, it.sizeBytes) }.sortedBy { it.first }
                val viaDiscovery = runBlocking {
                    ScopedMediaDiscovery(Access(mirror(root))).scan(listOf("content://tree"), false, excludedFolders = excluded)
                }.records.map { Triple(parseStableDocumentKey(it.media.id)!!.documentId, it.media.kind, it.media.sizeBytes) }.sortedBy { it.first }
                assertEquals("seed=$seed excluded=$excluded", viaFiles, viaDiscovery)
                compared += viaFiles.size
            } finally {
                root.deleteRecursively()
            }
        }
        assertTrue("the random trees must actually contain media", compared > 300)
    }
}
