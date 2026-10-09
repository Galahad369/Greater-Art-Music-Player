package com.local.listentomusic.data

import kotlinx.coroutines.sync.Mutex

/** Clear may be cancelled while acquiring writers; release only the prefix we own. */
internal suspend fun <T> withThumbnailWriterLocks(locks: Array<Mutex>, action: () -> T): T {
    var acquired = 0
    try {
        for (lock in locks) { lock.lock(); acquired++ }
        return action()
    } finally {
        for (index in acquired - 1 downTo 0) locks[index].unlock()
    }
}
