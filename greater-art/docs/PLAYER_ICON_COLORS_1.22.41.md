# Player icon colors — 1.22.41 / code 362

**Current test-only follow-up: 1.22.42 / code 363.** All production color fixes
below remain byte-identical. The 41 optimized APK is retained, not overwritten.
Its A55 diagnostic run passed six tests but could not draw the offscreen native
ripple in two tests. The follow-up attaches the real compact view to a window,
captures actual pressed pixels and requires the same check to reject an injected
old mint ripple. It does not weaken the icon-tint or disabled-button assertions.
The original 364-byte preferences are retained after a restoration assertion
failed; restore and verify them before continuing device testing.

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

## Prevention

Do not hard-code enabled glyph tints inside a button with disabled color roles.
Do not put a white-on-translucent-white transport over arbitrary video. Native
and Compose hosts must use the same adaptive policy, including intensity and
fixed-theme exclusions; update the press ripple with the glyph, not only at view
creation. Keep local button contrast separate from foreground video rendering.

References: [Material IconButton colors](https://developer.android.com/reference/kotlin/androidx/compose/material3/IconButtonDefaults),
[native ripple colors](https://developer.android.com/reference/android/graphics/drawable/RippleDrawable#setColor(android.content.res.ColorStateList)),
[non-text contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html).
