package com.local.listentomusic.model

import java.io.File
import java.util.Locale

/** Standard paths remain usable by other players; VLC options preserve optional CUE clipping. */
internal fun exportM3u(files: List<MediaFile>): String = buildString {
    appendLine("#EXTM3U")
    files.forEach { file ->
        appendLine("#EXTINF:${if (file.durationMs > 0) file.durationMs / 1000 else -1},${file.name.replace('\n', ' ').replace('\r', ' ')}")
        if (file.clipStartMs > 0 || file.clipEndMs != null) {
            appendLine("#EXTVLCOPT:start-time=${file.clipStartMs / 1000.0}")
            file.clipEndMs?.let { appendLine("#EXTVLCOPT:stop-time=${it / 1000.0}") }
        }
        appendLine(file.sourcePath)
    }
}

/** Portable share form: paths inside Download are relative instead of private absolute paths. */
internal fun exportPortableM3u(files: List<MediaFile>, root: File): String {
    val canonicalRoot = root.canonicalFile
    return buildString {
        appendLine("#EXTM3U")
        files.forEach { file ->
            appendLine("#EXTINF:${if (file.durationMs > 0) file.durationMs / 1000 else -1},${file.name.replace('\n', ' ').replace('\r', ' ')}")
            if (file.clipStartMs > 0 || file.clipEndMs != null) {
                appendLine("#EXTVLCOPT:start-time=${file.clipStartMs / 1000.0}")
                file.clipEndMs?.let { appendLine("#EXTVLCOPT:stop-time=${it / 1000.0}") }
            }
            if (MediaSource.parse(file.sourcePath) is MediaSource.ContentUri) {
                appendLine(file.relativePath ?: file.name)
                return@forEach
            }
            val source = File(file.sourcePath).canonicalFile
            val portable = source.relativeToOrNull(canonicalRoot)?.invariantSeparatorsPath
                ?.takeIf { it.isNotBlank() && !it.startsWith("../") }
                ?: source.name
            appendLine(portable)
        }
    }
}

internal data class M3uEntry(val path: String, val start: Long? = null, val end: Long? = null)

/** Build once per import; retain legacy first-match and ambiguous-basename rules. */
internal class M3uLibraryIndex(files: List<MediaFile>) {
    private val byReference = HashMap<String, MediaFile>().also { index ->
        files.forEach { file -> index.putIfAbsent(file.id, file); index.putIfAbsent(file.sourcePath, file) }
    }
    private val byClip = files.groupBy { it.clipStartMs to it.clipEndMs }
    private val byRelative = files.filter { it.relativePath != null }
        .groupBy { it.relativePath.orEmpty().replace('\\', '/') }
    private val byCanonical = files.filterNot { MediaSource.parse(it.sourcePath) is MediaSource.ContentUri }
        .groupBy { canonical(File(it.sourcePath)) }
    private val byName = files.groupBy { it.name.lowercase(Locale.ROOT) }

    private fun canonical(path: File): String? = runCatching { path.canonicalPath }.getOrNull()

    fun content(reference: String): MediaFile? = byReference[reference]

    fun clipped(source: String, start: Long, end: Long?): MediaFile? {
        val candidates = byClip[start to end].orEmpty()
        return candidates.firstOrNull { it.sourcePath == source }
            ?: candidates.filter { File(it.sourcePath).name == File(source).name }.singleOrNull()
    }

    fun resolve(entry: M3uEntry, root: File, decode: (String) -> String): MediaFile? {
        val raw = entry.path
        if (raw.startsWith("content://")) return content(raw)
        if ("://" in raw && !raw.startsWith("file://")) return null
        val path = raw.removePrefix("file://")
        val alternatives = listOf(path, decode(path)).distinct()
        fun matching(items: List<MediaFile>?) = items.orEmpty().singleOrNull {
            if (entry.start == null) it.clipStartMs == 0L && it.clipEndMs == null
            else it.clipStartMs == entry.start && it.clipEndMs == entry.end
        }
        for (value in alternatives) {
            val relative = value.replace('\\', '/').removePrefix("./")
            matching(byRelative[relative])?.let { return it }
            val file = File(value)
            val resolved = canonical(if (file.isAbsolute) file else File(root, value))
            if (resolved != null) matching(byCanonical[resolved])?.let { return it }
        }
        for (value in alternatives) {
            if (entry.start != null) clipped(value, entry.start, entry.end)?.let { return it }
            else matching(byName[File(value).name.lowercase(Locale.ROOT)])?.let { return it }
        }
        return null
    }
}

internal fun parseM3u(lines: List<String>): List<M3uEntry> {
    var start: Long? = null; var end: Long? = null
    val result = mutableListOf<M3uEntry>()
    lines.take(100_000).forEach { raw ->
        val line = raw.trim().removePrefix("\uFEFF")
        fun milliseconds(): Long? = line.substringAfter('=').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.times(1000)?.toLong()
        when {
            line.startsWith("#EXTVLCOPT:start-time=") -> start = milliseconds()
            line.startsWith("#EXTVLCOPT:stop-time=") -> end = milliseconds()
            line.isNotEmpty() && !line.startsWith('#') -> { result += M3uEntry(line, start, end); start = null; end = null }
        }
    }
    return result
}
