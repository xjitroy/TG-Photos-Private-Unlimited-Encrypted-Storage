#!/bin/bash
set -euo pipefail
# Rebuild the original boundary against an exact verified public TDLib binary; never reuse stale JNI.
TGPHOTOS_ABI="${1:?ABI required}"
TGPHOTOS_BASELINE="${2:?Downloaded verified baseline directory required}"
TGPHOTOS_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TGPHOTOS_BASELINE="$(cd "$TGPHOTOS_BASELINE" && pwd)"
cd "$TGPHOTOS_ROOT"
python3 scripts/check_native_baseline.py verify "$TGPHOTOS_ABI" "$TGPHOTOS_BASELINE"
: "${ANDROID_HOME:?SDK required}"
TGPHOTOS_NDK="$ANDROID_HOME/ndk/28.2.13676358"
TGPHOTOS_NATIVE_ROOT="$TGPHOTOS_ROOT/.toolchain/native/$TGPHOTOS_ABI"
TGPHOTOS_TD_COMMIT=c15d3f5a5de6e3ba5839822c451152e5e18bb700
mkdir -p "$TGPHOTOS_NATIVE_ROOT"
git init "$TGPHOTOS_NATIVE_ROOT/tdlib-source"
git -C "$TGPHOTOS_NATIVE_ROOT/tdlib-source" fetch --depth=1 https://github.com/tdlib/td.git "$TGPHOTOS_TD_COMMIT"
git -C "$TGPHOTOS_NATIVE_ROOT/tdlib-source" checkout --detach FETCH_HEAD
test "$(git -C "$TGPHOTOS_NATIVE_ROOT/tdlib-source" rev-parse HEAD)" = "$TGPHOTOS_TD_COMMIT"
cmake -S "$TGPHOTOS_ROOT/core/telegram/src/main/cpp" -B "$TGPHOTOS_NATIVE_ROOT/jni-build" -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$TGPHOTOS_NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$TGPHOTOS_ABI" -DANDROID_STL=c++_static -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=Release -DTGPHOTOS_TDLIB_SOURCE_DIR="$TGPHOTOS_NATIVE_ROOT/tdlib-source" \
  -DTGPHOTOS_TDLIB_PREBUILT_JSON="$TGPHOTOS_BASELINE/jniLibs/$TGPHOTOS_ABI/libtdjson.so"
cmake --build "$TGPHOTOS_NATIVE_ROOT/jni-build" --parallel 2
mkdir -p "$TGPHOTOS_NATIVE_ROOT/output/jniLibs/$TGPHOTOS_ABI" "$TGPHOTOS_NATIVE_ROOT/output/licenses"
cp "$TGPHOTOS_BASELINE/jniLibs/$TGPHOTOS_ABI/libtdjson.so" "$TGPHOTOS_NATIVE_ROOT/jni-build/libtgphotos_tdjson.so" \
  "$TGPHOTOS_NATIVE_ROOT/output/jniLibs/$TGPHOTOS_ABI/"
cp "$TGPHOTOS_BASELINE/licenses/OpenSSL.txt" "$TGPHOTOS_NATIVE_ROOT/output/licenses/OpenSSL.txt"
cp "$TGPHOTOS_NATIVE_ROOT/tdlib-source/LICENSE_1_0.txt" "$TGPHOTOS_NATIVE_ROOT/output/licenses/TDLib.txt"
cp "$TGPHOTOS_NATIVE_ROOT/tdlib-source/sqlite/sqlite/LICENSE" "$TGPHOTOS_NATIVE_ROOT/output/licenses/TDLib-SQLCipher.txt"
cp "$TGPHOTOS_BASELINE/provenance.json" "$TGPHOTOS_NATIVE_ROOT/output/tdlib-producer-provenance.json"
"$TGPHOTOS_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-debug --strip-unneeded \
  "$TGPHOTOS_NATIVE_ROOT/output/jniLibs/$TGPHOTOS_ABI/libtgphotos_tdjson.so"
python3 scripts/check_native_artifacts.py "$TGPHOTOS_NATIVE_ROOT/output" "$TGPHOTOS_ABI" "$TGPHOTOS_NDK" --record-build
python3 - "$TGPHOTOS_NATIVE_ROOT/output" "$TGPHOTOS_ABI" <<'PY'
import json, sys
from pathlib import Path
root, abi = Path(sys.argv[1]), sys.argv[2]
spec = json.loads(Path('docs/native-baseline.json').read_text())
path = root/'provenance.json'
report = json.loads(path.read_text())
report['tdlib_binary_producer'] = {'run_id': spec['producerRunId'], 'source_commit': spec['producerSourceCommit'],
                                 'sha256': spec['abis'][abi]['tdjsonSha256']}
report['scope'] = 'Original JNI rebuilt with exact previously compiled TDLib/OpenSSL baseline; no APK/device/live test'
path.write_text(json.dumps(report, indent=2)+'\n')
PY
