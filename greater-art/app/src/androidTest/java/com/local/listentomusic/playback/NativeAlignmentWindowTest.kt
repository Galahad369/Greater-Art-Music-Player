package com.local.listentomusic.playback

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class NativeAlignmentWindowTest {
    @Test fun quietFloatBackingRemainsPresentInNativeAndCoarseAlignmentViews(): Unit = runBlocking {
        val rate = 96000
        val frames = rate / 5
        val bytes = ByteBuffer.allocate(44 + frames * 8).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + frames * 8).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(3).putShort(2).putInt(rate).putInt(rate * 8)
            .putShort(8).putShort(32).put("data".toByteArray()).putInt(frames * 8)
        fun left(frame: Int) = .0000001f * (frame % 17 + 1)
        fun right(frame: Int) = -.00000005f * (frame % 17 + 1)
        repeat(frames) { bytes.putFloat(left(it)); bytes.putFloat(right(it)) }
        val source = File("/sdcard/Download", "ga-align-float-${UUID.randomUUID()}.wav")
        try {
            source.writeBytes(bytes.array())
            val file = MediaFile(source.canonicalPath, source.name, 200, source.length(), source.lastModified(), MediaKind.AUDIO)
            val native = decodeStackPcmWindow(file, 50, 100, requestedRate = null)
            assertEquals(rate, native.sampleRate); assertEquals(9600, native.mono.size)
            assertTrue(native.stereo)
            for (i in native.mono.indices) {
                val a = left(4800 + i).toDouble(); val b = right(4800 + i).toDouble()
                assertEquals(((a + b) / 2).toFloat(), native.mono[i], 0f)
                assertEquals(((a - b) / 2).toFloat(), native.side[i], 0f)
            }
            val coarse = decodeStackPcmWindow(file, 50, 100, requestedRate = 8000)
            assertEquals(8000, coarse.sampleRate)
            assertTrue(coarse.mono.all { it > 0f && it < .00001f })
            assertTrue(coarse.side.all { it > 0f && it < .00001f })
        } finally { source.delete() }
    }
}
