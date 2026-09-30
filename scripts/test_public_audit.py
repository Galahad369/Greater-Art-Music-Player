"""Regression checks for historical privacy coverage; requires Git and pwsh."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

SCANNER = pathlib.Path(__file__).with_name("audit-public-repo.ps1")


class HistoricalPrivacyAudit(unittest.TestCase):
    def setUp(self):
        self.scratch = tempfile.TemporaryDirectory()
        self.addCleanup(self.scratch.cleanup)
        self.root = pathlib.Path(self.scratch.name)
        self.git("init", "-b", "main")
        self.git("config", "user.name", "Audit Fixture")
        self.git("config", "user.email", "fixture@users.noreply.github.com")
        (self.root / "README.md").write_text("Clean fixture\n")
        self.commit("Clean baseline")

    def git(self, *args):
        return subprocess.run(["git", "-C", str(self.root), *args],
                              check=True, capture_output=True, text=True).stdout

    def commit(self, message):
        self.git("add", "-A")
        self.git("commit", "-m", message)

    def audit(self, root=None):
        return subprocess.run(["pwsh", "-NoProfile", "-File", str(SCANNER),
                               str(root or self.root)], capture_output=True, text=True)

    def test_clean_history_passes(self):
        self.assertEqual(self.audit().returncode, 0)

    def test_deleted_paths_on_tag_are_detected_without_value_disclosure(self):
        paths = ["D:" + "\\Users\\fixture\\private\\report.txt",
                 "/Users/" + "fixture/private/report.txt",
                 "/home/" + "fixture/private/report.txt"]
        for index, value in enumerate(paths):
            with self.subTest(value=index):
                self.git("checkout", "-b", f"leak-{index}", "main")
                (self.root / "build.txt").write_text(value)
                self.commit("Historical leak")
                self.git("tag", f"old-release-{index}")
                self.git("checkout", "main")
                self.git("branch", "-D", f"leak-{index}")
                result = self.audit()
                self.assertEqual(result.returncode, 1)
                self.assertIn("Workstation home path in reachable history", result.stdout)
                self.assertNotIn(value, result.stdout)
                self.git("tag", "-d", f"old-release-{index}")

    def test_shallow_clone_refuses_complete_history_claim(self):
        clone = self.root.parent / (self.root.name + "-shallow")
        self.addCleanup(shutil.rmtree, clone, True)
        subprocess.run(["git", "clone", "--depth=1", self.root.as_uri(), str(clone)],
                       check=True, capture_output=True)
        result = self.audit(clone)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("History audit requires a full clone", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
