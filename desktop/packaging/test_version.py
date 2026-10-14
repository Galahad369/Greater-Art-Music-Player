import importlib.util
import pathlib
import unittest

spec = importlib.util.spec_from_file_location("desktop_guard", pathlib.Path(__file__).with_name("validate_version.py"))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class DesktopVersionTest(unittest.TestCase):
    def test_historical_vendor_exception_is_exact_and_narrow(self):
        known = "517e260955169c415828e1819e5eacbc8919294a"
        self.assertTrue(guard.historical_vendor_only(known, ["desktop/app/runtime/windows-x64/vlc/skin.xml"]))
        self.assertFalse(guard.historical_vendor_only("a" * 40, ["desktop/app/runtime/windows-x64/vlc/skin.xml"]))
        self.assertFalse(guard.historical_vendor_only(known, ["desktop/native/App.cpp"]))

    def test_reads_independent_identity(self):
        self.assertEqual(("0.1.0", 1), guard.identity("Current desktop: **0.1.0 (code 1)**"))

    def test_rejects_missing_or_nonpositive_code(self):
        for text in ("Current source: **1.21.38 (code 261)**", "Current desktop: **0.1.0 (code 0)**"):
            with self.assertRaises(ValueError):
                guard.identity(text)

    def test_desktop_owns_only_its_workflow_and_implementation(self):
        for path in (".github/workflows/windows-desktop-ci.yml", "desktop/app/build.gradle.kts", "desktop/app/icon.ico", "desktop/native/App.cpp", "desktop/native/Window.inl", "desktop/native/Setup.rc", "desktop/native/Core.h", "desktop/native/Product.wxs", "desktop/native/App.manifest", "desktop/native/build.ps1", "desktop/native/vcpkg.json", "desktop/native/triplets/greater-art-x64-windows.cmake"):
            self.assertTrue(guard.desktop_code(path), path)
        for path in (".github/workflows/android-ci.yml", "greater-art/app/build.gradle.kts", "desktop/README.md", ".github/workflows/version-consistency.yml"):
            self.assertFalse(guard.desktop_code(path), path)

    def test_current_native_package_and_independent_code(self):
        reader = lambda path: (guard.ROOT / path).read_text(encoding="utf-8")
        version, code = guard.identity(reader("desktop/VERSION_DESKTOP.md"))
        guard.check_package(version, code, reader, native=True)
        with self.assertRaises(ValueError):
            guard.check_package(version, code + 1, reader, native=True)

    def test_resource_or_installer_drift_is_rejected(self):
        reader = lambda path: (guard.ROOT / path).read_text(encoding="utf-8")
        version, code = guard.identity(reader("desktop/VERSION_DESKTOP.md"))
        for broken in ("desktop/native/App.rc", "desktop/native/Setup.rc", "desktop/native/Product.wxs", "desktop/native/package.ps1", "desktop/native/vcpkg.json"):
            def altered(path):
                return "" if path == broken else reader(path)
            with self.assertRaises(ValueError):
                guard.check_package(version, code, altered, native=True)


if __name__ == "__main__":
    unittest.main()
