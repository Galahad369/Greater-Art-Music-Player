# Library Performance v2 and Stack synchronization roadmap

## Scope and state — October 9, 2026

Current local implementation: **1.21.18 / code 241**, uncommitted, **SOURCE_ONLY**.
Baseline: local main `3c8752d`, executable source equivalent to origin/main `1e61d2e` (1.21.16 / 239).
PR #156 already consumed 1.21.17 / 240; its unrelated theme work is not imported here.
Existing release artifacts are preserved. A build output is not a published or verified release.

The proposed musical alignment and shared-clock output are **different problems**.
Neither is solved by enlarging an artwork cache. Keep the packages independently testable;
do not replace the working player with an untested mixer in the same patch.

## Implemented: stages 1–2, artwork scheduling and cache pressure

### Ownership

`MediaCaches` owns one application-scoped `ThumbnailRepository`. All callers use its
`ThumbnailRequestScheduler`: at most 96 outstanding unique requests and two workers
on constrained heaps / three on larger heaps. Generation retains its tighter permit
and also shares `OfflineAnalysisBudget` with waveform/Stack analysis. Actual playback
does not acquire this analysis lock. Metadata scanning is **not yet** coordinated by it.

Library reports its actual visible indices. Pending visible rows win over offscreen
consumers; two adjacent rows may read cached disk images after 160 ms of stable idle.
Prefetch never generates a video frame or scans the full library. The source lock and
RAM recheck also avoid a duplicate decode when disk prefetch overlaps a foreground row.

### Scroll invariant

- RAM lookup uses source metadata already supplied by the scanner; it performs no
  filesystem stat or sibling-cover probing. Cached artwork stays immediate during fling.
- Cache misses wait in the bounded shared queue. A fling cancels current jobs; remaining
  consumers keep their result promise and retry after idle. Leaving composition cancels
  only that consumer; the last consumer removes obsolete work.
- Disk/extraction checkpoints also check the live scroll flag, including after waiting
  for a resource permit. Android thumbnail/query APIs receive a `CancellationSignal`.
- Some `MediaMetadataRetriever` and bitmap calls cannot be interrupted safely. Let the
  current call unwind and release its resources, then stop; never release a retriever
  from another thread or start a replacement worker before the old one really exits.
- Queue overflow returns a placeholder and records a rejection, not a permanent
  “no artwork” result. A later request may retry.

### Cache identity and memory

- Budget: normal heap/12 (existing 8–64 MiB policy), reduced for low-RAM devices,
  selected primary video, and allocated Stack slots. Four or more slots use one-quarter
  of the baseline. A tiny heap also bounds the floor. Account `allocationByteCount`,
  not compressed bytes, and never recycle a bitmap still held by a row.
- L0 row-held images are outside the LRU accounting: the reported peak is **cache peak**,
  not total process heap or native decoder memory. Physical memory profiling is still needed.
- Disk reads inspect bounds and sample directly toward the thumbnail target. This only
  changes artwork allocations, never playback resolution, bitrate, audio or frame rate.
- Identical embedded/folder covers use a SHA-256 digest of actual cover bytes. One
  `art-<digest>.webp` and RAM entry can serve many source `.ref` files. Never infer
  identity from album names, filenames or tags. References accept only that exact asset
  name, never a path. The digest is local and never uploaded.
- Existing per-file disk images and negative markers remain readable. Disk remains
  capped at 256 MiB / 6,000 entries, including references. Pressure evicts RAM, not valid
  disk entries. Clear cancels pending work and uses the existing writer locks/epoch.
- Diagnostics: queue depth, shared/cancelled/rejected requests, peak pending count,
  cache hits, verified cover reuse, current/budget/peak cache KiB, recoverable failures.

### Files and tests

`ThumbnailRequestScheduler.kt`, `ThumbnailRepository.kt`, `MediaCaches.kt`,
`LibraryScreen.kt`, `NowPlayingScreen.kt`, `MainViewModel.kt`, `GreaterArtApp.kt`,
and the playback track-selection callback in `PlaybackService.kt`.
`ThumbnailWriterLocks.kt` makes cancellation during clear release only owned locks;
DEV counter collection uses a child composition rather than the app root.

`ThumbnailRequestSchedulerTest` exercises priority, deduplication, per-consumer
cancellation, deferred retries, real permit lifetime, queue capacity and clear.
`ThumbnailPipelineTest` checks real Android bitmap/RAM/disk/reference behavior.
No new permission or dependency is introduced. See HANDOFF for final test results.
Final local gate: **249 JVM tests, 3 API 36 cache regressions, lint/build pass**.
The Library/dock video smoke reached READY with no current warnings. There is no
controlled before/after fling benchmark or physical acoustic result yet.

## Next: stage 3, instrument-first alignment (#145)

Create a new feature-cache schema; do not reinterpret v6 entries as v7 data.
Decode short regions distributed through the *clipped* timeline, retaining original
source-rate PCM only for bounded fine windows. Extract STFT harmonic/percussive
features, percussion onsets, bass-band rhythm and harmonic/chroma features. Store
each stream and confidence separately. HPSS separates harmonic/percussive structure,
**not vocals versus instruments**; harmonic vocals can remain. L−R is not vocal removal.

Optional accompaniment separation stays a separately licensed, checksummed, offline
model-import/bundling experiment. No automatic network/model downloads or change to
audible source files. Do not make a large model a dependency of normal playback.

## Stage 4: timing map and ambiguity (#145)

Match separated windows independently. Fit `t_companion = a * t_primary + b` only when
multiple distant landmarks agree. Repeated chorus/bar alternatives must compete;
reject small best-vs-second margins or inconsistent anchors. Silence, different
arrangements and incompatible clips must abstain, preserving the user's current offset.

Bound constrained DTW/piecewise mapping by time, slope, jump and memory limits;
unbounded whole-song cost matrices are not appropriate on a phone. Report the evidence
and confidence before applying a tempo map. Applying a non-unit map needs a tested,
explicit time-correction path; ordinary resampling changes pitch and is not automatically
equivalent to the current pitch-preserving speed correction.

## Stage 5: fine timing (#145)

Refine confident transients using original-rate PCM correlation/GCC-PHAT-style
estimation, several anchors and a consistency gate. Keep fractional/sample-frame timing
in the analysis result; preserve a backward-compatible millisecond manual offset.
Do not claim phase-perfect vocal alignment or nanosecond precision. At 48 kHz one
sample is about 20.8 microseconds. The requested 2–5 ms analysis error is a **target**,
not a measurement already achieved by this implementation.

## Stage 6: shared-clock prototype (#146)

Per-file decoder → bounded PCM queue → timeline mapping/delay → explicit gain/mute/solo
→ float accumulator → one AudioTrack and frame counter. Use timestamp/underrun metrics,
backpressure and cancellable seek/flush epochs. Prove offsets, seek, loop and end-of-file
semantics first. Primary-boundary loop restarts all voices together.

Keep sample-rate/channel/encoding policy explicit: a common output format may require
resampling. Do not silently reduce source fidelity, clip the summed signal, run duplicate
audible ExoPlayer outputs, or substitute reduced-quality proxy media. Unsupported source
formats/device capacities retain a clearly reported existing-player fallback. Preserve
the primary video pipeline, audio focus, Bluetooth/noisy behavior and overlay journey.
First release remains opt-in/prototype until physical output measurements are credible.

## Stage 7: physical regression matrix (#147)

Measure six real 孤独毒毒 takes plus controlled common-backing fixtures. Include repeated
chorus ambiguity, different tempi, mono/stereo, 44.1/48 kHz, clips, pause/seek/loop,
primary changes, headset disconnect, all presentation transitions and decoder pressure.
Measure acoustic relative timing, source/video quality, dropped frames, output underruns,
heap/native memory and cold/warm fling jank. An A55-sized emulator can verify logic and
UI, **not Samsung codec capacity, acoustic sync or “crash-free” operation everywhere**.

## Primary references

- [librosa HPSS](https://librosa.org/doc/0.11.0/generated/librosa.decompose.hpss.html)
- [librosa constrained/subsequence DTW](https://librosa.org/doc/0.11.0/generated/librosa.sequence.dtw.html)
- [Android AudioTrack timestamps and PCM output](https://developer.android.com/reference/android/media/AudioTrack)
- [Demucs research source](https://github.com/facebookresearch/demucs) — a reference,
  not an adopted Android runtime or a promise of affordable on-device separation.
