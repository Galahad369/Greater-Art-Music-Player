# Claude audit integration verification — October 20, 2026

## Identity and release boundary

Review baseline: main `e96f8dc690b4e138d1303d7fbf9cbed36c8342cb`.
Original PR #191 head: `1a6627dbc133faba7ffbb110a6f0c61ec160b0d4`.
Follow-up source: **1.21.61 / code284**, SOURCE_ONLY. Preserve the original
1.21.59 and 1.21.60 commits; no amended history or reused version identity.
The independent Windows identity remains **0.1.16 / code17**.
The immutable verified personal APK remains **1.21.39 / code262**.

PR #174 is excluded. No history rewrite, tag changes, protection bypass,
network permission, analytics, source-quality limit or signing-key change.
The owner's separate local main/Nodes work and APKs are not discarded.

## Additional repairs

- `StackPlaybackCoordinator`: route a real MediaController Pause through the
  coordinator even when its underlying primary is already parked. Initialize
  replacement companions before dismantling a working Stack. Compare surviving
  EOF boundaries in the master timeline, including offsets and rate mappings.
- `PlaybackService`: session-facing forwarding commands preserve the same
  ExoPlayer and respect the Stack readiness barrier. End-of-track sleep stops
  the entire mix before shorter-primary promotion. Destroying the service clears
  its timer; closing an Activity does not.
- `PlaybackSleepTimer`: injectable monotonic clock with overflow-safe minute
  conversion. This is not persistence across process death.
- `InternalMediaAdmission` / `LocalLibraryCallback`: validate content identity
  and actual read access rather than treating scoped URIs as filesystem paths.
  Scoped external browsing rechecks grants and remains audio-only.
- `MediaFile` / scanners / rule matching: preserve original relative filenames
  so folder/extension rules do not compare opaque provider IDs or edited titles.
- `MainViewModel`: clear stale Library entries when access is revoked, invalidate
  duration failures when a source changes, and retry failed connections only on
  demand. `PlaybackConnection` uses resource-specific leases so stale borrowers
  cannot release a replacement controller.
- MPEG-TS regression covers 188/192/204-byte packets and truncated input.
  Existing high-resolution extensions remain discoverable.

## Verification record

Commands run from `greater-art/`, Java21, Android SDK explicitly configured:

```text
GradleWrapperMain testDebugUnitTest lintDebug assembleDebug
  assembleDebugAndroidTest assembleRelease --offline --no-daemon --max-workers=2
```

The repository has `gradlew`, not `gradlew.bat`; on this Windows workstation the
wrapper JAR is invoked with Java21. Do not invent a missing batch wrapper or rely
on an inherited SDK environment. One diagnostic rebuild failed because
ANDROID_HOME was omitted; rerunning with the explicit SDK succeeded.

- Android full gate passed: 417 JVM tests, zero failures/errors/skips; lint zero
  errors, 46 warnings and four hints; debug, instrumentation and R8 release APKs
  assembled. This does not constitute release acceptance.
- Windows native build passed with required pinned source-built codec libraries;
  73/73 core tests passed. No Windows executable source/version change.
- Version validator and its four merge-policy tests passed.
- Public repository audit passed, including reachable credential history, current
  files, sensitive filenames, author identities and APK containers. This is not
  an ISO certification or closure of historical privacy PR #174.
- API36 A55-sized x86_64 emulator: the first nine new real-player regressions
  passed. The expanded eleven-test run exposed intermittent early stopping after
  primary EOF promotion. That failure must be resolved and repeated before merge;
  a passing initial run is not sufficient evidence of correctness.

The failing EOF trace identified `onMainPlayChanged(false)` during the new
primary's buffering gate at roughly 813ms, not the 2400ms survivor endpoint.
Media3's reentrant listener queue can dispatch an internally generated parked
callback after the local mutation guard returns. A false state event is not a
user command. The fix intercepts actual session Pause commands independently and
ignores parked false callbacks during a gate; direct pause still cancels it.
The regression explicitly distinguishes these cases. Temporary pause stack-trace
logging was removed after diagnosis; no media-path logging is shipped.

A second real race was exposed after that fix: the mapped Stack ticker can
promote at the primary duration before Media3 dispatches ENDED. Its boundary now
consults the service's sleep decision before promotion and before final looping.
The end-of-track test keeps its original-primary assertion; it was not weakened.
The timer-flow assertion is dispatched on Main after the timer callback completes,
so it cannot inspect a partially completed state update.

On a freshly created API35 emulator, background-only test playback was correctly
denied audio focus by Android (`procState:4`). Service regressions now launch the
real Activity before requesting playback, then explicitly close it in the lifetime
test. Do not bypass this platform restriction or mislabel it as a codec failure.

API36 UI/storage run passed **25 tests**, zero ignored/failures, in 60.231s.
It includes real fullscreen theme/rotation, six audio-layout cases, native PiP,
five inspector cases, scoped discovery/mapping, URI adapters and four thumbnail
pipeline tests. Two narrow-toolbar tests live in `FullscreenToolbarTest`, not
`FullscreenRotationTest`; the initial shell wrapper incorrectly expected 27 and
flagged the successful25 run. Run the separate class rather than inventing two
passes. Portrait/landscape screenshots were inspected: controls remain in bounds,
native 1280x720 fixture remains FIT, no observed replacement player/surface.
The emulator crash buffer includes a platform Bluetooth native abort at boot;
do not call the whole device crash-free. App-specific crash review remains separate.

Owned silent 48kHz PCM fixtures exercise actual decoders and controller commands,
not audible alignment. Minute durations use an injected monotonic clock; they
were not each observed for 60 wall-clock minutes. URI tests use owned real
MediaStore entries; corrupt restore tests assert no partial preference mutation.

## Prevention and remaining gates

Keep Kotlin sources unchanged while Gradle/lint is analyzing them. Never infer an
instrumentation pass from adb's exit code alone: require the expected test count,
no failed/ignored status, and inspect assertions/logs. Record failed runs rather
than replacing their history with the latest success.

Pending: stable EOF reruns, fullscreen theme/rotation and presentation regressions,
API34/35 coverage on this exact candidate, new-head CI, normal protected merge,
exact merged-main rebuild, and candidate signature/alignment/install checks.

Follow-up API36 run: **13/13 pass**, no skipped tests, 42.005s, including all
eleven corrected real-player cases and both narrow-toolbar cases. Five explicit
EOF repeats pass after the gate callback repair. Debug candidate package is
`com.local.listentomusic`, version1.21.61/code284, pinned signer SHA256
`9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`;
`zipalign -c -P 16 4` passes. Working-tree debug SHA256 before commit/rebuild:
`0aecfe10ddad4a824c7e6cd8729327a37e85eb55fda2108e3e0348905a790491`.
This is not an artifact promotion or a substituted hash for the final merged build.

Final runtime-source build gates pass **418 JVM tests**, zero failures/errors/skips,
zero lint errors, debug/instrumentation/R8 release assembly. A fractional-density
regression was added after API35 exposed a real two-pixel toolbar shortfall:
independently rounded12dp and4dp padding consumed more than their combined16dp
budget at420dpi. Round the combined inset once and calculate whole action slots
from actual pixels; never shrink primary targets to make secondary actions fit.
The fix preserves the normal six-action320dp layout at420dpi.

API34 passes **21/21 tests**, zero failures/ignored,128.758s, with owned24-second
video and PCM fixtures: all eleven playback cases, fullscreen, two narrow toolbar
cases, six audio layouts and native PiP. Its disposable device clean-installed39
and accepted a same-certificate upgrade to61. The first cold39 launch timed out
during emulator startup, so do not claim clean-launch39 acceptance from that call.

The combined API36 run completed37/38 assertions before the screenshot helper
failed on a transient null compositor capture during rotation. The helper now
retries null buffers inside its existing7s deadline, resets its stable-frame count,
and still requires three genuine stable screenshots. It does not substitute an
image or skip the test. The complete38-case rerun and corrected API35 run are
pending at this commit. Earlier API35 passed18/21, with exactly the three48dp
target failures described above; all eleven playback cases had passed.

Companion-construction failure preservation is not a universal transaction over
asynchronous codec failures or every main-player prepare failure. Physical-phone
playback, acoustic Stack alignment, every hi-res codec, all providers, tablet/
foldable rendering and universal crash freedom are not proven here. SAF stream
replacement remains provider-dependent and is not atomic.

The separate Library scrolling request requires controlled debug/release profiling
and a separate performance branch if a measured optimization is justified. Do not
mix speculative Library rendering changes into PR #191.

## Versioned follow-up:1.21.62/code285

1.21.61 was committed as`1893d2c19685e061dffa4f471d9f45c0887bf56f` and pushed
normally to the existing PR191 branch. Fresh Android/Windows/security CI was
started for that head, not inherited from Claude's old1.21.60 checks. Windows
Python's defaultcp1252 decoding failed on a Unicode commit-range diff; rerunning
`python -X utf8 scripts/validate-greater-art-version.py --base origin/main --head HEAD`
passed. Do not modify source/history to "repair" a shell locale error.

Corrected API35 passed21/21 in73.984s, including all original48dp assertions.
The API36 combined rerun passed fullscreen/theme/rotation and the remaining37
cases, but its first Stack timer case observed an empty Stack instead of its
original primary. It is therefore **not a38/38 pass**.

The new Activity-backed tests expose two independent hazards: ViewModel connection
can mistake Stack's one primary item for an ordinary restored queue and append
Library items; reentrant media callbacks can refer to the retired primary while
the actual player has already changed.1.21.62 guards both. Real user replacement
still exits Stack, while stale null/old-item callbacks do not. Dedicated tests
cover Activity reconnect plus completed scan, unchanged one-item Stack queue,
retired callbacks, and genuine media replacement. No media-format, sync algorithm,
source quality, history, privacy-branch or scrolling UI changes are involved.

The new version preserves committed61 rather than amending it. Build and new-head
runtime/CI verification are pending. Final merge/rebuild evidence belongs below,
not retroactively in the earlier-version test results.

Old-code control: install the newly compiled two regression tests against the
unchanged61 app on API35. **Both fail** (2 tests,2 failures,15.61s): Activity
reconnection expands the primary queue from1 to3, and a retired media callback
clears Stack. This demonstrates that the new tests guard real prior behavior,
not merely reproduce the new implementation. Candidate62 is installed afterward
for the full23-case rerun. Raw control output remains in private build evidence.

Candidate62: full Android command passes in7m8s,418 JVM tests, zero failures/
errors/skips, lint zero errors, debug/test/R8 release assembly. API35 passes
**23/23**, no skipped tests,121.064s: the same two old-code-control failures now
pass together with the eleven earlier playback cases, fullscreen/theme/rotation,
both narrow toolbars, six audio layouts and PiP. Final API34/36 and62 CI remain.

## Service-boundary follow-up:1.21.63/code286

1.21.62 is committed as`c2434f8399fce4cd42aaf6e2565319b17922aba3`.
Its Android, native Windows (source-built codecs/core tests/installer step),
version, public audit, dependency review and CodeQL checks all pass. Nevertheless
its larger-Library API36 run fails2/40: reconnect observes226 ordinary queue entries
and the timer case loses Stack. API34 fails3/23 (scan readiness and subsequent
playback/presentation timeouts). Neither run is counted as acceptance. Do not
merge the source merely because CI is green.

The ViewModel guard is too early for an append already in Media3's asynchronous
admission/command queue before Stack starts.1.21.63 checks internal admission
after IO on the service looper and rejects ordinary app requests while Stack owns
playback; the session-facing player also guards every append overload at command
application time. Companion additions still use the coordinator. Explicit ordinary
app playback exits Stack first and remains allowed. No player reload, quality cap
or extra decoder is introduced by this boundary guard.

A direct late-controller-append regression verifies authoritative correction of the
optimistic controller timeline. Correctness fixtures isolate and restore scoped
folder/exclusions, playlist, resume, floating/background and DEV preferences, not
inherit unrelated filters or wallpaper. This is not a benchmark with functionality
disabled; the separate performance audit must measure each requested mode.

63 remains uncommitted while full gates and controlled old-code/candidate tests
run. Preserve both61 and62 commits; never amend a consumed identity. Earlier
versions' CI results do not establish63 acceptance.

63 full Android gates pass (418 JVM, zero failures/errors/skips, zero lint errors,
debug/instrumentation/R8 release assembled,7m9s). The direct late-append test fails
on unchanged62 (1 test,1 failure,23.951s) and passes on63. API36 then passes the
complete **41/41** combined run, no ignored/failures,305.519s, including the real
larger-Library reconnect and all earlier UI/storage/thumbnail coverage.

API34's remaining scan/video timeouts were traced to a concrete fixture error:
`appops get ... MANAGE_EXTERNAL_STORAGE` returned`default`, with recent denied
access, after the update. Reapply and verify the QA`allow` prerequisite after each
install, not just on initial AVD creation. Do not modify playback to hide denial.
With that prerequisite corrected,63 passes **24/24**, zero ignored/failures,
141.950s: all14 playback cases plus fullscreen, narrow toolbars, audio layouts and
PiP. Passing39→61/62 update installation is distinct from clean cold-launch39
acceptance; the first cold launcher call timed out during emulator startup.

API34/36 portrait/landscape screenshot files are retained with the raw build
evidence. Final API35 rerun,63 CI and merged-main rebuild are still required.

## Final63 pre-integration acceptance

| Gate | Result | Scope |
| --- | --- | --- |
| JVM |418 passed;0 failed/errors/skipped|Pure logic, timer clock, mapping, scanners, leases, pixel bounds|
| Lint |0 errors;46 warnings;4 hints|Warnings/hints retained, not hidden|
| Android build |PASS|Debug, instrumentation and R8 release; wrapper9.8.1/Java21|
| API36 |41/41;305.519s|Fourteen real-player cases, fullscreen/theme/gestures, audio, PiP, inspector, scoped storage, thumbnails|
| API34 |24/24;141.950s|Fourteen real-player cases plus fullscreen/audio/narrow toolbar/PiP|
| API35 |24/24;144.726s|Same24-case matrix on Android15/420dpi|
| Local Windows |73/73 core tests;build PASS|Pinned codec source installation;unchanged0.1.16/code17|
| Preceding62 CI |PASS|Android,Windows/version/public audit/dependency review/CodeQL;not inherited as63 CI|

No ignored/fixture-skipped cases in these final runs. All devices are x86_64
emulators, not Samsung hardware. API36 is1080x2340/density450; API34/35 are
1080x2400/density420. Portrait/landscape screenshots were inspected: native FIT
frame retained, toolbar/timeline/transport inside safe bounds, no observed player
replacement. Narrow110dp/160dp component tests are not full tablet/foldable QA.
The preserved24-second1280x720 fixture is not a source-quality cap.

The original39 artifact hash/certificate remain unchanged.63 has not been
published/promoted, and no physical-phone, sample-perfect acoustic sync, all-codec,
all-provider or universal crash-free claim is made. The separate scrolling audit
is measurement-only until a dominant bottleneck is demonstrated; its scripts,
traces and experimental candidates do not belong in PR191.

At the63 commit, fresh-head CI and exact merged-main rebuild remain integration
gates. Record their actual SHAs/results in the PR and final technical report;
never substitute a branch or older-version pass for merged-source verification.
