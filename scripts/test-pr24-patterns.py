#!/usr/bin/env python3
"""Synthetic Python string/bytes and Git ERE checks. Makes NO Git mutations."""
import re
import runpy
import subprocess
import tempfile
import unittest
from pathlib import Path

patterns = runpy.run_path(str(Path(__file__).with_name('pr24-path-patterns.py')))
python_pattern = re.compile('|'.join(patterns['PATTERNS']))
byte_pattern = re.compile(('|'.join(patterns['PATTERNS'])).encode('ascii'))
windows_root = 'Z:' + '\\' + 'Users' + '\\'
mac_root = '/' + 'Users' + '/'
linux_root = '/' + 'home' + '/'


class PathPatternTests(unittest.TestCase):
    def test_supported_accounts_in_both_engines(self):
        fixtures = [windows_root + 'Synthetic Person' + '\\Documents\\',
                    'c:/' + 'users/' + 'synthetic-person/Documents/',
                    mac_root + 'Synthetic Person/Documents/',
                    linux_root + 'synthetic-person/Documents/',
                    windows_root + '測試人' + '\\Documents\\',
                    mac_root + '測試人/Documents/',
                    linux_root + '測試人/Documents/']
        with tempfile.TemporaryDirectory(prefix='ga-pr24-synthetic-') as temporary:
            for index, value in enumerate(fixtures):
                with self.subTest(index=index):
                    self.assertIsNotNone(python_pattern.search(value))
                    self.assertIsNotNone(byte_pattern.search(value.encode('utf-8')))
                    fixture = Path(temporary) / 'fixture.txt'
                    fixture.write_bytes(('Synthetic: ' + value + '\r\n').encode('utf-8'))
                    result = subprocess.run(['git', '-c', 'core.fsmonitor=false', 'grep', '--no-index',
                        '-I', '-q', '-E', patterns['GREP_PATTERN'], '--', 'fixture.txt'],
                        cwd=temporary, capture_output=True)
                    self.assertEqual(0, result.returncode, 'Git ERE missed a supported synthetic account')

    def test_python_replacement_preserves_following_lines(self):
        for root, suffix in ((windows_root, '\\keep.txt'), (mac_root, '/keep.txt'), (linux_root, '/keep.txt')):
            for newline in ('\n', '\r\n', '\r'):
                with self.subTest(root=root[:1], newline=repr(newline)):
                    # A truncated first line must not eat a later path separator.
                    text = root + 'incomplete' + newline + 'following-line' + suffix + newline
                    self.assertEqual(text, python_pattern.sub('[REDACTED_HOME]/', text))
                    raw = text.encode('utf-8')
                    self.assertEqual(raw, byte_pattern.sub(b'[REDACTED_HOME]/', raw))
                    complete = root + '測試人' + suffix + newline + 'keep this exact line' + newline
                    output = python_pattern.sub('[REDACTED_HOME]/', complete)
                    self.assertEqual(complete.count('\r'), output.count('\r'))
                    self.assertEqual(complete.count('\n'), output.count('\n'))
                    self.assertTrue(output.endswith('keep this exact line' + newline))
                    self.assertNotIn('測試人', output)

    def test_negative_paths_and_truncated_lines(self):
        fixtures = ['C:/Windows/System32/', '/var/data/file', '/tmp/Users/foo', 'docs/project/local',
                    windows_root + 'incomplete\r\nnext-line/file',
                    mac_root + 'incomplete\r\nnext-line/file',
                    linux_root + 'incomplete\r\nnext-line/file']
        with tempfile.TemporaryDirectory(prefix='ga-pr24-negative-') as temporary:
            fixture = Path(temporary) / 'fixture.txt'
            for index, value in enumerate(fixtures):
                with self.subTest(index=index):
                    self.assertIsNone(python_pattern.search(value))
                    fixture.write_bytes(value.encode('utf-8'))
                    result = subprocess.run(['git', '-c', 'core.fsmonitor=false', 'grep', '--no-index',
                        '-I', '-q', '-E', patterns['GREP_PATTERN'], '--', 'fixture.txt'],
                        cwd=temporary, capture_output=True)
                    self.assertEqual(1, result.returncode, 'Git ERE false positive or operational failure')


if __name__ == '__main__':
    unittest.main()
