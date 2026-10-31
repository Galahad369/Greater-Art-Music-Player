# Android trust baseline — integration handoff

Status as of31 October: **integrating1.21.95/code318; full debug build passed,
final verification paused for renewed concurrent playback edits**. User initially confirmed the other Greater Art writer was paused. Preserve
its1.21.94 reflection work in local commit6378014, then use the next unused
identity for Trust & privacy. Keep the existing signing key and all old APKs.
No branch switch, remote push or desktop rebuild is required.

##31 October continuation

- Preserved the other writer's14-file1.21.94 change in local commit6378014.
  The next integration source identity is1.21.95/code318, not yet committed or
  released. Existing1.21.93 release and1.21.94 internal test bytes are unchanged.
- Replaced an unavailable Reader helper with an explicit8192-character bounded
  read loop. Added real PackageManager/signature tests, deny/grant/revoke checks
  that restore prior app-ops, and an Android Settings return/refresh UI case.
  These new device cases have not yet compiled or run; do not count them as passes.
-23/23 artifact-policy tests pass; official actionlint passes the three edited
  workflows. Current-tree and6378014 commit-range version validation pass when
  Python is run with `-X utf8`. A first default-codepage invocation failed to
  decode Unicode Git output; no validator assertions were changed to hide it.
- Host memory varied from270MiB to920MiB and back to300MiB. The running device
  is CampusPlayMatch_API36/emulator-5572, a separate project's emulator. Its
  unrelated Gradle/compiler daemons also remain resident. Do not terminate those
  processes or close user apps without approval. User subsequently approved
  closing that emulator. Confirmed its exact AVD identity and used `emu kill`;
  ADB now reports no running device, with apps/data preserved. Free RAM briefly
  reached1.19GiB, then fell to430MiB. Asked separately whether the two now-idle
  CampusPlayMatch Gradle/Kotlin daemons may be stopped; do not terminate them
  without that approval. Still require enough available memory (roughly3GiB).
- No new Gradle build, emulator installation or APK promotion was attempted under
  this pressure. Resume with serial build/device checks below after resources
  are available. Keep the established signing key; do not use the older
  build-personal-debug.ps1 output naming/SDK assumptions for this optimized release.

## Serial verification after process cleanup

- With explicit approval, stopped only the two identified idle CampusPlayMatch
  Java daemons after checking their process names/command lines. No source,
  caches, original media or emulator data was deleted. RAM remained constrained.
- Used Java21/Gradle9.8.2 offline, one worker, no parallel tasks, in-process Kotlin
  and a1GiB build JVM. The emulator was stopped during builds. First complete
  build passed in6m59s. Fixed two new lint findings without suppression: inspect
  a permission declaration as text on older APIs, and use primitive Compose state.
  Final debug app/test build passed in4m04s, with480 JVM tests, no failures/errors/
  skips and lint0 errors/47 existing warnings/4 hints.
- Actual debug manifest inspection revealed Compose's preview and test Activity
  exports. Allow only those exact name/type/permission tuples, only in debuggable
  development APKs. Keep all non-debuggable/distribution gates strict.26/26
  artifact-policy tests pass, including the debug-only exception boundaries.
- Final debug APK:28,219,785 bytes, SHA-256
  `10f081f8e489558bc34cc4a439a50b6f0ef492425d795dfcd6a75668b29b6a17`.
  Matching test APK SHA-256
  `bf5424c00da472a262c0c597d740d19c358f23c40199d991b2106930bb2a5a4f`.
  Both are preserved under sibling `output/android-trust-20261031/`.
- Start only GreaterArt_Scroll_QA36/emulator-5570, API36, effective1080x2340/
  450dpi. Same-key upgrade from317 to318 and matching-test installation passed,
  with user data preserved. Test APK was ART-compiled before instrumentation.
- First seven-case trust invocation completed no test: instrumentation reported
  process crash, while Android recorded a startup ANR, not a Java exception.
  Preserve its log and trace. Main was in ART DexFileVerifier; scheduler wait was
  approximately21.9s versus1.3s CPU. The trace also records System UI, phone, Gboard,
  Google service startup ANRs and a Bluetooth system crash during emulator boot.
  Read only the specific app ANR via temporary debug-emulator root ADB, then
  restore non-root ADB. Do not label this a passed run or app cold-start acceptance.
- Precompile the unchanged debug app and rerun the same assertions/timeouts after
  boot settles. This extra debug-target preparation is
  disclosed; it cannot prove normal release cold-start performance.

## Permission-harness correction and renewed ownership blocker

- The rerun passed installed identity, then the in-process permission-revocation
  test terminated instrumentation. Android exit-info explicitly reports reason13,
  `MANAGE_EXTERNAL_STORAGE changed`, not a Java exception. Confirmed in Android16
  [StorageManagerService](https://android.googlesource.com/platform/frameworks/base/+/android-16.0.0_r2/services/core/java/com/android/server/StorageManagerService.java#5087):
  revoking All Files Access kills the UID; granting it does not. Do not classify
  this platform action as a product crash or attempt to bypass it.
- Corrected the device test to assert externally configured native grant state.
  The UI-return test now requires host-configured denied storage, opens Settings,
  grants access and verifies the resumed display. It no longer revokes access
  from inside its own process. The host owns setup and restoration. Added a real
  Material Surface to the UI fixture so dark-theme captures have correct colours.
- `scripts/test-android-trust.ps1` targets only the named Greater Art QA emulator,
  checks installed version/code, refuses existing logs, configures deny/grant/
  revoke outside instrumentation, requires9 actual successful cases and restores
  the original grants in `finally`. PowerShell parsing passes; the corrected
  runner has not yet run on a device. Its strict pass checks are needed because
  ADB can return exit0 when instrumentation reports process failure.
- Restored original storage/overlay grants to allow and stopped only the owned
  QA emulator before compiling the harness. The3m19s compile passed, but another
  writer changed production video classes/resources during that build:
  `ui/NowPlayingScreen.kt`, `res/layout/background_video.xml`, new
  `ui/components/AspectPreservingPlayerView.kt` and `VideoContentGeometry.kt`.
  Those changes appeared at16:27, after the previously verified debug APK.
- Preserve these new edits. Ask the user to pause the responsible chat and choose
  inclusion/exclusion. Do not pair the new test output with the earlier debug app,
  stage another writer's source, or create a release from a changing tree. No new
  source commit, branch switch, push or release promotion was performed here.
- Resume with a fresh complete debug/test build after ownership/scope is resolved,
  then run the host trust matrix, SettingsNavigationTest and the27-case reflection/
  compact/toolbar/real-video/IME regression group. Stop the owned emulator before
  exact-source optimized assembly. Keep the existing key and all historical bytes.

The [cross-platform policy](../../security/TRUST_BASELINE.md) is the canonical
description of claims, channels, boundaries and remaining release blockers.

## Changes

- `diagnostics/AppTrustSnapshot.kt`: actual installed package/certificate and
  current permissions/grants. Known sideload identity is labeled as compatibility,
  not as a new production publisher. No signing identity changes.
- `diagnostics/PrivateFailureReport.kt`: allowlisted code locations, bounded
  messages with no exception/media path content. Legacy/unstructured reports are
  omitted from export. `CrashReports` reads a bounded buffer and clear cannot be
  undone by its in-flight startup read.
- `ui/TrustPrivacyContent.kt`, `TrustCopy.kt`: installed facts, platform permission
  link, folder navigation, opt-in history and separate confirmed history/report
  clearing. Preview must precede an explicit share action. All supported languages
  have bundled text; Traditional Chinese is intentionally shared with Cantonese.
- Existing Settings privacy/data section contains this panel; original cache,
  backup/restore and reset controls remain. The ViewModel remains the history
  command source. No media/Stack decoder, quality, FPS, library order or cache
  format is changed.
- JVM formatter/translation tests and Compose trust-control tests are included.
  Compose tests inject diagnostic fixtures and do not clear the real local report.

## Checks completed without competing APK builds

- The actual formatter, snapshot and translation sources/tests were compiled
  with the existing Android Studio Kotlin CLI and SDK stubs, using the cached
  application class JAR only for the unchanged AppLanguage enum. **7/7** tests
  passed. This is isolated JVM model verification, not a complete Android UI
  compile, installed permission query or device acceptance.
- Shared artifact trust policy: **23/23** tests passed. Actual immutable
  `releases/GreaterArt-1.21.93.apk` inspection passed for the **sideload** channel:
  version1.21.93/code316,5,478,717 bytes, SHA-256
  `cee3f50632cdf186a1b58304c195378c1917284f20ce04d354c3549ab180f6cc`.
- Its valid v2 certificate remains
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.
  It is non-debuggable and has no Internet permission, but retains All Files Access
  and the Android Debug compatibility certificate. It does **not** pass production
  policy. No existing APK was changed or reissued.
- Edited workflows passed official actionlint1.7.12 (archive SHA independently
  compared with the official GitHub release digest). This is not a hosted CI run.

## Required completion after the other writer finishes

1. Check branch, version ledger, dirty diff and active build ownership again.
2. Integrate the next unused Android PATCH/code and update the main Android
   HANDOFF/README/website **without discarding the1.21.94 work**.
3. Run complete JVM, lint, debug/instrumentation compilation and the new
   `TrustPrivacyUiTest` on the owned emulator. Test denial/revocation and return
   from Android settings; no native-phone acceptance is yet claimed.
4. Use the existing approved sideload signing path only. Verify package/code,
   certificate, permissions and immutable output bytes. Do not create a key,
   call the legacy key production, or overwrite a consumed artifact.
5. Refresh source/artifact receipts and shared security status. Production signing,
   complete storage cutover and physical-device security assessment remain separate.

CLI lesson: PowerShell-to-`kotlinc.bat` split a semicolon classpath in the first
attempt. Invoke the existing compiler with Java directly and an explicit classpath;
do not alter installed toolchains or treat a copied test model as Android proof.
