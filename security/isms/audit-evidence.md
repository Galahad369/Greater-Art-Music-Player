# ISO readiness — evidence, audit and management review

**Template only: no internal audit has been performed and no management review has been held.** Maintain private sensitive proof outside the public repository; link only redacted indexes here.

## Documented-evidence index (complete only after independently checking it)

| Subject | Evidence required | Status |
|---|---|---|
| Scope/ownership | Approved scope, named ISMS owner, stakeholders, organizational context and climate relevance assessment | MISSING |
| Policies | Signed policy with published effective date, review cycle and distribution | DRAFT |
| Assets/classification | Repo/CI/accounts/keys/APKs/end-user and test data inventory, classification/retention | MISSING |
| Risk method | Approved scoring and criteria, authorized risk acceptance, current risk register | DRAFT |
| Risk treatment / SoA | Approved all-93-control SoA, decisions, implementation evidence and due dates | INCOMPLETE |
| Access | Account and CI access reviews, 2FA/permissions proof, signing key custody | MISSING |
| Vulnerability | Private intake test, issue triage, remediation timeline, rescans and reviewer sign-off | PARTIAL |
| Development & suppliers | Documented PR reviews, pinned dependencies, test failures/waivers, supplier reassessment | PARTIAL |
| Release | Signed build artifact hash, tag/source identity, signing certificate, no-secret-leak log | MISSING for current source |
| Recovery/incidents | Response playbook, incident exercise, backup/restore and secure rotation proof | MISSING |
| Internal audit | Independent plan, criteria, sampling, interviews, findings and corrective actions | NOT PERFORMED |
| Management review | Date, management attendees, risks, objectives, feedback, decisions/resources/actions | NOT PERFORMED |

## Internal audit procedure (proposed)

1. Identify qualified, impartial reviewer and approve audit scope, criteria and period.
2. Sample selected PRs, CI failures, release signing, permissions and risk treatment; confirm real execution, not just docs.
3. Record observations as conforming, opportunity for improvement, nonconformity or not verified; link redacted evidence.
4. Assign corrective action owners, root causes, due dates and closure verification.
5. Review the overall ISMS at an actual management meeting; document performance, changing context, audit results, risk status and decisions.

## Review templates

**Internal audit record:** audit ID/date; scope; criteria; auditor impartiality; evidence list; control tested; finding; severity; owner; corrective action; re-test/date.

**Management review record:** date; accountable participants; previous actions; changes in external/internal context; risk trends; audit and incident outcomes; security objectives; resources; decisions; action owners/dates.

**Security incident record (private):** report time; reporter/channel; classification and assets; exposure and containment; notification obligations; remediation; lessons learned. Never publish secrets or actual user media in this repository.

## Certification decision gate

Only seek quotes from accredited ISO/IEC 27001 certification bodies after scope and owner approval, risk treatment implementation, operation records, internal audit, management review and corrective-action evidence. Stage 1/Stage 2 and surveillance requirements belong to the certification body's process. Documentation alone cannot establish certification.
