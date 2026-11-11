"""Exercise the delivery decisions without requiring Android or a private key."""

import argparse
import importlib.util
import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location("android_gate", Path(__file__).with_name("greater-art-android-gate.py"))
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


class AndroidGateTests(unittest.TestCase):
    def setUp(self):
        parent = Path(tempfile.gettempdir()).resolve()
        owned = tempfile.TemporaryDirectory(prefix="greater-art-gate-test-", dir=parent)
        self.root = Path(owned.name).resolve()
        if self.root.parent != parent or not self.root.name.startswith("greater-art-gate-test-"):
            raise RuntimeError("Fixture cleanup would leave the owned test directory")
        self.addCleanup(owned.cleanup)
        self.repo = self.root / "repo"
        self.out = self.root / "evidence"
        self.revision = {"version": "1.22.15", "code": 336, "committed": True, "commit": "fixture", "sha256": "fixture"}

    def test_assembly_runs_when_signing_key_is_missing(self):
        def assemble(argv, *, cwd, env, log, timeout):
            self.assertIn(":app:assembleRelease", argv)
            self.assertTrue(all(name not in env for name in gate.SIGNING_ENV))
            for name in ("app/build/outputs/apk/release/app-release-unsigned.apk",
                         "app/build/outputs/mapping/release/mapping.txt"):
                file = cwd / name
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_bytes(b"fixture")
            return ""

        args = argparse.Namespace(out=self.out, preflight_only=False, windows_trusted_roots=False)
        with patch.object(gate, "ROOT", self.repo), patch.object(gate, "source", return_value=(self.revision, {})), \
                patch.object(gate, "preflight", return_value=(self.root / "java", self.root / "sdk", self.root / "missing-key", [])), \
                patch.object(gate, "run", side_effect=assemble) as command:
            stages = gate.build(args)["stages"]
        command.assert_called_once()
        self.assertEqual(stages["assembly"]["status"], "PASS")
        self.assertEqual(stages["signing"]["status"], "BLOCKED")
        self.assertEqual(stages["device_smoke"]["status"], "BLOCKED")

    def test_candidate_requires_device_result_and_exact_bytes(self):
        self.out.mkdir()
        apk = self.out / "GreaterArt-1.22.15.apk"
        apk.write_bytes(b"expected candidate")
        receipt = self.out / "build.json"
        result = {"repo": str(self.repo), "variant": "release", "source": self.revision,
                  "apk": {"path": str(apk), "sha256": gate.sha256(apk)},
                  "stages": {name: gate.stage("PASS") for name in ("assembly", "signing", "identity")}}
        result["stages"]["device_smoke"] = gate.stage("BLOCKED")
        gate.record(receipt, result)
        with patch.object(gate, "ROOT", self.repo), patch.object(gate, "source", return_value=(self.revision, {})):
            with self.assertRaisesRegex(ValueError, "device smoke must pass"):
                gate.exact_candidate(receipt, completed=True)
            result["stages"]["device_smoke"] = gate.stage("PASS")
            device = self.out / "device.json"
            gate.record(device, result)
            self.assertEqual(gate.exact_candidate(device, completed=True)[1], apk)
            apk.write_bytes(b"changed bytes")
            with self.assertRaisesRegex(ValueError, "no longer matches"):
                gate.exact_candidate(device, completed=True)

    def test_device_retry_reuses_candidate_and_preserves_blocked_result(self):
        self.out.mkdir()
        receipt = self.out / "build.json"
        receipt.touch()
        adb = self.root / "adb"
        args = argparse.Namespace(receipt=receipt, adb=adb, serial="emulator-5554", adapter=["smoke-adapter"])
        digest = "a" * 64
        build = {"source": self.revision, "apk": {"sha256": digest}, "stages": {}}

        def native(argv, *, env=None, log=None, timeout=120):
            if log:
                line = f"A55_EXACT_OPTIMIZED_PASS sha256={digest}\n"
                log.write_text(line, encoding="utf-8")
                return line
            if argv[-1] == "name":
                return "GreaterArt_A55_API36\n"
            if argv[-1] == gate.PACKAGE:
                return "package:/data/app/~~fixture/base.apk\n"
            return f"{digest}  /data/app/~~fixture/base.apk\n"

        with patch.object(gate, "exact_candidate", side_effect=lambda *_, **__: (
                {**build, "stages": {}}, self.out / "candidate.apk")), patch.object(gate, "run", side_effect=native):
            blocked = gate.smoke(args)
            adb.touch()
            retried = gate.smoke(args)
        self.assertEqual(blocked["stages"]["device_smoke"]["status"], "BLOCKED")
        self.assertEqual(retried["stages"]["device_smoke"]["status"], "PASS")
        self.assertNotEqual(retried["device_receipt"], blocked["device_receipt"])


if __name__ == "__main__":
    unittest.main()
