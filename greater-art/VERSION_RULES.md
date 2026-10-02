VERSION_RULES.md — Greater Art APK (HARDENED)

VERSIONED CHANGE RULE (ABSOLUTE): Any change that affects executable app behavior, build behavior, release behavior, resources, manifest state, or executable tooling requires the next PATCH and versionCode +=1 in the same commit, unless this file explicitly lists an exact series or consumed-branch transition. Documentation-only and release-finalization exceptions are limited to rule 8 below. No silent builds and no reused version numbers.

This file is the authoritative version policy for Greater Art. If any handoff note, README, old commit message, or conversation conflicts with this file, this file wins.

## Machine-checkable state


Current source: **1.15.32 (code 162)**
Current release state: **SOURCE_ONLY**
Latest verified APK: **1.15.26 (code 156)**
Allowed series transition: **1.14.15 -> 1.15.1**
Allowed consumed transition: **1.15.13 (code 143) -> 1.15.17 (code 147)**
Allowed consumed transition: **1.15.17 (code 147) -> 1.15.19 (code 149)**
Allowed consumed transition: **1.15.19 (code 149) -> 1.15.21 (code 151)**
Allowed consumed transition: **1.15.21 (code 151) -> 1.15.22 (code 152)**
Allowed consumed transition: **1.15.22 (code 152) -> 1.15.23 (code 153)**
Allowed consumed transition: **1.15.23 (code 153) -> 1.15.24 (code 154)**
Allowed consumed transition: **1.15.24 (code 154) -> 1.15.25 (code 155)**
Allowed consumed transition: **1.15.25 (code 155) -> 1.15.26 (code 156)**
Allowed consumed transition: **1.15.26 (code 156) -> 1.15.27 (code 157)**
Allowed consumed transition: **1.15.27 (code 157) -> 1.15.29 (code 159)**
Allowed consumed transition: **1.15.30 (code 160) -> 1.15.32 (code 162)**

## Non-negotiable rules

1. **Every versioned code commit consumes a new PATCH version.**
   - `versionName`: `X.Y.Z -> X.Y.(Z+1)`
   - `versionCode`: `N -> N+1`
   - The version bump must be in the **same commit** as the code change.
2. **Never reuse a consumed version.** Once a code commit lands with a version, that version is spent even if the build, emulator, signing, or release step later fails.
3. **No silent fixes under the same version.** If a build/test exposes another code bug, the fix is another code commit and therefore the next PATCH/code.
4. **Never skip a PATCH/code inside the active series unless this file explicitly authorizes the exact transition.** Series changes use `Allowed series transition`. A consumed-branch recovery may use `Allowed consumed transition` only when every skipped version/code was already committed on a side branch and remains recorded in the ledger. The exact 1.15.13/code143 -> 1.15.17/code147 transition is authorized because 1.15.14–1.15.16 were already consumed during reviewed recovery attempts.
5. **Release APKs are immutable.** Never modify, overwrite, rename, or copy an older APK into a new version filename.
5b. **Release APK filename MUST match the exact versionName and versionCode.** The artifact in `greater-art/releases/` must be named `GreaterArt-<versionName>.apk` (e.g., `GreaterArt-1.15.17.apk`). Sending or storing an APK with any other filename (e.g., `app-debug.apk`) is a release violation.
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
- 1.15.1 (code 131) — user-directed visual-system release: restrained semantic design tokens, typography hierarchy, consistent chrome/spacing/motion, 48 dp action targets, dynamic graph labels, and four-screen UI polish; local build verified
- 1.15.2 (code 132) — version-guard fix: allow new-version APKs detected as copies by checking SHA-256; source only, local build pending
- 1.15.3 (code 133) — CodeQL build-mode fix: use build-mode: auto to fix Java/Kotlin analysis; source only, local build pending
- 1.15.4 (code 134) — CodeQL build-mode fix: use build-mode: autobuild for Java/Kotlin analysis; source only, local build pending
- 1.15.5 (code 135) — Lag fixes: inspector bounds out of snapshot state; queue thumbnails read scroll state lazily; source only, local build pending
- 1.15.6 (code 136) — Stack playback lag fix: primary-player clock, buffer-aware companion gating, reduced hard-seek churn, Stack waveform suppression, and decorative video-background suspension; source only, local build pending
- 1.15.7 (code 137) — Stack synchronization follow-up: freeze companions before primary swaps and always mirror actual primary buffering/resume events; source only, local build pending
- 1.15.8 (code 138) — Stack/UI lag follow-up: isolate Stack rows from 2 Hz position ticks, stabilize StackSlot instances, defer Stack thumbnails during fling, and restore UiInspector unit-test bounds seam; local build verified
- 1.15.9 (code 139) — repository integrity cleanup: remove unsafe commit-amending version helpers, permissive branch-protection recipes, and unverified 1.15.4 release artifact; local build verified
- 1.15.10 (code 140) — version-guard hardening: root executable tooling consumes versions and current-tree release scan rejects unverified/newer or malformed APK artifacts; local build verified
- 1.15.11 (code 141) — force-rewrite CI recovery: version workflow skips only the impossible BASE..HEAD range check when a push's old base object is no longer reachable, while current-state/ledger validation remains mandatory; source only, local build pending
- 1.15.12 (code 142) — ColorTheme.FOREST wired into PlayerWindowExpandedContent; legacy theme handling cleaned up; source only, local build pending
- 1.15.13 (code 143) — full-screen video theme isolation and ColorTheme wiring; source only, local build pending
- 1.15.14 (code 144) — complete ColorTheme/Settings recovery on superseded side branch; all required CI passed; version consumed, not merged
- 1.15.15 (code 145) — rebased complete ColorTheme recovery after concurrent main drift; version consumed on side branch, not merged
- 1.15.16 (code 146) — consumed-transition validator support for the recovery branch; version consumed on side branch, not merged
- 1.15.17 (code 147) — canonical branch convergence: complete reviewed ColorTheme/Settings implementation + exact consumed-transition guard, preserving verified 1.15.10 release metadata; local build verified
- 1.15.18 (code 148) — Grok branch reviewed and rejected; claimed color-theme implementation was only Gradle bump + comment removal; version consumed
- 1.15.19 (code 149) — transport-control alignment and final branch convergence: `GaControl.heroIcon = 32.dp` token; main/video play icons consistent; prev/next use 48 dp `GaControl.touchTarget` with `GaControl.prominentIcon`; source only, local build pending
- 1.15.20 (code 150) — same-side double-tap seek for video/audio with 400 ms pairing, cross-side rejection, and hold-to-2x isolation; local build verified
- 1.15.21 (code 151) — superseded final-convergence PR published after concurrent main drift; version consumed on side branch, not merged
- 1.15.22 (code 152) — Grok Stack branch republished only the already-present offline recommendation helper/test with stale 1.15.19 policy metadata; version consumed, rejected
- 1.15.23 (code 153) — canonical forward recovery: remove the unintegrated Stack recommendation helper/test, preserve the reviewed 1.15.20 same-side seek + workflow docs, and restore one coherent main timeline; local build verified
- 1.15.24 (code 154) — canonical video-surface first-frame attribution recovery using analytics event time plus unchanged-presentation/candidate reconcile deduplication; local build verified
- 1.15.25 (code 155) — concurrent-branch convergence advanced the already-reviewed 1.15.24 surface implementation to the next source identity without further surface source/test changes; local build verified
- 1.15.26 (code 156) — **Grok**: LIBRARY_FAMILY_NAV text → icons (Layers / LibraryMusic / Hub); local build verified
- 1.15.27 (code 157) — final branch convergence: carry forward Replit Now Playing lock/top-bar alignment using shared GaControl touch-target tokens; source only, CI pending
- 1.15.28 (code 158) — incomplete Grok list-scroll branch: version bump plus unused `ListScrollBudget` helper only; intended AppBackground/list wiring never landed; version consumed, not merged
- 1.15.29 (code 159) — canonical list-scroll video budget: Library, Now Playing queue, Stack list and Stack picker report fling state; decorative CURRENT_VIDEO/CUSTOM_VIDEO stays attached but pauses playback/crop-position work until scrolling settles; source only, CI pending
- 1.15.30 (code 160) — Stack completion + Nodes linked-graph UI: NOW_PLAYING context/highlight, searchable multi-select, wired offline recommendations using local metadata/history, save Stack to playlist, and denser focused graph chrome; preserves 1.15.29 list-scroll budget; source only, CI pending
- 1.15.31 (code 161) — persistent Stack-keyword implementation published on superseded PR #78; Android CI, Version Consistency, dependency review, privacy audit and CodeQL passed, but a concurrent docs-only main change made the PR non-mergeable; version consumed, not merged
- 1.15.32 (code 162) — canonical persistent user-owned Stack keyword: pinned tracks plus optional local keyword dynamically append matching names/paths; legacy playlist rows remain compatible; source only, CI pending

Previous baseline: 1.13.26 (code 115).

**No silent builds. No version reuse. No version skips. No APK overwrite/rename/copy.**
