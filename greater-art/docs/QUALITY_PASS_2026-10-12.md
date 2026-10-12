# Engineering integration and release evidence

## Scope and baseline

Baseline main df7f8de, verified sideload 1.21.32/code255. Integration source
1.21.37/code260 combines all unique work in PR #168 and #169. Both histories
remain parents; overlapping code is resolved behaviorally. No new worktree.
Owner requested convergence to main; this supersedes the supplied handoff's
no-merge preference, but does not authorize weakening protected checks.

## Implemented

- One persisted selector cycles Mini Window → native Android PiP → No Floating.
  Legacy Compact values remain native PiP. Audio-only PiP has no fake video panel.
- Queue search: Locate / input / Clear-Close, independent 48dp outer targets;
  Locate clears an obstructing filter before scrolling. Duplicate identity retained.
- Fullscreen lock follows hidden-controls visibility; unlock remains above blocker.
- Classic preserved; shared four-layer Adaptive and all localized preferences
  survive integration in Activity and overlay presentations.
- PiP startup waits for the asynchronously connected video and destination frame,
  with bounded failure exit. No silent first-onResume request loss.
- Independent persisted High Contrast palette with six-language Settings label.
  Normal text pairs ≥7:1; tested control/outline pairs ≥3:1 in light and dark.
  Disabled alpha and actual wallpaper contrast still require visual acceptance.
- No bitrate/resolution/FPS limit, network permission, extra Ambient decoder,
  signing-key change, or production shared-clock adoption.

## Integration matrix

| Scope | Evidence / status |
| --- | --- |
| Combined source | 358 JVM tests pass, lint zero errors / 41 warnings / 3 hints |
| Ambient + real video + two Stack voices | Combined API36 regressions pass |
| Native PiP + No Floating Home | PASS: genuine launch, shared controller retained, native1280×720 unchanged |
| Mini/fullscreen/search/lock | Existing regression suite passes; exhaustive repeated physical interaction still NOT RUN |
| High Contrast | Measured palette regression tests pass; actual full-screen screenshots pending |
| Physical Samsung, Bluetooth, acoustic, thermal, API26 | NOT RUN; no matching hardware here |

## Security and release boundary

This is partial evidence, not a complete security assessment. Official references:
[MASVS](https://mas.owasp.org/MASVS/) and [MASTG](https://mas.owasp.org/MASTG/).

| Control subset | Evidence | Residual work |
| --- | --- | --- |
| PLATFORM: component boundaries | Launcher exported; internal player Activities/service/provider non-exported | Cross-app MediaSession and provider authorization tests remain |
| PRIVACY: networking | Manifest explicitly removes INTERNET; final APK permission inspection pending | Full dependency runtime/privacy assessment |
| STORAGE: URI grant lifecycle | Stage C selected-folder playback/cache readers retained | Sidecars, sharing/deletion, exclusions, legacy ID migration parity; broad permission retained |
| CODE: parsers/resources | Bounded scans/cache/analysis; tests, lint and protected scans | Independent malformed input/resource-exhaustion assessment |
| CRYPTO: updates | Existing sideload certificate pinned; exact-source artifact inspection required | Owner-approved production key/channel and signed release proof (#23) |
| ISMS / ISO | Existing scope, risk register and 93-control templates retained | Owner approvals, operating evidence, independent audit (#162) |
| Historical disclosure | Current audit is not proof all historical paths are gone | Coordinated separately authorized rewrite/backup/protected-tag handling (#24) |

No security issue is closed merely to hide it. #146 shared-clock prototype stays
default-off: production video routing, pitch-preserving correction, deadline and
physical acoustic/thermal evidence are adoption gates. #147 physical QA stays open.
#149 gains High Contrast; accent-only and launch-flash acceptance remain.
#148 gains no falsely claimed full parity. #150 remains the factual tracker.

## External Vercel status

PR #168 Android/security gates passed; its Vercel preview status failed separately.
Main protection requires Android/version/privacy/dependency/CodeQL, not Vercel.
No authenticated Vercel tool/CLI is present in this environment, so its external
git_info_fail cannot be diagnosed from private deployment logs here. No Vercel
check, protection or project configuration is disabled to hide that failure.

## Workflow prevention

- Windows checkout has `gradlew` and the wrapper JAR, not `gradlew.bat`.
  Invoke Java21 with org.gradle.wrapper.GradleWrapperMain; do not invent tooling.
- Read large handoffs in bounded sections. A truncated command output is not file
  content and must never be used as replacement text.
- ActivityScenario forcibly adds CLEAR_TASK even with explicit flags. Use an
  ActivityMonitor around a genuine app launch for multi-Activity continuity.
  A destroyed test ViewModel is not evidence the session crashed. MediaController
  size/position calls must run on its application thread, including in tests.
- Retain unrelated historical-APK deletions outside implementation staging.
- Release filenames do not prove version, signature, ZIP integrity or provenance.
  Build the committed source, inspect, install and hash before finalization.

## Final artifact — October 13

- Source b47d96c620a9ed5db0fd8c8e1e92df8857c99c6e, 1.21.37/code260.
- Java21 committed-source assemble passes; 358 JVM tests; 23 API36 tests:
  14 combined playback/Ambient/UI tests (72.031s), nine URI/discovery/cache tests
  with MANAGE_EXTERNAL_STORAGE denied (1.461s). Original AppOps restored.
- APK releases/GreaterArt-1.21.37.apk, 27,862,370 bytes, SHA-256
  4047097d81f999fe8f62d843e753975746f50d2b1d409362cd96ba4d78694fb6.
- Package com.local.listentomusic, API26 minimum/API37 target, no INTERNET
  permission. Pinned certificate 9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf;
  signature verification and 16KiB ZIP alignment pass. Upgrade install succeeds.
- Existing personal debug signing identity preserved. This is an emulator-verified
  sideload candidate, not the separate production-signing workstream.
- No app fatal in the inspected crash buffer. Physical/thermal/API26 acceptance,
  production signing, full storage parity and independent security assessment
  remain open; no universal bug-free/crash-free claim.
