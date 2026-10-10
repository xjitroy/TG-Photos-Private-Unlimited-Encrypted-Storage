#!/usr/bin/env python3
"""Check the Git index for private configuration and known registration values, without printing them."""
from pathlib import Path
import subprocess
from io import BytesIO
from zipfile import ZipFile, is_zipfile

ROOT = Path(__file__).resolve().parents[1]
paths = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
config = ROOT / ".private/telegram.properties"
values = {}
if config.exists():
    values = dict(line.split("=", 1) for line in config.read_text().splitlines() if "=" in line)
known = [value.encode() for key, value in values.items() if key in {"telegram.api_hash", "telegram.api_id"} and value]

def check_content(data, label, depth=0):
    if any(secret in data for secret in known):
        raise SystemExit(f"Known private registration value found in staged content: {label}")
    if is_zipfile(BytesIO(data)):
        if depth >= 3:
            raise SystemExit(f"Archive nesting exceeds inspection limit: {label}")
        with ZipFile(BytesIO(data)) as archive:
            if sum(entry.file_size for entry in archive.infolist()) > 64 * 1024 * 1024:
                raise SystemExit(f"Archive exceeds inspection size limit: {label}")
            for entry in archive.infolist():
                if not entry.is_dir():
                    check_content(archive.read(entry), f"{label}/{entry.filename}", depth + 1)

for path in filter(None, paths):
    if path.startswith((".private/", ".toolchain/")) or path.endswith((".apk", ".aab", ".jks", ".keystore")):
        raise SystemExit(f"Private/unapproved artifact found in Git index: {path}")
    data = subprocess.check_output(["git", "show", f":{path}"], cwd=ROOT)
    check_content(data, path)
print("PASS: Git index excludes private configuration, known registration values and unapproved artifacts.")
