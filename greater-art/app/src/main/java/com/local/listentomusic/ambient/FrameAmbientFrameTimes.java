package com.local.listentomusic.ambient;

import androidx.media3.common.C;

/** Exact, bounded SDR admissions. A release-time reorder must not erase an in-flight frame. */
final class FrameAmbientFrameTimes {
    private static final int CAPACITY = 64;
    private final long[] releaseTimes = new long[CAPACITY];
    private final long[] presentationTimes = new long[CAPACITY];
    private int next;
    private int size;

    synchronized void record(long releaseTimeNs, long presentationTimeUs) {
        releaseTimes[next] = releaseTimeNs;
        presentationTimes[next] = presentationTimeUs;
        next = (next + 1) % CAPACITY;
        if (size < CAPACITY) size++;
    }

    synchronized long lookup(long releaseTimeNs) {
        for (int age = 1; age <= size; age++) {
            int index = (next + CAPACITY - age) % CAPACITY;
            if (releaseTimes[index] == releaseTimeNs) return presentationTimes[index];
        }
        return C.TIME_UNSET;
    }

    synchronized long latest() {
        return size == 0 ? C.TIME_UNSET : releaseTimes[(next + CAPACITY - 1) % CAPACITY];
    }
}
