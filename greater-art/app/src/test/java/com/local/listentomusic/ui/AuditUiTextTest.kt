package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class AuditUiTextTest {
    @Test fun allNewFeedbackHasTranslationsInEverySupportedLocale() {
        val messages = listOf("Could not update document access. Please retry.",
            "Use a relative folder path without . or .. or :", "Clearing cache…",
            "Could not clear cache. Please retry.", "Paused", "Could not read playlist",
            "No matching files in playlist", "Playlist imported", "Could not import playlist",
            "Playlist exported", "Could not export playlist")
        AppLanguage.entries.filter { it != AppLanguage.ENGLISH }.forEach { language ->
            messages.forEach { message ->
                val translated = auditUiText(language, message)
                assertNotNull("$language: $message", translated)
                assertNotEquals("$language: $message", message, translated)
            }
        }
    }
}
