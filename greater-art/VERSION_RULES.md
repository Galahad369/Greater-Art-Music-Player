VERSION_RULES.md — Greater Art APK (HARDENED)

VERSIONED CHANGE RULE (ABSOLUTE): Any change that affects executable app behavior, build behavior, release behavior, resources, manifest state, or executable tooling requires the next PATCH and versionCode +=1 in the same commit, unless this file explicitly lists an exact series or consumed-branch transition. Documentation-only and release-finalization exceptions are limited to rule 8 below. No silent builds and no reused version numbers.

This file is the authoritative version policy for Greater Art. If any handoff note, README, old commit message, or conversation conflicts with this file, this file wins.

## Machine-checkable state


Current source: **1.21.6 (code 229)**
Current release state: **SOURCE_ONLY**
Latest verified APK: **1.15.77 (code 207)**
Allowed series transition: **1.14.15 -> 1.15.1**
Allowed series transition: **1.15.89 -> 1.20.2**
Allowed series transition: **1.20.5 -> 1.21.1**
Allowed consumed transition: **1.20.2 (code 220) -> 1.20.4 (code 222)**
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

**No silent builds. No version reuse. No version skips. No APK overwrite/rename/copy.**
