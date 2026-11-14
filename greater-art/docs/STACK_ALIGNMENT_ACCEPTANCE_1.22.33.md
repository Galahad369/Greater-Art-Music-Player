# Backing-alignment acceptance checkpoint — 1.22.33/code354

14 November 2026. The full six-take goal is **not complete**. This is a source
and evidence coverage review, not a release approval or an independent audit.
No production matcher, audio quality, shared-output flag, or public APK changed
after the [disabled window-kernel experiment](STACK_PCM_KERNEL_1.22.33.md).

## What the evidence actually covers

| Requirement | Current evidence | Remaining gate |
|---|---|---|
| Backing, not the singer | A fresh current-source bounded mono control confidently follows the vocal at 980 ms instead of the known 700 ms backing. Shared-vocal/wrong-backing raw input is also accepted. | Music identity and timing must both pass the unchanged positive and negative controls. A tighter drift loop cannot fix a wrong content map. |
| Five companions against Miku over the whole song | Earlier reference experiments qualify only limited native maps, not all six. | Distributed and held-out backing evidence for every valid companion; rejecting them is not completion. |
| Actual six-source output | E39's one joint clock predicts saved shared-output intervals with NCC about .997; all six independent source correlations exceed .98. | Original absolute gain checks still fail. Historical route/fader was not bound, so current-policy consistency cannot retroactively pass them. No whole-song backing claim. |
| Prospective unity-volume output | E41 verifies all six original MP4 hashes, but stops before capture because the requested Android volume did not take effect. | No new PCM exists and no output gate passes from this attempt. Keep the scorer, .90/.45 correlations, 5% gain, .001 absence floor and 2 ms tolerance unchanged. |
| Native precision | Current native 24-bit, quiet Float, mono/stereo clip and ordinary continuity tests pass. | Physical HDR/acoustics and complete function/format parity remain separate. |
| Native performance | Six of nine unchanged stress tests pass. Three fail with 14, 2 and 1 underruns. | Zero-underrun/cadence acceptance; do not hide failure with output pauses, lower quality, buffers or weaker assertions. |
| Non-unit timing scale | `SharedClockPcmPrototype.start` explicitly rejects `alignmentScale != 1.0`; no pitch-shifting substitute is used. | Qualified per-track pitch-preserving tempo correction before a shared-output replacement can preserve this existing function. Nine green native tests alone would not establish parity. |
| Normal UI/shared-output ownership | Current source constructs the prototype in instrumentation hosts, not the production Stack coordinator. Both release flags are false. | A separately approved production host, native video/surface ownership and all normal UI transitions still need verification after the independent engine gates. |
| Apply, manual Earlier/Later, save/reload, seek, primary change and loop | Existing source/tests and earlier diagnostic UI evidence are preserved. The exact optimized33 smoke covers normal Miku playback/pause/fullscreen, not these entire six-take sequences. | Exercise the complete sequences on the exact eventual candidate with actual mixed-output evidence. |
| Sound/picture separation | Controller methods and distinct result state remain unchanged. Earlier Quick/Wider evidence is retained. | Picture regression does not rescue sound failure; keep Riona's visual evidence and all existing deadlines/ranges separate. |
| Signing and artifact | Optimized33 bytes and maintenance identity remain preserved. | The original public signing key remains unavailable. This candidate is not an original-public update-compatible release. |
| Current A55 environment | A later cold start retains package metadata but loses its APK directory. The final repair run hangs during guest reboot. | Current cold-start persistence and final runtime restoration are not verified. Earlier successful smoke evidence remains historical, not erased or expanded. |

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

## Preservation and next authorized step

Private evidence is under `App/output/stack-resume-20261114-2235/`: E40/E41
protocols, `baseline40-regional-current-03/`, failed acquisitions,
`a55-cold-storage41-01/` and `actual-six33-unity-volume-recover-02/`.
The old failed classpath runs are retained; the corrected math harness compiles
fresh matcher classes and only current DTO types, with unchanged scorer source.

No further recorder-route matrix, model swap or performance variant is justified
by these failures. First obtain a stable permitted host/device environment and
complete cold-start persistence. Then the already defined unity-volume output
check and a fully acquired, hash-verified backing candidate can be evaluated
without relaxing original gates. Preserve the qualified raw route; no microphone,
sample repair, per-voice fit or picture-based timing may conceal sound failure.

The optimized candidate remains
`App/output/stack-resume-20261114-2235/optimized33-01/GreaterArt-1.22.33.apk`,
SHA-256 `79cc0106017f814e9bbaa7fa5912e4fea0f9a0b1fc7df73d1922fb33fde191d3`.
No push, merge, PR, public release, key replacement, or cleanup occurred.
