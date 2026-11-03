# Greater Art — verifiable trust baseline

Date: **30 October 2026**. Scope: Android and the Windows WinUI preview, plus
shared CI/release tooling. This is engineering evidence, not certification.

## What users can see and control

- Android **Settings → Privacy & data** shows the installed package version/code,
  debuggable state, actual signing-certificate SHA-256, declared Internet/broad
  storage access, current overlay and selected-folder grant state, and history.
  It links to Android permission settings and existing media-folder controls.
- History remains off by default. Turning recording off does not silently erase
  old entries. Clearing history is separately confirmed and never deletes media.
- Windows **Settings → Trust & privacy** identifies the preview, reads any embedded
  certificate locally, and explicitly says publisher validity/source cleanliness
  are **not verified by that display**. The process does not perform online chain
  verification. Windows folder selection controls indexing, not OS permissions.
- Reports are local and bounded. Android retains one8KiB report; Windows retains
  at most8 structured reports within32KiB of text. They omit exception messages,
  original source-file names and media paths. No automatic report upload exists.
- Export requires a preview and a separate sharing/save action. Clearing requires
  confirmation. Android's OS-owned exit records are not erased by clearing the
  app's report. Old WinUI raw failure logs are never included in the new preview;
  the explicit clear action removes them. No media/cache/preferences are swept.

App-private storage is not a claim of application-level encryption. A compromised
OS or another app chosen as a share recipient is outside this offline-app boundary.
Developer inspection/private test evidence can contain media details and must not
be published as though it were the privacy-safe diagnostic report.

## Artifact channels and acceptance

| Channel | Acceptance | Not implied |
|---|---|---|
| Development | Exact artifact version/bytes inspected; debug/unsigned state recorded honestly | Safe public distribution, owner-approved signer or device acceptance |
| Compatibility sideload | APK signature valid, expected owner-provided fingerprint, exact package/version/code, non-debuggable/non-test-only, no Internet permission, reviewed exports | New production identity, completed scoped-storage migration or certification |
| Production | Clean build context, approved nonlegacy signing identity, no broad Android storage permission, exact package/version, and expected valid Windows publisher signatures | Physical-device acceptance, acoustic alignment or absence of vulnerabilities |

Use `scripts/verify-trust-artifact.py`. It inspects files; it never signs, executes
an artifact, exports keys, uploads data or publishes. It refuses to overwrite a
receipt and rechecks byte hashes. WinUI bundle checks include compiled XAML/PRI,
configuration and codec evidence, not just EXE/DLL files; links/placeholders are
rejected. Android uses installed SDK tools. Windows uses
the platform's Authenticode inspection, including the two application DLLs for a
public WinUI bundle. Replaceable LGPL codec DLLs are inventoried; they are not
silently locked against the user's own compatible replacements.

The receipt's `buildContext.commit` is a **context claim**, not independent proof
that an arbitrary older binary came from that commit. `dirty` stays explicit.
GitHub provenance binds exact candidate bytes to a workflow separately. A bare
SHA-256 download hash detects differences, not the publisher's identity.

## CI changes prepared in this checkout

- Preserve pinned Actions and existing branch protection. No bypass or repository
  administration change is made.
- Windows CI initializes C++/C# CodeQL before the existing native and WinUI builds
  and analyzes those compilations afterward. It also runs the graph-model
  executable, version checks and native ABI tests. Confirm both language databases
  contain owned source before recording coverage; configuration alone is not
  acceptance evidence.
- Android CI inspects the **built APK**, not just source manifest text. Negative
  policy tests reject wrong versions/signers, Internet access, unknown exports,
  debug/test-only distribution and false production relabeling.
- Export allowlisting includes the exact Media3 Bluetooth validation and profile
  installer components **only with their platform signature permissions**. Names
  alone are insufficient. These were found in the real merged1.21.93 manifest.
- Dependabot configuration covers NuGet in addition to Gradle/Actions. Repository
  security-update enablement and mandatory-check administration are separate owner
  settings; they were inspected but not changed here.
- The tag-release workflow requires `ANDROID_RELEASE_CERT_SHA256` and passes the
  production gate before attestation/publication. Missing identity, legacy signing
  or remaining All Files Access blocks it. The user must resolve signing/migration
  separately; this task does not force a key change to make a gate green.
- Candidate APK, trust receipt and dependency inventory are attested using the
  official SHA-pinned `actions/attest` before publication. No workflow is launched
  by this task. The inventory is not a CVE/malware scan or complete standardized SBOM.

CI configuration records intended checks. Completed hosted coverage must be tied
to an exact commit and run; it does not establish production signing or interactive
acceptance.

## Verification and remaining work

Completed checks and exact Windows delivery status belong in
[`desktop/docs/TRUST_BASELINE_2026-10-30.md`](../desktop/docs/TRUST_BASELINE_2026-10-30.md).
Android integration/ownership status belongs in
[`greater-art/docs/TRUST_BASELINE_2026-10-30.md`](../greater-art/docs/TRUST_BASELINE_2026-10-30.md).

Remaining production gates include owner-approved signing and recovery, scoped
storage operation/sidecar parity before permission removal, physical-device QA,
clean Windows deployment, full platform security assessment and historical Git
privacy remediation. None is satisfied by adding this UI or documentation.

## Workflow mistakes prevented

1. Do not call a non-debuggable APK a production-signed release. Read its actual
   certificate and preserve upgrade compatibility.
2. Do not equate Windows folder indexing with a sandbox, or an embedded certificate
   with verified publisher trust.
3. Do not put `Exception.ToString()` or `Throwable.message` into an exportable report.
4. Do not add a test project without running it in CI.
5. Do not assume Android dependency exports are unprotected. Inspect component
   type, name **and** required permission in the real merged APK.
6. Do not publish first and attest later. Failed provenance must stop publication.
7. Do not overwrite a release or compete with another agent's build/version files.

## Primary references

- [Android signing and upgrade identity](https://developer.android.com/studio/publish/app-signing)
- [Android broad storage access](https://developer.android.com/training/data-storage/manage-all-files)
- [Microsoft code-signing options](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/code-signing-options)
- [GitHub artifact attestations](https://docs.github.com/en/actions/how-tos/secure-your-work/use-artifact-attestations/use-artifact-attestations)
- [Official attestation action](https://github.com/actions/attest)
- [OWASP MASVS](https://mas.owasp.org/MASVS/)
