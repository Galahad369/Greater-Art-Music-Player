#!/usr/bin/env python3
"""Rehearse a rewrite in a NEW disposable mirror. Never writes to the input or pushes.

Requires a full bare input mirror, git-filter-repo and explicit disposable-copy
confirmation. This is not authorization to rewrite published history or tags.
"""
import argparse
import json
import runpy
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

patterns = runpy.run_path(str(Path(__file__).with_name('pr24-path-patterns.py')))
PATTERNS = patterns['PATTERNS']
GREP_PATTERN = patterns['GREP_PATTERN']


def git(repo, *args, check=True):
    result = subprocess.run(['git', '-c', 'core.fsmonitor=false', '-C', str(repo), *args],
                            capture_output=True, text=True, encoding='utf-8', errors='replace')
    if check and result.returncode:
        raise RuntimeError('Git command failed: ' + args[0])
    return result


def refs(repo):
    found = {}
    for entry in git(repo, 'show-ref').stdout.splitlines():
        sha, name = entry.split(' ', 1)
        found[name] = sha
    if 'refs/heads/main' not in found or 'refs/tags/v1.5.0' not in found:
        raise RuntimeError('Full mirror must contain main and v1.5.0')
    if git(repo, 'cat-file', '-t', found['refs/tags/v1.5.0']).stdout.strip() != 'tag':
        raise RuntimeError('v1.5.0 annotated tag is missing')
    return found


def release_blobs(repo, all_refs):
    snapshots = {}
    for ref in sorted(all_refs):
        if not ref.startswith(('refs/heads/', 'refs/tags/')):
            continue
        items = {}
        for entry in git(repo, 'ls-tree', '-r', '--full-tree', ref).stdout.splitlines():
            if '\t' not in entry:
                continue
            metadata, path = entry.split('\t', 1)
            if path.lower().endswith('.apk'):
                items[path] = metadata.split()[-1]
        snapshots[ref] = items
    return snapshots


def scan(repo):
    commits = git(repo, 'rev-list', '--all').stdout.splitlines()
    affected = set()
    for sha in commits:
        result = git(repo, 'grep', '-I', '-l', '-E', GREP_PATTERN,
                     sha, '--', ':!*.apk', check=False)
        if result.returncode not in (0, 1):
            raise RuntimeError('History scanner encountered a Git error')
        if result.returncode == 0:
            for hit in result.stdout.splitlines():
                affected.add(hit.partition(':')[2])
    return len(commits), len(affected)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mirror', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--expected-main', required=True)
    parser.add_argument('--confirm-disposable-rehearsal', action='store_true')
    args = parser.parse_args()
    if not args.confirm_disposable_rehearsal:
        raise RuntimeError('Explicit disposable-copy confirmation is required; nothing changed')
    source = args.mirror.resolve(strict=True)
    output = args.output.resolve(strict=False)
    if output.exists() or source == output or source in output.parents or output in source.parents:
        raise RuntimeError('Output must be new and separate from the protected input')
    if git(source, 'rev-parse', '--is-bare-repository').stdout.strip() != 'true':
        raise RuntimeError('Input must be a bare mirror')
    before = refs(source)
    if before['refs/heads/main'] != args.expected_main:
        raise RuntimeError('Main changed; refresh inventory and retry safely')
    before_apks = release_blobs(source, before)
    old_commit_count, old_path_count = scan(source)
    if old_path_count == 0:
        raise RuntimeError('No detected historical paths; no rewrite attempted')
    if not shutil.which('git-filter-repo'):
        raise RuntimeError('Missing git-filter-repo; original mirror is unchanged')
    clone = subprocess.run(['git', 'clone', '--mirror', '--no-local', str(source), str(output)], capture_output=True)
    if clone.returncode:
        raise RuntimeError('Cannot create the isolated destination mirror')
    with tempfile.TemporaryDirectory(prefix='greater-art-private-replacements-') as temporary:
        rule_file = Path(temporary) / 'rules.txt'
        rule_file.write_text(''.join('regex:' + pattern + '==>[REDACTED_HOME]/\n' for pattern in PATTERNS), encoding='utf-8')
        rewrite = subprocess.run(['git', '-C', str(output), 'filter-repo', '--force',
            '--replace-text', str(rule_file), '--replace-message', str(rule_file),
            '--replace-refs', 'delete-no-add'], capture_output=True)
        if rewrite.returncode:
            raise RuntimeError('Disposable rewrite failed; input not changed')
    if refs(source) != before:
        raise RuntimeError('Input refs changed during the rehearsal; stop and investigate')
    after = refs(output)
    if set(before) != set(after):
        raise RuntimeError('Rewrite changed branch/tag/ref names')
    if before_apks != release_blobs(output, after):
        raise RuntimeError('Release APK blob identities changed')
    new_commit_count, new_path_count = scan(output)
    if new_path_count:
        raise RuntimeError('Workstation paths remain in the disposable history')
    if git(output, 'fsck', '--full', '--no-reflogs', check=False).returncode:
        raise RuntimeError('Disposable object graph failed integrity checks')
    print(json.dumps({
        'input_ref_count': len(before),
        'rewritten_ref_count': sum(before[key] != after[key] for key in before),
        'before_commit_count': old_commit_count,
        'after_commit_count': new_commit_count,
        'affected_paths_before': old_path_count,
        'affected_paths_after': new_path_count,
        'apk_blob_checks': sum(map(len, before_apks.values())),
        'main_changed': before['refs/heads/main'] != after['refs/heads/main'],
        'v1_5_0_tag_changed': before['refs/tags/v1.5.0'] != after['refs/tags/v1.5.0'],
        'ready_to_publish': False,
    }, indent=2))
    print('[OK] Disposable rehearsal only. Nothing published. Separate owner approval is still required.')


if __name__ == '__main__':
    try:
        main()
    except RuntimeError as error:
        print('[BLOCKED] ' + str(error), file=sys.stderr)
        sys.exit(1)
    except OSError:
        # OS exception text can contain the mirror path or private account name.
        print('[BLOCKED] Filesystem or process access failed; nothing published.', file=sys.stderr)
        sys.exit(1)
