#!/usr/bin/env python3
"""Record stripped native build hashes and verify ELF 16KB alignment; not runtime/device validation."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys

root, abi, ndk = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
reader = ndk / "toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
files = []
for name in ["libtdjson.so", "libtgphotos_tdjson.so"]:
    path = root / "jniLibs" / abi / name
    output = subprocess.check_output([str(reader), "--program-headers", "--wide", str(path)], text=True)
    segments = [line.split() for line in output.splitlines() if line.strip().startswith("LOAD ")]
    if not segments or any(int(line[-1], 16) < 16384 for line in segments):
        raise SystemExit(f"16KB ELF segment alignment failed: {name}")
    files.append({"path": str(path.relative_to(root)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                  "bytes": path.stat().st_size})
manifest = {"abi": abi, "android_api": 26, "ndk": "28.2.13676358", "tdlib": "c15d3f5a5de6e3ba5839822c451152e5e18bb700",
            "openssl": "45e844fa2a14ec92d146bd8f5778ac130b6625fb", "openssl_release": "3.5.9", "libraries": files,
            "scope": "Native compilation/ELF checks only; no APK or device/live Telegram test"}
(root / "provenance.json").write_text(json.dumps(manifest, indent=2) + "\n")
print(f"PASS: {abi} native library hashes and 16KB ELF alignment")
