import importlib.util
import pathlib
import unittest

spec = importlib.util.spec_from_file_location("desktop_guard", pathlib.Path(__file__).with_name("validate_version.py"))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class DesktopVersionTest(unittest.TestCase):
    def test_reads_independent_identity(self):
        self.assertEqual(("0.1.0", 1), guard.identity("Current desktop: **0.1.0 (code 1)**"))

    def test_rejects_missing_or_nonpositive_code(self):
        for text in ("Current source: **1.21.38 (code 261)**", "Current desktop: **0.1.0 (code 0)**"):
            with self.assertRaises(ValueError):
                guard.identity(text)

    def test_desktop_owns_only_its_workflow_and_implementation(self):
        for path in (".github/workflows/windows-desktop-ci.yml", "desktop/app/build.gradle.kts", "desktop/app/icon.ico", "desktop/packaging/prepare-runtime.ps1"):
            self.assertTrue(guard.desktop_code(path), path)
        for path in (".github/workflows/android-ci.yml", "greater-art/app/build.gradle.kts", "desktop/README.md", ".github/workflows/version-consistency.yml"):
            self.assertFalse(guard.desktop_code(path), path)


if __name__ == "__main__":
    unittest.main()
