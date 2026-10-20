# Quality follow-up — October 19, 2026

## Reviewed baseline and branch decisions

Baseline: main `9e34f40dfc43069d652deac67cdb3f6b34c6472b`, Android
1.21.55/code278, SOURCE_ONLY. Existing verified Android/desktop artifacts are
unchanged. The primary checkout contains unique local commits and uncommitted
release/handoff work; this follow-up uses a separate, clean checkout of main.

| Work | Review result | Integration decision |
| --- | --- | --- |
| #189 | Merged; reported Android, desktop, version and security checks passed | Preserve; test actual fullscreen behavior |
| #187 | Zero changed files, initial-plan commit only; targets old Grok base, not main | No implementation to merge; proposed 1.21.55 is consumed |
| #188 | Zero changed files, initial-plan commit only; targets old Grok base, not main | Implement diagnostics in a fresh version after the test-harness repair |
| `fix/fullscreen-control-visibility` | Superseded by merged changes | Do not re-import older gesture code |
| #174 | Draft; historical-privacy check fails | Keep separate and blocked; no history/ref/signature changes |

## Fullscreen harness repair — 1.21.56/code279

The baseline fails `compileDebugAndroidTestKotlin`: `createEmptyComposeRule`
cannot host `setContent`. A passing ordinary app build does not catch this.
Component toolbar tests now have a hosting rule; the real Activity/surface test
keeps its empty rule. Android CI also compiles `assembleDebugAndroidTest`.

Screenshot timing is another distinct problem: Android reports a landscape
configuration before the system compositor finishes its rotation animation.
Wait for root relayout and stable paused-fixture pixels; do not label a tilted
transition capture a final-layout defect or final-layout pass.

Local Java21 gate: 395 JVM tests passed with no failures/errors/skips; lint
reports zero errors, 45 warnings and three hints. App and instrumentation build.
API36 run: nine fullscreen/audio/toolbar tests pass in the 56.499-second suite;
one fixture-gated PiP test initially skips. Its explicit fixture-enabled rerun
passes in 10.015 seconds. Always inspect assumption/skip status codes as well
as JUnit's `OK`, which can include skipped tests.

The final stable-compositor screenshot helper passes its real-video rerun in
18.253s. Portrait/landscape screenshots are visually inspected and the rotation
recording is collected under `app/build/verification/quality-followup`. The video
keeps its native 1280x720 dimensions and FIT geometry. This is bounded API36
evidence, not proof of no black frames during every possible handoff.

The initial Android14/15 SDK download failed certificate validation. See the
command-scoped Windows trust-store remedy and subsequent matrix results below.

## Diagnostics implementation — 1.21.57/code280

Implements the Grok-proposed #188 intent without importing its stale empty branch.
Six/seven explicit localized summary lines replace report-string filtering. Both
hosts keep complete technical/region payloads on Copy; technical details start
collapsed. Existing picker, selected element, warning badge, permission action,
explicit region overlays and one-tap disabling remain available. A normal saved
process exit is not labelled as a crash.

Local gate: 400 JVM tests, zero lint errors (45 warnings/three hints), both APK
builds. Twelve API36 diagnostics/accessibility tests pass in 38.953s; final five
diagnostics tests after actual-window sizing pass in 18.994s. The latter includes
320x360dp at 1.7x font scale in the system-overlay host, complete clipboard output
and explicit region IDs after closing the panel. Summary/details screenshots are
visually inspected. No publication or verified-release promotion is implied.

## #187 scope review (existing implementation)

- SAF selection, persistable access and URI-backed production Library scanning
  already exist (`scopedMediaTreeUri`, `ScopedMediaDiscovery`, `rescan`). Do not
  install a competing file-only tree walker or silently fall back to broad
  storage after revocation. Broad-access removal remains a separate parity gate.
- Nodes already has strength-weighted attraction, user-configurable distance,
  repulsion and damping, a bounded background solver and weighted hub origin.
  Preserve drag activation and pager gesture ownership when refining focus.
- Artwork-adaptive Material accents and release minification are implemented in
  the separately versioned follow-up below, not inferred from the draft's title.

## Reviewed audit implementation — 1.21.58/code281 (October 19–20)

Credit: Grok's #187 proposal identifies the four review areas. Existing SAF/Nodes
implementations are preserved rather than importing stale or duplicate code.

- Ambient/Adaptive backgrounds reuse cached current artwork or the existing
  sparse video-color stream for three Material accent roles. A coherent palette
  is latched per media/presentation; previous-media colors are rejected. There is
  no extra decoder, extraction loop, player prepare/seek or source-quality cap.
- Decorative artwork reads are memory/disk-only. Their scheduler key is distinct
  from actual UI generation requests, so a decorative miss cannot suppress a real
  thumbnail request. Cancellation and writer/clear synchronization remain intact.
- Zero intensity preserves the exact theme; High contrast/Monochrome remain fixed.
  OLED surfaces remain black and existing foreground contrast is checked.
- Release-only R8 uses manifest/consumer keep rules and preserves persisted enum
  identities. Debug stays unminified; resource shrinking remains off. CI builds
  release and instrumentation APKs as well as running JVM/lint checks.
- Deterministic graph tests cover finite extreme options, weighted hub placement
  and actual link-distance effects; no speculative solver rewrite is introduced.

Final local gate: **406 JVM tests, zero failures/errors/skips; lint zero errors,
46 warnings and three hints; debug, instrumentation and R8 release assembly pass**.
The unsigned minified candidate is 5,435,641 bytes. Its disposable emulator-signed
copy verifies as 1.21.58/code281 with the existing pinned debug certificate and
16KiB ZIP alignment. It is not a production-signed or promoted release.
Minified API36 smoke: upgrade installation, cold launch, populated Library,
persisted Settings/Playback preferences, actual Media3 video playback and the
live-video dock render successfully; Library/dock screenshots are inspected.
The inspected app crash buffer has no fatal. This is not full minified feature
parity acceptance. The ordinary debug APK and original overlay grant are restored.

API36: 31 of the 32-test combined suite pass, including live Ambient/Stack video,
cache-only reads, diagnostics, accessibility, audio, narrow toolbars and PiP. The
new seek check initially fails because the prior test leaves playback near EOF,
where forward seeking is correctly clamped. The fixture now disables resume and
sets/restores its own seek interval. Its full gesture/rotation rerun passes in
40.880s; the same test with a temporary double cutout passes in 22.012s. Lock,
side seek, pinch/pan, paused auto-hide and retained player/surface/native video are
asserted. This is 31 passes plus successful targeted reruns, not a claimed clean
single 32-test run. The original cutout configuration is restored afterward.

Android15/API35: eight audio/narrow-toolbar component tests pass. The two real
Activity tests initially fail in ActivityScenario cleanup while a first-install
notification permission dialog is active. Logs identify the permission Activity,
not a fatal app exception. After handling that prompt on the isolated QA device,
both fullscreen gesture/rotation and native PiP tests pass their targeted rerun
in 138.175s. Temporary storage/overlay/notification grants are restored. This is
eight passes plus two successful reruns, not one clean ten-test execution.
Android14/API34 image is installed and its isolated QA device is booting; no
Android14 runtime result is claimed yet.

### Build and test mistakes to avoid

1. Freeze Kotlin sources while Gradle analyzes/builds them. Do not edit under an
   active lint/compiler run or mistake a resulting internal compiler error for an
   app regression.
2. JUnit `OK` can include assumption skips; reject instrumentation status -2/-3/-4
   and inspect expected test counts. ADB's exit code alone is not acceptance.
3. `wait-for-device` does not mean Android's package service is ready. Wait for
   `sys.boot_completed=1` before installation. Fixture-enabled runs need
   `-e ambientVideoFixture true` and explicit emulator-only overlay/storage grants;
   missing overlay permission means no visible video owner, not broken sampling.
   Fresh installations also need their notification prompt handled before an
   ActivityScenario test; its cleanup exception can mask the original interruption.
   Restore all temporary grants after the run.
4. Stateful fixture tests must set and restore resume/seek preferences. Do not
   diagnose an EOF-clamped seek as a broken gesture without checking positions.
5. This workstation's Java trust store lacks the platform's existing trusted
   certificate chain. R8's first uncached Compose mapping dependency and SDK
   manifests fail PKIX validation until the **child build process** uses Windows
   trust. With no existing custom `JAVA_TOOL_OPTIONS`, set only for that process:

   ```powershell
   if ($env:JAVA_TOOL_OPTIONS) { throw 'Preserve existing custom Java options' }
   $env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
   ```

   Then run the Java21 wrapper/SDK Manager normally. TLS certificate and hostname
   validation stay enabled; no global trust-store import, insecure download or
   certificate bypass is used. Kotlin tooling resolves from Maven Central, not
   an unnecessary first Google repository probe. Once cached, the offline full
   gate passes too.

Primary checkout changes, desktop artifacts and the immutable verified 1.21.39
APK remain untouched. Physical-phone acceptance, complete interruption/flicker
coverage, production signing and the separate #174 privacy gate remain open.

## #174 security failure and safe options

Read-only review of run 39048218582 reports 21 findings. They include workstation
home-path strings in reachable historical build/lint logs and source/test files,
plus current-file findings in HANDOFF and a pattern test. A successful rewrite
rehearsal does not remove those strings from published reachable history.

1. Fix current-file literals using synthetic fixtures and repository-relative
   instructions, then rerun the strict history-aware check on that branch.
2. Inventory affected blobs/refs and distinguish real exposure from synthetic
   pattern cases. Do not publish the actual personal paths in new reports.
3. If reachable historical exposure remains, prepare a backed-up rewrite plan
   covering all affected refs, release/tag impact and collaborator resynchronization.
4. Obtain explicit owner approval before any published-history rewrite, protected
   ref change or retag. Rehearse offline first and retain verification evidence.

Do not weaken detection, ignore the failing check, force-merge #174 or represent
ordinary main's passing audit as resolution of the separate privacy incident.
