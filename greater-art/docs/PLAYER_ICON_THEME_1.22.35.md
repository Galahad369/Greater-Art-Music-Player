# Complete normal-player icon coverage — 1.22.35 / code356

Status: SOURCE_ONLY; not built or device-verified. The retained tested private
[1.22.34 APK](PLAYER_ICON_THEME_1.22.34.md) remains unchanged.

## Cause and source correction

The actual optimized34 screenshot verifies the named top/header/transport fix,
but queue overflow still renders neutral white. A retained-image check binds
the exact screenshot, accessibility geometry and same-frame accent; it finds
zero matching pixels, failing the unchanged minimum of eight. This is an
observed rendered-color failure, not a source-name or compilation assertion.

The same Material secondary IconButton content color now covers queue Locate,
Clear/Close search, Add to list, More, Move up/down and Remove. Sleep Timer also
uses the current control accent instead of a neutral inactive outline. Timer
state text, all callbacks, glyphs, disabled state handling, touch targets,
animations, lazy reveal/clip logic and queue identity are unchanged.

Foreground-video overlays intentionally retain their high-contrast light-on-dark
palette. Filled Play/Pause keeps its on-accent symbol. No palette extraction,
cache/cadence, decoder/surface, geometry, quality, HDR, Stack, Nodes or desktop
code changes. No new key or permission. No source history rewrite.

## Verification and blocker

- Version35/code356 is the next identity after committed/tested34. No reuse.
- The extended native test compiles. Its retained34 baseline run stops BEFORE
  device boot because the disk preflight fails. This is not a native tint pass.
- The separate retained exact-APK screenshot check catches the queue mismatch.
- The35production color changes still require compile, relevant tests/lint,
  six focused tint/toolbar/live-theme checks and optimized exact-file smoke.
- The existing release preflight reports **BLOCKED: less than8GiB free**.
  C: dropped to about6.46GiB. Do not lower that gate or delete old artifacts,
  user work or failed evidence. No35player APK was built and no emulator was started
  for that blocked baseline attempt.
- The two broader320dp fixtures from34remain failed and unchanged. Their
  transport-tag/playRequested mismatches are documented in the34receipt; do not
  claim a fully green general regression suite or silently weaken those checks.
- Original-public signing remains unavailable. The existing maintenance signer
  stays unchanged and is not a substitute for the original-public identity.

## Resume

Free about4GiB on C: to allow build/capture headroom. Use the same checkout and
`App/output/icon-theme-20261115/` evidence folder; keep all earlier receipts.
Run the35checks and native rendered queue-color regression, then a new optimized
gate output folder and exact-file normal-entry smoke. Keep the current355APK/data
intact until an authorized356update; no wipe, downgrade or key replacement.
Update the final evidence without calling34the35fix. No push, merge, publication,
branch/worktree or cleanup is included.
