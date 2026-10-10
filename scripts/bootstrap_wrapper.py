#!/usr/bin/env python3
"""Fetch the official pinned wrapper only; never compile or package an APK."""
import hashlib
from pathlib import Path
import urllib.request
import urllib.error

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "gradle/wrapper/gradle-wrapper.jar"
SHA256 = "81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f"
URL = "https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar"

def main():
    if TARGET.exists():
        data = TARGET.read_bytes()
    else:
        try:
            with urllib.request.urlopen(URL, timeout=15) as response:
                data = response.read(1024 * 1024)
        except (urllib.error.URLError, OSError):
            raise SystemExit("Wrapper download blocked by network/toolchain environment. No binary was written.")
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit("Wrapper checksum mismatch; refusing to write or execute it")
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_bytes(data)
    print("Verified Gradle 8.13 wrapper installed. No build was performed.")

if __name__ == "__main__":
    main()
