package com.local.listentomusic.model

import org.junit.Assert.*
import org.junit.Test

class ThumbnailRequestIdentityTest {
    @Test fun identityIsReusedAndCopiesInvalidateTheRelevantFields() {
        val file = MediaFile("clip", "clip", 1_000, 42, 7, MediaKind.VIDEO, sourcePath = "source.mp4")
        assertSame(file.thumbnailRequestId, file.thumbnailRequestId)
        assertEquals("source.mp4|42|7|", file.thumbnailRequestId)
        assertNotEquals(file.thumbnailRequestId, file.copy(sourcePath = "other.mp4").thumbnailRequestId)
        assertNotEquals(file.thumbnailRequestId, file.copy(coverUri = "cover").thumbnailRequestId)
        assertNotEquals(file.thumbnailRequestId, file.copy(modifiedMs = 8).thumbnailRequestId)
        assertEquals(file.thumbnailRequestId, file.copy(name = "Display-only rename").thumbnailRequestId)
    }
}
