# Fullscreen controls/visual acceptance — 1.21.51/code274

## Source versus device verification

The implementation is SOURCE_ONLY. The immutable validated sideload APK remains
1.21.39/code262. Android CI runs JVM/lint/assemble; it does not execute the
Android instrumentation tests on hardware or emulator. Device acceptance MUST
be separately recorded against the exact 1.21.51 build SHA and certificate.

## Manual interactions (portrait and landscape, Android 14/15/16)

1. Start a playing video; controls are visible. Tap the middle 30% of the
   picture ONCE: chrome disappears. Tap the same middle region once more:
   chrome reappears. Verify it still auto-hides after ~2.5 seconds idle
   while playing, but remains shown when paused.
2. Tap the left/right side twice quickly: seek backward/forward by the
   configured jump. When controls are hidden, a side tap reveals them.
   Single centre taps must NOT seek or start 2× hold playback.
3. Start a seek drag and keep the finger down for >3 seconds. The slider,
   timestamps and chrome must remain visible throughout. Release: the seek
   completes, then the auto-hide interval restarts.
4. Hold video to activate temporary 2×; verify feedback is below the
   toolbar, never behind buttons. Drag down to lock 2× and check state.
   While pinching or panning the video, chrome must not auto-hide and
   side seek/hold must not trigger.
5. Zoom in; tap centre to hide/unhide chrome, then pan with one finger.
   Exit zoom to 1× and repeat ordinary centre/side gestures. Verify no
   gesture cancellation at the 1× threshold.
6. Lock the player controls and tap centre to hide/reveal the unlock
   action. Confirm seek/play controls stay blocked until unlocked.
7. Repeat on a 4:3 and an anamorphic/non-square-pixel clip. Video panning
   must follow the DISPLAYED aspect ratio, not raw encoded pixels.
8. In 320dp, 224dp, 160dp, 110dp and very short landscape/foldable
   viewports, check the top toolbar and 2× feedback do not overlap.
   Transport controls/timeline should not collide with compact toolbar;
   essential 48dp Rotate/Lock/Exit/Close buttons must remain reachable.
9. Enter/exit Android native PiP repeatedly; record video first-frame
   continuity, transient black/cropped frames, player identity, and return
   destination on API34, API35 and API36. The existing PlayerView handoff
   and API34 sync workaround are retained; no universal anti-flicker
   guarantee is established without device evidence.

## Automated checks

- testDebugUnitTest: `Release1114Test` centre zone, auto-hide eligibility,
  video FIT pan and Media3 pixel-aspect corrections.
- `FullscreenRotationTest` includes a fixture-dependent tap to
  hide/reveal plus existing portrait/landscape bounds and video ownership.
- `FloatingPresentationIntegrationTest` exercises native PiP player
  identity with the owned Ambient fixture.
- Run connectedAndroidTest on the configured emulator with the owned
  `/sdcard/Download/GA-Ambient-Test/ga-ambient-scenes.mp4` fixture,
  then inspect screenshot/recordings and failure logs. CI compilation of
  instrumentation sources is not evidence of executing them.

Record exact device, API, model, test run, screenshot/recording, and PASS/FAIL.

## October 23 — 1.21.72 Lock/top bar renderer parity
Every *visible* top toolbar action, including Lock/Unlock, is now drawn inside the actual toolbar through one shared renderer. When locked, an invisible but accessible Unlock hit target sits above the input blocker at the same measured position. Verify a consistent 48dp target, centered 32dp icon canvas and 26dp glyph against Rotate, Fullscreen and Close across portrait/landscape, locked/unlocked and paused/playing. The lock stays above its input blocker and uses the existing shared 180ms chrome fade. On fullscreen video, wait 2.5 seconds while idle and check all toolbar buttons hide together; center tap reveals them, and touch/seek/zoom gestures temporarily suspend the timer. Never accept a CI compile or geometry-only test as proof of visual fade parity on hardware.

## October 23 — 1.21.73 nonpersistent 2× feedback

- Hold on the fullscreen video to engage temporary 2×: the "2× · Pull down to lock" banner remains visible while actively holding.
- Pull down to lock 2×: "2× · Locked" confirms for about 1.2 seconds, then disappears without changing the locked 2× playback speed. It must not reappear on every 500ms position update, pause/resume, or regular video-controls show/hide.
- Start with 2× speed already active: no permanent video-stage banner. Hold to arm a return to normal speed: "Release for 1×" appears during the gesture and disappears at release.
- Verify the 2.5-second toolbar auto-hide, centre-tap toggle, video surface ownership, Lock/Unlock toolbar, seeking, pinch/pan, and both orientations remain intact.
- Source-only unit tests are not a substitute for real-device timing/visual acceptance.
