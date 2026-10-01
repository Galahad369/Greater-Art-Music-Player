# Greater Art

Greater Art is a local-first Android audio and video player. This repository contains the app, its documentation, release APKs, and the checks used to build and audit it. Canonical repository: `Galahad369/Greater-Art-Music-Player`.

> **Vibe-coded disclosure:** Greater Art was built through iterative work with AI coding agents. Human direction, product decisions, device feedback, and acceptance guide the work; substantial code and documentation are AI-assisted.

- Current source: **1.14.16 (code 131, SOURCE_ONLY)**
- Latest verified APK: **1.14.3 (code 118)** — [download](greater-art/releases/GreaterArt-1.14.3.apk)
- [App documentation](greater-art/README.md) · [Handoff and verification](greater-art/HANDOFF.md)
- [User demonstration](greater-art-user-demo.html) · [Technical demonstration](greater-art-technical-demo.html)

Greater Art has no Internet permission, advertisements, accounts, analytics, telemetry, or cloud playback dependency. Library files stay on the device. Playback history is optional, off by default, and can be burned locally.

## Build and verify

See [greater-art/README.md](greater-art/README.md) for the Java 21/Android SDK build command. The repository's [version check](scripts/validate-greater-art-version.py) and [public-repo security audit](scripts/audit-public-repo.ps1) help keep the APK, docs, and public files aligned. Public APKs are experimental sideload builds; review source and permissions before installation.

The former LocalKit and calculator projects have been removed from this repository's current tree. Their old commits remain in Git history; any private or local-only files were not included in this cleanup.

Security reports: [SECURITY.md](SECURITY.md).
