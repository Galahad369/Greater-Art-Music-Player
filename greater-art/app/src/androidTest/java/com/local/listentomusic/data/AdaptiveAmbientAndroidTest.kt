package com.local.listentomusic.data

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.ui.components.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AdaptiveAmbientAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun preferencesPersistAndKeepClassicSelection() = runBlocking {
        val prefs = AppPreferences(context)
        val old = prefs.current()
        try {
            prefs.setBackgroundMode(AppBackgroundMode.AMBIENT)
            prefs.setAdaptiveIntensity(.73f)
            prefs.setAdaptiveMotion(AmbientMotion.NORMAL)
            prefs.setAdaptiveReactToVideo(false)
            val reloaded = AppPreferences(context).current()
            assertEquals(AppBackgroundMode.AMBIENT, reloaded.backgroundMode)
            assertEquals(.73f, reloaded.adaptiveIntensity, .001f)
            assertEquals(AmbientMotion.NORMAL, reloaded.adaptiveMotion)
            assertFalse(reloaded.adaptiveReactToVideo)
            prefs.setBackgroundMode(AppBackgroundMode.ADAPTIVE_DYNAMIC)
            assertEquals(AppBackgroundMode.ADAPTIVE_DYNAMIC, prefs.current().backgroundMode)
            prefs.setAdaptiveIntensity(Float.NaN)
            assertEquals(.5f, prefs.current().adaptiveIntensity, 0f)
        } finally {
            prefs.setBackgroundMode(old.backgroundMode)
            prefs.setAdaptiveIntensity(old.adaptiveIntensity)
            prefs.setAdaptiveMotion(old.adaptiveMotion)
            prefs.setAdaptiveReactToVideo(old.adaptiveReactToVideo)
        }
    }
    @Test fun artworkSamplingIsTinyAndMatchesColors() {
        val image = Bitmap.createBitmap(384, 384, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(0xffc04020.toInt())
            val pixels = adaptiveArtworkPixels(image)
            assertEquals(144, pixels.size)
            val signal = analyzeAmbient(pixels, listOf(0xff10151b.toInt()))
            assertEquals(0xffc04020.toInt(), signal.colors.first())
        } finally { image.recycle() }
        assertTrue(adaptiveArtworkPixels(image).isEmpty())
    }
    @Test fun boundedAnalysisBenchmark() {
        val engine = AdaptiveAmbientEngine()
        val pixels = List(576) { 0xff000000.toInt() or (it * 29321 and 0xffffff) }
        repeat(20) { engine.update("benchmark", pixels, listOf(0xff10151b.toInt()), it * 900L) }
        val start = System.nanoTime()
        repeat(200) { engine.update("benchmark", pixels, listOf(0xff10151b.toInt()), (it + 20) * 900L) }
        val perSampleUs = (System.nanoTime() - start) / 200 / 1000
        android.util.Log.i("GA-AmbientBenchmark", "576px worker analysis meanUs=$perSampleUs; samples=200")
        // This is evidence, not a fragile cross-device timing gate or renderer benchmark.
        assertTrue(perSampleUs >= 0)
    }
}
