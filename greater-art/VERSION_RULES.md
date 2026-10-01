VERSION_RULES.md — Greater Art APK (HARDENED)

VERSIONED CHANGE RULE (ABSOLUTE): Any change that affects executable app behavior, build behavior, release behavior, resources, manifest state, or executable tooling requires the next PATCH and versionCode +=1 in the same commit, unless this file explicitly lists a one-time series transition. Documentation-only and release-finalization exceptions are limited to rule 8 below. No silent builds and no reused version numbers.

This file is the authoritative version policy for Greater Art. If any handoff note, README, old commit message, or conversation conflicts with this file, this file wins.

## Machine-checkable state

Current source: **1.15.2 (code 132)**
Current release state: **SOURCE_ONLY**
Latest verified APK: **1.15.1 (code 131)**
Allowed series transition: **1.14.15 -> 1.15.1**

## Non-negotiable rules

1. **Every versioned code commit consumes a new PATCH version.**
   - `versionName`: `X.Y.Z -> X.Y.(Z+1)`
   - `versionCode`: `N -> N+1`
   - The version bump must be in the **same commit** as the code change.
2. **Never reuse a consumed version.** Once a code commit lands with a version, that version is spent even if the build, emulator, signing, or release step later fails.
3. **No silent fixes under the same version.** If a build/test exposes another code bug, the fix is another code commit and therefore the next PATCH/code.
4. **Never skip a PATCH/code inside the active series unless this file explicitly authorizes the exact transition.** Series changes must be user-directed and recorded as an `Allowed series transition` before they land. For this design-system release, `1.14.15 -> 1.15.1` is the sole authorized transition; `1.15.0` is intentionally unused.
5. **Release APKs are immutable.** Never modify, overwrite, rename, or copy an older APK into a new version filename.
6. **A new APK must be built from the exact source commit carrying that version.** Renaming an existing APK is not a build.
7. **Source and release are two separate states.**
   - `SOURCE_ONLY`: code/version has landed, but no verified APK for that version may exist in `greater-art/releases/`.
   - `VERIFIED`: the exact current-version APK has been built locally, its manifest/version checked, smoke-tested, hashed, and recorded.
8. **Documentation-only or release-finalization commits do not bump the app version** provided they contain no versioned code changes.
9. **Do not overwrite history to repair a version mistake.** Consume the next PATCH and record what happened.

## What counts as versioned code

A commit requires a PATCH/code bump when it changes executable/build/release behavior, including:

- `greater-art/app/src/**` Kotlin/Java/XML/native source
- Gradle/settings/build logic outside the versionName/versionCode lines themselves
- `greater-art/gradle/**` build logic
- executable tooling under `scripts/**`
- CI/release/version workflows under `.github/workflows/**`

Markdown/docs-only changes, release hashes/metadata, and adding a newly verified immutable APK do not themselves require another bump.

## Required two-phase pipeline

### Phase A — source commit

1. Read `greater-art/app/build.gradle.kts`.
2. Read this file and confirm the last tracked version.
3. Bump PATCH exactly +1 and `versionCode` exactly +1, unless applying an exact `Allowed series transition` listed above. `versionCode` still increments exactly once.
4. Set:
   - `Current source` to the new version/code.
   - `Current release state` to `SOURCE_ONLY`.
   - keep `Latest verified APK` pointing at the previous verified release.
5. Apply the code change in the same commit.
6. Do **not** add/rename/copy a current-version APK yet.

### Phase B — local verification/release finalization

Using the exact source commit from Phase A:

1. Build with Java 21:
   `./gradlew testDebugUnitTest lintDebug :app:assembleDebug --offline`
2. Verify APK manifest:
   `aapt dump badging <apk>`
   - package must be `com.local.listentomusic`
   - `versionName` and `versionCode` must match Gradle exactly.
3. Install on the A55/API 36 test target when available.
4. Smoke:
   - `dumpsys package` reports expected version
   - monkey/manual launch succeeds
   - `pidof` shows the app alive
   - logcat contains **0 FATAL EXCEPTION** for the tested flow.
5. Verify the expected signing certificate for the personal sideload build.
6. Compute SHA-256.
7. Create a **new** `greater-art/releases/GreaterArt-<version>.apk`. Never overwrite an existing versioned APK.
8. Update this file:
   - `Current release state: VERIFIED`
   - `Latest verified APK` = current source version/code.
9. Update `HANDOFF.md` release header with exact version/code, APK filename, size/hash, build date, and verification boundary.
10. Run:
    `python3 scripts/validate-greater-art-version.py --release`
11. Only then commit the APK/release metadata. That finalization commit must not contain app/tooling code changes.

## Failure/recovery rules

- Build failed after a source commit: keep that version consumed. Fixing code requires the next PATCH/code.
- Network/agent interruption before the commit landed: re-read `main`; do not assume a version was consumed.
- Network/agent interruption after the commit landed: continue from that exact commit; do not recreate or reuse its version.
- Current-version APK already exists before verification: stop. Do not overwrite it. Investigate whether it is a valid immutable artifact.
- Existing APK is byte-identical to an older version or was produced by rename/copy: reject it and consume a new PATCH for any corrective code change.

## Automated guard

`.github/workflows/version-consistency.yml` runs `scripts/validate-greater-art-version.py`.

The guard checks:

- Gradle matches the machine-checkable state above.
- the tracked version ledger is sequential, except for exact one-time `Allowed series transition` entries.
- each versioned code commit in the pushed/PR range bumps PATCH and code exactly once.
- a code-changing commit lands as `SOURCE_ONLY`.
- release APK paths are immutable: modification/rename/copy is rejected.
- a newly added APK filename matches that commit's Gradle version.
- `--release` additionally requires the current APK, exact HANDOFF release metadata, SHA-256, and `aapt dump badging` match.

## Versions tracked

- 1.14.1 (code 116)
- 1.14.2 (code 117) — shared navigation container
- 1.14.3 (code 118) — crossMargin=9; local build verified
- 1.14.4 (code 119) — version-policy/CI hardening; source only, local build pending
- 1.14.5 (code 120) — inline row actions + queue-fling thumbnail deferral; source only, local build pending
- 1.14.6 (code 121) — stable queue row identity + stationary clipped action underlays; source only; CI compile failed on missing drawscope import
- 1.14.7 (code 122) — drawscope import + corrected SOURCE_ONLY handoff metadata; source only; CI compile failed on nested drawContent receiver
- 1.14.8 (code 123) — explicit ContentDrawScope receiver for clipped stationary action underlays; source only, local build pending
- 1.14.9 (code 124) — compact 48 dp square Add-to-list actions in Now Playing and Library; source only, local build pending
- 1.14.10 (code 125) — suspend independent video wallpaper while expanded Now Playing covers MainActivity; source only, local build pending
- 1.14.11 (code 126) — library delete button square (48dp), second video wallpaper suspended, logs consolidated; source only, local build pending
- 1.14.12 (code 127) — PR review cleanup: restore Delete accessibility label and allow versioned release APK tracking; source only, local build pending
- 1.14.13 (code 128) — deep audit: duplicate-safe queue identity/exact-index playback, unified normal queue startup, expanded-player waveform warmup suppression, retired thumbnail-preload cleanup with active cache pruning, stale backup source removal, and release identity/versionCode hardening; source only, local build pending
- 1.14.14 (code 129) — audit follow-up: propagate WAV warmup cancellation and make Locate current resolve the filtered queue's visible index; source only, local build pending
- 1.14.15 (code 130) — version-guard PR-head fix: validate real pull-request commits instead of GitHub's synthetic merge ref; source only, local build pending
- 1.15.1 (code 131) — user-directed visual-system release: restrained semantic design tokens, typography hierarchy, consistent chrome/spacing/motion, 48 dp interaction targets, dynamic graph labels, and four-screen UI polish; local build verified
- 1.15.2 (code 132) — version-guard fix: allow new-version APKs detected as copies by checking SHA-256; source only, local build pending

Previous baseline: 1.13.26 (code 115).

**No silent builds. No version reuse. No version skips. No APK overwrite/rename/copy.**