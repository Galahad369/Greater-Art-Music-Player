# ChatGPT → Hermes pull/build playbook for Greater Art

Use this when a ChatGPT coding session has created or merged GitHub branches/PRs and
Hermes must bring that work into the existing Windows checkout, build the APK, and
leave one coherent `main` branch. This is a Greater Art recovery procedure, not a
generic Git tutorial.

## Why the September 21 build stalled

The failure was not an Android/Kotlin compile error.

1. The restricted runner could not create Gradle's lock file under
   `%USERPROFILE%\.gradle\wrapper\dists\...gradle-9.7.1-bin.zip.lck`. The exact
   exception was `FileNotFoundException ... Access is denied`. Gradle needs access to
   its wrapper/cache directory, or `GRADLE_USER_HOME` must point to a writable ignored
   workspace directory.
2. `gh` was not installed, so a workflow that depended on `gh pr list`, `gh pr merge`,
   or `gh pr checks` could not run. Plain Git was available and was sufficient.
3. The Windows `py` launcher resolved to a blocked Microsoft Store Python alias, and
   plain `python` was not on PATH. A version check must prove the interpreter can run;
   finding a launcher name is not enough.
4. Remote branches looked divergent even when their patches were already on `main`.
   `git cherry origin/main <branch>` correctly showed `-` for patch-equivalent commits.
   Blindly retrying merges or waiting on PR tooling only burned time.
5. Source, docs and binaries had drifted: source contained post-1.12.4 changes while
   Gradle still said 1.12.4, root/app READMEs advertised older versions, and the
   1.12.4 APK predated the newer source. `assembleDebug` does not copy or rename an APK
   into `releases/` and does not update documentation.

The successful recovery used Git directly, JDK 21, the existing Android SDK and
Gradle cache with permission to write its lock files. It merged each unique branch
history once, built 1.12.5, copied a new non-overwriting artifact, then validated and
aligned all metadata.

## Non-negotiable invariants

- Work from the repository root, then build from `greater-art/`.
- Preserve uncommitted user work. Never use `git reset --hard`, `git clean -fd`, or
  checkout-based file destruction as a shortcut.
- Preserve package `com.local.listentomusic` and signing certificate SHA-256
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.
- Never overwrite an existing `releases/GreaterArt-<version>.apk`.
- Never add `INTERNET` permission.
- Never reduce source video resolution, bitrate or frame rate to hide jank.
- Do not claim real-device rendering success from JVM tests or APK inspection.

## 1. Establish the real state

Run from the repository root:

```powershell
git status --short --branch
git remote -v
git fetch --all --prune
git branch -vv
git branch -r
git log --graph --decorate --oneline --all -n 100
```

If the worktree is dirty, stop before switching/merging. Identify which changes belong
to the user and preserve them in a named commit or other explicit recoverable snapshot.
Do not silently stash and forget them.

Do not trust a handoff's claimed branch, PR or version until these commands confirm it.
The source of truth order is:

1. fetched commit graph, working tree and authoritative `greater-art/VERSION_RULES.md`;
2. exact source SHA and matching `greater-art/app/build.gradle.kts`;
3. independently verified APK metadata/signature/hash;
4. current handoff/README/HTML metadata (historical notes are not instructions).

## 2. Review branches without GitHub CLI

`gh` is optional. For each remote branch shown by `git branch -r`, run:

```powershell
git rev-list --left-right --count origin/main...origin/<branch>
git cherry origin/main origin/<branch>
git diff --stat origin/main...origin/<branch>
git log --oneline origin/main..origin/<branch>
```

Interpret `git cherry`:

- `+` means a patch is not on main yet;
- `-` means an equivalent patch is already on main, even if the commit SHA differs.

Review branch files before merging. Do not blindly merge stale documentation that
would roll versions backward. Merge the branch history once and resolve conflicts in
favor of verified current source/artifacts, while preserving genuinely new checks.

```powershell
git switch main
git pull --ff-only origin main
git merge --no-ff --no-edit origin/<reviewed-branch>
```

Repeat only for the finite, reviewed branch list. Re-run the graph after each merge.
If GitHub CLI is missing, do not wait for it: Git can merge and push the same commits.

## 3. Assign a new release version before building

Read VERSION_RULES before changing identity. Building an already-versioned SOURCE_ONLY
commit does **not** consume another version. Any new executable repair must consume the
next PATCH/code in the same commit, then be built from that exact SHA. Confirm the
target filename does not exist:

```powershell
Test-Path greater-art\releases\GreaterArt-<new-version>.apk
```

Expected result: `False`. If `True`, inspect provenance and verify the existing artifact.
Do not invent a new source version just to bypass a bad filename. Preserve invalid
placeholders outside releases; never overwrite an actual historical release.

## 4. Build the optimized APK with the pinned toolchain

Run the [Android release gate](ANDROID_RELEASE_GATE.md) from the repository root. It runs preflight and `:app:assembleRelease`. Check its separate assembly, signing, identity and device results. Do not close an Android app change with a host-only check or an `assembleDebug` APK.

```powershell
python -B scripts/greater-art-android-gate.py build --out ../output/android-release-1.22.15-new
```

Use `--java-home` and `--sdk` for existing trusted installs. A cache, network or toolchain blocker is not a source failure. If assembly succeeds but signing is blocked, retain the unsigned result as build evidence, not as a distributable APK.

### October 5 artifact-corruption lesson

The correct 1.15.75 APK installed and passed device tests, but a later operation
replaced output and a release placeholder with nine bytes (`BUILT -`). A new
untracked batch helper also appeared. Manifest/signature checks caught the corruption;
no invalid release was published. The intact installed APK was recovered only because
the exact build SHA, install time/version and signing provenance were known, then all
binary checks were repeated. Do not use this as a generic substitute for rebuilding.

- Coordinate **one active writer** in a checkout; pause other builders before packaging.
- Never redirect `echo`, shell transcripts, curl errors or status text into an APK path.
- Check `$LASTEXITCODE` immediately after each build/verification command; later successful
  commands must not hide an earlier failure.
- Do not trust `BUILT`, filename, file timestamp, Git tag or app version alone. Verify
  the ZIP, manifest, pinned signature, alignment, byte size and SHA-256.
- Re-hash the candidate just before copy/upload and confirm the destination hash matches.
- Preserve an unexpected file instead of deleting it or committing unrelated helpers.
- If source has changed since the tested build, build/test again; never copy an older
  installed binary into the new version name.

## 5. Validate the exact APK before delivery

Use the gate's signed candidate and `build.json` rather than copying from a Gradle output directory. The gate checks package, version/code, compatibility signer, manifest, ZIP and alignment. Run the native device smoke on the same bytes and keep `device.json`; do not rerun the same checks merely to rename the file. Run the relevant source tests and keep their results distinct from device acceptance. Copy only verified bytes to a new destination when authorized. Never overwrite or publish as part of the build command.

## 6. Align docs and run the consistency gate

After the artifact exists, update these together:

- root `README.md` version and exact APK link;
- `greater-art/README.md` version/code/path/hash and verification evidence;
- `greater-art/HANDOFF.md` current state, cause, prevention and device-test boundary;
- root `index.html` version and download link;
- any current demo that states the release version.

Then run from the repository root:

```powershell
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { throw 'A working Python 3 runtime is required for the version gate.' }
& $python.Source --version
if ($LASTEXITCODE -ne 0) { throw 'The resolved Python launcher cannot execute; do not use a blocked Windows Store alias.' }
& $python.Source scripts\validate-greater-art-version.py
pwsh -NoProfile -File scripts\audit-public-repo.ps1
git diff --check
```

The validator is deliberately strict. Fix the metadata; do not weaken the gate to
make stale documentation pass.

## 7. Commit, push and delete merged branches safely

Commit the release only after every local gate passes, then push `main`:

```powershell
git status --short
git add -- <explicit reviewed paths>
git commit -m "Release Greater Art <new-version>"
git push origin main
```

Before deleting each branch, prove its tip is contained in main:

```powershell
git fetch origin --prune
git merge-base --is-ancestor origin/<branch> origin/main
```

Exit code 0 means deletion is safe. Delete the exact branch—never a wildcard:

```powershell
git push origin --delete <branch>
git fetch origin --prune
```

Delete any matching local branch only after switching to main and confirming it is
merged. Finish with:

```powershell
git branch -a
git status --short --branch
git log --graph --decorate --oneline -n 30
```

Expected end state: clean `main`, `origin/main`, no other feature/fix remote branches,
one new immutable APK, and version metadata that passes the consistency script.

## Prompt to give Hermes

```text
Continue in the existing Greater-Art-Music-Player checkout. Read
greater-art/docs/CHATGPT_TO_HERMES_BUILD_PLAYBOOK.md completely and follow it as an
executable recovery procedure. Inspect the fetched graph and dirty state first. Use
plain Git if gh is unavailable. Preserve all user work and the pinned signing identity.
Merge each reviewed unique remote branch exactly once into main, build with JDK 21,
fix Gradle cache permissions if its .lck file is denied, create a new versioned APK
without overwriting any release, validate package/version/no-INTERNET/signature/16-KiB
alignment/tests/lint, align docs only after the artifact exists, push main, prove each
branch is an ancestor, then delete all non-main branches. Report the exact APK path,
SHA-256, remaining branches and any real-device checks that were not possible.
```
