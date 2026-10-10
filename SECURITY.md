# Security policy — Greater Art

## Scope and supported builds

Security reports are welcome for the Android app, source, packaged APKs, release process, build scripts, GitHub Actions and third-party dependencies. Development happens on `main`. Only explicitly owner-verified distributable APKs should be treated as verified releases; `SOURCE_ONLY` source builds and historical APKs are not automatically certified.

## Report privately

**Do not put exploits, personal media, credentials, signing keys or sensitive workstation information in public issues.**

Use the Greater Art repository's own **Security → Advisories → Report a vulnerability** workflow when the repository has private vulnerability reporting enabled:

https://github.com/Galahad369/Greater-Art-Music-Player/security/advisories/new

If that option is unavailable, contact the maintainer through an existing trusted private channel. If no private channel is available, you may open a public issue **only to request a private reporting channel**, without any technical exploit details or personal information. Private reporting availability has not been independently verified.

Provide affected version/commit, impact, reproducible steps using synthetic test data, and whether a secret may be compromised. The maintainer should acknowledge, assess, track remediation privately, validate a fix and disclose only after mitigation. Do not promise a response SLA until an owner and coverage have been established.

## Security boundaries

- Greater Art is designed for local/offline media use; the production manifest intentionally removes the Internet permission. Verify the merged manifest on every releasable APK.
- The current product still requests broad storage access on supported Android versions; see [the least-privilege migration plan](security/plans/storage-migration.md). Read-only SAF folder preview does **not** replace production media access.
- Sharing and MediaSession interactions cross Android process boundaries and must be tested as untrusted input and permission boundaries.
- GitHub Actions security, dependency, release-identity and version checks reduce risk but do not establish bug-free software, OWASP MASVS compliance or ISO/IEC 27001 certification.
- Never commit signing secrets, real user media, personal paths or private audit evidence. Treat committed secrets as exposed even when later removed from Git history.

## Security work and evidence

[Security readiness hub](security/README.md) · [Mobile security assessment](security/assessments/masvs-baseline.md) · [Risk register](security/isms/risk-register.md) · [Existing security/admin issue #23](https://github.com/Galahad369/Greater-Art-Music-Player/issues/23).
