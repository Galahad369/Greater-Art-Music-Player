# Frame-synchronized Ambient — 1.22.3 / code324

Status: private debug experiment. Production Ambient is unchanged. This is not
a release or a claim of zero display latency.

## Intent and boundary

Remove relative video-to-glow delay, rather than shorten the old palette timer.
One Media3 decoder produces one external texture. A single GL draw samples that
same latched frame for the native-aspect video and spatial reflected light.
Normal rendering has no CPU pixel readback, palette poll or temporal color fade.
The implementation follows the established [Media3 1.11.1 GL demo pattern](https://github.com/androidx/media/blob/1.11.1/demos/gl/src/main/java/androidx/media3/demo/gl/VideoProcessingGLSurfaceView.java).

All experiment code and its non-exported host are in `app/src/debug`.
The normal player, surface owner, artwork and production Ambient are not replaced.
No second decoder, quality cap, proxy playback or stretch mode is added.

Only explicit 8-bit SDR is admitted. Unknown precision, HDR and high-bit-depth
input stop the experiment before drawing. Refusal prevents a silent downgrade;
it is not HDR support or full function parity. Physical cadence, thermals,
battery, HDR and Dock/Expanded/Fullscreen handoffs remain adoption gates.

## Tests and evidence

Five JVM cases check proportional Fit/Crop geometry, color classification and
format refusal. Four real Android cases use generated 1280×720/30fps scene cuts:

- Compare actual foreground and glow pixels over repeated red/green/blue cuts.
- Deliberately retain a stale glow. The pixel comparison must detect mismatches
  even when frame timestamps match. This challenges the checker, not the old
  production implementation.
- Check pause, seek, Fit/Crop and rotation on one player. A white source square
  must remain square in native screenshots, not merely in layout calculations.
- Check that normal rendering does not collect per-frame diagnostic pixels.

Fixture SHA-256:
`94c7719a36c6b4242296ddf619f3a8ae6ff558fbe8197ed483d40c473ed07c9e`.
It contains rapid color cuts; use it for headless QA, not a human-facing demo.
Capture-only readback changes performance, so its timing is not a performance
benchmark. Device results and exact artifact identity must be recorded after
execution; a passing build alone is not visual acceptance.

## Local Java TLS recovery

The default Java21 trust store failed the official AGP POM with a PKIX error.
A separate Java process using `Windows-ROOT` and `NONE` fetched the correct POM
and rejected an untrusted self-signed HTTPS certificate. The pinned Gradle9.8.2
wrapper then completed its help task successfully.

```powershell
./build-personal-debug.ps1 -UseWindowsTrustedRoots -BuildOnly
```

The option applies only to this process and child JVMs; the helper restores the
caller's environment. It neither imports certificates nor disables TLS or
hostname validation. Existing conflicting trust-store options are rejected.
The helper always uses the pinned wrapper. Without `-BuildOnly`, debug outputs
go to an immutable candidate name under `build/candidates`, never `releases`.

References: [native Java Windows certificate stores](https://docs.oracle.com/en/java/javase/21/security/oracle-providers.html),
[Windows-root trust configuration](https://support.azul.com/hc/en-us/articles/16981081133588-Using-https-TLS-SSL-certificates-provided-by-the-Operating-System).

## Executed checks — November 3, 2026

- Full debug unit/lint/app/test and optimized R8 assembly passed. 500 JVM tests,
  zero failures/errors/skips. Debug lint: zero errors, 50 warnings and four hints;
  three warnings are from the private debug host. Later test-only repairs were
  rebuilt and linted separately; the final app bytes stayed unchanged.
- The final API36 run passed three of four instrumented cases. The positive
  probe observed 80 actual video/glow samples with zero dominant-color mismatch.
  Its stale-glow negative control detected 53 mismatches. Normal rendering
  collected no per-frame diagnostic pixels. This does not prove physical cadence,
  absolute display latency, battery use or a production performance improvement.
- The transport/geometry case is NOT accepted. Initial attempts exposed a queued
  frame after asynchronous pause and a screenshot taken during Android's rotation
  animation. The test now waits for the real GL viewport, drained paused frames
  and stable compositor screenshots, using the existing FullscreenRotationTest
  pattern. It retains the same square-pixel assertion and 20-second readiness
  bound, and adds a stopped-media-clock assertion. Failed captures are saved.
- Final native screenshots show a System UI ANR dialog over the video, which the
  geometry scan correctly refuses. Events also show phone/system-process ANRs
  before the test, plus a target-app startup timeout during that unstable boot.
  Do not relabel these runs as visual acceptance or universal crash freedom.
  The existing renderer/AVD settings were retained; only the owned emulator was
  stopped after evidence retrieval. No host configuration workaround was used.
- The exact debug APK passed manifest/version/certificate and 16KiB alignment
  checks. The non-exported prototype is absent from the optimized release
  mapping. No release signing, publishing or production adoption took place.
- Both retained app/test ZIP containers pass CRC validation. The repository's
  existing public privacy/secret audit passes. The process-only helper also
  passes actual environment-restoration and conflicting-trust-store rejection
  checks. These are bounded checks, not a security certification.

## Exact internal artifacts and evidence

Sibling workspace directory (outside this repository):
`output/ambient-frame-sync-20261103/`.

Final app: `candidate/GreaterArt-1.22.3-ambient-prototype.apk`, 27,136,009 bytes,
SHA-256 `cc9090a329e41eebe3aa79e7175384b1ccbe3cc8c473e10270e22b7df1c34dfd`.

Matching test APK: `candidate/GreaterArt-1.22.3-ambient-prototype-tests.apk`,
1,599,357 bytes, SHA-256
`4fe9c410db113d1e005020b8356a52565333d38993813f181602782900f63866`.

Both use the established certificate
`9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.
The runner checks the installed app's SHA against the exact input APK.
Source binding is a frozen working-tree checkpoint, not a CI attestation.

See `build-attempt*.log`, `checkpoint4/`, `runtime-attempt6/`,
`device-final-blocked/` and `debug-artifact-final-receipt.json`. Earlier failed
APKs are retained under `retained-attempt1/`, not overwritten or promoted.
The workflow correctly reports FAILED; no ready/promote step is permitted.

Next: repeat the unchanged final app/test pair on a stable emulator or real
device, inspect settled portrait/landscape screenshots, then qualify precision,
surface handoffs and physical performance. Do not enable this experiment or
replace verified 1.21.93 until those separate gates pass.
