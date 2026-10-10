#!/usr/bin/env python3
"""Exact-hash, source-bound native fixture for account-free Android tests only. No network or keys."""
import hashlib
import json
from pathlib import Path
import shutil
import re
import sys

spec = json.loads(Path('docs/native-runtime-fixture.json').read_text())
builder = Path('scripts/build_tdlib_android.sh').read_text()
for variable, key in [('TGPHOTOS_TD_COMMIT', 'tdlib'), ('TGPHOTOS_SSL_COMMIT', 'openssl')]:
    pin = re.search(rf'{variable}=([a-f0-9]{{40}})', builder)
    if pin is None or pin.group(1) != spec[key]:
        raise SystemExit('Native source pin changed; rebuild and review runtime fixture')
source = Path(sys.argv[1])
metadata = source / 'provenance.json'
if metadata.is_symlink() or metadata.stat().st_size > 131072:
    raise SystemExit('Invalid native fixture provenance')
provenance = json.loads(metadata.read_text())
for key in ['abi', 'tdlib', 'openssl', 'ndk', 'android_api']:
    if provenance.get(key) != spec[key]:
        raise SystemExit('Native fixture source/toolchain mismatch')
if provenance.get('builder_source_commit') != spec['builderSourceCommit']:
    raise SystemExit('Native fixture producer mismatch')
for name, expected in spec['sourceSha256'].items():
    if hashlib.sha256(Path(name).read_bytes()).hexdigest() != expected:
        raise SystemExit('JNI source changed; rebuild and review fixture before testing')
if spec['abi'] != 'x86_64' or {r['path'] for r in spec['libraries']} != {
    'jniLibs/x86_64/libtdjson.so', 'jniLibs/x86_64/libtgphotos_tdjson.so',
}:
    raise SystemExit('Unexpected native fixture contents')
validated = []
for record in spec['libraries']:
    file = source / record['path']
    if any(parent.is_symlink() for parent in [source, source / 'jniLibs', file.parent, file]) or \
            file.stat().st_size != record['bytes'] or \
            hashlib.sha256(file.read_bytes()).hexdigest() != record['sha256']:
        raise SystemExit('Native fixture binary integrity mismatch')
    if not any(r == record or all(r.get(k) == record[k] for k in record)
               for r in provenance.get('libraries', [])):
        raise SystemExit('Native fixture binary provenance mismatch')
    validated.append(file)
destination = Path('app/src/androidTest/jniLibs/x86_64')
if destination.parent.exists():
    raise SystemExit('Instrumentation fixture destination must start empty')
destination.mkdir(parents=True)
for file in validated:
    shutil.copyfile(file, destination / file.name)
print('PASS: exact source-bound native fixture copied to androidTest only; no production app enablement')
