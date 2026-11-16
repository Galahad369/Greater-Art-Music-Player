package com.local.listentomusic.ui.theme

import org.junit.Assert.*
import org.junit.Test

class VideoThemePaletteTest {
    private val red = 0xffff0000.toInt()
    private val blue = 0xff0000ff.toInt()
    private val green = 0xff00ff00.toInt()
    private fun VideoThemePalette.sample(id: String, color: Int, time: Long) = sample(id, List(3) { color }, color, time, time)

    @Test fun laterFramesMoveThroughIntermediateColorsThenReachTheirTarget() {
        val cache = VideoThemePalette()
        cache.sample("video", red, 0)
        cache.sample("video", blue, 1000)
        assertEquals(List(3) { red }, cache.colors("video"))
        cache.tick(1450)
        val middle = cache.colors("video").first()
        assertTrue((middle ushr 16 and 255) in 120..135)
        assertTrue((middle and 255) in 120..135)
        cache.tick(1900)
        assertEquals(List(3) { blue }, cache.colors("video"))
        assertFalse(cache.moving())
    }

    @Test fun hostReentryAndNewArtworkDoNotResetTheCachedPalette() {
        val cache = VideoThemePalette()
        cache.sample("video", red, 0)
        cache.sample("video", blue, 1000)
        cache.tick(1450)
        val displayed = cache.colors("video")
        cache.freeze(1450)
        cache.seedArtwork("video", listOf(green))
        cache.tick(9000)
        assertEquals(displayed, cache.colors("video"))
        assertEquals(displayed, cache.snapshot()["video"])
    }

    @Test fun powerSavingFreezesTheObservedAverageNotAnEntryTimeSnapshot() {
        val cache = VideoThemePalette()
        cache.sample("video", red, 0)
        cache.sample("video", blue, 1000)
        cache.tick(1900)
        cache.powerSaving(true, 2000)
        val stable = List(3) { 0xff800080.toInt() }
        assertEquals(stable, cache.colors("video"))
        cache.sample("video", green, 3000)
        cache.seedArtwork("video", listOf(green))
        cache.tick(9000)
        assertEquals(stable, cache.colors("video"))
        assertFalse(cache.moving())
        cache.powerSaving(false, 10000)
        cache.sample("video", blue, 11000)
        cache.tick(11900)
        assertEquals(List(3) { blue }, cache.colors("video"))
    }

    @Test fun firstVideoInPowerSavingCanReplaceArtworkOnlyOnce() {
        val cache = VideoThemePalette()
        cache.powerSaving(true, 0)
        cache.seedArtwork("video", listOf(green))
        cache.sample("video", red, 1000)
        cache.sample("video", blue, 2000)
        assertEquals(List(3) { red }, cache.colors("video"))
    }

    @Test fun repeatedOrStaleSamplesCannotReweightTheAverageAndTracksStaySeparate() {
        val cache = VideoThemePalette()
        cache.sample("a", red, 1000)
        cache.sample("a", blue, 900)
        cache.sample("a", blue, 1000)
        cache.sample("b", green, 2000)
        cache.powerSaving(true, 3000)
        assertEquals(List(3) { red }, cache.colors("a"))
        assertEquals(List(3) { green }, cache.colors("b"))
    }

    @Test fun cacheIsBoundedAndKeepsTheMostRecentlyUsedTrack() {
        val cache = VideoThemePalette(2)
        cache.sample("a", red, 1)
        cache.sample("b", green, 2)
        cache.colors("a")
        cache.sample("c", blue, 3)
        assertEquals(setOf("a", "c"), cache.snapshot().keys)
    }

    @Test fun averageIgnoresTransparentPixelsAndBoundsItsWork() {
        assertNull(averageVideoColor(listOf(0x00000000)))
        assertEquals(0xff7f007f.toInt(), averageVideoColor(listOf(red, blue, 0x00ffffff)))
        assertEquals(red, averageVideoColor(List(576) { red } + blue))
    }

    @Test fun everyIntermediateAccentKeepsTextReadableAndOledSurfacesUnchanged() {
        for (dark in listOf(false, true)) {
            val cache = VideoThemePalette()
            cache.sample("video", red, 0)
            cache.sample("video", blue, 1000)
            for (time in 1000L..1900L step 100) {
                cache.tick(time)
                val base = appColorScheme(com.local.listentomusic.data.ColorTheme.VIDEO, dark)
                val scheme = artworkAccentScheme(base, cache.colors("video"), 1f, com.local.listentomusic.data.ColorTheme.VIDEO)
                assertTrue(accentContrast(scheme.primary, scheme.onPrimary) >= 4.49f)
                assertEquals(base.background, scheme.background)
                assertEquals(base.surface, scheme.surface)
            }
        }
    }
}
