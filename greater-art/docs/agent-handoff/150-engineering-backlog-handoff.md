# Historical backlog bootstrap

Preserved from 501336c. Its baseline below is historical; current decisions are in
../BRANCH_AND_ISSUE_REVIEW_2026-10-10.md and GitHub issue #150.

# Issue #150 — Engineering backlog handoff

This branch/PR exists so future agents have a visible PR entry point instead of relying only on issue search.

## Current baseline

- main: 1.21.16 / code 239
- #143 and #144: completed in PR #151
- no executable change is intended in this bootstrap commit

## Active workspaces

- #145 / PR #152 — Stack Align v7
- #146 / PR #153 — shared-clock Stack mixer prototype
- #147 / PR #154 — physical-device regression matrix
- #148 — SAF folder-scoped storage
- #149 — theme/accessibility

## Security / release items

These remain issue-driven because their completion cannot be represented by an ordinary source PR alone:

- #23 — production signing / repository-admin verification
- #24 — reachable-history rewrite for workstation paths

## Agent rules

- Follow VERSION_RULES.md for executable changes.
- Do not reuse consumed versions.
- Keep Stack fail-safe: abstain rather than confidently align bad evidence.
- Do not weaken security/CI checks.
- Exact-head Android CI + Version Consistency + Public Repository Security must be green before merge.
- Physical verification is still required for touch/visual/audio behavior.

Close #150 only when its linked engineering work is completed or explicitly closed with rationale.
