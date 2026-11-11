"""Build and qualify a local optimized Greater Art APK. Never publish it."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile


ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "greater-art"
PACKAGE = "com.local.listentomusic"
CERTIFICATE = "9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf"
INPUTS = ("greater-art/app", "greater-art/gradle", "greater-art/build.gradle.kts",
          "greater-art/settings.gradle.kts", "greater-art/gradle.properties", "greater-art/gradlew")
SIGNING_ENV = ("ANDROID_RELEASE_KEYSTORE", "ANDROID_RELEASE_STORE_PASSWORD",
               "ANDROID_RELEASE_KEY_ALIAS", "ANDROID_RELEASE_KEY_PASSWORD")
EXE = ".exe" if os.name == "nt" else ""


def sha256(file: Path) -> str:
    with file.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def git(*args: str) -> str:
    return subprocess.check_output(["git", "-c", "core.fsmonitor=false", *args], cwd=ROOT,
                                   encoding="utf-8", env={**os.environ, "GIT_OPTIONAL_LOCKS": "0"})


def source() -> tuple[dict, dict[str, str | None]]:
    paths = set(git("ls-files", "--cached", "--others", "--exclude-standard", "-z", "--", *INPUTS).split("\0")) - {""}
    files = {name: sha256(ROOT / name) if (ROOT / name).is_file() else None for name in sorted(paths)}
    gradle = (PROJECT / "app/build.gradle.kts").read_text(encoding="utf-8")
    return {
        "commit": git("rev-parse", "HEAD").strip(),
        "committed": not git("status", "--porcelain", "--untracked-files=all", "--", *INPUTS).strip(),
        "sha256": hashlib.sha256(json.dumps(files, sort_keys=True).encode()).hexdigest(),
        "version": re.search(r'^\s*versionName\s*=\s*"(\d+\.\d+\.\d+)"', gradle, re.M)[1],
        "code": int(re.search(r"^\s*versionCode\s*=\s*(\d+)", gradle, re.M)[1]),
    }, files


def preflight(args):
    compile_sdk = re.search(r"^\s*compileSdk\s*=\s*(\d+)",
                            (PROJECT / "app/build.gradle.kts").read_text(encoding="utf-8"), re.M)[1]
    java_major = re.search(r"^toolchainVersion=(\d+)",
                           (PROJECT / "gradle/gradle-daemon-jvm.properties").read_text(encoding="utf-8"), re.M)[1]
    sdk_paths = [args.sdk] if args.sdk else [os.getenv("ANDROID_HOME"), os.getenv("ANDROID_SDK_ROOT")]
    if not args.sdk:
        properties = PROJECT / "local.properties"
        if properties.is_file():
            match = re.search(r"^sdk\.dir=(.*)$", properties.read_text(encoding="utf-8"), re.M)
            if match:
                sdk_paths.insert(0, match[1].strip().replace(r"\:", ":").replace("\\\\", "\\"))
        sdk_paths.append(Path(os.getenv("LOCALAPPDATA", Path.home())) / "Android/Sdk")
    sdk = next((path for entry in sdk_paths if entry for path in [Path(entry).resolve()]
                if any((path / f"platforms/android-{api}/android.jar").is_file()
                       for api in (compile_sdk, f"{compile_sdk}.0"))), None)
    # Resolve a JDK, not the first java.exe on PATH. Daemon criteria are pinned.
    jdk_paths = [args.java_home] if args.java_home else [os.getenv("JAVA_HOME"),
        *sorted((Path(os.getenv("ProgramFiles", "C:/Program Files")) / "Android/openjdk").glob("jdk-*"), reverse=True),
        *sorted((Path.home() / ".jdks").glob("*"), reverse=True)]
    java = next((path for entry in jdk_paths if entry for path in [Path(entry).resolve()]
                 if (path / f"bin/java{EXE}").is_file() and (path / "release").is_file()
                 and re.search(rf'^JAVA_VERSION="{java_major}(?:[.+\-"]|$)',
                               (path / "release").read_text(encoding="utf-8"), re.M)), None)
    keystore = (args.keystore or Path.home() / ".android/debug.keystore").resolve()
    blockers = []
    if java is None:
        blockers.append(f"JDK {java_major} unavailable; provide --java-home")
    if sdk is None:
        blockers.append(f"Android API {compile_sdk} unavailable; provide --sdk")
    if shutil.disk_usage(args.out.parent).free < 8 * 1024**3:
        blockers.append("Less than 8 GiB free on the build destination")
    return java, sdk, keystore, blockers


def record(path: Path, value: dict) -> None:
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2)
        stream.write("\n")


def run(argv, *, cwd=ROOT, env=None, log=None, timeout=120) -> str:
    result = subprocess.run(argv, cwd=cwd, env=env, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT if log else subprocess.PIPE, timeout=timeout)
    if log is not None:
        with log.open("xb") as stream:
            stream.write(result.stdout)
    if result.returncode:
        raise ValueError(f"{Path(argv[0]).name} exited {result.returncode}" + (f"; see {log.name}" if log else ""))
    return result.stdout.decode("utf-8", errors="replace")


def stage(status, reason=None):
    return {"status": status, **({"reason": reason} if reason else {})}


def build(args):
    out = args.out.resolve()
    if out.is_relative_to(ROOT):
        raise ValueError("Keep build evidence outside the app checkout")
    args.out = out
    java, sdk, keystore, blockers = preflight(args)
    revision, files = source()
    out.mkdir(exist_ok=False)
    result = {"schema": 1, "repo": str(ROOT), "source": revision,
              "utc": datetime.now(timezone.utc).isoformat(), "variant": "release", "stages": {}}
    steps = result["stages"]
    steps["preflight"] = stage("BLOCKED", "; ".join(blockers)) if blockers else stage("PASS")
    steps["assembly"] = stage("BLOCKED", "Build prerequisites unavailable") if blockers else stage("NOT_RUN")
    steps["signing"] = stage("BLOCKED", "Original sideload keystore unavailable") if not keystore.is_file() else stage("NOT_RUN")
    steps["identity"] = stage("NOT_RUN")
    steps["device_smoke"] = stage("BLOCKED", "No smoke test of the exact optimized APK yet")

    if not blockers and not args.preflight_only:
        current_stage = "assembly"
        try:
            isolated = out / "source"
            for name, digest in files.items():
                if digest is None:
                    continue
                src, dst = ROOT / name, isolated / name
                if not src.resolve().is_relative_to(ROOT) or src.is_symlink():
                    raise ValueError("Build input is linked outside the checkout")
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(src, dst)
                if sha256(dst) != digest:
                    raise ValueError("A build input changed while taking the source snapshot")
            env = {key: value for key, value in os.environ.items() if key not in SIGNING_ENV}
            env.update(JAVA_HOME=str(java), ANDROID_HOME=str(sdk), ANDROID_SDK_ROOT=str(sdk),
                       PATH=str(java / "bin") + os.pathsep + os.environ.get("PATH", ""))
            if args.windows_trusted_roots:
                options = env.get("JAVA_TOOL_OPTIONS", "")
                if os.name != "nt" or "javax.net.ssl.trustStore" in options:
                    raise ValueError("Resolve the existing Java trust-store configuration first")
                env["JAVA_TOOL_OPTIONS"] = options + " -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE"
            exe = java / f"bin/java{EXE}"
            project = isolated / "greater-art"
            # Assemble first and unsigned. Signing prerequisites cannot prevent an optimized build.
            run([exe, "-classpath", "gradle/wrapper/gradle-wrapper.jar", "org.gradle.wrapper.GradleWrapperMain",
                 ":app:assembleRelease", "--no-daemon", "--max-workers=1", "--no-parallel", "--console=plain"],
                cwd=project, env=env, log=out / "assembleRelease.log", timeout=1800)
            unsigned = project / "app/build/outputs/apk/release/app-release-unsigned.apk"
            if not unsigned.is_file() or not (project / "app/build/outputs/mapping/release/mapping.txt").is_file():
                raise ValueError("Gradle did not produce the expected unsigned optimized APK and R8 map")
            result["unsigned_apk"] = str(unsigned)
            steps["assembly"] = stage("PASS")
            tools_root = sdk / "build-tools"
            tools = sorted((p for p in tools_root.glob("*") if (p / "lib/apksigner.jar").is_file()),
                           key=lambda p: tuple(map(int, re.findall(r"\d+", p.name))))
            if keystore.is_file() and not tools:
                steps["signing"] = stage("BLOCKED", "Android build-tools are unavailable for alignment and signing")
            elif keystore.is_file():
                current_stage = "signing"
                # The established compatibility keystore uses Android's standard debug alias/password.
                # Keep that credential in the signer process; do not persist it or replace the key.
                env["GA_SIDELOAD_KEY_PASS"] = "android"
                tool = tools[-1]
                align = tool / f"zipalign{EXE}"
                aligned = out / "aligned-unsigned.apk"
                run([align, "-P", "16", "4", unsigned, aligned], log=out / "align.log")
                pending = out / "candidate"
                pending.mkdir()
                pending_apk = pending / "unverified-sideload.apk"
                run([exe, "-jar", tool / "lib/apksigner.jar", "sign", "--ks", keystore,
                     "--ks-key-alias", "androiddebugkey", "--ks-pass", "env:GA_SIDELOAD_KEY_PASS",
                     "--key-pass", "env:GA_SIDELOAD_KEY_PASS", "--out", pending_apk, aligned],
                    env=env, log=out / "sign.log")
                steps["signing"] = stage("PASS")
                current_stage = "identity"
                identity = json.loads(run([sys.executable, "-B", ROOT / "scripts/verify-trust-artifact.py",
                    "--kind", "android", "--channel", "sideload", "--artifact", pending_apk,
                    "--expected-version", revision["version"], "--expected-code", str(revision["code"]),
                    "--expected-certificate", CERTIFICATE, "--sdk", sdk], env=env, log=out / "identity.log"))
                run([align, "-c", "-P", "16", "-v", "4", pending_apk], log=out / "alignment.log")
                with zipfile.ZipFile(pending_apk) as archive:
                    if archive.testzip() is not None:
                        raise ValueError("APK ZIP integrity check failed")
                apk = pending / f"GreaterArt-{revision['version']}.apk"
                if os.name == "nt":
                    pending_apk.rename(apk)  # Non-replacing on Windows.
                else:
                    os.link(pending_apk, apk)
                    pending_apk.unlink()
                result["apk"] = {"path": str(apk), "sha256": identity["sha256"]}
                steps["identity"] = stage("PASS")
        except (OSError, ValueError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
            steps[current_stage] = stage("FAIL", str(error))

    record(out / "build.json", result)
    return result


def exact_candidate(receipt: Path, *, completed: bool):
    result = json.loads(receipt.read_text(encoding="utf-8"))
    if result.get("repo") != str(ROOT) or result.get("variant") != "release":
        raise ValueError("Use the optimized build receipt for this checkout")
    required = ("assembly", "signing", "identity", "device_smoke") if completed else ("assembly", "signing", "identity")
    if any(result["stages"][name]["status"] != "PASS" for name in required):
        raise ValueError("Optimized build, pinned signature, exact APK check and device smoke must pass before completion")
    revision, _ = source()
    if not revision["committed"] or revision != result["source"]:
        raise ValueError("The candidate must match the exact current versioned source commit")
    apk = Path(result["apk"]["path"]).resolve(strict=True)
    if not apk.is_relative_to(receipt.parent.resolve()) or sha256(apk) != result["apk"]["sha256"]:
        raise ValueError("Candidate APK no longer matches its build receipt")
    return result, apk


def smoke(args):
    receipt = args.receipt.resolve(strict=True)
    result, apk = exact_candidate(receipt, completed=False)
    output = receipt.with_name("device.json")
    if output.exists() or output.with_suffix(".log").exists():
        nonce = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S-%f")
        output = receipt.with_name(f"device-{nonce}.json")
    log = output.with_suffix(".log")
    started = False
    try:
        if not args.adapter:
            raise ValueError("Supply a reviewed device smoke command")
        env = {**os.environ, "GA_ADB": str(args.adb.resolve(strict=True)), "GA_SERIAL": args.serial,
               "GA_APK": str(apk), "GA_APK_SHA256": result["apk"]["sha256"],
               "GA_APK_VERSION": result["source"]["version"], "GA_APK_CODE": str(result["source"]["code"]),
               "GA_SMOKE_DIR": str(receipt.parent)}
        adb = [env["GA_ADB"], "-s", args.serial]
        if not re.fullmatch(r"emulator-\d+", args.serial) or run(adb + ["emu", "avd", "name"]).splitlines()[0] != "GreaterArt_A55_API36":
            raise ValueError("Choose the owned GreaterArt_A55_API36 emulator")
        # The reviewed adapter owns any authorized installation and actual playback assertions.
        # An exit code by itself is not evidence of those assertions.
        started = True
        observed = run(args.adapter, env=env, log=log, timeout=900)
        digest = result["apk"]["sha256"]
        if not re.search(rf"^A55_EXACT_OPTIMIZED_PASS sha256={digest}$", observed, re.M | re.I):
            raise ValueError("Device adapter did not report the expected exact-APK smoke result")
        command = adb + ["shell"]
        path = re.fullmatch(r"package:(/data/app/[A-Za-z0-9_./~=+-]+/base\.apk)", run(command + ["pm", "path", PACKAGE]).strip())
        if path is None or run(command + ["sha256sum", path[1]]).split()[0].lower() != digest:
            raise ValueError("Installed APK does not match the tested candidate")
        result["stages"]["device_smoke"] = {**stage("PASS"), "serial": args.serial, "sha256": digest,
                                             "logSha256": sha256(log)}
    except (OSError, ValueError, IndexError, subprocess.SubprocessError) as error:
        result["stages"]["device_smoke"] = stage("FAIL" if started else "BLOCKED", str(error))
    result["device_receipt"] = str(output)
    record(output, result)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    build_args = sub.add_parser("build")
    build_args.add_argument("--out", type=Path, required=True)
    build_args.add_argument("--java-home", type=Path)
    build_args.add_argument("--sdk", type=Path)
    build_args.add_argument("--keystore", type=Path, help="Authorized backup of the original sideload key")
    build_args.add_argument("--preflight-only", action="store_true")
    build_args.add_argument("--windows-trusted-roots", action="store_true")
    for action in ("smoke", "check"):
        command = sub.add_parser(action)
        command.add_argument("--receipt", type=Path, required=True)
        if action == "smoke":
            command.add_argument("--serial", required=True)
            command.add_argument("--adb", type=Path, required=True)
            command.add_argument("--adapter", nargs=argparse.REMAINDER, required=True)
    args = parser.parse_args()
    try:
        if args.action == "check":
            result, apk = exact_candidate(args.receipt, completed=True)
            proof = result["stages"]["device_smoke"]
            if proof["sha256"] != result["apk"]["sha256"] or proof["logSha256"] != sha256(args.receipt.with_suffix(".log")):
                raise ValueError("Device smoke proof changed or refers to other APK bytes")
            print(json.dumps({"status": "ANDROID_APK_VERIFIED", "apk": str(apk), "sha256": result["apk"]["sha256"]}))
            return 0
        result = build(args) if args.action == "build" else smoke(args)
        print(json.dumps(result, indent=2))
        steps = result["stages"]
        if any(step["status"] == "FAIL" for step in steps.values()):
            return 1
        if args.action == "smoke":
            return 0 if steps["device_smoke"]["status"] == "PASS" else 2
        if args.preflight_only:
            return 0 if steps["preflight"]["status"] == "PASS" else 2
        return 0 if steps["identity"]["status"] == "PASS" else 2
    except (KeyError, TypeError, OSError, ValueError, subprocess.SubprocessError) as error:
        print(json.dumps({"status": "ANDROID_GATE_NOT_ACCEPTED", "reason": str(error)}))
        return 1


if __name__ == "__main__":
    sys.exit(main())
