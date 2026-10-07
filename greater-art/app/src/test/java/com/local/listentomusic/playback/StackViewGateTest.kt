package com.local.listentomusic.playback

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackViewGateTest {
    private val layout = StackFeatureLayout(
        maxBins = 4_500,
        chromaSize = 12,
        fineSamplesPerBin = 64,
        fineSampleRateHz = 3_200,
    )

    private fun aligned(offset: Long) = StackAlignment(offset, 0.8, true)

    @Test
    fun acceptsWhenBothViewsAgreeWithinTheWindow() {
        val side = aligned(237)
        assertEquals(side, stackAgreeOnViews(side, aligned(237 + STACK_VIEW_AGREEMENT_MS)))
        assertEquals(side, stackAgreeOnViews(side, aligned(237 - STACK_VIEW_AGREEMENT_MS)))
    }

    @Test
    fun abstainsWhenViewsDisagree() {
        val result = stackAgreeOnViews(aligned(237), aligned(237 + STACK_VIEW_AGREEMENT_MS + 1))
        assertFalse(result.confident)
        assertEquals(0L, result.offsetMs)
    }

    @Test
    fun abstainsWhenMonoViewIsNotConfident() {
        val result = stackAgreeOnViews(aligned(237), StackAlignment(237, 0.2, false))
        assertFalse(result.confident)
        assertEquals(0L, result.offsetMs)
    }

    private fun features(seed: Long, n: Int = 400, withMono: Boolean): StackAudioFeatures {
        val random = Random(seed)
        fun chroma() = Array(n) { FloatArray(12) { random.nextFloat() } }
        fun fine() = FloatArray(n * 64) { random.nextFloat() }
        val envelope = FloatArray(n) { random.nextFloat() }
        val mono = StackAudioFeatures(envelope, chroma(), envelope, fine(), 3_200)
        return StackAudioFeatures(
            envelope,
            chroma(),
            FloatArray(n) { random.nextFloat() },
            fine(),
            3_200,
            if (withMono) mono else null,
        )
    }

    @Test
    fun withoutAnyMonoViewTheCallIsExactlyTheOriginal() {
        val a = features(1, withMono = false)
        val b = features(2, withMono = false)
        assertEquals(correlateStackFeatures(a, b), correlateStackFeaturesOnCommonView(a, b))
    }

    @Test
    fun whenOnlyOneFileUsedTheSideViewBothAreComparedAsMono() {
        val plain = features(1, withMono = false)
        val sided = features(3, withMono = true)
        val expected = correlateStackFeatures(plain, sided.monoView!!)
        assertEquals(expected, correlateStackFeaturesOnCommonView(plain, sided))
        assertEquals(
            correlateStackFeatures(sided.monoView!!, plain),
            correlateStackFeaturesOnCommonView(sided, plain),
        )
    }

    @Test
    fun whenBothUsedTheSideViewAgreementDecidesTheResult() {
        val a = features(4, withMono = true)
        val b = features(5, withMono = true)
        val side = correlateStackFeatures(a, b)
        val mono = correlateStackFeatures(a.monoView!!, b.monoView!!)
        assertEquals(
            if (side.confident) stackAgreeOnViews(side, mono) else side,
            correlateStackFeaturesOnCommonView(a, b),
        )
    }

    private fun roundTrip(original: StackAudioFeatures): StackAudioFeatures {
        val bytes = ByteArrayOutputStream().also {
            writeStackFeatures(DataOutputStream(it), original, layout)
        }.toByteArray()
        assertEquals(
            stackFeatureByteLength(original.envelope.size, layout, original.monoView != null),
            bytes.size.toLong(),
        )
        return readStackFeatures(
            DataInputStream(ByteArrayInputStream(bytes)),
            bytes.size.toLong(),
            layout,
        )
    }

    @Test
    fun cacheRoundTripKeepsBothViewsWithinSixteenBitPrecision() {
        val original = features(7, withMono = true)
        val restored = roundTrip(original)
        assertTrue(restored.monoView != null)
        assertTrue(original.envelope.contentEquals(restored.envelope))
        assertTrue(original.musicEnvelope.contentEquals(restored.musicEnvelope))
        for (view in listOf(original to restored, original.monoView!! to restored.monoView!!)) {
            assertTrue(view.first.chroma.indices.all {
                view.first.chroma[it].contentEquals(view.second.chroma[it])
            })
            val worst = view.first.fineSignal.indices.maxOf {
                kotlin.math.abs(view.first.fineSignal[it] - view.second.fineSignal[it])
            }
            assertTrue("fine signal error $worst", worst < 2e-5f)
        }
    }

    @Test
    fun cacheRoundTripWithoutMonoView() {
        val restored = roundTrip(features(8, withMono = false))
        assertEquals(null, restored.monoView)
    }

    @Test
    fun corruptCacheEntriesAreRejected() {
        val original = features(9, withMono = true)
        val bytes = ByteArrayOutputStream().also {
            writeStackFeatures(DataOutputStream(it), original, layout)
        }.toByteArray()

        fun reads(data: ByteArray, length: Long = data.size.toLong()) =
            runCatching {
                readStackFeatures(DataInputStream(ByteArrayInputStream(data)), length, layout)
            }.isSuccess

        assertTrue(reads(bytes))
        assertFalse("truncated", reads(bytes.copyOf(bytes.size - 5)))
        assertFalse("wrong length on disk", reads(bytes, bytes.size + 1L))
        val badFlags = bytes.copyOf().also { it[11] = 7 }
        assertFalse("unknown flags", reads(badFlags))
        val nanEnvelope = bytes.copyOf().also {
            for (i in 12 until 16) it[i] = 0xFF.toByte()
        }
        assertFalse("NaN in envelope", reads(nanEnvelope))
    }
}
