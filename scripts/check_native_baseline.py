#!/usr/bin/env python3
"""Bind reusable native artifacts to reviewed pins and exact binary hashes. No network/account data."""
import hashlib
import json
from pathlib import Path
import re
import sys

mode, abi = sys.argv[1:3]
spec = json.loads(Path('docs/native-baseline.json').read_text())
if abi not in spec['abis']:
    raise SystemExit('Unsupported native ABI')
builder = Path('scripts/build_tdlib_android.sh').read_text()
pins = {name: re.search(rf'{name}=([a-f0-9]{{40}})', builder).group(1)
        for name in ['TGPHOTOS_TD_COMMIT', 'TGPHOTOS_SSL_COMMIT']}
matches = pins['TGPHOTOS_TD_COMMIT'] == spec['tdlib'] and pins['TGPHOTOS_SSL_COMMIT'] == spec['openssl'] and spec['ndk'] in builder
if mode == 'can-reuse':
    raise SystemExit(0 if matches else 1)
if mode != 'verify' or len(sys.argv) != 4 or not matches:
    raise SystemExit('Native baseline does not match current build source pins')
root = Path(sys.argv[3])
provenance_path = root / 'provenance.json'
if provenance_path.is_symlink() or provenance_path.stat().st_size > 131072:
    raise SystemExit('Invalid native producer provenance')
provenance = json.loads(provenance_path.read_text())
for key in ['tdlib', 'openssl', 'ndk', 'android_api']:
    if provenance.get(key) != spec[key]:
        raise SystemExit('Native producer version/toolchain mismatch')
if provenance.get('abi') != abi:
    raise SystemExit('Native producer ABI mismatch')
relative = f'jniLibs/{abi}/libtdjson.so'
records = [record for record in provenance['libraries'] if record['path'] == relative]
file = root / relative
expected = spec['abis'][abi]
if len(records) != 1 or file.is_symlink() or file.stat().st_size != expected['tdjsonBytes'] or \
        records[0]['sha256'] != expected['tdjsonSha256'] or \
        hashlib.sha256(file.read_bytes()).hexdigest() != expected['tdjsonSha256']:
    raise SystemExit('Native baseline binary integrity check failed')
print(f'PASS: {abi} official TDLib/OpenSSL baseline pins, producer metadata and exact binary hash')
