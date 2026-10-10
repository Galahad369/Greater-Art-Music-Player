# Greater Art — OWASP MASVS assessment plan (Android)

**Not assessed yet.** This sheet is a security testing plan, not an OWASP certification, accreditation or pass. OWASP does **not** issue official MASVS certificates. Assess actual built APK(s), source, Android versions and documented app features using the [MASVS](https://mas.owasp.org/MASVS/) controls and [MASTG](https://mas.owasp.org/MASTG/) testing instructions.

## What is MASVS?

**Mobile Application Security Verification Standard** = list of things a secure app should do. **MASTG** = testing instructions. A qualified assessor can publish a report saying what they checked and what passed/failed; do not market it as 'OWASP-certified'.

## Scope and evidence per test

Date; reviewer; exact app source SHA/version, APK SHA-256 and signing cert, physical/emulated device + API level, defined threat model, test case, steps, expected/actual, screenshot/log with secrets redacted, result (PASS/FAIL/NOT RUN/N/A with reason), remediation issue and re-test.

| MASVS group | Priority for offline media player | Example concrete tests | Current status |
|---|---|---|---|
| STORAGE | High | Verify permissions, cached images/lyrics, backup settings, shared preferences, revoked SAF URIs, accidental data exposure to other apps | NOT RUN as a full assessment |
| PLATFORM | High | Inspect exported components, MediaSession client trust and browse/search, `content://` grants, malicious URI/share intents, clipboard and permission denial | NOT RUN |
| CODE | High | CodeQL/manual review, dependency SBOM/versions, unsafe input parsing, archive/import limits, leaks in logs, release/debug segregation | PARTIAL automation, manual assessment NOT RUN |
| PRIVACY | High | No network permission in merged APK, bounded local indexing, media data disclosure/retention, opt-in and user-initiated share/delete | NOT RUN end-to-end |
| CRYPTO | Applicable to release signatures / protected secrets | Verify signature, certificate, key custody, version and checksum; check whether app data needs encryption beyond platform protections | NOT RUN end-to-end |
| AUTH | Determine applicability | App has no user account, but cross-process MediaSession and provider authorization still matter; justify any N/A at control level | NOT ASSESSED |
| NETWORK | Determine applicability | Confirm no outbound permission, endpoint or unexpected networking in actual APK/dependencies; assess only real communications | NOT ASSESSED |
| RESILIENCE | Risk-based | Debuggable flags, production signing, tamper/debug settings and realistic threat model for a local media app | NOT RUN |

## Practical test sequence

1. Obtain the current OWASP MASVS control list and MASTG cases; select scope based on assets and threat model.
2. Static assessment: `AndroidManifest.xml`, merged manifest, `aapt`, `apksigner`, debug flags, source SAST, dependency advisories, key and log hygiene.
3. Dynamic assessment on a test phone: deny/revoke all grants, malformed M3U and bad document URIs, external media browsers, exported components, share/delete targets, cross-profile/file-provider behavior, background/PiP/overlay lifecycle.
4. Reproduce and grade actual findings with a fix owner. Repair/retest using an exact new version/source and new signed binary as required by `VERSION_RULES.md`.
5. Have someone competent *independently review* the scope, test evidence, known exclusions and risk acceptance. Publish only a redacted, dated report.

## Completion criteria

All selected control-level tests executed or justified N/A; every finding tracked, fixed or expressly accepted by authorized owner; exact candidate APK/device proof; independent review and scope limitations documented. A passing CodeQL workflow, an offline app, and a successful emulator test are never substitutes for the report.

Official explanation: https://mas.owasp.org/MASVS/04-Assessment_and_Certification/ .
