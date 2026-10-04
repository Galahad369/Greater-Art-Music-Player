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
    fun install(app: Application) {
        file = AtomicFile(File(app.filesDir, "last-failure.txt"))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, failure ->
            write("uncaught", failure)
            previous?.uncaughtException(thread, failure)
                ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val saved = runCatching { file?.openRead()?.bufferedReader()?.use { it.readText().take(8192) } }.getOrNull()
            val exits = if (Build.VERSION.SDK_INT >= 30) runCatching {
                app.getSystemService(ActivityManager::class.java)
                    .getHistoricalProcessExitReasons(app.packageName, 0, 4)
                    .joinToString("\n") { "exit reason=${it.reason} at=${it.timestamp} status=${it.status} pssKiB=${it.pss}" }
            }.getOrDefault("") else ""
            latest.value = listOfNotNull(saved, exits.takeIf { it.isNotEmpty() }).joinToString("\n")
                .ifEmpty { "No local failure record" }
        }
    }

    fun recordRecoverable(component: String, failure: Exception) { write(component, failure) }
    private fun write(component: String, failure: Throwable) = synchronized(lock) {
        val report = buildString {
            appendLine("failure=$component version=${BuildConfig.VERSION_NAME} at=${System.currentTimeMillis()}")
            var cause: Throwable? = failure
            repeat(4) {
                cause?.let { error ->
                    appendLine(error.javaClass.name)
                    error.stackTrace.take(24).forEach { appendLine("${it.className}.${it.methodName}:${it.lineNumber}") }
                    cause = error.cause
                }
            }
        }.take(8192)
        latest.value = report
        val target = file ?: return@synchronized
        var stream: java.io.FileOutputStream? = null
        try {
            stream = target.startWrite(); stream.write(report.toByteArray()); target.finishWrite(stream)
        } catch (_: Exception) { stream?.let { runCatching { target.failWrite(it) } } }
    }
}
