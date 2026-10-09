# Greater Art — Local Agent Workflow

Use this workflow when the coding agent has a local clone/check-out and can build or test the Android project directly.

## 1. Start from repository truth

Before editing:

1. Inspect `git status --short --branch`, current HEAD and concurrent builders before any checkout. Preserve unrelated work. Fetch/prune remotes; use current main as the integration baseline, but continue an already-authorized active branch instead of restarting.
2. Read `greater-art/VERSION_RULES.md` first. It is authoritative for source version, consumed versions, and release state.
3. Read the top/current-state sections of `greater-art/HANDOFF.md`.
4. Inspect existing branches and pull requests before starting overlapping work.
5. Confirm `greater-art/app/build.gradle.kts` matches the machine-checkable current source in `VERSION_RULES.md`.

Never assume a version number from chat, an old branch name, an APK filename, or a previous build.

## 2. Scope discipline

Work only on the task the user asked to implement.

Backlog notes, diagnostics, ideas, screenshots, and speculative improvements are context, not authorization to silently implement unrelated changes. Preserve useful observations in the handoff or issue notes when appropriate.

Prefer one coherent executable change per version. Avoid opportunistic redesigns while fixing a focused bug.

## 3. Branch and history rules

Create a focused branch from the current `main`, for example:

```text
fix/1.15.xx-short-description
```

Do not branch from another agent's unmerged work unless the user explicitly wants stacked work.

Do not rewrite published history to repair versioning. If a bad source commit consumed a version, treat that version as consumed and move forward.

Before merging, compare the branch against current `main` again in case another agent changed the repository.

## 4. Version contract

For any executable/configuration/build/resource/manifest/tooling change:

- bump PATCH,
- increment `versionCode` by exactly 1 unless `VERSION_RULES.md` explicitly authorizes a consumed-version jump,
- make the bump in the same source commit as the executable change,
- update the machine-checkable current source and consumed-version ledger in `greater-art/VERSION_RULES.md`,
- update the current-source section of `greater-art/HANDOFF.md`.

Documentation-only or release-finalization work follows the exceptions written in `VERSION_RULES.md`.

Never reuse a version that appeared on a rejected or abandoned executable branch.

## 5. Implement minimally

For bug fixes:

1. Reproduce or reduce the failure to a precise invariant.
2. Identify the smallest owner/state boundary responsible for it.
3. Patch that boundary rather than adding global state or duplicate mechanisms.
4. Add a deterministic unit test for the invariant when practical.
5. Keep diagnostics truthful; do not suppress a warning merely to make output look clean.

For UI work, preserve existing Greater Art design tokens and touch-target rules unless the task is specifically a redesign.

## 6. Local validation

Use Java 21 for the Android build unless the repository explicitly changes that requirement.

From `greater-art/`, run the repository's relevant validation set. At minimum for executable changes:

```text
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Also run the repository version validator and any security/privacy checks documented in the workflows or scripts.

If a task changes a specific subsystem, add the narrowest relevant test first, then run the full required gate.

Do not call a source version VERIFIED merely because Gradle compiled.

## 7. APK verification is separate from source completion

Source state and verified release state are intentionally separate.

A source commit remains `SOURCE_ONLY` until an APK built from that exact source SHA is independently checked. Verification should include, as applicable:

- APK versionName/versionCode,
- application ID,
- signing certificate identity,
- expected permissions/manifest,
- install/upgrade behavior,
- smoke test on the target device/emulator,
- feature-specific reproduction test.

Do not rename an old APK to a new version. Do not create release metadata for an APK that was not built and checked from the exact source commit.

## 8. Before merge

Re-read:

- `greater-art/VERSION_RULES.md`,
- `greater-art/HANDOFF.md`,
- the final diff against current `main`.

Confirm there is no accidental downgrade, stale duplicate implementation, generated junk, secret/private data, path leakage, debug-only behavior, or unrelated formatting churn.

Preserve versioned source commits. Use a checked merge commit with an expected-head guard when its tree exactly matches the reviewed branch parent. Do not squash several consumed versions into one unexplained jump. A conflict-resolution implementation requires another PATCH/code under VERSION_RULES.

## 9. After merge

1. Pull the new `main`.
2. Re-run the repository's post-merge validation if local verification is part of the task.
3. Ensure active work branches are either intentionally retained or converged/removed according to the user's branch policy.
4. Report exact source SHA, source version, release state, tests run, and anything still requiring device/APK verification.

## Core invariants

- If a small source push repeatedly times out, inspect pack progress and the
  actual remote SHA before retrying. Git may resend historical APK objects;
  negotiate existing objects rather than deleting binaries, rewriting history,
  or claiming that `Everything up-to-date` after an RPC error means success.

- `main` is canonical.
- Never silently reuse consumed versions.
- Executable change + version bump are atomic.
- Source-only is not verified-release.
- Existing useful work should be reviewed, not blindly merged or discarded.
- Backlog context is not implementation authorization.
- Diagnostics should describe reality, not hide it.

## October 2026 regression checklist — lessons, not promises

- **Artwork scheduling:** gate cache misses centrally, not RAM hits in every row.
  A semaphore bounds active workers, not queued requests. Test bounded backlog,
  visible-row priority, duplicate consumers, final-consumer cancellation, and a
  decoder that unwinds slowly after cancellation. Do not return its permit early.
  Native CancellationSignal support differs by API; never release a running
  MediaMetadataRetriever from another thread. Cancellation is not “missing cover”.
  A cancelled cache clear must release its acquired writer-lock prefix, not all locks
  or none. Keep DEV counter collection inside its own child composition.
- **Build writer isolation:** do not edit executable files while Gradle/lint reads them.
  A review fix made during a gate can invalidate its source snapshot and even produce
  misleading lint quick-fix locations. Finish the edit, then rerun the gate with source
  held stable. Likewise, list-only jank tests must not include wallpaper-reveal gestures.
- **Artwork budget/identity:** account decoded allocation bytes and playback pressure;
  LRU peak is not whole-process/native memory. Hash actual cover bytes before sharing,
  validate asset references against an exact digest pattern, and sample disk decode
  at the thumbnail target. No cache optimization may cap real video/audio quality.
- **Inherited release drift:** preserve mismatched/unverified artifacts and report the
  exact guard failure. Do not “fix” a nine-APK ledger mismatch by promoting filenames
  to VERIFIED or deleting user binaries. Separate local source checks from release
  provenance. Disable broken Git fsmonitor *for the audit invocation* so a failed
  file enumeration cannot masquerade as a clean scan.
- **Stack stages:** accompaniment matching, tempo mapping and shared output clock
  require different tests. HPSS is not vocal removal; L−R is not instrumental
  separation; a shared timer around independent players is not a shared PCM clock.
  Commit/build/verify each stage separately before changing normal playback.
- **PCM transport tests:** Android instrument methods must return Unit, not a log
  integer; numeric assertions must compare the same types. Launch a foreground
  test host for modern audio-focus rules and explicitly silence existing output.
  Close decoder children before leaving a coroutineScope at normal EOF: waiting
  for backpressured producers before cancelling them can deadlock. Generation-gate
  restart callbacks, unwrap unsigned AudioTrack heads, and report steady-state
  statistics before a loop resets the counters. Synthetic clock maths is not a
  physical/acoustic result. Never enable an experimental route in normal builds
  merely because its instrumentation harness works.

- **One binary writer:** never let agents/build shells write the same APK or version concurrently. Send text/logs to separate `.log` paths, never `app-debug.apk`. Require a valid ZIP, manifest, signature, alignment, size and SHA-256; repeat the hash just before copying/uploading. A zero exit code or `BUILT` message alone is insufficient. If another operation corrupts output, preserve evidence and rebuild the exact SHA in isolation, or recover the exact installed APK only when its build/install provenance is known and reverified. Never relabel another version.
- **Windows runtime:** set `JAVA_HOME`, `ANDROID_HOME`, `GRADLE_USER_HOME` and `ANDROID_USER_HOME` explicitly. Validate the Java/Python executable, not a Store alias. ADB's `\\.android`/no-device error can be environment misconfiguration, not missing hardware. Use bounded checks; do not busy-loop unchanging CI.
- **Wallpaper ownership:** service existence is not Detached visibility. Test Docked, Detached, Expanded, fullscreen and app lifecycle separately; hidden Dock must not permanently outrank BACKGROUND. Register before claiming and relinquish before detaching. One primary surface cannot simultaneously show live video in Dock and wallpaper; disclose the artwork fallback rather than adding an unrequested duplicate decoder.
- **Stack/audio isolation:** test actual local takes and surface-visible multi-video load, not only a coordinator with no video surfaces. Decorative video must never join the audio readiness barrier. Pause/seek/loop gates follow the primary; large drift seeks are bounded, small drift converges by pitch-preserving rate trim. Snapshot position equality is not acoustic synchronization proof.
- **Cache safety:** malformed/truncated entries regenerate atomically; cancellation and cache-clear epochs must prevent late writes/refills. Shared ViewModels must not instantiate duplicate caches or analysis jobs. Cache failure cannot discard a valid decoded result.
- **UI evidence:** take screenshots after the transition settles. Reject failed/null-root `uiautomator` dumps rather than using stale XML. Verify flexible lists, fixed controls, palette contrast and Settings Back with DEV enabled. Record software-emulator dropped frames and real-phone testing limitations honestly.
- **Documentation gate:** update root/app READMEs, active HANDOFF state, VERSION_RULES, landing-page APK link and both films together. Label films as illustrative, not device captures; remove outdated preload and simultaneous-surface claims. Keep source-only failures in history and explain the actual cause/prevention in the handoff.
