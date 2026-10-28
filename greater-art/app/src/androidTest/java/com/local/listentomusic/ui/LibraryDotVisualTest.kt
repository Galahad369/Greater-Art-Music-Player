package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.LibraryStatus
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.LibraryRowSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/** Full Activity screenshots with discovered local media, not placeholder rows. */
class LibraryDotVisualTest {
    @Test fun captureRealLibraryMatrix() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = AppPreferences(context)
        val old = prefs.current()
        var scenario: ActivityScenario<MainActivity>? = null
        try {
            prefs.setBackgroundMode(AppBackgroundMode.DEFAULT)
            prefs.setDeveloperMode(false)
            scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
            val activity = scenario
            lateinit var model: MainViewModel
            activity.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
            withTimeout(30_000) {
                while (model.library.value.status != LibraryStatus.READY || model.library.value.files.size < 5 || model.controller.value == null) delay(100)
            }
            val media = model.library.value.files[1]
            activity.onActivity { model.play(media) }
            withTimeout(20_000) { while (!model.playback.value.isPlaying) delay(100) }
            for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                prefs.setThemeMode(theme)
                for (size in LibraryRowSize.entries) for (thumbnails in listOf(true, false)) {
                    prefs.setLibraryRowSize(size)
                    prefs.setShowThumbnails(thumbnails)
                    withTimeout(10_000) {
                        while (model.settings.value.let { it.themeMode != theme || it.libraryRowSize != size || it.showThumbnails != thumbnails }) delay(100)
                    }
                    for (playing in listOf(true, false)) {
                        activity.onActivity { model.controller.value!!.let { if (playing) it.play() else it.pause() } }
                        withTimeout(10_000) { while (model.playback.value.isPlaying != playing) delay(100) }
                        delay(800)
                        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                        val file = File(requireNotNull(context.getExternalFilesDir(null)),
                            "dot-real-$theme-$size-$thumbnails-$playing-${UUID.randomUUID()}.png")
                        file.outputStream().use { assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                        screenshot.recycle()
                        Log.i("LibraryDotVisual", "SCREENSHOT ${file.absolutePath}")
                    }
                }
            }
        } finally {
            scenario?.onActivity { ViewModelProvider(it)[MainViewModel::class.java].controller.value?.let { player -> player.pause(); player.clearMediaItems() } }
            scenario?.close()
            prefs.setThemeMode(old.themeMode)
            prefs.setLibraryRowSize(old.libraryRowSize)
            prefs.setShowThumbnails(old.showThumbnails)
            prefs.setBackgroundMode(old.backgroundMode)
            prefs.setDeveloperMode(old.developerMode)
            prefs.savePlayback(old.lastPath, old.lastPositionMs, old.playbackSpeed, old.repeatMode)
        }
    }
}
