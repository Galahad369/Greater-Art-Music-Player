# Greater Art — dev mode / Nodes / mini-window conventions

Applied changes verified this session (each required new version per VERSION_RULES.md).

## Nodes screen (`ui/NodesScreen.kt`)
- Background dimming alpha: `.62f` → `.35f` (default). Any future change requires version bump + AVD visual verification (user explicitly flags missed observations).
- `AppBackground.kt`: single effective dim layer; stays outside pager. No duplicate full-screen scrim added.

## Settings screen (`ui/SettingsScreen.kt`)
- Background dimming slider `%` value: `clickable` `Text` → opens `AlertDialog` with `OutlinedTextField`.
- Input constraints: digits only (`KeyboardType.Number`), range 25–85, rounded up (`ceil` then divide by 5, then multiply by 5), then divided by 100 for the slider value.
- Slider untouched (layout unchanged); only input mechanism added.

## Developer diagnostics (`ui/DeveloperDiagnostics.kt`)
- Dialog Surface modifier: `.fillMaxWidth().padding(16.dp).widthIn(max = 420.dp)` to cap width; import `androidx.compose.foundation.layout.widthIn` required (build fails without it).
- Tags added for Nodes screen controls (`Tags` for individual graph nodes by filename); `Nodes` back button moved clear of `DEV` badge.

## Now Playing screen (`ui/NowPlayingScreen.kt`)
- `.pointerInput(Unit)` changed to `.pointerInput(immersive)` so `detectTransformGestures` restarts when entering fullscreen. `Unit` breaks zoom in full screen.
- Pull handle (`PLAYER_PULL_HANDLE`) removed; `NowPlayingScreen` fills via `fillMaxSize().weight(1f)`; no layout redesign.

## Mini window overlay (`playback/MiniWindowOverlayService.kt`)
- Red X (`crossMargin`) moved 2px higher: `11` → `9` (`1.14.3`). Formula: `y = dp(crossMargin) + crossRaisePx`.
- Constants (top of file): `crossHitSize = 57`, `crossSize = 25`, `crossMargin = 9` (after fix), `crossBaseAlpha = 1f`, `crossRaisePx = 28`.
- Any future change to these constants requires version bump + AVD visual verification on `GreaterArt_A55_API36`.
