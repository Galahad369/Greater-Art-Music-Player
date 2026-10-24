#!/usr/bin/env python3
"""Protected-main sync. No force pushes, resets, stash manipulation or release deletion."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = "Galahad369/Greater-Art-Music-Player"
RULES = "greater-art/VERSION_RULES.md"


def run(root, *args, input_text=None, allowed=(0,), timeout=120):
    env = dict(os.environ, GIT_TERMINAL_PROMPT="0", PYTHONUTF8="1")
    result = subprocess.run(args, cwd=root, env=env, input=input_text,
                            capture_output=True, text=True, encoding="utf-8", errors="replace",
                            timeout=timeout)
    if result.returncode not in allowed:
        raise RuntimeError(f"{args[0]} failed ({result.returncode}):\n{result.stderr or result.stdout}")
    return result.stdout.strip()


def git(root, *args, **kwargs):
    return run(root, "git", "-c", "core.fsmonitor=false", *args, **kwargs)


def latest_apk(root):
    text = (root / RULES).read_text(encoding="utf-8")
    match = re.search(r"Latest verified APK: \*\*(\d+\.\d+\.\d+) \(code \d+\)\*\*", text)
    if not match:
        raise RuntimeError("Latest verified APK is missing from VERSION_RULES.md.")
    return root / "greater-art" / "releases" / f"GreaterArt-{match[1]}.apk"


def require_clean(root):
    if git(root, "status", "--porcelain", "--untracked-files=all"):
        raise RuntimeError("Uncommitted/untracked files exist. Review and commit your intended work first. "
                           "Nothing was staged, discarded or stashed.")
    for marker in ("MERGE_HEAD", "rebase-merge", "rebase-apply", "CHERRY_PICK_HEAD"):
        path = Path(git(root, "rev-parse", "--git-path", marker))
        if (path if path.is_absolute() else root / path).exists():
            raise RuntimeError("A merge/rebase/cherry-pick is in progress. Finish or explicitly abort it first.")
    if not git(root, "branch", "--show-current"):
        raise RuntimeError("Detached HEAD: choose the intended branch before syncing.")


def deletion_only_cleanup(root):
    """Every unique commit must delete APKs only; net-diff checks alone are unsafe."""
    commits = git(root, "rev-list", "origin/main..HEAD").splitlines()
    if not commits:
        return False
    for commit in commits:
        if len(git(root, "rev-list", "--parents", "-n", "1", commit).split()) != 2:
            return False
        changes = git(root, "diff-tree", "--no-commit-id", "--name-status", "--no-renames", "-r", "-z", commit)
        fields = changes.rstrip("\0").split("\0")
        if not changes or len(fields) % 2:
            return False
        for status, path in zip(fields[::2], fields[1::2]):
            if status != "D" or not re.fullmatch(r"greater-art/releases/GreaterArt-\d+\.\d+\.\d+\.apk", path):
                return False
    return True


def latest_only(root):
    """Hide old tracked binaries without creating commits deleting them."""
    patterns = ["/*", "!/greater-art/releases/*.apk", "/" + latest_apk(root).relative_to(root).as_posix()]
    enabled = git(root, "config", "--bool", "--get", "core.sparseCheckout", allowed=(0, 1))
    if enabled == "true":
        path = Path(git(root, "rev-parse", "--git-path", "info/sparse-checkout"))
        old = (path if path.is_absolute() else root / path).read_text(encoding="utf-8").splitlines()
        if len(old) != 3 or old[:2] != patterns[:2] or not re.fullmatch(
                r"/greater-art/releases/GreaterArt-\d+\.\d+\.\d+\.apk", old[2]):
            raise RuntimeError("Custom sparse-checkout rules exist; refusing to overwrite them.")
    git(root, "sparse-checkout", "set", "--no-cone", "--stdin", input_text="\n".join(patterns) + "\n")


def repair_cleanup(root):
    require_clean(root)
    if git(root, "branch", "--show-current") != "main" or not deletion_only_cleanup(root):
        raise RuntimeError("Automatic cleanup repair requires main with ONLY committed APK deletions. "
                           "Mixed/source changes need review; nothing was discarded.")
    head = git(root, "rev-parse", "HEAD")
    recovery = f"refs/archive/apk-cleanup-{head}"
    git(root, "update-ref", recovery, head)
    if git(root, "rev-parse", recovery) != head:
        raise RuntimeError("Recovery verification failed; main was not moved.")
    git(root, "switch", "--detach", "origin/main")
    # The full original commit remains reachable under recovery, not orphaned.
    if git(root, "rev-parse", "main") != head:
        raise RuntimeError("Main changed concurrently; refusing branch-pointer repair.")
    git(root, "branch", "-D", "main")
    git(root, "switch", "-c", "main", "origin/main")
    print(f"APK cleanup preserved at {recovery}; main restored without rewriting any commit.")


def gh_command():
    found = shutil.which("gh")
    fallback = Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "GitHub CLI/gh.exe"
    if found:
        return found
    if fallback.is_file():
        return str(fallback)
    raise RuntimeError("GitHub CLI missing. Install GitHub CLI and run: gh auth login")


def sync(root, args):
    require_clean(root)
    remote = git(root, "remote", "get-url", "origin").removesuffix(".git").rstrip("/")
    if remote not in (f"https://github.com/{REPOSITORY}", f"git@github.com:{REPOSITORY}"):
        raise RuntimeError("origin is not the expected Greater Art repository; refusing network writes.")
    if not args.apply:
        print(git(root, "status", "--short", "--branch"))
        print("Read-only check; remote refs may be cached. Double-click Sync-GreaterArt.cmd to apply safely.")
        return
    gh = gh_command()
    run(root, gh, "auth", "status")
    git(root, "fetch", "origin", "--prune")
    if args.latest_apk_only:
        latest_only(root)
    if args.repair_apk_cleanup and git(root, "branch", "--show-current") == "main" and deletion_only_cleanup(root):
        repair_cleanup(root)
    ahead, behind = map(int, git(root, "rev-list", "--left-right", "--count", "HEAD...origin/main").split())
    if ahead and behind:
        raise RuntimeError("Local work and remote main diverged. No automatic rebase/conflict guesses. "
                           "Your work is intact; review the differing commits before integration.")
    if not ahead:
        git(root, "merge", "--ff-only", "origin/main")
        branch = git(root, "branch", "--show-current")
        if branch != "main":
            git(root, "merge-base", "--is-ancestor", "main", "origin/main")
            git(root, "switch", "main")
            git(root, "merge", "--ff-only", "origin/main")
            if branch.startswith("codex/sync-"):
                git(root, "branch", "-d", branch)
        if args.latest_apk_only:
            latest_only(root)
        print("Already synchronized; no push or PR needed.")
        return
    # Reject immutable-release deletions/version mistakes BEFORE publishing a branch.
    env_aapt = os.environ.get("AAPT")
    if not env_aapt and os.name == "nt":
        sdk = Path(os.environ.get("ANDROID_HOME") or Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk")
        tools = sorted((sdk / "build-tools").glob("*/aapt.exe"), reverse=True)
        if tools:
            os.environ["AAPT"] = str(tools[0])
    print(run(root, sys.executable, "scripts/validate-greater-art-version.py", "--base", "origin/main", "--head", "HEAD"))
    branch = git(root, "branch", "--show-current")
    if branch == "main":
        branch = "codex/sync-" + git(root, "rev-parse", "--short=12", "HEAD")
        git(root, "switch", "-c", branch)
    git(root, "push", "-u", "origin", branch, timeout=600)
    prs = json.loads(run(root, gh, "pr", "list", "--repo", REPOSITORY, "--head", branch, "--state", "open", "--json", "url"))
    url = prs[0]["url"] if prs else run(root, gh, "pr", "create", "--repo", REPOSITORY,
        "--base", "main", "--head", branch, "--title", f"Integrate {branch}",
        "--body", "Guarded local sync: version/immutable-release checks passed. Required CI remains mandatory. "
        "No force push, history rewrite, automatic staging or conflict resolution. Review commits and HANDOFF before acceptance.")
    print(f"PR: {url}", flush=True)
    run(root, gh, "pr", "merge", url, "--auto", "--merge", "--delete-branch")
    print("Waiting for required CI; do not close this window. Failed checks leave all work intact.", flush=True)
    print(run(root, gh, "pr", "checks", url, "--required", "--watch", "--interval", "30", timeout=1200))
    for _ in range(12):
        state = run(root, gh, "pr", "view", url, "--json", "state", "--jq", ".state")
        if state == "MERGED":
            break
        time.sleep(5)
    else:
        raise RuntimeError(f"Checks passed but PR is not merged yet. Review/approval may be needed: {url}")
    require_clean(root)
    git(root, "fetch", "origin", "--prune")
    git(root, "merge-base", "--is-ancestor", "main", "origin/main")
    git(root, "switch", "main")
    git(root, "merge", "--ff-only", "origin/main")
    if branch.startswith("codex/"):
        if branch in git(root, "for-each-ref", "--format=%(refname:short)", "refs/heads/").splitlines():
            git(root, "branch", "-d", branch)
    if args.latest_apk_only:
        latest_only(root)
    print("Merged; clean local main matches origin/main. No unrelated branches were touched.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Fetch, validate, PR/CI/merge and sync; otherwise inspect only")
    parser.add_argument("--latest-apk-only", action="store_true", help="Hide older APKs locally, never delete them from Git")
    parser.add_argument("--repair-apk-cleanup", action="store_true", help="Archive deletion-only main commits before restoring remote main")
    args = parser.parse_args()
    try:
        sync(ROOT, args)
        return 0
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print(f"STOPPED SAFELY: {error}", file=sys.stderr)
        return 1
    finally:
        try:
            print(f"Latest verified APK: {latest_apk(ROOT)}", flush=True)
        except (OSError, RuntimeError):
            pass


if __name__ == "__main__":
    sys.exit(main())
