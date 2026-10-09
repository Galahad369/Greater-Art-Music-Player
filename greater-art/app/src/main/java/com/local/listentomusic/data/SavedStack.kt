package com.local.listentomusic.data

import java.util.Base64

/** A simultaneous mix, deliberately independent of sequential Library playlists. */
data class SavedStackTrack(val path: String, val volume: Float = 1f, val muted: Boolean = false, val solo: Boolean = false,
    val offsetMs: Long = 0L, val alignmentScale: Double = 1.0, val alignmentOffsetUs: Double? = null)

data class SavedStack(
    val id: String,
    val name: String,
    val tracks: List<SavedStackTrack>,
    val primaryPath: String,
    val loopEnabled: Boolean = true,
    val keyword: String = "",
)

internal object SavedStackCodec {
    private fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decodeText(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    fun encode(stacks: List<SavedStack>): String = stacks.joinToString("\n") { stack ->
        listOf(encode(stack.id), encode(stack.name), encode(stack.primaryPath), stack.loopEnabled.toString(),
            encode(stack.keyword), stack.tracks.joinToString(",") {
                "${encode(it.path)}~${it.volume}~${it.muted}~${it.solo}~${it.offsetMs}" +
                    if (it.alignmentOffsetUs != null || it.alignmentScale != 1.0) "~${it.alignmentScale}~${it.alignmentOffsetUs ?: it.offsetMs * 1000.0}" else ""
            }).joinToString("|")
    }

    fun decode(raw: String): List<SavedStack> = raw.lineSequence().mapNotNull { line ->
        runCatching {
            val fields = line.split('|')
            require(fields.size == 6)
            val tracks = fields[5].split(',').map { track ->
                val parts = track.split('~')
                require(parts.size == 4 || parts.size == 5 || parts.size == 7)
                val volume = parts[1].toFloat().also { require(it.isFinite()) }.coerceIn(0f, 1f)
                SavedStackTrack(decodeText(parts[0]).also { require(it.isNotBlank()) }, volume,
                    parts[2].toBooleanStrict(), parts[3].toBooleanStrict(),
                    if (parts.size >= 5) parts[4].toLong().also { require(it in -30_000L..30_000L) } else 0L,
                    if (parts.size == 7) parts[5].toDouble().also { require(it.isFinite() && it in .97..1.03) } else 1.0,
                    if (parts.size == 7) parts[6].toDouble().also { require(it.isFinite() && it in -30_000_000.0..30_000_000.0) } else null)
            }.distinctBy { it.path }
            require(tracks.size in 1..8)
            val primary = decodeText(fields[2]).takeIf { path -> tracks.any { it.path == path } } ?: tracks.first().path
            SavedStack(decodeText(fields[0]).also { require(it.isNotBlank()) },
                decodeText(fields[1]).also { require(it.isNotBlank()) }, tracks, primary,
                fields[3].toBooleanStrict(), decodeText(fields[4]).take(80))
        }.getOrNull()
    }.distinctBy { it.id }.toList()
}
