package com.local.listentomusic.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.listentomusic.ui.design.DesignTokens

@Composable
fun GAText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = androidx.compose.ui.text.UNDEFINED,
    overflow: androidx.compose.ui.text.overflow.TextOverflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.Sp? = null,
    letterSpacing: androidx.compose.ui.unit.Sp? = null,
    lineHeight: androidx.compose.ui.unit.Sp? = null
) {
    androidx.compose.material3.Text(
        text = text,
        style = style.merge(TextStyle(
            fontWeight = fontWeight,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight
        )),
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

@Composable
fun GATitle(
    text: String,
    variant: GATitleVariant = GATitleVariant.H1,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = androidx.compose.ui.text.UNDEFINED,
    overflow: androidx.compose.ui.text.overflow.TextOverflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start
) {
    val style = when (variant) {
        GATitleVariant.H1 -> MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
        GATitleVariant.H2 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp)
        GATitleVariant.H3 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp)
        GATitleVariant.H4 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
        GATitleVariant.H5 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        GATitleVariant.H6 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
    androidx.compose.material3.Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

enum class GATitleVariant {
    H1, H2, H3, H4, H5, H6
}

@Composable
fun GABody(
    text: String,
    variant: GABodyVariant = GABodyVariant.MEDIUM,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = androidx.compose.ui.text.UNDEFINED,
    overflow: androidx.compose.ui.text.overflow.TextOverflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start
) {
    val style = when (variant) {
        GABodyVariant.LARGE -> MaterialTheme.typography.bodyLarge
        GABodyVariant.MEDIUM -> MaterialTheme.typography.bodyMedium
        GABodyVariant.SMALL -> MaterialTheme.typography.bodySmall
    }
    androidx.compose.material3.Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

enum class GABodyVariant {
    LARGE, MEDIUM, SMALL
}

@Composable
fun GALabel(
    text: String,
    variant: GALabelVariant = GALabelVariant.MEDIUM,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = androidx.compose.ui.text.UNDEFINED,
    overflow: androidx.compose.ui.text.overflow.TextOverflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
) {
    val style = when (variant) {
        GALabelVariant.LARGE -> MaterialTheme.typography.labelLarge
        GALabelVariant.MEDIUM -> MaterialTheme.typography.labelMedium
        GALabelVariant.SMALL -> MaterialTheme.typography.labelSmall
    }
    androidx.compose.material3.Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

enum class GALabelVariant {
    LARGE, MEDIUM, SMALL
}

@Composable
fun GACaption(
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = androidx.compose.ui.text.UNDEFINED,
    overflow: androidx.compose.ui.text.overflow.TextOverflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
) {
    androidx.compose.material3.Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}