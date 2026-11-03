package com.local.listentomusic.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTrustSnapshotTest {
    private fun snapshot(certificates: List<String>) = AppTrustSnapshot(
        version = "1.21.94", code = 317, debuggable = false, internetDeclared = false,
        broadAccessDeclared = true, broadAccessGranted = false, legacyStorageReadGranted = null,
        overlayGranted = false, selectedFolderGrantPresent = false, certificateSha256 = certificates,
    )

    @Test fun onlyTheExactSingleCompatibilitySignerGetsThatLabel() {
        assertTrue(snapshot(listOf(COMPATIBILITY_CERTIFICATE)).compatibilityIdentity)
        assertFalse(snapshot(emptyList()).compatibilityIdentity)
        assertFalse(snapshot(listOf("a".repeat(64))).compatibilityIdentity)
        assertFalse(snapshot(listOf(COMPATIBILITY_CERTIFICATE, "a".repeat(64))).compatibilityIdentity)
    }

    @Test fun certificateIdentityDoesNotImplyRuntimePermissionsOrReleaseSafety() {
        val installed = snapshot(listOf(COMPATIBILITY_CERTIFICATE))
        assertFalse(installed.broadAccessGranted)
        assertFalse(installed.overlayGranted)
        assertFalse(installed.selectedFolderGrantPresent)
        assertTrue(installed.broadAccessDeclared)
    }
}
