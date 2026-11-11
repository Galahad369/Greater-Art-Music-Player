package com.local.listentomusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import org.junit.Rule
import org.junit.Test

class NowPlayingPowerSavingNoticeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun powerSavingNoticeIsDismissibleAndDoesNotRepeatOnRecomposition() {
        val saving = mutableStateOf(false)
        val song = mutableStateOf("First song")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    androidx.compose.material3.Text(song.value)
                    NowPlayingPowerSavingNotice(saving.value, true, AppLanguage.ENGLISH)
                }
            }
        }
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
        compose.runOnIdle { saving.value = true }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText(POWER_SAVING_AMBIENT_MESSAGE).assertIsDisplayed()
        compose.onNodeWithTag("DISMISS_POWER_SAVING_NOTICE").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
        compose.runOnIdle { song.value = "Another song" }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
        compose.runOnIdle { saving.value = false }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { saving.value = true }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText(POWER_SAVING_AMBIENT_MESSAGE).assertIsDisplayed()
    }

    @Test fun hiddenOrNonSavingPlayerHasNoNoticeAndTurningSavingOffClearsIt() {
        val saving = mutableStateOf(true)
        val visible = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme { NowPlayingPowerSavingNotice(saving.value, visible.value, AppLanguage.ENGLISH) }
        }
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
        compose.runOnIdle { visible.value = true }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText(POWER_SAVING_AMBIENT_MESSAGE).assertIsDisplayed()
        compose.runOnIdle { saving.value = false }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
    }

    @Test fun noticeHidesWithPlayerAndDoesNotInterruptFullscreenReturn() {
        val visible = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme { NowPlayingPowerSavingNotice(true, visible.value, AppLanguage.ENGLISH) }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText(POWER_SAVING_AMBIENT_MESSAGE).assertIsDisplayed()
        compose.runOnIdle { visible.value = false }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
        compose.runOnIdle { visible.value = true }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
    }

    @Test fun longTranslatedNoticeFitsAndHasATappableCloseControl() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(240.dp)) { NowPlayingPowerSavingNotice(true, true, AppLanguage.GERMAN) }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText(uiText(AppLanguage.GERMAN, POWER_SAVING_AMBIENT_MESSAGE, ""))
            .assertIsDisplayed()
        compose.onNodeWithTag("DISMISS_POWER_SAVING_NOTICE").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("NOW_PLAYING_POWER_SAVING_NOTICE").assertDoesNotExist()
    }
}
