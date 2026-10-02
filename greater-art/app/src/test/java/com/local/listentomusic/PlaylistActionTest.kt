package com.local.listentomusic

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistActionTest {
    @Test
    fun stackSaveUsesExplicitNameWhenProvided() {
        assertEquals("My Stack", stackPlaylistName("  My Stack  ", "piano", 3))
    }

    @Test
    fun stackSaveFallsBackToKeywordWhenNameBlank() {
        assertEquals("Stack · piano", stackPlaylistName("", "  piano  ", 3))
    }

    @Test
    fun stackSaveStillWorksWithBlankNameAndKeyword() {
        assertEquals("Stack · 3 tracks", stackPlaylistName("", "", 3))
    }
}
