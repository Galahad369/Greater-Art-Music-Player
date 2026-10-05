package com.local.listentomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.ui.components.GaTonalIconAction
import com.local.listentomusic.ui.theme.GaControl
import com.local.listentomusic.ui.theme.GaSpacing
import com.local.listentomusic.ui.theme.gaChromeColor
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
    val destinations = listOf(
        LibraryFamilyDestination(
            icon = Icons.Rounded.Layers,
            label = uiText(language, "Stack", "疊播"),
        ),
        LibraryFamilyDestination(
            icon = Icons.Rounded.LibraryMusic,
            label = uiText(language, "All songs", "所有歌曲"),
        ),
        LibraryFamilyDestination(
            icon = Icons.Rounded.Hub,
            label = uiText(language, "Nodes", "關聯圖"),
        ),
    )
    Surface(
        modifier = Modifier.fillMaxWidth()
            .inspectElement("LIBRARY_FAMILY_NAV", "Persistent Stack, All songs, Nodes navigation"),
        color = gaChromeColor(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = GaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEachIndexed { index, destination ->
                    val selected = abs(position - index) < .5f
                    IconButton(
                        onClick = { onPage(index) },
                        modifier = Modifier.weight(1f).height(GaControl.touchTarget)
                            .inspectElement("LIBRARY_FAMILY_NAV_$index", "Open ${destination.label}"),
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (selected) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (selected) 26.dp else 24.dp),
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

private data class LibraryFamilyDestination(
    val icon: ImageVector,
    val label: String,
)

/** Shared Library-family header: Stack and Nodes are alternate Library views, not separate apps. */
@Composable
internal fun LibraryPageHeader(
    title: String,
    subtitle: String,
    onLibrary: () -> Unit,
    elementName: String,
) {
    Row(
        Modifier.fillMaxWidth().height(80.dp).padding(horizontal = GaSpacing.lg)
            .inspectElement(elementName, "$title title and Library navigation"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GaTonalIconAction(
            icon = Icons.Rounded.ArrowBack,
            contentDescription = "Library",
            onClick = onLibrary,
            modifier = Modifier.inspectElement("${elementName}_LIBRARY_BUTTON", "Returns to Library"),
        )
        Spacer(Modifier.width(GaSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
