package com.local.listentomusic.data

import com.local.listentomusic.model.LegacyMediaMigration
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaSource
import com.local.listentomusic.model.cueBounds
import com.local.listentomusic.model.migrateLegacyMediaId
import java.util.Locale

internal enum class RelinkFailure {
    /** Not an absolute path, or not on a storage volume the app knows (so it cannot map to a document). */
    NOT_A_KNOWN_VOLUME_PATH,

    /** Maps to a document, but that document is not in the scoped library right now (other folder, deleted, no grant). */
    NOT_IN_SCOPED_LIBRARY,

    /** A cue segment: scoped discovery lists the audio file, not its tracks, so there is nothing to relink it to yet. */
    CUE_TRACK_NOT_EXPANDED,

    UNSUPPORTED_ID,
}

internal data class LegacyRelinkResult(
    val relinked: Map<String, MediaFile>,
    val unresolved: Map<String, RelinkFailure>,
)

/**
 * Answers "which scoped item is this stored legacy id?" for playlists, history, Stack slots and custom order, using the
 * fixed ExternalStorageProvider rule path <-> document id. It never opens a file, never rewrites anything, and never
 * guesses: an id that cannot be matched is reported with the reason and left untouched.
 *
 * Matching compares (authority, document id) parsed from both sides, so it does not depend on how a provider happened to
 * percent-encode the id. If no document matches exactly, a case-insensitive match is accepted only when it is unique
 * (Android's emulated storage is case-insensitive; two different documents differing only by case are never merged).
 */
internal fun relinkLegacyMediaIds(
    legacyIds: Collection<String>,
    scoped: Collection<MediaFile>,
    volumes: List<StorageVolumeRoot>,
): LegacyRelinkResult {
    val exact = HashMap<DocumentRef, MediaFile>()
    val caseFolded = HashMap<Pair<String, String>, LinkedHashMap<DocumentRef, MediaFile>>()
    val byId = HashMap<String, MediaFile>()
    for (file in scoped) {
        byId.putIfAbsent(file.id, file)
        // A cue segment is not a candidate for a whole-file legacy reference.
        if (cueBounds(file.id) != null) continue
        val ref = parseStableDocumentKey(file.id) ?: continue
        exact.putIfAbsent(ref, file)
        caseFolded.getOrPut(ref.authority to ref.documentId.lowercase(Locale.ROOT)) { LinkedHashMap() }
            .putIfAbsent(ref, file)
    }
    val relinked = LinkedHashMap<String, MediaFile>()
    val unresolved = LinkedHashMap<String, RelinkFailure>()
    for (id in legacyIds.distinct()) {
        if (cueBounds(id) != null) {
            val key = migrateLegacyMediaKey(id, volumes)
            val match = byId[id] ?: key?.let { byId[it] }
            if (match != null) relinked[id] = match else unresolved[id] = RelinkFailure.CUE_TRACK_NOT_EXPANDED
            continue
        }
        val migration = migrateLegacyMediaId(id)
        if (migration is LegacyMediaMigration.Unsupported) { unresolved[id] = RelinkFailure.UNSUPPORTED_ID; continue }
        val source = (migration as LegacyMediaMigration.Migrated).reference.source
        if (source is MediaSource.ContentUri) {
            // Already a scoped identity (stored while a folder was selected).
            val match = byId[id] ?: parseStableDocumentKey(id)?.let { exact[it] }
            if (match != null) relinked[id] = match else unresolved[id] = RelinkFailure.NOT_IN_SCOPED_LIBRARY
            continue
        }
        val documentId = documentIdForLegacyPath(source.value, volumes)
        if (documentId == null) { unresolved[id] = RelinkFailure.NOT_A_KNOWN_VOLUME_PATH; continue }
        val ref = DocumentRef(EXTERNAL_STORAGE_AUTHORITY, documentId)
        val match = exact[ref]
            ?: caseFolded[EXTERNAL_STORAGE_AUTHORITY to documentId.lowercase(Locale.ROOT)]?.values?.singleOrNull()
        if (match != null) relinked[id] = match else unresolved[id] = RelinkFailure.NOT_IN_SCOPED_LIBRARY
    }
    return LegacyRelinkResult(relinked, unresolved)
}
