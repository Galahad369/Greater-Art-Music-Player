package com.local.listentomusic.model

/** Preserve session order and every entry, including duplicates and not-yet-scanned files. */
internal fun <T> reconcileSessionQueue(ids: List<String>, known: Map<String, T>, fallback: (Int) -> T): List<T> =
    ids.mapIndexed { index, id -> known[id] ?: fallback(index) }


/** Queue rows need identity that is unique even when one media ID appears repeatedly. */
internal data class QueueEntry(
    val index: Int,
    val file: MediaFile,
    val stableKey: String,
)

internal fun queueEntries(files: List<MediaFile>): List<QueueEntry> {
    val occurrences = HashMap<String, Int>()
    return files.mapIndexed { index, file ->
        val occurrence = occurrences[file.id] ?: 0
        occurrences[file.id] = occurrence + 1
        QueueEntry(index, file, "${file.id}\u0000$occurrence")
    }
}
