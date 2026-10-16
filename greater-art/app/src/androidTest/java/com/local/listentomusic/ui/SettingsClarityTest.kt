package com.local.listentomusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.LocalPlaylist
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.ui.theme.GreaterArtTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsClarityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun choiceShowsCurrentValueAndCancelDoesNotWrite() {
        val selected = mutableStateOf("Dark")
        var writes = 0
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) {
            ChoiceSetting("Theme", "Choose an appearance", listOf("Dark", "Light"), selected.value,
                { it }, { selected.value = it; writes++ })
        } }
        compose.onNodeWithText("Dark").assertIsDisplayed()
        compose.onNodeWithText("Light").assertDoesNotExist()
        compose.onNodeWithText("Theme").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, writes) }
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Light").assertIsNotSelected().performClick()
        compose.onNodeWithText("Light").assertIsDisplayed()
        compose.runOnIdle { assertEquals("Light", selected.value); assertEquals(1, writes) }
    }

    @Test fun selectingCurrentChoiceStillInvokesItsExistingActionExactlyOnce() {
        var calls = 0
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) {
            ChoiceSetting("Background", "Custom file", listOf("Image", "Video"), "Image", { it }, { calls++ })
        } }
        compose.onNodeWithText("Background").performClick()
        compose.onNode(hasText("Image") and isSelected()).performClick()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test fun translatedDialogAndLargeFontKeepSingleAccessibleActions() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp)) then
                DeviceConfigurationOverride.FontScale(2f)) {
                GreaterArtTheme(ThemeMode.LIGHT) {
                    ChoiceSetting("文字字型", "選擇整個應用程式使用的字型。", listOf("系統", "等寬"), "系統",
                        { it }, {}, language = AppLanguage.TRADITIONAL_CHINESE)
                }
            }
        }
        compose.onNodeWithText("文字字型").assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("等寬").assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("取消").assertIsDisplayed().performClick()
    }

    @Test fun playlistActionsNoLongerSqueezeItsTitleAndRemain48Dp() {
        val name = "Very long playlist — 很長的播放清單名稱"
        var calls = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp)) then
                DeviceConfigurationOverride.FontScale(2f)) {
                GreaterArtTheme(ThemeMode.DARK) { Column {
                    SettingsPlaylistRow(LocalPlaylist(id = "test", name = name, paths = listOf("/one.mp3")),
                        AppLanguage.ENGLISH, { calls++ }, { calls++ }, { calls++ }, { calls++ })
                } }
            }
        }
        val titleBounds = compose.onNodeWithText(name).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        for (action in listOf("Play", "Share playlist", "Rename", "Delete")) {
            val button = compose.onNodeWithContentDescription("$action: $name")
                .assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            assertTrue(button.fetchSemanticsNode().boundsInRoot.top >= titleBounds.bottom)
            button.performClick()
        }
        compose.runOnIdle { assertEquals(4, calls) }
    }
}
