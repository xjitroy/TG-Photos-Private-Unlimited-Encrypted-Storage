#!/usr/bin/env python3
"""Recover owner-provided documents and embedded design images from the committed archive."""
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "docs/reference/TG_Photos_Master_Plan_Source.zip"
OUTPUT = ROOT / ".toolchain/reference"
provenance = json.loads((ROOT / "docs/source-provenance.json").read_text())
documents = {name: digest for name, digest in provenance.items() if name != "embedded_images"}
images = {item["filename"]: item["sha256"] for item in provenance["embedded_images"]}

# Only known filenames can be written; archive paths are never used as output paths.
with ZipFile(ARCHIVE) as archive:
    if set(archive.namelist()) != set(documents):
        raise SystemExit("Unexpected reference archive entries")
    payloads = {name: archive.read(name) for name in documents}
for name, data in payloads.items():
    if sha256(data).hexdigest() != documents[name]:
        raise SystemExit(f"Document checksum mismatch: {name}")
with ZipFile(BytesIO(payloads["TG_Photos_E2EE_Master_Plan_BN.docx"])) as docx:
    embedded = {name: docx.read(f"word/media/{name}") for name in images}
for name, data in embedded.items():
    if sha256(data).hexdigest() != images[name]:
        raise SystemExit(f"Design checksum mismatch: {name}")
OUTPUT.mkdir(parents=True, exist_ok=True)
(OUTPUT / "images").mkdir(exist_ok=True)
for name, data in payloads.items():
    (OUTPUT / name).write_bytes(data)
for name, data in embedded.items():
    (OUTPUT / "images" / name).write_bytes(data)
print(f"PASS: verified and extracted {len(payloads)} original documents and {len(embedded)} design images to {OUTPUT}")
