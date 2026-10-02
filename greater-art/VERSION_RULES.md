VERSION_RULES.md — Greater Art APK (HARDENED)

VERSIONED CHANGE RULE (ABSOLUTE): Any change that affects executable app behavior, build behavior, release behavior, resources, manifest state, or executable tooling requires the next PATCH and versionCode +=1 in the same commit, unless this file explicitly lists an exact series or consumed-branch transition. Documentation-only and release-finalization exceptions are limited to rule 8 below. No silent builds and no reused version numbers.

This file is the authoritative version policy for Greater Art. If any handoff note, README, old commit message, or conversation conflicts with this file, this file wins.

## Machine-checkable state


Current source: **1.15.39 (code 169)**
Current release state: **SOURCE_ONLY**
Latest verified APK: **1.15.38 (code 168)**
Allowed series transition: **1.14.15 -> 1.15.1**
Allowed consumed transition: **1.15.13 (code 143) -> 1.15.17 (code 147)**
Allowed consumed transition: **1.15.17 (code 147) -> 1.15.19 (code 149)**
Allowed consumed transition: **1.15.19 (code 149) -> 1.15.21 (code 151)**
Allowed consumed transition: **1.15.21 (code 151) -> 1.15.22 (code 152)**
Allowed consumed transition: **1.15.22 (code 152) -> 1.15.23 (code 153)**
Allowed consumed transition: **1.15.23 (code 153) -> 1.15.24 (code 154)**
Allowed consumed transition: **1.15.24 (code 154) -> 1.15.25 (code 155)**
Allowed consumed transition: **1.15.25 (code 155) -> 1.15.26 (code 156)**
Allowed consumed transition: **1.15.26 (code 156) -> 1.15.27 (code 157)**
Allowed consumed transition: **1.15.27 (code 157) -> 1.15.29 (code 159)**
Allowed consumed transition: **1.15.31 (code 161) -> 1.15.34 (code 164)**
Allowed consumed transition: **1.15.34 (code 164) -> 1.15.36 (code 166)**

## Non-negotiable rules

1. **Every versioned code commit consumes a new PATCH version.**
   - `versionName`: `X.Y.Z -> X.Y.(Z+1)`
   - `versionCode`: `N -> N+1`
   - The version bump must be in the **same commit** as the code change.
2. **Never reuse a consumed version.** Once a code commit lands with a version, that version is spent even if the build, emulator, signing, or release step later fails.
3. **No silent fixes under the same version.** If a build/test exposes another code bug, the fix is another code commit and therefore the next PATCH/code.
4. **Never skip a PATCH/code inside the active series unless this file explicitly authorizes the exact transition.** Series changes use `Allowed series transition`. A consumed-branch recovery may use `Allowed consumed transition` only when every skipped version/code was already committed on a side branch and remains recorded in the ledger.
5. **Release APKs are immutable.** Never modify, overwrite, rename, or copy an older APK into a new version filename.
5b. **Release APK filename MUST match the exact versionName and versionCode.** The artifact in `greater-art/releases/` must be named `GreaterArt-<versionName>.apk`.
6. **A new APK must be built from the exact source commit carrying that version.** Renaming an existing APK is not a build.
7. **Source and release are two separate states.**
   - `SOURCE_ONLY`: code/version has landed, but no verified APK for that version may exist in `greater-art/releases/`.
   - `VERIFIED`: the exact current-version APK has been built locally, its manifest/version checked, smoke-tested, hashed, and recorded.
8. **Documentation-only or release-finalization commits do not bump the app version** provided they contain no versioned code changes.
9. **Do not overwrite history to repair a version mistake.** Consume the next PATCH and record what happened.

## What counts as versioned code

A commit requires a PATCH/code bump when it changes executable/build/release behavior, including:

- `greater-art/app/src/**` Kotlin/Java/XML/native source
- Gradle/settings/build logic outside the versionName/versionCode lines themselves
- `greater-art/gradle/**` build logic
- executable tooling under `scripts/**`
- CI/release/version workflows under `.github/workflows/**`

Markdown/docs-only changes, release hashes/metadata, and adding a newly verified immutable APK do not themselves require another bump.

## Automated guard

`.github/workflows/version-consistency.yml` runs `scripts/validate-greater-art-version.py`.

## Versions tracked

- 1.15.29 (code 159) — canonical list-scroll video budget: pause wallpaper during fling; source only
- 1.15.38 (code 168) — salvage Stack crash hardening; local build verified
- 1.15.39 (code 169) — **Grok**: YouTube-style list-fling video budget — detach wallpaper PlayerView surface (keep ExoPlayer), static base during scroll, 80ms settle; source only, local build pending
- Consumed side-branch identity: **1.15.35 (code 165)** — redundant forward-port PR #82; closed without merge
- Consumed side-branch identity: **1.15.33 (code 163)** — rejected PLACEHOLDER coordinator; never reuse
- Consumed side-branch identity: **1.15.32 (code 162)** — redundant forward-port PR #79; closed without merge

Previous baseline: 1.13.26 (code 115).

**No silent builds. No version reuse. No version skips. No APK overwrite/rename/copy.**
