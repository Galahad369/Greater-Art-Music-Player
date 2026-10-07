# Issue #145 — Stack Align v7

This branch is the implementation workspace for GitHub issue #145.

## Baseline

Start from main 1.21.16 / code 239. Stack v6 already:
- prevents side-vs-mono signal-domain comparisons,
- falls back to mono/mono when only one file selects side,
- requires side and mono agreement when both select side,
- abstains when those views disagree.

Do not regress those safeguards.

## Work to implement

1. Multi-anchor timing relation
   - estimate multiple separated anchor pairs,
   - fit t_companion = a * t_primary + b,
   - treat b as start offset and a as linear drift,
   - require geographically separated support rather than one local chorus.

2. Repeated-section ambiguity rejection
   - detect comparable whole-bar/chorus alternatives,
   - abstain instead of choosing an arbitrary repeated section.

3. Bounded nonlinear drift
   - investigate piecewise mapping / bounded DTW only after a trustworthy coarse alignment,
   - do not allow unconstrained warping to manufacture a match.

4. Transposition-aware harmonic evidence
   - evaluate chroma rotations before comparing harmonic structure.

5. Fine local timing
   - evaluate GCC-PHAT or similarly robust transient-local refinement.

## Acceptance

- Constant-offset synthetic cases remain at least as reliable as v6.
- Repeated-section synthetic cases reject wrong bar/chorus locks.
- Linear-drift cases recover offset + drift inside explicit tolerances.
- Transposed structurally equivalent tests contribute harmonic evidence.
- Unrelated/weak cases abstain.
- Analysis remains cancellable and bounded.
- Do not claim sample-accurate playback; #146 owns runtime architecture.

This bootstrap commit is documentation-only. Implementation commits on this branch must follow VERSION_RULES.md and consume the next executable version when behavior changes.
