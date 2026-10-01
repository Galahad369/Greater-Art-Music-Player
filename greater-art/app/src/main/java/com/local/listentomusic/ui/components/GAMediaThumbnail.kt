package com.local.listentomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GAMediaThumbnail(
    file: MediaFile,
    thumbnail: android.graphics.Bitmap?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 9.dp,
    showPlaceholder: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4.5f))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            androidx.compose.foundation.Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (showPlaceholder) {
            Icon(
                imageVector = if (file.kind == MediaKind.VIDEO) 
                    androidx.compose.material.icons.Icons.Rounded.Movie 
                else 
                    androidx.compose.material.icons.Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = androidx.compose.ui.Modifier.size(size / 2)
            )
        }
    }
}

@Composable
fun GAMediaRowThumbnail(
    file: MediaFile,
    thumbnail: android.graphics.Bitmap?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    showPlaceholder: Boolean = true
) {
    GAMediaThumbnail(
        file = file,
        thumbnail = thumbnail,
        modifier = modifier,
        size = size,
        cornerRadius = cornerRadius,
        showPlaceholder = showPlaceholder
    )
}

@Composable
fun GAMediaArtwork(
    file: MediaFile,
    artwork: android.graphics.Bitmap?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    showPlaceholder: Boolean = true
) {
    GAMediaThumbnail(
        file = file,
        thumbnail = artwork,
        modifier = modifier,
        size = size,
        cornerRadius = cornerRadius,
        showPlaceholder = showPlaceholder
    )
}