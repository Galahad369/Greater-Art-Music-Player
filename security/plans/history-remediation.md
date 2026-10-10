# Historical workstation-path exposure — controlled remediation plan

**Not an approved rewrite. No force-push, tag change, deletion, or audit-bypass is authorized by this plan.** Issue #24 records multiple reachable revisions containing workstation-home paths; current-tree clean scans do not prove history clean. Branch and tag protections may reject rewriting. Existing clones/forks/cache retain old objects.

## Before any history rewrite

1. Appoint repo owner and admin/release custodians; obtain explicit written approval for coordinated history rewriting, including scope, who may change protections and rollback.
2. Run the strongest existing history scanner over **all reachable refs** in an isolated mirrored checkout, including tags and pull-request ancestry where possible; inventory files, commits, affected tags and **classify** path-only metadata vs credentials/PII.
3. If a real secret was exposed, rotate/revoke it **before** history surgery; don't rely on removing Git objects. Assess notification obligations privately.
4. Create encrypted, access-limited repository mirror/backups and tag/reference maps; obtain collaborator/fork coordination, maintenance window and freeze new writes.
5. Prepare a deterministic `git-filter-repo` rewrite plan affecting only offending data while retaining legitimate source changes and release artifacts. Rehearse on a disposable clone.
6. Treat signed/verified APKs, signatures, source-to-artifact attestations, GitHub releases and protected tags as separate immutability commitments. Changing commits/tags can invalidate old attestations; require a recorded old→new evidence map and explicit disposition.

## Execution gate — admin-controlled, future only

- Temporarily adjust protected refs **only after owner approval**, apply rehearsed ref updates as one coordinated operation, and restore branch/tag protections promptly.
- Re-scan every rewritten reachable branch and tag with unchanged audit rules; diff source trees/test manifests, preserve expected release artifacts, run full Android CI, version consistency and security audits.
- Reconcile local clones, fork visibility, Actions cache and publicly mirrored commits; disclose residual unverifiable exposures rather than promising complete removal.
- Record before/after affected SHA mapping, run IDs, backups, access review and owner acceptance in a restricted evidence store.

## No-go conditions

No owner/admin authorization; unclear signing or protected tag impact; legitimate data loss in rehearsal; scanner failure; no key rotation for confirmed compromised credentials; no backup or coordination; unresolved release provenance. Never weaken `audit-public-repo.ps1` just to make CI green.

Tracker: [#24](https://github.com/Galahad369/Greater-Art-Music-Player/issues/24).
