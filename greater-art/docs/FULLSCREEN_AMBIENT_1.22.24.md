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

All542 JVM tests pass, with zero failures/errors/skips. Lint has zero errors,54 warnings and6 hints. Android instrumentation compiles/packages successfully; compilation is not a rendered-test pass. The new optimized assembly, exact-file identity and fresh native A55 smoke are pending for1.22.24. No old APK is overwritten, no publishing is authorized, and the historical signer gate stays blocked. Physical HDR, speaker output and the separate Stack alignment gates are not qualified here.
