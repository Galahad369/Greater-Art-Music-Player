# Greater Art — build/release pipeline (reusable steps)

Always applied before and after any version change.

## Commands that work (Windows, this environment)

Build (offline, no external downloads):
```bash
export JAVA_HOME="/c/Program Files/Android/openjdk/jdk-21.0.8"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew :app:assembleDebug --offline --console=plain
```
Full verification sequence (mandatory):
```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain
```
Check APK version (must match `build.gradle.kts`):
```bash
"$ANDROID_HOME/build-tools/34.0.0/aapt.exe" dump badging \
  app/build/outputs/apk/debug/app-debug.apk
```

AVD smoke (use `GreaterArt_A55_API36`):
```bash
"$ANDROID_HOME/platform-tools/adb.exe" install -r releases/GreaterArt-<version>.apk
"$ANDROID_HOME/platform-tools/adb.exe" shell dumpsys package com.local.listentomusic | \
  grep -E 'versionName|versionCode'
"$ANDROID_HOME/platform-tools/adb.exe" shell monkey -p com.local.listentomusic \
  -c android.intent.category.LAUNCHER 1
"$ANDROID_HOME/platform-tools/adb.exe" shell pidof com.local.listentomusic
# Confirm PID non-empty; screenshot via exec-out screencap; 0 FATAL in logcat
```
Clean stale releases (before copying new):
```bash
# Delete superseded versions; keep current working version only
# Example: remove 1.13.23, 1.13.25, 1.14.2 if superseded by 1.14.3
```
Copy release: `cp app/build/outputs/apk/debug/app-debug.apk releases/GreaterArt-<version>.apk`
Verify release copy: `aapt` on `releases/GreaterArt-<version>.apk`.

Update HANDOFF.md header/repo-state/APK line; sync VERSION_RULES.md.

## Environment gotcha (observed this session)
- `JAVA_HOME` must use native Windows path (`C:\Program Files\...`), not POSIX-style `/c/Program Files/...`. If `jlink.exe` is missing from VS Code Java extension JRE (`redhat.java-1.56.0-win32-x64`), build fails at `compileDebugJavaWithJavac` with `Execution failed for JdkImageTransform`. Fix: point `JAVA_HOME` to a native JDK with `jlink.exe`, or install JDK image. Not a version/code error.
