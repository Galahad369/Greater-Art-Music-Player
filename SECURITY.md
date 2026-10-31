# Security policy — Greater Art

## Scope and supported builds

Security reports are welcome for the Android app, Windows native/WinUI editions, source, APKs/desktop bundles, release process, build scripts, GitHub Actions and third-party dependencies. Development happens on `main`. A test receipt applies only to its exact bytes and scope. `SOURCE_ONLY`, a valid signature, passing CI or a checksum does not mean certified or vulnerability-free software.

The [cross-platform trust baseline](security/TRUST_BASELINE.md) describes current safeguards, development/sideload/production channels and acceptance blockers. Signing keys and publisher identities are never changed by that baseline. Historical signed binaries and release receipts remain immutable.

## Report privately

**Do not put exploits, personal media, credentials, signing keys or sensitive workstation information in public issues.**

Use the Greater Art repository's own **Security → Advisories → Report a vulnerability** workflow when the repository has private vulnerability reporting enabled:

https://github.com/Galahad369/Greater-Art-Music-Player/security/advisories/new

Private reporting was confirmed enabled through GitHub's API on 30 October 2026. This does not establish an exercised response SLA. If that option is unavailable, contact the maintainer through an existing trusted private channel. If no private channel is available, you may open a public issue **only to request a private reporting channel**, without technical exploit details or personal information.

Provide affected version/commit, impact, reproducible steps using synthetic test data, and whether a secret may be compromised. The maintainer should acknowledge, assess, track remediation privately, validate a fix and disclose only after mitigation. Do not promise a response SLA until an owner and coverage have been established.

## Security boundaries

- Greater Art is designed for local/offline media use; the production manifest intentionally removes the Internet permission. Verify the merged manifest on every releasable APK.
- The unpackaged Windows preview is offline by design, not an OS network sandbox. It inherits the launching user's file privileges. Selected-folder indexing is not a filesystem access-control boundary.
- User-initiated sharing and browser actions leave the app's boundary. A recipient can have its own network permissions; absence of Android Internet permission does not constrain another app.
- WinUI reports now exclude exception messages/media paths and have bounded retention. Preview/export is explicit. Older raw `failures.log` files are retained until the user confirms **Clear local diagnostics**. Developer inspection and private test captures are separate from this privacy-safe report channel.
- The current product still requests broad storage access on supported Android versions; see [the least-privilege migration plan](security/plans/storage-migration.md). Read-only SAF folder preview does **not** replace production media access.
- Sharing and MediaSession interactions cross Android process boundaries and must be tested as untrusted input and permission boundaries.
- GitHub Actions security, dependency, release-identity and version checks reduce risk but do not establish bug-free software, OWASP MASVS compliance or ISO/IEC 27001 certification.
- Never commit signing secrets, real user media, personal paths or private audit evidence. Treat committed secrets as exposed even when later removed from Git history.

## Security work and evidence

[Security readiness hub](security/README.md) · [Mobile security assessment](security/assessments/masvs-baseline.md) · [Risk register](security/isms/risk-register.md) · [Existing security/admin issue #23](https://github.com/Galahad369/Greater-Art-Music-Player/issues/23).
