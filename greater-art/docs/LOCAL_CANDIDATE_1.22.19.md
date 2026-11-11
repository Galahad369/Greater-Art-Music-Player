# Android 1.22.19 — Local A55-tested candidate

November 11, 2026. This records the existing local APK, not a new public release.

## Exact artifact

- Package: `com.local.listentomusic`; version `1.22.19`, code `340`.
- File: `App/output/android-toolbar-1.22.19-20261111/release/candidate/GreaterArt-1.22.19.apk`.
- Size: `5,546,768` bytes.
- SHA-256: `79328f13b443a543c655cb321360cc6feec6afdcda086332572bf43eb4a2886e`.
- Local certificate SHA-256: `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`, unchanged from the 1.22.15–18 local candidates. It cannot update an original-signer installation.
- Android input-manifest SHA-256: `f3019e4808ee8fe1f986e565889b5dc353e57e14d9c8f0604282a3599bb67842`. The APK was built from the frozen working-tree inputs before the requested commit. Retain that snapshot and receipt; the local smoke is not a CI provenance attestation or a retroactive claim that assembly ran on a clean commit.

## Checks actually completed

- Preflight, R8-optimized `:app:assembleRelease`, and `:app:compileDebugAndroidTestKotlin` passed. Compilation did not execute the Compose instrumentation tests.
- Package, version/code, non-debuggable state, certificate, ZIP CRC, 16 KiB alignment and exact-file SHA-256 passed.
- The registered `GreaterArt_A55_API36` booted with its existing API 36, 1080×2340, 450 dpi, four-GiB configuration and host renderer. No graphics or AVD configuration changes were made.
- Installed APK SHA-256 matched the candidate before and after the native run. Library → Now Playing, pause/resume, portrait fullscreen → Now Playing, detached Mini and Library return passed. The fixture advanced through red, green and blue frames; its video band was 1080×607 and its reference square was 66×67 pixels.
- Three existing local 孤独毒毒 videos were copied without transcoding for an additional screenshot check. Morisray playback, pause and two taps at the observed Lock position passed. Playing and paused Now Playing captures were visually inspected: the toolbar backplate is absent and controls remain in their existing positions.
- Temporary overlay/storage access and notification permission were restored. Only the newly copied test media was removed; originals, existing APKs and emulator data were preserved. No new app crash or ANR appeared in the native smoke run.

Local evidence folder: `App/output/android-toolbar-1.22.19-20261111/a55-20261111-053123/`. Keep `native-proof-retry2/receipt.json`, its complete runtime log, `a55-native-flow-retry2/`, and `a55-real-video-retry1/now-playing-paused.png`. Paused screenshot SHA-256: `51035da751dda9908d35f23013d026141a5321107f2c50fab4d70f18d29fdec8`.

## Failures preserved and limits

The initial run stopped at Android's notification prompt. The next captured the first-use fullscreen tutorial rather than unobscured video. The final run handled both system dialogs without changing the app or media/geometry assertions. The first screenshot script could not locate the Unlock accessibility node; actual touch at the same observed Lock position unlocked successfully. This does not establish TalkBack support.

The source and binary are not universally crash-free. Physical hardware, HDR output, acoustic Stack synchronization, full accessibility and performance tracing remain outside this run. Original-key signing and formal release promotion remain separate gates. `SOURCE_ONLY` and the verified 1.21.93 repository download remain unchanged; no APK or tag was published.
