package com.local.listentomusic

import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSourceAndroidTest {
    private fun media(source: String) = MediaFile("stable-id", "song", 1000, 1, 0,
        MediaKind.AUDIO, sourcePath = source)

    @Test fun fileAdapterPreservesUnicodeAndLiteralHash() {
        val source = "/storage/emulated/0/Download/孤独毒毒 # take.mp3"
        val item = media(source).toMediaItem()
        assertEquals("stable-id", item.mediaId)
        assertEquals("file", item.localConfiguration!!.uri.scheme)
        assertEquals(source, item.localConfiguration!!.uri.path)
    }

    @Test fun contentAdapterDoesNotReinterpretDocumentAsFile() {
        val source = "content://provider/document/primary%3AMusic%2Ftake.mp3"
        val item = media(source).toMediaItem()
        assertEquals(source, item.localConfiguration!!.uri.toString())
        assertEquals("content", item.localConfiguration!!.uri.scheme)
    }
}
