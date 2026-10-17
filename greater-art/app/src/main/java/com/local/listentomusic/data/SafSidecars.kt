package com.local.listentomusic.data

import java.util.Locale

/**
 * Sidecar files (lyrics, cover art) next to a media file, found from a FOLDER LISTING.
 *
 * With raw paths the app probes candidate files one by one (File.isFile). A SAF tree cannot be probed by path, only
 * listed, so the same rules are applied to the names in the listing instead. Names compare case-insensitively, as they
 * do on Android's case-insensitive emulated storage.
 */
internal object SidecarNames {
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")
    val FOLDER_ART_NAMES = setOf("cover", "folder", "front", "album", "artwork")
    val LYRICS_PRIORITY = listOf("ttml", "lrc", "srt")
}

private fun stemOf(name: String) = name.substringBeforeLast(".")
private fun extensionOf(name: String) = name.substringAfterLast('.', "")
private fun lower(value: String) = value.lowercase(Locale.ROOT)

/**
 * One folder's names, indexed once so each media file costs a handful of hash lookups instead of a scan of the folder
 * (a folder of thousands of files would otherwise cost millions of comparisons).
 *
 * Lyrics follow findMatchingLrc(File): prefer ttml, then lrc, then srt with the media file's own stem; otherwise a
 * lyrics file whose stem is the media's full file name. Artwork follows the thumbnail repository's candidate order:
 * "<media stem>.<image ext>" first, then cover / folder / front / album / artwork in each image extension.
 * When several names match, the first in listing order wins.
 */
internal class SidecarIndex(siblingNames: List<String>) {
    private val byLowerName = HashMap<String, String>()
    private val lyricsByLowerStem = HashMap<String, LinkedHashMap<String, String>>()

    init {
        val lyricsExtensions = SidecarNames.LYRICS_PRIORITY.toSet()
        for (name in siblingNames) {
            byLowerName.putIfAbsent(lower(name), name)
            val extension = lower(extensionOf(name))
            if (extension in lyricsExtensions) lyricsByLowerStem.getOrPut(lower(stemOf(name))) { LinkedHashMap() }
                .putIfAbsent(extension, name)
        }
    }

    fun lyricsFor(mediaName: String): String? {
        val sameStem = lyricsByLowerStem[lower(stemOf(mediaName))].orEmpty()
        for (extension in SidecarNames.LYRICS_PRIORITY) {
            sameStem[extension]?.let { return it }
        }
        return lyricsByLowerStem[lower(mediaName)]?.values?.firstOrNull()
    }

    fun artworkFor(mediaName: String): String? {
        val stem = lower(stemOf(mediaName))
        for (extension in SidecarNames.IMAGE_EXTENSIONS) byLowerName["$stem.$extension"]?.let { return it }
        for (name in SidecarNames.FOLDER_ART_NAMES) {
            for (extension in SidecarNames.IMAGE_EXTENSIONS) byLowerName["$name.$extension"]?.let { return it }
        }
        return null
    }

    /** Resolves a sidecar's name back to whatever the caller keyed it by (a URI, a document id). */
    fun <T> resolve(name: String?, valuesByName: Map<String, T>): T? = name?.let { valuesByName[lower(it)] }
}

internal fun matchLyricsSidecar(mediaName: String, siblingNames: List<String>): String? = SidecarIndex(siblingNames).lyricsFor(mediaName)

internal fun matchArtworkSidecar(mediaName: String, siblingNames: List<String>): String? = SidecarIndex(siblingNames).artworkFor(mediaName)
