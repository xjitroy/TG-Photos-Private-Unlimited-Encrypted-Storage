#!/usr/bin/env python3
"""Keep policy checks on every push; avoid Android recompilation for documentation-only diffs."""
import os
from pathlib import Path
import re
import subprocess


def requires_android(base, event):
    # Missing/unknown history, new branches and manual runs must receive complete checks.
    if event == 'workflow_dispatch' or not re.fullmatch(r'[0-9a-fA-F]{40}', base or '') or \
            base == '0' * 40:
        return True
    try:
        changed = subprocess.check_output(['git', 'diff', '--name-only', '-z', base, 'HEAD'],
                                          stderr=subprocess.DEVNULL).decode().split('\0')
    except (subprocess.CalledProcessError, UnicodeError):
        return True
    documents = {'README.md', 'PLAN.md', 'AGENTS.md', 'NOTICE', 'LICENSE'}
    return any(path and path not in documents and not path.startswith('docs/') for path in changed)


if __name__ == '__main__':
    required = requires_android(os.environ.get('TGPHOTOS_BASE_SHA'), os.environ.get('GITHUB_EVENT_NAME'))
    print('Android compilation required: ' + str(required).lower())
    output = os.environ.get('GITHUB_OUTPUT')
    if output:
        with Path(output).open('a') as stream:
            stream.write('android=' + str(required).lower() + '\n')
