package com.local.listentomusic.playback

import com.local.listentomusic.graph.FilenameSimilarity
import com.local.listentomusic.model.MediaFile
import kotlin.math.abs

/**
 * Offline Stack suggestions from local library metadata only.
 * Ranks by filename similarity, shared parent folder, artist match, and duration proximity.
 */
internal fun recommendStackTracks(
    seeds: List<MediaFile>,
    library: List<MediaFile>,
    limit: Int = 6,
): List<MediaFile> {
    if (seeds.isEmpty() || library.isEmpty() || limit <= 0) return emptyList()
    val seedPaths = seeds.map { it.path }.toSet()
    val seedFeatures = seeds.map { FilenameSimilarity.features(it.name) }
    val seedFolders = seeds.map { it.path.substringBeforeLast('/', "") }.toSet()
    val seedArtists = seeds.map { it.artist.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val seedDurations = seeds.map { it.durationMs }.filter { it > 0L }

    return library
        .asSequence()
        .filter { it.path !in seedPaths }
        .map { file ->
            val folder = file.path.substringBeforeLast('/', "")
            val nameScore = seedFeatures.maxOf { FilenameSimilarity.score(it, FilenameSimilarity.features(file.name)) }
            val folderBonus = if (folder.isNotEmpty() && folder in seedFolders) 0.22 else 0.0
            val artistBonus = if (file.artist.trim().lowercase() in seedArtists) 0.18 else 0.0
            val durationBonus = if (file.durationMs > 0L && seedDurations.isNotEmpty()) {
                val nearest = seedDurations.minOf { abs(it - file.durationMs) }
                when {
                    nearest <= 15_000L -> 0.12
                    nearest <= 45_000L -> 0.06
                    else -> 0.0
                }
            } else 0.0
            file to (nameScore + folderBonus + artistBonus + durationBonus)
        }
        .filter { it.second >= 0.28 }
        .sortedByDescending { it.second }
        .map { it.first }
        .distinctBy { it.path }
        .take(limit)
        .toList()
}
