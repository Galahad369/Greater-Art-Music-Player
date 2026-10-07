package com.local.listentomusic.playback

import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.roundToInt

/** Shape constants of the on-disk Stack feature cache, passed in so this file stays Android-free. */
internal class StackFeatureLayout(
    val maxBins: Int,
    val chromaSize: Int,
    val fineSamplesPerBin: Int,
    val fineSampleRateHz: Int,
)

// Fine signals are peak-normalized to 0..1 (1.1 allowed for slack). Unsigned 16-bit storage
// keeps roughly 2e-5 resolution while paying for the optional second mono feature view.
private const val FINE_SCALE = 50_000f
private const val FLAG_MONO_VIEW = 1
private const val HEADER_BYTES = 12L

internal fun stackFeatureByteLength(
    bins: Int,
    layout: StackFeatureLayout,
    hasMonoView: Boolean,
): Long {
    val view = 4L * bins * layout.chromaSize + 2L * bins * layout.fineSamplesPerBin
    return HEADER_BYTES + 8L * bins + view + if (hasMonoView) view else 0L
}

internal fun writeStackFeatures(
    output: DataOutputStream,
    features: StackAudioFeatures,
    layout: StackFeatureLayout,
) {
    val n = features.envelope.size
    require(features.musicEnvelope.size == n && features.chroma.size == n)
    require(features.fineSignal.size == n * layout.fineSamplesPerBin)
    val mono = features.monoView
    if (mono != null) {
        require(mono.chroma.size == n && mono.fineSignal.size == features.fineSignal.size)
    }

    output.writeInt(n)
    output.writeInt(features.fineSignal.size)
    output.writeInt(if (mono != null) FLAG_MONO_VIEW else 0)
    features.envelope.forEach(output::writeFloat)
    features.musicEnvelope.forEach(output::writeFloat)
    features.chroma.forEach { frame -> frame.forEach(output::writeFloat) }
    writeFine(output, features.fineSignal)
    if (mono != null) {
        mono.chroma.forEach { frame -> frame.forEach(output::writeFloat) }
        writeFine(output, mono.fineSignal)
    }
}

private fun writeFine(output: DataOutputStream, signal: FloatArray) {
    for (value in signal) {
        output.writeShort((value.coerceIn(0f, 1.1f) * FINE_SCALE).roundToInt())
    }
}

/** Throws on any inconsistency; callers treat malformed cache entries as a cache miss. */
internal fun readStackFeatures(
    input: DataInputStream,
    fileLength: Long,
    layout: StackFeatureLayout,
): StackAudioFeatures {
    val n = input.readInt()
    val fineCount = input.readInt()
    val flags = input.readInt()
    require(
        n in 150..layout.maxBins &&
            fineCount == n * layout.fineSamplesPerBin &&
            (flags == 0 || flags == FLAG_MONO_VIEW),
    )
    val hasMono = flags == FLAG_MONO_VIEW
    require(fileLength == stackFeatureByteLength(n, layout, hasMono))

    fun floats(count: Int, max: Float? = null) = FloatArray(count) {
        input.readFloat().also { value ->
            require(value.isFinite() && value >= 0f && (max == null || value <= max))
        }
    }
    fun chroma() = Array(n) { floats(layout.chromaSize, 1.1f) }
    fun fine() = FloatArray(fineCount) {
        ((input.readShort().toInt() and 0xFFFF) / FINE_SCALE).also { value ->
            require(value <= 1.1f + 1e-4f)
        }
    }

    val envelope = floats(n)
    val musicEnvelope = floats(n)
    val mainChroma = chroma()
    val mainFine = fine()
    val monoView = if (hasMono) {
        StackAudioFeatures(
            envelope = envelope,
            chroma = chroma(),
            musicEnvelope = envelope,
            fineSignal = fine(),
            fineSampleRateHz = layout.fineSampleRateHz,
        )
    } else {
        null
    }
    return StackAudioFeatures(
        envelope = envelope,
        chroma = mainChroma,
        musicEnvelope = musicEnvelope,
        fineSignal = mainFine,
        fineSampleRateHz = layout.fineSampleRateHz,
        monoView = monoView,
    )
}
