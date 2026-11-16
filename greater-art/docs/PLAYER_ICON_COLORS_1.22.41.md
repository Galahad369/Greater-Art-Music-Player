# Player icon colors — 1.22.41 / code 362

**Current test-only follow-up: 1.22.42 / code 363.** All production color fixes
below remain byte-identical. The 41 optimized APK is retained, not overwritten.
Its A55 diagnostic run passed six tests but could not draw the offscreen native
ripple in two tests. The follow-up attaches the real compact view to a window,
captures actual pressed pixels and requires the same check to reject an injected
old mint ripple. It does not weaken the icon-tint or disabled-button assertions.
The original 364-byte preferences were recovered/restored byte-for-byte after
the initial adapter's restoration assertion failed. The A55's later `pkg=null`
record was repaired with the same signer and retained data, never an uninstall.
Sync guest writes before stopping the emulator. Keep all failed receipts.

## Scope and cause

The owner approved all four findings from the read-only 1.22.40 icon review.
Fullscreen Previous/Next explicitly forced the enabled light tint. The center
Play button used a light glyph over a translucent white plate. Native compact
players applied adaptive artwork accents only to Follow Video and never updated
the initially mint press ripple.

Use Material's inherited disabled content color, darker **local button plates**,
the existing artwork-accent policy, and `RippleDrawable.setColor`. No full-video
scrim, video filter, new decoder, resolution/speed change, new holder or geometry
change. Normal toolbar, Lock selection, pull handle and all callbacks remain.

## Acceptance

- Fullscreen transport glyph contrast is at least 3:1 over black and white video.
- Disabled Previous/Next are visibly dimmer and do not invoke actions.
- Button bounds are identical before/after state changes; all production sizes,
  spacing and video/surface code remain unchanged.
- Native compact colors match expanded artwork-adaptive Ambient in dark/light,
  fixed/Video/High Contrast themes, and return to base when disabled.
- Native press ripple follows the current accent, retaining its existing alpha.
- Exercise normal-entry real-video playback and fullscreen return on the A55
  with the exact optimized APK. Diagnostic tests alone do not qualify that file.

## Evidence and limits

Task evidence: `App/output/icon-colors-20261116/`.
The new contrast test catches the retained original failure before the repair.
Preflight passes with the existing JDK 21/API 37 toolchain. All 620 JVM tests pass;
lint has zero errors/54 warnings; debug and instrumentation APKs compile. The
first test compile correctly rejected a nonexistent native ripple color getter;
the final check renders the pressed drawable instead of inspecting private state.
Optimized assembly, signer/exact-file checks and A55 acceptance remain separate.
The existing private maintenance signer is preserved; the missing original public
signer remains a separate release gate. No push, merge or publication is authorized.

## Final executed acceptance — 1.22.42

- 620 JVM tests pass. Lint: zero errors, 54 existing warnings. Debug/test assembly passes.
- Nine A55 color tests PASS in 32.056s: normal toolbar/current-media/transport
  colors and callbacks, native palette/ripple parity over two artworks in dark/light,
  intensity/background changes, vector geometry, fullscreen disabled glyphs/clicks,
  and existing adaptive-theme checks. The injected old mint ripple must fail the
  same on-screen pixel check before the real accent passes.
- Exact optimized `assembleRelease` passes in 3m 9s. Version/package, pinned
  maintenance certificate, non-debug/non-test flags, ZIP CRC and 16 KiB alignment pass.
- Exact installed optimized bytes pass normal-entry Library/Now Playing playback,
  video frame advancement, pause/resume, fullscreen, actual landscape rotation and
  return to Expanded. The adapter uses the normal side-tap reveal instead of waiting
  for an auto-hidden Rotate button. Screenshots were visually inspected.
- Original preferences, app ID/data inode and AVD profile are preserved. Temporary
  app-op changes are restored; the guest filesystem is synced and the owned A55 stops.

Exact APK: `App/output/icon-colors-20261116/optimized42-maintenance/GreaterArt-1.22.42.apk`.
Size: 5,604,312 bytes. SHA-256:
`f8fcea1f1a6b9b525b678a3d899d6eb3fa02849dbaa21a41ac3c47a056954d13`.
Source commit: `d3fa3a40955e4cf023d96f3d33d10d5cc0df07ff`.
App-input SHA-256: `78190301dc7096cf3d78d9a1ec0b8c0ee0298a1228153a4dbe0cc8e20a81453b`.
Maintenance certificate: `0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
Evidence: `a55-42-03/instrumentation.txt`, `a55-42-04/report.json`, captures and
`optimized42-maintenance/artifact-verification.json` under the task folder.
This is a tested private candidate, not an original-public-signer release or
physical-phone/HDR certification. The public signing gate still rejects the
unavailable original identity. Existing verified releases remain unchanged.

## Branch cleanup

One local branch, main, and one worktree. The deleted merged toolbar remote-tracking
ref was confirmed ancestral to main and pruned. Remote #242 remains open with a
unique Gradle 9.9.1 update and a failed version guard; do not delete that work or
mix an unreviewed toolchain change into this tested APK. The existing Windows Trust
stash and the owner's separate Stack files are retained. Nothing was pushed/merged.

## Prevention

Do not hard-code enabled glyph tints inside a button with disabled color roles.
Do not put a white-on-translucent-white transport over arbitrary video. Native
and Compose hosts must use the same adaptive policy, including intensity and
fixed-theme exclusions; update the press ripple with the glyph, not only at view
creation. Keep local button contrast separate from foreground video rendering.

References: [Material IconButton colors](https://developer.android.com/reference/kotlin/androidx/compose/material3/IconButtonDefaults),
[native ripple colors](https://developer.android.com/reference/android/graphics/drawable/RippleDrawable#setColor(android.content.res.ColorStateList)),
[non-text contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html).
