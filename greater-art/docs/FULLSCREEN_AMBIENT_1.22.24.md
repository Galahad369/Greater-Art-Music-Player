# Fullscreen Ambient and usable toolbar — 1.22.24 / code 345

The owner requested sampled Ambient in fullscreen's blank space, the existing DEV tools, and evenly distributed Now Playing top buttons. Preserve native video, HDR, Fit/Crop, zoom/pan, all controls, the pull-to-Library gesture and source quality.

## A55 caught what the isolated toolbar test missed

[1.22.23's receipt](FULLSCREEN_AMBIENT_1.22.23.md) preserves the exact optimized APK and failures. A correct icon-centre tap at797,223 did not open fullscreen. A diagnostic tap at796,270, inside the same48dp button but below the pull region, opened it. Fullscreen glow and DEV then passed. The screenshot also shows the centered DEV badge clipping two actions.

The issue was not a missing callback or a stale-coordinate assumption: the before/after UI dumps report identical fullscreen bounds. Compose hit testing gives the overlapping sibling pull layer the event instead of the button below. See [Android's gesture dispatch documentation](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures).

## Repair

- Recognize the existing pull gesture on the toolbar **parent**, so child buttons receive normal taps. Keep the central horizontal activation band, resistance, activation threshold, cancel cleanup and return callback. The parent is bounded to the toolbar; it does not take queue/video gestures.
- Retain the visual handle without an input-stealing layer. Place its mark above the button glyphs. Keep all48dp button targets and the even spacing.
- Use measured toolbar bounds for the expanded DEV badge, as in native fullscreen. DEV no longer occupies the space between the middle actions. The same measurement callback covers audio and video toolbars.
- Carry forward automatic sampled fullscreen Ambient, native HDR/video, Fit/Crop and the existing complete DEV report/inspector. No new renderer, decoder or quality cap.

## Verification

Evidence stays in `App/output/fullscreen-ambient-toolbar-20261112/`. Run the normal icon-centre test, **without** its diagnostic lower-tap switch, against the exact new APK. Require the original native glow/DEV assertions, full inspector actions, even unoccluded button centers and a working pull return. Do not use the diagnostic to manufacture a normal-entry pass.

All542 JVM tests pass, with zero failures/errors/skips. Lint has zero errors,54 warnings and6 hints. Android instrumentation compiles/packages successfully; the component tests were not executed on this build. The normal-entry native check below exercises the optimized APK instead of substituting a debug build.

### Exact optimized candidate

- Runtime commit: `bf0ae87e817ad0004aa78c996eb97fd66c31d033`.
- Android executable-input SHA-256: `ebd1f30576b0296e239b66800556f937f4b7eb460b6cab86c84c59334a78da64`.
- Preflight and `:app:assembleRelease`: PASS. R8 assembly completed in3m22s. This is build time, not an app-performance benchmark.
- APK: `App/output/fullscreen-ambient-toolbar-20261112/optimized-local-24/GreaterArt-1.22.24.apk`,5,579,536bytes.
- APK SHA-256: `cd5f4d32a8f7252f9753566f0784bdbe68355b9dc026300eb9dbf808ebd0e026`.
- Package `com.local.listentomusic`, version1.22.24/code345, not debuggable or test-only; ZIP/16KiB alignment and existing maintenance signature checks pass.
- Maintenance certificate: `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`, unchanged from1.22.19–23. This is not the missing historical signing key. The unchanged original-signer gate correctly fails and no repository release is promoted.

### Native A55 acceptance

`green-native-24-02.log` and its same-named capture folder record a PASS on the unchanged1080×2340/API36 A55 profile. The installed APK SHA-256 matches the candidate before and after the test.

- Library → real Miku video → dock → Now Playing → **icon-center** fullscreen tap: PASS, without the diagnostic lower-edge switch.
- Colored unused Fit margins and visible fullscreen DEV: PASS. The sampled bands changed from0% colored on1.22.22 to100% on1.22.24. This is a local screenshot assertion, not whole-song/perceptual/HDR proof.
- Existing DEV summary, Copy/Technical action presence, Pick an element, actual `VIDEO_STAGE` selection and Back cancellation: PASS. Clipboard contents were not separately audited.
- Fullscreen → expanded → **icon-center Home** → Library, then dock → deliberate central header pull → Library: PASS.
- Actual native button centers are113,284,455,626,797,968px: five equal171px intervals. Captures were visually inspected; no DEV or handle clips the glyphs. `Check-ToolbarSpacing.py` rejects the old135/135/315/135/135px arrangement.
- Original DEV-off preference and both temporary app-ops (`MANAGE_EXTERNAL_STORAGE`, `SYSTEM_ALERT_WINDOW`) restored. No microphone permission, capture of other apps or device-data wipe.
- Native log SHA-256: `e866a13883dc1ded41cec11902064f5fd7eafecf7cc5ac1e15d12dc820afbd0d`. Final inspected expanded image SHA-256: `cc258f531c688b24b2abe4d1ae111c943e58cb7b30b232299f42d7ef1cdcc1b3`; fullscreen image: `0fa7625872100a32cf346e82a045226066002a4c6e1c596738a43624fe9df5a3`.

The first1.22.24 run reached the new native glow/DEV/inspection checks but stopped in test cleanup when a script variable collided with PowerShell's reserved HOME variable. Renaming that variable, without changing app code, assertions or timeouts, let the unchanged APK pass the complete sequence. Retain that failure and both captures. This was not an app crash.

Self-review compared the source changes with the saved1.22.22 baseline and retained1.22.23 source/candidate. No decoder, session/surface owner, quality constraint, permissions, alignment matcher, Stack save or desktop behavior changed. The invisible hit layer was removed, not its pull function. This is self-review, not independent review or security certification.

No old APK was overwritten and nothing was pushed, merged or published for this task. Physical HDR/speaker output, landscape/rotation/PiP regression on this exact build, universal crash freedom, and separate Stack alignment acceptance remain unverified. The original-signer release stays blocked and source remains `SOURCE_ONLY`. Follow-up documentation commits do not change the runtime commit or candidate bytes.
