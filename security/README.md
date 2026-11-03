# Greater Art security-readiness program — draft

**Status: partial safeguards and acceptance evidence, not ISO/IEC 27001 certification, OWASP endorsement, security clearance or blanket release approval.** See the dated [cross-platform trust baseline](TRUST_BASELINE.md). Older assessment/ISMS drafts below retain their original scope and are not current-version acceptance receipts.

On **30 October 2026**, the existing Android **1.21.93/code316** APK's identity, SHA-256 and compatibility sideload certificate were independently rechecked. It is non-debuggable and has no Internet permission, but retains All Files Access and the legacy Android Debug certificate. Windows WinUI **0.1.22/code23** is a local development candidate, not an approved publisher-signed release. GitHub's latest public release was still v1.4.0 when checked; local receipts must not be confused with a new public publication. Concurrent Android1.21.94 work and its artifacts belong to another writer; trust integration does not relabel them.

## Plain-language map

- **ISO/IEC 27001**: organization-wide information-security management system (**ISMS**). External certification audits the defined organization/process scope, not just an APK.
- **OWASP MASVS**: public security checklist for mobile apps. **MASTG**: how to test those controls. OWASP does **not** issue MASVS app certificates.
- **SAF / MediaStore**: Android APIs allowing narrower access to user-selected documents or indexed media.
- **SoA**: *Statement of Applicability* explaining which ISO Annex A controls apply, why, and what evidence exists.
- **CI**: automated checks. Passing CI does not verify an app on a physical device.
- **Risk treatment**: eliminate, mitigate, transfer, or explicitly accept a documented risk with accountable authority.

## Workstreams / references

| Track | Current state | Plan / evidence needed |
|---|---|---|
| Cross-platform user trust | Runtime permission/build facts and explicit privacy-safe report controls prepared; platform acceptance tracked separately | [Trust baseline and verification limits](TRUST_BASELINE.md) |
| Minimize file permission | Selected-folder URI Library/playback works; broad permission remains until sidecar/operation parity | [Storage migration](../greater-art/docs/STORAGE_MIGRATION.md), issue #148 |
| Signed release chain | Exact-tag workflow exists; successful private release-signing proof not established | [Release plan](plans/release-signing.md), issue #23 |
| Old workstation paths | Reachable history remains a potential disclosure | [History plan](plans/history-remediation.md), issue #24 |
| Vulnerability intake | Correct repository link and private reporting enabled; intake exercise pending | [Security policy](../SECURITY.md) |
| Technical mobile tests | Partial static and emulator evidence; full independent review not completed | [MASVS evidence checklist](assessments/masvs-baseline.md) |
| ISO management system | Organizational scope, owners, decisions, audits not established | [Scope/policy](isms/scope-and-policy.md), [risk register](isms/risk-register.md), [SoA draft](isms/statement-of-applicability.md), [audit/evidence](isms/audit-evidence.md) |

## Rules for claiming progress

Document dated test commands, exact source/manifest hash, environment, expected/actual results, reviewer, and limitations. Use `NOT RUN`, `PASS`, `FAIL` or `NOT APPLICABLE (justified)` rather than guessing. Do not copy screenshots containing usernames or local media to the public repository. Re-run after relevant changes.

## Gates

1. Security-contact link and draft plans reviewed, with existing behavior unchanged.
2. Owner identifies organization/maintainer, accountable risk owner, intended release channel and supported devices.
3. Fix and verify permission and signing issues independently.
4. Complete repeatable MASVS tests on signed candidate APKs, including negative and revocation paths.
5. Operate the ISMS: assign owners, approve scope/SoA/risk treatments, retain internal audit and management-review evidence. Seek accredited ISO audit **only then**.

Sources: https://mas.owasp.org/MASVS/ , https://mas.owasp.org/MASVS/04-Assessment_and_Certification/ , https://developer.android.com/training/data-storage/manage-all-files .
