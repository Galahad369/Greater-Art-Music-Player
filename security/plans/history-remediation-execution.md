# Issue #24 — coordinated history rewrite execution checklist

**Status: PREPARATION ONLY. No force-push, tag replacement, protection bypass,
history deletion, or release rewrite has been authorized or performed.**

## Verified starting evidence (2026-10-14)

- The old generated build logs at historical commit `1dc45f77889215bd51fc360ef99dcfeaeed88aba` still contain workstation-home patterns. Those removed files are reachable through `main` history.
- `v1.5.0` is an annotated tag object (`da936359c251cf5433df041ad72afcaefd514c75`) targeting older commit `c1b34f6e8e8c3961e860c6f281372fcd0f6b3068`. It is not enough to change `main`.
- At preflight, remote branches include `main` and `codex/desktop-integration-review`. Re-inventory at the execution window; newer refs may appear.
- Issue #24 and historical PR #51 identify **additional** affected generated/source files. Do not limit sanitization to the two sample logs.
- Neither this document nor the audit should reproduce exposed username/path values. Keep raw evidence and replacement mappings restricted.
- Test Windows profiles on any drive (including spaces), macOS and Linux home paths. These are exposure indicators, not proof of a credential leak.
- Git `grep` and `git-filter-repo --replace-text` scan blob/message contents, not all pathnames. Identity-bearing filenames and pathnames require separate owner-reviewed rename/removal handling; do not claim that text substitution sanitizes them.
- Reassess GitHub pull refs, forks and cached copies at cutover; the disposable mirror alone does not establish their remediation. Residual inaccessible copies remain a documented risk.

## Phase A — authorization, classification and freeze (mandatory)

1. Obtain repository-owner written consent specifically naming history rewrite of `main`, affected branches, and protected `v1.5.0` and any other affected tags. Record who may temporarily amend GitHub protection rules and who will restore them.
2. Agree on a maintenance window; freeze pushes, new PR merges, tag creation and releases. Coordinate with owners of desktop branches, active clones, forks and workflows that embed commit hashes.
3. Classify each finding as path-only metadata, personally identifying content, a live credential, or another secret. If credentials exist, rotate/revoke them **before** rewriting and handle notifications privately.
4. Create at least one encrypted, access-controlled full mirror plus an independently verified backup. Save refs, annotated-tag objects/messages, release attachment provenance, commit/tag signature information, and an old→new reference map. Do **not** upload backup or raw reports to this public repository.

## Phase B — full-ref inventory (read only)

Run on a controlled machine with a complete mirror, not in a live developer checkout.

```bash
git clone --mirror https://github.com/Galahad369/Greater-Art-Music-Player.git greater-art-backup.git
cd greater-art-backup.git
git show-ref > /private/location/original-refs.txt
git fsck --full
```

Also retrieve and inventory reachable pull-request refs where permitted, and list forks/caches separately. The audit uses `git rev-list --all` for refs present in its checkout: **do not assume a normal shallow CI checkout includes external fork or every PR object**. Run the separate read-only `scripts/audit-historical-workstation-paths.py` from the matching source revision against a full mirror. It prints counts only. Keep any separately collected raw evidence private. The existing required public security gate is unchanged by this safe-tools integration.

Check generated logs, build scripts, documentation revisions, binary/archive entries, public commit identities, tag objects and release attachments. Investigate false positives rather than creating blanket allowlists. Do not treat path-only leakage as proof a secret exists.

## Phase C — deterministic rewrite rehearsal (offline, disposable clone)

1. Use a pinned `git-filter-repo` version in a throwaway clone of the encrypted backup. Do not modify the backup itself.
2. Remove generated historical build/lint outputs that are not legitimate source artifacts; replace only actual identity-bearing strings in files that must remain. Make replacements from a **private** rules file, never publicly commit raw source values.
3. Rewrite affected branches and tags as a consistent graph. Compare legitimate source trees and test manifests before and after; retain the actual verified APK bytes and hashes. For annotated or signed tags, explicitly preserve/recreate intended tag annotations and account for signatures invalidated by rewriting.
4. Produce a restricted old→new SHA/tag map. Rehearse CI, media fixtures, version ledger checks and an unchanged strengthened history audit. Capture failures; do not allowlist or weaken the detector.
5. Verify `v1.5.0` resolves only to its intended rewritten target and no original identity-bearing paths remain in the complete rehearsed reachable graph.

A rewrite of `main` alone is insufficient while another branch or protected tag still retains old commits. Git object garbage collection and GitHub caches/forks are separate limitations; never promise historical content is erased from every public copy.

## Phase D — owner-controlled production cutover

**Only after explicit approval of the rehearsal, backups, source diff and ref map:**

1. Freeze collaborators and automated writers again; verify remote refs equal the inventoried snapshot.
2. Temporarily relax exactly the affected protections using an approved admin; record original settings and authorization.
3. Publish the rehearsed rewritten branch and tag refs as a coordinated cutover, not ad hoc separate force-pushes. Fail closed if any unexpected ref has moved.
4. Restore branch and tag protections immediately, and confirm their actual effective status.
5. Re-clone from the remote; verify all reachable refs, annotated-tag targets, content hashes, signed artifacts and release descriptions. Run the **unchanged** stricter public-history scanner, regression fixture and full Android/Windows/version/security checks.
6. Repair branch tracking, collaborator clones and external SHA references. Record the residual limits of forks, caches, PR refs and release mirrors.

## Release / closure criteria

- Strengthened audit passes on the full remote reachable graph **without exception lists**.
- Main and every affected branch/tag resolve only to the reviewed rewritten graph.
- `v1.5.0` protection is restored and its annotation/release integrity disposition is recorded.
- Android verified APK byte hashes and desktop installers are preserved or explicitly re-attested. No release is silently relabeled.
- Version rules, source functionality and all applicable checks pass.
- Owner signs off on residual exposure to forks, caches and existing clones.
- Only then close issue #24.

## Current safe integration rule

The user approved safe tools only. The repaired historical detector is a separate manual audit that **fails on unsanitized history**. The disposable rehearsal requires explicit opt-in and cannot publish. The existing required security gate remains unchanged. Retain this plan as preparation; do not call the repository privacy-clean or close issue24 until the owner-controlled cleanup and independent verification are complete. See [safe integration evidence](safe-history-tools.md).
