# Backing-alignment acceptance checkpoint — 1.22.33/code354

14–15 November 2026. The full six-take goal is **not complete**. This is a source
and evidence coverage review, not a release approval or an independent audit.
No production matcher, audio quality, shared-output flag, or public APK changed
after the [disabled window-kernel experiment](STACK_PCM_KERNEL_1.22.33.md).

## What the evidence actually covers

| Requirement | Current evidence | Remaining gate |
|---|---|---|
| Backing, not the singer | A fresh current-source bounded mono control confidently follows the vocal at 980 ms instead of the known 700 ms backing. Shared-vocal/wrong-backing raw input is also accepted. | Music identity and timing must both pass the unchanged positive and negative controls. A tighter drift loop cannot fix a wrong content map. |
| Five companions against Miku over the whole song | Earlier reference experiments qualify only limited native maps, not all six. | Distributed and held-out backing evidence for every valid companion; rejecting them is not completion. |
| Actual six-source output | E39's saved shared-output intervals and E52's NEW raw recording support six-source prediction. E52 held-out mix NCCs are .995299/.996177, with all12 individual correlations .963820–.985922. | ALL original absolute gain checks still fail. These selected sound maps do not establish backing timing, complete-song drift or native parity. |
| Prospective unity-volume output | E52 observes the normal system slider/getter at15 before and after capture; native policy separately records speaker15/default-device5. Complete raw PCM and lossless first20s measurement are retained. | Observed gain ratios .023145–.023555 still fail the original unity±5% rule. The active capture route/fader is not proved by surrounding dumps. Do not fit/rescale captured audio or change .90/.45 correlations, .001 absence floor or2ms tolerance. |
| Native precision | Current native 24-bit, quiet Float, mono/stereo clip and ordinary continuity tests pass. | Physical HDR/acoustics and complete function/format parity remain separate. |
| Native performance | Six of nine unchanged stress tests pass. Three fail with 14, 2 and 1 underruns. | Zero-underrun/cadence acceptance; do not hide failure with output pauses, lower quality, buffers or weaker assertions. |
| Non-unit timing scale | `SharedClockPcmPrototype.start` explicitly rejects `alignmentScale != 1.0`; no pitch-shifting substitute is used. | Qualified per-track pitch-preserving tempo correction before a shared-output replacement can preserve this existing function. Nine green native tests alone would not establish parity. |
| Normal UI/shared-output ownership | Current source constructs the prototype in instrumentation hosts, not the production Stack coordinator. Both release flags are false. | A separately approved production host, native video/surface ownership and all normal UI transitions still need verification after the independent engine gates. |
| Apply, manual Earlier/Later, save/reload, seek, primary change and loop | E46 passes11 unchanged A55 callback/transport checks. E48/E50 manual/restart/save/reload/pause/primary flows pass. E58 reads five automatic-review rows and Apply sets the three inspected values correctly before its private scroll guard stops. | E58 aggregate remains FAILED; the other automatic applied values and saved reload are unverified. State/UI observations are not backing or mixed-output acceptance after every action. Whole-song loop/Apply/seek output remains incomplete. |
| Sound/picture separation | Controller methods and distinct result state remain unchanged. Earlier Quick/Wider evidence is retained. | Picture regression does not rescue sound failure; keep Riona's visual evidence and all existing deadlines/ranges separate. |
| Signing and artifact | Optimized33 bytes and maintenance identity remain preserved. | The original public signing key remains unavailable. This candidate is not an original-public update-compatible release. |
| Current A55 environment | Later cold starts and normal entry preserve exact APK/data identity. E46/E48/E50/E52 restore original preferences, volume, app-ops, test absence, APK/data and profile hashes; owned guest shutdown completes. | Preserve E41/E43/E45/E51 failed harness/environment reports. Later success does not prove their root causes, physical hardware quality or complete app acceptance. |

## E40 — a different separator was researched, not executed

The explicitly MIT-licensed KimberleyJensen Mel-Band RoFormer ONNX conversion
was inspected as an isolated alternative to the earlier KUIELab MDX-Net analysis.
The code driver is pinned at `3f727cde9710cd0237130a96155a70e2c850d553`.
The one proposed FP32 model is 931,054,408 bytes, publisher SHA-256
`1b8afd7780d8a234527748821dee6bc746d346f2088751c12fd48e8c873f625a`.

The code/model licenses, driver source, tests and release manifest were inspected.
The model and binary-runtime transfers time out before verification. A second,
bounded range transfer also times out. Neither model file has a successful final
hash receipt. One file is preallocated: its apparent full length is NOT completion.
No downloaded model, driver unit suite, separation, new offset or app integration
ran. This is an acquisition block, not evidence that this separator is accurate
or inaccurate on the six takes. Do not execute/bundle partial downloads.

Sources: [published model and manifest](https://github.com/santiquiroz/port-bs-roformer-onnx/releases/tag/models-v1.0),
[model author's explicit license](https://huggingface.co/KimberleyJSN/melbandroformer/blob/main/README.md),
[pinned driver](https://github.com/santiquiroz/port-bs-roformer-onnx/tree/3f727cde9710cd0237130a96155a70e2c850d553/driver).
The publisher reports substantial memory/compute cost; those figures are not
local or mobile measurements. Do not add this large model to Android on that basis.

## E41 — environment failure, not new capture evidence

The old current-policy prediction at volume 5/15 is .02364558 amplitude. The
prospective experiment would use 15/15 and unchanged unity-gain checks instead
of scaling old audio. No volume change was observed: the immediate query still
reports 5. No scrcpy recording starts. Do not count a requested setting as applied.

Read-only cold boots confirm `pkg=null` and `ENOENT` for the recorded APK directory,
including after an extra 30 seconds. Other package scanning completes. The generated
hardware configuration resolves the profile's `<temp>` default to the existing
userdata image; `<temp>` alone therefore does NOT prove disposable storage. Disk
metadata reports no corrupt/dirty flag, not a complete guest-filesystem check.

A same-file, same-certificate install restores appId 10216/data inode 565714 without
uninstalling Greater Art, wiping data or changing the key. Preferences restore byte-for-byte, original app-ops
and test-package absence are restored, and the exact optimized APK hash is checked
before the later guest reboot hangs. Both the standard ADB and emulator-console
channels then time out. After bounded graceful attempts, only the PID/start-time/
executable/path-verified owned QEMU process is stopped at 18:57:26 UTC. Profile hashes
are unchanged. Other desktop apps and the physical phone are untouched; no GPU
setting changes or disk-image deletions occur. A final cold-start/restoration
pass is NOT claimed.

The final missing codePath belongs to the last restore install shortly before
console shutdown. A guest reboot had checked an earlier install, not that final
write. Generated QEMU arguments use `cache=unsafe`; this is relevant durability
evidence, not proof of the cause. Do not keep reinstalling blindly, bypass the
signature check, wipe the AVD, edit its profile or claim a mere readiness delay.

## E42 — smaller direct instrument model, still not qualified

The official [Open-Unmix HQ author record](https://zenodo.org/records/3370489)
and its [publisher API](https://zenodo.org/api/records/3370489) explicitly license
these weights under MIT. This is distinct from Demucs code licensing: inspected
Demucs author metadata does not grant a clear current product-model licence, so
no Demucs weights are adopted. The Open-Unmix
[source revision](https://github.com/sigsep/open-unmix-pytorch/tree/fb672c9584997c2b05e148eeaa65b4c23ed4693b)
is pinned; four35,637,796-byte weights match publisher checksums and full local
SHA-256 receipts. An existing Torch2.7.1 runtime is reused read-only, CPU only,
without installs, uploads or changes to its owning application.

The author's direct drums+bass+other estimates are summed at unchanged HQ
defaults, not constructed as mixture minus vocals. Safe tensor-only loading
first detects three old preprocessing entries; exact rate/window validation
handles only those duplicate entries while all learned weights load strictly.
The retained failed loader log is not a quality-test result. The private adapter
omits an unused torchaudio import only; model/filter bodies are unchanged.
Three transform round trips pass with maximum absolute error1.49e-7.

The two tested positive regions find the correct700ms backing rather than980ms
vocals, with NCC0.999559/0.999084. The four tested negatives are rejected. But the
second true-music case has retention0.1997119974, below the frozen0.20 admission
floor. It FAILS; the9.125s run stops after6of18 cases. Original scorer hashes,
2ms tolerance, band and admission boundaries remain unchanged. Do not lower or
round the floor, claim all-six performance, run the held-back later gates, or
adopt this candidate from two correct timing values. Evidence and stop conditions
are in the notebook's E42 entry and the private`unmix42-controls-02/report.json`.
Both production shared-output flags and the tested optimized33APK remain unchanged.

## E43/E44 — persistence observed; volume-command acceptance still fails

E43's later two cold boots retain the exact optimized33file, appId10216 and inode
565714 without any reinstall, wipe or key/profile/GPU change. The first private
harness incorrectly calls non-exported MainActivity; its corrected entry uses
PackageManager's MAIN/LAUNCHER component. Both normal entries reach Library.
Screenshots are inspected, including a still-dimmed second entry; this is not
whole-player rendering acceptance. Guest sync/shutdown exits normally. Its
aggregate report nevertheless remains FALSE because a20s automatic shutdown
watchdog outlasts the final5s poll. Later independent absence is not used to
rewrite that failed report or prove the cause of earlier missing code.

E44 again verifies the same exact APK/data and captures the volume command's
real response. It exits0 but nine observations over5s remain5/15, disproving the
simple asynchronous-read hypothesis. No diagnostic APK is installed, no audio
is acquired and no new source-output score exists. Original volume5, app-ops,
test-package absence and optimized APK/data are observed on restoration. The
guest shuts down and its watchdog finishes; no emulator remains20:00:25UTC.
Keep these bounded native observations separate from a full alignment/release
pass. Evidence is in`a55-guest-shutdown43-02/` and
`actual-six33-unity-volume44-01/` in the existing private resumed-goal folder.

## E45–E52 — live functions improve coverage; new output still fails gain

The E45 caller/package explanation is not confirmed. Its evidence precondition
fails before any alternative command/capture, and its restoration wrapper also
records an assertion failure. E46's later full preservation check is separate;
do not rewrite E45 as passed. E46 runs11 unchanged manual/review/transport device
tests, all pass,0skips. No recording test or matcher assertion is substituted.

E47 inspects retained model output without new inference or gate changes. Known
backing itself retains .233756 of mixture RMS in the second failed positive,
above the .20 floor, while the model retains .199712. Model/truth RMS .854361,
truth NCC .897136 and normalized error .443821 show actual evidence loss, not
just rounding. The candidate remains rejected; do not weaken its admission.

E48/E50 use the exact optimized APK and real exported launcher/taps, not injected
session state. All six original MP4 hashes are verified. Riona +17.375ms Apply,
Cancel of999ms, process-restart save/reload and Riona-primary/Miku−17.375ms rebase
and reload pass. Playing/pause/resume controls work. E48 nevertheless preserves
a3/6screenshot after a6/6XML observation. E50 challenges that weak single sample:
four quiet native snapshots over15s show six active44.1k stereo app tracks with
zero per-track underruns; the next nine UI snapshots over about19s show6/6.
The original transient is not erased or assigned an unsupported cause. Neither
track counts nor player positions prove actual mixed audio or backing alignment.

E51 reaches system media volume15 through the ordinary Android Sound slider;
restoring5 uses that same UI, without policy overrides. Its initial actual raw
capture completes but a missing private report-writer `hashlib` binding fails
the observation. Raw files and the failed receipt remain. E52's preflight catches
that exact old dependency and validates the corrected private context before
device work. The frozen capture-function body and every numerical gate remain.

E52 then retains995,328native48k S16 stereo frames; the lossless float first20s
contains960,000frames and all35,328tail frames remain in the raw file. No captured
PCM is resampled, repaired, trimmed to fit timing, or gain-normalized. Native WAV
SHA-256`a33bb127b2465be6e0aa05f5da882b2457ace9cea602355cc220e59f3fb06807`;
measurement WAV SHA-256`8f1b88b04e94573e9856fde904c65dc1021d1182b48b6ff52ae66c176a3e387f`.
The unchanged original six-source diagnostic trace passes1test. Its selected
offsets are NOT newly qualified backing maps and this does not enable the engine.

The fixed joint-source predictor passes its known-clock/omission/wrong-sign/
10%-gain-fault controls before scoring the new recording. One common training
origin62.640583s has NCC.996048; held-out[2,6]/[14,18]mix NCCs are.995299/.996177.
All12source correlations pass. All12absolute gains nevertheless FAIL at
.023145–.023555 rather than unity. Native before/after policy contains
speaker index15 but default-device index5. This independently challenges the
assumption that a stream getter15 guarantees every route15; it does NOT prove
the active route during capture or retroactively pass failed gains. No further
recorder/backend/volume variation or scorer change follows this failure.

E52's complete acquisition/restoration receipt passes; original preferences,
volume5, app-ops, test-package absence, exact optimizedAPK/appId/inode and both
profile hashes are restored. No owned emulator remains20:41:19UTC. This is a
restoration pass, not a full backing, physical acoustics or release pass.

## E53–E55 — preserve the cancellation miss and separate device indices

The exact optimized real-six sound action completes with2/5proposals before
E53's first running-state hierarchy read. Its cancellation precondition fails;
no Cancel is exercised and no cancellation pass or app hang is claimed. The
failed screenshot/report are retained. APK/preferences/settings restore before
guest shutdown then hangs and requires the verified owned-process stop.

E54's separate READ-ONLY cold boot confirms exact optimizedAPK/appId/inode,
original access/volume5 and test-package absence, plus normal exported Library
entry. No recovery install or setting write occurs. Guest shutdown finishes
and no emulator remains21:00:25UTC, with profile/GPU unchanged. This establishes
later preservation, not retrospective acceptance of E53.

E55 reuses the already pinned AOSP curve formula offline with the E52surrounding
policy: speaker15/default-device5. The independent index5prediction matches all
12observed common gain factors within2.1161%; separate index4/6/15controls do
not. This tests an assumption, NOT the original acceptance. Unity-gain scores,
captured audio and thresholds remain untouched and FAILED. The active capture
route/fader still is not proved. No further recorder/volume variation follows.

## E57/E58 — actual Sound Apply only partly observed

Actual review screenshots show Riona17.375→0.65ms and unc0→83.216ms, with the
other three tracks unchanged. One private hierarchy-reader repair is proved on
retained XML, without changing app code or any equality assertion. Apply timing(2)
resumes the six-track group. The first three normal manual dialogs agree:
Riona0.65ms, unc83.216ms, Forte0. The next scroll area is ambiguous to the test
driver, so the full test FAILS; later applied values, automatic-map save/reload
and following primary assertions are not reached. No further geometry retries.
Sound proposals are not independently qualified backing truth. Both failed
receipts remain; final exact preferences/APK/data/settings restoration and guest
shutdown pass at21:18:23UTC, with no emulator or stored profile/GPU change.

Frozen-gate source review also distinguishes relative combined/solo recorder
gain qualification from the actual-source scorer's absolute coefficient/GAIN
check. Common system attenuation cancels only in the former. This explains
why qualification need not imply absolute unity, not the actual active route.
Neither gate changes and the failed actual gains/full goal stay failed.

## Preservation and next authorized step

Private evidence is under `App/output/stack-resume-20261114-2235/`: E40/E41
protocols, `baseline40-regional-current-03/`, failed acquisitions,
`a55-cold-storage41-01/` and `actual-six33-unity-volume-recover-02/`.
The old failed classpath runs are retained; the corrected math harness compiles
fresh matcher classes and only current DTO types, with unchanged scorer source.

E42 was feasible only after independently finding an installed compatible runtime
and a smaller explicitly licensed model with a distinct instrument-head design.
Its failed required gate closes that candidate; no further recorder-route matrix,
model/head/band tuning or performance variant follows. Later native persistence,
normal function and acquisition/restoration evidence now exists; do not repeat
the obsolete claim that no new PCM was captured. The new gain failure and backing
candidate rejection remain open, distinct causes. A future backing candidate must
first pass unchanged distributed positive/negative controls, before an app host,
whole-song actual-output and full function/format acceptance. Preserve the
qualified raw route; no microphone, sample repair, per-voice fit or picture-based
timing may conceal sound failure. Latest private evidence includes
`alignment-controls46-01/`, `retention47-diagnosis.json`, `stack-normal-ui48-01/`,
`stack-sustain50-01/`, failed`actual-six33-system-volume51-01/`,
`actual-six33-system-volume52-02/` and`shared-prospective52-score-01/`.

The optimized candidate remains
`App/output/stack-resume-20261114-2235/optimized33-01/GreaterArt-1.22.33.apk`,
SHA-256 `79cc0106017f814e9bbaa7fa5912e4fea0f9a0b1fc7df73d1922fb33fde191d3`.
No push, merge, PR, public release, key replacement, or cleanup occurred.
