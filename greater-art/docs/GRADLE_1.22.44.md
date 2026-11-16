# Gradle 9.9.1 review — Android 1.22.44 / code 365

## Cause and approved repair

The owner requested review, repairs, conditional merge and branch cleanup for
PR242, while preserving the exact tested43 APK and all unrelated work. Dependabot
rebased its head to `5b87a1a947e6b35100d3c3e25f4139b9ef4fe741` on current main.
Five required checks pass, but the per-commit guard correctly rejects a wrapper
change at unchanged1.22.43/code364. Appending a bump does not fix that old commit.

Carry its two exact wrapper files into a replacement commit from main, with
1.22.44/code365 in that same commit. Retain the original head in a private archive
ref. Do not force-push, weaken the guard or discard the bot's useful change.
Close the original PR and remove its branch only after the replacement lands.
Keep one checkout, no extra worktree, and the original Windows Trust stash and
three unrelated local files intact.

## Authenticity and compatibility

Official Gradle endpoints independently confirm:

- `gradle-9.9.1-wrapper.jar`: `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`.
- `gradle-9.9.1-bin.zip`: `d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228`.

The JAR is byte-identical to PR242 and matches the vendor checksum before any
execution. Keep HTTPS, checksum enforcement, distribution URL validation and the
existing timeout. No new dependency, plugin, repository, signing key or app source.

[Gradle release notes](https://docs.gradle.org/9.9.1/release-notes.html) identify
three security repairs and recommend this patch. The project's existing JDK21
is supported. The published [compatibility matrix](https://docs.gradle.org/9.9.1/userguide/compatibility.html)
lists Android plugin tests only through9.5.0-alpha10; it does not by itself prove
compatibility with this project's9.5.1. The original PR's Android build passed;
verify the exact replacement locally and in all protected CI checks rather than
equating the table or a dependency-bot label with runtime acceptance.

## Verification and preservation

Evidence: `App/output/pr242-gradle-1.22.44-20261116/`.
Preflight requires existing JDK21, SDK37, maintenance signer, free disk and a
healthy owned A55. Estimated additional peak working space is2GiB; no cleanup or
toolchain/profile reset is authorized. Keep Windows TLS validation enabled.
Run629 JVM tests, lint, diagnostic assembly and optimized `assembleRelease`.
An atomic versioned replacement must pass the existing per-commit validator.
Do not rerun unchanged picture/music experiments just for a wrapper update.
Run normal-entry playback/fullscreen/return on the exact new optimized APK;
original-public signing remains a separate blocked gate.

Record actual host/build/device results in the associated replacement PR and exact
local receipts before landing; this design record alone does not claim they pass.
Preserve the exact43 APK at
`App/output/picture-reliability-20261116/optimized43-maintenance/GreaterArt-1.22.43.apk`:
SHA-256 `f453f0bedf9ead91e891d363c8539f23af7395b0196184520e9046e74f867f77`.
It stays at its existing path and version. Never overwrite it, replace its key,
or describe a rebuilt44 APK as the same tested artifact. No public publication.

## Prevention

Check per-commit version policy before pushing bot repairs. A no-bump original
commit needs an atomic versioned replacement, not a cosmetic follow-up bump.
Verify official wrapper hashes, preserve the old exact APK and source evidence,
and use a real merge record plus ancestry/patch equivalence before deleting a
closed branch. Closed is not equivalent to merged or disposable. Required-check
results, exact binary identity and device acceptance are separate facts.
