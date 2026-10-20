package com.local.listentomusic.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.ui.theme.GaControl
import com.local.listentomusic.ui.theme.GaInspectorPalette
import com.local.listentomusic.ui.theme.GaInspectorTheme

/** Local presentation only. The full copy payload is independent of the bounded summary. */
@Composable
internal fun DeveloperDiagnostics(
    summaryLines: List<String>,
    detailReport: String,
    regions: List<String>,
    warning: Boolean,
    inspector: UiInspectorState,
    onDisableDeveloperMode: () -> Unit,
    modifier: Modifier = Modifier,
    systemOverlay: Boolean = false,
    badgeAlignment: Alignment = if (systemOverlay) Alignment.TopCenter else Alignment.TopStart,
    language: AppLanguage = AppLanguage.ENGLISH,
) {
    val context = LocalContext.current
    var overlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                overlayAllowed = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var open by remember { mutableStateOf(false) }
    var showRegions by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    val accent = if (warning) GaInspectorPalette.error else GaInspectorPalette.accent
    fun text(key: String) = diagnosticsText(language, key)

    Box(Modifier.fillMaxSize()) {
        if (showRegions) Column(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 8.dp, top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            regions.forEach { region ->
                Text(region, Modifier.background(Color.Black.copy(alpha = .88f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White,
                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
        }
        Surface(
            onClick = {
                overlayAllowed = Settings.canDrawOverlays(context)
                showAdvanced = false
                showRegions = false
                open = true
            },
            modifier = modifier.align(badgeAlignment)
                .then(if (systemOverlay) Modifier else Modifier.statusBarsPadding())
                .padding(start = if (!systemOverlay && badgeAlignment == Alignment.TopStart) 18.dp else 0.dp,
                    end = if (!systemOverlay && badgeAlignment == Alignment.TopEnd) 18.dp else 0.dp,
                    top = if (systemOverlay) 2.dp else 16.dp)
                .sizeIn(minWidth = GaControl.touchTarget, minHeight = GaControl.touchTarget)
                .inspectElement("DEVELOPER_BUTTON", "Opens local diagnostics and element inspector"),
            shape = RoundedCornerShape(7.dp),
            color = Color.Black.copy(alpha = .88f),
            border = BorderStroke(1.dp, accent.copy(alpha = .9f)),
        ) {
            Text(if (warning) "DEV!" else "DEV",
                Modifier.padding(horizontal = 9.dp, vertical = 5.dp).wrapContentHeight(Alignment.CenterVertically),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = accent,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall)
        }
    }

    if (open) DiagnosticsDialog(
        onDismissRequest = { open = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        systemOverlay = systemOverlay,
    ) {
        // Bound the complete panel, not only its technical dump. Landscape and
        // large fonts must still allow reaching Copy/Pick/Disable by scrolling.
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            Modifier.padding(16.dp).widthIn(max = 420.dp).fillMaxWidth().heightIn(max = (maxHeight - 32.dp).coerceAtLeast(1.dp)),
            color = GaInspectorPalette.surface, contentColor = Color.White, shape = RoundedCornerShape(18.dp),
        ) {
            LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(text("Developer"), style = MaterialTheme.typography.titleLarge)
                            Text(text(if (warning) "Needs attention" else "OK · local only"), color = accent,
                                style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { open = false },
                            modifier = Modifier.sizeIn(minHeight = GaControl.touchTarget)) { Text(text("Close")) }
                    }
                }
                item {
                    SelectionContainer {
                        Text(summaryLines.take(8).joinToString("\n"), fontFamily = FontFamily.Monospace,
                            maxLines = 8, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    Text(text(if (overlayAllowed) "Floating permission: enabled" else "Floating permission: not granted"),
                        color = if (overlayAllowed) accent else GaInspectorPalette.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (!overlayAllowed) item {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }, modifier = Modifier.sizeIn(minHeight = GaControl.touchTarget)) { Text(text("Open permission settings")) }
                }
                item {
                    Button(onClick = { open = false; showRegions = false; inspector.arm() },
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = GaControl.touchTarget)) { Text(text("Pick an element")) }
                }
                inspector.selected?.let { selected -> item {
                    Text("${selected.label} · ${selected.bounds.width.toInt()}×${selected.bounds.height.toInt()} px",
                        fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = accent)
                } }
                item {
                    Button(onClick = {
                        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                            ClipData.newPlainText("Greater Art bug report", completeDeveloperReport(detailReport, regions)))
                    }, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = GaControl.touchTarget)) { Text(text("Copy bug report")) }
                }
                item {
                    TextButton(onClick = {
                        showAdvanced = !showAdvanced
                        if (!showAdvanced) showRegions = false
                    }, modifier = Modifier.sizeIn(minHeight = GaControl.touchTarget)) {
                        Text(text(if (showAdvanced) "Hide technical details" else "Technical details"))
                    }
                }
                if (showAdvanced) {
                    item {
                        Row(Modifier.fillMaxWidth().sizeIn(minHeight = GaControl.touchTarget)
                            .toggleable(showRegions, role = Role.Switch, onValueChange = { showRegions = it }),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(text("Show region IDs"), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Switch(showRegions, onCheckedChange = null)
                        }
                    }
                    item {
                        Surface(color = GaInspectorPalette.raised, contentColor = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .24f)), shape = RoundedCornerShape(12.dp)) {
                            SelectionContainer {
                                Text(detailReport, Modifier.fillMaxWidth().padding(14.dp),
                                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (showRegions) items(regions) { region ->
                        Text(region, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    OutlinedButton(onClick = {
                        open = false; showRegions = false
                        inspector.cancel(); inspector.clearSelection(); onDisableDeveloperMode()
                    }, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = GaControl.touchTarget)
                        .inspectElement("DISABLE_DEVELOPER_MODE_BUTTON", "Turns off local developer diagnostics")) {
                        Text(text("Turn off Developer Mode"))
                    }
                }
            }
        }
        }
    }
}

/** A Service has no Activity dialog token; keep the original report inside its window. */
@Composable
private fun DiagnosticsDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    systemOverlay: Boolean,
    content: @Composable () -> Unit,
) {
    if (systemOverlay) {
        androidx.activity.compose.BackHandler { onDismissRequest() }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .4f)), contentAlignment = Alignment.Center) {
            GaInspectorTheme(content)
        }
    } else Dialog(onDismissRequest = onDismissRequest, properties = properties) { GaInspectorTheme(content) }
}
