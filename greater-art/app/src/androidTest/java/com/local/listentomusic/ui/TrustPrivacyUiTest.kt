package com.local.listentomusic.ui

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.local.listentomusic.data.UserPreferences
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Real Compose controls; diagnostic actions use private fixtures, not the user's report. */
class TrustPrivacyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun installedIdentityAndFolderNavigationAreAccessible() {
        val folders = AtomicInteger()
        show(onFolders = { folders.incrementAndGet() })
        compose.waitUntil(5000) { compose.onAllNodesWithText("Installed build").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Installed build").assertIsDisplayed()
        capture("identity")
        compose.onNodeWithText("Media folders").performScrollTo().performClick()
        assertEquals(1, folders.get())
    }

    @Test fun emptyReportCannotOpenSharing() {
        show()
        compose.onNodeWithText("Preview report").performScrollTo().performClick()
        compose.onNodeWithText("No exportable local report.").assertIsDisplayed()
        compose.onNodeWithText("Open sharing").assertIsNotEnabled()
        capture("empty-preview", dialog = true)
    }

    @Test fun clearingDiagnosticsRequiresConfirmationAndDoesNotClearHistory() {
        val cleared = AtomicInteger()
        val history = AtomicInteger()
        show(onClear = { cleared.incrementAndGet() }, onClearHistory = { history.incrementAndGet() })
        compose.onNodeWithText("Clear local report").performScrollTo().performClick()
        assertEquals(0, cleared.get())
        compose.onNodeWithText("Remove only the local diagnostic report? Your media and preferences stay unchanged.").assertIsDisplayed()
        // The dialog and underlying button have the same label; the last match is the dialog action.
        compose.onAllNodesWithText("Clear local report").onLast().performClick()
        compose.waitUntil(3000) { cleared.get() == 1 }
        assertEquals(0, history.get())
    }

    @Test fun returningFromAndroidSettingsRefreshesPermissionState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }
        assumeTrue("Host owns permission setup/restoration outside the app process",
            InstrumentationRegistry.getArguments().getString("permissionReturnFixture") == "true")
        assertFalse(com.local.listentomusic.diagnostics.AppTrustSnapshot.read(instrumentation.targetContext, null).broadAccessGranted)
        show()
        val denied = "All Files Access is declared by this build · Not granted"
        compose.waitUntil(5000) { compose.onAllNodesWithText(denied).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Manage Android permissions").performScrollTo().performClick()
        compose.waitUntil(5000) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            root?.packageName == "com.android.settings" &&
                root.findAccessibilityNodeInfosByText(instrumentation.targetContext.applicationInfo
                    .loadLabel(instrumentation.targetContext.packageManager).toString()).isNotEmpty()
        }
        shell("appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
        // Settings is a different process; send Android Back instead of looking for an Espresso app root.
        shell("input keyevent KEYCODE_BACK")
        compose.waitUntil(5000) {
            shell("dumpsys activity activities").lineSequence().any { it.contains("ResumedActivity") && it.contains(packageName) }
        }
        val granted = "All Files Access is declared by this build · Granted"
        compose.waitUntil(5000) { compose.onAllNodesWithText(granted).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(granted).performScrollTo().assertIsDisplayed()
        capture("permission-return")
    }

    private fun capture(label: String, dialog: Boolean = false) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "trust-privacy")
        check(directory.isDirectory || directory.mkdirs())
        // Dialogs add a second Compose root. Capture the owned display instead of an ambiguous root query.
        val bitmap = if (dialog) requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            else compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$label.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show(onFolders: () -> Unit = {}, onClear: () -> Unit = {}, onClearHistory: () -> Unit = {}) {
        val preferences = mutableStateOf(UserPreferences())
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        TrustPrivacyContent(preferences.value,
                            onHistoryEnabled = { preferences.value = preferences.value.copy(playHistoryEnabled = it) },
                            onClearHistory = onClearHistory, onFolders = onFolders,
                            previewReport = { "" }, clearLocalReport = onClear)
                    }
                }
            }
        }
    }
}
