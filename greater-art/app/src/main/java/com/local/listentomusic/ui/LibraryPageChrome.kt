package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import kotlin.math.abs

/**
 * Persistent navigation for the Library family. The pager is the source of truth:
 * Stack = 0, All songs = 1, Nodes = 2. The indicator follows drag/fling progress
 * continuously so navigation, page motion and the shared wallpaper stay spatially aligned.
 */
@Composable
internal fun LibraryFamilyNavigationBar(
    currentPage: Int,
    pageOffsetFraction: Float,
    language: AppLanguage,
    onPage: (Int) -> Unit,
) {
    val position = libraryPagerNavigationPosition(currentPage, pageOffsetFraction)
    val labels = listOf(
        uiText(language, "Stack", "疊播"),
        uiText(language, "All songs", "所有歌曲"),
        uiText(language, "Nodes", "關聯圖"),
    )
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
            .inspectElement("LIBRARY_FAMILY_NAV", "Persistent Stack, All songs, Nodes navigation"),
        color = MaterialTheme.colorScheme.background.copy(alpha = .50f),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.forEachIndexed { index, label ->
                    val selected = abs(position - index) < .5f
                    TextButton(
                        onClick = { onPage(index) },
                        modifier = Modifier.weight(1f).height(42.dp)
                            .inspectElement("LIBRARY_FAMILY_NAV_$index", "Open $label"),
                    ) {
                        Text(
                            label,
                            color = if (selected) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().height(3.dp)) {
                val segmentWidth = maxWidth / 3
                Box(
                    Modifier
                        .offset(x = segmentWidth * position)
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 18.dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(50)),
                )
            }
        }
    }
}

/** Shared Library-family header: Stack and Nodes are alternate Library views, not separate apps. */
@Composable
internal fun LibraryPageHeader(
    title: String,
    subtitle: String,
    onLibrary: () -> Unit,
    elementName: String,
) {
    Row(
        Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 14.dp)
            .inspectElement(elementName, "$title title and Library navigation"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onLibrary,
            modifier = Modifier.size(40.dp).inspectElement("${elementName}_LIBRARY_BUTTON", "Returns to Library"),
        ) { Icon(Icons.Rounded.ArrowBack, "Library", Modifier.size(21.dp)) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
