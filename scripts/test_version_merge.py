import importlib.util
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("guard", Path(__file__).with_name("validate-greater-art-version.py"))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class IntegrationMergeTest(unittest.TestCase):
    def check_merge(self, parents, trees):
        def git(*args):
            if args[0] == "rev-list":
                return "commit " + " ".join(parents)
            return trees[args[1].split("^")[0]]
        with patch.object(guard, "run_git", side_effect=git):
            return guard.is_pure_integration_merge("commit")

    def test_normal_commit_never_exempt(self):
        self.assertFalse(self.check_merge(["a"], {"commit": "same", "a": "same"}))

    def test_exact_second_parent_integration(self):
        self.assertTrue(self.check_merge(["a", "b"], {"commit": "verified", "a": "old", "b": "verified"}))

    def test_conflict_resolution_not_exempt(self):
        self.assertFalse(self.check_merge(["a", "b"], {"commit": "new", "a": "old", "b": "branch"}))


if __name__ == "__main__":
    unittest.main()
