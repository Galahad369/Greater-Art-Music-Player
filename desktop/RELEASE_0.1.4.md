# Desktop 0.1.4 — local development delivery

Desktop **0.1.4/code5**, October 14, 2026. Exact build source: **227c7fe101bb1a8a7d4ed297ac00adc09d59e5d6**, local **main**. Desktop integration merge: dbcd06c. No new branch/worktree/PR or public push/release. **Not full Android parity, production acceptance or security certification.**

## Immutable artifacts

Ignored repository-relative files; never overwrite:

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| desktop/releases/GreaterArt-Desktop-Setup-0.1.4.exe | 32,661,504 | a5ec060c1f3669902eb297be1151c22b4a1cace48703944719c2ba7ebb56450a |
| desktop/releases/GreaterArt-Desktop-Portable-0.1.4.zip | 32,620,796 | 35a9b08271e302bf26755c7466e050b892d89d7b8ed8de24ea4df746ac0c3a2f |

App EXE: 953,856 bytes, file version 0.1.4, SHA-256 `55d861f4e12f3de5f33d21425c79cf2806eb712d40de1c946f42d5393dc17c08`.

Embedded MSI SHA-256: `c581665a9f16119326796f1a293e172b6fdc0a519ca937f31ba5bc1864aaf48a`.

Installed codec source ZIP SHA-256: `8b6405adbb1116f493024ba378fb4b943b4a35a8211157477d5159a7a4083535`.

0.1.1 frozen artifacts remain unchanged. 0.1.2/0.1.3 were source/build checkpoints, not frozen verified deliveries.

## Running it

Setup: double-click, keep Create desktop shortcut checked, then Install. Final 0.1.4 is already installed on this workstation at `%LOCALAPPDATA%/Programs/GreaterArtDesktop/GreaterArt.exe`; Desktop and Start menu shortcut targets were verified. Existing media/preferences were retained.

Portable: extract the entire ZIP, then run GreaterArt.exe **beside all six DLLs**. Not a single-file app. Settings remain in LocalAppData. No VLC/Java/Electron/external player or runtime download. Unsigned development binaries; hashes are integrity evidence, not publisher authenticity. Do not disable Windows protections.

## Actual verification

- Installed EXE **51/51 core/decoder tests**: Unicode identities/lists/favorites/Stack offsets, opt-in bounded history, LRC/A–B, graph/cache/math, malformed settings, no-overwrite exports, safe cache eviction and decoded 500 ms alignment.
- Installed WASAPI checks **9/9 WAV + 9/9 Opus**: real endpoint clock, pause hold, paused seek/resume, ½×/2× processing, continuous speed timeline and invalid command rejection.
- Installed production AIFF fallback **8/8 starts**. Native rejection transfers to bundled codec/WASAPI, not a transient terminal error.
- Installed video/window probe **PASS**: six playing engines, source 1280×720, 221 rendered frames, fullscreen monitor bounds, direct Mini/dock/expanded transfers and named MSAA hit tests. It deliberately minimizes before fullscreen. Two source-build runs also passed (216/220 frames).
- Actual setup install/upgrade, installed EXE hash, six codec hashes, source ZIP hash, shortcut targets and file version checked.
- Frozen portable ZIP extracted independently; launcher hash and **51/51 tests** passed without build-folder dependencies.
- Desktop current/history validator and **6/6 guard tests** passed. Android guard passed: source 1.21.39/code262, latest verified APK 1.21.37/code260 unchanged. Android build/device QA was not rerun for desktop-only changes.

Local evidence: native/build/installed-014, native/build/portable-014-check, native/build/final-014-ui-0 and -1, native/build/package-receipt.json. Captures include owned HWNDs only, never desktop pixels. No reports were uploaded. GitHub CI was configured but not claimed run for unpushed commits.

These tests do not prove six real singers sound phase-locked. Clean-machine, Narrator, all Share targets, endpoint reconnection and mixed-DPI/multi-monitor acceptance remain pending.

## Provenance / quality

Original C++20/Win32 UI and coordinator; static MSVC CRT. Visual Studio 18 Community, MSVC 14.51.36231, Windows SDK 10.0.26100.0; WiX 3 is build-only. Native Media Foundation video; original WASAPI audio path.

Source-built FFmpeg 9.0.2/port9.0.2#1, pinned vcpkg baseline e456309491fd875487bf02b8a0dca76c1b1b00cc. Default/GPL/nonfree/external-codec features disabled; dynamic replaceable libraries report LGPL-2.1-or-later. Exact patched source, port/configuration/recipe, notices and hashes accompany Codecs. The broad prebuilt archive is not shipped. See native/CODECS.md and installed Codecs/BUILD-RECEIPT.json.

No resolution, bitrate or FPS constraints, proxies, transcoding or thumbnail-video substitution. Local custom input rejects URLs/UNC/alternate streams and denies referenced container inputs. Bundled video output is not yet wired; native Windows video formats still vary by PC.

## Failures retained, not hidden

0.1.2 installed AIFF routing exposed native failure before the timer started fallback, although the raw decoder passed. 0.1.3 unified state-query/timer handoff; eight installed retries passed.

One 0.1.3 capture showed a minimized 237×39 player; a later run passed. The original minimization cause was not established. 0.1.4 explicitly restores destinations and exercises this case in the probe.

Build/Codecs collided with build/codecs on Windows; payload staging now uses CodecDocs. Generic include sorting broke SDK/fragment prerequisites; native formatting disables sorting. App and Setup were rebuilt. Upstream FFmpeg conversion warnings and MSI ICE91 warnings remain documented; the installer deliberately supports per-user only. No build error was ignored.

## Remaining scope

Full bundled video renderer/Dynamic Ambient; queue/list editing and complete translations; instrumental time maps/shared-sample mixing and tiled Stack video; three floating choices/tray, complete settings/cache/accessibility, clean-machine/security/license review and owner-approved signing. See docs/ANDROID_WINDOWS_PARITY.md. Full-parity 1.0.0 is reserved.
