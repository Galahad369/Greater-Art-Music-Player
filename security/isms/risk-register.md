# Initial information-security risk register — draft

This is a **hypothesis-based starting register**, not a completed risk assessment. Assign an accountable owner, verify assumptions and obtain explicit decisions. Never store sensitive incident data publicly.

Scoring proposal: likelihood L and impact I each 1–5; score = L×I. Priority: 1–5 Low, 6–10 Moderate, 11–15 High, 16–25 Critical. Recalculate residual score only after verified treatment.

| ID | Risk scenario / affected assets | L×I | Proposed treatment and acceptance evidence | Owner / state |
|---|---|---:|---|---|
| R01 | All Files Access can expose broader on-device media than needed | 4×4=16 | Replace with MediaStore and persisted user-picked SAF grants without feature regression; no-broad-permission device tests; #148 | TBD / OPEN |
| R02 | Missing evidence that releases use approved production signing identity and exact tagged source | 3×5=15 | Controlled keystore, protected tags, isolated secrets, manifest/cert/hash checks, reproducible build provenance; #23 | TBD / OPEN |
| R03 | Public reachable Git history includes workstation paths or generated local output | 4×3=12 | Inventory reachable tags/branches, classify exposure, rotate any actual secrets, controlled history rewrite plus ref verification; #24 | TBD / OPEN |
| R04 | Incorrect vulnerability-reporting destination delays confidential triage | 3×4=12 | Correct SECURITY.md; owner test the actual private advisory workflow and response ownership | TBD / MITIGATION PROPOSED |
| R05 | Persisted SAF/background URI grants can remain or become inaccessible after UI changes | 3×4=12 | Permission grant/revoke state machine, transactional preference updates, device revocation tests | TBD / OPEN |
| R06 | Exported MediaLibraryService and cross-app media intents admit unexpected callers/inputs | 3×4=12 | Review `onConnect`, trusted controllers, URI/path validation and external-client instrumentation | TBD / TEST PENDING |
| R07 | Untrusted or vulnerable third-party/mobile build dependencies compromise release integrity | 3×4=12 | Dependabot/dependency review, pinned Actions, CodeQL, periodic triage and escalation | TBD / PARTIALLY CONTROLLED |
| R08 | Destructive delete/share action accesses or exposes unintended user files | 3×5=15 | Exact URI-scoped grants, platform delete prompts, negative paths and consent/confirm tests | TBD / TEST PENDING |
| R09 | Logs/diagnostics, backups, or test artifacts inadvertently reveal private media/path metadata | 3×3=9 | Data inventory, redaction, backup allowlist, no public real-media fixtures, retention/deletion review | TBD / TEST PENDING |
| R10 | AI-assisted generated code or wrong PR integration bypasses human review/security checks | 4×3=12 | Require diff review, secure design checklist, CI/review gate, independent acceptance on sensitive changes | TBD / OPEN |
| R11 | Device loss/compromise exposes stored settings, thumbnails or media metadata | 3×3=9 | Verify storage locations, OS protections, nonbackup data classification and retention minimization | TBD / REVIEW |
| R12 | Support or incident response is delayed because no owner, reporting SLA or incident workflow exists | 3×4=12 | Assign owner and private channel, triage policy, incident log, release rollback plan and periodic exercise | TBD / OPEN |

For each risk record: discovery/source, existing controls, impact to confidentiality/integrity/availability, measured likelihood basis, chosen response, due date, owner approval, residual L×I, linked evidence and review date.

**No risk is currently accepted on behalf of the user.** This table alone does not fulfill ISO assessment or treatment obligations.
