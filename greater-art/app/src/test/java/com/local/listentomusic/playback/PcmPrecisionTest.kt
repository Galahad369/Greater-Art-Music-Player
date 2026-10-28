package com.local.listentomusic.playback

import android.media.AudioFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmPrecisionTest {
    @Test fun convertedOrUnknownRawPrecisionCannotBeAcceptedAsNativePcm16() {
        for (bits in listOf(null, 0, 24, 32, 64)) {
            assertFalse("native bits=$bits", pcmPrecisionAdmissible("audio/raw", bits, AudioFormat.ENCODING_PCM_16BIT))
        }
        assertFalse(pcmPrecisionAdmissible("audio/flac", 24, AudioFormat.ENCODING_PCM_16BIT))
        assertFalse(pcmPrecisionAdmissible("audio/raw", 64, AudioFormat.ENCODING_PCM_FLOAT))
        assertFalse("Float output cannot preserve integer PCM32", pcmPrecisionAdmissible("audio/raw", 32, AudioFormat.ENCODING_PCM_FLOAT))
        assertFalse("Lossless PCM32 cannot silently lose its low bits", pcmPrecisionAdmissible("audio/flac", 32, AudioFormat.ENCODING_PCM_FLOAT))
    }

    @Test fun qualifiedNativeFormatsAndLossyDecoderOutputRemainAvailable() {
        assertTrue(pcmPrecisionAdmissible("audio/raw", 16, AudioFormat.ENCODING_PCM_16BIT))
        assertTrue(pcmPrecisionAdmissible("audio/raw", 8, AudioFormat.ENCODING_PCM_16BIT))
        assertTrue(pcmPrecisionAdmissible("audio/raw", 32, AudioFormat.ENCODING_PCM_FLOAT, sourceFloat = true))
        assertTrue(pcmPrecisionAdmissible("audio/flac", 24, AudioFormat.ENCODING_PCM_FLOAT))
        assertTrue(pcmPrecisionAdmissible("audio/mp4a-latm", null, AudioFormat.ENCODING_PCM_16BIT))
        assertTrue(pcmPrecisionAdmissible("audio/mpeg", null, AudioFormat.ENCODING_PCM_16BIT))
        assertFalse(pcmPrecisionAdmissible("audio/raw", 24, AudioFormat.ENCODING_PCM_24BIT_PACKED))
    }
}
