# ISO/IEC 27001:2022 Statement of Applicability — working draft

**NOT APPROVED / NOT COMPLETE.** A final SoA must explicitly assess **all 93 Annex A controls**, select or exclude each with rationale, identify implementation status and retain evidence. This is a *risk-mapped starter*, not a claim that the standard is fulfilled. Use a properly licensed copy of ISO/IEC 27001 for the definitive control wording.

| Control area / Annex A reference examples | Provisional applicability and rationale | Current evidence / gap |
|---|---|---|
| 5.1 information security policies | Yes: operating rules and approvals | Draft [scope and policy](scope-and-policy.md); no owner approval |
| 5.9 inventory of information and other associated assets | Yes: code, releases, keys, CI, device-local assets | Asset inventory and ownership not signed off |
| 5.15 / 5.18 access control / access rights | Yes: repo, production key, vendor accounts | Main protection partially evidenced; owner/admin review pending |
| 5.19 / 5.21 supplier relationships / ICT supply chain | Yes: GitHub, Google/Android and Gradle dependencies | Dependency review exists; supplier inventory/evaluation pending |
| 5.24–5.28 incident preparedness, response and evidence | Yes: vulnerability and incident processes | SECURITY.md revised; no exercised incident records |
| 5.31 / 5.34 legal requirements / privacy and PII protection | Likely: media data, user rights, distribution jurisdictions | Confirm applicable laws and data flow; obtain qualified legal review when necessary |
| 5.35 / 5.36 independent review / compliance | Yes: security audits and findings | No periodic independent ISMS assessment established |
| 5.37 documented operating procedures | Yes: builds, response and release | Partial workflow docs; approvals and execution logs incomplete |
| 8.8 technical vulnerability management | Yes: mobile dependencies and code | CodeQL, dependency checks; manual findings treatment pending |
| 8.9 configuration management | Yes: Android manifest, Gradle, CI and repos | Version guard / reviewed configuration, change process must be documented |
| 8.10 information deletion | Yes: user-file deletion and caches | Deletion confirmations exist; test actual behavior and permissions |
| 8.12 data leakage prevention | Yes: code logs, issue reporting, APK handling | Secret/path scans; historical path issue #24 unresolved |
| 8.15 / 8.16 logging / monitoring | Yes with privacy minimization | No documented monitoring/retention/access policy |
| 8.24 use of cryptography | Yes for signed releases and any sensitive store | Signed-release key custody evidence still missing |
| 8.25–8.29 secure SDLC, application security requirements, secure coding and security tests | Yes | Android CI and CodeQL run; MASVS test evidence incomplete |
| 8.32 change management | Yes: patches, PRs, tagged release chain | Guard + PR; require reviewed scope and release attestation |

**Decision log:** All decisions above are proposals, not assigned risk treatment approvals. Controls not listed are **NOT ASSESSED**, not excluded. For the final SoA create a row for every Annex A control, with Applicability (Yes/No), exclusion justification, implementation (planned/partial/operating), owner, linked risk(s), objective evidence and approval date. Do not infer that absent authentication or network functionality automatically eliminates all related organizational obligations.

Related: [risk register](risk-register.md), [audit/evidence](audit-evidence.md), [security hub](../README.md).
