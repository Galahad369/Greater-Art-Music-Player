package com.local.listentomusic.ui

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class MediaFormattersTest {
    @Test fun hourLongDurationIsConsistentAndDoesNotOverflowInt() {
        assertEquals("1:10:00", formatDuration(70L * 60 * 1000))
        assertEquals("1:10:00", formatPlaybackTime(70L * 60 * 1000))
        assertEquals("1000:00:00", formatDuration(1000L * 3600 * 1000))
    }
    @Test fun unknownMetadataIsNotElapsedZero() {
        assertEquals("--:--", formatDuration(0))
        assertEquals("--:--", formatDuration(-1))
        assertEquals("0:00", formatPlaybackTime(0))
        assertEquals("0:00", formatPlaybackTime(-1))
        assertEquals("1:01", formatPlaybackTime(61000))
    }
    @Test fun displayDoesNotDependOnProcessLocale() {
        val prior = Locale.getDefault()
        try {
            for (locale in listOf(Locale.GERMANY, Locale.FRANCE, Locale.forLanguageTag("ar"))) {
                Locale.setDefault(locale)
                assertEquals("1:10:00", formatDuration(4_200_000))
                assertEquals("1.5 MB", formatBytes(1_572_864))
                assertEquals("1.0 GB", formatBytes(1_073_741_824))
                assertEquals("1.0 KB", formatBytes(1024))
                assertEquals("0 B", formatBytes(-1))
            }
        } finally { Locale.setDefault(prior) }
    }
}
