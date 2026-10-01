# Security Policy

## Scope

Security reports are welcome for the Android source, packaged APKs, GitHub Actions, and reusable skills in this repository. Only the latest `main` branch and the newest tagged app releases are supported.

## Report privately

Do not open a public issue containing a live credential, private key, personal information, or an exploitable vulnerability.

Use GitHub's private vulnerability reporting page:

<https://github.com/Galahad369/Greater-Art-Music-Player/security/advisories/new>

If private reporting is unavailable and a real credential is exposed, revoke or rotate it immediately before discussing it anywhere. A committed secret must be treated as compromised even if the file is later deleted, because Git history and forks may retain it.

## Repository guarantees and limits

- Greater Art intentionally contains no analytics or advertising SDKs.
- Greater Art intentionally declares no Internet permission.
- Automated checks scan for known credential formats, sensitive filenames, local user paths, vulnerable code patterns, and dependency risks.
- Automated scanning reduces risk but cannot prove that arbitrary source or binary data is harmless.

Do not submit real passwords, API keys, seed phrases, wallet secrets, signing keys, personal documents, or private media as test data.
