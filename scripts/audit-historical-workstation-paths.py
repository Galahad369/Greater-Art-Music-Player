#!/usr/bin/env python3
"""Read-only historical path audit. Prints counts, never matched content or user identities.

Exit 1 means detected historical paths. Exit 2 means an operational error.
Does not replace or weaken the repository's existing required security gate.
"""
import argparse
import json
import runpy
import sys
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', type=Path, default=Path('.'))
    args = parser.parse_args()
    helpers = runpy.run_path(str(Path(__file__).with_name('pr24-rehearse-history.py')))
    commits, paths = helpers['scan'](args.repository.resolve(strict=True))
    print(json.dumps({'reachable_commits': commits, 'affected_path_count': paths,
                      'history_rewritten': False, 'published': False}))
    return 1 if paths else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, OSError):
        # Subprocess stderr and private paths must not reach public logs.
        print('[BLOCKED] Historical audit could not complete; no changes made', file=sys.stderr)
        sys.exit(2)
