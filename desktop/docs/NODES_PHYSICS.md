# Windows Nodes physics

Desktop 0.1.14/code15 (physics introduced in 0.1.13). Android is outside this change.

The reference is [Obsidian's documented graph interactions and forces](https://obsidian.md/help/plugins/graph): connected nodes respond to dragging, stronger hubs appear larger, hover emphasizes neighbors, and center/repel/link force/link distance are adjustable. Greater Art still builds edges only from local filename similarity; this does not introduce note links, tags or online metadata.

## Behavior

- Drag a node to hold it under the cursor. Its linked neighbors follow through damped springs; releasing it lets the network settle. Drag empty space, use the middle mouse button, or hold Space and drag to pan. The two modified pan gestures work over nodes too. Release/focus loss clears the gesture; typing Space into search still works. A slow drag must never start a song.
- Scroll to zoom around the cursor. Fit recomputes bounds only on request; physics never moves the camera underneath a held node.
- Hover highlights the node's links and immediate neighbors. Labels become available on hover and zoom, with collision rejection. Playing media keeps an accent ring.
- Hub radius follows normalized weighted degree. Stronger links have shorter target distances and greater stiffness. High-degree hubs receive normalized spring forces so they do not accelerate once for every connected edge.
- The wand reveals nodes with a small spring overshoot and restarts local physics. It does not scan files, recreate the player or touch the decoder.
- The Nodes gear exposes center force, repulsion, link force and link distance, plus restore defaults. Values persist in an optional trailing preferences section; pre-existing preferences keep their playlists/history and default forces.
- The graph settles instead of shaking indefinitely. Hidden panes stop requesting work. Windows reduced motion uses a final background-computed layout without the reveal animation.

## Implementation / performance

`GraphSimulation` owns world-space positions and velocities. It integrates at 1/240-second numerical steps regardless of display refresh; this is a physics precision choice, not a video/frame-rate limit. Numerical catch-up is bounded to 50 ms while cooling follows accepted elapsed time, coalesced up to 250 ms. Springs use similarity-weighted rest lengths, softened Barnes–Hut repulsion, node-size collision clearance, damping and bounded velocity. Coincident points separate deterministically. No node is omitted for large libraries.

`GraphMotion` runs below normal priority on its own worker, with one coalesced request and one replaceable result. Library generations discard stale output. UI drag positions stay authoritative. Main-thread drawing batches edges and circles into physical-DPI antialiased paths; there are no new runtime dependencies, media reloads or quality reductions.

Core regression coverage includes strong/weak equilibrium, 60/180-update equivalence, pinned drag/neighbor response, settling/reheating, coincident nodes, 2,000-node bounds, empty/singleton layout, stale generations and preference compatibility. `--verify-graph <local-video> <isolated-output>` exercises the real window with its surrounding local library, captures only the owned window and checks continued native playback. Test outputs stay local and ignored; do not commit a user's paths, filenames or media.

This reproduces the interaction principles, not Obsidian's proprietary implementation or exact numeric tuning. Physical display smoothness and very large libraries still depend on hardware. The probe must pass before a new installer is described as verified.
