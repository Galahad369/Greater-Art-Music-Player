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
- Optimized assembly and maintenance-key identity/ZIP/alignment checks: PASS. Exact candidate `App/output/fullscreen-ambient-toolbar-20261112/optimized-local/GreaterArt-1.22.23.apk`, 5,579,536 bytes, SHA-256 `38c874c501113cce068d2558a778a666d908b683bea1ff6cb0c5deb19986ae96`, runtime commit `5d19aff`.
- A551.22.22 baseline reproduced black Fit margins (zero colored pixels in the measured margin bands), missing DEV, and toolbar spacing135/135/315/135/135px.
- A551.22.23 normal center-tap acceptance FAILED twice: fullscreen did not launch. Captures also show the old DEV badge overlapping the redistributed buttons. A separate lower-edge diagnostic at the same48dp button opens native fullscreen:100% of measured margin-band pixels have color; DEV, its existing actions and element picking work. This diagnostic is **not** normal-button acceptance. The overlapping40dp sibling pull region covers the icon center, but not the lower edge. Preserve all failures and the exact APK. [1.22.24](FULLSCREEN_AMBIENT_1.22.24.md) repairs that observed hit-test problem.
- Test-harness correction: API36 reports `topResumedActivity=` / `ResumedActivity:`, not the assumed `mResumedActivity:`. ActivityTaskManager confirmed the earlier baseline launch. Corrected field recognition retained the actual Activity check and reproduced the original Ambient failure. It did not weaken the app assertions.

The historical signer is still unavailable. Use only the already approved maintenance key for a separate local candidate; do not repin the historical signature, overwrite a previous APK, uninstall the app, publish or claim physical HDR/output acceptance.
