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
same app-input fingerprint must be bound to the local source commit before the
optimized build. Optimized assembly/exact-file native playback smoke remain pending.

Private evidence: `App/output/now-playing-controls-20261114/`.
The earlier exact30 screenshot is an observed baseline, not an image of this build.

## Preserve

No Stack matcher/engine/threshold change. No native video resolution, bitrate,
FPS, HDR, sample-format, Fit/Crop or surface-owner change. Shared-output flags stay
off. The unfinished private post-DSP observer is outside the app. The fullscreen
transition defect is diagnosed separately and not claimed fixed. No push/merge,
publication, key replacement, app uninstall, device downgrade or data wipe.
