#!/usr/bin/env python3
"""Exercise the read-only audit against an owned disposable Git repository."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

AUDIT = Path(__file__).with_name('audit-historical-workstation-paths.py')


class HistoricalAuditTests(unittest.TestCase):
    def test_clean_deleted_paths_and_non_repository(self):
        with tempfile.TemporaryDirectory(prefix='ga-history-audit-test-') as temporary:
            root = Path(temporary)
            repository = root / 'repository'
            repository.mkdir()

            def git(*args):
                subprocess.run(['git', '-c', 'core.fsmonitor=false', '-C', str(repository), *args],
                               check=True, capture_output=True)

            def audit(directory):
                return subprocess.run([sys.executable, str(AUDIT), '--repository', str(directory)],
                                      capture_output=True, text=True, encoding='utf-8')

            git('init', '-q')
            git('config', 'user.name', 'Synthetic Test')
            git('config', 'user.email', '123456+privacy-test@users.noreply.github.com')
            (repository / 'README.txt').write_text('clean test\n', encoding='utf-8')
            git('add', 'README.txt')
            git('commit', '-q', '-m', 'clean')
            clean = audit(repository)
            self.assertEqual(0, clean.returncode, clean.stderr)
            self.assertEqual(0, json.loads(clean.stdout)['affected_path_count'])

            slash = chr(92)
            fixtures = [
                'Z:' + slash + 'Users' + slash + 'Synthetic Person' + slash + 'Documents' + slash,
                'c:/' + 'users/' + 'synthetic-person/Documents/',
                '/' + 'Users' + '/Synthetic Person/Documents/',
                '/' + 'home' + '/測試人/Documents/',
            ]
            for index, value in enumerate(fixtures):
                (repository / f'synthetic-{index}.log').write_text(value + '\n', encoding='utf-8')
            git('add', '.')
            git('commit', '-q', '-m', 'synthetic paths')
            git('rm', '-q', '--', *[f'synthetic-{index}.log' for index in range(len(fixtures))])
            git('commit', '-q', '-m', 'remove synthetic paths')
            detected = audit(repository)
            self.assertEqual(1, detected.returncode, detected.stderr)
            summary = json.loads(detected.stdout)
            self.assertEqual(4, summary['affected_path_count'])
            self.assertFalse(summary['history_rewritten'])
            self.assertFalse(summary['published'])
            for identity in ['Synthetic Person', 'synthetic-person', '測試人']:
                self.assertNotIn(identity, detected.stdout + detected.stderr)
            failed = audit(root)
            self.assertEqual(2, failed.returncode)
            self.assertNotIn(str(root), failed.stdout + failed.stderr)


if __name__ == '__main__':
    unittest.main()
