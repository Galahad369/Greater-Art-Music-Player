# Android 1.22.39 / code 360 — frame-ready fullscreen handoff

## Cause and approved repair

The expanded service hid its window as soon as it launched the fullscreen Activity.
Keeping the old PlayerView alive preserved playback, but did not preserve visible
pixels during Activity setup and GL-to-native surface transfer. The owner asked
to fix the abrupt, multi-step visible transition on 16 November 2026.

- Retain the outgoing expanded picture while fullscreen prepares its native surface.
- Give input/focus to the Activity immediately: no touches, dim or accessibility
  actions on the visual cover. Sharing still suppresses the overlay.
- Accept readiness only after a newer NOW_PLAYING surface generation has a first
  frame. The old source frame and attachment without a frame are not readiness.
- Commit cover removal on a display frame, rather than an arbitrary successful-path
  timer. The shared player/controller and source-quality policy remain; this
  change adds no decoder restart or player reprepare.
- If no destination frame arrives within eight seconds, return to the original
  destination and clear readiness. This replaces the old 1.5-second handoff-cleanup
  timeout; it does not add an eight-second delay to normal entry or weaken tests.
- Preserve portrait-first entry, Rotate, native HDR, Fit/Crop, return modes, Lock,
  toolbar spacing, Mini, sharing, cache, Stack and audio behavior. No bitmap copy,
  resolution cap, forced landscape, replacement engine or extra decoder is added.

## Checks and limits

Private evidence: `App/output/fullscreen-ready-20261116/`. `baseline.json` records
the original source/user changes and exact 34/38 APK hashes. Build preflight passes.
There is no attached A55 and only about 1.5 GiB free RAM at inspection; leave the
owner's other work alone and do not blindly repeat emulator startup failures.

`checks-red-01.log` / `policy-red-01.xml` preserve the same-check failure:
“Keep the outgoing frame until the destination renders expected 1.0 but was 0.0”.
The baseline helper is a behavior-preserving extraction of the old alpha expression,
not a native screen recording. New tests also reject source/stale/wrong-owner frames
and attachment without a rendered frame. Existing fullscreen instrumentation keeps
all player, dimensions, playback and return assertions, and adds readiness/reset
checks. `checks-all-02.log` passes in 4m 25s: 616 JVM tests, zero failures/errors/
skips, lint zero errors/54 warnings, debug and instrumentation APK assembly.
Preserve `checks-all-01.log`, the compile-time generation counter mismatch fixed
to match the existing Long type. No assertion or fixture was weakened. Optimized
assembly and exact-A55 acceptance remain separate. `self-review39.json` records
the source-bound self-review (not independent review): 655 unrelated files and
the original APK hashes are preserved. Existing attach/reconcile, entry launch,
return, orientation and Now Playing controls are unchanged.

This change removes an identified visual gap in policy. Host assertions cannot
prove SurfaceFlinger timing, renderer retention or perceived smoothness on a phone.
Readiness uses the existing lease-generation/timestamp frame attribution, not a
new physical-display measurement.
Before claiming that, record actual entry/return and first-frame/overlay timing on
a healthy A55 with normal SDR Ambient, Power saving, paused video, HDR routing,
Rotate, Fit/Crop, repeated entry, Home and launch failure/timeout. Preserve original
frame dimensions and player identity, and bind normal-entry smoke to the exact APK.

References: [Media3 view handoff](https://developer.android.com/reference/androidx/media3/ui/PlayerView#setPlayer(androidx.media3.common.Player))
and [display-frame scheduling](https://developer.android.com/reference/android/view/View#postOnAnimation(java.lang.Runnable)).
Reuse the native presentation and first-frame signal; do not switch HDR to an SDR
texture or add a low-resolution screenshot transition to conceal the handoff.
