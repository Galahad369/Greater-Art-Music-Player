package com.local.listentomusic.data

import android.os.Environment
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object MediaScanner {
    internal val videoExtensions = setOf(
        "mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi",
    )
    internal val audioExtensions = setOf(
        "mp3", "aac", "m4a", "flac", "wav", "alac", "aif", "aiff", "opus", "ogg",
        "ape", "dsf", "dff", "amr", "ac3", "eac3", "mka",
    )
    internal val supportedExtensions = videoExtensions + audioExtensions

    /** A TypeScript source file is also named .ts. Inspect only three TS packet headers. */
    internal fun looksLikeTransportStream(file: File): Boolean = runCatching {
        val sample = ByteArray(616)
        var count = 0
        file.inputStream().use { input ->
            while (count < sample.size) {
                val n = input.read(sample, count, sample.size - count)
                if (n <= 0) break
                count += n
            }
        }
        listOf(188 to 0, 192 to 4, 204 to 0).any { (stride, offset) ->
            count > offset + stride * 2 &&
                sample[offset] == 0x47.toByte() &&
                sample[offset + stride] == 0x47.toByte() &&
                sample[offset + 2 * stride] == 0x47.toByte()
        }
    }.getOrDefault(false)

    internal fun isSupportedLocalMedia(file: File): Boolean {
        val extension = file.extension.lowercase(Locale.ROOT)
        return extension in supportedExtensions && (extension != "ts" || looksLikeTransportStream(file))
    }

    @Suppress("DEPRECATION")
    fun targetFolder(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun isInsideTarget(file: File): Boolean = runCatching {
        val root = targetFolder().canonicalFile.toPath()
        file.canonicalFile.toPath().startsWith(root)
    }.getOrDefault(false)

    /**
     * Recursively scans Download so media inside any subfolder is discovered without
     * hardcoding or exposing a particular source-app folder name.
     *
     * A Play-Store build should replace direct File access with the Storage Access Framework,
     * persist a tree URI using takePersistableUriPermission(), and scan DocumentFile children.
     */
    suspend fun scan(excludedFolders: Set<String> = emptySet()): ScanResult = withContext(Dispatchers.IO) {
        val folder = targetFolder()
        if (!folder.exists()) return@withContext ScanResult.FolderMissing(folder.absolutePath)
        if (!folder.isDirectory || !folder.canRead()) {
            return@withContext ScanResult.PermissionMissing(folder.absolutePath)
        }

        ScanResult.Success(scanFolder(folder, excludedFolders))
    }

    /**
     * Hidden files are never library items. Android can park trashed/in-progress media beside
     * originals as dot-prefixed files, and macOS can leave AppleDouble "._name" stubs.
     * Hidden directories are intentionally unchanged.
     */
    internal fun isHiddenEntry(file: File): Boolean = file.name.startsWith(".")

    /** Pure folder traversal kept separate so recursive discovery can be unit-tested. */
    internal fun scanFolder(folder: File, excludedFolders: Set<String> = emptySet()): List<MediaFile> {
        val excluded = excludedFolders.map { it.replace('\\', '/').trim('/').lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }.toSet()
        val sheets = mutableListOf<File>()
        val files = folder.walkTopDown()
            .onEnter { dir ->
                if (dir == folder) true else {
                    val relative = dir.relativeTo(folder).invariantSeparatorsPath.lowercase(Locale.ROOT)
                    excluded.none { relative == it || relative.startsWith("$it/") }
                }
            }
            .onFail { _, _ -> /* Ignore unreadable children and keep the rest of the library. */ }
            .onEach { if (it.isFile && !isHiddenEntry(it) && it.extension.equals("cue", true)) sheets += it }
            .filter { it.isFile && !isHiddenEntry(it) && isSupportedLocalMedia(it) }
            // Keep the launch scan fast: do not open or decode every file here.
            // Thumbnails and expensive metadata are loaded from a bounded background cache.
            .map { file ->
                MediaFile(
                    path = file.absolutePath,
                    name = file.name,
                    durationMs = 0L,
                    sizeBytes = file.length(),
                    modifiedMs = file.lastModified(),
                    kind = if (file.extension.lowercase(Locale.ROOT) in videoExtensions) {
                        MediaKind.VIDEO
                    } else {
                        MediaKind.AUDIO
                    },
                )
            }
            .toList()
        return com.local.listentomusic.model.expandCueSheets(files, sheets)
    }
}

sealed interface ScanResult {
    data class Success(val files: List<MediaFile>) : ScanResult
    data class FolderMissing(val path: String) : ScanResult
    data class PermissionMissing(val path: String) : ScanResult
}
