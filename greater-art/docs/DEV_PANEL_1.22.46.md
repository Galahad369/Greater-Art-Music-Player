# DEV panel repair — 1.22.46 / code367

The actual1.22.45 optimized APK passed DEV placement, locked queue scrolling and
correct pull destination on the A55. The final normal DEV → Pick check failed.
This is an app regression, not a test timeout to waive: the service's diagnostics
panel is inline content, not an independent Dialog window. Moving the whole
component into the72×52dp badge slot also constrained its panel and hid actions.

## Repair

Share a small local diagnostics state between the toolbar badge and full-player
panel. Render only the badge inside the handle's parent, retaining its drag input
and geometry. Render the panel at the player root, retaining all summaries,
technical details, warning indicators, region IDs, permission checks and complete
Copy Bug Report output. Non-player callers keep the original default badge/panel
API and behavior. No new library, ViewModel, storage or permission is needed.

This follows [Compose state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting).
State has one owner; layout constraints must not be shared merely because state
is shared. A component-only fixture cannot prove production placement, so keep
the actual service-hosted DEV → Pick native check mandatory.

## Acceptance

- A narrow badge can open a full-width service panel; Pick is visible and tappable.
- Actual DEV → Pick → compact prompt → Cancel works on the exact optimized APK.
- Rerun the unchanged queue, pull, toolbar, notice and diagnostic regression checks,
  plus the new narrow-badge/full-panel test. Do not relax prior assertions.
- Preserve45's commit, failed exact APK and native report. Consume46/code367 in
  the same repair commit. Never amend45 or relabel/re-sign it as46.
- Keep all45 user features, primary video quality and existing signing identity.
  No changes to Stack, Nodes, decoder/shader behavior, releases or unrelated work.

Evidence stays in `App/output/now-playing-controls-20261117/`. Current optimized
assembly, existing-maintenance identity, exact A55 acceptance and original-public
signing are distinct gates. No public publication or physical-phone claim.
