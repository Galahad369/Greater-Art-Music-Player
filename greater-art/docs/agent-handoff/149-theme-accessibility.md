# Issue #149 — Theme/accessibility backlog

This branch is the implementation workspace for GitHub issue #149.

## Features

### OLED / Void
- true-black background option
- retain surface hierarchy and readable controls
- no accidental all-black loss of semantic state
- verify Ambient/video transitions

### High Contrast
- stronger text/control/outline contrast
- preserve selected/muted/disabled distinctions
- verify accessibility contrast targets where applicable

### Artwork-adaptive accent-only
- derive accent from current artwork
- keep base surfaces neutral/stable
- use adaptive color only for accents, selection and progress
- avoid recoloring the entire interface

### Launch/theme transition
- eliminate light->dark launch flash
- align splash/window background with the selected theme before the first Compose frame

## Acceptance

- modes are independently selectable
- settings persist across restart
- playback/media behavior is unaffected
- visual states remain distinguishable
- add screenshot/visual regression support where practical

This bootstrap commit is documentation-only. Executable/theme-resource changes must follow VERSION_RULES.md.
