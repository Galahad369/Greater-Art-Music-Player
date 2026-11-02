# Android optimized candidate — 1.21.97 / code320

Date:31 October2026. **Artifact checks passed; complete interactive emulator
acceptance is blocked. This is not a promoted verified release.**

## Exact artifact

- Retained sibling-workspace file: `App/output/retained-artifacts/GreaterArt-1.21.97-candidate.apk` (hash-verified move on2 November after98 was built; the original delivery path was `App/output/GreaterArt-1.21.97.apk`).
- Package: `com.local.listentomusic`; version1.21.97, code320.
- Size:5,495,129 bytes.
- SHA-256: `10ffd6525215af8522e3e577b130f56741b00488649d655338e5d7be4d772de9`.
- Runtime source: `56ecdf6eb6b64500af6bba99bc612fdc76aecbdb`.
- R8-minified release variant, **non-debuggable**, not a renamed debug APK.
- Existing certificate SHA-256:
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.

No key was generated, replaced, exported or committed. This remains the existing
Android Debug compatibility sideload identity, not a modern production publisher
certificate. All Files Access remains declared. No production certification,
store publication or public CI/provenance run is implied.

## Included work

- Preserve1.21.94's default-route Now Playing reflections.
- Trust1.21.95/source9690c10: actual installed identity/grants, localized privacy
  controls, opt-in history and bounded redacted diagnostic preview/confirmed clear.
- Strict Fit/Crop1.21.96/source44d5281: native proportional bounds, retained valid
  aspect on reuse and no near-aspect stretching. Credit Greater Art mobile
  chat019fdcca-2d14-7e71-9257-a6995ff2136b for the implementation and geometry tests.
- Version97 fixes two device fixtures: attach the native view before expecting
  posted callbacks and wait for native Settings/app readiness. Production logic
  is otherwise unchanged from96. No new primary quality/resolution/FPS constraint,
  decoder replacement, surface reload, permission or network dependency is added.

## Completed checks

- Java21/Gradle9.8.2, offline, one worker, no parallel build/emulator: debug unit,
  lint, app and test assembly pass in8m02s. **495 JVM tests, no failures/errors/
  skips; lint0 errors,47 warnings,4 hints.** Existing warnings are not hidden.
- Optimized same-key `assembleRelease` plus resolved dependency report passes
  in11m14s. The release build uses a2GiB JVM; debug used1GiB/in-process Kotlin.
- Actual APK inspection passes: package/version/code, approved signer, signature,
  non-debuggable/not-test-only, no `INTERNET`, and reviewed component exports.
  ZIP CRC and16KiB ZIP alignment pass. Dependency inventory contains144 records;
  it is not a complete standardized SBOM or vulnerability assessment.
- Native API36 geometry: **8/8 pass in23.999s**, including the repaired attached-
  callback case, Fit/Crop, rotation, reused surface, square/portrait/anamorphic
  sources, invalid aspect updates and rejection of unsupported stretch modes.
  These are measured native frame/surface checks, **not decoder-pixel, physical
  phone, cold-start performance or OEM-compositor proof**.
- Final trust run:6/7 pass in122.887s. The remaining external-Settings readiness
  case fails while an inspected screenshot/XML shows **System UI isn't responding**.
  Recovery then reveals **Pixel Launcher isn't responding**. The host runner
  refuses acceptance, retains failures and restores its input grants. Do not
  represent this run as seven passing cases or a new Greater Art Java crash.
- Existing1.21.93 release SHA-256 is unchanged:
  `cee3f50632cdf186a1b58304c195378c1917284f20ce04d354c3549ab180f6cc`.

## Evidence and remaining gate

Evidence: sibling `App/output/android-trust-20261031/`. Preserve build logs,
`release-1.21.97-trust.json`, `release-1.21.97-dependencies.json`, failed and
successful device logs, ANR traces, screenshots and source snapshots.
The source is recorded externally; the receipt's Git context is not an independent
cryptographic source-to-byte attestation. Unrelated Windows source remains dirty;
the Android source tree matches its recorded commit.

Exact R8 mapping is retained as `release-1.21.97-mapping.txt`, SHA-256
`acee9d547fa8237106832a0fb777da0db43730453a9d4b099c37bbcaa4a7d739`.
It is diagnostic evidence, not disposable staging output.

Host RAM was about260MiB during repeated Android-system ANRs and870MiB just
after optimized build inspection. Do not repeatedly boot a pressured emulator,
weaken assertions, lower media quality or call component bounds visual acceptance.
The user has been asked to free roughly3GiB. Keep the owned QA emulator stopped
while building. On resume, use its normal four-CPU configuration, wait for actual
accessible native UI without ANR dialogs, and retain the same assertion timeouts.

Still required before promotion:

1. Run `scripts/test-android-trust.ps1` on the owned API36 emulator: nine actual
   deny/grant/revoke/preview/clear/Settings-return cases, restoring grants.
2. Run SettingsNavigationTest and the preserved reflection/compact/toolbar/real-
   video/IME group, plus real fullscreen Fit/Crop/rotation/return verification.
3. Inspect accepted screenshots. Separately install this exact optimized APK
   with `adb install -r`, without clearing data, and smoke-test native playback,
   Settings, dock, expanded player and fullscreen. Debug tests do not validate
   a minified binary's private class references.
4. Recheck this frozen hash and unchanged source, then promote the same bytes
   under the repository's immutable release rules and update the verified state.

Physical Samsung acceptance, complete security/storage cutover, signing migration,
acoustic Stack validation and universal crash freedom remain separate gates.

## Resume checkpoint — 31 October 2026

- The user requested continuation and a commit. The other chat released the
  exclusive mobile verification window; its uncommitted Windows work is preserved.
- A fresh host check found only 303 MiB of free physical RAM out of 15,658 MiB.
  ADB reported no connected devices. Do not boot the emulator under the same
  pressure that caused the retained Android system ANRs. The user was asked to
  free approximately 3 GiB before the unchanged interactive checks resume.
- After the user reported freeing memory, elevated checks found 777 MiB and then
  422 MiB available, still below the owned AVD's 2 GiB guest allocation. ADB had
  no connected devices. Independent process checks found no live Java, Gradle,
  Kotlin or emulator helper to reclaim. Do not delete caches or stop unrelated
  applications. Asked the user to save work and close unused browser/ChatGPT
  windows; no emulator was launched and no new runtime result is claimed.
- After further app closure, independent CIM and performance counters still
  showed only 291–777 MiB available across the retry checks; the latest reading
  was 657 MiB. Nonpaged kernel pool was 3,166 MiB and paged-pool allocation was
  3,728 MiB. Do not sum these as resident RAM: paged pool is not wholly resident.
  These snapshots establish physical-memory pressure, not a proven leak or an
  identified faulty driver. Commit capacity was not exhausted. The PC had about
  eight days of uptime. Recommended a user-controlled Windows restart after
  saving work, followed by a fresh memory check. No restart, security-software
  change, pagefile change or emulator launch was performed. Approximately 3 GiB
  free is a conservative QA target, not a universal emulator requirement.
- The optimized APK still has the exact hash recorded above. The Android source
  and build inputs are unchanged from `56ecdf6`; the current `app/src/main` Git
  tree is `3097172f031725dd475223e37181a21a7bbb2faf`. This is local source
  comparison, not an independent source-to-binary attestation.
- Cleanup is complete for nine obsolete or duplicate outputs: 153,604,909 bytes
  were sent to the Windows Recycle Bin. The loose legacy 1.15.75 APK was archived
  intact. The output root has one delivery candidate, `GreaterArt-1.21.97.apk`.
  Preserve the matching 1.21.97 debug/test APK pair, mapping, fixtures, evidence,
  signing material and immutable release history. The sibling output index
  records the cleanup inventory and retained paths.
- No new build, emulator test, promotion, version bump, push or publication is
  implied by this checkpoint. Resume the existing acceptance sequence above when
  the host has enough free RAM; do not replace it with static artifact checks.

## Resume checkpoint — 2 November 2026

- The mobile chat explicitly transferred the exclusive Android QA window to
  this chat and remains idle. Existing desktop edits are unchanged.
- Windows reports its last boot as 1 November 2026. Elevated checks found
  206 MiB free initially, then 1,265 MiB; an independent Windows counter showed
  1,307 MiB available. No Java/emulator helper is running; ADB has no devices.
  The restart occurred, but adequate headroom has not yet been restored.
- Rechecked the delivery APK: its SHA-256 is still
  `10ffd6525215af8522e3e577b130f56741b00488649d655338e5d7be4d772de9`.
  No build, emulator boot, install or runtime test ran. All acceptance gates
  above remain open; source identity, signing key and release status are unchanged.
  Resume only when headroom is adequate; approximately 3 GiB remains the
  conservative target after the retained system ANRs, not a universal requirement.
