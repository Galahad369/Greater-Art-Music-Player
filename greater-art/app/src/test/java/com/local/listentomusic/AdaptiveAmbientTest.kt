package com.local.listentomusic

import com.local.listentomusic.ui.components.*
import com.local.listentomusic.data.*
import org.junit.Assert.*
import org.junit.Test

class AdaptiveAmbientTest {
    private val fallback = listOf(0xff10151b.toInt(), 0xff141a22.toInt(), 0xff090d10.toInt())
    @Test fun brightAndDarkFramesHaveDifferentBloom() {
        val bright = analyzeAmbient(List(576) { 0xffffff00.toInt() }, fallback)
        val dark = analyzeAmbient(List(576) { 0xff030303.toInt() }, fallback)
        assertTrue(bright.brightness > dark.brightness)
        assertTrue(ambientLayers(bright, .5f, true, AmbientMotion.SUBTLE).bloom >
            ambientLayers(dark, .5f, true, AmbientMotion.SUBTLE).bloom)
        assertEquals(fallback, dark.colors)
    }
    @Test fun saturatedPixelsProduceTheirOwnPalette() {
        val signal = analyzeAmbient(List(576) { 0xff0044ff.toInt() }, fallback)
        assertTrue(signal.saturation > .9f)
        assertEquals(0xff0044ff.toInt(), signal.colors.first())
    }
    @Test fun transparentEmptyAndBlackHaveDefinedFallback() {
        assertEquals(fallback, analyzeAmbient(emptyList(), fallback).colors)
        assertEquals(fallback, analyzeAmbient(listOf(0x00123456), fallback).colors)
        assertEquals(fallback, analyzeAmbient(listOf(0xff000000.toInt()), fallback).colors)
        assertEquals(3, analyzeAmbient(emptyList(), emptyList()).colors.size)
    }
    @Test fun neutralFramesAreNotInventedAsSaturated() {
        val signal = analyzeAmbient(List(576) { 0xffaaaaaa.toInt() }, fallback)
        assertEquals(0f, signal.saturation, .0001f)
        assertEquals(0xffaaaaaa.toInt(), signal.colors.first())
    }
    @Test fun analysisHasBoundedInput() {
        val valid = List(576) { 0xffff0000.toInt() }
        assertEquals(analyzeAmbient(valid, fallback),
            analyzeAmbient(valid + List(10000) { 0xff00ff00.toInt() }, fallback))
    }
    @Test fun sceneCutsAreSmoothedButPaletteTargetsChange() {
        val engine = AdaptiveAmbientEngine()
        val first = engine.update("one", List(576) { 0xff030303.toInt() }, fallback, 0)
        val next = engine.update("one", List(576) { 0xffffffff.toInt() }, fallback, 900)
        assertTrue(next.brightness > first.brightness)
        assertTrue(next.brightness < .6f)
        assertTrue(next.activity in 0f..1f)
        assertNotEquals(first.colors, next.colors)
    }
    @Test fun mediaChangesClearActivityAndBrightnessHistory() {
        val engine = AdaptiveAmbientEngine()
        engine.update("one", List(576) { 0xffff0000.toInt() }, fallback, 0)
        engine.update("one", List(576) { 0xff00ff00.toInt() }, fallback, 900)
        val audio = engine.update("two", emptyList(), fallback, 1000)
        assertEquals(0f, audio.activity, 0f)
        assertEquals(fallback, audio.colors)
    }
    @Test fun identicalSamplesRemainStable() {
        val engine = AdaptiveAmbientEngine()
        val first = engine.update("same", List(100) { 0xff6633aa.toInt() }, fallback, 0)
        repeat(20) { assertEquals(first, engine.update("same", List(100) { 0xff6633aa.toInt() }, fallback, (it + 1) * 900L)) }
    }
    @Test fun nonfiniteAndExtremeParametersAreBounded() {
        val bad = AmbientSignal(fallback, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val layers = ambientLayers(bad, Float.NaN, true, AmbientMotion.NORMAL)
        assertTrue(layers.aurora.isFinite() && layers.bloom.isFinite() && layers.flow.isFinite())
        assertEquals(ambientLayers(bad, 1f, true, AmbientMotion.NORMAL),
            ambientLayers(bad, 500f, true, AmbientMotion.NORMAL))
        assertEquals(0f, ambientLayers(bad, -5f, true, AmbientMotion.NORMAL).aurora, 0f)
    }
    @Test fun allFourLayersCoexistWhenMotionOff() {
        val layers = ambientLayers(analyzeAmbient(listOf(0xff336699.toInt()), fallback), .5f, true, AmbientMotion.OFF)
        assertEquals(1f, layers.classic, 0f)
        assertTrue(layers.aurora > 0 && layers.bloom > 0 && layers.flow > 0)
        assertEquals(0f, layers.motion, 0f)
    }
    @Test fun reactionOffUsesSteadyParametersNotVideoMetrics() {
        val a = ambientLayers(AmbientSignal(fallback, 0f, 0f, 0f), .5f, false, AmbientMotion.SUBTLE)
        val b = ambientLayers(AmbientSignal(fallback, 1f, 1f, 1f), .5f, false, AmbientMotion.SUBTLE)
        assertEquals(a, b)
    }
    @Test fun reflectedLightStrengthIsVividButBoundedAndZeroStillDisablesIt() {
        val vivid = AmbientSignal(fallback, 1f, 1f, 1f)
        val full = ambientLayers(vivid, 1f, true, AmbientMotion.NORMAL)
        assertEquals(.4f, full.aurora, .0001f)
        assertEquals(.34f, full.bloom, .0001f)
        assertEquals(.3f, full.flow, .0001f)
        val half = ambientLayers(vivid, .5f, true, AmbientMotion.NORMAL)
        assertEquals(full.aurora * .5f, half.aurora, .0001f)
        assertEquals(full.bloom * .5f, half.bloom, .0001f)
        val off = ambientLayers(vivid, 0f, true, AmbientMotion.OFF)
        assertEquals(0f, off.aurora + off.bloom + off.flow + off.motion, 0f)
    }
    @Test fun ambientVeilMatchesAcrossLibraryAndOtherPagesWithoutChangingCustomMedia() {
        for (mode in AppBackgroundMode.entries) {
            assertEquals(androidx.compose.ui.graphics.Color.Black, appBackgroundVeil(mode, false))
            val light = if (mode in listOf(AppBackgroundMode.DEFAULT, AppBackgroundMode.AMBIENT,
                    AppBackgroundMode.ADAPTIVE_DYNAMIC)) androidx.compose.ui.graphics.Color.White
                else androidx.compose.ui.graphics.Color.Black
            assertEquals(light, appBackgroundVeil(mode, true))
        }
    }
    @Test fun hiddenScrollingReducedMotionAndBatterySuspendAnimation() {
        fun run(v: Boolean = true, r: Boolean = true, s: Boolean = false,
            reduced: Boolean = false, power: Boolean = false, mode: AmbientMotion = AmbientMotion.SUBTLE) =
            ambientShouldAnimate(v, r, s, reduced, power, mode)
        assertTrue(run()); assertFalse(run(v = false)); assertFalse(run(r = false))
        assertFalse(run(s = true)); assertFalse(run(reduced = true))
        assertFalse(run(power = true)); assertFalse(run(mode = AmbientMotion.OFF))
    }
    @Test fun preferenceDefaultsAndUnknownValuesAreSafe() {
        assertEquals(AppBackgroundMode.CURRENT_VIDEO, UserPreferences().backgroundMode)
        assertEquals(.5f, ambientIntensityFromStorage(null), 0f)
        assertEquals(.5f, ambientIntensityFromStorage(Float.NaN), 0f)
        assertEquals(1f, ambientIntensityFromStorage(500f), 0f)
        assertEquals(AmbientMotion.SUBTLE, ambientMotionFromStorage("future-value"))
        assertEquals(AmbientMotion.OFF, ambientMotionFromStorage("OFF"))
        assertEquals(AppBackgroundMode.AMBIENT, AppBackgroundMode.valueOf("AMBIENT"))
    }
    @Test fun sharedSnapshotsPreservePaletteAndAreBounded() {
        val cache = AdaptiveAmbientCache()
        val warm = cache.update("audio", listOf(0xffdd4422.toInt()), fallback, 0)
        assertEquals(warm, cache.snapshot("audio", fallback))
        assertEquals(fallback, cache.snapshot("different", fallback).colors)
        repeat(10) { cache.update("file-$it", listOf(0xff334466.toInt()), fallback, it * 900L) }
        assertEquals(2, cache.size)
        assertEquals(fallback, cache.snapshot("audio", fallback).colors)
    }
    @Test fun staleVideoCannotOverrideNewAudioArtwork() {
        val oldVideo = listOf(0xffff0000.toInt())
        val audioArt = listOf(0xff0000ff.toInt())
        assertEquals(audioArt, ambientInputPixels("new-audio", "old-video", oldVideo, audioArt))
        assertEquals(oldVideo, ambientInputPixels("old-video", "old-video", oldVideo, audioArt))
        assertEquals(audioArt, ambientInputPixels(null, "old-video", oldVideo, audioArt))
        assertEquals(audioArt, ambientInputPixels("old-video", "old-video", emptyList(), audioArt))
    }
}
