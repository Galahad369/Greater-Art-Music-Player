# Historical privacy tools — safe integration

Current scope: prepare and test tools. Do not rewrite or publish history.

The existing required security workflow and scanner remain unchanged. The
separate manual historical audit detects workstation paths in all commits
reachable from the refs in its checkout. It prints counts, not matched values.
It is not a full secret, filename, author, tag, fork or cache assessment.

## Run the safe checks

```powershell
python scripts/test-pr24-patterns.py
python scripts/test-historical-workstation-audit.py
python scripts/audit-historical-workstation-paths.py --repository .
```

Use a full checkout for the historical scan. Exit0 means no matched paths.
Exit1 means exposure remains. Exit2 means the scan could not finish. A clean
working tree is not proof of clean history. The October28 pre-integration scan
found19 affected paths across761 reachable commits. Issue24 remains open.

The first test checks Python strings/bytes and Git ERE separately, including
Unicode, spaces, CR and LF. The second creates an owned temporary repository.
It checks clean history, four deleted path variants, non-disclosure of raw
identities and a non-repository error. It replaces the older PowerShell test;
the original remains in the retained PR174 history.

## Disposable rehearsal boundary

The manual workflow defaults confirm_disposable_rehearsal to false. Dispatch
without that option runs tests and read-only detection only. If the option is
explicitly selected, the tool requires an expected main SHA, a bare input
mirror and a new, separate output. It checks input refs and APK blob identity.
It never pushes. The workflow has read-only repository permission and no
artifact upload. No rehearsal was run as part of this integration.

Do not run a rewrite in a working checkout. Do not publish its output. Actual
cleanup still requires owner approval, protected backups, a collaborator
freeze, signature/tag planning, rollback and independent post-cutover checks.
See [the preparation plan](history-remediation-execution.md).

## Review decisions

- Retain the useful PR174 audit, rehearsal and planning intent.
- Repair cross-line Python matching and the PR-only manual-dispatch gate.
- Keep the required main security gate and current action pins intact.
- Reject stale app versions and the setup-java downgrade.
- Preserve all original commits as ancestry, without force-push or retagging.
- Keep genuine historical findings visible. Do not call synthetic tests a
  privacy-remediation or certification pass.

References: [Python regular expressions](https://docs.python.org/3/library/re.html),
[Git grep](https://git-scm.com/docs/git-grep),
[GitHub job conditions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-jobs-with-conditions),
[GitHub sensitive-data removal](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).
