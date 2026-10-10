package com.local.listentomusic.ui

internal fun normalizeExcludedFolderInput(input: String): String? {
    val path = input.trim().replace('\\', '/').trim('/')
    return path.takeIf { it.isNotBlank() && ':' !in it && '\u0000' !in it &&
        it.split('/').none { part -> part.isEmpty() || part == "." || part == ".." } }
}
