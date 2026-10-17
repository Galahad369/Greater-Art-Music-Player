package com.local.listentomusic.ui

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.MainActivity
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Uses production navigation and persisted preferences; never resets or rewrites user lists. */
class SettingsNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun categoryJourneyThemePersistenceLanguageAndBackStayPredictable(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = AppPreferences(context)
        val old = preferences.current()
        preferences.setAppLanguage(AppLanguage.ENGLISH)
        preferences.setThemeMode(ThemeMode.DARK)
        val main = ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        try {
            compose.onNodeWithContentDescription("Settings").performClick()
            for (section in SettingsSection.entries) {
                compose.onNodeWithText(section.title(AppLanguage.ENGLISH)).performScrollTo().assertIsDisplayed()
            }
            compose.onNodeWithText("Tap to expand").assertDoesNotExist()
            capture("index")
            compose.onNodeWithText("Language & appearance").performScrollTo().performClick()
            compose.onNodeWithText("Theme").assertIsDisplayed().performClick()
            compose.onNodeWithText("Light").performClick()
            compose.waitUntil(10_000) { runBlocking { preferences.current().themeMode == ThemeMode.LIGHT } }
            compose.onNodeWithText("Light").assertIsDisplayed()
            capture("appearance-light")
            compose.onNodeWithText("Language").performScrollTo().performClick()
            compose.onNodeWithText("日本語").performClick()
            compose.waitUntil(10_000) { runBlocking { preferences.current().appLanguage == AppLanguage.JAPANESE } }
            compose.onNodeWithText(SettingsSection.APPEARANCE.title(AppLanguage.JAPANESE)).assertIsDisplayed()
            compose.onNodeWithContentDescription(uiText(AppLanguage.JAPANESE, "Back", "返回")).performClick()
            compose.onNodeWithText(SettingsSection.FOLDERS.title(AppLanguage.JAPANESE)).performScrollTo().assertIsDisplayed()
            preferences.setAppLanguage(AppLanguage.ENGLISH)
            compose.onNodeWithText("Playback").performScrollTo().performClick()
            compose.onNodeWithText("ReplayGain").assertIsDisplayed()
            compose.onNodeWithText("Mini window size").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Library & lists").performScrollTo().performClick()
            compose.onNodeWithText("Library row size").assertIsDisplayed()
            compose.onNodeWithText("Show thumbnails").assertIsDisplayed()
            capture("library")
            // Android Back returns to the category list, not out of Settings.
            main.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Media folders").performScrollTo().performClick()
            compose.onNodeWithText("Choose folder").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Privacy & data").performScrollTo().performClick()
            compose.onNodeWithText("Reset app settings").performScrollTo().performClick()
            compose.onNodeWithText("Keep settings").performClick()
            assertEquals(ThemeMode.LIGHT, preferences.current().themeMode)
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Developer").performScrollTo().performClick()
            compose.onNodeWithText("Developer diagnostics").assertIsDisplayed()
        } finally {
            main.close()
            preferences.setAppLanguage(old.appLanguage)
            preferences.setThemeMode(old.themeMode)
        }
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(instrumentation.targetContext.getExternalFilesDir(null), "settings-$name.png")
                .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
}
