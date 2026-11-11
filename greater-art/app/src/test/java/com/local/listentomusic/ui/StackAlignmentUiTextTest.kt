package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StackAlignmentUiTextTest {
    @Test fun signedFractionalInputAndLegacyDecimalCommaStaySupported() {
        assertEquals(12.375, parseStackOffset("+12.375")!!, 0.0)
        assertEquals(-12.375, parseStackOffset("−12,375")!!, 0.0)
        assertEquals(30000.0, parseStackOffset("30000")!!, 0.0)
        assertEquals(-30000.0, parseStackOffset("-30000")!!, 0.0)
        for (input in listOf("30000.001", "-30000.001", "NaN", "Infinity", "")) {
            assertNull(input, parseStackOffset(input))
        }
        assertEquals("12.375", stackOffsetText(12.375))
        assertEquals("30000", stackOffsetText(30000.0))
    }

    @Test fun alignmentReviewAndManualDirectionsAreBundledForEveryLanguage() {
        val keys = listOf(
            "Timing applied", "Reference track", "Current → proposed", "Speed fit",
            "Sound matching follows the shared backing track, not the singers' phrasing.",
            "Picture matching aligns images. It does not guarantee matching sound.",
            "Could not establish one consistent sound offset.",
            "Could not establish one consistent picture offset.",
            "Not completed before the quick scan time limit.",
            "Kept unchanged", "Apply timing ({count})", "Earlier", "Later",
            "Enter −30000 to +30000 ms",
            "Positive moves this track earlier; negative moves it later.",
            "Applying timing briefly pauses the Stack. Listen to check the result.",
            "Timing was applied, not verified. Listen and adjust if needed.",
        )
        for (language in AppLanguage.entries) for (key in keys) {
            val translated = additionalUiText(language, key)
            assertNotEquals("Missing $language translation: $key", null, translated)
            if (language != AppLanguage.ENGLISH) {
                assertNotEquals("English fallback for $language: $key", key, translated)
            }
            assertEquals(translated, uiText(language, key, "SHOULD_NOT_FALL_BACK"))
        }
    }
}
