package com.local.listentomusic.model

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.io.RandomAccessFile

data class LyricWord(val timeMs: Long, val text: String)
data class LyricLine(val timeMs: Long, val text: String, val words: List<LyricWord> = emptyList())

data class LocalLyrics(val sourcePath: String, val lines: List<LyricLine>)

private val timestampPattern = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
private val offsetPattern = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

internal fun parseLrc(content: String): List<LyricLine> {
    val offsetMs = offsetPattern.find(content)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    return content.lineSequence().flatMap { rawLine ->
        val matches = timestampPattern.findAll(rawLine).toList()
        if (matches.isEmpty()) return@flatMap emptySequence()
        val body = rawLine.substring(matches.last().range.last + 1).trim()
        val stamps = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>").findAll(body).toList()
        val words = stamps.mapIndexedNotNull { index, word ->
            val seconds = word.groupValues[2].toLong()
            if (seconds > 59) return@mapIndexedNotNull null
            val label = body.substring(word.range.last + 1, stamps.getOrNull(index + 1)?.range?.first ?: body.length)
            if (label.isEmpty()) null else LyricWord(word.groupValues[1].toLong() * 60_000 + seconds * 1000 +
                word.groupValues[3].padEnd(3, '0').toLong() + offsetMs, label)
        }
        val text = body.replace(Regex("<[^>]*>"), "")
        if (text.isEmpty()) return@flatMap emptySequence()
        matches.asSequence().mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            if (seconds > 59L) return@mapNotNull null
            val fraction = match.groupValues[3]
            val fractionMs = when (fraction.length) {
                1 -> fraction.toLongOrNull()?.times(100L)
                2 -> fraction.toLongOrNull()?.times(10L)
                3 -> fraction.toLongOrNull()
                else -> 0L
            } ?: 0L
            LyricLine(
                (minutes * 60_000L + seconds * 1_000L + fractionMs + offsetMs).coerceAtLeast(0L),
                text,
                words,
            )
        }
    }.sortedBy(LyricLine::timeMs).toList()
}

internal fun findMatchingLrc(mediaPath: String): File? {
    if (MediaSource.parse(mediaPath) is MediaSource.ContentUri) return null
    val media = File(mediaPath)
    val parent = media.parentFile ?: return null
    for (extension in listOf("ttml", "lrc", "srt")) {
        val direct = File(parent, "${media.nameWithoutExtension}.$extension")
        if (direct.isFile && direct.canRead()) return direct
    }
    val acceptedStems = setOf(
        media.nameWithoutExtension.lowercase(Locale.ROOT),
        media.name.lowercase(Locale.ROOT),
    )
    return parent.listFiles()?.firstOrNull { candidate ->
        candidate.isFile && candidate.canRead() &&
            candidate.extension.lowercase(Locale.ROOT) in setOf("lrc", "ttml", "srt") &&
            candidate.nameWithoutExtension.lowercase(Locale.ROOT) in acceptedStems
    }
}

fun loadLocalLyrics(mediaPath: String?): LocalLyrics? {
    if (mediaPath.isNullOrBlank()) return null
    val file = findMatchingLrc(mediaPath)
    if (file != null) {
        val lines = runCatching {
            require(file.length() <= 1_048_576)
            val content = decodeLyrics(file.readBytes())
            when (file.extension.lowercase(Locale.ROOT)) { "ttml" -> parseTtml(content); "srt" -> parseSrt(content); else -> parseLrc(content) }
        }.getOrNull().orEmpty()
        lines.takeIf { it.isNotEmpty() }?.let { return LocalLyrics(file.absolutePath, it) }
    }
    return loadEmbeddedLyrics(File(mediaPath))
}

private fun loadEmbeddedLyrics(media: File): LocalLyrics? {
    if (!media.isFile) return null
    val text = when (media.extension.lowercase(Locale.ROOT)) {
        "mp3" -> readId3UnsynchronisedLyrics(media)
        "flac", "ogg", "opus" -> readVorbisLyrics(media)
        else -> null
    }?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val synced = parseLrc(text)
    val lines = if (synced.isNotEmpty()) synced else text.lineSequence()
        .map(String::trim).filter(String::isNotEmpty).map { LyricLine(0L, it) }.toList()
    return lines.takeIf { it.isNotEmpty() }?.let { LocalLyrics("embedded:${media.absolutePath}", it) }
}

private fun readId3UnsynchronisedLyrics(file: File): String? = runCatching {
    RandomAccessFile(file, "r").use { input ->
        val header = ByteArray(10)
        input.readFully(header)
        if (!header.copyOfRange(0, 3).contentEquals("ID3".toByteArray())) return null
        val version = header[3].toInt()
        val tagSize = syncSafe(header, 6)
        var consumed = 0
        while (consumed + 10 <= tagSize) {
            val frame = ByteArray(10)
            input.readFully(frame)
            consumed += 10
            val id = frame.copyOfRange(0, 4).toString(Charsets.ISO_8859_1)
            val size = if (version >= 4) syncSafe(frame, 4) else
                ((frame[4].toInt() and 255) shl 24) or ((frame[5].toInt() and 255) shl 16) or
                    ((frame[6].toInt() and 255) shl 8) or (frame[7].toInt() and 255)
            if (size <= 0 || size > tagSize - consumed) return null
            if (id == "USLT") {
                if (size > MAX_LYRIC_BYTES) return null
                val data = ByteArray(size)
                input.readFully(data)
                val encoding = data.firstOrNull()?.toInt() ?: return null
                val body = data.copyOfRange(4.coerceAtMost(data.size), data.size)
                val charset = when (encoding) { 1 -> StandardCharsets.UTF_16; 2 -> StandardCharsets.UTF_16BE; 3 -> StandardCharsets.UTF_8; else -> Charsets.ISO_8859_1 }
                val decoded = body.toString(charset)
                return decoded.substringAfter('\u0000').trim('\u0000', ' ')
            }
            input.seek(input.filePointer + size)
            consumed += size
        }
        null
    }
}.getOrNull()

private fun readVorbisLyrics(file: File): String? = runCatching {
    val limit = minOf(file.length(), MAX_SCAN_BYTES.toLong()).toInt()
    val bytes = ByteArray(limit)
    RandomAccessFile(file, "r").use { it.readFully(bytes) }
    val text = bytes.toString(StandardCharsets.UTF_8)
    val marker = listOf("LYRICS=", "UNSYNCEDLYRICS=").firstNotNullOfOrNull { key ->
        text.indexOf(key, ignoreCase = true).takeIf { it >= 0 }?.let { it + key.length }
    } ?: return null
    text.substring(marker).take(MAX_LYRIC_BYTES).substringBefore('\u0000').trim()
}.getOrNull()

private fun syncSafe(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0x7f) shl 21) or ((bytes[offset + 1].toInt() and 0x7f) shl 14) or
        ((bytes[offset + 2].toInt() and 0x7f) shl 7) or (bytes[offset + 3].toInt() and 0x7f)

private const val MAX_LYRIC_BYTES = 256 * 1024
private const val MAX_SCAN_BYTES = 8 * 1024 * 1024

private fun decodeLyrics(bytes: ByteArray): String {
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return bytes.copyOfRange(3, bytes.size).toString(StandardCharsets.UTF_8)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return bytes.copyOfRange(2, bytes.size).toString(StandardCharsets.UTF_16LE)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return bytes.copyOfRange(2, bytes.size).toString(StandardCharsets.UTF_16BE)
    }
    return runCatching {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }.getOrElse {
        bytes.toString(Charset.forName("Big5"))
    }
}
