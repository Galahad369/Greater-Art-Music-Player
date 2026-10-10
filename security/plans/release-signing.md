# Release signing and artifact-provenance acceptance plan

**Current:** `.github/workflows/github-release.yml` checks out the exact tag, validates versionName, builds tests/lint/`assembleRelease`, checks signature/package/version and attaches SHA-256. It uses dedicated secret references. CI Android debug builds are **not release signing proof**. No successful modern end-to-end production-tag signing run has been independently established. Current documented source 1.21.27/code250 is `SOURCE_ONLY`; last documented owner-verified APK is 1.21.18/code241. See issue #23.

## Roles and prerequisites

- Owner selects distribution channel: private signed sideload (may have established debug certificate), signed GitHub production release, or Google Play. **Do not interchange certificates** or call a debug APK a production-signed release.
- Choose/recover an authorized keystore, keep the signing private key offline/in encrypted storage with an owner-approved recovery plan; validate intended cert fingerprint and update compatibility on a disposable phone.
- In GitHub Actions configure `ANDROID_RELEASE_KEYSTORE_B64`, `ANDROID_RELEASE_STORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, `ANDROID_RELEASE_KEY_PASSWORD` as Actions secrets **through owner-controlled settings**; no secret values in PRs, logs, files, issue comments or bot chat.
- Verify authorized tag creators, branch protections, secret/environment access boundaries and least-privilege workflow permissions. Record who approved release and how release key rotation/loss will be handled.

## First controlled release proof — not executed

1. Pick an exact reviewed source commit and unused version; preserve `VERSION_RULES.md` and immutable verified APK ledger.
2. Complete CI/security checks and physical smoke tests; human release approval.
3. Create a protected tag pointing to that exact commit; trigger signed-release workflow. **Never guess or retroactively rewrite a version tag.**
4. Confirm artifact `packageName`, `versionName`, `versionCode`, signing cert fingerprint, signature schemes, 16KiB alignment where applicable, SHA-256 and no DEBUG/test instrumentation components/permissions.
5. Install on a clean physical device; check upgrade from the **same signing identity** where applicable (otherwise document incompatibility), full playback, media access, share/delete, overlays, background behavior and rollback.
6. Publish immutable binary/checksum/release provenance; attach redacted CI/job links and owner confirmation. Update verified ledger only against exact bytes with required policy guard intact.

## Go/no-go

**GO:** trusted tag/source mapping, signed artifact with expected fingerprint, all checks PASS, smoke evidence, no committed secrets, documented responsible approver. **NO-GO:** missing signing key, unavailable owner consent, skipped tests, manifest mismatch, failed device upgrade, old APK renamed/copied to a new version, or incorrect `SOURCE_ONLY` → `VERIFIED` claim.

Do not start a signing workflow from this documentation PR. Tracker: [#23](https://github.com/Galahad369/Greater-Art-Music-Player/issues/23).
