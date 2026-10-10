# Adaptive Stack sync — integration and feedback

2026-10-10. Local source 1.21.24/code247; based on main `3060545`.
Adapted from Claude's supplied patch `ef5cf61`; its proposed 1.21.23/code246
was already consumed by the separate PCM prototype. No release APK replaced.

## Implemented

- Per-voice robust successive-difference MAD noise estimate, 24-reading ring.
- Unknown noise until 12 readings; 1.5 sigma dead zone clamped to 2.5–12 ms.
- Small drift must persist in one direction; large errors retain immediate
  correction. Existing hard-seek threshold/cooldown, pitch preservation and
  primary media quality are unchanged.
- Fine trims quantized to 0.1%; apply updates of at least 0.2% or return neutral.
- DEV voice diagnostics expose sigma, deadzone, trim and drift.

Integration differs from the supplied patch in three reviewed details:
Float subtraction could suppress exactly 0.2%, so comparisons use quantized
thousandths. Dwell resets when drift reverses and saturates instead of overflowing.
Fixing Float comparison increased model trim activations at sigma=2 ms beyond
the existing assertion. Twelve same-direction ticks (600 ms, rather than eight/
400 ms) pass that original noise budget and convergence guard without relaxing
either assertion. The simulation uses the same direction rule as the controller.

## Verified

- All 286 JVM tests pass, including 15 adaptive-control tests.
- Android main/test APK assembly passes. Lint: zero errors, 28 warnings, 2 hints.
- Unchanged version guard passes; current and reachable-history security audit
  passes. No permission, network, decoder-quality, resolution/FPS/bitrate change.
- API36 A55-sized emulator: six actual local takes play for 60 seconds;
  diagnostic test PASS in 62.857 seconds. Initial attempt failed before collecting
  readings because `c2.android.aac.decoder` could not initialize. Stopping the
  stale app process and retrying passed; do not erase that failure or call it a
  proven production fix.
- Unchanged six-take start/seek/resume/primary-boundary-loop regression PASS
  in 13.061 seconds, including the existing mean-settled-drift <75 ms assertion.
  A single passing repeat does not erase the earlier runtime failure or establish
  acoustic latency or sustained decoder capacity.
  Final eight sampled max-drifts: 58,46,45,41,40,43,52,51 ms (mean 47 ms).

Last-half-minute diagnostics, five companions sampled once/second (155 values):

| Reported metric | Result |
| --- | --- |
| Absolute smoothed position drift median / p95 / max | 28 / 61 / 75 ms |
| Estimated raw-reading sigma range | 3.1–12.5 ms |
| Adaptive dead-zone range | 4.7–12.0 ms |

Second-60 companion snapshot for feedback (primary is not a companion row):

| Voice | trim | drift ms | sigma ms | deadzone ms |
| --- | --- | --- | --- | --- |
| 1 | 0.963 | 66 | 10.4 | 12.0 |
| 2 | 1.016 | 6 | 7.3 | 11.0 |
| 3 | 1.020 | -40 | 6.2 | 9.4 |
| 4 | 0.990 | 19 | 8.3 | 12.0 |
| 5 | 1.007 | 7 | 7.3 | 11.0 |

These are pooled position diagnostics from emulator software decoders, **not
acoustic timing**, not a before/after comparison, and not physical Samsung data.
No alignment offsets were applied in this control-loop harness. The test checks
that voices remain running, not that different arrangements match musically.

## Feedback for Claude

The smaller trigger is a reasonable control-loop improvement, not proof of the
cause of beating. Real emulator sigma exceeded the clean-model assumptions and
often reached the old 12 ms cap. Similar player positions do not imply aligned
PCM at the speaker: independent AudioTracks, decoder startup and Sonic buffering
can still differ. The new controller is not sample-accurate and cannot establish
phase lock. High-noise fallback preserves the old trigger threshold, **not the
entire old algorithm**, because trim quantization and dwell differ.

Physical follow-up: play the six aligned takes for a minute, record these same
fields plus seek/buffering events, and listen or capture loopback output with an
independent reference. Do not infer acoustic improvement from model p95 alone.
The separate one-AudioTrack prototype remains default-off and was not merged
into this change. No new GitHub CI result or release certification is claimed.
