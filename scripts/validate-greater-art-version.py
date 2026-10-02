#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE_REL = "greater-art/app/build.gradle.kts"
RULES_REL = "greater-art/VERSION_RULES.md"
HANDOFF_REL = "greater-art/HANDOFF.md"
RELEASES_REL = "greater-art/releases"
PACKAGE_ID = "com.local.listentomusic"

SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
APK_NAME_RE = re.compile(r"^GreaterArt-(\d+\.\d+\.\d+)\.apk$")
VERSION_LINE_RE = re.compile(r"^\s*version(?:Code|Name)\s*=")
SERIES_TRANSITION_RE = re.compile(
    r"^Allowed series transition:\s*\*\*(\d+\.\d+\.\d+) -> (\d+\.\d+\.\d+)\*\*$",
    re.MULTILINE,
)
CONSUMED_TRANSITION_RE = re.compile(
    r"^Allowed consumed transition:\s*\*\*(\d+\.\d+\.\d+) \(code (\d+)\) -> "
    r"(\d+\.\d+\.\d+) \(code (\d+)\)\*\*$",
    re.MULTILINE,
)


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def run_git(*args: str, check: bool = True) -> str:
    proc = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if check and proc.returncode:
        raise RuntimeError(f"git {' '.join(args)} failed: {proc.stderr.strip()}")
    return proc.stdout


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def parse_gradle(text: str, label: str) -> tuple[str, int]:
    version_match = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    code_match = re.search(r"versionCode\s*=\s*(\d+)", text)
    if not version_match or not code_match:
        raise ValueError(f"{label}: versionName/versionCode not found")
    version = version_match.group(1)
    if not SEMVER_RE.fullmatch(version):
        raise ValueError(f"{label}: versionName {version!r} is not major.minor.patch")
    return version, int(code_match.group(1))


def parse_version(value: str) -> tuple[int, int, int]:
    match = SEMVER_RE.fullmatch(value)
    if not match:
        raise ValueError(f"invalid semantic version {value!r}")
    major, minor, patch = match.groups()
    return int(major), int(minor), int(patch)


def version_from_ref(ref: str) -> tuple[str, int]:
    return parse_gradle(run_git("show", f"{ref}:{GRADLE_REL}"), ref)


def parse_rules(text: str) -> tuple[str, int, str, str, int, list[tuple[str, int]]]:
    current = re.search(
        r"^Current source:\s*\*\*(\d+\.\d+\.\d+) \(code (\d+)\)\*\*$",
        text,
        re.MULTILINE,
    )
    state = re.search(
        r"^Current release state:\s*\*\*(SOURCE_ONLY|VERIFIED)\*\*$",
        text,
        re.MULTILINE,
    )
    latest = re.search(
        r"^Latest verified APK:\s*\*\*(\d+\.\d+\.\d+) \(code (\d+)\)\*\*$",
        text,
        re.MULTILINE,
    )
    tracked = [
        (version, int(code))
        for version, code in re.findall(
            r"^- (\d+\.\d+\.\d+) \(code (\d+)\)",
            text,
            re.MULTILINE,
        )
    ]
    if not current or not state or not latest or not tracked:
        raise ValueError("VERSION_RULES.md is missing hardened machine-checkable state")
    return (
        current.group(1),
        int(current.group(2)),
        state.group(1),
        latest.group(1),
        int(latest.group(2)),
        tracked,
    )


def is_next_patch(previous: str, current: str) -> bool:
    p_major, p_minor, p_patch = parse_version(previous)
    c_major, c_minor, c_patch = parse_version(current)
    return (c_major, c_minor, c_patch) == (p_major, p_minor, p_patch + 1)


def allowed_series_transitions(rules_text: str) -> set[tuple[str, str]]:
    return set(SERIES_TRANSITION_RE.findall(rules_text))


def is_allowed_version_step(previous: str, current: str, rules_text: str) -> bool:
    return is_next_patch(previous, current) or (
        previous,
        current,
    ) in allowed_series_transitions(rules_text)


def allowed_consumed_transitions(
    rules_text: str,
) -> set[tuple[str, int, str, int]]:
    return {
        (previous, int(previous_code), current, int(current_code))
        for previous, previous_code, current, current_code
        in CONSUMED_TRANSITION_RE.findall(rules_text)
    }


def is_allowed_version_code_step(
    previous_version: str,
    previous_code: int,
    current_version: str,
    current_code: int,
    commit_rules_text: str,
    head_rules_text: str,
) -> bool:
    normal_step = (
        is_allowed_version_step(previous_version, current_version, commit_rules_text)
        and current_code == previous_code + 1
    )
    consumed_step = (
        previous_version,
        previous_code,
        current_version,
        current_code,
    ) in allowed_consumed_transitions(head_rules_text)
    return normal_step or consumed_step


def gradle_has_substantive_change(parent: str, commit: str) -> bool:
    diff = run_git("diff", "--unified=0", parent, commit, "--", GRADLE_REL)
    for line in diff.splitlines():
        if not line or line.startswith(("+++", "---", "@@")):
            continue
        if line[0] not in "+-":
            continue
        if not VERSION_LINE_RE.match(line[1:]):
            return True
    return False


def changed_paths(parent: str, commit: str) -> list[str]:
    return [
        path.strip()
        for path in run_git("diff", "--name-only", parent, commit).splitlines()
        if path.strip()
    ]


def is_versioned_code_path(path: str, parent: str, commit: str) -> bool:
    if path == GRADLE_REL:
        return gradle_has_substantive_change(parent, commit)

    if path.startswith("greater-art/app/src/"):
        return Path(path).suffix.lower() in {
            ".kt", ".java", ".xml", ".aidl", ".c", ".cc", ".cpp", ".h", ".hpp",
        }

    if path in {
        "greater-art/build.gradle.kts",
        "greater-art/settings.gradle.kts",
        "greater-art/gradle.properties",
        "greater-art/gradle/libs.versions.toml",
    }:
        return True

    if path.startswith("greater-art/gradle/") and not path.endswith(".md"):
        return True

    if path.startswith("scripts/"):
        return Path(path).suffix.lower() in {".py", ".ps1", ".sh", ".js", ".ts"}

    if "/" not in path and Path(path).suffix.lower() in {".py", ".ps1", ".sh", ".js", ".ts"}:
        # Root-level executable helpers are still repository tooling. Keeping this
        # explicit closes the gap that allowed commit-amending version scripts to
        # change without consuming a source version.
        return True

    if path.startswith(".github/workflows/"):
        return Path(path).suffix.lower() in {".yml", ".yaml"}

    return False


def rules_from_ref(ref: str) -> tuple[str, int, str, str, int, list[tuple[str, int]]]:
    return parse_rules(run_git("show", f"{ref}:{RULES_REL}"))


def validate_commit_range(errors: list[str], base: str, head: str = "HEAD") -> None:
    try:
        head_rules_text = run_git("show", f"{head}:{RULES_REL}")
    except Exception as exc:
        fail(errors, f"cannot read head VERSION_RULES for range validation: {exc}")
        head_rules_text = ""

    commits = [
        commit
        for commit in run_git("rev-list", "--reverse", f"{base}..{head}").splitlines()
        if commit
    ]

    for commit in commits:
        parent = run_git("rev-parse", f"{commit}^").strip()
        paths = changed_paths(parent, commit)
        substantive = any(
            is_versioned_code_path(path, parent, commit)
            for path in paths
        )

        if substantive:
            try:
                previous_version, previous_code = version_from_ref(parent)
                current_version, current_code = version_from_ref(commit)
            except Exception as exc:
                fail(errors, f"{commit[:12]}: cannot read version metadata: {exc}")
                continue

            try:
                commit_rules_text = run_git("show", f"{commit}:{RULES_REL}")
            except Exception as exc:
                fail(errors, f"{commit[:12]}: cannot read VERSION_RULES for version step: {exc}")
                commit_rules_text = ""

            if not is_allowed_version_code_step(
                previous_version,
                previous_code,
                current_version,
                current_code,
                commit_rules_text,
                head_rules_text,
            ):
                fail(
                    errors,
                    f"{commit[:12]}: versioned code changed but version/code must be "
                    f"the next PATCH/+1 code, an allowed series transition, or an "
                    f"exact consumed-branch transition declared at the range head; "
                    f"found {previous_version}/code {previous_code} -> "
                    f"{current_version}/code {current_code}",
                )

            try:
                rule_version, rule_code, state, _, _, tracked = rules_from_ref(commit)
                if (rule_version, rule_code) != (current_version, current_code):
                    fail(
                        errors,
                        f"{commit[:12]}: VERSION_RULES current source "
                        f"{rule_version}/code {rule_code} does not match Gradle "
                        f"{current_version}/code {current_code}",
                    )
                if not tracked or tracked[-1] != (current_version, current_code):
                    fail(
                        errors,
                        f"{commit[:12]}: tracked version ledger must end at "
                        f"{current_version} (code {current_code})",
                    )
                if state != "SOURCE_ONLY":
                    fail(
                        errors,
                        f"{commit[:12]}: a code-changing commit must land as SOURCE_ONLY; "
                        "local build verification is a separate follow-up",
                    )
            except Exception as exc:
                fail(errors, f"{commit[:12]}: VERSION_RULES check failed: {exc}")

        status = run_git(
            "diff-tree",
            "--no-commit-id",
            "--name-status",
            "-r",
            "-M",
            "-C",
            "--find-copies-harder",
            parent,
            commit,
            "--",
            RELEASES_REL,
        )

        for line in status.splitlines():
                    parts = line.split("\t")
                    if not parts:
                        continue
                    kind = parts[0][0]
                    apk_paths = [path for path in parts[1:] if path.endswith(".apk")]
                    if not apk_paths:
                        continue

                    if kind in {"M", "R"}:
                        fail(
                            errors,
                            f"{commit[:12]}: release APKs are immutable; "
                            f"modification/rename detected: {line}",
                        )

                    if kind == "C":
                        # Copy detection can trigger on similar APK artifacts (e.g. new version
                        # from same source). Only fail if the actual content is identical.
                        apk_path = apk_paths[-1]
                        full_path = ROOT / apk_path
                        if full_path.is_file():
                            digest = hashlib.sha256()
                            with full_path.open("rb") as handle:
                                for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                                    digest.update(chunk)
                            new_hash = digest.hexdigest()
                            # Check if any existing APK in the tree has the same hash
                            for existing_apk in (ROOT / RELEASES_REL).glob("*.apk"):
                                if existing_apk == full_path:
                                    continue
                                existing_digest = hashlib.sha256()
                                with existing_apk.open("rb") as handle:
                                    for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                                        existing_digest.update(chunk)
                                if existing_digest.hexdigest() == new_hash:
                                    fail(
                                        errors,
                                        f"{commit[:12]}: release APK is byte-identical to "
                                        f"existing {existing_apk.relative_to(ROOT)}; "
                                        f"immutable APKs must not be copied/renamed",
                                    )
                                    break

                    if kind == "A":
                        apk_path = apk_paths[-1]
                        match = APK_NAME_RE.fullmatch(Path(apk_path).name)
                        if not match:
                            fail(errors, f"{commit[:12]}: invalid release APK filename: {apk_path}")
                            continue
                        try:
                            current_version, _ = version_from_ref(commit)
                        except Exception as exc:
                            fail(
                                errors,
                                f"{commit[:12]}: cannot validate APK filename against Gradle: {exc}",
                            )
                            continue
                        if match.group(1) != current_version:
                            fail(
                                errors,
                                f"{commit[:12]}: new APK {Path(apk_path).name} does not match "
                                f"Gradle version {current_version}",
                            )


def find_aapt() -> str | None:
    explicit = os.environ.get("AAPT")
    if explicit and Path(explicit).is_file():
        return explicit

    direct = shutil.which("aapt")
    if direct:
        return direct

    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        build_tools = Path(sdk) / "build-tools"
        if build_tools.is_dir():
            candidates = sorted(build_tools.glob("*/aapt"), reverse=True)
            if candidates:
                return str(candidates[0])

    return None


def validate_release_artifact(
    errors: list[str],
    version: str,
    code: int,
    handoff: str,
) -> None:
    apk_rel = f"{RELEASES_REL}/GreaterArt-{version}.apk"
    apk = ROOT / apk_rel

    if not apk.is_file():
        fail(errors, f"verified release is missing APK: {apk_rel}")
        return

    apk_name = apk.name
    expected_name = f"GreaterArt-{version}.apk"
    if apk_name != expected_name:
        fail(errors, f"APK filename must be '{expected_name}', found '{apk_name}'")

    digest = hashlib.sha256()
    with apk.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    apk_hash = digest.hexdigest()

    if apk_hash.lower() not in handoff.lower():
        fail(errors, "HANDOFF must record the exact SHA-256 of the verified APK")

    aapt = find_aapt()
    if not aapt:
        fail(
            errors,
            "aapt not found; --release validation requires Android build-tools",
        )
        return

    proc = subprocess.run(
        [aapt, "dump", "badging", str(apk)],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if proc.returncode:
        fail(errors, f"aapt dump badging failed: {proc.stderr.strip()}")
        return

    package = re.search(
        r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",
        proc.stdout,
    )
    if not package:
        fail(errors, "aapt output did not contain package/version metadata")
        return

    package_name, apk_code, apk_version = package.groups()
    if package_name != PACKAGE_ID:
        fail(errors, f"APK package must be {PACKAGE_ID}, found {package_name}")
    if apk_version != version:
        fail(errors, f"APK versionName must be {version}, found {apk_version}")
    if apk_code != str(code):
        fail(errors, f"APK versionCode must be {code}, found {apk_code}")


def validate_current_state(errors: list[str], require_release: bool) -> None:
    try:
        gradle_version, gradle_code = parse_gradle(read(GRADLE_REL), "Gradle")
        rules_text = read(RULES_REL)
        (
            rules_version,
            rules_code,
            state,
            latest_version,
            latest_code,
            tracked,
        ) = parse_rules(rules_text)
    except Exception as exc:
        fail(errors, str(exc))
        return

    if (rules_version, rules_code) != (gradle_version, gradle_code):
        fail(
            errors,
            f"VERSION_RULES current source {rules_version}/code {rules_code} "
            f"does not match Gradle {gradle_version}/code {gradle_code}",
        )

    if tracked[-1] != (gradle_version, gradle_code):
        fail(
            errors,
            f"VERSION_RULES tracked ledger must end at "
            f"{gradle_version} (code {gradle_code})",
        )

    for (previous_version, previous_code), (current_version, current_code) in zip(
        tracked,
        tracked[1:],
    ):
        if (
            not is_allowed_version_step(previous_version, current_version, rules_text)
            or current_code != previous_code + 1
        ):
            fail(
                errors,
                f"VERSION_RULES ledger is not sequential or explicitly allowed: "
                f"{previous_version}/code {previous_code} -> "
                f"{current_version}/code {current_code}",
            )

    handoff = read(HANDOFF_REL)
    handoff_current = re.search(
        r"^\*\*Current version:\*\*\s*\x60(\d+\.\d+\.\d+) \(code (\d+)\)\x60",
        handoff,
        re.MULTILINE,
    )
    handoff_apk = re.search(
        r"^\*\*Latest APK:\*\*\s*\x60releases/GreaterArt-(\d+\.\d+\.\d+)\.apk\x60",
        handoff,
        re.MULTILINE,
    )

    expected_handoff = (
        (gradle_version, gradle_code)
        if state == "VERIFIED"
        else (latest_version, latest_code)
    )

    if not handoff_current:
        fail(errors, "HANDOFF Current version line not found")
    elif (
        handoff_current.group(1),
        int(handoff_current.group(2)),
    ) != expected_handoff:
        fail(
            errors,
            f"HANDOFF Current version must describe latest verified release "
            f"{expected_handoff[0]} (code {expected_handoff[1]}) while state={state}",
        )

    if not handoff_apk:
        fail(errors, "HANDOFF Latest APK line not found")
    elif handoff_apk.group(1) != latest_version:
        fail(
            errors,
            f"HANDOFF Latest APK must be GreaterArt-{latest_version}.apk, "
            f"found {handoff_apk.group(1)}",
        )

    current_apk = ROOT / RELEASES_REL / f"GreaterArt-{gradle_version}.apk"
    latest_apk = ROOT / RELEASES_REL / f"GreaterArt-{latest_version}.apk"

    latest_tuple = parse_version(latest_version)
    for release_apk in sorted((ROOT / RELEASES_REL).glob("*.apk")):
        match = APK_NAME_RE.fullmatch(release_apk.name)
        if not match:
            fail(errors, f"invalid release APK filename in current tree: {release_apk.relative_to(ROOT)}")
            continue
        artifact_version = match.group(1)
        artifact_tuple = parse_version(artifact_version)
        if artifact_tuple > latest_tuple and not (
            state == "VERIFIED" and artifact_version == gradle_version
        ):
            fail(
                errors,
                f"unverified APK newer than Latest verified APK is present: "
                f"{release_apk.relative_to(ROOT)} while latest verified is {latest_version}",
            )

    if state == "SOURCE_ONLY":
        if current_apk.exists():
            fail(
                errors,
                f"SOURCE_ONLY forbids a current-version APK at "
                f"{current_apk.relative_to(ROOT)}; verify it first, then mark VERIFIED",
            )
        if not latest_apk.exists():
            fail(
                errors,
                f"latest verified APK is missing: {latest_apk.relative_to(ROOT)}",
            )
        if require_release:
            fail(
                errors,
                "release validation requested but VERSION_RULES state is SOURCE_ONLY",
            )

    elif state == "VERIFIED":
        if (latest_version, latest_code) != (gradle_version, gradle_code):
            fail(
                errors,
                "VERIFIED state requires Latest verified APK to equal Current source",
            )
        validate_release_artifact(errors, gradle_version, gradle_code, handoff)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Greater Art version and immutable-release guard",
    )
    parser.add_argument(
        "--base",
        help="Validate every commit in BASE..HEAD for exact version bumps and APK immutability",
    )
    parser.add_argument(
        "--head",
        default="HEAD",
        help="Commit/ref to use as the end of --base range validation (default: HEAD)",
    )
    parser.add_argument(
        "--release",
        action="store_true",
        help="Require current source to have a locally verified current-version APK",
    )
    args = parser.parse_args()

    errors: list[str] = []
    validate_current_state(errors, require_release=args.release)

    if args.base:
        try:
            run_git("rev-parse", "--verify", args.base)
            run_git("rev-parse", "--verify", args.head)
            validate_commit_range(errors, args.base, args.head)
        except Exception as exc:
            fail(errors, f"commit-range validation failed: {exc}")

    if errors:
        print(
            f"Greater Art version guard FAILED with {len(errors)} problem(s):",
            file=sys.stderr,
        )
        for error in errors:
            print(f" - {error}", file=sys.stderr)
        return 1

    version, code = parse_gradle(read(GRADLE_REL), "Gradle")
    _, _, state, latest_version, latest_code, _ = parse_rules(read(RULES_REL))
    print(
        f"Greater Art version guard OK: source {version} (code {code}), "
        f"state={state}"
    )
    print(f"Latest verified APK: {latest_version} (code {latest_code})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
