package com.local.listentomusic.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Explicit, local-only diagnostics. It reports state supplied by the app and never
 * introspects private Compose internals or sends data anywhere.
 */
@Composable
internal fun DeveloperDiagnostics(
    report: String,
    regions: List<String>,
    warning: Boolean,
    inspector: UiInspectorState,
    modifier: Modifier = Modifier,
    systemOverlay: Boolean = false,
) {
    val context = LocalContext.current
    var overlayAllowed by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var open by remember { mutableStateOf(false) }
    var showRegions by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    val accent = if (warning) Color(0xFFFF5C68) else Color(0xFF75EBD4)
    val summary = remember(report) {
        report.lineSequence().filter {
            it.startsWith("screen=") || it.startsWith("playing=") ||
                it.startsWith("playerState=") || it.startsWith("warnings=")
        }.toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showRegions) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                regions.forEach { region ->
                    Text(
                        text = region,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Surface(
            onClick = {
                overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                open = true
            },
            modifier = modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                .inspectElement("DEVELOPER_BUTTON", "Opens local diagnostics and element inspector"),
            shape = RoundedCornerShape(7.dp),
            color = Color.Black.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.9f)),
        ) {
            Text(
                if (warning) "DEV!" else "DEV",
                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    if (open) {
        DiagnosticsDialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            systemOverlay = systemOverlay,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp).widthIn(max = 420.dp),
                color = Color(0xFF080C0D),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Inspector", style = MaterialTheme.typography.titleLarge)
                            Text(if (warning) "Needs attention" else "Running locally", color = accent, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { open = false }) { Text("CLOSE") }
                    }
                    summary.forEach { line ->
                        Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (overlayAllowed) "Floating window permission: enabled"
                        else "Floating window permission: OFF — Mini and Now Playing cannot open",
                        color = if (overlayAllowed) accent else Color(0xFFFF5C68),
                        style = MaterialTheme.typography.bodySmall)
                    if (!overlayAllowed) TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"))) }
                    }) { Text("Open floating window permission") }
                    Button(
                        onClick = { open = false; inspector.arm() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Pick an element") }
                    inspector.selected?.let { selected ->
                        Text("${selected.label} · ${selected.bounds.width.toInt()}×${selected.bounds.height.toInt()} px",
                            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                    TextButton(onClick = { showAdvanced = !showAdvanced; if (!showAdvanced) showRegions = false }) {
                        Text(if (showAdvanced) "Hide technical details" else "Technical details")
                    }
                    if (showAdvanced) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Show region IDs", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = showRegions, onCheckedChange = { showRegions = it })
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF141A1C),
                            contentColor = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp).padding(14.dp)) {
                                item {
                                    SelectionContainer {
                                        Text(report, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                items(regions) { region ->
                                    Text("• $region", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val payload = buildString {
                                appendLine(report)
                                appendLine("ACTIVE REGIONS")
                                regions.forEach { appendLine("- $it") }
                            }
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Greater Art bug report", payload))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy bug report") }
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
            content()
        }
    } else Dialog(onDismissRequest = onDismissRequest, properties = properties, content = content)
}
