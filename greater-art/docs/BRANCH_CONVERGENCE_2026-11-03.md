# Approved branch repair — Android1.22.2/code323

## Scope and identity

Owner approval: “Yes—repair, verify, merge, and clean up”, continued through the
approved convergence handoff. No app redesign, desktop Trust adoption, history
rewrite, signing-key replacement or verified-release promotion is included.

Integration source: `c873d0766c80927ffa64093b08e7214661d6a71a`.
Parents: frozen mobile checkpoint `8d036ac` and current main `0a295c6` (desktop
security PR215). Android production tree remains
`3097172f031725dd475223e37181a21a7bbb2faf`, exactly equal to1.22.1. Desktop files
and its two owned configuration files exactly match main's reviewed PR215.

[Replacement PR216](https://github.com/Galahad369/Greater-Art-Music-Player/pull/216)
contains the retained versioned mobile history and only these new build changes:

- AGP9.4.1 →9.5.0.
- Three setup-gradle pins →`8da46b539cd8fe6ab27ca75128919a18d757604c` (6.4.1).
- Atomic Android1.22.2/code323, SOURCE_ONLY, plus accurate documentation pointers.

AGP9.5's documented Gradle≥9.6.0/JDK≥17/API37 prerequisites are satisfied by
the existing9.8.2/Java21/API37 baseline. This is compatibility evidence, not an
APK runtime result. [Official AGP release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes).
The action update includes GHSA-chpw-2mqm-95cc remediation. No affected
download-and-submit configuration or token compromise was observed here.
[Upstream release](https://github.com/gradle/actions/releases/tag/v6.4.1),
[advisory](https://github.com/gradle/actions/security/advisories/GHSA-chpw-2mqm-95cc).

## Original branches are preserved, not blindly merged

PR213: `5668857aa95665a029e79fa322bd88ac53d9cf5e`.
PR214: `de7162354daab0cb16a4f4773526b7a800e06d95`.
The complete original authored mail patches are retained in
`imported-patches/0001-Bump-com.android.application.patch` and
`imported-patches/0001-Bump-gradle-actions-setup-gradle.patch`.
Local `refs/archive/dependabot-pr213-original` and
`refs/archive/dependabot-pr214-original` preserve the exact commits.

The unchanged version validator was executed against each original commit and
failed both: build code changed without PATCH/code advancement and while marked
VERIFIED. The same validator passes the replacement range. Do not weaken the
validator, import invalid commits, force-align bot branches or rewrite history.
Close/delete superseded branches only after replacement CI/merge, after checking
their exact remote tips again. Final operations are recorded in PR216.

## Local verification and actual blocker

- Current-tree/range version guard passes;26 artifact-policy unit tests pass.
- Public repository audit passes with Git fsmonitor disabled for that invocation.
  An earlier run emitted daemon errors despite an OK summary; it is not used as
  the clean result. No scanner rule or branch protection was changed.
- A separate agent reviewed the exact integration commit and retained tests.
  It found no actionable changes to player/geometry/permission assertions,
  deadlines, quality, desktop security or source-only release boundaries.
- All five requested local Gradle tasks were attempted serially using the
  repository wrapper/Java21. Configuration stopped before compilation because
  AGP9.5.0 could not resolve. No current-version local build/test pass is claimed.
- A bounded Java21 request to the same public Google Maven POM confirmed
  `SSLHandshakeException: PKIX path building failed`, caused by
  `SunCertPathBuilderException: unable to find valid certification path`.
  Windows HTTPS retrieved the actual9.5.0 POM successfully. The missing
  Java-trusted chain, not a missing AGP release, is the demonstrated boundary;
  the certificate-chain origin is not established.
- The owner did not approve a different trust store. Java/Windows certificate
  stores, TLS validation, repositories and emulator configuration remain unchanged.
  No certificates were imported and no Build Scan was uploaded.

Required GitHub contexts are `greater-art`, `Greater Art version guard`,
`privacy-and-secret-audit`, `dependency-review`, `codeql` and `desktop`, bound to
GitHub Actions app15368. Strict/up-to-date protection remains enabled. Inspect
the final PR head and post-merge run conclusions; this document does not assert
pending checks passed. Green source CI cannot complete local APK acceptance.

Local evidence is in sibling `App/output/branch-convergence-20261103/`, including
frozen workflow/checkpoint inputs, the failed execution receipt, transport
diagnostics and reliable `privacy-local-fsmonitor-off.log`. The initial workflow
contract's output marker was corrected before execution to match the validator's
literal `Greater Art version guard OK`; no assertion or source validator changed.
There is no fabricated app-runtime challenge: this change adopts build tools.
Actual original version-guard failures are retained separately from app tests.

## Immutable APK and unfinished Windows work

The exact1.22.1 optimized APK remains in sibling
`App/output/android-release-1.22.1-20261103/GreaterArt-1.22.1.apk`:
5,495,125 bytes, SHA-256
`e952cad07dcdf2b7c381886cc3b989b3d63772cc2ab22ab595e9fe9d48222e7e`.
See [its candidate receipt](RELEASE_1.22.1.md). It is not1.22.2, not a newly
verified release, and must not be overwritten or renamed. Latest verified APK
remains1.21.93/code316; optimized return/Mini, physical QA and shared-output
parity gates remain separate.

The25 unfinished Windows/workflow files were copied byte-for-byte and rehashed
under sibling `output/branch-convergence-20261103/windows-preservation/` with
`manifest.csv`. They are also retained in stash OID
`6554c055a5d138537dd53a03c25445e34012d5a9` and archive ref
`refs/archive/windows-trust-pre-convergence-20261103`.
Their desktop0.1.22/code23 identity conflicts with PR215; they were not discarded,
published, or blindly restored over that fix. A future Windows reconciliation
must allocate the next valid desktop identity and verify restoration before
dropping either preservation copy. A clean checkout is not evidence that this
unfinished work was completed.

## Prevention

Inspect and preserve exact branch diffs before integration. Consume versions
atomically for bot updates too. Keep the default certificate and security
policies intact when a build environment fails. Separate source CI, local
assembly, exact-artifact runtime testing and release promotion. Delete reviewed
superseded refs only after their useful changes and recovery copies are proven.
