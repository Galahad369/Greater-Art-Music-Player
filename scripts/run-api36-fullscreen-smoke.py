#!/usr/bin/env python3
"""Run and verify the existing fixture-free API36 component test classes."""
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET


CLASSES = (
    ("NowPlayingToolbarChromeTest", 4),
    ("FullscreenTransportIconColorTest", 4),
    ("FullscreenAmbientBoundsTest", 2),
    ("FullscreenToolbarTest", 4),
)
PACKAGE = "com.local.listentomusic.ui."


def inspect_results(directory, class_name, expected):
    report = dict(class_name=class_name, expected=expected, tests=0,
                  failures=0, errors=0, skipped=0, cases=[], outcomes=[], problems=[])
    files = sorted(directory.rglob("*.xml")) if directory.exists() else []
    if not files:
        report["problems"].append("No fresh JUnit XML results")
    for path in files:
        try:
            root = ET.parse(path).getroot()
            suites = list(root.iter("testsuite"))
            if not suites:
                raise ValueError("Missing testsuite element")
            for suite in suites:
                actual = len(list(suite.iter("testcase")))
                if "tests" in suite.attrib and int(suite.attrib["tests"]) != actual:
                    raise ValueError("Declared suite count disagrees with testcase count")
                for key in ("failures", "errors", "skipped"):
                    if int(suite.attrib.get(key, "0")) != 0:
                        report["problems"].append(f"{path.name}: declared {key}={suite.attrib[key]}")
            for case in root.iter("testcase"):
                identity = (case.get("classname", ""), case.get("name", ""))
                if identity[0] != class_name or not identity[1]:
                    report["problems"].append(f"Unexpected testcase identity: {identity}")
                if identity in report["cases"]:
                    report["problems"].append(f"Duplicate testcase: {identity}")
                report["cases"].append(identity)
                report["tests"] += 1
                details = []
                for tag, key in (("failure", "failures"), ("error", "errors"), ("skipped", "skipped")):
                    report[key] += int(case.find(tag) is not None)
                    for result in case.findall(tag):
                        details.append(dict(kind=tag, attributes=dict(result.attrib),
                                            text="".join(result.itertext())))
                status = next((status for tag, status in
                               (("error", "error"), ("failure", "failed"), ("skipped", "skipped"))
                               if case.find(tag) is not None), "passed")
                report["outcomes"].append(dict(class_name=identity[0], name=identity[1],
                                              status=status, details=details))
        except (ET.ParseError, OSError, ValueError) as error:
            report["problems"].append(f"{path.name}: {error}")
    if report["tests"] != expected:
        report["problems"].append(f"Expected {expected} tests; observed {report['tests']}")
    if any(report[key] for key in ("failures", "errors", "skipped")):
        report["problems"].append("Test failures, errors, or skips are not a pass")
    return report


def run(project):
    evidence_root = project / "app/build/reports/api36-fullscreen-components"
    evidence_root.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ-")
    evidence = Path(tempfile.mkdtemp(prefix=timestamp, dir=evidence_root))
    results = project / "app/build/outputs/androidTest-results/connected"

    def archive(label):
        destination = evidence / label
        if results.exists():
            shutil.move(str(results), str(destination))
        return destination

    # Keep prior reports, but never count them as this invocation's results.
    archive("preexisting-results")
    print(f"Component evidence directory: {evidence}", flush=True)
    reports = []
    for short_name, expected in CLASSES:
        class_name = PACKAGE + short_name
        print(f"Running {class_name}; expected {expected} tests", flush=True)
        command = [str(project / "gradlew"), ":app:connectedDebugAndroidTest", "--no-daemon",
                   f"-Pandroid.testInstrumentationRunnerArguments.class={class_name}"]
        try:
            exit_code = subprocess.run(command, cwd=project, check=False).returncode
            launch_error = None
        except OSError as error:
            exit_code, launch_error = 1, str(error)
        report = inspect_results(archive(short_name), class_name, expected)
        report["gradle_exit_code"] = exit_code
        if exit_code:
            report["problems"].append(f"Gradle exited with {exit_code}")
        if launch_error:
            report["problems"].append(launch_error)
        reports.append(report)
        print(json.dumps(report, indent=2), flush=True)
    summary = dict(scope="Fixture-free components only; not exact-APK or A55 acceptance",
                   expected_tests=sum(count for _, count in CLASSES),
                   observed_tests=sum(report["tests"] for report in reports), classes=reports)
    passed = all(not report["problems"] for report in reports)
    summary["passed"] = passed
    (evidence / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(f"Component suite {'PASS' if passed else 'FAIL'}: "
          f"{summary['observed_tests']}/{summary['expected_tests']} tests; evidence {evidence}", flush=True)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(run(Path(__file__).resolve().parent.parent / "greater-art"))
