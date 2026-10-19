package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class DeveloperSummaryTest {
    private fun snapshot() = DeveloperSummary("test", "NODES", true, true, 5, 219,
        stackTracks = 6, surfaceOwner = "BACKGROUND", warnings = listOf("NO_FRAME", "NO_FRAME"), hasFailure = true)

    @Test fun summaryIsBoundedAndUsesExplicitScreenAndCounts() {
        val lines = snapshot().lines(AppLanguage.ENGLISH)
        assertEquals(7, lines.size)
        assertTrue(lines.contains("Screen: NODES"))
        assertTrue(lines.any { it.contains("Queue: 5") && it.contains("Library: 219") })
        assertEquals(1, lines.count { it.contains("NO_FRAME") })
        assertTrue(lines.none { it.contains("position=") || it.contains("decoder=") })
    }

    @Test fun emptyOverlaySummaryDoesNotInventLibraryCountsOrFailures() {
        val lines = DeveloperSummary("v", "NOW_PLAYING", false, false, 0).lines(AppLanguage.ENGLISH)
        assertEquals(6, lines.size)
        assertTrue(lines.any { it.startsWith("Paused · Audio") })
        assertTrue(lines.contains("Stack: Off"))
        assertTrue(lines.contains("Warnings: None"))
        assertTrue(lines.none { it.contains("Library:") || it.contains("failure") })
    }

    @Test fun everyLanguageGetsLocalizedChromeAndSummary() {
        for (language in AppLanguage.entries.filter { it != AppLanguage.ENGLISH }) {
            assertNotEquals("Developer", diagnosticsText(language, "Developer"))
            assertFalse(snapshot().lines(language).any { it.startsWith("Screen:") })
        }
    }

    @Test fun copyAlwaysPreservesFullPayloadAndAllRegionDetails() {
        val detail = (1..400).joinToString("\n") { "diagnostic_$it=$it" }
        assertEquals("$detail\nACTIVE REGIONS\n- 節點 · 48×48px\n- timeline\n",
            completeDeveloperReport(detail, listOf("節點 · 48×48px", "timeline")))
    }

    @Test fun normalExitRecordsAreNotMislabelledCrashes() {
        assertFalse(hasLocalFailure("No local failure record"))
        assertFalse(hasLocalFailure("exit reason=10 at=0 status=0"))
        assertTrue(hasLocalFailure("exit reason=10 at=0\nfailure=uncaught version=v"))
    }
}
