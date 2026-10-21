package com.local.listentomusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.ui.theme.GaMotion
import com.local.listentomusic.ui.theme.GreaterArtTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryRowWorkTest {
    @get:Rule val compose = createComposeRule()

    private val file = MediaFile("/owned/音楽.mp4", "音楽.mp4", 70 * 60_000L,
        1_048_576L, 1L, MediaKind.VIDEO)

    @Composable private fun Row(
        item: MediaFile = file,
        open: Boolean,
        toggle: () -> Unit,
        play: () -> Unit = {},
        add: () -> Unit = {},
        delete: () -> Unit = {},
    ) {
        MediaFileRow(item, isCurrent = true, isSelected = false, index = 0, itemCount = 1,
            dragEnabled = false, onMove = { _, _ -> }, onLoadThumbnail = { null },
            thumbnailBrush = Brush.linearGradient(listOf(Color.Black, Color.DarkGray)),
            rowSize = LibraryRowSize.SMALL, miniSize = LibraryRowSize.SMALL,
            showThumbnails = false, showFileDetails = true, language = AppLanguage.ENGLISH,
            actionsOpen = open, onPlay = play, onToggleActions = toggle,
            onAddToList = add, onDelete = delete, moreDescription = "Actions")
    }

    @Test fun closedActionsAreAbsentAndOpenActionsKeepFullTouchTargets() {
        val open = mutableStateOf(false)
        var added = 0
        var deleted = 0
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) {
            Row(open = open.value, toggle = { open.value = !open.value },
                add = { added++ }, delete = { deleted++ })
        } }
        compose.onNodeWithContentDescription("Add to list").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
        compose.onNodeWithContentDescription("Actions").performClick()
        compose.onNodeWithContentDescription("Add to list").assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription("Delete").assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, added); assertEquals(1, deleted) }
    }

    @Test fun closingKeepsUnderlayUntilAnimationEndsWithoutAcceptingHiddenActions() {
        val open = mutableStateOf(true)
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) {
            Row(open = open.value, toggle = { open.value = !open.value })
        } }
        compose.onNodeWithContentDescription("Delete").assertExists()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Actions").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Delete").assertExists().assertIsNotEnabled()
        compose.mainClock.advanceTimeBy(GaMotion.standardMs.toLong() + 64)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add to list").assertDoesNotExist()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithContentDescription("Actions").performClick()
        compose.onNodeWithContentDescription("Delete").assertIsEnabled()
    }

    @Test fun foregroundClosesActionsInsteadOfPlayingAndMetadataRefreshes() {
        val open = mutableStateOf(true)
        val item = mutableStateOf(file)
        var plays = 0
        compose.setContent { GreaterArtTheme(ThemeMode.LIGHT) {
            Row(item = item.value, open = open.value, toggle = { open.value = !open.value },
                play = { plays++ })
        } }
        compose.onNodeWithText("MP4  •  1.0 MB  •  1:10:00").assertIsDisplayed()
        compose.onNodeWithText("音楽").performClick()
        compose.runOnIdle { assertFalse(open.value); assertEquals(0, plays) }
        compose.onNodeWithText("音楽").performClick()
        compose.runOnIdle {
            assertEquals(1, plays)
            item.value = file.copy(name = "更新.flac", sizeBytes = 2048, durationMs = 61_000)
        }
        compose.onNodeWithText("更新").assertIsDisplayed()
        compose.onNodeWithText("FLAC  •  2.0 KB  •  1:01").assertIsDisplayed()
        compose.onNodeWithText("MP4  •  1.0 MB  •  1:10:00").assertDoesNotExist()
    }

    @Test fun inspectorDoesNotRetainClosedUnderlayRegions() {
        val open = mutableStateOf(false)
        val inspector = UiInspectorState()
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) {
            UiInspectorHost(true, inspector) {
                Row(open = open.value, toggle = { open.value = !open.value })
            }
        } }
        fun hasActions() = inspector.regions.values.any {
            it.label == "LIBRARY_ADD_TO_LIST" || it.label == "LIBRARY_DELETE_INLINE"
        }
        compose.runOnIdle { assertFalse(hasActions()) }
        compose.onNodeWithContentDescription("Actions").performClick()
        compose.runOnIdle { assertTrue(hasActions()) }
        compose.onNodeWithContentDescription("Actions").performClick()
        compose.runOnIdle { assertFalse(hasActions()) }
    }
}
