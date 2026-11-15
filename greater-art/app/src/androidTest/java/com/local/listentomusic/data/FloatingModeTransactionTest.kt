package com.local.listentomusic.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingModeTransactionTest {
    @Test fun queuedCyclesReadTheLatestStoredMode(): Unit = runBlocking {
        val prefs = AppPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = prefs.current().floatingWindowMode
        try {
            prefs.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
            List(5) { launch { prefs.cycleFloatingWindowMode() } }.joinAll()
            assertEquals(FloatingWindowMode.NONE, prefs.current().floatingWindowMode)
            // The explicit Settings setter still selects rather than cycles.
            prefs.setFloatingWindowMode(FloatingWindowMode.MINI_WINDOW)
            assertEquals(FloatingWindowMode.MINI_WINDOW, prefs.current().floatingWindowMode)
            prefs.setFloatingWindowMode(FloatingWindowMode.COMPACT)
            prefs.cycleFloatingWindowMode()
            assertEquals(FloatingWindowMode.NONE, prefs.current().floatingWindowMode)
        } finally {
            prefs.setFloatingWindowMode(original)
        }
    }
}
