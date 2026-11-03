# Android maintenance verification — 1.22.1 / code322

Date: **3 November 2026**. **Frozen candidate record — SOURCE_ONLY.** Debug
verification and optimized-artifact inspection are complete. Optimized native
return/Mini acceptance remains incomplete. This record does not promote the APK
or replace the verified1.21.93 release.

## Source and signing boundary

- Application ID: `com.local.listentomusic`.
- Version: `1.22.1`, code `322`.
- Exact executable source commit: `86c2e1cce5e62a86d6289bfa6b2befc11f07b2c9`.
- Production `app/src/main` tree: `3097172f031725dd475223e37181a21a7bbb2faf`,
  unchanged from1.21.98. Test-fixture and version changes do not replace the
  production playback engine.
- Existing certificate SHA-256:
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.

The certificate above identifies the existing Android Debug compatibility
sideload key, not a production publisher identity. No new signing identity is
authorized by this record.

## Frozen optimized artifact

- Local path: sibling `App/output/android-release-1.22.1-20261103/GreaterArt-1.22.1.apk`.
- Size: **5,495,125 bytes**.
- SHA-256: `e952cad07dcdf2b7c381886cc3b989b3d63772cc2ab22ab595e9fe9d48222e7e`.
- Exact manifest: `com.local.listentomusic`,1.22.1/code322, non-debuggable,
  not testOnly; existing certificate above verified.
- ZIP CRC and16KiB alignment pass. Exported-component policy passes; no INTERNET
  permission. Broad storage remains declared.
- R8 assembly passed in3m50s. Mapping SHA-256:
  `acee9d547fa8237106832a0fb777da0db43730453a9d4b099c37bbcaa4a7d739`.
- `release-trust.json` and139-record `release-dependencies.json` are retained
  beside the APK. The inventory is not a full vulnerability assessment or SBOM.
- These exact bytes installed as an upgrade without data clearing and reached
  the native Library after a4.4-second cold launch. No optimized video-return
  acceptance is inferred from that launch.

Shared output remains disabled. Primary source resolution, bitrate, frame rate
and existing functions are not reduced. No new network permission is introduced.
Broad storage access remains declared; this is not a completed permission
cutover, security certification or physical-device acceptance.

## Fixture repairs

- Preserve fullscreen auto-hide and use the normal reveal gesture before
  checking hidden controls. Keep orientation, Fit/Crop, safe-bound, gesture,
  player/surface and source-size assertions.
- Complete the native Settings permission-return test without depending on
  unsupported virtual-provider text search. Keep its package/label checks,
  five-second readiness gate and actual permission/return assertions.
- Replace global-idle-dependent Activity operations during live Stack/ambient
  playback with direct main-thread operations. Retain actual colour, primary
  identity,1280×720 geometry, pause and two-voice Stack checks and15-second
  condition deadlines.
- Finish the owned Activity before calling Scenario cleanup, so continuing
  Ambient frames cannot prevent the cleanup's global-idle wait. Verify actual
  destruction, stop owned playback/overlay services, clear the fixture session,
  restore original preferences/playback state and remove only the companion file
  created by this test. Do not pause playback or disable animations to force the
  live assertions to pass.

## Completed build and debug-device checks

Java21 / Gradle9.8.2 builds were run serially with the emulator:

- Debug app/test, unit and lint build: **4m17s**.
- Final fixture-cleanup build: **1m12s**.
- JVM tests: **495 passed**, zero failures, errors or skips.
- Lint: **0 errors,47 warnings,4 hints**. Warnings were not hidden.
- Host-owned Trust matrix: **9/9 passed** — denied7, granted1, revoked1.
  The runner reports that original grants were restored.
- Combined debug UI/geometry suite: **33/33 passed in367.734s**, without host
  intervention. This includes the previously stalled Stack/ambient case and its
  final Activity/service cleanup. The earlier1.21.98 failures remain evidence;
  they are not relabelled as successful runs.

The owned `GreaterArt_Scroll_QA36` API36 emulator was also operated through the
public Launcher, Library filter and dock. Actual expanded-player green/red video,
portrait fullscreen Fit and a settled2340×1080 landscape red frame were captured
and visually inspected. These native steps used the debug installation; they do
not prove that the separately optimized APK has the same runtime behaviour.

## Host failure and remaining acceptance

Android Back from the landscape debug view was followed by an emulator host
crash. Windows recorded `qemu-system-x86_64-headless.exe` with native access
violation `0xc0000005`, unknown faulting module, on3 November around00:59.
This identifies a host emulator failure, not its underlying cause or a Greater
Art Java crash. It does not validate the interrupted return path.

The user declined a Vulkan-disabled retry. Do not change host graphics settings,
lower playback quality or treat that declined workaround as an acceptance pass.

The next optimized smoke command did not start: Windows returned0xC0000142
(-1073741502), a process initialization failure. No playback steps or screenshots
from that failed command count as evidence. At the subsequent convergence
handoff, no emulator was running and about0.8GiB of RAM was free. Resource pressure
is a possible contributor, not a demonstrated root cause.

Remaining before promotion: complete and inspect optimized real-video,
portrait-fullscreen return and Mini continuity with the unchanged AVD
configuration. Direct landscape return also remains limited by the recorded
QEMU host failure. The approved dependency convergence proceeds separately as
1.22.2; it must not rebuild, overwrite or relabel this frozen1.22.1 APK.

No physical-phone, acoustic Stack alignment, high-resolution shared-output
parity, production signing or universal crash-freedom claim is made.

## Retained evidence and prevention

Local evidence is under the sibling workspace directory
`App/output/android-release-1.22.1-20261103/`: `debug-build.log`,
`fixture-cleanup-build.log`, `trust-matrix-run.log`, `ui-regressions.log`, native
screenshots/hierarchies, `emulator-host-crash.json`, `release-build.log`,
`release-trust.json`, `release-dependencies.json` and `release-mapping.txt`.

- Do not use global UI idleness as proof that a continuously rendered player is
  ready. Wait for the required player, lifecycle or geometry state instead.
- Keep fixture assertions and deadlines; repair the synchronization boundary,
  not the expected production behaviour.
- Stop and restore owned fixture state before the next case. Do not delete
  original media, reuse stale APK/test pairs or overwrite a versioned release.
- Run Gradle and the emulator serially. Distinguish an app exception, failed
  assertion, fixture stall and host emulator crash in the report.
