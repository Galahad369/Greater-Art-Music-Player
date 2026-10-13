# Greater Art Desktop version policy

Current desktop: **0.1.1 (code 2)**
Release state: **DEVELOPMENT**
Android remains independent: 1.21.39/code262 SOURCE_ONLY on this branch; verified Android APK 1.21.37/code260 is unchanged.

Every committed desktop executable/build/workflow change increments PATCH and code together. Documentation and verified-artifact finalization do not consume another version. Never overwrite a versioned artifact. Full-parity 1.0.0 is reserved until all parity gates pass.

| Version | State | Changes |
| --- | --- | --- |
| 0.1.0 / 1 | Historical checkpoint 702976a | Compose/JVM workspace and VLC-backed prototype. Superseded; retained in Git history. |
| 0.1.1 / 2 | Native Windows development build | Original C++/Win32 IDE-style workspace, Windows Media Foundation playback, offline self-contained installer, desktop/Start menu shortcuts. |

The native source/resource/MSI/artifact identities are checked by packaging/validate_version.py. The guard also validates the historical Compose checkpoint. Installer signing, clean-machine testing, full accessibility and full mobile feature parity remain open gates. No APK is rebuilt or relabelled for desktop work.
