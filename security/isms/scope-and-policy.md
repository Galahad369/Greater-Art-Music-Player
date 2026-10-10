# Draft ISMS scope and security policy

**DRAFT — owner review and organizational approval required. This is not evidence of an operating or certified ISMS.**

## Proposed scope

"Security of the design, AI-assisted development, testing, maintenance, vulnerability handling, dependency management, and signed distribution of the Greater Art Android media player and its GitHub repository, operated by [legal entity or individual business TO BE IDENTIFIED]."

**Scope boundaries to confirm:** maintainer(s) and devices; GitHub organization/repository and Actions runners; secrets and keystore custody; user support/reporting channels; third-party dependencies; sideload vs Google Play vs tagged release channels. User-owned on-device media remain outside the maintainer's custody unless explicitly received for support/testing; this must be verified, not assumed. The app currently declares no Internet permission.

Out-of-scope claims require written reasoning; never exclude supplier/identity/signing risks simply because the app is offline.

## Roles — unassigned, pending approval

| Role | Responsibility | Accountable person |
|---|---|---|
| ISMS owner / top management | Approves scope, objectives, budget, risk acceptance, annual management review | **TBD** |
| Engineering/security owner | Vulnerability triage, access and secret controls, secure coding, release gates | **TBD** |
| Release/signing custodian | Custody/rotation/recovery of production keystore and tagged artifact provenance | **TBD** |
| Independent internal auditor | Audits the ISMS without auditing own decisions where practicable | **TBD** |

## Proposed information-security policy

1. Minimize permissions and collected/retained data; user media stays local unless the user explicitly shares it.
2. Changes require traceable source, test results, security review and version/release provenance.
3. Restrict write and secret access; avoid plaintext secrets in source, logs and CI outputs.
4. Report and triage suspected vulnerabilities privately; record incidents, impact, remediation and lessons.
5. Track supplier risks and dependencies; do not blindly trust AI-generated changes.
6. Preserve evidence and backups securely with access limits and retention rules.
7. Review risks and effectiveness regularly, after incidents, and before major release changes.

## Proposed measurable objectives — not achieved yet

- Every distributable release has signed source identity, manifest/cert/hash, reviewed tag and bounded device smoke evidence.
- 100% of actionable security findings have owner, risk severity, disposition, date and closure evidence.
- No unjustified broad file permission in the production manifest once migration parity is proven.
- No known high-risk live signing credentials are publicly reachable; identified leaks trigger rotation/remediation.
- At least one documented internal audit and management review per planned ISMS review cycle.

## ISO clauses 4–10 implementation sequence

Context and interested parties → scope and leadership policy → risk method/objectives → people, competency, communications and controlled documents → secure operations and risk treatments → monitoring/measurement/internal audit/management review → corrective actions and improvement.

ISO/IEC 27001:2022 Annex A controls are selected on assessed risks; see [draft SoA](statement-of-applicability.md). Review the exact licensed standard text for a final complete SoA. Consider the applicable 2024 management-system climate amendment when assessing organizational context.

**Approval record:** pending (person, date, scope version, risk acceptance authority).
