# Claude Library row follow-up — 1.21.64/code287

Base: merged main `e545821ed3c7d02b9cf3e909b524c34104aff78d` (1.21.63).
Credit: Claude's supplied static Library scrolling audit of1.21.58. Implementation
selectively preserves current behavior instead of applying every hypothesis.

## Changes and prevention

- Closed row Add/Delete underlays were still composed/measured/registered even when
  clipped away. Compose them only while the transition's current or target state is
  open. Retain until exit completes, preventing abruptly disappearing underlays.
  Disable closing actions; dispose their inspector registrations after the animation.
- Read animated row color inside `drawBehind`; press/reveal already used draw/layer
  state and retain that behavior. No card appearance, alpha, target size or layout change.
- Remember metadata by name/size/duration and ImageBitmap by backing Bitmap. Refresh
  on corresponding source changes; do not memoize without invalidation keys.
- Remember the selected-path set once per selected-list change, preserving saveable
  ordering and result-scoped selection. No change to selection commands.

## Scope limits

Target physical RAM is8GB. Android still grants a smaller per-app managed heap; retain
existing measured heap and low-memory budgets. Do not set an8GB heap/cache assumption.
No quality caps, surface detachment, decoder change, thumbnail TTL change or shipping
baseline profile is bundled. Source inspection and the previous software-rendered
emulator trace do not prove those other hypotheses are physical-phone bottlenecks.
`UiInspector` already avoids per-coordinate snapshot writes, and the thumbnail
scheduler correctly requeues cancellation. Neither is replaced by an older proposal.

## Verification

Precommit candidate passes418 JVM tests, lint(0 errors,46 warnings,4 hints),
debug/test APK assembly and release-only R8 assembly using Java21 and the repository
wrapper offline. Version guard and whitespace checks pass.

Owned API36 emulator `GreaterArt_Scroll_QA36`, booted with8192MB RAM, reports
MemTotal8136436kB and a576MB managed heap limit.13/13 instrumented tests pass in38.246s:
four new `LibraryRowWorkTest`, four existing `ThumbnailPipelineTest`, five existing
`DeveloperDiagnosticsTest`. No ignored tests. These verify closed-action absence,
48dp open targets/commands, complete exit lifecycle, reopening, close-before-play,
metadata invalidation, inspector disposal and retained cache/retry/diagnostics behavior.
This is a generic x86_64 Android16 emulator, not Samsung hardware or an acoustic test.

The original2GB SwiftShader timing data cannot be compared directly with this8GB
boot. No before/after physical-device speedup, GPU cause or universally bug-free claim.
Final committed-source APK provenance is separate from the above precommit evidence.

## Artifact and release boundaries

SOURCE_ONLY until separate acceptance. Immutable verified APK stays1.21.39/code262;
signing identity, Desktop and privacy PR174 are unchanged. Candidate APKs must use a
new non-overwriting path and carry their actual version/hash/source provenance.

## October 21 — Range-keyed viewport and row-layer follow-up (1.21.67)
An additional read-only audit noted map allocation each scroll frame, filtered-list
allocation in scheduler selection and a graphics-layer wrapper on every visible row.
The source follow-up collects (scrolling, first, last) triples, skips unchanged viewport
sets, performs a stable one-pass scheduler priority search and avoids layers on
non-current thumbnails. It retains all PR #192 swipe/action optimizations and the
1.21.65 indicator. These are source inefficiencies, not verified frame-time causes.
No changes to cancellation/decode behavior without real-device profiling evidence.

## October 21 — Scroll-time disk-cache work (1.21.70; first side-branch identity 1.21.69 consumed)
The scheduler now distinguishes cheap on-disk cache probes from platform-media thumbnail extraction. An in-flight disk probe survives scroll start; new scroll-time disk probes are serialized to one; disk hits can render during scrolling. Misses queue a separate generation stage that starts only after scrolling stops. Native generation still cancels on scroll start and holds its permit until actual exit. JVM tests protect this separation. Device Perfetto/FrameTimeline data is required to determine whether fling-stop pop-in improves; do not assume source changes guarantee lower jank.
