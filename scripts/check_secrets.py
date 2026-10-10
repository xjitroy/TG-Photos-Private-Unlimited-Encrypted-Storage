#!/usr/bin/env python3
"""Check the Git index for private configuration and known registration values, without printing them."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
paths = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
config = ROOT / ".private/telegram.properties"
values = {}
if config.exists():
    values = dict(line.split("=", 1) for line in config.read_text().splitlines() if "=" in line)
known = [value.encode() for key, value in values.items() if key in {"telegram.api_hash", "telegram.api_id"} and value]
for path in filter(None, paths):
    if path.startswith((".private/", ".toolchain/")) or path.endswith((".apk", ".aab", ".jks", ".keystore")):
        raise SystemExit(f"Private/unapproved artifact found in Git index: {path}")
    data = subprocess.check_output(["git", "show", f":{path}"], cwd=ROOT)
    if any(secret in data for secret in known):
        raise SystemExit(f"Known private registration value found in staged file: {path}")
print("PASS: Git index excludes private configuration, known registration values and unapproved artifacts.")
