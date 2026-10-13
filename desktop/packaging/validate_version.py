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
    return path.startswith("desktop/") and pathlib.Path(path).suffix in {".kt", ".kts", ".properties", ".py", ".ps1", ".yml", ".yaml", ".xml", ".java", ".ico", ".svg", ".png", ".cpp", ".h", ".inl", ".rc", ".manifest", ".wxs"}


def check_package(version, code, reader, native):
    if not native:
        # Preserve validation of the historical 0.1.0 Compose checkpoint.
        if f'packageVersion = "{version}"' not in reader("desktop/app/build.gradle.kts"):
            raise ValueError("Legacy desktop package differs from ledger")
        return
    core = reader("desktop/native/Core.h")
    if f'VERSION[] = L"{version}"' not in core or not re.search(rf"VERSION_CODE\s*=\s*{code}\s*;", core):
        raise ValueError("Native desktop identity differs from ledger")
    numeric = ",".join(version.split(".")) + ",0"
    for resource in ("App.rc", "Setup.rc"):
        text = reader("desktop/native/" + resource)
        for field in ("FILEVERSION", "PRODUCTVERSION"):
            if not re.search(rf"{field}\s+{re.escape(numeric)}\b", text):
                raise ValueError(f"{resource} resource identity differs from ledger")
    if f'Version="{version}"' not in reader("desktop/native/Product.wxs"):
        raise ValueError("Installer identity differs from ledger")
    packaging = reader("desktop/native/package.ps1")
    if f"Setup-{version}.exe" not in packaging or f"version='{version}';code={code};" not in packaging:
        raise ValueError("Installer artifact identity differs from ledger")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base")
    parser.add_argument("--head", default="HEAD")
    options = parser.parse_args()
    rules = ROOT / "desktop/VERSION_DESKTOP.md"
    if not rules.exists():
        raise ValueError("Desktop implementation requires its version ledger")
    version, code = identity(rules.read_text(encoding="utf-8"))
    check_package(version, code, lambda path: (ROOT / path).read_text(encoding="utf-8"),
                  (ROOT / "desktop/native/Core.h").exists())
    if options.base:
        for commit in git("rev-list", "--reverse", f"{options.base}..{options.head}").splitlines():
            parent = git("rev-parse", f"{commit}^").strip()
            changed = git("diff", "--name-only", parent, commit).splitlines()
            if not any(desktop_code(path) for path in changed):
                continue
            current = identity(git("show", f"{commit}:desktop/VERSION_DESKTOP.md"))
            check_package(*current, lambda path: git("show", f"{commit}:{path}"),
                          bool(git("ls-tree", "--name-only", commit, "desktop/native/Core.h").strip()))
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
