package com.local.listentomusic.data

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ScopedMappingAndroidTest {
    @Test fun stableIdentitiesMatchActualAndroidEncoding() {
        for (id in listOf("primary:Music/a b.mp3", "primary:音樂/🎵 #+%.flac", "primary:a!b(c).mp3")) {
            assertEquals(Uri.encode(id), encodeUriComponent(id))
            val key = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, id).toString()
            assertEquals(key, stableDocumentKey(EXTERNAL_STORAGE_AUTHORITY, id))
            assertEquals(DocumentRef(EXTERNAL_STORAGE_AUTHORITY, id), parseStableDocumentKey(key))
        }
    }

    @Test fun actualPrimaryVolumeAndAliasesMapToOneIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val roots = storageVolumeRoots(context)
        val primary = roots.single { it.volumeId == "primary" }
        assertTrue(primary.rootPaths.contains(Environment.getExternalStorageDirectory().path))
        for (root in primary.rootPaths) {
            assertEquals("primary:Music/🎵.mp3", documentIdForLegacyPath("$root/Music/🎵.mp3", roots))
        }
        assertEquals(Environment.getExternalStorageDirectory().path + "/Music/a.mp3",
            legacyPathForDocumentId("primary:Music/a.mp3", roots))
    }
}
