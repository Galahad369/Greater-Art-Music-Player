package com.local.listentomusic.ui

import org.junit.Assert.*
import org.junit.Test

class FolderInputTest {
    @Test fun acceptsNestedFolderAndNormalizesSeparators() {
        assertEquals("Music/Covers", normalizeExcludedFolderInput(" /Music\\Covers/ "))
    }
    @Test fun rejectsTraversalEmptySegmentsAndDevicePaths() {
        listOf("", " ", ".", "..", "Music/../Covers", "Music/./Covers", "C:/Music", "Music//Covers", "a\u0000b").forEach {
            assertNull(it, normalizeExcludedFolderInput(it))
        }
    }
}
