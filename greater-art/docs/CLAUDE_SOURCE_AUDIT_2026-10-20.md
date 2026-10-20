# Claude source-audit follow-up — 2026-10-20

## Baseline and isolation

This source-only candidate is **1.21.59 / code282**, based on main
`e96f8dc690b4e138d1303d7fbf9cbed36c8342cb`.
No release artifact, signature or historical branch/tag changes.
The latest verified personal sideload is still **1.21.39 / code262**.
Do not merge until required checks pass and the scope is reviewed.

## Implemented source fixes

- PracticeLoop only runs a 50ms ticker when both markers exist and the primary is
  playing. The separate 5s checkpoint write remains unchanged.
- Sleep timer now belongs to the independent PlaybackService, with monotonic
  elapsed-time countdown, end-of-track/repeat/queue-end callbacks and Stack pause.
  Leaving the Activity does not cancel it while the service lives.
- File scanner recognizes .ts only when it resembles MPEG transport packets;
  scoped document scan requires a video MIME for ambiguous .ts. The existing
  discoverable hi-res formats are deliberately preserved for decoder QA.
- Legacy and scoped folder exclusions use case-normalized relative paths; the
  service-side internal local file admission follows the same .ts filter.
- Scoped production rescan now passes excluded folders; concurrent rescan
  requests are queued, rather than silently discarded; unknown durations
  are cached to avoid repeating an expensive metadata retriever attempt.
- External trusted MediaBrowser remains audio-only; the unreachable internal
  condition is removed. Controller connection failure is reported rather
  than silently ignored.
- Backup/restore cancellation propagates, backup output is pre-serialized before
  opening the truncating stream, and the four result messages are localized.
- Overlay root setup uses stable local references instead of nullable assertions.
- Stack uses longest-remaining take for session duration; EOF attempts to
  promote a viable companion, and removing a primary from a two-track Stack
  resumes its surviving primary in ordinary playback mode.
- Immersive tap recognizer reads the latest fullscreen state, theme transitions
  no longer dispose the fullscreen window effect, queue Locate consumes one
  request rather than tracking every subsequent track change, and fullscreen
  transport content descriptions follow current language.

## Tests and manual verification required

- Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --no-daemon`
  and version/security checks. CI output is distinct from physical runtime acceptance.
- A/B loop accuracy at 0.5×, 1× and 2×, enabled/disabled/paused idle CPU wakeups.
- End-of-track timer with Repeat One, final playlist item and active Stack;
  countdown after leaving the Activity; cancellation and multiple presentations.
- TypeScript in Download, genuine TS packetized video, provider-supplied video/mp2t,
  mixed-case folder exclusion and rescans while scanning.
- Backup with cancellation, read failure and interrupted SAF provider write;
  provider atomics are not guaranteed by ContentResolver's `wt` mode.
- Stack shorter-primary/longer-companion EOF, remove primary from two-voice
  playback, start-gate pause, decoder-init failure rollback and actual sync quality.
- Rotate during paused or playing fullscreen, switch themes, hidden/revealed
  controls, queue Locate after filter change; test on API34, API35, API36 and
  a supported large-screen / foldable device.
- Real playback compatibility for `ape`, `dsf`, `dff`, standalone `alac`,
  MPEG-TS variants and device-specific codecs is **not** claimed.

## Not included / needs separate decisions

- The unfinished Stack start-gate external-pause and all-or-nothing allocation
  improvements are not proven safe by source review alone. Leave for focused QA.
- Play history may intentionally record listening sessions on resume; determine
  expected counting semantics before changing it.
- Scoped rule playlists may need separate URI-aware path matching.
- A universal atomic save for arbitrary third-party SAF document providers is
  not available through `openOutputStream("wt")`; a provider-specific temporary
  document and rename workflow is a separate feature.
- No automatic APK deletion, Git history rewrite, signing change, disabling
  Dependabot, or modification of branch protection. PR #174 privacy work is
  blocked and must stay isolated.
- This patch does not purport to fix every untranslated label or prove physical
  orientation-lock compliance for target SDK 37.
