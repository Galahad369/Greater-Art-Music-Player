# Android candidate — 1.21.98 / code321

Date: 2 November 2026. **Built and artifact-checked. Not a promoted verified
release: the permission-return fixture still fails and the Stack/ambient test
required host intervention.**

## Exact artifact

- Local delivery file: sibling `App/output/GreaterArt-1.21.98.apk`.
- Package: `com.local.listentomusic`; version1.21.98, code321.
- Size: 5,495,129 bytes.
- SHA-256: `36d5279c58b155f70cc7a47ca3af348e285df1e8bf5197eeca43f9a14ef8ba02`.
- Source commit: `85dee789c253720e2d13e0982fda245e338acb60`.
- Production `app/src/main` tree: `3097172f031725dd475223e37181a21a7bbb2faf`.
- R8-minified release variant; non-debuggable and not test-only.
- Existing signing certificate SHA-256:
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.

This is the existing Android Debug compatibility sideload identity, not a
production publisher certificate. No signing key was generated, exported or
replaced. All Files Access remains declared. No security certification, physical
device acceptance, publication or independent source-to-binary attestation is
implied. The checkout also contains preserved unrelated desktop edits.

## Changes

Only device-test fixtures and version metadata change from1.21.97:

- Reveal auto-hidden fullscreen chrome with a normal single side tap before the
  post-theme assertions. Assert that this does not seek. Keep the existing
  orientation, safe bounds, player/surface, gestures and source-size checks.
- Traverse visible accessibility children for the installed app label in native
  Android Settings, rather than relying on virtual-provider text search. Keep
  the Settings package, five-second readiness gate and grant/return assertions.

Production playback, strict Fit/Crop, reflections, timers, permissions and source
quality are unchanged. Shared output remains disabled.

## Completed checks

- Java21 / Gradle9.8.2, offline and one worker: debug app/test assembly,
  `testDebugUnitTest` and `lintDebug` pass in6m12s. 495 JVM tests pass with no
  failures, errors or skips. Lint reports0 errors,47 warnings and4 hints.
- Optimized signed `assembleRelease` passes in6m07s with the emulator stopped.
- Actual APK package/version/code, signer, signature, non-debuggable state,
  reviewed exports and no INTERNET permission pass the artifact policy.
  ZIP CRC and16KiB ZIP alignment pass. The26 artifact-policy tests also pass.
- Fullscreen rotation passes1/1 in36.652s on the owned API36 emulator.
- Isolated native geometry passes8/8 in4.946s. These are native measured
  frame/surface checks, not proof of every decoded pixel on physical hardware.
- Isolated Ambient surface journeys pass2/2 in129.013s without host UI
  interaction. They cover actual video continuity through Library, Nodes,
  Stack backgrounds and the expanded system player.
- 31 actual screenshots were retained; selected portrait/landscape fullscreen,
  default/Adaptive expanded player, light Library and compact light-audio
  captures were visually inspected. The landscape fixture capture follows
  pinch/pan and is not a fresh unzoomed Fit baseline.

## Failed or incomplete gates

- Trust denied suite:6/7 in20.414s. `TrustPrivacyUiTest.kt:99` still times out
  at the five-second native Settings readiness gate, before grant/return.
  The visible Settings app label is present in the later native capture.
  Child traversal alone did not solve the observed readiness failure.
  The host runner aborts before granted/revoked cases and restores input grants.
- Mixed32-case UI/geometry run:31/32 in1559.908s, **not accepted as a clean
  pass**. The Stack/ambient case waited indefinitely for main-thread idle at
  `ActivityScenario.onActivity`, line88. Host media-pause and foreground launch
  unblocked it; the following default-player Ambient case then timed out.
  The separate successful Ambient run does not erase this failure or establish
  Stack fixture reliability.
- Diagnostic debugger-wait attempts caused separate startup-deadline ANRs.
  These attempts are not runtime acceptance evidence. The Java thread snapshot
  was resumed and its temporary debugger initialization file removed. No device
  root, data wipe, quality reduction or timeout extension was used.
- Exact optimized upgrade succeeds without clearing data. After a System UI ANR,
  one Wait action restores Library/Settings. Settings displays1.21.98 and the
  existing MediaSession plays the selected cover fixture. The first expanded
  screenshot has a black video stage; fullscreen later displays actual video in
  portrait Fit. The rotation request reaches2340x1080/ROTATION_90, but the captured
  frame is still portrait during transition, not accepted landscape evidence.
- Before fullscreen return/Mini acceptance, the emulator disconnects and its
  managed session exits1. Windows events1000/1001 at23:27–23:28 record
  `qemu-system-x86_64-headless.exe` failing with `0xc0000005`; faulting module is
  unknown. This proves a host emulator crash, not its cause or an app Java crash.
  No further boot or host remediation was attempted. Optimized smoke remains
  partial, not passed. Preserve `optimized-*.png/xml` and the emulator log.
- On the next healthy QA resume, restore the temporarily changed floating mode
  from Mini Window to its recorded original No Floating. The emulator disconnected
  before restoration. No source media or app data was deleted.

Keep SOURCE_ONLY and verified1.21.93 unchanged. Do not put this candidate into
`releases/`, claim crash freedom, or promote it solely because assembly passes.

## Evidence and prevention

Sibling `App/output/android-trust-20261031/v1.21.98/` contains the frozen matching
debug/test APK pair, build/device logs, screenshots, thread snapshot and exact
artifact receipt `release-1.21.98-trust.json`. Exact R8 mapping is
`release-1.21.98-mapping.txt`, SHA-256
`acee9d547fa8237106832a0fb777da0db43730453a9d4b099c37bbcaa4a7d739`.

- Quote the entire PowerShell argument `'-Pkotlin.compiler.execution.strategy=in-process'`.
  The first unquoted attempt split the property and failed task selection;
  `release-build.log` retains that failure. `release-build-2.log` is the actual
  successful build. No source or signing input changed between attempts.
- Run Gradle and the emulator serially. Keep the emulator in a managed foreground
  session; do not depend on a detached helper lifetime.
- Do not run external accessibility automation while instrumentation owns its
  automation service. Capture native hierarchies after the instrumented run.
- Distinguish a completed assertion, a fixture stall, host-assisted recovery,
  and an actual production crash. Keep failed evidence rather than replacing it.
- A later executable/test repair must use the next version under VERSION_RULES;
  do not silently change this committed version or rename an old APK.

The superseded1.21.97 candidate is retained byte-for-byte at sibling
`App/output/retained-artifacts/GreaterArt-1.21.97-candidate.apk`, SHA-256
`10ffd6525215af8522e3e577b130f56741b00488649d655338e5d7be4d772de9`.
Only1.21.98 remains as a loose delivery APK in `App/output/`. Immutable releases
and dated QA evidence were not removed.
