package com.local.listentomusic.ui

/** Confirm Android's grant state, not merely whether a provider call threw. */
internal fun retainDocumentReadGrant(take: () -> Unit, hasReadGrant: () -> Boolean): Boolean {
    runCatching(take)
    return runCatching(hasReadGrant).getOrDefault(false)
}

internal fun releaseDocumentReadGrant(release: () -> Unit, hasReadGrant: () -> Boolean): Boolean {
    runCatching(release)
    return runCatching { !hasReadGrant() }.getOrDefault(false)
}
