# Native fullscreen Ambient and DEV — 1.22.23 / code 344

## Scope and cause

The owner approved automatic sampled Ambient in fullscreen's unused space, with native video and HDR unchanged. This is not a request to move fullscreen onto the SDR frame-synced GL compositor.

Fullscreen explicitly disabled the existing backdrop and painted two full-viewport black layers above it. Its separate Activity also omitted the existing inspector host and developer panel. The normal toolbar grouped three actions at each side of one large spacer.

## Changes

- Expose the existing sampled backdrop through native fullscreen Fit margins. Retain the black backdrop of the ordinary non-immersive video stage.
- Load only cached artwork as the initial fallback, independently of the browsing wallpaper. Existing SurfaceView PixelCopy sampling updates the palette without a second decoder or filtering the source video. Samples are periodic, not same-frame beams or zero-latency fullscreen color.
- Restore the existing concise DEV summary, complete report, technical details and element picking. Report `FULLSCREEN`, hide DEV in PiP, and place its badge below measured toolbar bounds. Back cancels inspection before exiting fullscreen.
- Distribute top actions using Compose's existing `Arrangement.SpaceBetween`. Keep every callback, 48 dp target, narrow-width fallback, lock anchor and contrast policy.

No changes to the decoder, playback/session ownership, media constraints, source resolution, bitrate, frame rate, HDR route, Fit/Crop sizing, pinch/pan, sound/picture matching or desktop app.

## Verification state

Evidence: `App/output/fullscreen-ambient-toolbar-20261112/`.

- Build preflight: PASS, existing JDK 21 and API 37 SDK. Remote main still `910351f`; 1.22.23/code344 was free.
- Baseline source saved by the selected workflow. The initial native baseline attempt could not run because the A55 emulator exited. This is a host/setup failure, not an observed app assertion failure.
- Extended the existing rendered toolbar test to check equal action-center intervals while retaining the transparent-background, callback, target-size and fullscreen-scrim assertions.
- Production compile and all 542 JVM tests: PASS, zero failures/errors/skips. Lint: zero errors, 52 warnings and 5 hints. Android test compilation/package: PASS. The first test compile found an unnecessary import of a Compose member as an extension; removing that import fixed compilation before the versioned commit. Assertions were not relaxed.
- Optimized assembly, signing, exact APK identity, rendered test execution and native A55 smoke: pending. Do not infer these results from source checks or test compilation.

The historical signer is still unavailable. Use only the already approved maintenance key for a separate local candidate; do not repin the historical signature, overwrite a previous APK, uninstall the app, publish or claim physical HDR/output acceptance.
