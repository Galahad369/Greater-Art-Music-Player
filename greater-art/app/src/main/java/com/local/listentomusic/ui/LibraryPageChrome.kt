package com.local.listentomusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
