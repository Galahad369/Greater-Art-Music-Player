package com.local.listentomusic.diagnostics

import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real PackageManager/app-ops checks; the host sets/restores grants between processes. */
class AppTrustSnapshotDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun installedIdentityMatchesTheActualTestedPackage() {
        val snapshot = AppTrustSnapshot.read(context, null)
        assertEquals(BuildConfig.VERSION_NAME, snapshot.version)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), snapshot.code)
        assertEquals(BuildConfig.DEBUG, snapshot.debuggable)
        assertEquals(listOf(COMPATIBILITY_CERTIFICATE), snapshot.certificateSha256)
        assertTrue(snapshot.compatibilityIdentity)
        assertFalse(snapshot.internetDeclared)
        assertFalse(snapshot.selectedFolderGrantPresent)
    }

    @Test fun anUnselectedUriDoesNotInventAReadGrant() {
        val snapshot = AppTrustSnapshot.read(context, "content://com.example.ungranted/tree/absent")
        assertFalse(snapshot.selectedFolderGrantPresent)
    }

    @Test fun permissionsMatchTheExternallyConfiguredState() {
        val arguments = InstrumentationRegistry.getArguments()
        val storage = arguments.getString("expectedStorageGranted")
        val overlay = arguments.getString("expectedOverlayGranted")
        assumeTrue("Host must configure grants before instrumentation; revocation kills its process", storage != null && overlay != null)
        val snapshot = AppTrustSnapshot.read(context, null)
        assertEquals("All Files Access", requireNotNull(storage).toBooleanStrict(), snapshot.broadAccessGranted)
        assertEquals("Overlay access", requireNotNull(overlay).toBooleanStrict(), snapshot.overlayGranted)
    }
}
