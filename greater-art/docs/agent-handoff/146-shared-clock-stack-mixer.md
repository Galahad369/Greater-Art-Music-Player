# Issue #146 — Shared-clock Stack mixer prototype

This branch is the implementation workspace for GitHub issue #146.

## Objective

Prototype Stack playback around one output clock:

A -> decoder -> PCM -> delay/resampler \
B -> decoder -> PCM -> delay/resampler  -> float mixer -> ONE AudioTrack
C -> decoder -> PCM -> delay/resampler /

Current independent player paths cannot guarantee sample/frame-level lock.

## Prototype requirements

- Decode each Stack source to PCM without changing the normal single-track player.
- Feed one shared mixer/output clock.
- Support positive and negative per-track delay.
- Preserve mute, solo and gain semantics.
- Handle small drift through bounded resampling/time-stretch rather than repeated hard seeks.
- Define buffering, underrun and recovery policy.
- Bound CPU/memory for the supported Stack track count.
- Preserve source quality where supported.
- Fall back safely for unsupported codecs/sources.
- Add measurable synchronization benchmarks/tests.

## Non-goals for the first prototype

- No requirement to replace production Stack immediately.
- No UI redesign.
- No “nanosecond” accuracy claim.
- Do not merge until there is a clear performance/correctness decision.

This bootstrap commit is documentation-only. Executable prototype commits must take the next version/code under VERSION_RULES.md.
