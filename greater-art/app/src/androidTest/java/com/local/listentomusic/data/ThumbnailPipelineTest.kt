package com.local.listentomusic.data

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.ui.ListScrollBudget
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ThumbnailPipelineTest {
    private class Fixture {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.cacheDir, "thumbnail-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context: Context = object : ContextWrapper(base) { override fun getCacheDir(): File = directory }
        val repository = ThumbnailRepository(context)
        val cache get() = File(directory, "media_thumbnails")

        init {
            val cover = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
            File(directory, "folder.png").outputStream().use { cover.compress(Bitmap.CompressFormat.PNG, 100, it) }
            cover.recycle()
        }

        fun audio(name: String): MediaFile {
            val source = File(directory, "$name.wav")
            val frames = 4_410
            val data = ByteBuffer.allocate(44 + frames * 2).order(ByteOrder.LITTLE_ENDIAN)
            data.put("RIFF".toByteArray()).putInt(data.capacity() - 8).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(44_100).putInt(88_200)
                .putShort(2).putShort(16).put("data".toByteArray()).putInt(frames * 2)
            source.writeBytes(data.array())
            return MediaFile(source.path, name, 100, source.length(), source.lastModified(), MediaKind.AUDIO)
        }

        suspend fun close() {
            ListScrollBudget.set("thumbnail-test", false)
            repository.clear()
            // Delete only the UUID-named fixture owned by this test, never the app cache.
            directory.deleteRecursively()
        }
    }

    private suspend fun until(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(10)
    }

    @Test fun flingShowsRamHitsImmediatelyAndDefersMissesThenReusesVerifiedCover() = runBlocking {
        val fixture = Fixture()
        try {
            val a = fixture.audio("first")
            val b = fixture.audio("second")
            val first = withTimeout(8_000) { fixture.repository.load(a) }
            assertNotNull(first)
            val before = fixture.repository.stats.value.generated
            ListScrollBudget.set("thumbnail-test", true)
            assertSame(first, withTimeout(1_000) { fixture.repository.load(a) })
            val pending = async { fixture.repository.load(b) }
            until { fixture.repository.stats.value.queued == 1 }
            assertEquals(before, fixture.repository.stats.value.generated)
            assertFalse(pending.isCompleted)
            ListScrollBudget.set("thumbnail-test", false)
            assertSame(first, withTimeout(8_000) { pending.await() })
            assertEquals(1, fixture.repository.stats.value.artworkReuses)
            assertEquals(1, fixture.cache.listFiles().orEmpty().count { it.extension == "webp" })
            assertEquals(2, fixture.cache.listFiles().orEmpty().count { it.extension == "ref" })
            assertEquals(first!!.allocationByteCount / 1024, fixture.repository.stats.value.memoryKb)
            assertFalse(first.isRecycled)
        } finally { fixture.close() }
    }

    @Test fun warmDiskSurvivesTrimAndOversizedDiskImagesDecodeAtThumbnailSize() = runBlocking {
        val fixture = Fixture()
        try {
            val audio = fixture.audio("warm")
            assertNotNull(withTimeout(8_000) { fixture.repository.load(audio) })
            fixture.repository.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            assertEquals(0, fixture.repository.stats.value.memoryKb)
            val asset = fixture.cache.listFiles()!!.single { it.extension == "webp" }
            val oversized = Bitmap.createBitmap(2_048, 2_048, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
            asset.outputStream().use { oversized.compress(Bitmap.CompressFormat.PNG, 100, it) }
            oversized.recycle()
            val generated = fixture.repository.stats.value.generated
            val result = withTimeout(8_000) { fixture.repository.load(audio) }
            assertNotNull(result)
            assertTrue(result!!.width <= 512 && result.height <= 512)
            assertEquals(generated, fixture.repository.stats.value.generated)
            assertTrue(fixture.repository.stats.value.diskHits > 0)
            assertFalse(result.isRecycled)
        } finally { fixture.close() }
    }

    @Test fun clearingCacheCancelsPendingRowsAndRejectsUntrustedArtworkReferences() = runBlocking {
        val fixture = Fixture()
        try {
            val audio = fixture.audio("clear")
            ListScrollBudget.set("thumbnail-test", true)
            val pending = async { fixture.repository.load(audio) }
            until { fixture.repository.stats.value.queued == 1 }
            fixture.repository.clear()
            assertNull(withTimeout(2_000) { pending.await() })
            assertEquals(0, fixture.repository.stats.value.queued)
            assertTrue(fixture.cache.listFiles().orEmpty().isEmpty())
            ListScrollBudget.set("thumbnail-test", false)
            assertNotNull(withTimeout(8_000) { fixture.repository.load(audio) })
            fixture.repository.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            fixture.cache.listFiles()!!.single { it.extension == "ref" }.writeText("../../outside-cache")
            // A reference is an exact SHA-256 asset name, never an arbitrary filesystem path.
            assertNotNull(withTimeout(8_000) { fixture.repository.load(audio) })
            assertTrue(fixture.cache.listFiles()!!.single { it.extension == "ref" }.readText().matches(Regex("art-[0-9a-f]{64}")))
        } finally { fixture.close() }
    }
}
