# Greater Art — Local Agent Workflow

Use this workflow when the coding agent has a local clone/check-out and can build or test the Android project directly.

## 1. Start from repository truth

Before editing:

1. Check out `main` and fetch/prune remotes.
2. Read `greater-art/VERSION_RULES.md` first. It is authoritative for source version, consumed versions, and release state.
3. Read the top/current-state sections of `greater-art/HANDOFF.md`.
4. Inspect existing branches and pull requests before starting overlapping work.
5. Confirm `greater-art/app/build.gradle.kts` matches the machine-checkable current source in `VERSION_RULES.md`.

Never assume a version number from chat, an old branch name, an APK filename, or a previous build.

## 2. Scope discipline

Work only on the task the user asked to implement.

Backlog notes, diagnostics, ideas, screenshots, and speculative improvements are context, not authorization to silently implement unrelated changes. Preserve useful observations in the handoff or issue notes when appropriate.

Prefer one coherent executable change per version. Avoid opportunistic redesigns while fixing a focused bug.

## 3. Branch and history rules

Create a focused branch from the current `main`, for example:

```text
fix/1.15.xx-short-description
```

Do not branch from another agent's unmerged work unless the user explicitly wants stacked work.

Do not rewrite published history to repair versioning. If a bad source commit consumed a version, treat that version as consumed and move forward.

Before merging, compare the branch against current `main` again in case another agent changed the repository.

## 4. Version contract

For any executable/configuration/build/resource/manifest/tooling change:

- bump PATCH,
- increment `versionCode` by exactly 1 unless `VERSION_RULES.md` explicitly authorizes a consumed-version jump,
- make the bump in the same source commit as the executable change,
- update the machine-checkable current source and consumed-version ledger in `greater-art/VERSION_RULES.md`,
- update the current-source section of `greater-art/HANDOFF.md`.

Documentation-only or release-finalization work follows the exceptions written in `VERSION_RULES.md`.

Never reuse a version that appeared on a rejected or abandoned executable branch.

## 5. Implement minimally

For bug fixes:

1. Reproduce or reduce the failure to a precise invariant.
2. Identify the smallest owner/state boundary responsible for it.
3. Patch that boundary rather than adding global state or duplicate mechanisms.
4. Add a deterministic unit test for the invariant when practical.
5. Keep diagnostics truthful; do not suppress a warning merely to make output look clean.

For UI work, preserve existing Greater Art design tokens and touch-target rules unless the task is specifically a redesign.

## 6. Local validation

Use Java 21 for the Android build unless the repository explicitly changes that requirement.

From `greater-art/`, run the repository's relevant validation set. At minimum for executable changes:

```text
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Also run the repository version validator and any security/privacy checks documented in the workflows or scripts.

If a task changes a specific subsystem, add the narrowest relevant test first, then run the full required gate.

Do not call a source version VERIFIED merely because Gradle compiled.

## 7. APK verification is separate from source completion

Source state and verified release state are intentionally separate.

A source commit remains `SOURCE_ONLY` until an APK built from that exact source SHA is independently checked. Verification should include, as applicable:

- APK versionName/versionCode,
- application ID,
- signing certificate identity,
- expected permissions/manifest,
- install/upgrade behavior,
- smoke test on the target device/emulator,
- feature-specific reproduction test.

Do not rename an old APK to a new version. Do not create release metadata for an APK that was not built and checked from the exact source commit.

## 8. Before merge

Re-read:

- `greater-art/VERSION_RULES.md`,
- `greater-art/HANDOFF.md`,
- the final diff against current `main`.

Confirm there is no accidental downgrade, stale duplicate implementation, generated junk, secret/private data, path leakage, debug-only behavior, or unrelated formatting churn.

Prefer rebase merge for a clean linear patch when the repository state permits it.

## 9. After merge

1. Pull the new `main`.
2. Re-run the repository's post-merge validation if local verification is part of the task.
3. Ensure active work branches are either intentionally retained or converged/removed according to the user's branch policy.
4. Report exact source SHA, source version, release state, tests run, and anything still requiring device/APK verification.

## Core invariants

- `main` is canonical.
- Never silently reuse consumed versions.
- Executable change + version bump are atomic.
- Source-only is not verified-release.
- Existing useful work should be reviewed, not blindly merged or discarded.
- Backlog context is not implementation authorization.
- Diagnostics should describe reality, not hide it.
