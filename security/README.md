# Greater Art security-readiness program — draft

**Status: plan and initial risk assessment only; not an ISO/IEC 27001 certification, OWASP-endorsed assessment, security clearance, or release approval.** Last scoped source reviewed: `main` 1.21.27/code250 at `0078fc8` (11 October 2026); revalidate against actual PR head before relying on these observations.

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
| Minimize file permission | Production still uses `MANAGE_EXTERNAL_STORAGE`; SAF is preview only | [Storage migration](plans/storage-migration.md), issue #148 |
| Signed release chain | Exact-tag workflow exists; successful private release-signing proof not established | [Release plan](plans/release-signing.md), issue #23 |
| Old workstation paths | Reachable history remains a potential disclosure | [History plan](plans/history-remediation.md), issue #24 |
| Vulnerability intake | SECURITY.md repaired to reference *this* repository; private reporting feature must be verified by owner | [Security policy](../SECURITY.md) |
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
