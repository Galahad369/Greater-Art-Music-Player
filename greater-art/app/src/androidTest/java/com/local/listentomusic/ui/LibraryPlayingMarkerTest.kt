package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.ColorTheme
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.ui.theme.appColorScheme
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryPlayingMarkerTest {
    @get:Rule val compose = createComposeRule()
    private val thumbnail = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    private val files = listOf(
        MediaFile("/owned/a.mp4", "A long video title — 孤独毒毒 and a second singer with a long filename.mp4",
            188000, 1_048_576, 1, MediaKind.VIDEO),
        MediaFile("/owned/b.mp3", "B long audio title — 同じ曲 and another singer with a long filename.mp3",
            189000, 2_097_152, 1, MediaKind.AUDIO),
    )
    private data class Configuration(
        val size: LibraryRowSize = LibraryRowSize.SMALL,
        val mini: LibraryRowSize = LibraryRowSize.SMALL,
        val thumbnails: Boolean = true,
        val details: Boolean = true,
        val selected: Boolean = false,
        val reorder: Boolean = false,
        val language: AppLanguage = AppLanguage.ENGLISH,
    )

    @Composable private fun Rows(config: Configuration, current: Int, playing: Boolean = true,
        onPlay: (Int) -> Unit = {}, onMove: (Int, Int) -> Unit = { _, _ -> }) {
        // Match Library's Scaffold content color, not the test host's default black.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                files.forEachIndexed { index, file ->
                    MediaFileRow(file, isCurrent = current == index, isPlaying = playing && current == index,
                        isSelected = config.selected && index == 1, index = index, itemCount = files.size,
                        dragEnabled = config.reorder, onMove = onMove,
                        onLoadThumbnail = { error("Seeded marker test must not decode artwork") },
                        onPeekThumbnail = { thumbnail },
                        thumbnailBrush = Brush.linearGradient(listOf(Color.Black, Color.DarkGray)),
                        rowSize = config.size, miniSize = config.mini, showThumbnails = config.thumbnails,
                        showFileDetails = config.details, language = config.language, actionsOpen = false,
                        onPlay = { onPlay(index) }, onToggleActions = {}, onAddToList = {}, onDelete = {},
                        moreDescription = "Actions $index")
                }
            }
        }
    }

    private fun bounds(tag: String): List<Rect> =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }

    @Test fun markerDoesNotMoveArtworkTitlesOrActionsAcrossAllSizesAndModes() {
        val config = mutableStateOf(Configuration())
        val current = mutableStateOf(-1)
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) { Rows(config.value, current.value) } }
        for (size in LibraryRowSize.entries) for (mini in LibraryRowSize.entries) {
            for (thumbnails in listOf(false, true)) for (details in listOf(false, true)) {
                compose.runOnIdle {
                    current.value = -1
                    config.value = Configuration(size, mini, thumbnails, details,
                        selected = !details, reorder = details)
                }
                compose.onNodeWithTag("LIBRARY_CURRENT_INDICATOR", useUnmergedTree = true).assertDoesNotExist()
                val titles = bounds("LIBRARY_MEDIA_TITLE")
                val artwork = bounds("LIBRARY_THUMBNAIL")
                val rows = bounds("LIBRARY_MEDIA_FOREGROUND")
                val actions = files.indices.map { compose.onNodeWithContentDescription("Actions $it").fetchSemanticsNode().boundsInRoot }
                assertEquals(if (thumbnails) 2 else 0, artwork.size)
                for (active in files.indices) {
                    compose.runOnIdle { current.value = active }
                    compose.onAllNodesWithTag("LIBRARY_CURRENT_INDICATOR", useUnmergedTree = true).assertCountEquals(1)
                    val markerNode = compose.onNodeWithTag("LIBRARY_CURRENT_INDICATOR", useUnmergedTree = true)
                    markerNode.assertWidthIsEqualTo(if (thumbnails) 24.dp else 20.dp)
                        .assertHeightIsEqualTo(if (thumbnails) 24.dp else 20.dp)
                    compose.onNodeWithTag("LIBRARY_CURRENT_PLAY_SYMBOL", useUnmergedTree = true)
                        .assertWidthIsEqualTo(16.dp).assertHeightIsEqualTo(16.dp)
                    assertEquals("Title bounds changed: ${config.value}", titles, bounds("LIBRARY_MEDIA_TITLE"))
                    assertEquals("Artwork bounds changed: ${config.value}", artwork, bounds("LIBRARY_THUMBNAIL"))
                    assertEquals("Row bounds changed: ${config.value}", rows, bounds("LIBRARY_MEDIA_FOREGROUND"))
                    assertEquals("Action bounds changed: ${config.value}", actions,
                        files.indices.map { compose.onNodeWithContentDescription("Actions $it").fetchSemanticsNode().boundsInRoot })
                    val marker = markerNode.fetchSemanticsNode().boundsInRoot
                    assertTrue("Badge escaped row", marker.left >= rows[active].left)
                    assertTrue("Badge overlaps title", marker.right < titles[active].left)
                    assertTrue(marker.center.y in rows[active].top..rows[active].bottom)
                    if (thumbnails) {
                        assertTrue("Badge is not attached to artwork", marker.left < artwork[active].left && marker.right > artwork[active].left)
                    }
                }
            }
        }
    }

    @Test fun currentRowHasTranslatedStateAndPausedMarkerDoesNotSuggestPlaying() {
        val config = mutableStateOf(Configuration(thumbnails = false, selected = true))
        val playing = mutableStateOf(true)
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) { Rows(config.value, 0, playing.value) } }
        val labels = mapOf(
            AppLanguage.ENGLISH to ("Now playing" to "Paused"),
            AppLanguage.TRADITIONAL_CHINESE to ("正在播放" to "已暫停"),
            AppLanguage.CANTONESE to ("播放緊" to "暫停咗"),
            AppLanguage.JAPANESE to ("再生中" to "一時停止"),
            AppLanguage.GERMAN to ("Wiedergabe läuft" to "Pausiert"),
            AppLanguage.FRENCH to ("Lecture en cours" to "En pause"),
        )
        for ((language, expected) in labels) for (running in listOf(true, false)) {
            compose.runOnIdle { config.value = config.value.copy(language = language); playing.value = running }
            val rows = compose.onAllNodesWithTag("LIBRARY_MEDIA_FOREGROUND", useUnmergedTree = true).fetchSemanticsNodes()
            assertEquals(if (running) expected.first else expected.second,
                rows[0].config[SemanticsProperties.StateDescription])
            assertTrue("Selection must not imply current playback", !rows[1].config.contains(SemanticsProperties.StateDescription))
            compose.onAllNodesWithTag("LIBRARY_CURRENT_INDICATOR", useUnmergedTree = true).assertCountEquals(1)
            if (running) {
                compose.onNodeWithTag("LIBRARY_CURRENT_PLAY_SYMBOL", useUnmergedTree = true).assertIsDisplayed()
                compose.onNodeWithTag("LIBRARY_CURRENT_PAUSED_SYMBOL", useUnmergedTree = true).assertDoesNotExist()
            } else {
                compose.onNodeWithTag("LIBRARY_CURRENT_PAUSED_SYMBOL", useUnmergedTree = true)
                    .assertIsDisplayed().assertWidthIsEqualTo(8.dp).assertHeightIsEqualTo(8.dp)
                compose.onNodeWithTag("LIBRARY_CURRENT_PLAY_SYMBOL", useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }

    @Test fun badgeTapsUseTheExistingRowActionOnceAndReorderStillWorks() {
        val config = mutableStateOf(Configuration())
        var plays = 0
        val moves = mutableListOf<Pair<Int, Int>>()
        compose.setContent { GreaterArtTheme(ThemeMode.DARK) {
            Rows(config.value, 0, onPlay = { plays++ }, onMove = { from, to -> moves += from to to })
        } }
        for (thumbnails in listOf(true, false)) {
            compose.runOnIdle { config.value = config.value.copy(thumbnails = thumbnails) }
            val badge = compose.onNodeWithTag("LIBRARY_CURRENT_INDICATOR", useUnmergedTree = true)
            assertTrue(!badge.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
            badge.performTouchInput { click(center) }
        }
        compose.runOnIdle { assertEquals(2, plays); config.value = config.value.copy(reorder = true) }
        val distance = 80f * InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        compose.onAllNodesWithTag("LIBRARY_MEDIA_FOREGROUND", useUnmergedTree = true)[0].performTouchInput {
            down(center)
            advanceEventTime(650)
            moveTo(center + Offset(0f, distance))
            up()
        }
        compose.runOnIdle { assertEquals(listOf(0 to 1), moves); assertEquals(2, plays) }
    }

    @Test fun badgePaletteRemainsOpaqueAndReadableInLightDarkOledAndHighContrast() {
        val theme = mutableStateOf(ThemeMode.DARK to ColorTheme.FOREST)
        compose.setContent { GreaterArtTheme(theme.value.first, theme.value.second) { Rows(Configuration(), 0) } }
        for (palette in ColorTheme.entries) for (dark in listOf(false, true)) {
            val colors = appColorScheme(palette, dark)
            val a = colors.primary.luminance(); val b = colors.onPrimary.luminance()
            assertEquals(1f, colors.primary.alpha, 0f)
            assertTrue("$palette dark=$dark", (maxOf(a, b) + .05f) / (minOf(a, b) + .05f) >= 3f)
        }
        for (choice in listOf(ThemeMode.LIGHT to ColorTheme.FOREST, ThemeMode.DARK to ColorTheme.FOREST,
            ThemeMode.DARK to ColorTheme.VOID, ThemeMode.DARK to ColorTheme.HIGH_CONTRAST)) {
            compose.runOnIdle { theme.value = choice }
            compose.onNodeWithTag("LIBRARY_CURRENT_INDICATOR", useUnmergedTree = true).assertIsDisplayed()
            captureIfRequested("${choice.first}-${choice.second}")
        }
    }

    private fun captureIfRequested(label: String) {
        if (InstrumentationRegistry.getArguments().getString("libraryMarkerScreenshots") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val target = File(requireNotNull(context.getExternalFilesDir(null)), "library-marker-$label-${UUID.randomUUID()}.png")
        target.outputStream().use { output ->
            assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        Log.i("LibraryMarkerTest", "SCREENSHOT ${target.absolutePath}")
    }
}
