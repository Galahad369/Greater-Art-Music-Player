VERSION_RULES.md — Greater Art APK

Rule (PATCH): versionName/code +=1 for ANY code change. Never reuse, never overwrite a release APK.

Steps (pipeline):
1. Read build.gradle.kts (current version)
2. Bump versionCode +1; versionName X.Y.Z+1
3. Apply code change
4. Build: ./gradlew :app:assembleDebug --offline
5. Verify: aapt dump badging (version match)
6. AVD smoke: install, dumpsys version, monkey launch, pidof, 0 FATAL
7. Clean releases: delete stale versions that don't match new version
8. Copy build APK to releases/GreaterArt-<version>.apk
9. Update HANDOFF.md header (version, code, build date, APK line)
10. Confirm: no duplicate APKs; HANDOFF.md matches build.gradle.kts

Versions tracked:
- 1.14.1 (code 116)
- 1.14.2 (code 117) — pulled main (da8dbfc), shared nav container
- 1.14.3 (code 118) — crossMargin=9 (red X 2px higher), build verified
- Previous: 1.13.26 (code 115) — removed stale release

No silent builds; no version skips; no overwrite.
