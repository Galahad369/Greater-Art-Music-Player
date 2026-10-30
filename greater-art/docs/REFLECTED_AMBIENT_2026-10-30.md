# Reflected Ambient — local verification, October30

Source identity: **1.21.92/code315, SOURCE_ONLY**.

## Approved scope and implementation

The user approved stronger reflected light across Ambient surfaces in Now Playing,
Library, Nodes and Stack. Classic Ambient now uses the cached artwork that its
background route previously discarded. Both modes share the existing palette and
rendering path, with stronger edge glows, two broad light ribbons and a specular
edge behind the readability veil.

These are colour-driven light reflections, not mirrored video copies. Classic
keeps steady geometry. Adaptive retains intensity, scene response and motion
settings. Motion follows existing lifecycle, scrolling, reduced-motion and
power-saving gates. Zero intensity removes the decorative layers. Hidden
fullscreen backdrops do not draw.

No new dependency, decoder, video Surface, bitmap readback, sample frequency,
network access or source-quality constraint is added. Five bounded reflection
draws extend the existing renderer; the complete path has ten draws including
the base and contrast veil. This is not a measured performance improvement.

Official references considered: [Compose brushes](https://developer.android.com/develop/ui/compose/graphics/draw/brush)
and [Android AGSL](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl).
The existing Canvas/gradient path was extended instead of adopting an external
shader library or an API33-only renderer.

## Checks performed

- Java21/Gradle9.8.2:472 JVM tests, zero failures/errors/skips.
- Lint:0 errors,47 warnings,4 hints. Debug app and test APKs assemble.
- API36 Android16 QA emulator,1080x2340 at450dpi:21 focused cases pass across two
  invocations. The initial run passed20/21; the corrected walkthrough passed1/1.
- Component tests cover Classic/Adaptive, light/dark, red/blue cached artwork,
  stable motion-off output, zero intensity, zero viewport and fixed button input.
- Existing tests cover compact Now Playing callbacks/layout, real audio/video
  IME-safe dock continuity, fullscreen toolbar and actual1280x720 video sampling.
  The existing device flow also checks two Stack voices without a controller or
  primary-surface replacement during the background-mode change.
- The real-screen walkthrough retains the same shared controller, media ID/URI,
  video dimensions and active playback through Library, Nodes, Stack and the
  expanded system player in both Ambient modes.
- All14 component screenshots and10 corrected real-screen screenshots were
  opened and visually inspected. Actual Nodes renders its graph over the light
  ribbons. Fixed playback controls remain visible in the system overlay.

Evidence remains in the sibling workspace directory
`output/reflected-ambient-20261030/`: build logs, failed initial test log,
corrected walkthrough log and original PNG captures. Captures with UUIDs from
the failed walkthrough are rejected, not relabeled as passing evidence.

### Test failure and prevention

The first walkthrough waited for wall time while Compose owned a separate
animation clock. Pager and theme transitions stayed behind the requested page,
and the test timed out waiting for the Library dock. Fix only the test:
explicitly settle Compose's animation clock before native screenshots and
visibility checks. Do not weaken the dock/playback/first-frame assertions or
extend their timeout. The corrected test passes in66.441s.

Independent source review also caught a light-theme veil mismatch in Library
and unnecessary drawing under hidden fullscreen content. Both were repaired
before the final production build and emulator checks.

## Internal APK, not a promoted release

Path, relative to the parent App workspace:
`output/reflected-ambient-20261030/GreaterArt-1.21.92-internal-test.apk`

- Bytes:28,156,033
- SHA-256:`86b7c55acafcff7f386682df511d18146a8ae73ac0b526a58424999115ca1717`
- Package:`com.local.listentomusic`; version1.21.92/code315
- Certificate SHA-256:`9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`
- Signature, ZIP CRC and16KiB ZIP alignment pass.

The working tree includes preceding compact-player changes and another task's
uncommitted Android graph changes. The APK therefore is not an isolated committed
Ambient release. Those graph files and unrelated desktop work were preserved.
No commit, push, merge, branch switch or release replacement was performed.
PR211 still requires the user's explicit merge authorization.

The verified1.21.80 APK retains its recorded SHA-256 unchanged. Shared-output
Stack remains disabled; its high-resolution stress gate is not fixed by this
visual change. Physical-phone GPU/frame-time/battery checks, full eight-voice
and acoustic acceptance, and universal crash-free proof remain outside these
results. Real-page Stack captures show the empty page's background, not a loaded
mix. Muted footage produces muted reflections; saturated test artwork produces
stronger colour. No beat-synchronization claim is made.
