"""Independent desktop version guard; no network, execution or Android mutation."""
import argparse
import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[2]
PRE_LEDGER_VENDOR_COMMITS = {
    "517e260955169c415828e1819e5eacbc8919294a",
    "86baa3d53a2ccfe6ce84a50eadcdb4b2f4ba0010",
}


def historical_vendor_only(commit, changed):
    # These two immutable user commits predate the desktop policy. They added
    # only obsolete vendor bundles/bytecode, not an application implementation.
    prefixes = ("desktop/app/runtime/", "desktop/app/native-runtime/",
                "desktop/packaging/__pycache__/", "scripts/__pycache__/")
    return commit in PRE_LEDGER_VENDOR_COMMITS and all(path.startswith(prefixes) for path in changed)


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
    if path == "desktop/native/.clang-format":
        return True
    return path.startswith("desktop/") and pathlib.Path(path).suffix in {".kt", ".kts", ".properties", ".py", ".ps1", ".yml", ".yaml", ".xml", ".java", ".ico", ".svg", ".png", ".cpp", ".h", ".inl", ".rc", ".manifest", ".wxs", ".json", ".cmake"}


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
    if tuple(map(int, version.split('.'))) >= (0, 1, 2):
        manifest = json.loads(reader("desktop/native/vcpkg.json"))
        if manifest.get("version-string") != version:
            raise ValueError("Codec manifest identity differs from ledger")
        dependency = manifest["dependencies"][0]
        if dependency.get("default-features") is not False or set(dependency["features"]) != {"avcodec", "avformat", "avfilter", "swresample", "swscale"}:
            raise ValueError("Codec feature/license policy changed")
        if not re.fullmatch(r'[0-9a-f]{40}', manifest.get("builtin-baseline", "")):
            raise ValueError("Codec source baseline must be pinned")


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
            if historical_vendor_only(commit, changed):
                continue
            current = identity(git("show", f"{commit}:desktop/VERSION_DESKTOP.md"))
            check_package(*current, lambda path: git("show", f"{commit}:{path}"),
                          bool(git("ls-tree", "--name-only", commit, "desktop/native/Core.h").strip()))
            # A pure integration adds no executable behavior of its own.
            parents = git("rev-list", "--parents", "-n", "1", commit).split()[1:]
            tree = git("rev-parse", f"{commit}^{{tree}}").strip()
            if len(parents) > 1 and any(tree == git("rev-parse", f"{p}^{{tree}}").strip() for p in parents):
                continue
            # A merge may combine unrelated files, so its entire tree need not
            # equal either parent. Accept only executable blobs inherited intact
            # from a parent with the same validated desktop identity.
            if len(parents) > 1:
                inherited = []
                for p in parents:
                    if git("ls-tree", "--name-only", p, "desktop/VERSION_DESKTOP.md").strip():
                        if identity(git("show", f"{p}:desktop/VERSION_DESKTOP.md")) == current:
                            inherited.append(p)
                paths = [path for path in changed if desktop_code(path)]
                if inherited and all(any(git("ls-tree", commit, path).split("\t")[0] ==
                                             git("ls-tree", p, path).split("\t")[0]
                                         for p in inherited) for path in paths):
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
