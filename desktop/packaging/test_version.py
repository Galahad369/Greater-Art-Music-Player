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
        for path in (".github/workflows/windows-desktop-ci.yml", "desktop/app/build.gradle.kts", "desktop/app/icon.ico", "desktop/native/App.cpp", "desktop/native/Window.inl", "desktop/native/Setup.rc", "desktop/native/Core.h", "desktop/native/Product.wxs", "desktop/native/App.manifest", "desktop/native/build.ps1", "desktop/native/vcpkg.json", "desktop/native/triplets/greater-art-x64-windows.cmake", "desktop/winui/Engine.cs", "desktop/winui/MainWindow.xaml", guard.WINUI_PROJECT):
            self.assertTrue(guard.desktop_code(path), path)
        for path in (".github/workflows/android-ci.yml", "greater-art/app/build.gradle.kts", "desktop/README.md", ".github/workflows/version-consistency.yml"):
            self.assertFalse(guard.desktop_code(path), path)

    def test_current_native_package_and_independent_code(self):
        reader = lambda path: (guard.ROOT / path).read_text(encoding="utf-8")
        version, code = guard.identity(reader("desktop/VERSION_DESKTOP.md"))
        guard.check_package(version, code, reader, native=True, winui=(guard.ROOT / guard.WINUI_PROJECT).exists())
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

    def test_normal_patch_and_exact_consumed_identity_recovery(self):
        guard.require_patch_increment(("0.1.15", 16), ("0.1.16", 17))
        guard.require_patch_increment(("0.1.16", 17), ("0.1.19", 20))
        guard.require_patch_increment(("0.1.19", 20), ("0.1.20", 21))

    def test_other_skips_reuse_and_code_drift_are_rejected(self):
        for previous, current in (
            (("0.1.16", 17), ("0.1.18", 19)),
            (("0.1.16", 17), ("0.1.19", 19)),
            (("0.1.16", 16), ("0.1.19", 20)),
            (("0.1.15", 16), ("0.1.19", 20)),
            (("0.1.19", 20), ("0.1.22", 23)),
            (("0.1.19", 20), ("0.1.19", 20)),
            (("0.1.19", 20), ("0.1.20", 20)),
        ):
            with self.subTest(previous=previous, current=current), self.assertRaises(ValueError):
                guard.require_patch_increment(previous, current)

    def test_winui_version_fields_match_and_drift_is_rejected(self):
        reader = lambda path: (guard.ROOT / path).read_text(encoding="utf-8")
        version, code = guard.identity(reader("desktop/VERSION_DESKTOP.md"))
        project = ("<Project><PropertyGroup>"
                   f"<Version>{version}</Version>"
                   f"<AssemblyVersion>{version}.0</AssemblyVersion>"
                   f"<FileVersion>{version}.0</FileVersion>"
                   "</PropertyGroup></Project>")

        def with_project(text):
            return lambda path: text if path == guard.WINUI_PROJECT else reader(path)

        guard.check_package(version, code, with_project(project), native=True, winui=True)
        for field in ("Version", "AssemblyVersion", "FileVersion"):
            changed = project.replace(f"<{field}>{version}", f"<{field}>9.9.9")
            with self.subTest(field=field), self.assertRaises(ValueError):
                guard.check_package(version, code, with_project(changed), native=True, winui=True)
        for broken in ("", "<Project />", "<Project>"):
            with self.subTest(project=broken), self.assertRaises(ValueError):
                guard.check_package(version, code, with_project(broken), native=True, winui=True)


if __name__ == "__main__":
    unittest.main()
