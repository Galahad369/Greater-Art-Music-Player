#!/usr/bin/env python3
"""Issue #24: disposable offline Git-history rewrite rehearsal; NEVER PUSHES.

Requires an existing full bare mirror (which remains unchanged), git-filter-repo,
and a new, nonexistent output path. Sanitized summary only; no raw identities.
"""
import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

# Compatible with Git grep POSIX ERE and Python bytes-regex in git-filter-repo.
# Account segments can contain spaces/Unicode; drive letters are not C-only.
# Split Linux root literals so content self-scans do not match their own code.
PATTERNS = [
    r'[A-Za-z]:[\\/]+[Uu][Ss][Ee][Rr][Ss][\\/]+[^\\/]+[\\/]',
    r'/[Uu]sers/[^/]+/',
    '/' + 'home/' + r'[^/]+/',
]
GREP_PATTERN = '(' + '|'.join(PATTERNS) + ')'

def git(repo, *args, check=True):
    p = subprocess.run(['git', '-C', str(repo), *args],
                       capture_output=True, text=True, errors='replace')
    if check and p.returncode:
        raise RuntimeError('Git command failed: ' + args[0])
    return p

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
        if result.returncode > 1:
            raise RuntimeError('History scanner encountered a Git error')
        if result.returncode == 0:
            for hit in result.stdout.splitlines():
                affected.add(hit.partition(':')[2])
    return len(commits), len(affected)

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--mirror', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    ap.add_argument('--expected-main', default=None)
    args = ap.parse_args()
    source = args.mirror.resolve(strict=True)
    output = args.output.absolute()
    if output.exists() or source == output or source in output.parents or output in source.parents:
        raise RuntimeError('Output must be new and separate from the protected input')
    if git(source, 'rev-parse', '--is-bare-repository').stdout.strip() != 'true':
        raise RuntimeError('Input must be a bare mirror')
    before = refs(source)
    if args.expected_main and before['refs/heads/main'] != args.expected_main:
        raise RuntimeError('Main HEAD changed; refresh inventory and retry safely')
    before_apks = release_blobs(source, before)
    old_commit_count, old_path_count = scan(source)
    if old_path_count == 0:
        raise RuntimeError('Source has no detected historical paths; no rewrite attempted')
    if not shutil.which('git-filter-repo'):
        raise RuntimeError('Missing git-filter-repo; original mirror is unchanged')
    clone = subprocess.run(['git', 'clone', '--mirror', '--no-local',
                            str(source), str(output)], capture_output=True)
    if clone.returncode:
        raise RuntimeError('Cannot create isolated destination mirror')
    with tempfile.TemporaryDirectory(prefix='greater-art-private-replacements-') as tmp:
        rule_file = Path(tmp) / 'rules.txt'
        rule_file.write_text(''.join(
            'regex:' + p + '==>[REDACTED_HOME]/\n' for p in PATTERNS),
            encoding='utf-8')
        rewrite = subprocess.run(
            ['git', '-C', str(output), 'filter-repo', '--force',
             '--replace-text', str(rule_file), '--replace-message', str(rule_file),
             '--replace-refs', 'delete-no-add'],
            capture_output=True)
        if rewrite.returncode:
            raise RuntimeError('Disposable rewrite failed; input not changed')
    after = refs(output)
    if set(before) != set(after):
        raise RuntimeError('Rewrite changed branch/tag/ref identities')
    if before_apks != release_blobs(output, after):
        raise RuntimeError('Release APK blob identities changed')
    new_commit_count, new_path_count = scan(output)
    if new_path_count:
        raise RuntimeError('Workstation home paths remain in rewritten history')
    if git(output, 'fsck', '--full', '--no-reflogs', check=False).returncode:
        raise RuntimeError('Rewritten Git object graph failed integrity checks')
    result = {
        'input_ref_count': len(before),
        'rewritten_ref_count': sum(before[k] != after[k] for k in before),
        'before_commit_count': old_commit_count,
        'after_commit_count': new_commit_count,
        'affected_paths_before': old_path_count,
        'affected_paths_after': new_path_count,
        'apk_blob_checks': sum(map(len, before_apks.values())),
        'main_changed': before['refs/heads/main'] != after['refs/heads/main'],
        'v1_5_0_tag_changed': before['refs/tags/v1.5.0'] != after['refs/tags/v1.5.0'],
        'ready_to_publish': False,
    }
    print(json.dumps(result, indent=2))
    if not result['main_changed'] or not result['v1_5_0_tag_changed']:
        raise RuntimeError('Main or protected annotated tag was not rewritten')
    print('[OK] Disposable full-ref rehearsal succeeded; nothing published')

if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError) as error:
        print('[BLOCKED] ' + str(error), file=sys.stderr)
        sys.exit(1)
