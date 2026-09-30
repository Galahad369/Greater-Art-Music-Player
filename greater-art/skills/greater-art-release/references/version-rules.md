# Greater Art release — version rules (from VERSION_RULES.md, repo root)

Rule (PATCH, absolute, zero exceptions): ANY change to ANY file — code line, constant (`crossMargin` 11→9), comment, spacing, pixel offset, resource, manifest token, version string — requires `versionName` PATCH +1 (`1.14.3` → `1.14.4`) and `versionCode` +1. Even a single byte. Even 1px. Even localization string change (`UiText.kt`). Even build config change.

No silent builds. No skipped version numbers. No reused release filenames (`releases/GreaterArt-1.14.3.apk` never overwritten by a different build with same number).

Pipeline order (reusable):
1. Read `HANDOFF.md` + `build.gradle.kts` (current version)
2. Check repo hygiene (`git status`); `releases/` clean of stale versions
3. Clean stale releases (delete superseded versions — previous `1.13.23`, `1.13.25`, `1.13.26`, `1.14.2` removed previously)
4. Bump `versionName`/`versionCode` in `build.gradle.kts`; sync `HANDOFF.md`
5. Apply code change
6. Build: `./gradlew :app:assembleDebug --offline`
7. Verify APK: `aapt dump badging` (match version/code)
8. AVD smoke: install → `dumpsys` → `monkey` → `pidof` → screenshot; 0 FATAL
9. Copy release: `releases/GreaterArt-<version>.apk` = `app-debug.apk`
10. Update `HANDOFF.md` (version, code, date, APK line, SHA-256 when available)
11. Confirm no duplicate releases; `VERSION_RULES.md` and `.gitignore` consistent

Pitfall: never claim build passes without `testDebugUnitTest lintDebug assembleDebug --offline` output showing `BUILD SUCCESSFUL`. Never claim APK verified without `aapt` output.
