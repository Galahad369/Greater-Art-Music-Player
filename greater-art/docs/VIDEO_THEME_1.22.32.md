# Follow video colors — Android1.22.32/code353

Status: SOURCE_ONLY. Private optimized build and exact-file A55 acceptance pending.
This is not a public release or completion of the separate Stack alignment work.

## Cause and fix

The old theme called `Flow.first` for the first valid video palette, then stopped
collecting. Each Activity/overlay owned its own state keyed by artwork identity.
Re-entering Now Playing could latch another moment. The SDR Ambient renderer was
also excluded from the shared sampler, and the native compact view used artwork
independently. Removing `first` alone would not cover those routes.

Follow video colors now uses one numeric, eight-track, in-memory cache shared by
the Library, expanded player, fullscreen and native compact controls. Screen and
artwork recreation do not discard an existing video palette. New media IDs stay
separate. Existing non-Video themes and the Black OLED default are unchanged.

Live mode uses the existing approximately900ms sampling loop. A900ms smooth blend
publishes at most10theme updates per second, rather than rebuilding MaterialTheme
at every display frame. The interpolation worker stops when settled or when no
presentation needs video theming. There is no new shiny surface or UI holder.

The existing SDR compositor supplies a16×16 RGB sample from its already-decoded
texture, only on request, on the GL thread. It does not reuse the beam-coordinate
map as RGB. Its primary video/beam shaders, frame latch, viewport geometry and
decoder stay intact. Native/HDR and Power saving retain native video. The
original24×24 native sampler is reused. No second decoder or proxy video is used.

Power saving uses one frozen representative average from video samples already
observed for that media. Later scenes and entering another host cannot replace
it. If only artwork exists, the first real video sample can replace that fallback
once. This is not a complete-file average: no whole-video scan is performed just
for decoration. The cache lasts for this app process, not across force-stop or
process death. Theme contrast and true-black base surfaces remain protected.

## Verification

- The same actual Compose theme check fails on retained Android1.22.31 at the
  later-frame assertion, then passes on the1.22.32 diagnostic build. Constructor
  compatibility is retained so a missing API cannot fake the original failure.
- Two A55 Compose theme tests pass: live updates and first-visible-color continuity
  after destroying/recreating the presentation.
-603 JVM tests pass; lint reports0errors,54warnings and6hints. Eight new numeric
  cases cover interpolation, averages, stale samples, separate IDs, cache bounds,
  Power saving, host re-entry and intermediate contrast.
- The first normal-button trial stops before playback because its title selector
  also matched the search field. Its evidence remains intact; this is a harness
  ambiguity, not an app-color pass or failure. The retry selects the actual row.
- The actual player renders the1280×720 color-changing clip with the original
  frame proportions. Its live control accent changes alongside the visible video.
  A raw per-capture smoothness threshold initially fails because screenshot
  intervals are1.38–3.38seconds, not the requested650ms sleep. The final check uses
  measured elapsed time, keeps the color-change requirement and rejects a
  synthetic instantaneous255-channel jump. The corrected actual live check passes.
- A later cold-launch trial exceeds30seconds on the low-memory host, with no
  recorded app crash. The following warm trial passes actual live color but stops
  at the Home/dock readiness gate; it is not a complete Power saving/entry pass.
  Preserve those failures. No unrelated navigation repair or relaxed dock wait is
  included. Power saving and optimized transition acceptance remain to be tested.
- Exact optimized assembly/signing/device results must be added after execution.

An emulator can verify routing, captured colors, geometry and native controls.
It cannot establish physical-phone battery cost, hardware HDR fidelity, or a
universal absence of jank. A tiny readback can still stall a GPU; do not claim it
is free merely because the resulting bitmap is small. Preserve failed evidence.

## Delivery constraints

Use the mandatory optimized Android build gate. Preserve every earlier APK and
the existing signing identities. The original public signer is still a separate
blocked gate; a private maintenance-signed APK is not an original-key update.
No Stack matcher, Nodes, desktop, history rewrite, cleanup deletion, push, merge,
tag or public publication is included.

## Primary references checked

- [Kotlin Flow.first](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/first.html)
- [Android Compose animation guidance](https://developer.android.com/develop/ui/compose/animation/quick-guide)
- [Android TextureView readback](https://developer.android.com/reference/android/view/TextureView)
- [Official Jetcaster dynamic-theming example](https://github.com/android/compose-samples)
