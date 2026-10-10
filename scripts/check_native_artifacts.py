#!/usr/bin/env python3
"""Record stripped native build hashes and verify ELF 16KB alignment; not runtime/device validation."""
import hashlib
import json
import os
import re
from pathlib import Path
import subprocess
import sys

root, abi, ndk = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
record_build = len(sys.argv) == 5 and sys.argv[4] == '--record-build'
if len(sys.argv) not in {4, 5} or (len(sys.argv) == 5 and not record_build):
    raise SystemExit('Expected output-dir ABI NDK-dir [--record-build]')
reader = ndk / "toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
files = []
machines = {"arm64-v8a": "AArch64", "x86_64": "Advanced Micro Devices X86-64"}
system_libraries = {"libc.so", "libdl.so", "libm.so", "liblog.so", "libz.so", "libandroid.so"}
exports = {
    "libtdjson.so": {"td_create_client_id", "td_send", "td_receive", "td_execute"},
    "libtgphotos_tdjson.so": {"Java_app_tgphotos_core_telegram_NativeTdJson_" + method
                            for method in ["createClient", "send", "receive", "execute"]},
}
if abi not in machines:
    raise SystemExit("Unsupported native ABI")
for name in ["libtdjson.so", "libtgphotos_tdjson.so"]:
    path = root / "jniLibs" / abi / name
    header = subprocess.check_output([str(reader), "--file-header", str(path)], text=True)
    if "ELF64" not in header or not re.search(r"Type:\s+DYN", header) or machines[abi] not in header:
        raise SystemExit(f"ELF class/type/ABI mismatch: {name}")
    output = subprocess.check_output([str(reader), "--program-headers", "--wide", str(path)], text=True)
    segments = [line.split() for line in output.splitlines() if line.strip().startswith("LOAD ")]
    if not segments or any(int(line[-1], 16) < 16384 or
                           (int(line[1], 16) - int(line[2], 16)) % int(line[-1], 16)
                           for line in segments):
        raise SystemExit(f"16KB ELF segment alignment failed: {name}")
    dynamic = subprocess.check_output([str(reader), "--dynamic", str(path)], text=True)
    soname = re.findall(r"\(SONAME\).*?\[([^\]]+)\]", dynamic)
    if soname != [name]:
        raise SystemExit(f"Android-packagable SONAME mismatch: {name}: {soname}")
    needed = set(re.findall(r"\(NEEDED\).*?\[([^\]]+)\]", dynamic))
    allowed = system_libraries | ({"libtdjson.so"} if name == "libtgphotos_tdjson.so" else set())
    if not needed or needed - allowed or (name == "libtgphotos_tdjson.so" and "libtdjson.so" not in needed):
        raise SystemExit(f"Unresolved/unapproved native dependencies: {name}: {sorted(needed)}")
    symbols = subprocess.check_output([str(reader), "--dyn-syms", "--wide", str(path)], text=True)
    defined = {line.split()[-1] for line in symbols.splitlines()
               if len(line.split()) >= 8 and line.split()[6] != "UND"}
    if not exports[name].issubset(defined):
        raise SystemExit(f"Required public C/JNI symbols missing: {name}")
    files.append({"path": str(path.relative_to(root)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                  "bytes": path.stat().st_size, "needed_libraries": sorted(needed),
                  "verified_exports": sorted(exports[name])})
manifest = {"abi": abi, "android_api": 26, "ndk": "28.2.13676358", "tdlib": "c15d3f5a5de6e3ba5839822c451152e5e18bb700",
            "openssl": "45e844fa2a14ec92d146bd8f5778ac130b6625fb", "openssl_release": "3.5.9", "libraries": files,
            "scope": "Native build/ELF checks only; no APK or device/live Telegram test" if record_build else
                     "Existing artifact ELF validation only; consult producer provenance for compilation/source claims"}
if record_build:
    manifest['builder_source_commit'] = os.environ.get('GITHUB_SHA', 'local build; source commit not provided')
(root / ("provenance.json" if record_build else "elf-review.json")).write_text(json.dumps(manifest, indent=2) + "\n")
print(f"PASS: {abi} native hashes, ELF ABI/16KB alignment, linked dependencies and C/JNI exports")
