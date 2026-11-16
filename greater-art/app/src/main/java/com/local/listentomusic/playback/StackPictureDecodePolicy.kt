package com.local.listentomusic.playback

import java.nio.ByteBuffer

internal class IncompleteLocalDecode(cause: Throwable? = null, val reason: String = "incomplete-output") : IllegalStateException(reason, cause)

internal fun stackPictureUsesLocalAvc(search: StackPictureSearch, hardware: String, abi: String?, mime: String): Boolean =
    search == StackPictureSearch.QUICK && hardware == "ranchu" && abi == "x86_64" && mime == "video/avc"

internal const val STACK_PICTURE_MAX_PROBE_BYTES = 16 * 1024 * 1024

/** Metadata is a hint, not permission for an unbounded native allocation. */
internal fun stackPictureProbeCapacity(sampleSize: Long, formatMaximum: Int): Int {
    if (sampleSize > STACK_PICTURE_MAX_PROBE_BYTES) throw IncompleteLocalDecode(reason = "probe-limit")
    return maxOf(512 * 1024L, sampleSize, formatMaximum.toLong().coerceAtMost(STACK_PICTURE_MAX_PROBE_BYTES.toLong())).toInt()
}

internal fun stackPictureHasAvcIdr(packet: ByteBuffer, size: Int): Boolean {
    if (size !in 4..packet.limit()) return false
    for (index in 0 until size - 3) {
        if (packet.get(index).toInt() != 0 || packet.get(index + 1).toInt() != 0 || packet.get(index + 2).toInt() != 1) continue
        when (packet.get(index + 3).toInt() and 31) {
            5 -> return true
            in 1..4 -> break
        }
    }
    return false
}

/** Output must be complete AND in the presentation order consumed by the sampler. */
internal fun stackPictureOutputComplete(submitted: List<Long>, returned: List<Long>): Boolean =
    submitted.sorted() == returned

internal suspend fun <T> readStackPictureWindows(windows: List<StackPictureWindow>, local: Boolean,
    read: suspend (index: Int, window: StackPictureWindow, local: Boolean) -> List<T>): List<T> {
    var useLocal = local
    return buildList {
        for ((index, window) in windows.withIndex()) {
            val frames = try {
                read(index, window, useLocal)
            } catch (failure: IncompleteLocalDecode) {
                if (!useLocal) throw failure
                // Keep completed evidence, discard the failed attempt's partial frames,
                // then use the platform decoder for this and subsequent windows.
                useLocal = false
                read(index, window, false)
            }
            addAll(frames)
        }
    }
}
