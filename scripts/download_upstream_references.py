#!/usr/bin/env python3
"""Restore checksum-verified public source references. Never executes or adds them to the app."""
import argparse
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / ".toolchain/upstream"
manifest = json.loads((ROOT / "docs/upstream-source-manifest.json").read_text())
parser = argparse.ArgumentParser()
parser.add_argument("--verify-only", action="store_true", help="Check already downloaded files without network access")
args = parser.parse_args()

def verify(data, entry):
    digest = hashlib.sha256(data).hexdigest()
    blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    if len(data) != entry["bytes"] or digest != entry["sha256"] or blob != entry["blob"]:
        raise SystemExit(f"Reference checksum mismatch: {entry['repo']}/{entry['path']}")

for entry in manifest["files"]:
    relative = Path(entry["repo"]) / entry["path"]
    if relative.is_absolute() or ".." in relative.parts:
        raise SystemExit("Invalid reference output path")
    path = OUTPUT / relative
    if not path.resolve().is_relative_to(OUTPUT.resolve()):
        raise SystemExit("Reference output escapes private staging directory")
    if path.exists():
        verify(path.read_bytes(), entry)
        continue
    if args.verify_only:
        raise SystemExit(f"Reference missing: {relative}")
    url = f"https://raw.githubusercontent.com/{entry['repo']}/{entry['commit']}/{entry['path']}"
    # Public pinned source only. No app API configuration, user data or account headers are read.
    with urlopen(Request(url, headers={"User-Agent": "TGPhotos-Reference-Downloader"}), timeout=30) as response:
        data = response.read(2 * 1024 * 1024 + 1)
    verify(data, entry)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
print(f"PASS: {len(manifest['files'])} pinned public reference files verified; application reuse not enabled")
