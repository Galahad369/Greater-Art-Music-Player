# Contributing to Greater Art

Greater Art is developed with both human contributors and coding agents. The repository uses strict versioning, branch-review, privacy, and release-verification rules so concurrent work does not corrupt source or release state.

## Start here

Before making changes, read:

1. `greater-art/VERSION_RULES.md` — authoritative source/version policy.
2. `greater-art/HANDOFF.md` — current repository and release state.
3. The workflow matching how you are working:
   - Local checkout/build agent: `greater-art/docs/AGENT_LOCAL_WORKFLOW.md`
   - GitHub-connected agent: `greater-art/docs/AGENT_GITHUB_WORKFLOW.md`

If another document, branch name, old commit message, or chat conflicts with `VERSION_RULES.md`, follow `VERSION_RULES.md`.

## Before coding

- Start from the current `main`.
- Fetch/review open pull requests and divergent branches before duplicating work.
- Inspect actual diffs rather than trusting branch or PR descriptions.
- Keep the task scoped to what was requested. Backlog notes and diagnostics are not automatic authorization for unrelated features.
- Do not include private file paths, listening history, credentials, secrets, or identifying local-machine data.

## Versioning

Any change that affects executable app behavior, build behavior, resources, manifest state, release behavior, or executable tooling must follow the next valid PATCH/versionCode transition defined by `greater-art/VERSION_RULES.md`.

The executable change and its version bump must be in the same source commit.

A version is considered consumed once it appears on a published executable commit, even if that branch or PR is later rejected. Never reuse a consumed version.

Documentation-only changes follow the explicit documentation exception in `VERSION_RULES.md` and do not require an app version bump unless the validator says otherwise.

## Branches

Use a focused branch from current `main`, for example:

```text
fix/1.15.xx-short-description
```

Do not stack new work on another unmerged agent branch unless stacked development is intentional.

Do not rewrite published history to hide a bad version transition. Record the consumed version and advance.

## Patch design

Prefer the smallest coherent fix.

For bug fixes:

- reduce the problem to a precise invariant,
- patch the responsible state/ownership boundary,
- add deterministic tests when practical,
- avoid duplicate state machines or workaround layers,
- keep diagnostics truthful rather than suppressing warnings.

For UI changes, reuse the existing Greater Art design tokens and accessibility/touch-target conventions unless the task specifically changes the design system.

## Validation

Executable changes should pass the repository's required gates before merge. The expected set normally includes:

- version consistency,
- Android unit tests,
- lint,
- debug/verification assemble,
- privacy/history audit,
- dependency review,
- CodeQL/security analysis.

For a local checkout, use Java 21 unless the repository explicitly changes that requirement.

From `greater-art/`, the baseline local Android checks are:

```text
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Also run the repository version validator and applicable security/privacy scripts.

A successful compile is not a verified release.

## Pull requests

Executable changes should normally go through a pull request to `main`.

A PR should state:

- the behavioral change,
- source version and versionCode,
- release state,
- tests/checks performed,
- whether any release artifact is included,
- any device/APK verification still outstanding.

Before merge, review the complete diff, comments, reviews, unresolved threads, and required Actions results.

Prefer rebase merge for a focused linear patch when repository state permits it. Use head-SHA protection when performing automated merges so a moved branch cannot be merged accidentally.

## Source state vs verified APK

Greater Art deliberately separates source completion from release verification.

A new source version remains `SOURCE_ONLY` until an APK built from that exact source SHA has been independently validated. Depending on the release, validation may include:

- versionName/versionCode,
- application ID,
- signing certificate,
- manifest/permissions,
- install or upgrade behavior,
- target device/emulator smoke testing,
- feature-specific reproduction testing.

Never rename or relabel an older APK as a newer version. Never replace verified-release metadata with source-only metadata.

## Security and privacy

Do not commit:

- passwords, tokens, API keys, signing secrets, or keystores,
- personal file paths or account identifiers,
- listening-history data or media-library contents,
- unnecessary logs containing local device/user data.

Greater Art is intended to remain offline/ad-free unless a deliberate reviewed change says otherwise. Network permissions or new remote dependencies require explicit justification and security review.

## Concurrent-agent rule

Multiple agents may act on this repository at the same time.

Before merge:

1. Re-read live `main`.
2. Re-list open PRs and divergent branches.
3. Compare your branch against the latest `main`.
4. Reallocate the source version if another published executable commit consumed the number you planned to use.
5. Preserve unique useful work from other branches before any branch convergence or force-alignment.

Never force-align an unreviewed divergent branch.

## After merge

Confirm the final `main` SHA and post-merge checks. Keep the repository's source version, release state, and verified APK metadata internally consistent.

When reporting completion, state exact facts: final SHA, source version/code, release state, checks passed, and any device/APK verification that remains.
