VERSION_RULES.md — Greater Art APK (HARDENED)

VERSIONED CHANGE RULE (ABSOLUTE): Any change that affects executable app behavior, build behavior, release behavior, resources, manifest state, or executable tooling requires the next PATCH and versionCode +=1 in the same commit, unless this file explicitly lists an exact series or consumed-branch transition. Documentation-only and release-finalization exceptions are limited to rule 8 below. No silent builds and no reused version numbers.

This file is the authoritative version policy for Greater Art. If any handoff note, README, old commit message, or conversation conflicts with this file, this file wins.

## NON-NEGOTIABLE video geometry: NEVER STRETCH

- **Fullscreen foreground video** and every video surface, including PiP, mini/expanded players, and decorative backgrounds, MUST preserve the original displayed aspect ratio (account for pixel width/height ratio and video rotation). NEVER distort by independently scaling X and Y.
- Fullscreen offers **two and ONLY two** sizing choices: **Fit** (entire frame visible, letterbox/pillarbox if necessary) and **Crop** (fill screen while cutting the edges). Default fullscreen mode is Fit; the toolbar must expose a Crop/Fit toggle in immersive playback, including narrow safe-width layouts.
- Media3 PlayerView: Fit = `AspectRatioFrameLayout.RESIZE_MODE_FIT`; Crop = `AspectRatioFrameLayout.RESIZE_MODE_ZOOM`. **`RESIZE_MODE_FILL` is forbidden**, as are `ContentScale.FillBounds`, `FIT_XY`, matrices or independently computed scaleX/scaleY that deform video. User pinch zoom must scale both axes identically and crop at the viewport bounds.
- **Reapply** the selected aspect-preserving PlayerView resize mode when a surface is created, updated, handed between owners, or returned to mini; a reused view MUST NOT inherit an incompatible render mode. PiP and compact previews default to Fit. Do not replace or reprepare the playing video/decoder solely to change Fit/Crop.
- Background wallpaper Fit/Crop is a separate setting. Its selection must NOT silently alter fullscreen video; fullscreen mode must NOT silently alter wallpaper mode. Historical Stretch/Fill preferences migrate to Crop, never resurrect Stretch.
- Regression tests must cover mapping to Fit/Zoom, reject Fill, expose both choices, and exercise full-screen orientation/cutouts and surface reuse. Android CI passing does not prove pixel-perfect visual geometry: real-device landscape/portrait and mismatched-aspect fixture evidence is required before calling it verified.

## Machine-checkable state


Current source: **1.21.76 (code 299)**
Allowed consumed transition: **1.21.68 (code 291) -> 1.21.70 (code 293)**

An earlier side-branch 1.21.69/code292 introduced the staged-cache policy but
failed the per-commit ledger validator because its tracked entry did not use
the required machine-readable syntax. Do not rewrite that history or merge it.
Reintroduce the reviewed source changes directly atop main under the next
unused 1.21.70/code293 identity; record 1.21.69 as consumed in the ledger.
Allowed consumed transition: **1.21.65 (code 288) -> 1.21.68 (code 291)**

Side branch PR #195 consumed 1.21.66/code289 for interactive Android Nodes
physics, but its initial CI caught a direction-specific spring assertion that
was not physically correct when a pinned node crossed its neighbor.
Side branch PR #196 consumed 1.21.67/code290 for source-level Library
performance improvements while PR #195 was being verified.
This integration commit keeps both histories, fixes that assertion, and carries
both sets of source changes forward into 1.21.68/code291 without reusing their
versions or importing an old-version merge into main.
Allowed consumed transition: **1.21.45 (code 268) -> 1.21.48 (code 271)**

The open draft security PR #174 separately consumes 1.21.46/code269
(commit 7343936) and 1.21.47/code270 (commit f69b7db). This
UI-only change deliberately does not merge the still-blocked privacy branch or
reuse those versions; all prior version identities are preserved.
Allowed consumed transition: **1.21.39 (code 262) -> 1.21.45 (code 268)**

Integration retains the 1.21.42–44 commits as a merge parent. Versions 40/41
were consumed by blocked PR #174 and overlapping local Nodes work. The latter's
unsafe physics edits are not imported; useful focus styling is integrated at 45.
Allowed consumed transition: **1.21.39 (code 262) -> 1.21.42 (code 265)**

PR #174 consumed 1.21.40/code263 (eeb06d9) and 1.21.41/code264 (8b81e82).
That blocked historical-privacy branch is not imported or altered. Claude's
96760a2 patch proposed 1.21.39, already consumed on main; preserve its original
mail patch and credit without reusing that identity.
Allowed consumed transition: **1.21.32 (code 255) -> 1.21.36 (code 259)**

PR #168 separately consumed 1.21.33/code256 (5aae831), 1.21.34/code257
(f82e0a4) and 1.21.35/code258 (886a130). Ambient uses the next identity,
now integrated under 1.21.37 after behavioral conflict review.
Allowed consumed transition: **1.21.32 (code 255) -> 1.21.35 (code 258)**
# The parallel floating-mode feature branch used 1.21.32–1.21.34 for pre-integration
# source-only commits; main independently verified 1.21.32 for URI-backed playback.
# Integration retains main's immutable APK and records the consumed branch lineage.
Allowed consumed transition: **1.21.27 (code 250) -> 1.21.29 (code 252)**

1.21.28/code251 was committed by Claude as eb17af5128fd265bc1c2b2eeba21dc461694e4e7
on the supplied ui/symmetry branch. Its complete mail patch is preserved in
docs/imported-patches/claude-stack-symmetry-1.21.28.patch; no remote branch exists
at integration time. Import its layout changes selectively alongside the pending
audit fixes under 1.21.29/code252 rather than reuse that consumed identity.
Allowed consumed transition: **1.21.24 (code 247) -> 1.21.26 (code 249)**
Allowed consumed transition: **1.21.22 (code 245) -> 1.21.26 (code 249)**
Allowed consumed transition: **1.21.22 (code 245) -> 1.21.25 (code 248)**
Allowed consumed transition: **1.21.22 (code 245) -> 1.21.24 (code 247)**

1.21.23/code246 is consumed by the separate PR #153 PCM prototype (`881f39c`).
Claude's supplied adaptive-sync patch proposed that identity too; integration
used 1.21.24/code247 in its preserved history. Adaptive sync is model-backed, not acoustically verified.
Current release state: **SOURCE_ONLY**
Latest verified APK: **1.21.75 (code 298)**
Allowed series transition: **1.14.15 -> 1.15.1**
Allowed series transition: **1.15.89 -> 1.20.2**
Allowed series transition: **1.20.5 -> 1.21.1**
Allowed consumed transition: **1.20.2 (code 220) -> 1.20.4 (code 222)**
Allowed consumed transition: **1.21.16 (code 239) -> 1.21.18 (code 241)**
Allowed consumed transition: **1.21.18 (code 241) -> 1.21.20 (code 243)**
Allowed consumed transition: **1.21.20 (code 243) -> 1.21.22 (code 245)**
Allowed consumed transition: **1.21.20 (code 243) -> 1.21.23 (code 246)**
Allowed consumed transition: **1.21.22 (code 245) -> 1.21.24 (code 247)**
Allowed consumed transition: **1.15.13 (code 143) -> 1.15.17 (code 147)**
Allowed consumed transition: **1.15.17 (code 147) -> 1.15.19 (code 149)**
Allowed consumed transition: **1.15.19 (code 149) -> 1.15.21 (code 151)**
Allowed consumed transition: **1.15.21 (code 151) -> 1.15.22 (code 152)**
Allowed consumed transition: **1.15.22 (code 152) -> 1.15.23 (code 153)**
Allowed consumed transition: **1.15.23 (code 153) -> 1.15.24 (code 154)**
Allowed consumed transition: **1.15.24 (code 154) -> 1.15.25 (code 155)**
Allowed consumed transition: **1.15.25 (code 155) -> 1.15.26 (code 156)**
Allowed consumed transition: **1.15.26 (code 156) -> 1.15.27 (code 157)**
Allowed consumed transition: **1.15.27 (code 157) -> 1.15.29 (code 159)**
Allowed consumed transition: **1.15.31 (code 161) -> 1.15.34 (code 164)**
Allowed consumed transition: **1.15.34 (code 164) -> 1.15.36 (code 166)**
Allowed consumed transition: **1.15.36 (code 166) -> 1.15.38 (code 168)**
Allowed consumed transition: **1.15.39 (code 169) -> 1.15.41 (code 171)**
Allowed consumed transition: **1.15.39 (code 169) -> 1.15.42 (code 172)**
Allowed consumed transition: **1.15.47 (code 177) -> 1.15.51 (code 181)**
Allowed consumed transition: **1.15.52 (code 182) -> 1.15.54 (code 184)**

## Non-negotiable rules

Desktop is independently versioned under `desktop/VERSION_DESKTOP.md`.
Desktop executable/build files and the exact owned workflow
`.github/workflows/windows-desktop-ci.yml` consume desktop identities only.
Android/shared tooling and shared workflows still consume Android identities.
The one-time establishment of this separation consumes 1.21.39/code262; no Android
runtime behavior or verified APK changes are implied.

1. **Every versioned code commit consumes a new PATCH version.**
   - `versionName`: `X.Y.Z -> X.Y.(Z+1)`
   - `versionCode`: `N -> N+1`
   - The version bump must be in the **same commit** as the code change.
2. **Never reuse a consumed version.** Once a code commit lands with a version, that version is spent even if the build, emulator, signing, or release step later fails.
3. **No silent fixes under the same version.** If a build/test exposes another code bug, the fix is another code commit and therefore the next PATCH/code.
4. **Never skip a PATCH/code inside the active series unless this file explicitly authorizes the exact transition.** Series changes use `Allowed series transition`. A consumed-branch recovery may use `Allowed consumed transition` only when every skipped version/code was already committed on a side branch and remains recorded in the ledger.
5. **Release APKs are immutable.** Never modify, overwrite, rename, or copy an older APK into a new version filename.
5b. **Release APK filename MUST match the exact versionName and versionCode.** The artifact in `greater-art/releases/` must be named `GreaterArt-<versionName>.apk`.
6. **A new APK must be built from the exact source commit carrying that version.** Renaming an existing APK is not a build.
7. **Source and release are two separate states.**
   - `SOURCE_ONLY`: code/version has landed, but no verified APK for that version may exist in `greater-art/releases/`.
   - `VERIFIED`: the exact current-version APK has been built locally, its manifest/version checked, smoke-tested, hashed, and recorded.
8. **Documentation-only or release-finalization commits do not bump the app version** provided they contain no versioned code changes.
9. **Do not overwrite history to repair a version mistake.** Consume the next PATCH and record what happened.
10. **Pure integration merges do not consume another source version.** Only a merge whose complete tree exactly matches a parent qualifies. Underlying commits are still validated, and APK immutability checks still apply. Merges introducing conflict-resolution code must follow the normal source-version rules.

## What counts as versioned code

A commit requires a PATCH/code bump when it changes executable/build/release behavior, including:

- `greater-art/app/src/**` Kotlin/Java/XML/native source
- Gradle/settings/build logic outside the versionName/versionCode lines themselves
- `greater-art/gradle/**` build logic
- executable tooling under `scripts/**`
- CI/release/version workflows under `.github/workflows/**`

Markdown/docs-only changes, release hashes/metadata, and adding a newly verified immutable APK do not themselves require another bump.

## Automated guard

`.github/workflows/version-consistency.yml` runs `scripts/validate-greater-art-version.py`.

## Versions tracked




- 1.14.1 (code 116)
- 1.14.2 (code 117) — shared navigation container
- 1.14.3 (code 118) — crossMargin=9; local build verified
- 1.14.4 (code 119) — version-policy/CI hardening; source only, local build pending
- 1.14.5 (code 120) — inline row actions + queue-fling thumbnail deferral; source only, local build pending
- 1.14.6 (code 121) — stable queue row identity + stationary clipped action underlays; source only; CI compile failed on missing drawscope import
- 1.14.7 (code 122) — drawscope import + corrected SOURCE_ONLY handoff metadata; source only; CI compile failed on nested drawContent receiver
- 1.14.8 (code 123) — explicit ContentDrawScope receiver for clipped stationary action underlays; source only, local build pending
- 1.14.9 (code 124) — compact 48 dp square Add-to-list actions in Now Playing and Library; source only, local build pending
- 1.14.10 (code 125) — suspend independent video wallpaper while expanded Now Playing covers MainActivity; source only, local build pending
- 1.14.11 (code 126) — library delete button square (48dp), second video wallpaper suspended, logs consolidated; source only, local build pending
- 1.14.12 (code 127) — PR review cleanup: restore Delete accessibility label and allow versioned release APK tracking; source only, local build pending
- 1.14.13 (code 128) — deep audit: duplicate-safe queue identity/exact-index playback, unified normal queue startup, expanded-player waveform warmup suppression, retired thumbnail-preload cleanup with active cache pruning, stale backup source removal, and release identity/versionCode hardening; source only, local build pending
- 1.14.14 (code 129) — audit follow-up: propagate WAV warmup cancellation and make Locate current resolve the filtered queue's visible index; source only, local build pending
- 1.14.15 (code 130) — version-guard PR-head fix: validate real pull-request commits instead of GitHub's synthetic merge ref; source only, local build pending
- 1.15.1 (code 131) — user-directed visual-system release: restrained semantic design tokens, typography hierarchy, consistent chrome/spacing/motion, 48 dp action targets, dynamic graph labels, and four-screen UI polish; local build verified
- 1.15.2 (code 132) — version-guard fix: allow new-version APKs detected as copies by checking SHA-256; source only, local build pending
- 1.15.3 (code 133) — CodeQL build-mode fix: use build-mode: auto to fix Java/Kotlin analysis; source only, local build pending
- 1.15.4 (code 134) — CodeQL build-mode fix: use build-mode: autobuild for Java/Kotlin analysis; source only, local build pending
- 1.15.5 (code 135) — Lag fixes: inspector bounds out of snapshot state; queue thumbnails read scroll state lazily; source only, local build pending
- 1.15.6 (code 136) — Stack playback lag fix: primary-player clock, buffer-aware companion gating, reduced hard-seek churn, Stack waveform suppression, and decorative video-background suspension; source only, local build pending
- 1.15.7 (code 137) — Stack synchronization follow-up: freeze companions before primary swaps and always mirror actual primary buffering/resume events; source only, local build pending
- 1.15.8 (code 138) — Stack/UI lag follow-up: isolate Stack rows from 2 Hz position ticks, stabilize StackSlot instances, defer Stack thumbnails during fling, and restore UiInspector unit-test bounds seam; local build verified
- 1.15.9 (code 139) — repository integrity cleanup: remove unsafe commit-amending version helpers, permissive branch-protection recipes, and unverified 1.15.4 release artifact; local build verified
- 1.15.10 (code 140) — version-guard hardening: root executable tooling consumes versions and current-tree release scan rejects unverified/newer or malformed APK artifacts; local build verified
- 1.15.11 (code 141) — force-rewrite CI recovery: version workflow skips only the impossible BASE..HEAD range check when a push's old base object is no longer reachable, while current-state/ledger validation remains mandatory; source only, local build pending
- 1.15.12 (code 142) — ColorTheme.FOREST wired into PlayerWindowExpandedContent; legacy theme handling cleaned up; source only, local build pending
- 1.15.13 (code 143) — full-screen video theme isolation and ColorTheme wiring; source only, local build pending
- 1.15.14 (code 144) — complete ColorTheme/Settings recovery on superseded side branch; all required CI passed; version consumed, not merged
- 1.15.15 (code 145) — rebased complete ColorTheme recovery after concurrent main drift; version consumed on side branch, not merged
- 1.15.16 (code 146) — consumed-transition validator support for the recovery branch; version consumed on side branch, not merged
- 1.15.17 (code 147) — canonical branch convergence: complete reviewed ColorTheme/Settings implementation + exact consumed-transition guard, preserving verified 1.15.10 release metadata; local build verified
- 1.15.18 (code 148) — Grok branch reviewed and rejected; claimed color-theme implementation was only Gradle bump + comment removal; version consumed
- 1.15.19 (code 149) — transport-control alignment and final branch convergence: `GaControl.heroIcon = 32.dp` token; main/video play icons consistent; prev/next use 48 dp `GaControl.touchTarget` with `GaControl.prominentIcon`; source only, local build pending
- 1.15.20 (code 150) — same-side double-tap seek for video/audio with 400 ms pairing, cross-side rejection, and hold-to-2x isolation; local build verified
- 1.15.21 (code 151) — canonical forward convergence: same-side double-tap seek for audio/video plus contributor/agent workflow documentation; release-finalized and verified on main (release commit `ac9a6932`)
- 1.15.22 (code 152) — canonical temporary StackRecommend helper/test integration; release-finalized and verified on main (release commit `07701f24`), then superseded functionally by 1.15.23 removing the unintegrated dead helper
- 1.15.23 (code 153) — canonical forward recovery: remove the unintegrated Stack recommendation helper/test, preserve the reviewed 1.15.20 same-side seek + workflow docs, and restore one coherent main timeline; local build verified
- 1.15.24 (code 154) — canonical video-surface first-frame attribution recovery using analytics event time plus unchanged-presentation/candidate reconcile deduplication; local build verified
- 1.15.25 (code 155) — concurrent-branch convergence advanced the already-reviewed 1.15.24 surface implementation to the next source identity without further surface source/test changes; local build verified
- 1.15.26 (code 156) — **Grok**: LIBRARY_FAMILY_NAV text → icons (Layers / LibraryMusic / Hub); local build verified
- 1.15.27 (code 157) — final branch convergence: carry forward Replit Now Playing lock/top-bar alignment using shared GaControl touch-target tokens; source only, CI pending
- 1.15.28 (code 158) — incomplete Grok list-scroll branch: version bump plus unused `ListScrollBudget` helper only; intended AppBackground/list wiring never landed; version consumed, not merged
- 1.15.29 (code 159) — canonical list-scroll video budget: Library, Now Playing queue, Stack list and Stack picker report fling state; decorative CURRENT_VIDEO/CUSTOM_VIDEO stays attached but pauses playback/crop-position work until scrolling settles; source only, CI pending
- 1.15.30 (code 160) — Stack completion + Nodes linked-graph UI: NOW_PLAYING context/highlight, searchable multi-select, wired offline recommendations using local metadata/history, save Stack to playlist, and denser focused graph chrome; preserves 1.15.29 list-scroll budget; source only, CI pending
- 1.15.31 (code 161) — persistent user-owned Stack keyword: saved Stack keeps pinned tracks plus an optional local keyword that dynamically includes matching song names/paths when reopened; legacy playlist rows remain compatible; source only, CI pending
- Consumed side-branch identity: **1.15.33 (code 163)** — rejected Grok crash-hardening commit `82c9e654`; despite its message claiming a full fix, its executable diff replaced the entire Stack coordinator with the literal `PLACEHOLDER`; consumed, not merged, never reuse
- 1.15.34 (code 164) — Stack mini-window event-churn reduction plus whole-Stack loop control; duplicate compact binding/layout/artwork work is gated, and Stack repeat toggles synchronized session restart instead of independent primary repeat; source only, CI pending
- 1.15.36 (code 166) — version-guard consistency fix: current-state ledger validation now honors exact Allowed consumed transition entries using the same version/code-step helper as commit-range validation; local build verified
- Consumed side-branch identity: **1.15.37 (code 167)** — stale combined convergence PR #88 was published from pre-1.15.36 main while 1.15.36 concurrently became canonical; closed unmerged and must not be reused
- 1.15.38 (code 168) — salvage Stack crash hardening: deduplicate Grok/copilot crash-hardening branches, fix Stack coordinator null-safety, preserve 1.15.34 list-scroll budget and 1.15.36 version-guard fix; local build verified; current verified APK
- Consumed side-branch identity: **1.15.35 (code 165)** — redundant forward-port PR #82; executable/test blobs were byte-for-byte identical to canonical 1.15.34, so it was closed without merge and must not be reused
- Consumed side-branch identity: **1.15.32 (code 162)** — redundant forward-port PR #79; executable/test blobs were identical to merged 1.15.31, so it was closed without merge and must not be reused
- 1.15.39 (code 169) — **Grok**: YouTube-style list-fling video budget — detach wallpaper PlayerView surface (keep ExoPlayer), static base during scroll, 80ms settle; local build verified; current verified APK
- Consumed side-branch identity: **1.15.40 (code 170)** — stale forward-port PR #91 duplicated canonical 1.15.39 behavior from an older base; only a small test seam remained unique, so the PR was closed rather than merging conflicting provenance; consumed, not merged, never reuse
- Consumed side-branch identity: **1.15.41 (code 171)** — Grok wallpaper-decoder-budget branch split executable code, version bump, and VERSION_RULES across separate commits and truncated the canonical ledger; reviewed implementation was selectively recreated at 1.15.42; consumed, not merged, never reuse
- 1.15.42 (code 172) — canonical forward recovery: retain the current Now Playing repeat/random text-fit fix, selectively forward-port the wallpaper decoder budget, cap only duplicate CURRENT_VIDEO wallpaper to 640×360, reduce decorative-video buffers, keep CUSTOM_VIDEO resolution uncapped, and preserve native primary resolution/bitrate/FPS; local build verified; current verified APK
- 1.15.43 (code 173) — Stack-save and playlist-delete reliability: Stack save accepts blank metadata via deterministic local fallback naming, surfaces success through reversible snackbar feedback, Library delete requires confirmation, and delete Undo restores prior active-playlist selection; local build verified; current verified APK
- 1.15.44 (code 174) — initial fullscreen lifecycle/surface handoff hardening; consumed intermediate source. Review found the new fullscreen flag was still evaluated after the registered hidden Mini Window overlay, so finishing the launch handoff could hand ownership back to the hidden overlay.
- 1.15.45 (code 175) — corrected fullscreen owner precedence: explicit fullscreen Activity ownership outranks the hidden registered Mini Window while ordinary overlay precedence remains unchanged; retains first-frame handoff completion and terminal-destruction recovery from 1.15.44; required GitHub CI/security gates passed; source only, device verification pending

- 1.15.46 (code 176) — fullscreen single-source recovery: overlay visibility, launch suppression, and return recovery now use VideoSurfaceOwner as the sole fullscreen-active state; removes the service shadow flag and adds JVM coverage; source only, CI pending

- 1.15.47 (code 177) — Stack defaults to whole-session loop, transport and Stack controls use the loop glyph instead of repeat-mode icons, and Stack cards/toolbar use Library chrome tokens (surface/primary/onSurface) instead of washed hardcoded alphas; verified APK retained as the latest immutable release
- Consumed mainline identity: **1.15.48 (code 178)** — saved Stack-mix / primary-boundary-loop source commit later rolled back during 1.15.47 release finalization; remains in main history and must not be reused
- Consumed mainline identity: **1.15.49 (code 179)** — overlay-dock Activity-dialog interception source commit later rolled back during 1.15.47 release finalization; remains in main history and must not be reused
- Consumed mainline identity: **1.15.50 (code 180)** — Stack primary-seek acknowledgement source commit later partially rolled back during 1.15.47 release finalization; remains in main history and must not be reused
- 1.15.51 (code 181) — forward recovery from the partial 1.15.48–1.15.50 rollback: restore the Stack helper/coordinator contracts already referenced by live service/UI/tests, preserve primary-seek acknowledgement and repeat-persistence behavior, and show every non-primary Stack track as a compact tappable subordinate row under the normal Now Playing primary header; local build verified
- 1.15.52 (code 182) — Stack loop UI fix: loop glyph visibility in Now Playing now correctly reflects session loop state; late-ready Stack alignment race fixed by waiting for secondary player readiness before advancing primary; preserves 1.15.51 Stack rows and contracts; local build verified
- Consumed side-branch identity: **1.15.53 (code 183)** — stale final-convergence recovery was committed from pre-1.15.52 main while PR #106 concurrently became canonical. Its useful Stack loop and late-ready alignment work is already represented by verified 1.15.52; the side-branch identity remains spent and must not be reused.

Previous baseline: 1.13.26 (code 115).

- 1.15.53 (code 183) — initial alignment branch identity in `00b48bd`, discovered concurrently consumed by main bookkeeping PR #109; never released here. Forward recovery at 1.15.54 preserves both histories without reusing the identity.
- 1.15.54 (code 184) — Grok-proposed offline sound alignment: cached 20ms envelopes, confidence-gated ±15s correlation, signed per-voice offsets, delayed starts, primary rebasing, saved-mix compatibility and cancellation/progress UI; exact source `c04d12a`, 171 tests/lint/build, signing/version and emulator checks passed. SHA-256 `434d8ffe8118b2c6fc7faf7a481bfca81f78a232cf0600eb9e8fabfda988cd79`. Real varying-singer/tempo-drift and six/eight-track hardware listening tests pending. PR #105 contained no implementation and was not merged.

- 1.15.55 (code 185) — release guard recognizes exact-parent integration merges while retaining source-commit and immutable APK validation. Exact build source `2f093d3`; 171 app tests, three guard tests, lint/build and emulator saved-Stack/cached-alignment smoke passed. APK SHA-256 `3e1f29142a736315938d4e5f0069bdb0e78da2190cd3da647b51fc618ac24a11`.
- 1.15.56 (code 186) — MVP missing-parts pass: Stack alignment adds cached 12-bin chroma/harmonic fingerprints fused with the existing 20 ms energy-envelope correlation while retaining ambiguity rejection; ColorTheme display names become Luna / Orbit / Sol / Astra / Nova / Space Black without changing persisted enum identities; CURRENT_VIDEO uses the primary MediaController surface through VideoSurfaceOwner instead of a duplicate ExoPlayer, with BACKGROUND handoff retention to reduce black-frame transitions; deterministic eight-track headroom/cap coverage added. Source only; real 6–8 varying-singer hardware listening remains pending and no phase-perfect/time-warp claim is made.

- 1.15.57 (code 187) — Stack blank-page snackbar constraint fix, 100ms averaged harmonic windows, one-reference batch alignment, cancellable correlation, analysis pause/resume and localized results; source only pending verification.

- 1.15.58 (code 188) — verification follow-up: compact undo/save feedback clears Stack's fixed transport controls as well as the Library dock. Exact source fd85f03; 174 tests/lint/build, emulator six-take alignment/persistence, manifest/signing checked. SHA-256 `1f5f08bf13a6996496492dd97cc85b2863f0bfe344c294ca96f2313eb37e96e8`. 1.15.57 identity consumed on repair branch, never released here.
- 1.15.59 (code 189) — Stack sync engine: start gate releases primary and companions together once all are READY; drift corrected by pitch-preserving ±5% rate trim instead of 750ms seek loop; seeks only above 400ms with learned lead; primary stalls regroup all voices. Align by sound adds onset-novelty correlation and sub-frame peak refinement. SOURCE_ONLY: not compiled in v0 sandbox (no JDK); needs tests/lint/build and real-device six-take listening.

- 1.15.60 (code 190) — Codex review of Opus sync: gate requires a READY primary even after timeout/external Play; companion buffering regroups and re-parks every voice at the frozen primary timeline. Source-only pending full local verification. 1.15.59 consumed on v0 branch; not overwritten.

- 1.15.61 (code 191) — exact shared v0 bot identity accepted by public-author audit; no personal addresses or vendor-wide exceptions. App synchronization behavior remains 1.15.60. SOURCE_ONLY pending final artifact verification.

- 1.15.62 (code 192) — emulator end-seek exposed stale Media3 position overwriting the barrier's requested seek anchor. Gates now preserve the frozen/requested master timeline through asynchronous seek acknowledgement. SOURCE_ONLY pending verification.

- 1.15.63 (code 193) — aspect-preserving background policy: legacy saved STRETCH resolves to CROP and the picker offers only Crop/Whole-frame fit. Primary video FIT remains unchanged. Exact app source `aaf1304`; 181 JVM tests, lint and assemble passed; API 36 six-take emulator smoke checks passed; immutable verified APK SHA-256 `718385f181b65c480fc264a1117961a79d3e243de932bba168cd06d30a71f5d2`. State: VERIFIED.
- 1.15.64 (code 194) — CURRENT_VIDEO background surface-lease repair. Source was built/smoke-tested and a local hash was recorded in HANDOFF, but `releases/GreaterArt-1.15.64.apk` is absent from the current Git tree; under rules 5–7 the canonical repository state therefore remains SOURCE_ONLY and 1.15.63 is the latest committed verified APK.
- 1.15.65 (code 195) — audit hardening: skip dot-prefixed hidden media/CUE entries (including Android .trashed/.pending and macOS AppleDouble stubs); share one successful Download scan for 15 seconds across Android Auto/Assistant browse/search callbacks with exclusion-set invalidation; narrow overlay addView recovery from Throwable to RuntimeException; untrack/ignore .lh editor-history snapshots. SOURCE_ONLY pending CI/device verification.

- 1.15.66 (code 196) — Stack stability refinement: shared Library-family header, exclusive Stack transport, session-owned alignment, controlled-seek versus real-stall recovery, shared/atomic caches, live compositor-derived Ambient gradients, theme container coherence and bounded private failure/exit records. Six-take device regression added. SOURCE_ONLY pending build/device verification; preserves reviewed Grok/Opus algorithms and primary source quality.

- 1.15.67 (code 197) — verification repair: pass the existing overlay lambda directly (Kotlin rejects a variable callable reference); bind alignment results to their originating mix and keep thumbnail disk retention bounded after writes. 1.15.66 consumed on this branch and never released. SOURCE_ONLY pending verification.

- 1.15.68 (code 198) — verification follow-up: lint indentation repaired; Stack rows moved from the fixed Now Playing identity header into its flexible searchable list; all Material container/on-background colors follow the chosen palette; DEV no longer covers Sort/Close; real-media cache corruption/reuse and six-voice drift assertions added. 1.15.67 passed 185 JVM tests and six-take device start/seek/resume/loop, but lint prevented release. SOURCE_ONLY pending final verification.

- 1.15.69 (code 199) — device-test correction: rate trim is a convergent recovery, not instantaneous clock equality after a seek. Record post-seek drift and assert uninterrupted six-voice playback plus final-window convergence (<75 ms mean; no persistent 200 ms drift), retaining corruption recovery. 1.15.68 build/lint/187 JVM tests/cache device test passed; instantaneous post-seek assertion failed at 203 ms. SOURCE_ONLY pending final device convergence test; no acoustic synchronization claim.

- 1.15.70 (code 200) — final refinement: remove overriding DEV alignment, prevent stale waveform disk-hit requests refilling a cleared cache, and animate live Ambient gradients in the draw phase instead of recomposing the video/queue every frame. 1.15.69 passed 187 JVM tests, lint/build and both six-take/cache device tests. SOURCE_ONLY pending exact-source verification.

- 1.15.71 (code 201) — remove Stretch from the enum, picker and all renderers; Crop remains default and legacy saved/imported Stretch becomes Crop, with migration regression coverage. Move the cache-generation guard into load rather than clear (compile error caught on 1.15.70; consumed, never released). SOURCE_ONLY pending verification.

- 1.15.72 (code 202) — user-requested Fit Stack background: responsive aspect-preserving tiles lease the existing Stack companion engines, visible only behind foreground Library/Nodes/Stack; no extra audio/player copies or source-quality caps. Hidden tiles disable their video tracks. Hardware video failure preserves the singer as audio with explicit UI/diagnostics. Add eligibility/layout/identity tests. 1.15.71 passed 188 JVM tests, lint/build, signing/privacy checks and both cache/six-take device tests. SOURCE_ONLY pending new feature verification.

- 1.15.73 (code 203) — repair Compose lint finding: tiled background observes its distinct layout flow without reading StateFlow.value directly in composition. 1.15.72 compiled/assembled and passed 191 JVM tests but lint blocked release. SOURCE_ONLY pending verification.

- 1.15.74 (code 204) — fix verified background ownership defect: unified service registration must distinguish Dock from Detached/Expanded, so a foreground CURRENT_VIDEO surface can render instead of being permanently outranked by a hidden Mini. Dock retains artwork while the primary surface serves the background, not a black empty preview; no duplicate video decode. Add ownership precedence regression. 1.15.73 passed 191 JVM tests, lint/build and both cache/six-take device tests. SOURCE_ONLY pending background/tiles/transition verification.

- 1.15.75 (code 205) — Fit stress-test repair: decorative companion video has its own silent video-only lane following the existing audio clock; it cannot gate/reprepare/stop the six singers. Hidden previews release; failures preserve audio with a warning, never source-quality caps. Add device test for stopped preview/stale lease isolation. DEV avoids Settings Back and settings copy documents Fit capacity. VERIFIED: exact source a24b86e; 193 JVM tests, lint/build, three API 36 device tests, version/signature/zip alignment/privacy checks; six actual takes aligned 5/5. Physical-phone listening and multi-video frame pacing remain unverified.

- 1.15.76 (code 206) — long native Fit stress exposed decoder NO_MEMORY during a surface transition. Release optional preview codecs and retry the same full-quality primary once before normal failure handling; a proven READY demoted audio voice clears its stale video-error marker, and healthy running counts cannot exceed their denominator. Record primary failures privately and add a device recovery regression. SOURCE_ONLY pending exact-source verification; no source-quality cap.

- 1.15.77 (code 207) — final recovery review: coroutine cancellation propagates, and an identity token prevents an old decoder-recovery job from pausing/publishing into a replacement Stack. Extend the device recovery test with restart-during-recovery. 1.15.76 consumed, not released. VERIFIED: exact app source `bc1c645`; release APK `GreaterArt-1.15.77.apk`; SHA-256 `2d1d5edb67013a1ecc39daa3f83903ac1a9d127ef0d69a2db414f500d841024f`; HANDOFF records full offline test/lint/assemble, package/version/signature and API 36 smoke verification.

- 1.15.78 (code 208) — remote-branch convergence: forward-port the only unmerged Copilot/Grok idea from empty PR #120. Stack loop-off stays Repeat while loop-on uses RepeatOne in Stack and Now Playing; playback/loop semantics, drift/alignment, source quality and permissions are unchanged. Add a pure JVM assertion for the icon-state contract. SOURCE_ONLY pending CI/device verification.

- 1.15.79 (code 209) — music-first Stack alignment: treat vocals as interference rather than the alignment target. Stereo analysis now prefers an L-R side fingerprint when it contains meaningful programme energy, suppressing center-panned vocals before rhythm/onset and chroma correlation; mono/near-mono files retain the full-mix fallback. Fusion weights favor backing-track onsets over raw loudness, UI says Align music, and deterministic tests cover misleading vocal timing plus side-signal selection. SOURCE_ONLY pending exact-source CI/device/listening verification.

- 1.15.80 (code 210) — Now Playing hold-speed lock: the existing 700 ms hold still gives temporary 2×; while holding, a deliberate 72 dp downward pull commits 2× so it remains after release. Holding again while at 2× arms “Release for 1×” and release returns playback to 1×. The unified one-finger detector preserves side double-tap seeking and fullscreen pinch isolation; locked 2× uses the normal speed persistence path rather than a hidden mode. SOURCE_ONLY pending exact-source CI/device gesture verification.

- 1.15.81 (code 211) — compile repair for the 1.15.80 hold-speed gesture: move the 700 ms timer coroutine outside Compose's restricted AwaitPointerEventScope while keeping pointer events inside that scope. No gesture semantics changed; 1.15.80 was consumed by the failed exact-source build and is not released. SOURCE_ONLY pending CI/device gesture verification.

- 1.15.82 (code 212) — harden true immersive fullscreen: when fullscreen is active, reassert edge-to-edge system-bar hiding after window-focus returns and whenever Android reports status/navigation bars visible, preventing the player from remaining in the half-fullscreen state with a persistent top strip. FullscreenVideoActivity also reapplies immersive bars on create, resume, and focus regain. Android can still reserve the OS edge gesture for a deliberate notification-shade reveal on personal devices, but Greater Art no longer stays stranded with system bars visible afterward. SOURCE_ONLY pending exact-source CI/device verification.

- 1.15.83 (code 213) — scroll/cache audit: Library rows now defer fresh thumbnail disk/frame work while their LazyColumn is actively scrolling, matching the existing Now Playing/Stack fling budget, and PlaybackService widget artwork reuses the process-wide MediaCaches thumbnail repository instead of allocating an isolated cache/decoder budget. No playback resolution, bitrate, Stack sync, permissions, or media-source behavior changes. SOURCE_ONLY pending CI/device fast-fling verification.

- 1.15.84 (code 214) — Stack Align Music v5 fine pass: retain the vocal-resistant 20 ms side/onset/chroma coarse matcher, then refine only a bounded ±30 ms neighborhood using a normalized ~3.2 kHz attack fingerprint from the same offline decode. Up to six separated transient-rich anchors must reach a three-anchor/≈4 ms consensus or the coarse offset remains unchanged. Cache namespace advances to stack-align-v5, fine-cache retention is capped at 24 files, and the stale corruption instrumentation test now targets the active cache. This is millisecond-level initial music alignment, not a nanosecond/phase-lock claim and not variable-tempo warping. SOURCE_ONLY pending exact-head CI/device/listening verification.

- 1.15.85 (code 215) — landscape immersive-window hardening for both Now Playing and FullscreenVideoActivity: fullscreen video now explicitly lays out through supported display-cutout/notch regions (ALWAYS on API 30+, SHORT_EDGES on API 28–29), reasserts edge-to-edge/system-bar hiding when Compose observes an orientation relayout, and the dedicated fullscreen Activity reapplies the same policy after onConfigurationChanged because orientation is handled without Activity recreation. Exiting embedded fullscreen restores the prior cutout mode. Android's deliberate system-edge notification gesture remains OS-owned; this prevents Greater Art from remaining visually inset/half-fullscreen after rotation. SOURCE_ONLY pending exact-head CI and physical landscape/cutout verification.

- 1.15.86 (code 216) — complete horizontal Now Playing coverage omitted by 1.15.85: the expanded WindowManager player now treats a landscape video as immersive even before the dedicated fullscreen Activity is launched. Its overlay stops fitting system-bar/display-cutout insets, keeps no-limits/fullscreen window flags while horizontal video is active, re-syncs system-bar hiding after orientation changes and media-type changes, and returns to inset-safe behavior for portrait/audio. Pure regression coverage distinguishes landscape-video immersion from ordinary expanded audio/portrait. SOURCE_ONLY pending exact-head CI and physical landscape/overlay verification.

- 1.15.87 (code 217) — finish landscape cutout coverage for the WindowManager-hosted Now Playing surface: immersive horizontal video now sets the overlay window's layoutInDisplayCutoutMode to ALWAYS on API 30+ or SHORT_EDGES on API 28–29, matching the Activity fullscreen policy instead of relying only on fit-inset removal. Portrait/audio/docked/detached states restore DEFAULT cutout behavior. Adds pure API-level regression coverage. 1.15.86 remains the consumed intermediate overlay-inset fix. SOURCE_ONLY pending exact-head CI and physical cutout-device verification.

- 1.15.88 (code 218) — lint-safe API-28 cutout access repair after 1.15.87's exact-head Android gate failed NewApi lint on direct layoutInDisplayCutoutMode reads/writes. The window cutout field access is now isolated behind @RequiresApi(P) helpers and every call is guarded by Build.VERSION.SDK_INT >= P; immersive semantics are unchanged. 1.15.87 is consumed by the failed lint build and is not released. SOURCE_ONLY pending exact-head CI and physical landscape/cutout verification.

- 1.15.89 (code 219) — Now Playing ambient continuity: the ambient gradient now retains a restrained artwork/video tint through the bottom of Now Playing instead of fading completely back to the neutral theme base. Lower portrait-video chrome no longer paints an opaque surface over the backdrop: its container is transparent, header/timeline panels use a 74% theme-surface layer, and normal queue rows use 58% surface while selected rows keep the existing translucent primary container. This applies the same ambient treatment across upper and lower Now Playing without sacrificing control/text contrast or changing the global app-background behavior. SOURCE_ONLY pending exact-head CI and visual verification.

- Consumed mainline identity: **1.20.1 (code 215)** — a concurrent version-only bump was committed from the 1.15.84/code214 tree while the landscape work was advancing separately. It was later superseded by the 1.15.85–1.15.89 sequence and must never be reused. Its unverified APK was removed from the current tree during release cleanup; no 1.20.1 release is claimed.

- 1.20.2 (code 220) — begin the 1.20 series with thumbnail/cache hardening: cache sibling-art stamps briefly so memory-hit loads do not probe ~30 artwork candidates every time; bound disk bitmap decodes to three; move periodic/startup disk pruning off the caller path with single-flight pruning; stat each cached file once per prune; let the existing 256 MiB byte budget, rather than a 600-file cliff, govern retention up to 6,000 entries; contain thumbnail-generation OutOfMemoryError by evicting the thumbnail LRU and returning a miss. Cache file-key format remains SHA-256 hex and disk thumbnail format is unchanged. No permission/network/media-source change. SOURCE_ONLY pending exact-head CI and device fast-fling verification.

- Consumed branch identity: **1.20.3 (code 221)** — background-aspect PR #128 was explicitly stopped/superseded before merge and must not be reused.

- 1.20.4 (code 222) — Library dock video ownership fix: while the dock is visibly presented in Library, CURRENT_VIDEO wallpaper yields the single primary Media3 video surface instead of outranking the dock. The dock therefore renders live video; the wallpaper falls back to its existing ambient/static treatment until the dock is no longer visible, after which CURRENT_VIDEO may reacquire the surface. No second decoder, network, permission, media-quality, or timeline change. SOURCE_ONLY pending exact-head CI/device verification.

- 1.20.5 (code 223) — Library dual-video presentation: 1.20.4 correctly returned the primary Media3 surface to the visible dock, but that made CURRENT_VIDEO wallpaper fall back to ambient. When both are requested, the dock now keeps the primary player surface while CURRENT_VIDEO uses a muted video-only secondary renderer synced to the primary media position, play/pause state, playback speed, repeat mode, scale mode and horizontal crop position. The secondary renderer exists only while the dock prevents the wallpaper from using the primary surface; all other CURRENT_VIDEO states retain the single-decoder lease path. No duplicate audio, network, permission or media-quality change. SOURCE_ONLY pending exact-head CI and physical dual-surface verification.

- 1.21.1 (code 224) — Library-family pure-background reveal, forward-ported from the Grok design after its named branch/PR refs contained no implementation commits. Stack / All songs / Nodes share one pull-down reveal state: downward overscroll at the top expands an empty wallpaper-only band up to 240dp, upward drag collapses it, and release/fling snaps at a 0.35 threshold. A thin backup drag handle sits below the content. The app root is transparent while Library is active; the content region below the reveal keeps a local 94% light-theme surface scrim for readability. Existing BackgroundScaleMode remains unchanged: Crop default, Fit optional, never Stretch/RESIZE_MODE_FILL. No playback, network, permission, Stack alignment, or loop change. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.2 (code 225) — compile repair for the Library background reveal after 1.21.1 exact-head Android CI caught an invalid explicit import of ColumnScope.weight. The reveal behavior, 240dp geometry, 0.35 snap threshold, pure wallpaper band, and Crop/Fit-only background policy are unchanged. 1.21.1 remains consumed by the failed compile and is not released. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.3 (code 226) — correct the Library-family wallpaper reveal geometry: the first implementation expanded an upper spacer and remeasured the content region while dragging. The reveal is now a fixed full-screen overlay model: AppBackground remains full-screen and unchanged, while the Stack / All songs / Nodes content surface translates downward and is clipped at the viewport. The exposed upper band is an empty transparent hole with no Library scrim, Surface, placeholder, scaling, or redraw layer. Background scaling rules remain authoritative and unchanged: Crop is the default, Fit is available in Settings, Stretch/FillBounds/RESIZE_MODE_FILL is absent. Adds pure translation-bound regression coverage. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.4 (code 227) — deepen and purify the Stack / All songs / Nodes wallpaper reveal. The maximum pull is now responsive to the usable Library viewport (58%, capped at 72% on constrained windows, with the former 240dp retained as a preferred minimum) instead of stopping at a fixed 240dp. While Library is active AppBackground's own dim layer is forced to zero; the normal configured/default dim and light-theme readability scrim are reproduced only inside the translated Library content layer. The exposed upper band therefore contains only the raw app wallpaper, with no dim, Library title, settings affordance, family navigation, dock chrome, or drag handle; developer diagnostics remain intentionally outside the translated layer. Crop remains the default background scale, Fit remains optional in Settings, and Stretch/FillBounds/RESIZE_MODE_FILL remains absent. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.5 (code 228) — increase the Stack / All songs / Nodes pull-down wallpaper reveal from 58% to 80% of the usable Library viewport, with a 90% safety cap so a strip of content remains available for collapse gestures. The 240dp preferred minimum, zero-dim raw reveal band, translated Library dim/scrim, and Crop/Fit-only scaling contract remain unchanged. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.6 (code 229) — extend the Library-family reveal to 94% of the usable viewport (98.5% safety cap) and make the exposed wallpaper itself an active vertical drag surface, so users can pull upward anywhere on the raw wallpaper to recover the translated Library sheet instead of becoming stranded with only a tiny content edge. While any reveal is active, PlayerWindowVisibility explicitly suppresses the docked mini player; collapse restores it automatically. The exposed area remains raw zero-dim wallpaper with no Library chrome. Crop default, Fit optional, Stretch prohibited. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.7 (code 230) — make wallpaper recovery directional instead of threshold-hostile: an upward drag that starts on the exposed wallpaper now settles the reveal closed on release, even when the sheet was still above the normal 0.35 snap threshold. Downward/non-upward release keeps the standard snap rule. This prevents the near-full 94% reveal from snapping back open and trapping the user after a short recovery gesture. SOURCE_ONLY pending exact-head CI and touch/device verification.

- 1.21.8 (code 231) — thumbnail memory-pressure hardening for fast-scroll stability: retain the existing process-wide memory+disk cache and scroll deferral, but reduce heavy frame/artwork generation to one worker and disk bitmap decode fan-out to two on low-RAM or sub-384 MiB heap devices. The shared RAM LRU now responds to Android trim-memory callbacks (progressive running/UI-hidden trims and full background eviction) while the persistent disk cache remains intact, so memory can be reclaimed without forcing expensive thumbnail regeneration. No playback quality, source format, network, storage permission, or media-file behavior change. SOURCE_ONLY pending exact-head CI and physical fast-fling/OOM verification.

- 1.21.9 (code 232) — restore and smooth pull navigation across Expanded Mini and the Library-family wallpaper reveal. Expanded Mini's previously disconnected pull callbacks now use a larger top-center drag affordance; WindowManager movement is coalesced to one layout update per display frame, clamps against the real viewport instead of MATCH_PARENT, and uses a short-screen adaptive return threshold. Stack / All songs / Nodes can now translate the sheet a full 100% of viewport height on every screen size, leaving the viewport as pure undimmed wallpaper with no dock or residual content; the wallpaper itself remains an invisible recovery surface. Reveal flings follow gesture direction, the open threshold is reduced to 18%, and a larger grab zone travels with the sheet so pull-down and pull-up are easier without contaminating the exposed wallpaper. SOURCE_ONLY pending exact-head CI and portrait/landscape touch verification.

- 1.21.10 (code 233) — sharpen Now Playing while preserving Ambient as the full-screen visual identity. Artwork uses a restrained outlined stage instead of liquid-metal chrome; title hierarchy adds available Artist · Album metadata; portrait video header/timeline and queue rows share one low-shadow translucent surface language with thin outlines; queue spacing/selection is clearer; seek thumbs/tracks lose decorative glow for cleaner geometry; the central play control becomes a stable solid accent while repeat/speed remain secondary. Ambient gradient generation, video surfaces, playback gestures, source quality and transport semantics are unchanged. SOURCE_ONLY pending exact-head CI and physical visual/touch verification.

- 1.21.11 (code 234) — extend the sharp/low-shadow visual language to Library, Stack and Nodes while keeping each screen's function distinct. Library media rows become outlined ambient cards and every SMALL/MEDIUM/LARGE row now uses the immutable MiniWindowMetrics 103×56dp thumbnail footprint; row size changes only typography/padding. Stack track/current/master controls gain consistent restrained surfaces and state outlines without changing mix or alignment semantics. Nodes keeps an open transparent graph, replacing soft active halos/control slabs with crisp rings and thin outlined chrome. Full pure-wallpaper mode now axis-locks gestures: vertical drag recovers the Library while horizontal drag pans a dedicated wallpaper position without paging Stack/All songs/Nodes. The pan is shared by custom image, custom video, current-video primary and mirror rendering; Crop uses real overflow, Fit uses only legal horizontal slack, and Stretch remains impossible. SOURCE_ONLY pending exact-head CI and physical portrait/landscape gesture/visual verification.

- 1.21.12 (code 235) — post-merge review repair: preserve the original Stack | All songs | Nodes wallpaper motion while the Library sheet is closed by synchronizing WallpaperPanState to the pager only at zero reveal, then freeze framing during pull-down and let pure-wallpaper horizontal gestures own it at full reveal. Also close the remaining thumbnail fast-scroll crash path by containing OutOfMemoryError from warm disk-cache Bitmap decode: evict the RAM thumbnail LRU, retry once, and retain the valid disk cache entry if allocation pressure persists. No playback, Stack alignment, media quality, permission, cache format, or wallpaper scale-mode change. SOURCE_ONLY pending exact-head CI and physical gesture/memory-pressure verification.

- 1.21.13 (code 236) — make the Now Playing control-lock position structural instead of offset-based. Video top bars expose a measured 48dp lock slot between flexible actions and Fullscreen/Close; the global lock/unlock button is placed from that slot's real window bounds, so toolbar padding/action-count/theme changes cannot displace it. Audio, which intentionally has no top bar, exposes its own explicit safe-area top-right anchor. Mode/orientation changes reset stale anchor state; any actual video-stage resize (rotation, split-screen, inset/cutout or future overlay geometry change) re-exposes controls long enough to measure the new slot; and the lock remains above the input-blocking layer so Unlock always works. Removes the historical '100dp/two buttons from right' positioning assumption. No playback, media quality, gesture semantics, permission or lock behavior change beyond stable placement. SOURCE_ONLY pending exact-head CI and portrait/landscape/audio/video/expanded-Mini verification.

- 1.21.14 (code 237) — harden accidental drag gestures. Expanded Mini / Now Playing pull-down now requires a 24dp downward activation gate before the WindowManager sheet moves, applies 0.72× resisted follow after activation, raises the normal return-to-Library threshold to an adaptive 30% of viewport bounded to 56–120dp, and permits only a deliberate fast downward gesture (≥56dp and ≥1400dp/s average) to shortcut that distance. Small pulls snap back. Nodes no longer begins moving a touched media node after ordinary touch slop or accumulated finger jitter: drag activation uses net displacement with a minimum 20dp gate (or 1.5× platform touch slop, whichever is larger), while sub-threshold shaky touches remain tap-to-play. Empty-canvas pager swipes and two-finger graph zoom are unchanged. SOURCE_ONLY pending exact-head CI and physical touch verification.

- 1.21.15 (code 238) — Stack Align Music v6 common-view hardening. A file that selects the stereo L-R side signal now also retains a plain-mono feature view; alignment never compares one file's side features against another file's mono features. If only one file selected side, both are correlated as mono. If both selected side, the side result is accepted only when an independent mono-vs-mono result is also confident and agrees within 40 ms; otherwise alignment abstains instead of returning a confident mixed-view lag. The feature cache moves from stack-align-v5 to stack-align-v6, stores the optional mono chroma/fine view, and quantizes the peak-normalized ~3.2 kHz fine signal to unsigned 16-bit while retaining float envelope/chroma data. Legacy v5 cache is removed on first v6 use. Adds Android-free codec corruption/round-trip and common-view gating tests. Also ignores nested .lh editor local-history directories in addition to the existing root rule. Repeated/looped arrangements remain inherently ambiguous and are not claimed solved. SOURCE_ONLY pending exact-head CI and physical listening/alignment verification.

- 1.21.16 (code 239) — close Library follow-ups #143 and #144. The Library background reveal container now always occupies the full Scaffold viewport; Mini Window dock padding is applied only to the translated foreground, so pure-wallpaper horizontal pan and upward recovery can start from the bottom dock region without changing reveal geometry mid-gesture. Library thumbnail rows now reuse a remembered gradient brush and rows created during a fling wake in a short deterministic 36–120 ms stagger after scrolling settles instead of releasing together. ThumbnailRepository persists fingerprint-keyed .missing markers only for successfully probed audio files with no custom, embedded or sibling artwork; transient decode/OOM/custom-cover failures still use the 30-second retry path. Missing markers share the existing cache fingerprint, clear operation and LRU-style disk pruning, so source/cover changes naturally invalidate them. Existing RAM/disk worker limits, warm-disk OOM retry, immutable 103×56dp Library thumbnail size, playback and Stack behavior are unchanged. SOURCE_ONLY pending exact-head CI and physical full-screen gesture/fast-fling verification.

**No silent builds. No version reuse. No version skips. No APK overwrite/rename/copy.**

- Consumed side-branch identity: **1.21.17 (code 240)** — PR #156 (`wip/149-theme-accessibility`, tip `b24c54f`) already commits this identity. This performance stage does not import that unrelated theme work; 1.21.17 must not be reused.
- 1.21.18 (code 241) — Library Performance v2, stages 1–2: bounded shared thumbnail queue with viewport priority, fling cancellation/deferral, immediate filesystem-free RAM hits, two-entry idle disk-only prefetch, playback-aware allocation-byte LRU budget, sampled disk reads, verified-byte embedded/folder-cover deduplication, offline-analysis permits and queue/memory diagnostics. Stack audio/video output and alignment v6 are unchanged. SOURCE_ONLY; build/tests and device checks are recorded separately in HANDOFF.md.
- Consumed side-branch identity: **1.21.19 (code 242)** — PR #156, commit `876cc24`, integrates Void OLED on 1.21.18. Preserved but not imported into the Stack work.
- Consumed side-branch identity: **1.21.20 (code 243)** — Stack v7 commit `49a3cd6`. Main concurrently consumed this numeric identity for fullscreen-lock changes; both histories remain preserved, not rewritten.

- 1.21.19 (code 242) — Keep full-size Greater Art pages awake regardless of audio/video playback state: foreground Library/Settings, expanded system-overlay Now Playing, and dedicated landscape fullscreen Activity. Both docked/detached Mini Window overlays clear KEEP_SCREEN_ON; Android PiP clears MainActivity's flag, restoring it upon exit. Add regression tests for window-mode flag transitions. SOURCE_ONLY; no 1.21.19 APK. **Version-policy recovery:** the immediately preceding implementation commit `51659dc` changed executable code without the mandatory version bump. In accordance with rule 9, history was preserved and this follow-up records and consumes 1.21.19/code242. It does not retroactively make that earlier commit compliant; range-guard failure on the earlier commit is recorded, and current-tree validation must pass.

- 1.21.20 (code 243) — Fullscreen Now Playing chrome and player-lock icon now share a single visibility state: fade the lock with all other immersive video controls (including after the 2.5-second auto-hide); a tap on locked video restores access to Unlock without sending touches to seek/play/zoom. Preserve nonimmersive audio and portrait-video lock visibility, same-side double-tap seeking, and the measured toolbar anchor position. Add visibility-regression assertions. SOURCE_ONLY; no APK was produced.

- 1.21.21 (code 244) — Reconcile PR #156 Void OLED palette from the previously consumed side branch onto the 1.21.20 main source without replacing the 1.21.19 screen-awake or 1.21.20 fullscreen lock fixes. Add true-black flat surfaces, distinct raised control surfaces, forced dark Void appearance, untinted elevation, persisted Settings selection and unit palette regressions. Space Black and other themes remain unchanged. Issue #149's High Contrast, artwork-adaptive accent and launch-window polish remain separate. SOURCE_ONLY; exact-head CI and physical OLED verification required; no APK produced.
## 9 October 2026 — 1.21.18 release-finalization candidate

- Owner confirmed that the 1.21.18 APK was verified. The existing HANDOFF QA record specifies package `com.local.listentomusic`, version 1.21.18/code241, a 27,746,006-byte APK, SHA-256 `bba973d127a9dec1f03b29fca1a146707c9feee82497e9a65ab196870b696b3d`, signing certificate `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`, zip alignment, 249 JVM tests and three API36 emulator regressions. The working-tree QA run was recorded before commit `2edb328a`; exact committed-source equivalence is owner-attested and **not independently proven by that older handoff alone**.
- **Mandatory independent gate before merge:** The unmodified version guard must read the *tracked* `releases/GreaterArt-1.21.18.apk`, recompute its SHA-256, match the hash in HANDOFF, and validate APK package/version metadata via `aapt`. If it fails, do not merge this candidate, relabel the artifact, or modify the guard to conceal provenance defects.
- Older 1.21.x binaries are preserved, not retroactively certified. This finalization only applies to the 1.21.18 candidate; issue #158 retains full history and evidence.
- 1.21.22 (code 245) — integrate Stack instrument-first v7 with current main's screen-awake/fullscreen-lock/Void OLED changes and owner-confirmed 1.21.18 metadata. Parallel 1.21.20 histories are retained; main's committed 1.21.21 is accounted for by the exact consumed transition. SOURCE_ONLY; no release artifact produced.

- 1.21.23 (code 246) — default-off shared-clock PCM prototype for PR #153: bounded native decoder queues, stereo float AudioTrack, sample-frame mapping/windowed-sinc conversion, gain/mute/solo, deterministic transport and exclusive-host/focus/noisy safeguards. Debug instrumentation entry only; unsupported tempo maps/precision/channel formats fall back before output. Production runtime unchanged. SOURCE_ONLY.

- 1.21.24 (code 247) — PR #153 debug-only PCM mixer stereo-hot-path optimization: compute the same 32-tap windowed-sinc weights once per active voice/frame for both channels; retain native rate/precision, positive/negative offsets, gain/mute/solo and coherent headroom; add scalar stereo reference and solo regression tests. SOURCE_ONLY; production route unchanged, real-time cold eight-voice output deadline and physical tests still pending.

- Reconciled PR #153 integration: transition **1.21.22/code245 -> 1.21.24/code247** incorporates the already-consumed intermediate **1.21.23/code246** debug mixer prototype from the preserved side-branch history. PR #154's physical QA worksheet is retained in the integrated tree. This is an exact, justified consumed-branch transition, not a general validation exemption or a new APK.



- Consumed parallel draft identities: **1.21.23 (code 246)** and **1.21.24 (code 247)** belong to PR #153's shared-clock source history, not this SAF branch; do not reuse them.
- 1.21.25 (code 248) — experimental scoped media folder grant and bounded Storage Access Framework tree preview on PR #155. Introduce persistent read-only user-selected tree permission, revocation/reselection handling, bounded ContentResolver child enumeration and local media-type candidate tests; explicitly do NOT change the production filesystem-backed Library, playback, metadata/cache, or broad-permission request yet. SOURCE_ONLY; not a completed SAF migration and no APK produced.
- 1.21.26 (code 249) — integrate reviewed PR #153 default-off PCM kernel and PR #155 bounded SAF preview with Claude adaptive sync; consumed parallel 1.21.24/code247 histories remain parents, 1.21.25/code248 remains preserved. Fix partial-reveal horizontal gestures, scalable borderless Mini/Library artwork, collapsed Settings, clear palette swatches and Ambient background. SOURCE_ONLY; no existing APK changed.
- 1.21.27 (code 250) — distributed backing-track candidate consensus, real local four-family alignment diagnostics and regression tests. SOURCE_ONLY; no acoustic perfection claim or immutable release replacement.
- 1.21.28 (code 251) — Claude Stack layout symmetry side-branch commit eb17af5; preserved supplied mail patch; source only, not Android-verified by its author.
- 1.21.29 (code 252) — UI audit: result-scoped selection, confirmed document grants, exact percentage entry, folder validation, contextual overlay permission, truthful playlist/cache/Stack feedback, shared media formatting, labelled toggle rows and Nodes language wiring. SOURCE_ONLY.
- 1.21.30 (code 253) — Typed file/content identities, lossless legacy/cue migration and scheme/bounds tests. No storage permission cutover. SOURCE_ONLY.
- 1.21.31 (code 254) — Bounded cancellable MediaStore/SAF metadata discovery, stable document identities, grant/revocation handling and Settings inspection. Production Library/Stack remain filesystem-backed pending URI IO parity. SOURCE_ONLY.
- 1.21.32 (code 255) — Stage C core: selected SAF folder becomes Library source; URI-backed metadata/artwork/waveform and Stack decode/access. Download filesystem mode retained; scoped sharing/deletion/sidecars and final permission cutover remain separate gates.
- 1.21.33 (code 256) — Consumed by PR #168 commit 5aae831; preserved integration parent.
- 1.21.34 (code 257) — Consumed by PR #168 commit f82e0a4; preserved integration parent.
- 1.21.35 (code 258) — Consumed by PR #168 commit 886a130; preserved integration parent.
- 1.21.36 (code 259) — Adaptive Dynamic Ambient: combined Classic/Aurora/Bloom/Flow, existing compositor samples, cached-art fallback, bounded scene metrics and persisted localized controls. SOURCE_ONLY; Classic and playback quality unchanged.
- 1.21.37 (code 260) — Reviewed integration of native PiP/three floating modes, queue Locate and Adaptive Ambient, preserving both histories; bounded PiP readiness, localized mode descriptions and independent High Contrast. Emulator-verified sideload; physical/production-signing acceptance remain open.

- 1.21.38 (code 261) — Add a prominently visible one-tap “Turn off Developer Mode” control inside the local diagnostics inspector, available in Library and expanded Now Playing. The action clears inspector interaction state and persists the existing Developer Mode preference; the DEV badge/panel disappear reactively. Add a measured Compose UI regression for one-tap dismissal. SOURCE_ONLY pending CI and device acceptance; verified 1.21.37 APK remains untouched.
- 1.21.39 (code 262) — Owner-approved tooling-policy separation for independent Windows desktop versions and CI. Only the owned desktop workflow is exempt from Android bumps; shared workflows remain versioned, with regression tests. Android runtime and immutable verified 1.21.37 APK unchanged. SOURCE_ONLY.
- 1.21.40 (code 263) — Consumed by separate blocked PR #174 commit eeb06d9; not imported.
- 1.21.41 (code 264) — Consumed by separate blocked PR #174 commit 8b81e82; not imported.
- 1.21.42 (code 265) — Integrate Claude 96760a2 scoped exclusions/sidecar-index/relink foundation with Unicode, strict identity parsing, cue isolation and duplicate-listing regressions. Restore the missing audio Now Playing toolbar independently of artwork, with shared lock anchoring and compact landscape layout. No automatic preference rewrite, sidecar byte reads, permission cutover or shared playback changes. SOURCE_ONLY; verified 1.21.39 APK preserved.
- 1.21.43 (code 266) — Fullscreen opens portrait-first. Add explicit portrait/landscape rotation action with destination-shaped phone icon, arrow and localized accessibility labels; retain the shared player and native video quality. SOURCE_ONLY; verified 1.21.39 APK preserved.
- 1.21.44 (code 267) — Clarify Settings with category navigation, current-value rows and accessible single-choice dialogs; group Library appearance and floating-window size logically, separate playlist actions from titles, use an opaque readable surface and stop hidden decorative backgrounds. Preference defaults, persistence, playback and permissions are unchanged. SOURCE_ONLY; verified 1.21.39 APK preserved.
- 1.21.45 (code 268) — Reviewed branch convergence: retain storage/audio/fullscreen/Settings commits, port Dependabot Gradle 9.8.1 / Compose plugin 2.4.21 and pinned setup-java 6.1.0, selectively import Nodes focus styling without blocking layout or ignoring bounce preferences. SOURCE_ONLY; verified Android 1.21.39 and Desktop 0.1.16 artifacts unchanged.

- 1.21.46 (code 269) — Consumed by still-draft PR #174 integration commit 7343936, not imported into main.
- 1.21.47 (code 270) — Consumed by still-draft PR #174 fixture commit f69b7db, not imported into main.
- 1.21.48 (code 271) — Fullscreen Rotate/Lock alignment: move the overlay Lock transition to the exact measured toolbar slot, standardize 48dp targets and centered 26dp icons, and make both actions use one video chrome visibility transition and 180ms fade. Add real-Activity portrait/landscape measured alignment assertions. SOURCE_ONLY; latest verified APK 1.21.39/code262 unchanged.

- 1.21.49 (code 272) — Fullscreen visual regression fixes: recenter zoom/pan on media and viewport changes; invalidate the elevated Lock anchor for rotation/viewport changes; inset immersive toolbar for Android display cutouts without insetting video; adapt secondary actions to cutout-safe narrow widths without reducing 48dp tap targets; normalize toolbar primary icon sizes to 26dp and match Lock tint to GaVideoOverlay.foreground; add deterministic width tests and portrait/landscape safe-area assertions. SOURCE_ONLY pending real emulator/phone acceptance; immutable verified 1.21.39 APK retained.

- 1.21.50 (code 273) — Complete fullscreen rendering follow-up: protect audio fullscreen and all immersive bottom/seek/feedback controls from display cutouts; bound transformed video pan to the scaled FIT image rather than the full viewport; enable single-finger pan only while zoomed, retain tap-to-reveal and suppress conflicting seek/hold then; enable Media3's documented API34 Compose SurfaceView sync workaround on retained PlayerView; wrap essential 48dp toolbar targets in two/four rows below 224/128dp; add geometry and narrow-layout regression tests and a cross-API visual acceptance checklist. SOURCE_ONLY; previous 1.21.39 APK immutable and real-device verification pending.

- 1.21.51 (code 274) — Fullscreen UX/rendering follow-up: tap centre 30% of video once to toggle chrome hidden/visible, with side-zone double-tap seek unchanged, including zoomed and locked modes; retain 2.5s auto-hide only when playing and no slider, pan, hold or pointer gesture is active, and restart after interaction. Keep existing pointer recognizers stable during pinch instead of changing a modifier mid-gesture. Move 2× feedback below measured top toolbar; hide transport/timeline selectively in height-constrained windows to avoid overlapping wrapped toolbar. Correct FIT/pan/video display aspect ratio for Media3 non-square pixel content. Add centre/autohide/anamorphic JVM regressions and fixture-dependent portrait tap scenario; document PiP first-frame device acceptance boundary. SOURCE_ONLY; latest verified APK 1.21.39 unchanged.

- 1.21.52 (code 275) — Follow-up to the 1.21.51 centre-tap/idle-control patch: correct Compose declaration ordering for the responsive toolbar's lastViewportSize state before derived height constraints. The previous 1.21.51 commit is retained in PR history; no release artifact is claimed. Preserve centre-toggle, idle-aware auto-hide, measured 2× banner, short-height adaptation and anamorphic display ratio. SOURCE_ONLY until CI and device verification.

- 1.21.53 (code 276) — Fullscreen auto-hide refinement: bind the video timeline slider's real pressed/dragged interactions to the idle timer, not only onValueChange seeking, so merely pressing/holding the thumb for >2.5s cannot hide the chrome. Return interaction status to false on slider disposal, preserving the existing 2.5s playing-idle hide when released. JVM eligibility regression expanded. SOURCE_ONLY until checks and hardware acceptance.

- 1.21.54 (code 277) — Fullscreen idle controls: preserve the 1.21.53 middle-tap toggle, gesture-aware timer and seek/zoom/lock protections, but auto-hide after 2.5 seconds of inactivity whether playback is playing or paused. Pause/resume no longer gates the idle countdown; fullscreen exit cancels the timer. Update JVM eligibility tests. SOURCE_ONLY; latest verified APK remains 1.21.39/code262.

- 1.21.55 (code 278) — Fullscreen rendering/gesture hardening: reset pan and seek/hold active flags with try/finally on pointer-input cancellation (media change, orientation, disposal), and release temporary 2× speed when interrupted. Inset fullscreen side-seek feedback around display cutouts. Update fixture-dependent paused auto-hide instrumentation test to match 1.21.54's timer and verify middle taps. SOURCE_ONLY; latest verified APK remains 1.21.39/code262.

- 1.21.56 (code 279) — Repair fullscreen instrumentation hosting: isolate component toolbar tests from the real-Activity empty rule, wait for actual root relayout before screenshots, and compile instrumentation in Android CI. No playback behavior change. SOURCE_ONLY; immutable verified 1.21.39 APK retained. PR #188 only proposed this identity in an empty draft; no executable commit consumed it before this repair.

- 1.21.57 (code 280) — Complete the Grok-proposed diagnostics cleanup from empty draft #188: explicit bounded localized summary, independent complete Copy report, collapsed technical/region details, scroll-bounded panel, retained picker/disable controls and surface warnings in both hosts. No playback-quality or storage-permission changes. SOURCE_ONLY; verified APK unchanged.

- 1.21.58 (code 281) — Review and complete remaining Grok #187 scope: stable current-artwork/existing-sample Material accents gated by Ambient/intensity with contrast/High-contrast/Monochrome/OLED guards; release-only R8 with consumer rules and stable persisted enum members, resource shrinking deferred; release compilation in CI; existing SAF/Nodes behavior retained with solver regressions. SOURCE_ONLY until release acceptance; no verified artifacts replaced.

- 1.21.59 (code 282) — Claude source-audit hardening on clean 1.21.58: service-driven monotonic sleep timer including Repeat One/end-of-queue; suspend idle 50ms A–B polling; reject TypeScript .ts in Download via bounded transport-packet recognition and require video MIME for scoped .ts; case-insensitive relative excluded folders in both scanners; queued rescans and unsuccessful duration-probe caching; external library audio-only dead-branch cleanup; explicit controller failure, cancelled backup/restore propagation with localized outcomes and prebuilt payload; null-safe overlay construction; longest-remaining Stack duration and survivor playback; updated fullscreen immersive captures/theme-effect and explicit Locate behavior; targeted tests and build-indent cleanup. SOURCE_ONLY; physical QA/codec testing required; last verified APK remains 1.21.39/code262.

- 1.21.60 (code 283) — Follow-up to 1.21.59: fix the fullscreen Locate one-shot state declaration so Kotlin/Compose compiles under the project's imported runtime API. Use existing imported `mutableStateOf(0)` instead of unimported `mutableIntStateOf`. No additional playback or UI behavior changes. SOURCE_ONLY, latest verified APK remains 1.21.39/code262.

- 1.21.61 (code 284) — Evidence-driven Claude follow-up: command-level Stack pause during readiness gating, reentrant parked-callback protection, allocation-failure preservation, mapped EOF selection with pre-promotion sleep boundary, service-lifetime timer cleanup and clock tests, grant-checked URI admission and scoped browser/rule matching, permission-revocation cleanup and user-requested connection recovery; pixel-rounded fullscreen toolbar targets and real-device regression coverage. Verification recorded separately; SOURCE_ONLY, immutable verified 1.21.39 remains unchanged.

- 1.21.62 (code 285) — Follow-up to committed1.21.61: preserve Stack across Activity reconnection by preventing ordinary restored-Library queue expansion while Stack owns playback; ignore retired media callbacks that no longer match the actual primary. Real-player regressions cover reconnect/scanning and stale-versus-real replacement. SOURCE_ONLY; unchanged verified APK/signing identity, no Library scrolling optimization mixed into PR191.

- 1.21.63 (code 286) — Enforce Stack primary-queue isolation at the service command/admission boundary, including late controller appends already queued before Stack starts; keep explicit ordinary playback and coordinator companion additions unchanged. Add real late-append regression and restore fixture preferences so user folders/playlists/background do not contaminate correctness tests. SOURCE_ONLY; no verified artifacts replaced.

- 1.21.64 (code 287) — Claude Library-row audit follow-up: omit closed action underlays while preserving opening/closing animation and48dp targets; draw animated row color without composition reads; reuse formatted details, bitmap wrappers and selected-path membership. Add measured Compose action lifecycle/metadata/inspector regressions.8GB target RAM does not override actual heap/pressure limits; no video surface/quality, cache freshness or signing changes. SOURCE_ONLY; verified1.21.39 unchanged.
- 1.21.65 (code 288) — Four-fix integration atop 1.21.64 main: Bluetooth-only opt-in, Black Disc visible dark rim, concise Ads toggle without explanatory joke text, Library play marker and thumbnail-only 4dp visual shift. The original parallel PR #193 already consumed the 1.21.64/code287 label on its unmerged branch; its changes are ported as a new single-main-parent commit under 1.21.65, without importing the duplicate-version commit. Keep the immutable 1.21.39 APK untouched.

- 1.21.66 (code 289): Android Nodes local spring response after deliberate node drag, with bounded two-hop interactions and off-main post-release settling; restore existing persisted force sliders to the Nodes controls. Graph build, playback, pager/swipe ownership, pinch and full-library memory limits preserved. No desktop or personal APK changes.
- 1.21.67 (code 290): reduce Library viewport allocations with distinct integer range snapshots, preserve viewport priorities with single-pass scheduler selection and repeated-viewport suppression, skip duplicate queue-stats emissions, remove graphics-layer wrapper from non-current Library rows and scope playback state to the active row. Preserve existing cancellation, thumbnail generation, RAM, disk, video/wallpaper and PR #192 optimizations.
- 1.21.68 (code 291): coordinate and verify the combined Android Nodes interactive physics plus Library viewport allocation improvements; correct the neighbor-direction assertion identified by PR #195's Android CI. Document 1.21.66/67 side branch consumed identity and explicitly authorize transition from main 1.21.65 to 1.21.68. Final integration remains SOURCE_ONLY pending exact SHA CI.

- 1.21.69 (code 292): consumed by the original side-branch implementation fa74f05a (PR #198), which failed the strict per-commit ledger check. Its exact commit and verification failure remain preserved; the unmergeable history is not imported.
- 1.21.70 (code 293): integrate the staged disk-cache probe and deferred-generation policy cleanly atop main 1.21.68. During scrolling, at most one new disk probe can begin; in-flight disk jobs continue, misses queue generation until idle, and expensive extraction remains cancellable. Add scheduler tests for cache-hit delivery, disk-miss deferral, in-flight preservation and generation retry. SOURCE_ONLY, last verified 1.21.39 sideload unchanged.
- 1.21.71 (code 294): selectively integrate Claude d0fe253 performance audit after PR199: paused Stack ticker with immediate command wake, lazy ambient composition, idle sampler backoff, lyric memoization and indexed M3U references. Preserve staged thumbnails and carry forward the local real-disk regression; defer cache pruning during scrolling. Reject unsafe split resume storage, delayed ViewModel resume polling and stepping waveform thumb; preserve original patch and private local69 recovery history. SOURCE_ONLY; no measured battery/performance claim or replacement of verified artifacts.
- 1.21.72 (code 295): unify all Now Playing top-bar action rendering through one Compose component, including the in-toolbar Lock/Unlock action (only the invisible Unlock hit target is elevated over the input blocker); standardize the 48dp hit target, 32dp canvas, 26dp glyph, tint, accessibility and inspector behavior. Keep measured Lock placement above the input blocker, existing shared 180ms chrome fade, 2.5s idle auto-hide and gestures, and media surfaces unchanged. Extend fullscreen instrumentation toolbar alignment to Rotate/Lock/Exit/Close. SOURCE_ONLY; verified 1.21.39 APK unchanged.
- 1.21.73 (code 296): remove persistent "2× · Locked" video-stage text. Show "2× · Pull down to lock" and "Release for 1×" only during their gestures; after a successful lock, display a 1.2-second confirmation then hide. Clear it on unlock/media change. No change to actual 2× playback, 2× hold/drag gesture, user speed setting, fullscreen/toolbar visibility, or original certified APK. Add pure policy regression tests; SOURCE_ONLY.
- 1.21.74 (code 297): enforce non-stretching fullscreen geometry. Explicit fullscreen Fit/Crop toggle with Fit default, Crop = aspect-preserving Media3 ZOOM (cuts edges), never FILL/stretch; apply the selected resize mode in the PlayerView factory and on every AndroidView update including borrowed views, restore Fit in compact mini when a shared view returns. Keep pinch zoom uniform, video decoder/surface ownership, fullscreen/lock/speed and wallpaper modes independent. Add policy and fullscreen accessibility regressions. SOURCE_ONLY, verified 1.21.39 APK unchanged.
- 1.21.75 (code 298): defer the shell's scroll-state read to AppBackground; gate thumbnail RAM-hit counting by Developer Mode and batch diagnostic publication once per second. Add atomic counter and shell-recomposition regressions. Preserve staged scheduler, swipe/reveal and source playback quality. SOURCE_ONLY; physical fling profiling pending.
- 1.21.76 (code 299): add a guarded user-run sync command with immutable-APK/version preflight, protected-main PR/CI workflow, clean-tree/divergence checks, recovery refs for deletion-only APK cleanup and opt-in latest-APK-only sparse checkout. Add disposable-repository safety tests to CI and a user runbook. No Android runtime change beyond the required version identity; source-only until separately verified.
