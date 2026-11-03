package com.local.listentomusic.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.security.MessageDigest

internal const val COMPATIBILITY_CERTIFICATE = "9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf"

internal data class AppTrustSnapshot(
    val version: String,
    val code: Long,
    val debuggable: Boolean,
    val internetDeclared: Boolean,
    val broadAccessDeclared: Boolean,
    val broadAccessGranted: Boolean,
    val legacyStorageReadGranted: Boolean?,
    val overlayGranted: Boolean,
    val selectedFolderGrantPresent: Boolean,
    val certificateSha256: List<String>,
) {
    val compatibilityIdentity: Boolean get() = certificateSha256.size == 1 && certificateSha256[0] == COMPATIBILITY_CERTIFICATE

    companion object {
        @Suppress("DEPRECATION")
        fun read(context: Context, selectedTree: String?): AppTrustSnapshot {
            val flags = PackageManager.GET_PERMISSIONS or if (Build.VERSION.SDK_INT >= 28)
                PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            val info = context.packageManager.getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            val permissions = info.requestedPermissions.orEmpty().toSet()
            return AppTrustSnapshot(
                info.versionName ?: "unknown", if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
                Manifest.permission.INTERNET in permissions,
                // Inspect the manifest string even on API26–29, where the grant is not supported.
                "android.permission.MANAGE_EXTERNAL_STORAGE" in permissions,
                Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager(),
                if (Build.VERSION.SDK_INT < 30) context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED else null,
                Settings.canDrawOverlays(context),
                selectedTree != null && context.contentResolver.persistedUriPermissions.any { it.uri.toString() == selectedTree && it.isReadPermission },
                signatures.orEmpty().map { signature ->
                    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
                }.sorted(),
            )
        }
    }
}
