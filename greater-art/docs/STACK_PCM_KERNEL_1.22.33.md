# Disabled PCM window experiment —1.22.33/code354

This does not enable shared output or change the normal Media3 engine, matcher,
video quality, Follow video colors, buffers or Sonic. Both prototype flags stay
off by default. Full six-original backing/output acceptance remains incomplete.

The private preflight passed on14November2026. A copied kernel comparison passes
900 exact Float-bit cases and all9original clock-math tests. A deliberately lost
tap fails the exact-bit gate. On the unchanged A55, two alternating same-process
CPU trials measure19–23% lower mean kernel time. Desktop results were slower;
these microbenchmarks do not establish native output continuity or app acceptance.

The specialized window kernel keeps the same32-tap coefficients, Double sums,
voice ordering, gain normalization, negative-entry rule and final Float rounding.
One reusable Double accumulation buffer belongs to one render worker. Mixed-rate
lanes use the original kernel. Tests include8–192kHz, up to8voices, partial blocks,
negative/fractional offsets, five-hour clocks and quiet samples below PCM16.

The unchanged native suite passes6of9checks. Fractional192k still fails with14
underruns and205output waits; the other fractional tempo test fails at2underruns,
and the partial-block EOF test at1. Native24-bit, quiet float, mono/stereo clips,
ordinary48/192k continuity and basic pause/seek/loop/solo checks pass. Do not adopt
the engine, relax the gates or call the microbenchmark an app-performance fix.

604JVM tests pass without failures/skips; lint has0errors,54warnings and6hints.
Optimized assembly passes from source commit`d70bc83025862dc854fa4538bf53c8e2a3126dd3`,
app-input SHA-256`ccfd3a35eea6f9fd233f000c3ba700e8ce98191efa31a22fa53a8eae158fae1d`.
The private optimized maintenance APK is5,595,920bytes, SHA-256
`79cc0106017f814e9bbaa7fa5912e4fea0f9a0b1fc7df73d1922fb33fde191d3`:
`App/output/stack-resume-20261114-2235/optimized33-01/GreaterArt-1.22.33.apk`.
Its existing maintenance certificate is
`0a7b9cebf928022c1930b64f2c47581c13def3007b0e97737bbb9ca3e53de6b7`.
The original-public-signer check still FAILS; this is not a public update.

The exact optimized APK passes actual Miku Library→expanded playback, video-frame
advancement, pause/resume and fullscreen return with the same process. Inspected
screenshots show the real video and visible fixed controls. This is not acoustic
Stack, physical-HDR or whole-app acceptance. Preserve the tested1.22.32 APK.

Three earlier cold-boot attempts are retained. Android kept the install metadata
but its APK code file was missing (`pkg=null`), not merely slow package readiness.
The retained Android signature hash was matched to the candidate's verified public
certificate before a full-file, non-incremental same-key install-r. App ID10216and
CE data inode565714remain; the exact file then survives a guest reboot. A previous
incremental/streamed-install dependency is a hypothesis, not a proven cause: the
old install stdout was not retained. Future private device workers should record
the install mode, use full-file installs and check cold-boot persistence. Never
uninstall/clear app data or regenerate a key to conceal this failure.

Preferences restore byte-for-byte; original access modes, volume5/15, absent test
package and profile/GPU hashes are preserved. The owned A55 stops17:49:03UTC.

**Later cold-start qualification:** those statements describe the recorded smoke
at17:49, not a guarantee of later emulator persistence. A subsequent cold boot
again loses the APK directory while retaining install metadata. The final restore
install had occurred AFTER the previously tested guest reboot. A later same-key
recovery and final-reboot attempt hangs; the owned QEMU process is stopped after
bounded control timeouts. No new audio is captured and no current cold-boot pass
is claimed. See [the full acceptance checkpoint](STACK_ALIGNMENT_ACCEPTANCE_1.22.33.md).
Keep the original successful and failed receipts rather than overwriting either.

Evidence root: `App/output/stack-resume-20261114-2235/`.
E37's22queue/persistence checks do not qualify the native observer; its stage
closed without app integration. The recorder qualification budget remains closed.
No push, merge, public publication or cleanup is authorized by this experiment.
