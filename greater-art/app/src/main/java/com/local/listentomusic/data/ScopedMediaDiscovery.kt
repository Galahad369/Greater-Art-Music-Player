package com.local.listentomusic.data

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.MediaSource
import java.util.ArrayDeque
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class ScopedOrigin { MEDIA_STORE, SAF }
internal enum class DiscoveryProblem { PERMISSION_DENIED, PROVIDER_UNAVAILABLE, INVALID_ROOT, LIMIT_REACHED }
internal data class DiscoveryIssue(val origin: ScopedOrigin, val problem: DiscoveryProblem)
/** Sidecar documents found in the SAME folder listing as the media (MediaStore items have none: it cannot see .lrc or cover files). */
internal data class ScopedSidecars(val lyricsUri: String? = null, val artworkUri: String? = null)
internal data class ScopedMediaRecord(val media: MediaFile, val origin: ScopedOrigin, val sidecars: ScopedSidecars = ScopedSidecars())
internal data class ScopedDiscoveryResult(
    val records: List<ScopedMediaRecord>, val issues: List<DiscoveryIssue>, val inspected: Int,
)
internal data class ScopedRow(
    val documentId: String, val stableId: String, val uri: String, val name: String,
    val mime: String?, val directory: Boolean = false, val size: Long = 0,
    val modifiedMs: Long = 0, val durationMs: Long = 0,
)
internal data class ScopedListing(val rows: List<ScopedRow>, val truncated: Boolean = false,
    val inspected: Int = rows.size, val problems: Set<DiscoveryProblem> = emptySet())

/** Provider boundary is injectable so denial, cycles and cancellation are behavioral tests. */
internal interface ScopedDiscoveryAccess {
    suspend fun mediaStore(limit: Int): ScopedListing
    fun treeRoot(tree: String): String
    fun hasReadGrant(tree: String): Boolean
    suspend fun children(tree: String, documentId: String, limit: Int): ScopedListing
}

internal fun scopedMediaKind(name: String, mime: String?): MediaKind? {
    if (name.isBlank() || name.startsWith('.')) return null
    val type = mime.orEmpty().lowercase(Locale.ROOT)
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    // SAF does not expose raw packet headers here. Require an explicit video MIME for .ts.
    if (extension == "ts" && !type.startsWith("video/")) return null
    return when {
        type.startsWith("video/") || extension in MediaScanner.videoExtensions -> MediaKind.VIDEO
        type.startsWith("audio/") || extension in MediaScanner.audioExtensions -> MediaKind.AUDIO
        else -> null
    }
}

/** Fresh snapshots only. Never merges failed/revoked listings into an old cached result. */
internal class ScopedMediaDiscovery(private val access: ScopedDiscoveryAccess) {
    suspend fun scan(trees: List<String>, includeMediaStore: Boolean = true,
        maxDocuments: Int = 10_000, maxFolders: Int = 512, maxDepth: Int = 24,
        excludedFolders: Set<String> = emptySet()): ScopedDiscoveryResult = withContext(Dispatchers.Default) {
        require(maxDocuments in 1..100_000 && maxFolders in 1..2000 && maxDepth in 1..64)
        // Same rule as MediaScanner.scanFolder: paths relative to the granted root, slash-normalised, a folder is excluded
        // with its whole subtree when `relative == it || relative.startsWith("$it/")`.
        val excluded = excludedFolders.map { it.replace('\\', '/').trim('/').lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }.toSet()
        val records = linkedMapOf<String, ScopedMediaRecord>()
        val issues = mutableListOf<DiscoveryIssue>()
        val completedRoots = mutableListOf<Pair<String, List<ScopedMediaRecord>>>()
        var inspected = 0
        var queriedFolders = 0
        fun add(row: ScopedRow, origin: ScopedOrigin, target: MutableMap<String, ScopedMediaRecord>,
            sidecars: ScopedSidecars = ScopedSidecars(), relativePath: String? = null) {
            val kind = scopedMediaKind(row.name, row.mime) ?: return
            if (row.directory || MediaSource.parse(row.uri) !is MediaSource.ContentUri ||
                MediaSource.parse(row.stableId) !is MediaSource.ContentUri) return
            val file = MediaFile(row.stableId, row.name, row.durationMs.coerceAtLeast(0),
                row.size.coerceAtLeast(0), row.modifiedMs.coerceAtLeast(0), kind, sourcePath = row.uri,
                relativePath = relativePath)
            target.putIfAbsent(row.stableId, ScopedMediaRecord(file, origin, sidecars))
        }
        suspend fun guarded(origin: ScopedOrigin, block: suspend () -> Unit) {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { issues += DiscoveryIssue(origin, DiscoveryProblem.PERMISSION_DENIED) }
            catch (_: IllegalArgumentException) { issues += DiscoveryIssue(origin, DiscoveryProblem.INVALID_ROOT) }
            catch (_: Exception) { issues += DiscoveryIssue(origin, DiscoveryProblem.PROVIDER_UNAVAILABLE) }
        }
        if (includeMediaStore) guarded(ScopedOrigin.MEDIA_STORE) {
            currentCoroutineContext().ensureActive()
            val listing = access.mediaStore(maxDocuments)
            for (row in listing.rows.take(maxDocuments)) {
                currentCoroutineContext().ensureActive(); add(row, ScopedOrigin.MEDIA_STORE, records)
            }
            inspected += listing.inspected.coerceIn(0, maxDocuments)
            issues += listing.problems.map { DiscoveryIssue(ScopedOrigin.MEDIA_STORE, it) }
            if (listing.truncated || listing.rows.size > maxDocuments)
                issues += DiscoveryIssue(ScopedOrigin.MEDIA_STORE, DiscoveryProblem.LIMIT_REACHED)
        }
        for (tree in trees.distinct().take(maxFolders)) guarded(ScopedOrigin.SAF) {
            currentCoroutineContext().ensureActive()
            val root = access.treeRoot(tree)
            if (!access.hasReadGrant(tree)) throw SecurityException()
            // Commit a root only after its final grant check. Mid-scan revocation discards it.
            val rootRecords = linkedMapOf<String, ScopedMediaRecord>()
            val pending = ArrayDeque<Triple<String, Int, String>>()
            val seen = hashSetOf<String>()
            pending.add(Triple(root, 0, ""))
            var limited = false
            while (pending.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val (id, depth, relative) = pending.removeFirst()
                if (!seen.add(id)) continue
                if (!access.hasReadGrant(tree)) throw SecurityException()
                val remaining = maxDocuments - inspected
                if (remaining <= 0 || queriedFolders >= maxFolders) { limited = true; break }
                queriedFolders++
                val listing = access.children(tree, id, remaining)
                limited = limited || listing.truncated || listing.rows.size > remaining
                val rows = listing.rows.take(remaining)
                // Sidecars come from the names in this folder's own listing (SAF cannot probe paths). Truncated listings
                // can only find sidecars among the rows that were returned.
                val files = rows.filter { !it.directory && MediaSource.parse(it.uri) is MediaSource.ContentUri }
                val sidecarIndex = SidecarIndex(files.map { it.name })
                val uriByLowerName = HashMap<String, String>()
                for (file in files) uriByLowerName.putIfAbsent(file.name.lowercase(Locale.ROOT), file.uri)
                for (row in rows) {
                    currentCoroutineContext().ensureActive()
                    if (row.directory) {
                        val childRelative = if (relative.isEmpty()) row.name else "$relative/${row.name}"
                        val relativeKey = childRelative.lowercase(Locale.ROOT)
                        if (excluded.any { relativeKey == it || relativeKey.startsWith("$it/") }) continue
                        if (depth < maxDepth && queriedFolders + pending.size < maxFolders)
                            pending.add(Triple(row.documentId, depth + 1, childRelative))
                        else limited = true
                    } else {
                        val sidecars = if (scopedMediaKind(row.name, row.mime) == null) ScopedSidecars() else ScopedSidecars(
                            lyricsUri = sidecarIndex.resolve(sidecarIndex.lyricsFor(row.name), uriByLowerName),
                            artworkUri = sidecarIndex.resolve(sidecarIndex.artworkFor(row.name), uriByLowerName),
                        )
                        add(row, ScopedOrigin.SAF, rootRecords, sidecars,
                            if (relative.isEmpty()) row.name else "$relative/${row.name}")
                    }
                }
                inspected += listing.inspected.coerceIn(0, remaining)
            }
            if (!access.hasReadGrant(tree)) throw SecurityException()
            completedRoots += tree to rootRecords.values.toList()
            if (limited) issues += DiscoveryIssue(ScopedOrigin.SAF, DiscoveryProblem.LIMIT_REACHED)
        }
        if (trees.distinct().size > maxFolders) issues += DiscoveryIssue(ScopedOrigin.SAF, DiscoveryProblem.LIMIT_REACHED)
        // A later root can take time: recheck earlier grants before publishing the snapshot.
        for ((tree, entries) in completedRoots) guarded(ScopedOrigin.SAF) {
            currentCoroutineContext().ensureActive()
            if (!access.hasReadGrant(tree)) throw SecurityException()
            entries.forEach { records.putIfAbsent(it.media.id, it) }
        }
        ScopedDiscoveryResult(records.values.toList(), issues.distinct(), inspected)
    }
}
