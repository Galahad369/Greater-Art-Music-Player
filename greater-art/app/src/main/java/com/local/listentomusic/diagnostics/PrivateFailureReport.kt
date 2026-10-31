package com.local.listentomusic.diagnostics

/** Allowlisted code locations only. Never serialize Throwable.message, fileName or toString(). */
internal object PrivateFailureReport {
    const val LIMIT = 8192
    private val identifier = Regex("[A-Za-z0-9_.$<>-]{1,180}")
    private val version = Regex("[0-9]+(\\.[0-9]+){1,3}")

    fun format(component: String, failure: Throwable, appVersion: String, nowMs: Long): String = buildString {
        appendLine("diagnostic-format=1")
        appendLine("component=${token(component)}")
        appendLine("version=${appVersion.takeIf(version::matches) ?: "unknown"}")
        appendLine("at=${nowMs.coerceAtLeast(0)}")
        var cause: Throwable? = failure
        val visited = mutableSetOf<Throwable>()
        repeat(4) {
            val error = cause ?: return@repeat
            if (!visited.add(error)) return@repeat
            appendLine("type=${token(error.javaClass.name)}")
            error.stackTrace.take(16).forEach { frame ->
                appendLine("frame=${token(frame.className)}.${token(frame.methodName)}:${frame.lineNumber.coerceAtLeast(0)}")
            }
            cause = error.cause
        }
    }.take(LIMIT)

    fun storedPreview(text: String): String {
        // Do not export legacy/unstructured text, even if it happens to be in the local file.
        if (!text.startsWith("diagnostic-format=1\n")) return ""
        val allowed = Regex("(diagnostic-format=1|component=[A-Za-z0-9_.$<>-]{1,180}|version=(unknown|[0-9]+(\\.[0-9]+){1,3})|at=[0-9]{1,19}|type=[A-Za-z0-9_.$<>-]{1,180}|frame=[A-Za-z0-9_.$<>-]{1,361}:[0-9]{1,10})")
        return text.take(LIMIT).lineSequence().filter(allowed::matches).joinToString("\n")
    }

    private fun token(value: String): String = value.takeIf(identifier::matches) ?: "unknown"
}
