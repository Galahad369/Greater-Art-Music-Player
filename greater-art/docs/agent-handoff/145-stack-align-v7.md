# Issue #145 — Stack Align v7

Implementation: source 1.21.20 / code 243, existing PR #152. This is no longer
a documentation-only bootstrap. Production matching delegates to StackInstrumentAlign;
the original v6 decoder remains an internal device-regression baseline only.

## Architecture and measured limits

- 16 kHz analysis, 1024-point Hann STFT, 20 ms hops. Five-frame temporal / seven-bin
  frequency median masks weight percussion, bass flux and harmonic progression.
  Pitch-class rotations are bounded to 12; instrumental rhythm dominates scoring.
- Three to six independent six-second regions across the clipped primary. Each companion
  window is at most 36 seconds, ±15-second search. Every hop is inspected; weak
  percussion/bass candidates skip harmonic work. Competing peaks >500 ms apart with
  insufficient margin abstain. This does not distinguish identical repeated music
  merely by knowing the title or singer.
- At least three distributed anchors, consistent rotation, ≥40% timeline spread,
  ≤25 ms maximum fit residual, scale 0.985–1.015 and offset ±30 seconds. Piecewise/DTW
  is deliberately not enabled: nonlinear inconsistency abstains rather than warping
  unrelated performances. Conservative abstention may reject valid arrangements.
- Optional original-rate, 400 ms windows refine attacks over ±12 ms. Every native-sample
  lag is examined with bounded comparison points; full-resolution local scoring then
  retains fractional microsecond timing. Three corrections must agree within 4 ms.
  Analysis resampling is not audible playback resampling or a quality downgrade.
- v6 common-domain safeguards retained: mono/mono always; side/side only with independent
  mono agreement within 40 ms for both offset and end-of-recording scale difference.
- Generated cache namespace v7 is separate from v6, atomic, exact-length/finite validated,
  limited to 48 entries; source/clip/window fingerprint. Shared offline permit bounds
  active decode, windows cap allocations, cancellation unwinds codec/extractor.
- Saved Stack schema reads legacy 4/5 fields and mapped 7-field rows. Manual nudge
  preserves scale; Reset clears it. Promotion rejects out-of-range rebases before
  changing any player. Production remains independent ExoPlayers, not one PCM clock.

## Local verification, October 9

- Java 21 offline full JVM/build/lint gate: 265 tests, zero failures; lint zero errors,
  28 warnings, two hints; app and instrumentation APKs compile. Public-history/source
  security audit passes. No new permissions/dependencies and no source quality caps.
- Feature-domain corpus: 9/9 accepted off-grid offsets, max error 0 ms; 0/20 unrelated
  false matches (20/20 abstentions). These are deterministic synthetic cases, not a
  population accuracy estimate or actual singer recordings.
- Linear-drift feature fixture: scale error 0.00000793, intercept error 3.044 ms,
  maximum anchor residual 4.762 ms over 180 seconds.
- Synthesized shared percussion/bass with different vocal frequencies, phrasing and
  gain: 700 ms offset, error 0 ms, score 0.9174. Native 48 kHz timing fixture agrees
  within one sample. Silence, repeated loops, inconsistent anchors, insufficient
  coverage, cancellation, cache corruption and backward persistence are covered.
- API 36 real-take suite: cache corruption recovery plus two decoder/visual-lane
  isolation tests passed. The unchanged independent-player seek test failed its
  convergence assertion: final samples [143,180,80,80,136,60,68,25] ms (mean 96.5 ms).
  It was not weakened or labelled passed. This is a production-runtime limitation
  motivating #146, not proof that alignment analysis causes drift.
- First-pair comparison: v6 accepted -27 ms; four-window v7 abstained because an
  intro produced a conflicting +160 ms/pitch candidate and there were insufficient
  distributed mono anchors. Six-window support and evidence-gated pitch consensus
  were added; all JVM tests/lint/build pass, but the emulator process disappeared
  during that rerun. The six-window real-take rerun remains unverified.
- No physical
  acoustic ground truth or universal phase lock is claimed. v6 comparison logs must
  not be interpreted as accuracy measurements without manually labelled ground truth.
- Version guard is blocked by exactly ten inherited newer unverified APKs (#158).
  Nothing was deleted, relabelled, promoted or bypassed. Current source stays SOURCE_ONLY.

## Baseline

Start from main 1.21.16 / code 239. Stack v6 already:
- prevents side-vs-mono signal-domain comparisons,
- falls back to mono/mono when only one file selects side,
- requires side and mono agreement when both select side,
- abstains when those views disagree.

Do not regress those safeguards.

## Original design checklist (implemented above except nonlinear mapping)

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

The original bootstrap was documentation-only. This source stage consumes 1.21.20;
1.21.17 and 1.21.19 remain recorded as consumed side-branch identities.
