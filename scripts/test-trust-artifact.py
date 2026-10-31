import importlib.util
from pathlib import Path
import unittest
import tempfile

spec = importlib.util.spec_from_file_location("trust_artifact", Path(__file__).with_name("verify-trust-artifact.py"))
trust = importlib.util.module_from_spec(spec)
spec.loader.exec_module(trust)
inventory_spec = importlib.util.spec_from_file_location("trust_inventory", Path(__file__).with_name("trust-dependency-inventory.py"))
inventory = importlib.util.module_from_spec(inventory_spec)
inventory_spec.loader.exec_module(inventory)


class ArtifactTrustTest(unittest.TestCase):
    def android(self, **changes):
        values = dict(badging="package: name='com.local.listentomusic' versionCode='316' versionName='1.21.93'",
            permissions="uses-permission: name='android.permission.WAKE_LOCK'\nuses-permission: name='android.permission.MANAGE_EXTERNAL_STORAGE'",
            xml='E: activity\n A: android:name(0x1)="com.local.listentomusic.LauncherActivity"\n A: android:exported(0x2)=(type 0x12)0xffffffff',
            signature="Verifies\nV2 Signer: certificate SHA-256 digest: " + trust.COMPATIBILITY_CERT,
            channel="sideload", expected_version="1.21.93", expected_code=316, expected_cert=trust.COMPATIBILITY_CERT)
        values.update(changes)
        return trust.android_policy(**values)

    def test_sideload_is_labelled_honestly(self):
        result = self.android()
        self.assertTrue(result["broadStorageDeclared"])
        self.assertIn("not production", result["channelWarning"])

    def test_missing_signature_is_rejected(self):
        with self.assertRaises(ValueError): self.android(signature="Does not verify")

    def test_internet_permission_is_rejected(self):
        with self.assertRaises(ValueError): self.android(permissions="uses-permission: name='android.permission.INTERNET'")

    def test_wrong_certificate_is_rejected(self):
        with self.assertRaises(ValueError): self.android(expected_cert="0" * 64)

    def test_missing_approved_certificate_is_rejected(self):
        with self.assertRaises(ValueError): self.android(expected_cert=None)

    def test_wrong_package_is_rejected(self):
        with self.assertRaises(ValueError): self.android(badging="package: name='other.app' versionCode='316' versionName='1.21.93'")

    def test_wrong_version_is_rejected(self):
        with self.assertRaises(ValueError): self.android(expected_version="1.21.94")

    def test_debug_distribution_is_rejected(self):
        with self.assertRaises(ValueError): self.android(xml="A: android:debuggable=(type 0x12)0xffffffff")

    def test_test_only_apk_is_rejected_even_for_development(self):
        with self.assertRaises(ValueError): self.android(channel="development", xml="A: android:testOnly=(type 0x12)0xffffffff")

    def test_exported_unknown_component_is_rejected(self):
        with self.assertRaises(ValueError): self.android(xml='E: service\n A: android:name="other.Exported"\n A: android:exported=(type 0x12)0xffffffff')

    def test_known_compose_hosts_are_allowed_only_for_debug_development(self):
        for name in ("androidx.compose.ui.tooling.PreviewActivity", "androidx.activity.ComponentActivity"):
            with self.subTest(name=name):
                component = f'E: activity\n A: android:name="{name}"\n A: android:exported=(type 0x12)0xffffffff'
                result = self.android(channel="development", xml='A: android:debuggable=(type 0x12)0xffffffff\n' + component)
                self.assertEqual([name], result["exportedComponents"])
                with self.assertRaises(ValueError): self.android(channel="development", xml=component)
                with self.assertRaises(ValueError): self.android(xml=component)

    def test_unknown_debug_export_is_still_rejected(self):
        with self.assertRaises(ValueError):
            self.android(channel="development", xml='A: android:debuggable=(type 0x12)0xffffffff\nE: activity\n A: android:name="other.Exported"\n A: android:exported=(type 0x12)0xffffffff')

    def test_compose_host_exception_cannot_change_component_type_or_permission(self):
        for component in (
            'E: service\n A: android:name="androidx.activity.ComponentActivity"\n A: android:exported=(type 0x12)0xffffffff',
            'E: activity\n A: android:name="androidx.activity.ComponentActivity"\n A: android:permission="other.Permission"\n A: android:exported=(type 0x12)0xffffffff',
        ):
            with self.assertRaises(ValueError):
                self.android(channel="development", xml='A: android:debuggable=(type 0x12)0xffffffff\n' + component)

    def test_attribute_order_does_not_change_export_validation(self):
        result = self.android(xml='E: activity\n A: android:exported=(type 0x12)0xffffffff\n A: android:name=".LauncherActivity"')
        self.assertEqual(["com.local.listentomusic.LauncherActivity"], result["exportedComponents"])

    def test_library_export_requires_its_platform_permission(self):
        component = 'E: receiver\n A: android:name="androidx.profileinstaller.ProfileInstallReceiver"\n A: android:exported=(type 0x12)0xffffffff'
        with self.assertRaises(ValueError): self.android(xml=component)
        self.android(xml=component + '\n A: android:permission="android.permission.DUMP"')

    def test_production_cannot_relabel_legacy_sideload(self):
        with self.assertRaises(ValueError): self.android(channel="production")

    def test_production_rejects_legacy_key_without_broad_storage(self):
        with self.assertRaises(ValueError): self.android(channel="production", permissions="uses-permission: name='android.permission.WAKE_LOCK'")

    def test_production_rejects_broad_storage_with_new_key(self):
        with self.assertRaises(ValueError): self.android(channel="production", expected_cert="a" * 64,
            signature="Verifies\ncertificate SHA-256 digest: " + "a" * 64)

    def test_production_allows_reviewed_nonlegacy_identity_and_narrow_permissions(self):
        result = self.android(channel="production", expected_cert="a" * 64,
            permissions="uses-permission: name='android.permission.WAKE_LOCK'",
            signature="Verifies\ncertificate SHA-256 digest: " + "a" * 64)
        self.assertFalse(result["broadStorageDeclared"])

    def test_windows_unsigned_allowed_only_in_development(self):
        metadata = {"version": "0.1.22.0", "status": "NotSigned", "certificateSha256": None}
        self.assertEqual(metadata, trust.windows_policy(metadata, "development", "0.1.22", None))
        with self.assertRaises(ValueError): trust.windows_policy(metadata, "production", "0.1.22", "a" * 64)

    def test_windows_publisher_must_match(self):
        metadata = {"version": "0.1.22.0", "status": "Valid", "certificateSha256": "a" * 64}
        self.assertEqual(metadata, trust.windows_policy(metadata, "production", "0.1.22", "a" * 64))
        with self.assertRaises(ValueError): trust.windows_policy(metadata, "production", "0.1.22", "b" * 64)

    def test_gradle_inventory_uses_selected_version_and_omits_paths(self):
        result = inventory.gradle_packages("/synthetic/private/project\n+--- androidx.example:module:1.0 -> 1.1\n\\--- androidx.example:module:1.1")
        self.assertEqual([{"group": "androidx.example", "name": "module", "version": "1.1"}], result)
        self.assertNotIn("/synthetic/private", str(result))

    def test_windows_inventory_uses_locked_transitive_versions(self):
        result = inventory.windows_packages({"dependencies": {"net10": {"Package": {"resolved": "2.0", "contentHash": "sha"}}}},
            {"builtin-baseline": "a" * 40, "dependencies": [{"features": ["avcodec"]}]})
        self.assertEqual("2.0", result[0]["version"])
        self.assertEqual("a" * 40, result[1]["registryBaseline"])

    def test_gradle_relocation_preserves_the_actual_selected_coordinates(self):
        result = inventory.gradle_packages("+--- example.old:module:1.0 -> example.new:replacement:2.0")
        self.assertEqual([{"group": "example.new", "name": "replacement", "version": "2.0"}], result)

    def test_unresolved_dependencies_cannot_be_laundered_into_an_inventory(self):
        with self.assertRaises(ValueError): inventory.gradle_packages("+--- example:module:1.0 FAILED")

    def test_bundle_inventory_covers_compiled_ui_and_configuration(self):
        with tempfile.TemporaryDirectory(prefix="GreaterArt-trust-bundle-") as temporary:
            folder = Path(temporary)
            (folder / "MainWindow.xbf").write_bytes(b"compiled UI fixture")
            (folder / "GreaterArt.WinUI.runtimeconfig.json").write_text("{}")
            first = trust.winui_bundle(folder)
            self.assertIn("MainWindow.xbf", first)
            self.assertIn("GreaterArt.WinUI.runtimeconfig.json", first)
            (folder / "MainWindow.xbf").write_bytes(b"different compiled UI fixture")
            self.assertNotEqual(first["MainWindow.xbf"], trust.winui_bundle(folder)["MainWindow.xbf"])


if __name__ == "__main__":
    unittest.main()
