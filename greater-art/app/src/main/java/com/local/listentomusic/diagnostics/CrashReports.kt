package com.local.listentomusic.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.util.AtomicFile
import com.local.listentomusic.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/** Bounded app-private diagnostics. No media names, exception messages or network. */
object CrashReports {
    val latest = MutableStateFlow("No local failure record")
    private var file: AtomicFile? = null
    private val lock = Any()
    private var revision = 0L
    fun install(app: Application) {
        file = AtomicFile(File(app.filesDir, "last-failure.txt"))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, failure ->
            write("uncaught", failure)
            previous?.uncaughtException(thread, failure)
                ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
        val initialRevision = synchronized(lock) { revision }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val saved = runCatching { file?.openRead()?.bufferedReader()?.use { reader ->
                val buffer = CharArray(PrivateFailureReport.LIMIT)
                var length = 0
                while (length < buffer.size) {
                    val count = reader.read(buffer, length, buffer.size - length)
                    if (count <= 0) break
                    length += count
                }
                PrivateFailureReport.storedPreview(String(buffer, 0, length))
            } }.getOrNull()?.takeIf { it.isNotBlank() }
            val exits = if (Build.VERSION.SDK_INT >= 30) runCatching {
                app.getSystemService(ActivityManager::class.java)
                    .getHistoricalProcessExitReasons(app.packageName, 0, 4)
                    .joinToString("\n") { "exit reason=${it.reason} at=${it.timestamp} status=${it.status} pssKiB=${it.pss}" }
            }.getOrDefault("") else ""
            synchronized(lock) {
                if (revision == initialRevision) latest.value = listOfNotNull(saved, exits.takeIf { it.isNotEmpty() }).joinToString("\n")
                    .ifEmpty { "No local failure record" }
            }
        }
    }

    fun recordRecoverable(component: String, failure: Exception) { write(component, failure) }
    fun preview(): String = synchronized(lock) { PrivateFailureReport.storedPreview(latest.value) }
    fun clear() = synchronized(lock) {
        val target = file
        target?.delete()
        if (target?.baseFile?.exists() == true) throw java.io.IOException("Diagnostic clear failed")
        ++revision
        latest.value = "No local failure record"
    }
    private fun write(component: String, failure: Throwable) = synchronized(lock) {
        val report = PrivateFailureReport.format(component, failure, BuildConfig.VERSION_NAME, System.currentTimeMillis())
        ++revision
        latest.value = report
        val target = file ?: return@synchronized
        var stream: java.io.FileOutputStream? = null
        try {
            stream = target.startWrite(); stream.write(report.toByteArray()); target.finishWrite(stream)
        } catch (_: Exception) { stream?.let { runCatching { target.failWrite(it) } } }
    }
}
