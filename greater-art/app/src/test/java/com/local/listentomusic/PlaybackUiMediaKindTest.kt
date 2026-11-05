package com.local.listentomusic

import com.local.listentomusic.model.MediaKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackUiMediaKindTest {
    @Test fun scopedVideoDoesNotNeedAnExtensionOrFirstFrame() {
        assertTrue(PlaybackUiState(currentPath = "content://provider/document/42", mediaKind = MediaKind.VIDEO).isVideo)
    }

    @Test fun typedAudioOverridesStaleVideoGeometry() {
        assertFalse(PlaybackUiState(currentPath = "content://provider/document/43", mediaKind = MediaKind.AUDIO,
            videoWidth = 1920, videoHeight = 1080).isVideo)
    }
}
