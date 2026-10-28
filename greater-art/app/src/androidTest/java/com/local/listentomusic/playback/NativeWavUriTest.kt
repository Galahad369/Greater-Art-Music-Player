package com.local.listentomusic.playback

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class NativeWavUriTest {
    @Test @SdkSuppress(minSdkVersion = 29)
    fun scopedFloatWavIsExactAndDeletedUriCannotAcquireOutput(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val bytes = ByteBuffer.allocate(44 + 400).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(436).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(3).putShort(1).putInt(96_000).putInt(384_000)
            .putShort(4).putShort(32).put("data".toByteArray()).putInt(400)
        repeat(100) { bytes.putFloat(.0000001f * (it + 1)) }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "ga-native-${UUID.randomUUID()}.wav")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GreaterArtSharedOutputTest/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { it.write(bytes.array()) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            val file = MediaFile(uri.toString(), "Native float URI", 1, bytes.capacity().toLong(), 0L, MediaKind.AUDIO)
            val spec = pcmSpec(file, context)
            assertEquals(96_000, spec.rate); assertEquals(100L, spec.durationFrames)
            val stream = StackPcmStream(scope, file, spec, 0, context)
            try {
                val block = withTimeout(2000) { stream.blocks.receive() }
                assertEquals(100, block.samples.size)
                repeat(100) { assertEquals(.0000001f * (it + 1), block.samples[it], 0f) }
            } finally { withTimeout(2000) { stream.close() } }
            assertEquals(1, resolver.delete(uri, null, null))
            val host = object : PcmPrototypeHost {
                override suspend fun acquireExclusiveAudio(): Boolean { fail("Deleted URI acquired output"); return false }
                override suspend fun restoreLegacy(positionMs: Long, resume: Boolean, reason: String?) { fail("Deleted URI changed playback") }
                override fun onClock(positionMs: Long, playing: Boolean) { fail("Deleted URI changed clock") }
            }
            val engine = SharedClockPcmPrototype(context, host)
            try { assertFalse(engine.start(listOf(StackSlot(file)), file.path)) } finally { engine.release() }
        } finally {
            scope.cancel()
            resolver.delete(uri, null, null) // only this test's UUID-owned MediaStore row
        }
    }
}
