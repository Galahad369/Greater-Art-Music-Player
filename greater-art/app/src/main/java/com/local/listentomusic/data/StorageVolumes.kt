package com.local.listentomusic.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager

/**
 * Storage volume roots named the way ExternalStorageProvider names them ("primary" or the volume UUID), so stored raw
 * paths can be mapped to documents by [documentIdForLegacyPath]. API 30+ reads each volume's directory from
 * StorageManager; on older releases only the primary volume can be mapped. Not wired into a production flow yet.
 */
internal fun storageVolumeRoots(context: Context): List<StorageVolumeRoot> {
    val manager = context.getSystemService(StorageManager::class.java) ?: return primaryVolumeOnly()
    return manager.storageVolumes.mapNotNull { volume ->
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) volume.directory else null
        val path = directory?.path ?: if (volume.isPrimary) Environment.getExternalStorageDirectory().path else return@mapNotNull null
        val id = if (volume.isPrimary) "primary" else volume.uuid ?: return@mapNotNull null
        val roots = if (volume.isPrimary) listOf(path, "/sdcard", "/storage/self/primary") else listOf(path)
        StorageVolumeRoot(id, roots.distinct())
    }.ifEmpty { primaryVolumeOnly() }
}

private fun primaryVolumeOnly() = listOf(
    StorageVolumeRoot("primary", listOf(Environment.getExternalStorageDirectory().path, "/sdcard", "/storage/self/primary").distinct()),
)
