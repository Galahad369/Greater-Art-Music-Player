#!/usr/bin/env python3
"""Synthetic cross-engine regression for the PR24 historical path patterns."""
import re
import runpy
import subprocess
import tempfile
from pathlib import Path

ns = runpy.run_path(str(Path(__file__).with_name("pr24-rehearse-history.py")))
regex = re.compile("|".join(ns["PATTERNS"]))
fixtures = {
    "win-space.log": "Z:" + "\\" + "Users" + "\\" + "Synthetic Person" + "\\" + "Documents" + "\\",
    "win-lower.log": "c:/users/synthetic-person/Documents/",
    "mac-space.log": "/" + "Users" + "/Synthetic Person/Documents/",
    "linux-space.log": "/" + "home" + "/synthetic-person/Documents/",
}
negative = ("C:/Windows/System32/", "/var/data/file", "/tmp/Users/foo", "docs/project/local")
for name, value in fixtures.items():
    assert regex.search(value), f"Python regex missed {name}"
for value in negative:
    assert not regex.search(value), f"Python regex false positive: {value}"

with tempfile.TemporaryDirectory(prefix="pr24-fixtures-") as d:
    root = Path(d)
    def git(*args):
        result = subprocess.run(["git", "-C", d, *args], text=True, capture_output=True)
        if result.returncode:
            raise RuntimeError("git " + args[0] + " failed")
        return result.stdout.strip()
    git("init", "-q")
    git("config", "user.name", "Synthetic Tester")
    git("config", "user.email", "123456+pr24-test@users.noreply.github.com")
    (root / "README.txt").write_text("safe text\n", encoding="utf-8")
    git("add", ".")
    git("commit", "-qm", "clean")
    count, affected = ns["scan"](root)
    assert affected == 0, f"Clean history falsely matched {affected} files"
    for name, value in fixtures.items():
        (root / name).write_text("Synthetic: " + value + "\n", encoding="utf-8")
    (root / "negative.log").write_text("\n".join(negative), encoding="utf-8")
    git("add", ".")
    git("commit", "-qm", "add synthetic data")
    for name in fixtures: git("rm", "-q", name)
    git("rm", "-q", "negative.log")
    git("commit", "-qm", "delete synthetic data")
    count, affected = ns["scan"](root)
    assert count == 3 and affected == len(fixtures), (count, affected)
print("[OK] Four removed historical paths detected; false positives excluded")
