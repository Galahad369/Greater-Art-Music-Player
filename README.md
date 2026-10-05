# Greater Art

Greater Art is a local-first Android audio and video player. This repository contains the app, its documentation, release APKs, and the checks used to build and audit it: `Galahad369/Greater-Art-Music-Player`.

> **Vibe-coded disclosure:** Greater Art was built through iterative work with AI coding agents. Human direction, product decisions, device feedback, and acceptance guide the work; substantial code and documentation are AI-assisted.

- Verified build: **1.15.75 (code 205)** — repaired video-background ownership, shared Library/Nodes/Stack header, session-owned sound alignment, isolated Fit video previews, atomic caches and live video-derived Ambient gradients. 193 JVM tests, lint/build and three API 36 device regressions pass. Emulator state is not acoustic synchronization proof; see the handoff.
- APK: [GreaterArt-1.15.75.apk](greater-art/releases/GreaterArt-1.15.75.apk)
- [App documentation](greater-art/README.md) · [Handoff and verification](greater-art/HANDOFF.md)
- [User demonstration](greater-art-user-demo.html) · [Technical demonstration](greater-art-technical-demo.html)

Greater Art has no Internet permission, advertisements, accounts, analytics, telemetry, or cloud playback dependency. Library files stay on the device. Playback history is optional, off by default, and can be burned locally.

Stack plays up to eight local tracks together. Choose **Now-playing video → Fit** in Background settings to tile Stack videos behind Library, Nodes and Stack. **Cut to screen size (Crop)** remains the default; Stretch is removed. Optional video previews need device decoder/GPU capacity and never reduce source quality. The single primary video surface serves the wallpaper while docked artwork remains visible; expanding Now Playing transfers that surface directly.

## Build and verify

See [greater-art/README.md](greater-art/README.md) for the Java 21/Android SDK build command. The repository's [version check](scripts/validate-greater-art-version.py) and [public-repo security audit](scripts/audit-public-repo.ps1) help keep the APK, docs, and public files aligned. Public APKs are experimental sideload builds; review source and permissions before installation.

The former LocalKit and calculator projects have been removed from this repository's current tree. Their old commits remain in Git history; any private or local-only files were not included in this cleanup.

Security reports: [SECURITY.md](SECURITY.md).
