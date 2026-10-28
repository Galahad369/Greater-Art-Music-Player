# Greater Art Desktop version policy

Current desktop: **0.1.20 (code 21)**
Release state: **SOURCE_ONLY candidate — WinUI 3 playback prototype; verification and distributable pending**
Android versions remain independent. This desktop stage does not change Android source or artifacts. Refer to the Android ledger for its current identity.

Every committed desktop executable/build/workflow change increments PATCH and code together. Documentation and verified-artifact finalization do not consume another version. Never overwrite a versioned artifact. Full-parity 1.0.0 is reserved until all parity gates pass.

| Version | State | Changes |
| --- | --- | --- |
| 0.1.0 / 1 | Historical checkpoint 702976a | Compose/JVM workspace and VLC-backed prototype. Superseded; retained in Git history. |
| 0.1.1 / 2 | Native Windows development build | Original C++/Win32 IDE-style workspace, Windows Media Foundation playback, offline self-contained installer, desktop/Start menu shortcuts. |
| 0.1.2 / 3 | Source checkpoint; no frozen delivery | Offline playlists/history/LRC/A–B/sleep/lock/resume, selected metadata/artwork/bounded waveform cache, artwork-color Ambient, sound analysis and saved offsets, coordinated starts/drift trim, native sharing/MSAA, original WASAPI audio path with pinned source-built replaceable LGPL codec DLLs. Installed testing found a fallback startup race. |
| 0.1.3 / 4 | Source checkpoint; no frozen delivery | Fix native-error/fallback startup race caught by installed AIFF testing; repeated installed audio 8/8 passed. One UI run captured a minimized player; a subsequent run passed. |
| 0.1.4 / 5 | Locally verified development delivery | Explicit destination restore, minimized fullscreen regression, installed audio/core/renderer and frozen portable checks passed. See RELEASE_0.1.4.md; full parity and production acceptance remain pending. |
| 0.1.5 / 6 | PR review source; not a new frozen release | Bounded background queue reports admission/failure instead of silently dropping work; UI scan/alignment busy flags recover; audio frames use RAII on exceptional exits. |
| 0.1.6 / 7 | CI repair source; not a new frozen release | Fetch the exact pinned official vcpkg registry baseline on hosted runners before source-building codecs; no codec upgrade or runtime-quality change. |
| 0.1.7 / 8 | CI repair source; not a new frozen release | Align hosted registry HEAD and version database with the exact pinned baseline; no force checkout, runtime or codec upgrade. |
| 0.1.8 / 9 | Installer CI repair source; not a new frozen release | Package exact verified port source from the pinned registry when vcpkg builds directly from HEAD rather than cached git trees. Reject dirty or mismatched port trees. |
| 0.1.9 / 10 | Live Ambient implementation candidate | Optional decoded-frame pass-through color tap; nonblocking bounded GPU analysis; video/artwork palette with smooth Classic/Adaptive backgrounds. Verification in progress, not full parity. |
| 0.1.10 / 11 | Shared-clock Stack implementation candidate | One bounded PCM producer and one WASAPI audio output for all voices; fractional offsets before mixing. Verification in progress; not full parity or perfect performance matching. |
| 0.1.11 / 12 | Source/installed checkpoint; no frozen delivery | Stack handoff/prebuffer/loop repairs, DPI vectors, native Ambient caching and display-rate presentation. Playback tests passed; reduced-motion diagnostic assertion failed from counting stale callbacks. Preserved, not relabelled. |
| 0.1.12 / 13 | Locally verified development delivery | Correct accepted-frame accounting; installed/portable 62/62, six-source controls 11/11 and calibrated output/loop/video/Ambient/motion acceptance pass. Immutable installer/ZIP and limits in RELEASE_0.1.12.md. |
| 0.1.13 / 14 | Installed checkpoint; no frozen delivery | Background spring/repulsion simulation, weighted hubs, hover links, cursor-anchored zoom, drag capture and idle/reduced-motion behavior. Core 73/73 and 4K interaction passed; installed gesture probe exposed an off-canvas test anchor. |
| 0.1.14 / 15 | Installed checkpoint; no frozen delivery | Corrected pan probe; installed 73/73 and real 1080p gestures passed. Presentation profiling exposed unrelated clipped text and redundant background work. |
| 0.1.15 / 16 | Installed/portable checkpoint; no frozen delivery | Removed redundant painting. Both core copies 73/73; installed real 1080p/4K graph and transfers pass. General transfer report still printed its old hardcoded version. |
| 0.1.16 / 17 | Locally verified development delivery | Installed/portable 73/73, real-library 4K graph interaction and 1080p fullscreen/Mini/MSAA transfer pass. Diagnostics report actual build identity. Immutable hashes and limits in RELEASE_0.1.16.md. |
| 0.1.17 / 18 | Consumed historical identity; preserved artifacts | Previously used identity, not reused by the rebuild. Current checkout returned to the 0.1.16 source baseline; no verification is inferred from a versioned filename. |
| 0.1.18 / 19 | Consumed historical identity; preserved artifacts | Recorded by historical ledger commit `c2d4c4e819afad5a77092bf1640dc0c481b86ee3`. Existing artifacts remain untouched; this record does not establish their provenance or verification status. |
| 0.1.19 / 20 | SOURCE_ONLY candidate; verification pending | User-approved WinUI 3-first desktop shell with the existing native Media Foundation/Stack services behind a C ABI. Real playback, transfers, DPI and offline deployment are acceptance gates; Android is unchanged. |
| 0.1.20 / 21 | SOURCE_ONLY build repair | CI must prepare the existing codec redistribution evidence before WinUI checks it. Read the canonical build/CodecDocs folder, not build/codecs. Keep missing-evidence failure and all license/source checks. No desktop playback or UI implementation changes. Verified0.1.16 artifacts are unchanged. |

The one-time source-baseline transition from 0.1.16/code17 to 0.1.19/code20 skips the two consumed identities above. The version guard permits only this exact transition; subsequent executable/build changes still require PATCH/code +1. This does not relabel or overwrite a historical release.

The native source/resource/MSI/artifact identities and the WinUI project identity (when present) are checked by packaging/validate_version.py. The guard also validates the historical Compose checkpoint. Installer signing, clean-machine testing, full accessibility and full mobile feature parity remain open gates. No APK is rebuilt or relabelled for desktop work.
