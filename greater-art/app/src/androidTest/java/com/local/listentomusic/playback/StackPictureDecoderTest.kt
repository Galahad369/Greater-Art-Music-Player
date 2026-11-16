package com.local.listentomusic.playback

import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.OfflineAnalysisBudget
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackPictureDecoderTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun riona(): MediaFile {
        assertEquals("Run the decoder comparison on the owned A55 emulator", "ranchu", Build.HARDWARE)
        assertEquals("x86_64", Build.SUPPORTED_ABIS.first())
        val file = File(context.getExternalFilesDir(null), "qa-stack-picture-20261105/take-1.mp4")
        assertTrue("Use the existing verified original, never a replacement fixture", file.isFile)
        return MediaFile(file.path, file.name, 188_000, file.length(), file.lastModified(), MediaKind.VIDEO)
    }

    @Test fun localAndPlatformDecodersKeepExactlyTheSameFrameEvidence(): Unit = runBlocking {
        val engine = StackPictureAlign(context)
        val file = riona()
        val window = listOf(StackPictureWindow(27_333_333, 35_333_333))
        val local = (engine.readSamples(file, StackPictureSearch.QUICK, window) as StackPictureReadResult.Ready).samples
        val platform = (engine.readSamples(file, StackPictureSearch.QUICK, window, allowLocalAvc = false) as StackPictureReadResult.Ready).samples
        assertTrue(local.decoderNames.contains("c2.android.avc.decoder"))
        assertFalse("The comparison must use distinct local/platform implementations", platform.decoderNames.any { it in local.decoderNames })
        assertTrue("Parity must exercise the local decoder, not compare two platform fallbacks", local.fallbackWindows.isEmpty())
        assertTrue("Real decoded evidence is required", local.frames.size >= 60)
        assertEquals(local.windows, platform.windows)
        assertEquals(local.frames.size, platform.frames.size)
        fun fingerprint(frames: List<StackPictureFrame>): String {
            val sha = MessageDigest.getInstance("SHA-256")
            val bytes = ByteBuffer.allocate(16 + STACK_PICTURE_GRID * STACK_PICTURE_GRID * 4)
            for (frame in frames) {
                bytes.clear(); bytes.putLong(frame.timeUs); bytes.putLong(frame.edges)
                frame.pixels.forEach { bytes.putFloat(it) }
                sha.update(bytes.array())
            }
            return sha.digest().joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 255) }
        }
        val localHash = fingerprint(local.frames)
        assertEquals("Both timestamps AND all normalized luma pixels must agree", localHash, fingerprint(platform.frames))
        Log.i("GA-StackPictureQA", "decoderParity local=${local.decoderNames} platform=${platform.decoderNames} frames=${local.frames.size} sha256=$localHash")
    }

    @Test fun cancellationReleasesTheDecoderBudgetAndTheNextReadSucceeds(): Unit = runBlocking {
        assertFalse(OfflineAnalysisBudget.mutex.isLocked)
        val file = riona()
        val engine = StackPictureAlign(context)
        val job = async(Dispatchers.Default) { engine.readSamples(file.copy(clipEndMs = 180_000), StackPictureSearch.WIDE) }
        withTimeout(5000) { while (!OfflineAnalysisBudget.mutex.isLocked) delay(1) }
        delay(200)
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse("A cancelled scan must not hold the analysis decoder", OfflineAnalysisBudget.mutex.isLocked)
        assertTrue(withTimeout(45_000) { engine.readSamples(file.copy(clipStartMs = 30_000, clipEndMs = 34_000), StackPictureSearch.QUICK) } is StackPictureReadResult.Ready)
    }

    @Test fun unreadableReferenceIsNotPresentedAsPictureMismatch(): Unit = runBlocking {
        val missing = MediaFile(File(context.cacheDir, "missing-picture-file.mp4").path, "Missing", 5000, 0, 0, MediaKind.VIDEO)
        val other = missing.copy(path = "/owned/companion.mp4", sourcePath = "/owned/companion.mp4")
        val result = StackPictureAlign(context).estimateAll(missing, listOf(other), StackPictureSearch.QUICK) {}
        assertTrue(result.matches.isEmpty())
        assertFalse(result.quickTimedOut)
        assertEquals(StackPictureFailure.UNREADABLE, result.failures[missing.path])
        assertEquals(StackPictureFailure.REFERENCE_UNAVAILABLE, result.failures[other.path])
    }

    @Test fun audioOnlyInputIsAnUnsupportedVideoNotANoMatch(): Unit = runBlocking {
        val file = File.createTempFile("picture-audio-only-", ".wav", context.cacheDir)
        try {
            val wav = ByteBuffer.allocate(44 + 1600).order(ByteOrder.LITTLE_ENDIAN)
            wav.put("RIFF".toByteArray()).putInt(36 + 1600).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(1600)
            file.writeBytes(wav.array())
            val read = StackPictureAlign(context).readSamples(MediaFile(file.path, file.name, 100, file.length(), 0, MediaKind.VIDEO), StackPictureSearch.QUICK)
            assertEquals(StackPictureReadResult.Failed(StackPictureFailure.UNSUPPORTED), read)
        } finally { assertTrue(file.delete()) }
    }
}
