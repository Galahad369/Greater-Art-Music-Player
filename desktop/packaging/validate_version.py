"""Independent desktop version guard; no network, execution or Android mutation."""
import argparse
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[2]


def git(*arguments):
    return subprocess.check_output(["git", "-c", "core.fsmonitor=false", *arguments], cwd=ROOT, text=True, encoding="utf-8")


def identity(text):
    match = re.search(r"Current desktop: \*\*(\d+\.\d+\.\d+) \(code (\d+)\)\*\*", text)
    if not match:
        raise ValueError("Missing desktop version identity")
    code = int(match[2])
    if code < 1:
        raise ValueError("Desktop code must be positive")
    return match[1], code


def desktop_code(path):
    if path == ".github/workflows/windows-desktop-ci.yml":
        return True
    return path.startswith("desktop/") and pathlib.Path(path).suffix in {".kt", ".kts", ".properties", ".py", ".ps1", ".yml", ".yaml", ".xml", ".java", ".ico", ".svg", ".png"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base")
    parser.add_argument("--head", default="HEAD")
    options = parser.parse_args()
    rules = ROOT / "desktop/VERSION_DESKTOP.md"
    if not rules.exists():
        raise ValueError("Desktop implementation requires its version ledger")
    version, code = identity(rules.read_text(encoding="utf-8"))
    build = (ROOT / "desktop/app/build.gradle.kts").read_text(encoding="utf-8")
    if f'packageVersion = "{version}"' not in build:
        raise ValueError("Desktop package version differs from ledger")
    if options.base:
        for commit in git("rev-list", "--reverse", f"{options.base}..{options.head}").splitlines():
            parent = git("rev-parse", f"{commit}^").strip()
            changed = git("diff", "--name-only", parent, commit).splitlines()
            if not any(desktop_code(path) for path in changed):
                continue
            current = identity(git("show", f"{commit}:desktop/VERSION_DESKTOP.md"))
            build_at_commit = git("show", f"{commit}:desktop/app/build.gradle.kts")
            if f'packageVersion = "{current[0]}"' not in build_at_commit:
                raise ValueError(f"{commit[:12]} package identity differs from its ledger")
            # A pure integration adds no executable behavior of its own.
            parents = git("rev-list", "--parents", "-n", "1", commit).split()[1:]
            tree = git("rev-parse", f"{commit}^{{tree}}").strip()
            if len(parents) > 1 and any(tree == git("rev-parse", f"{p}^{{tree}}").strip() for p in parents):
                continue
            exists = git("ls-tree", "--name-only", parent, "desktop/VERSION_DESKTOP.md").strip()
            if not exists:
                if current != ("0.1.0", 1):
                    raise ValueError("First desktop implementation must start at 0.1.0/code1")
            else:
                old_version, old_code = identity(git("show", f"{parent}:desktop/VERSION_DESKTOP.md"))
                a, b, patch = map(int, old_version.split("."))
                if current != (f"{a}.{b}.{patch + 1}", old_code + 1):
                    raise ValueError(f"{commit[:12]} desktop code changed without PATCH/code +1")
    print(f"Desktop version guard OK: {version} (code {code})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
