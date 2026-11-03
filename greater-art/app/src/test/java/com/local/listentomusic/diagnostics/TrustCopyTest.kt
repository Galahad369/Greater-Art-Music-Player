package com.local.listentomusic.diagnostics

import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.ui.TrustCopy
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustCopyTest {
    @Test fun everyTrustStringExistsForEveryAppLanguage() {
        TrustCopy.entries.forEach { copy ->
            AppLanguage.entries.forEach { language -> assertTrue("$copy / $language", copy.text(language).isNotBlank()) }
        }
    }
}
