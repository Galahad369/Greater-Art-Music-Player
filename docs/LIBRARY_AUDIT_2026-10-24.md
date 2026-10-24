# Library hot-path follow-up — 1.21.77/code300

Base: main at `6145063` (1.21.76). The supplied read-only audit used older `adee4c6`.

## Implemented

- `peekMemory(file)` reuses TTL-valid RAM aliases/LRU only. No filesystem, suspend pipeline, decoder, scheduler entry or stats publication. Rows seed their remembered bitmap from this probe; only misses install the loading effect. Hidden thumbnails do not probe. No thumbnail preloading added.
- The unchanged four-field thumbnail request key is lazy-memoized per immutable MediaFile, not stored in an unbounded global map. Copying a media file recomputes the identity from its new fields. MediaFile's immutable public contract is explicit so the lazy implementation does not accidentally destabilize Compose rows. The row's keys include sourcePath to invalidate a replaced URI source.
- Viewport callbacks conflate obsolete ranges and pace scrolling work at 50ms. Scroll-budget edges and closing swipe actions run upstream, immediately; final idle viewport delivery can lag by at most the remaining 50ms pacing window. Existing generation cancellation, disk reads during fling, priority selection, two-adjacent idle prefetch and cancellation cleanup remain intact.
- The shell collects distinct playback snapshots with position omitted. Its initial snapshot is remembered, not polled in composition. The diagnostics leaf collects the real clock; Settings consumes control state, and Now Playing/Stack retain their own full-rate clock. This does not lower video/audio quality or animation FPS.

## Audit corrections and deferred work

- A suspend RAM hit does not prove a user-visible placeholder frame; effect scheduling and rendering timing must be measured. The new probe/effect test proves the hit avoids the suspend loader, not a particular frame-time saving.
- ProfileInstaller already exists transitively: the current 1.21.76 debug APK contains its version metadata. The existing local unsigned R8 output contains `assets/dexopt/baseline.prof` and `.profm`, but its manifest is **1.21.71/code294**. That old output is not evidence that a new release was built or tested.
- No app-owned generated profile was found or fabricated. A duplicate ProfileInstaller dependency or blanket hand-written class rules would not establish useful coverage. Android supports library-supplied profiles; app-specific profiles should be generated and validated against real exercised journeys. [Official Android baseline-profile guidance](https://developer.android.com/topic/performance/baselineprofiles/overview).
- No phone is attached. The requested before/after release Perfetto traces are **not collected**. App-specific profile generation/validation is a separate unfinished follow-up, not inherently limited to physical hardware; real-device performance acceptance is blocked by device availability.
- Keep this change pending performance acceptance. No before/after p95/p99, root skip-rate, GPU-cause, release/debug speedup or battery benefit is claimed. No background/inspector changes were made.

## Actual functional verification

- 440 JVM cases passed, including request-key reuse/invalidation, projection control/media changes and viewport burst/final-idle behavior.
- Lint: 0 errors, 46 warnings, 4 hints. Debug app and instrumentation APKs assembled.
- Owned API36 emulator: 12/12 cases passed in 27.769s: two scroll/clock restart-boundary tests, five row tests (including seeded RAM hit and disabled-thumbnail behavior), and five existing real-cache/pipeline cases. This is not a Perfetto benchmark or proof of physical smoothness.
- The viewport test initially emitted before its SharedFlow subscriber attached; waiting for subscription fixed the harness. Lint caught direct StateFlow.value reads in initial-value setup; remembering the shell snapshot and keeping the test's initial state outside composition fixed them. These were corrected before the versioned source commit, not waived or baselined away.
- Existing full fullscreen-rotation regression remains unresolved; this branch does not touch or claim to fix it.

## Required performance acceptance

On one identified physical phone, compare base and candidate minified builds with the same corpus, background, refresh rate, thermal state and compilation mode. Record three-second flings after cold launch, warm navigation, and Stack→Library return. Repeat with DEFAULT background as an A/B control. Attach original traces plus exact APK hashes, installation/compilation conditions and frame-selection criteria. Classify main-thread versus RenderThread work; do not infer the cause from aggregate jank percentages alone. Separately generate and verify an app-owned profile if the measured journeys justify it.
