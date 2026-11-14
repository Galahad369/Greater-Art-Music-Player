# Android1.22.31/code352 — controls and Developer Mode

SOURCE_ONLY. This is not a new original-signer public release or completed Stack
backing/output acceptance. Keep every earlier artifact and failed experiment.

## Requested changes

- Smaller speed-gesture captions sit at the video bottom, respecting Fit margins
  and visible transport controls. All six UI languages have the gesture copy.
- Already at2x: hold/release without movement stays2x; hold and pull upward to1x.
  Initial temporary2x, pull-down lock, speed-button release and cancellation remain.
- Expanded toolbar has a reserved72dp center handle area. Its left/right groups
  keep all six actions and48dp targets. Narrow widths wrap rather than overlap.
- Diagnostics and element inspection inherit the app's current palette. The
  opaque report remains readable over Ambient; Pick/Copy/Close stay available
  while details scroll. Copy retains the full technical report, all regions and
  the selected element's coordinates. Inspector labels follow the UI language.

## Verification at source checkpoint

Unit checks reproduce the old stationary-release and oversized-handle-region
failures before the repair, then pass unchanged. The theme mismatch and omission
of selected-element details have retained red assertions. Current source passes
595JVM tests (zero failures/errors/skips), lint (zero errors,54warnings,6hints),
and Android instrumentation compilation.

The first A55 diagnostic run passes23/27checks. Retain its failures: the selected
Copy action measures about40dp and is corrected to an explicit48dp minimum;
two toolbar assertions mistakenly measure the128dp outer forced-size fixture.
Those assertions now target the toolbar itself, retaining the same64dp and pixel
checks. The corrected private debug APK passes ALL27checks across seven classes,
with zero skips/errors and an empty crash buffer. It verifies actual Compose
gestures, original-DOWN handle ownership, large-font short/narrow inspection,
complete clipboard, toolbar actions, audio-artwork branches and speed buttons.
These component checks do not prove native-video or six-song output acceptance.

Tested diagnostic source fingerprint:
`355ba93de578e09416f6baec6e5dd9e70a13203bca50ca9df98c4cf93e93d2fd`.
App APK SHA-256:`d162aa95393f4a88a80b5e0cb1f340cb0f610af6e2f71aa4b3b8f11318dda289`;
test APK SHA-256:`f1690a8581bb15722924b8942afd18549c071975d29a86bfd7a403835d31b39b`.
Both use the existing private maintenance certificate, NOT the unavailable
original public signer. Source was uncommitted during these diagnostics; the
same app-input fingerprint is now bound to local commit
`f6959b307a598dba5baec62d519c73ab49a13865`.

## Exact optimized candidate and native UI

Preflight and `assembleRelease` PASS. The normal release gate's signing operation
completes, but its identity check FAILS against the original approved signer; do
not call that a signed-public-release pass. The existing A55 maintenance key signs
the same retained unsigned R8 output separately. No key is created or replaced.

Private candidate: `App/output/now-playing-controls-20261114/optimized31-maintenance/GreaterArt-1.22.31.apk`.
It is 5,595,920 bytes, version 1.22.31/code 352, non-debuggable/non-test, SHA-256
`19658f77dbc8e237a41162a3b3fc410752380833b49eee64d23d7d672fe26039`.
Maintenance certificate:
`0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
The original public certificate remains
`9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`;
its private key is unavailable. This candidate is not update-compatible with
installations signed by that original key and is not a public release.

The installed APK hash matches those exact candidate bytes. Normal A55 Library
video → expanded playback passes measured six 48 dp actions, a clear 72 dp center
handle, stationary 2× hold/release, upward-only restore to 1×, video-bottom hint bounds,
seek/pause/resume/frame advancement and Home return. Native Developer Mode passes
Copy Bug Report, Pick, selected-element Copy/Close and disable. Actual Now Playing,
hint, Developer and selected-element screenshots were visually inspected.
The native report finishes at 11:32:23 UTC on 14 November with an empty crash buffer.

An additional native portrait → landscape → expanded round trip stays in the
same app process and passes. The following Mini check is NOT accepted: a window
query times out, then even guest echo and emulator control stop responding.
Windows inspection finds all 164 owned QEMU threads suspended; a single owned-process
resume succeeds, but the emulator subsequently exits. Retain that failed receipt;
it is not proof of an app crash, a fixed transition defect, or Mini acceptance.

A cleanup-only restart of the same A55 restores both original access app-ops to
DEFAULT and volume 5/15, removes only the hash-checked test package that was absent
at baseline, preserves the optimized installed app, and stops normally at
11:44:19 UTC. Original AVD profile hashes and GPU configuration are unchanged.
The successful native flow already restored Developer Mode off and original 1×.

The separate, bounded Mini-only retry now PASSES on those SAME optimized bytes.
Normal Library video → expanded → Mini → expanded stays in PID1781. Position
advances from12,988ms to31,268ms over18.281seconds, without reset or unexpected
jump. The actual Mini bounds are[33,975]→[312,1132]px; its screenshot and the
returned expanded view are visually inspected. Crash buffer stays empty.
Both DEFAULT app-ops, original volume5/15, test-package absence and stored profile
hashes remain intact. The owned A55 stops normally at12:32:14UTC14November.
Retain the earlier host-interrupted attempt; this clean retry does not repair or
qualify every fullscreen handoff, physical device or Stack output scenario.

Receipts: `optimized31-native-smoke-01/report.json`,
`optimized31-native-transitions-01/report.json`, `owned-emulator-resume.json`, and
`a55-restore-01/restoration.json` plus final `a55-mini-retry-01/report.json` under
the private evidence root below. `preservation-schema-corrected.json` verifies
577unchanged original files and41unchanged baseline absences. Its retained first
checker incorrectly treated `absent=true` sparse-worktree entries as newly lost
APKs; no previously present file is missing and no APK restoration was attempted.
`final-verification.json` consolidates the exact APK, committed app-input binding,
debug component checks, optimized native UI/Mini results, restoration and release
limits. It does not replace the retained failed receipts or sign a CI attestation.

Private evidence: `App/output/now-playing-controls-20261114/`.
The earlier exact30 screenshot is an observed baseline, not an image of this build.
The public download remains the earlier verified 1.21.93. No source or artifact
has been pushed, merged, published or promoted as a completed backing repair.

## Preserve

No Stack matcher/engine/threshold change. No native video resolution, bitrate,
FPS, HDR, sample-format, Fit/Crop or surface-owner change. Shared-output flags stay
off. The unfinished private post-DSP observer is outside the app. The fullscreen
transition defect is diagnosed separately and not claimed fixed. No push/merge,
publication, key replacement, app uninstall, device downgrade or data wipe.
