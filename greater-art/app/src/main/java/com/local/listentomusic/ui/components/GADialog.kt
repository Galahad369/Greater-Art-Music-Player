package com.local.listentomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.listentomusic.ui.design.DesignTokens

@Composable
fun GAAlertDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    text: String? = null,
    confirmButton: GADialogButton? = null,
    dismissButton: GADialogButton? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { title?.let { Text(text = it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp)) } },
        text = { text?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        confirmButton = { confirmButton?.let { btn ->
            Button(
                onClick = {
                    btn.onClick()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = btn.text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            }
        } },
        dismissButton = { dismissButton?.let { btn ->
            androidx.compose.material3.TextButton(
                onClick = {
                    btn.onClick()
                    onDismiss()
                },
                colors = androidx.compose.material3.TextButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = btn.text, style = MaterialTheme.typography.labelLarge)
            }
        } },
        modifier = androidx.compose.ui.Modifier.padding(24.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

data class GADialogButton(
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun GAConfirmDialog(
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    destructive: Boolean = false
) {
    GAAlertDialog(
        onDismiss = onDismiss,
        title = title,
        text = message,
        confirmButton = GADialogButton(
            text = confirmText,
            onClick = { /* handled by dismiss */ }
        ) {
            // The actual click is handled by the dialog's onDismiss
        }.also { it.onClick = { onDismiss(); it.onClick() } },
        dismissButton = GADialogButton(
            text = dismissText,
            onClick = onDismiss
        )
    )
}

@Composable
fun GABottomSheetDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    content: @Composable () -> Unit
) {
    // Bottom sheet implementation would go here
    // Using a simple column for now
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            title?.let {
                Text(text = it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            }
            content()
        }
    }
}

@Composable
fun GAChoiceDialog(
    onDismiss: () -> Unit,
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) },
        text = { },
        confirmButton = { },
        dismissButton = { },
        modifier = Modifier.padding(24.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        // Custom content for choice list
        Column(Modifier.padding(24.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .clickable { 
                            onItemClick(index)
                            onDismiss()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}