# WinUI Nodes verification — 0.1.21/code22

Date: October 29, 2026. State: **locally verified preview**, not a complete desktop release.

These binary results were recorded from desktop changes tested on source base `1f17884`, before Git integration. The Windows changes are integrated separately onto `main`; the Android commit in that original build base is not part of this desktop change. No branch switch, stash restore or new worktree is needed for that integration. Other-chat Android changes in this checkout are preserved and excluded from this work. Git integration does not relabel or rebuild the tested binaries.

## Run the tested build

Open `desktop/winui/bin/x64/Release/net10.0-windows10.0.26100.0/win-x64/publish/GreaterArt.WinUI.exe` from the repository. Keep the entire published folder, including compiled XBF/PRI resources, DLLs and `Codecs`.

No versioned ZIP or installer was frozen in this task. The previous frozen native 0.1.16 delivery is not replaced or relabelled.

| Tested file | SHA-256 |
| --- | --- |
| GreaterArt.WinUI.exe | `2B9C00E78C24A106113CC0419E74711BCBAECE062B94A085504C357363897F16` |
| GreaterArt.WinUI.dll | `5F54206F79138A6583D2556A8204D3280695D86375248F69BBBCA7374B7935D6` |
| GreaterArt.Engine.dll | `07AC69F222C65AD25D4A8D4C79CEBF0126B1BF60FA37DD40F185485562D890E4` |

The verification script checked all three hashes before and after both cases. They did not change during the run. These hashes identify this local build, not an authenticated publisher or a signed release.

## Changes

- The final drag coordinate crosses the native worker boundary before the pin is released. Sequenced release/reset commands suppress stale positions and old in-flight frames. A force reset retains the dropped point.
- Large graphs use a coprime stride to sample 384 distinct other files per node. This removes repeated short sampling cycles. Every file remains a node; graph cache v3 invalidates the old result.
- The gear opens bounded, collapsible Filters, Groups, Display and Forces sections without reducing the canvas height.
- Filename filtering, minimum similarity, orphan visibility, local depth 1–3 and first-match color groups operate on the existing graph. They do not restart playback or recompute filename similarity.
- Labels respond to zoom, selection, playback and hover. Weighted hubs stay larger at fitted zoom. The currently playing track has a separate ring.
- Arrow keys pan, +/− zoom, Home fits and Enter plays the selected track. Middle-mouse and Space+drag pan over nodes. Escape can close settings without applying canvas navigation to a text field.
- Selection survives an unchanged-library refresh. Enabling local mode chooses a visible center rather than a hidden old selection. Empty results have an explicit message.
- WinUI graph preferences use separate, debounced atomic storage. Verification uses isolated storage, not user preferences.
- World-coordinate paths now live inside Canvas. The outer viewport supplies the final clip. This fixes nodes lost by Grid layout clipping before their camera transform.
- Published builds explicitly include compiled XBF and PRI resources. The published EXE now launches without a source-tree resource fallback.

## Checks performed

| Check | Result |
| --- | --- |
| Full native desktop build and native self-tests | PASS, 81/81 |
| Native graph worker tests | PASS, 17/17, also included in the native suite |
| Actual graph DLL ABI, sampling, release/reset regressions | PASS, 15,378 assertions |
| Pure C# filter/group/depth model | PASS, 85/85 |
| Desktop identity guard and version tests | PASS, 0.1.21/code22; 9/9 |
| WinUI Release publish | PASS; compiled resources present |
| Real 1920×1080 MP4 flow | PASS |
| Real 3840×2160 MP4 flow | PASS |
| Real library | 209 files, 209 nodes, 895 edges |
| Exploration controls | PASS, 16 checks in each video case |
| Viewport math and ABI layout | PASS, 6 checks in each case |
| Isolated preference roundtrip | PASS, 4 checks in each case |
| Hidden graph | Rendering subscription stops |
| Owned-window captures | Inspected with graph settings open and closed; missing quadrants now render and hubs have distinct sizes |

Both video flows tested pause/resume, seek, dock, Mini, fullscreen, repeated presentation transfers and compact fixed controls. The original playback identity remained active and video frames advanced while Nodes was shown. No resolution, bitrate or FPS cap was added. Audio was muted by the test.

The 1.25× steady-rate checks measured 1.24970× and 1.24981×. Their rate-change warmup was 1,185 ms and 1,477 ms. This warmup remains a playback limitation, not an optimization claim. Graph callback averages were 0.108 ms and 0.133 ms; worst callbacks were 16.3 ms and 24.7 ms. These short local measurements do not establish a display frame rate or guarantee that every interaction is free of jank.

Final ignored local evidence: `desktop/winui/artifacts/nodes-0.1.21-delivery-check/verification.json`, plus each case's `flow-result.json`, `nodes-geometry.json` and owned BMP captures. Test media paths and filenames remain in ignored evidence rather than this report. Native evidence is under `artifacts/native-regression-0.1.21/`.

## Failures retained and lessons

Earlier directories are retained. They are failed or intermediate runs, not alternative final receipts.

1. A successful compile did not ensure a runnable publish. Missing compiled XAML resources broke startup. Include XBF/PRI and test the published EXE.
2. The first filter test used an untemplated control in a collapsed panel. Open and lay out the real panel, then await its actual TextChanged event. Do not substitute an immediate model call for a user-facing control test.
3. Seek resets the Media Foundation frame counter. Compare counters within one epoch. Measure rate changes after readiness and advancing time, and report the warmup separately.
4. Immediate captures can show the preceding UI state. Let the compositor display the restored state before capture.
5. Numeric tests passed while the graph was visibly wrong. All 209 projected centers were inside the viewport, but Grid clipped paths with negative world coordinates before the render transform. An isolated Canvas A/B capture exposed and fixed the visual boundary. Keep image inspection as well as model tests.

## Reference and scope

The settings hierarchy and graph exploration were checked against the [real Obsidian graph documentation](https://obsidian.md/help/Plugins/Graph%2Bview). Greater Art still connects media by filenames only. It does not claim Obsidian note backlinks, tags, chronology animation or its complete query language. The wand reheats the existing damped physics; it is not a permanent artificial jiggle.

The layout fix follows Microsoft's documented [Grid/Canvas layout boundaries](https://learn.microsoft.com/en-us/windows/apps/develop/ui/layouts-with-xaml). It reuses WinUI and the existing native graph worker. No new rendering dependency was added.

## Acceptance limits

This is not complete Android parity, production signing, a frozen installer or clean-machine deployment acceptance. Physical pointer/touch feel, Narrator, high contrast, multiple monitors/DPI settings, high-refresh scanout and physical listening were not verified by these programmatic checks. Large-library algorithm fixtures do not prove the feel of a 100,000-file real library. The test's window captures show only this application's owned windows; they are not acoustic proof or a guarantee of no future bugs.
