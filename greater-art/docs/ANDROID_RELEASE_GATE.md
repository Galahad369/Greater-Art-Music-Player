# Greater Art Android release gate

One local delivery path: **preflight → optimized assembly → original sideload signature and exact APK check → device smoke**. Each stage records its own result. No step here publishes to GitHub or changes release metadata. Debug APKs, host shader tests, and CI compile checks are not release acceptance.

## Run it

From the repository root, use Python 3.11+ and a new evidence folder outside the checkout. For app work that has not started, `build --preflight-only --out <new-folder>` resolves prerequisites without building. For a build request, run the build directly; it includes preflight:

```powershell
python -B scripts/greater-art-android-gate.py build --out ../output/android-release-1.22.15-new
```

The build command runs preflight first. It reads the compile SDK and Gradle daemon JDK from source, uses the Gradle wrapper and a byte-checked copy of the current Android inputs, and runs `:app:assembleRelease`. Use `--java-home` and `--sdk` to point at existing trusted installs if they are not found. If necessary, `--windows-trusted-roots` uses the existing Windows ROOT store for this process only; it does not disable TLS. No SDK installation or key replacement is performed by the gate.

Assembly is unsigned and produces its own log and R8 output even if signing is blocked. The gate then uses the existing compatibility keystore and verifies the resulting APK with `verify-trust-artifact.py`, ZIP integrity, and 16 KiB alignment. An authorized backup of the same key can be provided with `--keystore`. Do not paste keys or passwords into chat. Read `build.json` for the separate results. The versioned candidate under `candidate/` is not yet a verified release. A dirty-source build can diagnose assembly but cannot pass final delivery; use the exact committed source carrying the version for that.

## Exact-device smoke

Use the owned `GreaterArt_A55_API36` emulator and a reviewed native smoke adapter for the current task. The local `App/output/android-release-1.22.14-20261107/Verify-A55.ps1` records the earlier native playback procedure; its hard-coded version and historical result cannot verify a new build. The adapter must use the exact candidate (not a debug build), exercise playback through Library and Now Playing with a synthetic clip, confirm video advancement and the relevant changed behavior, and cover fullscreen/Mini return where applicable. For Ambient, inspect the beam during playback and after seeking. Install only when authorized; use `adb install -r` without a wipe or downgrade. Keep device settings unchanged unless separately authorized.

```powershell
python -B scripts/greater-art-android-gate.py smoke --receipt ../output/android-release-1.22.15-new/build.json --adb <sdk>/platform-tools/adb.exe --serial emulator-5554 --adapter pwsh -NoProfile -File <reviewed-native-smoke-script>
```

The adapter receives `GA_ADB`, `GA_SERIAL`, `GA_APK`, `GA_APK_SHA256`, `GA_APK_VERSION`, `GA_APK_CODE`, and `GA_SMOKE_DIR`. It emits `A55_EXACT_OPTIMIZED_PASS sha256=<GA_APK_SHA256>` only after the actual smoke assertions pass. The gate checks the installed base APK hash on the named A55 and writes the device receipt with its captured log digest. A bare exit code, marker from an older APK, or a screenshot without a real smoke test is not acceptance. If an adapter/device is unavailable, report smoke as blocked. Retry with the same `build.json` when ready; the gate keeps the earlier device result and does not rebuild.

Pass the successful `device_receipt` path to the verified-coding runner's `ready --android-receipt <path>`. The runner requires the gate for actual Android changes, even if the task contract only listed host checks. The gate binds the receipt to the current versioned commit, exact APK bytes and smoke log. No repeated device run is needed for the same evidence. Keep the prior APK immutable. A blocked build remains `SOURCE_ONLY`; public publication is a separate approval and workflow.
