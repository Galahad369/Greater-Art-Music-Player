# Desktop integration PR review — October 14, 2026

Current review source: desktop **0.1.5/code6**, SOURCE_ONLY. Latest verified installer/portable ZIP remains 0.1.4/code5. No immutable artifact is overwritten. Android runtime and verified APK remain untouched.

Follow-up source: **0.1.6/code7** repairs a real Windows CI failure: the hosted vcpkg checkout lacked the pinned registry commit. The workflow fetches the exact manifest SHA from microsoft/vcpkg and verifies its baseline JSON before building. A fresh isolated local Git repository successfully fetched that exact official SHA and verified the object. The version guard and its six tests pass. Fresh Windows CI remains the acceptance gate; no new installer has been frozen.

No open PRs existed at review start. Protected main requires a PR and status checks; direct/admin bypass is not permitted. The existing local desktop history is being published on the temporary codex/desktop-integration-review branch for normal integration. No new worktree is created.

## Findings repaired

| Finding | Repair | Verification |
| --- | --- | --- |
| P1: full background queue popped an admitted job; alignment/scan could stay busy forever | Explicit nonblocking admission rejection; retain admitted jobs; caller clears its busy state on rejection | Saturate a real one-worker queue, reject overflow, prove both accepted jobs finish |
| P1: caught worker exception provided no failure completion | Optional failure callback; generation-checked UI message resets scan/alignment state; stale completions ignored | Throw in a real worker, observe failure callback, prove next job runs |
| P2: audio AVFrame objects leaked if allocations/filter work threw after frame creation | RAII frame owners on all exits | Source ownership review + real audio regression; OOM injection not performed |
| P2: Windows CI depended on vcpkg PATH discovery only | Check PATH, runner VCPKG_INSTALLATION_ROOT and installed Visual Studio tool path; fail rather than substitute an unknown binary | Local tool path exists; fresh CI is authoritative |

Native code compiles. Core **56/56**, including five queue regressions; real Opus output **9/9**. Video/window and final CI results are recorded as observed, not assumed. Compiler warnings in upstream codec headers are not represented as fixed.

## Explicit boundaries

## Published review evidence

- Integration PR: https://github.com/Galahad369/Greater-Art-Music-Player/pull/172
- Reviewed source commit: `f6601c7d93a46fdf131aebdb1ce2fe0f7fb219af`.
- Local public-repository audit passed: reachable history, current files, sensitive filenames, public author identities and APK containers reported no high-confidence findings. This is not a complete security assessment.
- GitHub version guard and dependency review passed. Android, privacy audit, CodeQL and native Windows CI were still running at this checkpoint. Merge and branch cleanup are not yet complete.

This is not full Android feature parity or sample-perfect real-cover synchronization. Missing features are listed in ANDROID_WINDOWS_PARITY.md. Fresh GitHub Android/security/version/desktop jobs must pass before merging. PR identity, final commit, checks and branch cleanup will be appended after completion. Signing/clean-machine/Narrator/physical acoustic acceptance remain unfinished.
