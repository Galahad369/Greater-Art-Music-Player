"""Create a bounded release dependency inventory using existing build outputs.

This is not a vulnerability scan or a claim of a complete standardized SBOM.
No dependency is downloaded or executed by this script.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def gradle_packages(report):
    packages = set()
    for line in report.splitlines():
        if re.search(r"\bFAILED\b", line):
            raise ValueError("The dependency report contains unresolved modules")
        match = re.search(r"([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([A-Za-z0-9_.+-]+)(?: -> (?:([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):)?([A-Za-z0-9_.+-]+))?", line)
        if match:
            packages.add((match[4] or match[1], match[5] or match[2], match[6] or match[3]))
    return [{"group": group, "name": name, "version": version} for group, name, version in sorted(packages)]


def windows_packages(lock, manifest):
    packages = set()
    for dependencies in lock["dependencies"].values():
        for name, data in dependencies.items():
            if data.get("resolved"):
                packages.add((name, data["resolved"], data.get("contentHash")))
    result = [{"ecosystem": "nuget", "name": name, "version": version, "contentHash": content_hash}
              for name, version, content_hash in sorted(packages)]
    result.append({"ecosystem": "vcpkg", "name": "ffmpeg", "registryBaseline": manifest["builtin-baseline"],
                   "features": manifest["dependencies"][0]["features"],
                   "limits": "Registry baseline identifies the approved port; see the bundled codec build receipt and corresponding source."})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kind", choices=("android", "windows"), required=True)
    parser.add_argument("--gradle-report", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.kind == "android":
        if not args.gradle_report:
            raise ValueError("Provide the same build's releaseRuntimeClasspath dependency report")
        packages = gradle_packages(args.gradle_report.read_text(encoding="utf-8"))
        evidence = hashlib.sha256(args.gradle_report.read_bytes()).hexdigest()
    else:
        lock_path = ROOT / "desktop/winui/packages.lock.json"
        manifest_path = ROOT / "desktop/native/vcpkg.json"
        packages = windows_packages(json.loads(lock_path.read_text()), json.loads(manifest_path.read_text()))
        evidence = {"nugetLockSha256": hashlib.sha256(lock_path.read_bytes()).hexdigest(),
                    "vcpkgManifestSha256": hashlib.sha256(manifest_path.read_bytes()).hexdigest()}
    if not packages:
        raise ValueError("No resolved dependencies were found; do not publish an empty inventory")
    inventory = {"schema": 1, "platform": args.kind, "packages": packages, "inputSha256": evidence,
        "limits": "Build dependency inventory, not a malware/CVE scan or complete standardized SBOM. Android reports can include dependency constraints."}
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(inventory, stream, indent=2)
        stream.write("\n")
    print(f"Dependency inventory: {len(packages)} records; no runtime networking added.")


if __name__ == "__main__":
    main()
