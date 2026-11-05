package com.local.listentomusic.ui

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.MediaSource
import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest

@Composable
fun DuplicateDialog(files: List<MediaFile>, currentPath: String?, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val mixLayers by com.local.listentomusic.playback.ParallelPlayback.layers.collectAsState()
    val stack by com.local.listentomusic.playback.StackPlayback.state.collectAsState()
    val protectedPaths = (mixLayers.map { com.local.listentomusic.model.sourceMediaPath(it.path) } +
        listOfNotNull(currentPath?.let { com.local.listentomusic.model.sourceMediaPath(it) }) +
        stack.slots.map { it.file.sourcePath }).toSet()
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<List<MediaFile>>?>(null) }
    var message by remember { mutableStateOf("Checking same-size files locally…") }
    val delete = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == android.app.Activity.RESULT_OK) { onChanged(); onDismiss() }
    }
    LaunchedEffect(Unit) {
        val (matches, skipped) = withContext(Dispatchers.IO) {
            val distinct = files.distinctBy { it.sourcePath }
            var unreadable = distinct.count { it.sizeBytes <= 0 }
            val matches = distinct.filter { it.sizeBytes > 0 }.groupBy { it.sizeBytes }.values.filter { it.size > 1 }.flatMap { candidates ->
                candidates.mapNotNull { media ->
                    ensureActive()
                    runCatching {
                        val digest = MessageDigest.getInstance("SHA-256")
                        val stream = when (val source = media.reference.source) {
                            is MediaSource.LocalFile -> File(source.value).inputStream()
                            is MediaSource.ContentUri -> requireNotNull(context.contentResolver.openInputStream(Uri.parse(source.value)))
                        }
                        stream.buffered().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                digest.update(buffer, 0, count)
                            }
                        }
                        digest.digest().joinToString("") { "%02x".format(it) } to media
                    }.getOrElse { if (it is CancellationException) throw it; unreadable++; null }
                }.groupBy({ it.first }, { it.second }).values.filter { it.size > 1 }
            }
            matches to unreadable
        }
        groups = matches
        message = if (skipped == 0) "Exact file matches. Review each path before deleting. Active tracks are protected."
            else "$skipped files could not be checked. Results only include files with known sizes that could be read."
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Duplicate files") }, text = {
        Column {
            Text(message)
            if (groups == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            else if (groups.orEmpty().isEmpty()) Text("No exact duplicates among checked files.")
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                groups.orEmpty().forEach { group ->
                    item { HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
                    group.forEach { file -> item {
                        Text(file.relativePath ?: file.sourcePath, style = MaterialTheme.typography.bodySmall)
                        if (file.reference.source is MediaSource.ContentUri) {
                            Text("Manage this folder file in Android Files.", style = MaterialTheme.typography.labelSmall)
                        } else TextButton(enabled = file.sourcePath !in protectedPaths, onClick = {
                            scope.launch {
                                if (Build.VERSION.SDK_INT < 30) { message = "System-confirmed deletion requires Android 11 or newer."; return@launch }
                                val uri = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val collection = if (file.kind == MediaKind.VIDEO)
                                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                        context.contentResolver.query(collection,
                                            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.VOLUME_NAME),
                                            "${MediaStore.MediaColumns.DATA} = ?", arrayOf(file.sourcePath), null)?.use { cursor ->
                                            if (!cursor.moveToFirst()) return@use null
                                            val volume = cursor.getString(1) ?: return@use null
                                            val typed = if (file.kind == MediaKind.VIDEO) MediaStore.Video.Media.getContentUri(volume)
                                                else MediaStore.Audio.Media.getContentUri(volume)
                                            ContentUris.withAppendedId(typed, cursor.getLong(0))
                                        }
                                    }.getOrNull()
                                }
                                if (uri == null) message = "Android has not indexed this file. Delete it using your Files app."
                                else runCatching {
                                    delete.launch(IntentSenderRequest.Builder(MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender).build())
                                }.onFailure { message = "Android could not open deletion confirmation. No file was deleted." }
                            }
                        }) { Text("Ask Android to delete", color = MaterialTheme.colorScheme.error) }
                    } }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
