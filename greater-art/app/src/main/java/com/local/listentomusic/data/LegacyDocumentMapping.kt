package com.local.listentomusic.data

import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/*
 * Legacy raw-path -> SAF document mapping (stage C remainder in docs/STORAGE_MIGRATION.md). Pure Kotlin.
 *
 * ScopedMediaDiscovery identifies a SAF document as DocumentsContract.buildDocumentUri(authority, documentId), i.e.
 * content://<authority>/document/<encoded id>, independent of which folder grant reached it. Everything that was stored
 * while the Library was path-based (playlists, history, Stack slots, custom order) holds raw paths. For documents on
 * ExternalStorageProvider the two are linked by a fixed rule ("/storage/emulated/0/Music/a.mp3" is document
 * "primary:Music/a.mp3"), so a stored path can be re-linked to the document that now represents it without any file access.
 *
 * Nothing here rewrites stored data: it answers "which scoped item is this legacy id?" and leaves the decision to the caller.
 */

internal const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val CUE_SUFFIX = "#ga-cue="

/** One storage volume as ExternalStorageProvider names it: document ids look like "<volumeId>:<path inside the volume>". */
internal data class StorageVolumeRoot(val volumeId: String, val rootPaths: List<String>)

private const val UNRESERVED = "_-!.~'()*"

/** Same character set android.net.Uri.encode leaves alone; everything else becomes UTF-8 %XX (upper-case hex). */
internal fun encodeUriComponent(value: String): String {
    val out = StringBuilder(value.length + 8)
    for (byte in value.toByteArray(StandardCharsets.UTF_8)) {
        val c = (byte.toInt() and 0xFF)
        val ch = c.toChar()
        if (c < 0x80 && (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch in UNRESERVED)) out.append(ch)
        else out.append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 0x0F])
    }
    return out.toString()
}

/** Inverse of [encodeUriComponent]; null for a malformed escape or invalid UTF-8. */
internal fun decodeUriComponent(value: String): String? {
    val bytes = ByteArrayOutputStream(value.length)
    var i = 0
    while (i < value.length) {
        val ch = value[i]
        if (ch == '%') {
            if (i + 2 >= value.length) return null
            val hi = "0123456789abcdef".indexOf(value[i + 1].lowercaseChar())
            val lo = "0123456789abcdef".indexOf(value[i + 2].lowercaseChar())
            if (hi < 0 || lo < 0) return null
            bytes.write(hi * 16 + lo)
            i += 3
        } else {
            // Keep literal supplementary characters intact instead of encoding
            // each surrogate as a replacement '?' and aliasing different names.
            val count = if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= value.length || !Character.isLowSurrogate(value[i + 1])) return null
                2
            } else {
                if (Character.isLowSurrogate(ch)) return null
                1
            }
            val encoded = value.substring(i, i + count).toByteArray(StandardCharsets.UTF_8)
            bytes.write(encoded, 0, encoded.size)
            i += count
        }
    }
    return try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

internal fun stableDocumentKey(authority: String, documentId: String): String =
    "content://$authority/document/${encodeUriComponent(documentId)}"

internal data class DocumentRef(val authority: String, val documentId: String)

/** Parses a [stableDocumentKey] (with or without a cue suffix); null for anything else, including tree-qualified URIs. */
internal fun parseStableDocumentKey(key: String): DocumentRef? {
    val base = key.substringBefore(CUE_SUFFIX)
    if (base != key && com.local.listentomusic.model.cueBounds(key) == null) return null
    val uri = runCatching { URI(base) }.getOrNull() ?: return null
    if (uri.rawQuery != null || uri.rawFragment != null || uri.rawUserInfo != null || uri.port != -1) return null
    val prefix = "content://"
    if (!base.startsWith(prefix)) return null
    val rest = base.substring(prefix.length)
    val slash = rest.indexOf('/')
    if (slash <= 0) return null
    val authority = rest.substring(0, slash)
    if (authority.any { !(it.isLetterOrDigit() || it in "._-") }) return null
    val tail = rest.substring(slash)
    val marker = "/document/"
    if (!tail.startsWith(marker)) return null
    val encodedId = tail.substring(marker.length)
    if ('/' in encodedId) return null
    val id = decodeUriComponent(encodedId) ?: return null
    return if (id.isEmpty()) null else DocumentRef(authority, id)
}

// ---- ExternalStorageProvider <-> legacy file paths -------------------------------------------------------------------

private fun normalizePath(path: String): String? {
    if (!path.startsWith("/") || path.any { it.code < 32 || it.code == 127 } ||
        !StandardCharsets.UTF_8.newEncoder().canEncode(path)) return null
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.any { it == ".." || it == "." }) return null
    return "/" + parts.joinToString("/")
}

/**
 * "/storage/emulated/0/Music/a.mp3" -> "primary:Music/a.mp3". The longest matching volume root wins, so a nested
 * mount point beats its parent. Null when the path is not under a known volume or tries to climb with "..".
 */
internal fun documentIdForLegacyPath(path: String, volumes: List<StorageVolumeRoot>): String? {
    val normalized = normalizePath(path) ?: return null
    var best: Pair<StorageVolumeRoot, String>? = null
    for (volume in volumes) for (rootRaw in volume.rootPaths) {
        val root = normalizePath(rootRaw) ?: continue
        if ((normalized == root || normalized.startsWith("$root/")) && (best == null || root.length > best.second.length)) best = volume to root
    }
    val (volume, root) = best ?: return null
    return "${volume.volumeId}:${normalized.substring(root.length).trimStart('/')}"
}

/** "primary:Music/a.mp3" -> "/storage/emulated/0/Music/a.mp3" using the volume's first (canonical) root. */
internal fun legacyPathForDocumentId(documentId: String, volumes: List<StorageVolumeRoot>): String? {
    val colon = documentId.indexOf(':')
    if (colon <= 0) return null
    val volume = volumes.firstOrNull { it.volumeId == documentId.substring(0, colon) } ?: return null
    val relative = documentId.substring(colon + 1)
    if (relative.split('/').any { it == ".." || it == "." }) return null
    val root = normalizePath(volume.rootPaths.firstOrNull() ?: return null) ?: return null
    return if (relative.isEmpty()) root else "$root/${relative.trim('/')}"
}

/**
 * Turns a stored legacy media key (playlist, history, Stack slot, cache key) into the stable SAF key, keeping a cue
 * suffix. Null when the key is not a path on a known ExternalStorageProvider volume.
 */
internal fun migrateLegacyMediaKey(legacyKey: String, volumes: List<StorageVolumeRoot>): String? {
    val suffix = if (legacyKey.contains(CUE_SUFFIX)) CUE_SUFFIX + legacyKey.substringAfter(CUE_SUFFIX) else ""
    val id = documentIdForLegacyPath(legacyKey.substringBefore(CUE_SUFFIX), volumes) ?: return null
    return stableDocumentKey(EXTERNAL_STORAGE_AUTHORITY, id) + suffix
}

/** Inverse of [migrateLegacyMediaKey] for ExternalStorageProvider keys, e.g. to show a readable path. */
internal fun legacyPathForStableKey(key: String, volumes: List<StorageVolumeRoot>): String? {
    val ref = parseStableDocumentKey(key) ?: return null
    if (ref.authority != EXTERNAL_STORAGE_AUTHORITY) return null
    val suffix = if (key.contains(CUE_SUFFIX)) CUE_SUFFIX + key.substringAfter(CUE_SUFFIX) else ""
    return (legacyPathForDocumentId(ref.documentId, volumes) ?: return null) + suffix
}

/**
 * Path of [documentId] inside [treeDocumentId] ("" for the tree root), or null if it is outside the tree.
 * Only valid for providers whose ids are hierarchical (ExternalStorageProvider); the library planner does not use it
 * and walks parent links instead.
 */
internal fun externalStorageRelativePath(documentId: String, treeDocumentId: String): String? {
    val relative = when {
    documentId == treeDocumentId -> ""
    treeDocumentId.endsWith(":") && documentId.startsWith(treeDocumentId) -> documentId.substring(treeDocumentId.length).trim('/')
    documentId.startsWith("$treeDocumentId/") -> documentId.substring(treeDocumentId.length + 1).trim('/')
    else -> null
    }
    return relative?.takeUnless { path -> path.split('/').any { it == "." || it == ".." } }
}
