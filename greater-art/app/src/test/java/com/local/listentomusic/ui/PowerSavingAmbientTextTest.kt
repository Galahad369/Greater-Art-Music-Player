package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PowerSavingAmbientTextTest {
    @Test fun noticeAndDismissActionAreBundledForEverySupportedLanguage() {
        for (language in AppLanguage.entries) {
            for (key in listOf(POWER_SAVING_AMBIENT_MESSAGE, "Dismiss power saving notice")) {
                val translated = additionalUiText(language, key)
                assertNotNull("Missing $language translation", translated)
                if (language != AppLanguage.ENGLISH) assertNotEquals(key, translated)
            }
        }
    }
}
