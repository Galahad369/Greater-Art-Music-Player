"""Offline integration tests against disposable Git repos; never touch the real remote."""
import argparse
import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("safe_sync", Path(__file__).with_name("sync-greater-art.py"))
sync = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sync)


class SafeSyncTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="ga-sync-test-")
        self.addCleanup(self.temp.cleanup)
        base = Path(self.temp.name)
        self.root = base / "working"
        self.root.mkdir()
        sync.run(base, "git", "init", "--bare", str(base / "remote.git"))
        self.g("init", "-b", "main")
        self.g("config", "user.email", "test@example.invalid")
        self.g("config", "user.name", "Offline fixture")
        self.write(sync.RULES, "Latest verified APK: **1.0.2 (code 2)**\n")
        self.old = "greater-art/releases/GreaterArt-1.0.1.apk"
        self.new = "greater-art/releases/GreaterArt-1.0.2.apk"
        self.write(self.old, "immutable old fixture")
        self.write(self.new, "current fixture")
        self.write("source.txt", "original")
        self.commit("baseline")
        self.g("remote", "add", "origin", str(base / "remote.git"))
        self.g("push", "-u", "origin", "main")

    def g(self, *args):
        return sync.git(self.root, *args)

    def write(self, path, text):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8")

    def commit(self, message):
        self.g("add", "-A")
        self.g("commit", "-m", message)

    def delete_old(self):
        self.g("rm", self.old)
        self.commit("apk cleanup")

    def test_deletion_cleanup_is_archived_and_restored_without_deleting_remote_history(self):
        baseline = self.g("rev-parse", "origin/main")
        self.delete_old()
        deleted = self.g("rev-parse", "HEAD")
        self.assertTrue(sync.deletion_only_cleanup(self.root))
        sync.latest_only(self.root)
        sync.repair_cleanup(self.root)
        self.assertEqual(baseline, self.g("rev-parse", "HEAD"))
        self.assertEqual(deleted, self.g("rev-parse", f"refs/archive/apk-cleanup-{deleted}"))
        self.assertFalse((self.root / self.old).exists())
        self.assertTrue((self.root / self.new).is_file())
        self.assertEqual("immutable old fixture", self.g("show", f"HEAD:{self.old}"))
        self.assertEqual("", self.g("status", "--porcelain"))

    def test_mixed_source_work_is_never_automatically_removed(self):
        self.g("rm", self.old)
        self.write("source.txt", "unique work")
        self.commit("mixed")
        head = self.g("rev-parse", "HEAD")
        self.assertFalse(sync.deletion_only_cleanup(self.root))
        with self.assertRaises(RuntimeError):
            sync.repair_cleanup(self.root)
        self.assertEqual(head, self.g("rev-parse", "HEAD"))
        self.assertEqual("unique work", (self.root / "source.txt").read_text())

    def test_reverted_source_edits_still_make_history_unsafe_for_automatic_repair(self):
        self.delete_old()
        self.write("source.txt", "change")
        self.commit("source")
        self.write("source.txt", "original")
        self.commit("revert source")
        self.assertFalse(sync.deletion_only_cleanup(self.root))

    def test_dirty_tree_and_untracked_work_stop_before_any_ref_change(self):
        self.delete_old()
        head = self.g("rev-parse", "HEAD")
        self.write("untracked.txt", "keep me")
        with self.assertRaises(RuntimeError):
            sync.repair_cleanup(self.root)
        self.assertEqual(head, self.g("rev-parse", "HEAD"))
        self.assertTrue((self.root / "untracked.txt").exists())

    def test_latest_only_is_idempotent_and_never_records_apk_deletions(self):
        sync.latest_only(self.root)
        sync.latest_only(self.root)
        self.assertFalse((self.root / self.old).exists())
        self.assertEqual("", self.g("status", "--porcelain"))
        self.assertIn(self.old, self.g("ls-files"))

    def test_custom_sparse_rules_are_preserved(self):
        sync.git(self.root, "sparse-checkout", "set", "--no-cone", "--stdin", input_text="/*\n!/source.txt\n")
        with self.assertRaises(RuntimeError):
            sync.latest_only(self.root)
        self.assertFalse((self.root / "source.txt").exists())

    def test_cleanup_on_feature_branch_is_not_pointer_repaired(self):
        self.g("switch", "-c", "feature")
        self.delete_old()
        with self.assertRaises(RuntimeError):
            sync.repair_cleanup(self.root)
        self.assertEqual("feature", self.g("branch", "--show-current"))

    def test_wrong_remote_cannot_be_pushed(self):
        args = argparse.Namespace(apply=True, latest_apk_only=False, repair_apk_cleanup=False)
        with self.assertRaisesRegex(RuntimeError, "not the expected"):
            sync.sync(self.root, args)

    def test_no_unique_work_is_not_cleanup(self):
        self.assertFalse(sync.deletion_only_cleanup(self.root))


if __name__ == "__main__":
    unittest.main()
