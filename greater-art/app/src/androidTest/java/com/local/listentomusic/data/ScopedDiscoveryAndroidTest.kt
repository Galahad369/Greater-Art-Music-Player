package com.local.listentomusic.data

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@androidx.test.filters.SdkSuppress(minSdkVersion = 29)
class ScopedDiscoveryAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun realOwnedMediaStoreFixtureIsDiscoveredAndRemovedOnRescan() = runBlocking {
        val resolver = context.contentResolver
        val name = "GA-scan-孤独-${System.nanoTime()}.wav"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GreaterArtScannerTest/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            val scanner = ScopedMediaDiscovery(AndroidScopedDiscoveryAccess(resolver))
            assertFalse(scanner.scan(emptyList()).records.any { it.media.name == name })
            val pcmBytes = 3200
            val wav = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000)
                .putShort(2).putShort(16).put("data".toByteArray()).putInt(pcmBytes).array()
            requireNotNull(resolver.openOutputStream(uri)).use { it.write(wav) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            val first = scanner.scan(emptyList()).records.single { it.media.name == name }.media
            val second = scanner.scan(emptyList()).records.single { it.media.name == name }.media
            assertEquals(first.id, second.id)
            assertEquals("content", first.toMediaItem().localConfiguration!!.uri.scheme)
            assertEquals(44L + pcmBytes, first.sizeBytes)
            // Optional real-picker evidence. Never grant or inspect an arbitrary user folder.
            val tree = InstrumentationRegistry.getArguments().getString("safTree")
            val safItem = tree?.let {
                assertEquals("primary:Download/GreaterArtScannerTest",
                    android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(it)))
                val scoped = scanner.scan(listOf(it), false)
                assertTrue(scoped.issues.toString(), scoped.issues.isEmpty())
                val item = scoped.records.single { record -> record.media.name == name }.media
                assertTrue(item.sourcePath.contains("/tree/"))
                assertFalse(item.id.contains("/tree/"))
                assertEquals(item.id, scanner.scan(listOf(it), false).records.single { record -> record.media.name == name }.media.id)
                requireNotNull(resolver.openFileDescriptor(Uri.parse(item.sourcePath), "r")).use {
                    assertEquals(44L + pcmBytes, it.statSize)
                }
                item
            }
            resolver.delete(uri, null, null)
            assertFalse(scanner.scan(emptyList()).records.any { it.media.id == first.id })
            if (tree != null && safItem != null) {
                assertFalse(scanner.scan(listOf(tree), false).records.any { it.media.id == safItem.id })
                resolver.releasePersistableUriPermission(Uri.parse(tree), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val revoked = scanner.scan(listOf(tree), false)
                assertTrue(revoked.records.isEmpty())
                assertEquals(DiscoveryProblem.PERMISSION_DENIED, revoked.issues.single().problem)
            }
        } finally {
            // Only this test-created, exact URI; never delete user media or folders.
            resolver.delete(uri, null, null)
        }
    }

    @Test fun ungrantedRealTreeIsDeniedBeforeEnumeration() = runBlocking {
        val scanner = ScopedMediaDiscovery(AndroidScopedDiscoveryAccess(context.contentResolver))
        val result = scanner.scan(listOf("content://com.android.externalstorage.documents/tree/primary%3AGA-ungranted-${System.nanoTime()}"), false)
        assertTrue(result.records.isEmpty())
        assertEquals(DiscoveryProblem.PERMISSION_DENIED, result.issues.single().problem)
    }

    private abstract class Provider : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
    }

    private fun resolverFor(provider: Provider): ContentResolver {
        provider.attachInfo(context, android.content.pm.ProviderInfo().apply { authority = "media"; exported = true })
        return ContentResolver.wrap(provider)
    }

    @Test fun providerContractKeepsReadableAudioWhenVideoDenied() = runBlocking {
        val provider = object : Provider() {
            override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                selectionArgs: Array<String>?, sortOrder: String?): Cursor {
                assertTrue(selection.orEmpty().contains("relative_path LIKE ?"))
                assertTrue(selection.orEmpty().contains("is_pending=0"))
                assertTrue(selection.orEmpty().contains("is_trashed=0"))
                assertEquals("Download/%", selectionArgs!!.single())
                assertFalse(projection!!.contains("_data"))
                if (uri.path.orEmpty().contains("video")) throw SecurityException()
                return MatrixCursor(projection).apply {
                    addRow(projection.map { when (it) {
                        "_id" -> 7L; "_display_name" -> "fixture.mp3"; "mime_type" -> "audio/mpeg"
                        "_size" -> 42L; "date_modified" -> 100L; "duration" -> 1234L; else -> null
                    } }.toTypedArray<Any?>())
                }
            }
        }
        val result = ScopedMediaDiscovery(AndroidScopedDiscoveryAccess(resolverFor(provider))).scan(emptyList())
        assertEquals(1, result.records.size)
        assertEquals(100000L, result.records.single().media.modifiedMs)
        assertEquals(1234L, result.records.single().media.durationMs)
        assertEquals(DiscoveryProblem.PERMISSION_DENIED, result.issues.single().problem)
    }

    @Test fun cancellationReachesBlockedAndroidProvider() = runBlocking {
        val entered = CountDownLatch(1)
        val released = CountDownLatch(1)
        val provider = object : Provider() {
            override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                selectionArgs: Array<String>?, sortOrder: String?): Cursor = error("Expected cancellable query")
            override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                selectionArgs: Array<String>?, sortOrder: String?, cancellationSignal: CancellationSignal?): Cursor {
                requireNotNull(cancellationSignal).setOnCancelListener { released.countDown() }
                entered.countDown()
                check(released.await(5, TimeUnit.SECONDS)) { "Cancellation never reached provider" }
                cancellationSignal.throwIfCanceled()
                return MatrixCursor(requireNotNull(projection))
            }
        }
        val scanner = ScopedMediaDiscovery(AndroidScopedDiscoveryAccess(resolverFor(provider)))
        val job = launch(Dispatchers.IO) { scanner.scan(emptyList()) }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            job.cancelAndJoin()
            assertEquals(0L, released.count)
            assertTrue(job.isCancelled)
        } finally { released.countDown(); job.cancelAndJoin() }
    }
}
