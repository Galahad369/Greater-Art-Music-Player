---
name: greater-art-release
description: Build/deploy/release pipeline and quality gates for the Greater Art Android media player (Compose, Kotlin, floating mini-window overlay, offline-first). Always-on rules + references.
---

# Greater Art release — always-on rules

Class: Greater Art Android app build/release/release-state verification.
Not a session artifact — the workflow applies to every version bump of this project.

## Always-on gates (every instance)
1. VERSION BUMP: ANY code change — even 1px (`crossMargin` 11→9), one comment, spacing, localization string, manifest edit — requires `versionName` PATCH +1 (`1.14.3` → `1.14.4`) AND `versionCode` +1. Never reuse an existing version; never silent build. See `VERSION_RULES.md` (repo root) — HARDENED absolute rule.
2. HANDOFF.md = single source of truth; must reference current `build.gradle.kts` version + `releases/GreaterArt-<version>.apk`; never out of sync with build.
3. Build verification (mandatory sequence, no exceptions): `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline`. Never claim build passes from `assembleDebug` alone; verify `SUCCESSFUL` in output.
4. AVD smoke (one assert-style self-check): install APK → `dumpsys package com.local.listentomusic` (confirm `versionName`/`versionCode`) → `monkey -c android.intent.category.LAUNCHER 1` → `pidof` (non-empty PID) → `screencap` screenshot; `logcat -d` must show `0 FATAL` exceptions. Zero FATAL is a gate.
5. APK verification (`aapt dump badging`): `versionName` and `versionCode` must match `build.gradle.kts`; file size must match build output; never claim verified without `aapt` confirmation.
6. Release copy: `releases/GreaterArt-<version>.apk` = exact copy of `app/build/outputs/apk/debug/app-debug.apk` (same hash/size); delete superseded versions (`1.13.23`, `1.13.25`, `1.13.26`, `1.14.2` removed previously); never duplicate; never overwrite a versioned release.
7. No `INTERNET` permission check: verify `AndroidManifest.xml` has no `android.permission.INTERNET`; confirm before any release.

## References (see `references/` folder in this skill)
- `references/version-rules.md`: the absolute PATCH rule text (copied from `VERSION_RULES.md` at repo root); references the build/release pipeline.
- `references/build-pipeline.md`: step order (read/build/verify/AAVD/clean/copy/update); includes `build_release.ps1` reference.
- `references/dev-mode.md`: `DeveloperDiagnostics.kt` conventions (`.widthIn(max = 420.dp)`; `import ...widthIn` required); Nodes screen `.35f` default; slider `%` input dialog; pull handle removal (`PLAYER_PULL_HANDLE` removed); zoom fix (`.pointerInput(immersive)`).

## Pitfalls (capture the mechanism, not the session)
- `.pointerInput(Unit)` in `NowPlayingScreen.kt`: the key must be `immersive`, not `Unit`, so `detectTransformGestures` restarts when entering fullscreen. `Unit` causes zoom to remain broken in full screen.
- `AppPreferences.kt`: changing `SortMode.NAME_ASC` to `DATE_DESC` requires source verification (`FormattingTest.kt` or UI check) — don't assume default change propagates correctly.
- `MiniWindowOverlayService.kt`: `crossMargin` (red X vertical position) must be verified after any layout change; `crossMargin = 11` was changed to `9` (2px higher). Any future change requires version bump + AVD visual verification.
- `VERSION_RULES.md`: never append "UPDATE: actually..."; edit the sentence that misled; never write the same lesson twice.
- `build_release.ps1`: pipeline uses stdlib/shell only (`git`, `gradlew`, `aapt`, `copy`, `rm`); one `demo()` style self-check (assert-style AVD smoke) per non-trivial branch.

## Skill loading / usage
Load: `skill_view(name='greater-art-release')` before running any build/release pipeline or before making version-related edits.
References: `skill_view(name='greater-art-release', file_path='references/version-rules.md')` for the PATCH rule; `file_path='references/build-pipeline.md'` for the step sequence; `file_path='references/dev-mode.md'` for Nodes/background/slider/mini-window conventions.
