package com.local.listentomusic.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ScopedMediaDiscoveryTest {
    private fun audio(id: String = "take", uri: String = "content://provider/document/$id") =
        ScopedRow(id, "content://provider/document/$id", uri, "孤独毒毒 $id.mp3", "audio/mpeg")
    private fun folder(id: String) = ScopedRow(id, "", "", id, null, directory = true)
    private class Access : ScopedDiscoveryAccess {
        var granted = true
        var store = ScopedListing(emptyList())
        var storeDenied = false
        var visits = 0
        val children = mutableMapOf<String, List<ScopedRow>>()
        var onVisit: (() -> Unit)? = null
        override suspend fun mediaStore(limit: Int): ScopedListing {
            if (storeDenied) throw SecurityException()
            return store
        }
        override fun treeRoot(tree: String): String {
            require(tree.startsWith("content://")); return "root"
        }
        override fun hasReadGrant(tree: String) = granted
        override suspend fun children(tree: String, documentId: String, limit: Int): ScopedListing {
            visits++; onVisit?.invoke()
            val rows = children[documentId].orEmpty()
            return ScopedListing(rows.take(limit), rows.size > limit)
        }
    }

    @Test fun freshInstallNeedsNoTreeAndReturnsOwnedIndex() = runBlocking {
        val access = Access().apply { store = ScopedListing(listOf(audio())) }
        val result = ScopedMediaDiscovery(access).scan(emptyList())
        assertEquals(1, result.records.size)
        assertEquals(0, access.visits)
        assertEquals(ScopedOrigin.MEDIA_STORE, result.records.single().origin)
    }

    @Test fun rescanningKeepsIdentityButRefreshesMetadata() = runBlocking {
        val access = Access().apply { children["root"] = listOf(audio()) }
        val scanner = ScopedMediaDiscovery(access)
        val first = scanner.scan(listOf("content://tree"), false).records.single().media
        access.children["root"] = listOf(audio().copy(name = "renamed.mp3", size = 123))
        val second = scanner.scan(listOf("content://tree"), false).records.single().media
        assertEquals(first.id, second.id)
        assertEquals(123L, second.sizeBytes)
        assertEquals("renamed.mp3", second.name)
    }

    @Test fun missingGrantNeverQueriesFolder() = runBlocking {
        val access = Access().apply { granted = false }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://tree"), false)
        assertTrue(result.records.isEmpty()); assertEquals(0, access.visits)
        assertEquals(DiscoveryProblem.PERMISSION_DENIED, result.issues.single().problem)
    }

    @Test fun midScanRevocationDiscardsPartialRoot() = runBlocking {
        val access = Access().apply {
            children["root"] = listOf(audio())
            onVisit = { granted = false }
        }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://tree"), false)
        assertTrue(result.records.isEmpty())
        assertEquals(DiscoveryProblem.PERMISSION_DENIED, result.issues.single().problem)
    }

    @Test fun revokedRescanDoesNotKeepOldEntries() = runBlocking {
        val access = Access().apply { children["root"] = listOf(audio()) }
        val scanner = ScopedMediaDiscovery(access)
        assertEquals(1, scanner.scan(listOf("content://tree"), false).records.size)
        access.granted = false
        assertTrue(scanner.scan(listOf("content://tree"), false).records.isEmpty())
    }

    @Test fun revocationWhileAnotherRootScansDoesNotPublishEarlierRoot() = runBlocking {
        val granted = mutableSetOf("content://treeA", "content://treeB")
        val access = object : ScopedDiscoveryAccess {
            override suspend fun mediaStore(limit: Int) = ScopedListing(emptyList())
            override fun treeRoot(tree: String) = "root"
            override fun hasReadGrant(tree: String) = tree in granted
            override suspend fun children(tree: String, documentId: String, limit: Int): ScopedListing {
                if (tree == "content://treeB") granted.remove("content://treeA")
                return ScopedListing(listOf(audio(if (tree.endsWith('A')) "a" else "b")))
            }
        }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://treeA", "content://treeB"), false)
        assertEquals("content://provider/document/b", result.records.single().media.id)
        assertEquals(DiscoveryProblem.PERMISSION_DENIED, result.issues.single().problem)
    }

    @Test fun cyclesAndRepeatedRootsDoNotRequeryForever() = runBlocking {
        val access = Access().apply {
            children["root"] = listOf(folder("root"), audio(), audio())
        }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://tree", "content://tree"), false)
        assertEquals(1, result.records.size); assertEquals(1, access.visits)
    }

    @Test fun differentRootAliasesDeduplicateDocumentNotFilename() = runBlocking {
        val access = Access().apply { children["root"] = listOf(audio("one"), audio("two").copy(name = audio("one").name)) }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://treeA", "content://treeB"), false)
        assertEquals(2, result.records.size)
        assertTrue(result.records.all { it.media.sourcePath.startsWith("content://") })
    }

    @Test fun storeDenialDoesNotBlockGrantedFolder() = runBlocking {
        val access = Access().apply { storeDenied = true; children["root"] = listOf(audio()) }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://tree"))
        assertEquals(1, result.records.size)
        assertEquals(ScopedOrigin.MEDIA_STORE, result.issues.single().origin)
    }

    @Test fun documentBudgetIsGlobalIncludingNonMedia() = runBlocking {
        val access = Access().apply {
            children["root"] = listOf(folder("child"), audio(), audio("extra"))
            children["child"] = listOf(audio("later"))
        }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://tree"), false, maxDocuments = 2)
        assertEquals(2, result.inspected); assertEquals(1, access.visits)
        assertEquals(DiscoveryProblem.LIMIT_REACHED, result.issues.single().problem)
    }

    @Test fun folderBudgetIsGlobalAcrossRoots() = runBlocking {
        val access = Access().apply {
            children["root"] = listOf(folder("child"), audio())
            children["child"] = listOf(audio("child-song"))
        }
        val result = ScopedMediaDiscovery(access).scan(listOf("content://treeA", "content://treeB"), false,
            maxFolders = 2)
        assertEquals(2, access.visits)
        assertEquals(DiscoveryProblem.LIMIT_REACHED, result.issues.single().problem)
    }

    @Test fun hiddenUnsupportedAndNetworkLocationsAreNotIndexed() = runBlocking {
        val access = Access().apply { store = ScopedListing(listOf(
            audio().copy(name = ".hidden.mp3"), audio().copy(name = "readme.txt", mime = "text/plain"),
            audio(uri = "https://example.com/song.mp3"), audio("good"))) }
        assertEquals(1, ScopedMediaDiscovery(access).scan(emptyList()).records.size)
    }

    @Test fun malformedTreeDoesNotAbortOtherSources() = runBlocking {
        val access = Access().apply { store = ScopedListing(listOf(audio())) }
        val result = ScopedMediaDiscovery(access).scan(listOf("file:///root"))
        assertEquals(1, result.records.size)
        assertEquals(DiscoveryProblem.INVALID_ROOT, result.issues.single().problem)
    }

    @Test fun cancellationIsNotReportedAsProviderFailure() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val access = object : ScopedDiscoveryAccess {
            override suspend fun mediaStore(limit: Int): ScopedListing {
                entered.complete(Unit); awaitCancellation()
            }
            override fun treeRoot(tree: String) = "root"
            override fun hasReadGrant(tree: String) = true
            override suspend fun children(tree: String, documentId: String, limit: Int) = ScopedListing(emptyList())
        }
        var completed = false
        val job = launch { ScopedMediaDiscovery(access).scan(emptyList()); completed = true }
        entered.await(); job.cancelAndJoin()
        assertFalse(completed); assertTrue(job.isCancelled)
    }
}
