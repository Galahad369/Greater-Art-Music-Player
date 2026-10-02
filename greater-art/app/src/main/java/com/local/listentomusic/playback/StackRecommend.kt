package com.local.listentomusic.playback

import com.local.listentomusic.data.PlayHistoryEntry
import com.local.listentomusic.graph.FilenameSimilarity
import com.local.listentomusic.model.MediaFile
import java.util.Locale
import kotlin.math.abs

enum class StackRecommendationReason {
    FILENAME, SAME_FOLDER, SAME_ARTIST, SAME_ALBUM, SIMILAR_DURATION, PLAY_HISTORY,
}

data class StackRecommendation(
    val file: MediaFile,
    val score: Double,
    val reasons: List<StackRecommendationReason>,
)

/** Deterministic offline suggestions from local metadata and optional on-device play history. */
internal fun recommendStackTracks(
    seeds: List<MediaFile>,
    library: List<MediaFile>,
    history: List<PlayHistoryEntry> = emptyList(),
    limit: Int = 8,
): List<StackRecommendation> {
    if (seeds.isEmpty() || library.isEmpty() || limit <= 0) return emptyList()

    val seedPaths = seeds.map(MediaFile::path).toSet()
    val seedFeatures = seeds.map { FilenameSimilarity.features(it.name) }
    val seedFolders = seeds.map { it.sourcePath.substringBeforeLast('/', "") }.filter(String::isNotEmpty).toSet()
    val seedArtists = seeds.map { it.artist.trim().lowercase(Locale.ROOT) }.filter(String::isNotEmpty).toSet()
    val seedAlbums = seeds.map { it.album.trim().lowercase(Locale.ROOT) }.filter(String::isNotEmpty).toSet()
    val seedDurations = seeds.map(MediaFile::durationMs).filter { it > 0L }

    val historyBonusByPath = mutableMapOf<String, Double>()
    val recent = history.take(300)
    recent.forEachIndexed { index, entry ->
        if (entry.path !in seedPaths) return@forEachIndexed
        for (distance in 1..3) {
            val bonus = 0.24 / distance
            listOf(index - distance, index + distance).forEach { neighborIndex ->
                val neighbor = recent.getOrNull(neighborIndex)?.path ?: return@forEach
                if (neighbor !in seedPaths) {
                    historyBonusByPath[neighbor] = (historyBonusByPath[neighbor] ?: 0.0) + bonus
                }
            }
        }
    }

    return library.asSequence()
        .filter { it.path !in seedPaths }
        .map { file ->
            val reasons = linkedSetOf<StackRecommendationReason>()
            val filenameScore = seedFeatures.maxOf {
                FilenameSimilarity.score(it, FilenameSimilarity.features(file.name))
            }
            if (filenameScore >= 0.18) reasons += StackRecommendationReason.FILENAME

            val folder = file.sourcePath.substringBeforeLast('/', "")
            val folderBonus = if (folder.isNotEmpty() && folder in seedFolders) {
                reasons += StackRecommendationReason.SAME_FOLDER
                0.22
            } else 0.0

            val artist = file.artist.trim().lowercase(Locale.ROOT)
            val artistBonus = if (artist.isNotEmpty() && artist in seedArtists) {
                reasons += StackRecommendationReason.SAME_ARTIST
                0.18
            } else 0.0

            val album = file.album.trim().lowercase(Locale.ROOT)
            val albumBonus = if (album.isNotEmpty() && album in seedAlbums) {
                reasons += StackRecommendationReason.SAME_ALBUM
                0.16
            } else 0.0

            val durationBonus = if (file.durationMs > 0L && seedDurations.isNotEmpty()) {
                when (seedDurations.minOf { abs(it - file.durationMs) }) {
                    in 0L..15_000L -> {
                        reasons += StackRecommendationReason.SIMILAR_DURATION
                        0.12
                    }
                    in 15_001L..45_000L -> {
                        reasons += StackRecommendationReason.SIMILAR_DURATION
                        0.06
                    }
                    else -> 0.0
                }
            } else 0.0

            val historyBonus = (historyBonusByPath[file.path] ?: 0.0).coerceAtMost(0.30)
            if (historyBonus > 0.0) reasons += StackRecommendationReason.PLAY_HISTORY

            StackRecommendation(
                file,
                filenameScore + folderBonus + artistBonus + albumBonus + durationBonus + historyBonus,
                reasons.toList(),
            )
        }
        .filter { it.score >= 0.20 }
        .sortedWith(
            compareByDescending<StackRecommendation> { it.score }
                .thenBy { it.file.name.lowercase(Locale.ROOT) }
                .thenBy { it.file.path },
        )
        .distinctBy { it.file.path }
        .take(limit)
        .toList()
}
