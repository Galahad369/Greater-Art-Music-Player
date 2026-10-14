# Greater Art Desktop version policy

Current desktop: **0.1.5 (code 6)**
Release state: **SOURCE_ONLY — PR review fixes; latest verified installer 0.1.4/code5**
Android remains independent: 1.21.39/code262 SOURCE_ONLY on main; verified Android APK 1.21.37/code260 is unchanged.

Every committed desktop executable/build/workflow change increments PATCH and code together. Documentation and verified-artifact finalization do not consume another version. Never overwrite a versioned artifact. Full-parity 1.0.0 is reserved until all parity gates pass.

| Version | State | Changes |
| --- | --- | --- |
| 0.1.0 / 1 | Historical checkpoint 702976a | Compose/JVM workspace and VLC-backed prototype. Superseded; retained in Git history. |
| 0.1.1 / 2 | Native Windows development build | Original C++/Win32 IDE-style workspace, Windows Media Foundation playback, offline self-contained installer, desktop/Start menu shortcuts. |
| 0.1.2 / 3 | Source checkpoint; no frozen delivery | Offline playlists/history/LRC/A–B/sleep/lock/resume, selected metadata/artwork/bounded waveform cache, artwork-color Ambient, sound analysis and saved offsets, coordinated starts/drift trim, native sharing/MSAA, original WASAPI audio path with pinned source-built replaceable LGPL codec DLLs. Installed testing found a fallback startup race. |
| 0.1.3 / 4 | Source checkpoint; no frozen delivery | Fix native-error/fallback startup race caught by installed AIFF testing; repeated installed audio 8/8 passed. One UI run captured a minimized player; a subsequent run passed. |
| 0.1.4 / 5 | Locally verified development delivery | Explicit destination restore, minimized fullscreen regression, installed audio/core/renderer and frozen portable checks passed. See RELEASE_0.1.4.md; full parity and production acceptance remain pending. |
| 0.1.5 / 6 | PR review source; not a new frozen release | Bounded background queue reports admission/failure instead of silently dropping work; UI scan/alignment busy flags recover; audio frames use RAII on exceptional exits. |

The native source/resource/MSI/artifact identities are checked by packaging/validate_version.py. The guard also validates the historical Compose checkpoint. Installer signing, clean-machine testing, full accessibility and full mobile feature parity remain open gates. No APK is rebuilt or relabelled for desktop work.
