package com.local.listentomusic.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateFailureReportTest {
    @Test fun messagesAndSourceFileNamesNeverEnterReport() {
        val failure = IllegalStateException("/storage/emulated/0/Download/孤独毒毒.mp4 content://private?token=secret")
        failure.stackTrace = arrayOf(StackTraceElement("com.local.Player", "play", "PrivatePerson.kt", 42))
        val report = PrivateFailureReport.format("player", failure, "1.21.95", 5)
        assertTrue(report.contains("com.local.Player.play:42"))
        listOf("/storage", "孤独", "content://", "secret", "PrivatePerson").forEach { assertFalse(report.contains(it)) }
        assertEquals(report.trim(), PrivateFailureReport.storedPreview(report))
    }

    @Test fun unstructuredLegacyRecordsAreNotExported() {
        assertEquals("", PrivateFailureReport.storedPreview("failure=player path=/private/track.mp3"))
        assertFalse(PrivateFailureReport.storedPreview("diagnostic-format=1\nmessage=private\ntype=java.io.IOException\n").contains("private"))
    }

    @Test fun callerSuppliedComponentAndVersionCannotSmugglePaths() {
        val report = PrivateFailureReport.format("/private/track.mp3", RuntimeException("secret"), "secret\npath", -1)
        assertTrue(report.contains("component=unknown\nversion=unknown\nat=0"))
        assertFalse(report.contains("secret"))
    }

    @Test fun oversizedStacksAndChainsAreBounded() {
        var failure: Throwable = RuntimeException("private")
        repeat(20) { failure = RuntimeException("secret", failure) }
        failure.stackTrace = Array(200) { StackTraceElement("com.local.Player", "tick", "secret.mp4", 10) }
        val report = PrivateFailureReport.format("player", failure, "1.21.95", 0)
        assertTrue(report.length <= PrivateFailureReport.LIMIT)
        assertTrue(report.lineSequence().count { it.startsWith("type=") } <= 4)
    }
}
