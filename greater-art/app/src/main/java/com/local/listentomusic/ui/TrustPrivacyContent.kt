package com.local.listentomusic.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.diagnostics.AppTrustSnapshot
import com.local.listentomusic.diagnostics.CrashReports
import com.local.listentomusic.ui.components.GaDivider
import com.local.listentomusic.ui.theme.GaSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrustPrivacyContent(
    preferences: UserPreferences,
    onHistoryEnabled: (Boolean) -> Unit,
    onClearHistory: (() -> Unit)?,
    onFolders: () -> Unit,
    previewReport: () -> String = CrashReports::preview,
    clearLocalReport: () -> Unit = CrashReports::clear,
) {
    val context = LocalContext.current
    val language = preferences.appLanguage
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<AppTrustSnapshot?>(null) }
    var report by remember { mutableStateOf<String?>(null) }
    var clearReport by remember { mutableStateOf(false) }
    var clearHistory by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<TrustCopy?>(null) }
    fun copy(key: TrustCopy) = key.text(language)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(refresh, preferences.scopedMediaTreeUri) {
        snapshot = withContext(Dispatchers.IO) {
            try { AppTrustSnapshot.read(context, preferences.scopedMediaTreeUri) }
            catch (_: android.content.pm.PackageManager.NameNotFoundException) { null }
            catch (_: SecurityException) { null }
        }
    }
    Column(Modifier.fillMaxWidth().padding(GaSpacing.lg).inspectElement("TRUST_PRIVACY", "Installed build identity, permissions and private diagnostic controls"),
        verticalArrangement = Arrangement.spacedBy(GaSpacing.md)) {
        Text(copy(TrustCopy.TITLE), style = MaterialTheme.typography.titleLarge)
        val installed = snapshot
        if (installed == null) Text(copy(TrustCopy.UNAVAILABLE))
        else {
            TrustDetail(copy(TrustCopy.BUILD), "${installed.version} / ${installed.code} · ${copy(if (installed.debuggable) TrustCopy.DEBUG else TrustCopy.OPTIMIZED)}")
            Text(copy(if (installed.compatibilityIdentity) TrustCopy.COMPATIBILITY else TrustCopy.UNVERIFIED), style = MaterialTheme.typography.bodySmall)
            SelectionContainer { TrustDetail(copy(TrustCopy.CERTIFICATE), installed.certificateSha256.joinToString("\n").ifEmpty { "—" }) }
            TrustDetail(copy(TrustCopy.NETWORK), copy(if (installed.internetDeclared) TrustCopy.INTERNET_PRESENT else TrustCopy.NO_INTERNET))
            TrustDetail(copy(TrustCopy.ACCESS), if (installed.legacyStorageReadGranted != null)
                "${copy(TrustCopy.LEGACY_READ)} · ${copy(if (installed.legacyStorageReadGranted) TrustCopy.GRANTED else TrustCopy.DENIED)}"
                else if (installed.broadAccessDeclared)
                "${copy(TrustCopy.BROAD)} · ${copy(if (installed.broadAccessGranted) TrustCopy.GRANTED else TrustCopy.DENIED)}"
                else copy(TrustCopy.DENIED))
            TrustDetail(copy(TrustCopy.FOLDER), if (preferences.scopedMediaTreeUri == null) copy(TrustCopy.NO_FOLDER)
                else copy(if (installed.selectedFolderGrantPresent) TrustCopy.GRANTED else TrustCopy.DENIED))
            TrustDetail(copy(TrustCopy.OVERLAY), copy(if (installed.overlayGranted) TrustCopy.GRANTED else TrustCopy.DENIED))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm)) {
            OutlinedButton(onClick = {
                try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }
                catch (_: ActivityNotFoundException) { status = TrustCopy.FAILED }
                catch (_: SecurityException) { status = TrustCopy.FAILED }
            }) { Text(copy(TrustCopy.PERMISSIONS)) }
            TextButton(onClick = onFolders) { Text(copy(TrustCopy.FOLDERS)) }
        }
        Text(copy(TrustCopy.STORAGE), style = MaterialTheme.typography.bodySmall)
        GaDivider()
        SwitchSetting(copy(TrustCopy.HISTORY), copy(TrustCopy.HISTORY_NOTE), preferences.playHistoryEnabled, onHistoryEnabled)
        if (onClearHistory != null) TextButton(onClick = { clearHistory = true }) { Text(copy(TrustCopy.CLEAR_HISTORY)) }
        Text(copy(TrustCopy.DIAGNOSTICS), fontWeight = FontWeight.SemiBold)
        Text(copy(TrustCopy.DIAGNOSTICS_NOTE), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GaSpacing.sm)) {
            OutlinedButton(enabled = !busy, onClick = { report = previewReport() }) { Text(copy(TrustCopy.PREVIEW)) }
            TextButton(enabled = !busy, onClick = { clearReport = true }) { Text(copy(TrustCopy.CLEAR)) }
        }
        status?.let { Text(copy(it), style = MaterialTheme.typography.bodySmall) }
        Text(copy(TrustCopy.LIMITS), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    report?.let { text ->
        AlertDialog(onDismissRequest = { report = null }, title = { Text(copy(TrustCopy.PREVIEW)) },
            text = { SelectionContainer { Text(text.ifEmpty { copy(TrustCopy.EMPTY) }, Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = {
                try {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), copy(TrustCopy.SHARE)))
                    report = null
                } catch (_: ActivityNotFoundException) { status = TrustCopy.FAILED; report = null }
                catch (_: SecurityException) { status = TrustCopy.FAILED; report = null }
            }) { Text(copy(TrustCopy.SHARE)) } },
            dismissButton = { TextButton(onClick = { report = null }) { Text(uiText(language, "Cancel", "取消")) } })
    }
    if (clearReport) AlertDialog(onDismissRequest = { clearReport = false },
        title = { Text(copy(TrustCopy.CLEAR)) }, text = { Text(copy(TrustCopy.CONFIRM_CLEAR)) },
        confirmButton = { TextButton(onClick = {
            clearReport = false; busy = true
            scope.launch {
                try { withContext(Dispatchers.IO) { clearLocalReport() }; status = TrustCopy.CLEARED }
                catch (_: java.io.IOException) { status = TrustCopy.FAILED }
                finally { busy = false }
            }
        }) { Text(copy(TrustCopy.CLEAR)) } },
        dismissButton = { TextButton(onClick = { clearReport = false }) { Text(uiText(language, "Cancel", "取消")) } })
    if (clearHistory) AlertDialog(onDismissRequest = { clearHistory = false },
        title = { Text(copy(TrustCopy.CLEAR_HISTORY)) }, text = { Text(copy(TrustCopy.HISTORY_NOTE)) },
        confirmButton = { TextButton(onClick = { onClearHistory?.invoke(); clearHistory = false }) { Text(copy(TrustCopy.CLEAR_HISTORY)) } },
        dismissButton = { TextButton(onClick = { clearHistory = false }) { Text(uiText(language, "Cancel", "取消")) } })
}

@Composable
private fun TrustDetail(title: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GaSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
