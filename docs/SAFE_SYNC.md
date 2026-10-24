# Sync Greater Art without an AI agent

## Normal use on Windows

1. Save your edits. In GitHub Desktop, review and commit only the changes you intend to keep. Do not delete old tracked APKs to tidy the folder.
2. Double-click **Sync-GreaterArt.cmd** in the repository root.
3. Keep its window open while GitHub checks run. Success says local main matches origin/main and prints the latest verified APK's full path.

The first run needs Git, Python 3.11+, GitHub CLI, Internet access and `gh auth login` for your GitHub account. No bot token, signing key, paid AI service or model is required. Android build-tools/aapt are also required when validating a newly verified APK; the helper discovers the installed Windows SDK or respects `AAPT`/`ANDROID_HOME`.

This command syncs **already committed work**. It does not guess which files to commit, bump versions, build an APK, certify an app, resolve semantic conflicts, or claim a failing test passed. Code commits must still obey VERSION_RULES.md. Existing required CI checks must pass before integration. If the published artifact is older than current source, the printed path deliberately points to the latest verified artifact, not an unbuilt version.

## What it does

- Fetches origin and checks this is the expected Greater Art repository.
- Hides historical release APKs locally using Git sparse checkout. They remain tracked and recoverable on GitHub. All other project files stay visible. This is not a deletion commit or a history rewrite.
- If main contains **only committed APK deletions**, preserves the complete original commit at `refs/archive/apk-cleanup-<full SHA>` and verifies that ref before returning main to origin/main. Mixed code changes, dirty files, detached HEAD and in-progress operations stop safely. No stashes are dropped.
- Validates source versions and immutable releases before publishing code. Creates a feature branch when necessary; never pushes directly to protected main.
- Creates or reuses a PR, enables normal auto-merge and waits for required checks. A failed check leaves the branch and PR intact. A review requirement is not bypassed.
- After merge, fast-forwards local main and removes its merged Codex feature branch. It never merges unrelated branches, touches the blocked privacy PR or rewrites public history.

You can see recovery refs with `git for-each-ref refs/archive`. Inspect one with `git show <ref>`. These refs are private local recovery records; do not upload them indiscriminately, because archived history may contain old private paths.

## If it stops

| Message | What you do |
| --- | --- |
| Uncommitted/untracked files | Review them in GitHub Desktop, then commit the intended work. Nothing was automatically staged or discarded. |
| Local and remote diverged | Review both sets of commits; this needs an actual compatibility decision, not force-push. |
| Version/immutable-release guard failed | Read the specific errors. Do not modify/delete an old release or reuse an app version. |
| Authentication failure | Run `gh auth login` and authenticate to the expected repository account. |
| Checks failed / PR not merged | Open the printed PR URL and its failed check. Fix the cause in a new correctly versioned commit, then rerun. |
| Custom sparse rules exist | Keep your rules or explicitly disable sparse checkout; the helper will not overwrite custom rules. |
| Timeout/network failure | Work remains committed. Retry after checking connectivity and the printed PR state. |

To show **all** historical release files again: `git sparse-checkout disable`. They will reappear, so do not commit their deletion. Rerun the sync command to return to latest-only view. Sparse checkout changes local visibility only and does not save Git object-history disk space.

## Terminal alternatives

Read-only local diagnosis (no fetch or push):

```powershell
python scripts/sync-greater-art.py
```

Apply the same guarded workflow as the double-click command:

```powershell
python scripts/sync-greater-art.py --apply --latest-apk-only --repair-apk-cleanup
```

Offline safety tests, using disposable temporary Git repositories:

```powershell
python -m unittest discover -s scripts -p test_safe_sync.py -v
```

The final APK path is printed on success **and on safely handled failure**. Do not confuse an existing verified APK with a successful sync of new changes.
