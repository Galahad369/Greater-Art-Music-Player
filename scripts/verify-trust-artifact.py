"""Inspect exact artifact bytes. No execution, signing, key access, upload or publishing.

The source SHA is build-context information, NOT an independent proof that these
bytes came from that source. CI attestations provide that separate binding.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
COMPATIBILITY_CERT = "9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf"
ALLOWED_EXPORTED = {
    "com.local.listentomusic.LauncherActivity": ("activity", None),
    "com.local.listentomusic.playback.PlaybackService": ("service", None),
    "androidx.media3.session.BluetoothValidationActivity": ("activity", "android.permission.BLUETOOTH_PRIVILEGED"),
    "androidx.profileinstaller.ProfileInstallReceiver": ("receiver", "android.permission.DUMP"),
}


def command(arguments):
    result = subprocess.run([str(value) for value in arguments], cwd=ROOT, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=120, check=False)
    if result.returncode:
        # Tool output may contain local paths. Do not include it in public receipts.
        raise ValueError(f"{Path(arguments[0]).name} verification failed (exit {result.returncode})")
    return result.stdout + result.stderr


def android_policy(badging, permissions, xml, signature, channel, expected_version, expected_code, expected_cert):
    package = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging, re.M)
    if not package or package[1] != "com.local.listentomusic":
        raise ValueError("Unexpected APK package identity")
    if package[3] != expected_version or int(package[2]) != expected_code:
        raise ValueError("APK version differs from the requested release identity")
    declared = sorted(set(re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'", permissions)))
    if not declared:
        raise ValueError("APK permissions could not be inspected")
    if "android.permission.INTERNET" in declared:
        raise ValueError("Offline APK must not declare INTERNET")
    debug = "application-debuggable" in badging or bool(re.search(r"android:debuggable.*0xffffffff", xml))
    test_only = bool(re.search(r"android:testOnly.*0xffffffff", xml))
    if test_only or (debug and channel != "development"):
        raise ValueError("Distributable APK must not be test-only or debuggable")
    certificates = sorted(set(value.lower() for value in re.findall(r"certificate SHA-256 digest: ([0-9a-fA-F]{64})", signature)))
    if "Verifies" not in signature or not certificates:
        raise ValueError("APK signature was not verified")
    if expected_cert and certificates != [expected_cert.lower()]:
        raise ValueError("APK signing identity differs from the approved fingerprint")
    if channel != "development" and not expected_cert:
        raise ValueError("An owner-approved certificate fingerprint is required")
    broad = "android.permission.MANAGE_EXTERNAL_STORAGE" in declared
    if channel == "production" and (COMPATIBILITY_CERT in certificates or broad):
        raise ValueError("Production gate rejects the legacy sideload key and All Files Access")
    # Compose tooling/test hosts exist only in the debug variant. They must never
    # gain an exception in a distributable or non-debuggable artifact.
    allowed_exported = dict(ALLOWED_EXPORTED)
    if channel == "development" and debug:
        allowed_exported.update({
            "androidx.compose.ui.tooling.PreviewActivity": ("activity", None),
            "androidx.activity.ComponentActivity": ("activity", None),
        })
    # Inspect explicit exported components in the actual binary manifest, not source XML.
    current = None
    exported = []
    def finish_component():
        if not current or not current.get("exported"):
            return
        name = current["name"]
        if name and name.startswith("."):
            name = "com.local.listentomusic" + name
        if allowed_exported.get(name) != (current["kind"], current.get("permission")):
            raise ValueError("APK exports a component outside the reviewed name/type/permission allowlist")
        exported.append(name)

    for line in xml.splitlines():
        element = re.match(r"\s*E: (activity|activity-alias|service|receiver|provider)\b", line)
        if element:
            finish_component()
            current = {"kind": element[1], "name": None}
        elif re.match(r"\s*E:", line):
            # A nested intent-filter ends the component's own attribute list.
            finish_component()
            current = None
        elif current:
            name = re.search(r'android:name[^=]*="([^"]+)"', line)
            if name:
                current["name"] = name[1]
            permission = re.search(r'android:permission[^=]*="([^"]+)"', line)
            if permission:
                current["permission"] = permission[1]
            if re.search(r"android:exported.*0xffffffff", line):
                current["exported"] = True
    finish_component()
    return {"package": package[1], "version": package[3], "code": int(package[2]),
            "debuggable": debug, "testOnly": test_only, "permissions": declared,
            "broadStorageDeclared": broad, "certificateSha256": certificates,
            "exportedComponents": exported,
            "channelWarning": "Existing sideload identity; not production publisher signing" if COMPATIBILITY_CERT in certificates else None}


def windows_policy(metadata, channel, expected_version, expected_cert):
    if (metadata.get("version") or "").strip() not in (expected_version, expected_version + ".0"):
        raise ValueError("Windows artifact version differs from the requested identity")
    if channel != "development":
        if metadata.get("status") != "Valid" or not expected_cert:
            raise ValueError("Windows distribution requires a valid, owner-approved publisher signature")
        if metadata.get("certificateSha256") != expected_cert.lower():
            raise ValueError("Windows publisher fingerprint differs from the approved identity")
    return metadata


def fingerprint(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def winui_bundle(directory):
    # Include XBF/PRI/configuration, not just PE files: changed UI resources are
    # changed application bytes too. A junction must not expand this into other folders.
    files = []
    pending = [directory]
    while pending:
        for file in pending.pop().iterdir():
            attributes = file.stat(follow_symlinks=False)
            reparse = os.name == "nt" and bool(attributes.st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT)
            if file.is_symlink() or reparse:
                raise ValueError("WinUI verification requires a regular extracted bundle, without links or placeholders")
            if file.is_dir():
                pending.append(file)
            elif file.is_file():
                files.append(file)
    return {file.relative_to(directory).as_posix(): fingerprint(file) for file in sorted(files)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kind", choices=("android", "windows"), required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--channel", choices=("development", "sideload", "production"), default="development")
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--expected-code", type=int)
    parser.add_argument("--expected-certificate")
    parser.add_argument("--sdk", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.expected_certificate and not re.fullmatch(r"[0-9a-fA-F]{64}", args.expected_certificate):
        raise ValueError("Expected certificate must be a SHA-256 fingerprint")
    artifact = args.artifact.resolve(strict=True)
    if not artifact.is_file():
        raise ValueError("Artifact must be a regular file")
    before = fingerprint(artifact)
    commit = command(["git", "-c", "core.fsmonitor=false", "rev-parse", "HEAD"]).strip()
    dirty = bool(command(["git", "-c", "core.fsmonitor=false", "status", "--porcelain"]).strip())
    if args.channel == "production" and dirty:
        raise ValueError("Production acceptance requires a clean source checkout")
    if args.kind == "android":
        if args.expected_code is None:
            raise ValueError("Android acceptance requires an explicit version code")
        sdk = args.sdk or Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
        tools = [entry for entry in (sdk / "build-tools").iterdir() if (entry / "lib/apksigner.jar").is_file()]
        tools.sort(key=lambda entry: tuple(int(part) for part in re.findall(r"\d+", entry.name)))
        if not tools:
            raise ValueError("Install Android SDK build tools; no tool download is attempted")
        aapt = tools[-1] / ("aapt.exe" if os.name == "nt" else "aapt")
        java = shutil.which("java")
        if not java:
            raise ValueError("Java is required to verify APK signatures")
        identity = android_policy(command([aapt, "dump", "badging", artifact]),
            command([aapt, "dump", "permissions", artifact]),
            command([aapt, "dump", "xmltree", artifact, "AndroidManifest.xml"]),
            command([java, "-jar", tools[-1] / "lib/apksigner.jar", "verify", "--verbose", "--print-certs", artifact]),
            args.channel, args.expected_version, args.expected_code, args.expected_certificate)
        bundle = {artifact.name: before}
    else:
        powershell = shutil.which("pwsh")
        if os.name != "nt" or not powershell:
            raise ValueError("Windows and PowerShell are required for publisher verification")
        def inspect(file):
            return json.loads(command([powershell, "-NoProfile", "-NonInteractive", "-File",
                ROOT / "scripts/inspect-windows-signature.ps1", "-Artifact", file]))
        identity = windows_policy(inspect(artifact), args.channel, args.expected_version, args.expected_certificate)
        bundle = {artifact.name: before}
        if artifact.name == "GreaterArt.WinUI.exe":
            if args.output and args.output.resolve().is_relative_to(artifact.parent):
                raise ValueError("Keep trust receipts outside the verified WinUI bundle")
            bundle = winui_bundle(artifact.parent)
            for name in ("GreaterArt.WinUI.dll", "GreaterArt.Engine.dll"):
                file = artifact.parent / name
                if not file.is_file():
                    raise ValueError("The complete WinUI bundle is required")
                if args.channel != "development":
                    metadata = inspect(file)
                    if metadata.get("status") != "Valid" or metadata.get("certificateSha256") != args.expected_certificate.lower():
                        raise ValueError("An application DLL lacks the approved publisher signature")
    if before != fingerprint(artifact):
        raise ValueError("Artifact changed during verification")
    if any(fingerprint(artifact.parent / name) != digest for name, digest in bundle.items()):
        raise ValueError("A bundle member changed during verification")
    receipt = {"schema": 1, "checkedUtc": datetime.now(timezone.utc).isoformat(), "kind": args.kind,
        "channel": args.channel, "artifact": artifact.name, "bytes": artifact.stat().st_size, "sha256": before,
        "buildContext": {"commit": commit, "dirty": dirty, "binding": "Context only; verify source binding using CI attestation"},
        "identity": identity, "bundleSha256": bundle,
        "limits": ["Not a security certification", "Not proof of physical-device acceptance", "Checksums are not publisher signatures"]}
    encoded = json.dumps(receipt, indent=2, ensure_ascii=True) + "\n"
    if args.output:
        # Never overwrite an existing receipt, including a consumed version's evidence.
        with args.output.open("x", encoding="utf-8") as stream:
            stream.write(encoded)
    print(encoded)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        print(f"Trust gate failed: {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(1)
