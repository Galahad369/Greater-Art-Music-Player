package com.local.listentomusic.data

import java.util.Base64

/** A simultaneous mix, deliberately independent of sequential Library playlists. */
data class SavedStackTrack(val path: String, val volume: Float = 1f, val muted: Boolean = false, val solo: Boolean = false)

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
                "${encode(it.path)}~${it.volume}~${it.muted}~${it.solo}"
            }).joinToString("|")
    }

    fun decode(raw: String): List<SavedStack> = raw.lineSequence().mapNotNull { line ->
        runCatching {
            val fields = line.split('|')
            require(fields.size == 6)
            val tracks = fields[5].split(',').map { track ->
                val parts = track.split('~')
                require(parts.size == 4)
                val volume = parts[1].toFloat().also { require(it.isFinite()) }.coerceIn(0f, 1f)
                SavedStackTrack(decodeText(parts[0]).also { require(it.isNotBlank()) }, volume,
                    parts[2].toBooleanStrict(), parts[3].toBooleanStrict())
            }.distinctBy { it.path }
            require(tracks.size in 1..8)
            val primary = decodeText(fields[2]).takeIf { path -> tracks.any { it.path == path } } ?: tracks.first().path
            SavedStack(decodeText(fields[0]).also { require(it.isNotBlank()) },
                decodeText(fields[1]).also { require(it.isNotBlank()) }, tracks, primary,
                fields[3].toBooleanStrict(), decodeText(fields[4]).take(80))
        }.getOrNull()
    }.distinctBy { it.id }.toList()
}
