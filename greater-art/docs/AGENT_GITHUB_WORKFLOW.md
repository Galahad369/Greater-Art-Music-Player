# Greater Art — GitHub-Connected Agent Workflow

Use this workflow when the AI agent can read and write the GitHub repository, inspect pull requests and Actions, and perform the work remotely without a trusted local Android/device build.

## 1. Establish live repository state first

Before editing anything, query GitHub for:

- current `main` SHA,
- open pull requests,
- active branches,
- recent commits,
- `greater-art/VERSION_RULES.md`,
- `greater-art/HANDOFF.md`,
- `greater-art/app/build.gradle.kts`.

Do this again before merge if the task lasts long enough for concurrent agents to have changed the repository.

Never rely on a remembered SHA or version when GitHub can be read live.

## 2. Review concurrent work before creating new work

For every divergent branch or relevant pull request:

1. Compare it with `main`.
2. Inspect the actual changed files, not just the PR title/body.
3. Read review comments and unresolved threads.
4. Check whether the branch uses a consumed/stale version.
5. Decide whether its implementation is useful, incomplete, duplicate, broken, or unrelated.

Do not blindly merge an agent branch. If it contains useful logic but invalid history/versioning, recreate the useful change cleanly from current `main` under the next valid version.

A version used by a published executable commit is consumed even if that branch is later rejected.

## 3. Scope discipline

Implement only the user's current requested patch set.

Treat diagnostics and future-feature notes as backlog unless the user has asked to implement them now. Do not allow autonomous agents or old PR descriptions to broaden scope.

Keep unrelated fixes on separate future versions.

## 4. Version contract

For executable/configuration/build/resource/manifest/tooling changes:

- allocate the next valid PATCH/versionCode from live `VERSION_RULES.md`,
- bump Gradle in the same commit as the source change,
- update `VERSION_RULES.md` current source and ledger coherently,
- update `HANDOFF.md` current source while preserving verified-APK metadata,
- never overwrite the latest verified APK metadata with a source-only version.

Documentation-only commits may follow the explicit documentation exception in `VERSION_RULES.md`.

If another branch consumes the planned version before your source commit is finalized, stop using that number and advance.

## 5. Build the patch from current main

Prefer a focused branch named for the version and change.

Keep the source diff minimal. Preserve unique useful work from reviewed branches, but do not preserve broken history merely for authorship continuity.

Add tests for the actual behavioral invariant. Avoid tests that only restate implementation details.

## 6. Pull-request gate

Open a PR to `main` for executable work.

Before merge, inspect:

- complete diff,
- PR comments,
- submitted reviews,
- unresolved review threads,
- required Actions/checks.

Expected gates normally include:

- version consistency,
- Android unit tests,
- lint,
- debug/verification assemble,
- privacy/history audit,
- dependency review on the PR,
- CodeQL/security analysis.

A green unrelated job does not compensate for a failed compile, version guard, or CodeQL build.

Do not merge while required checks are pending or failed unless the user explicitly directs an emergency exception and the repository rules allow it.

## 7. Merge strategy

Preserve the reviewed versioned commits. Prefer a merge commit whose tree exactly matches the approved branch parent; VERSION_RULES explicitly permits that pure integration without consuming a new version. Avoid squash/rebase policies that obscure consumed identities. Conflict-resolution code requires a fresh PATCH/code and verification.

Use the expected PR head SHA when merging so GitHub rejects the merge if another actor moved the branch.

After merge, fetch the resulting `main` SHA and inspect push-triggered workflows. Confirm the final main tree, not merely the pre-merge PR head, passes the required checks.

## 8. Branch convergence

When the user's policy requires branch convergence:

1. Re-list branches after the merge.
2. Compare every divergent branch with final `main`.
3. Preserve any unique useful work intentionally.
4. Close obsolete PRs with a clear reason.
5. Delete only reviewed exact branches after proving their tips are ancestors of final main. If deletion is unavailable, report the permission blocker; do not force-align a divergent branch as a substitute.

Never force-align an unreviewed divergent branch; doing so could erase unique work.

## 9. Release-state boundary

GitHub CI success establishes source health, not physical APK verification.

Unless the agent can obtain and verify an APK built from the exact final source SHA, keep the source as `SOURCE_ONLY` and preserve the last verified APK entry.

Never manufacture, rename, or relabel an older APK as the current source version.

Before attaching a locally supplied binary, verify the ZIP, package/version/code, pinned certificate, alignment, size and SHA-256 against exact source/build evidence. Text placeholders and build logs are not APKs. Repeat the hash immediately before upload; coordinate with local builders so no actor overwrites the candidate during review.

If a CI artifact is available, it may be downloaded for inspection, but release metadata should change only after the required manifest/signing/install/smoke checks are actually satisfied.

## 10. Final report

Report exact facts:

- final `main` SHA,
- source version/versionCode,
- release state,
- latest verified APK version,
- PR number and merge method,
- checks that passed/failed,
- branches/PRs still divergent,
- device/APK validation still outstanding.

Do not claim local/device verification that the GitHub-connected workflow did not perform.

## Core invariants

- Read live state before acting.
- `main` is canonical.
- Inspect diffs, not agent claims.
- Review every divergent branch before convergence.
- Consumed versions stay consumed.
- Executable source and version bump are atomic.
- Green CI is not the same as VERIFIED APK.
- Backlog notes are not permission for autonomous scope expansion.
- Preserve useful code; reject broken history.
