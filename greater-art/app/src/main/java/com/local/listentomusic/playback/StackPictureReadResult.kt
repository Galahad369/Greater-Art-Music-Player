package com.local.listentomusic.playback

/** Analysis failures are not evidence that the pictures differ. Never apply an offset for them. */
enum class StackPictureFailure {
    QUICK_TIME_LIMIT, WAIT_TIME_LIMIT, DECODE_TIME_LIMIT, UNREADABLE, UNSUPPORTED,
    DECODER_ERROR, REFERENCE_UNAVAILABLE, INSUFFICIENT_REFERENCE,
}

internal data class StackPictureSamples(
    val windows: List<StackPictureWindow>,
    val frames: List<StackPictureFrame>,
    val decoderNames: Set<String>,
    val fallbackWindows: Set<Int>,
)

internal sealed interface StackPictureReadResult {
    data class Ready(val samples: StackPictureSamples) : StackPictureReadResult
    data class Failed(val reason: StackPictureFailure) : StackPictureReadResult
}

internal class UnsupportedPictureSource : IllegalArgumentException()
internal class UnreadablePictureSource(cause: Throwable) : java.io.IOException(cause)

/** Same 90s bound as before; distinguish lock contention from actual decoding. */
internal suspend fun <T : Any> boundedStackPictureRead(timeoutMs: Long = 90_000L,
    block: suspend (onDecoderAcquired: () -> Unit) -> T): Pair<T?, StackPictureFailure?> {
    var acquired = false
    val value = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { block { acquired = true } }
    return value to if (value != null) null else if (acquired) StackPictureFailure.DECODE_TIME_LIMIT else StackPictureFailure.WAIT_TIME_LIMIT
}
