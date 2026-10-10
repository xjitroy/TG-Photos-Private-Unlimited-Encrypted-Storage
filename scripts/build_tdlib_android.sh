#!/usr/bin/env bash
set -euo pipefail
# Native libraries only: no application package, credentials, user data or signing.
TGPHOTOS_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TGPHOTOS_ABI="${1:?Specify arm64-v8a or x86_64}"
case "$TGPHOTOS_ABI" in
  arm64-v8a) TGPHOTOS_SSL_TARGET=android-arm64 ;;
  x86_64) TGPHOTOS_SSL_TARGET=android-x86_64 ;;
  *) echo 'Unsupported native ABI' >&2; exit 2 ;;
esac
: "${ANDROID_HOME:?Android SDK is required}"
TGPHOTOS_NDK="$ANDROID_HOME/ndk/28.2.13676358"
test -f "$TGPHOTOS_NDK/build/cmake/android.toolchain.cmake"
TGPHOTOS_NATIVE_ROOT="$TGPHOTOS_ROOT/.toolchain/native/$TGPHOTOS_ABI"
TGPHOTOS_TD_COMMIT=c15d3f5a5de6e3ba5839822c451152e5e18bb700
TGPHOTOS_SSL_COMMIT=45e844fa2a14ec92d146bd8f5778ac130b6625fb
mkdir -p "$TGPHOTOS_NATIVE_ROOT"

fetch_source() {
  local source_repo="$1" source_commit="$2" source_dir="$3"
  if [ ! -d "$source_dir/.git" ]; then
    git init "$source_dir"
    git -C "$source_dir" fetch --depth=1 "https://github.com/$source_repo.git" "$source_commit"
    git -C "$source_dir" checkout --detach FETCH_HEAD
  fi
  test "$(git -C "$source_dir" rev-parse HEAD)" = "$source_commit"
  test -z "$(git -C "$source_dir" status --porcelain)"
}
fetch_source tdlib/td "$TGPHOTOS_TD_COMMIT" "$TGPHOTOS_NATIVE_ROOT/tdlib-source"
fetch_source openssl/openssl "$TGPHOTOS_SSL_COMMIT" "$TGPHOTOS_NATIVE_ROOT/openssl-source"

export ANDROID_NDK_ROOT="$TGPHOTOS_NDK"
export PATH="$TGPHOTOS_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"
pushd "$TGPHOTOS_NATIVE_ROOT/openssl-source"
./Configure "$TGPHOTOS_SSL_TARGET" -D__ANDROID_API__=26 no-shared no-tests \
  --prefix="$TGPHOTOS_NATIVE_ROOT/openssl-install" --libdir=lib -fPIC -ffunction-sections -fdata-sections
make -j2
make install_sw
popd

# Follow the official Android example's host-generation/cross-compile split.
cmake -S "$TGPHOTOS_NATIVE_ROOT/tdlib-source/example/android" \
  -B "$TGPHOTOS_NATIVE_ROOT/tdlib-host" -G Ninja \
  -DTD_ANDROID_JSON=ON -DTD_GENERATE_SOURCE_FILES=ON -DCMAKE_BUILD_TYPE=Release
cmake --build "$TGPHOTOS_NATIVE_ROOT/tdlib-host" --parallel 2
cmake -S "$TGPHOTOS_NATIVE_ROOT/tdlib-source/example/android" \
  -B "$TGPHOTOS_NATIVE_ROOT/tdlib-build" -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$TGPHOTOS_NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$TGPHOTOS_ABI" -DANDROID_STL=c++_static -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=RelWithDebInfo -DTD_ANDROID_JSON=ON \
  -DOPENSSL_ROOT_DIR="$TGPHOTOS_NATIVE_ROOT/openssl-install" -DOPENSSL_USE_STATIC_LIBS=TRUE
cmake --build "$TGPHOTOS_NATIVE_ROOT/tdlib-build" --target tdjson --parallel 2
cmake -S "$TGPHOTOS_ROOT/core/telegram/src/main/cpp" \
  -B "$TGPHOTOS_NATIVE_ROOT/jni-build" -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$TGPHOTOS_NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$TGPHOTOS_ABI" -DANDROID_STL=c++_static -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=Release \
  -DTGPHOTOS_TDLIB_SOURCE_DIR="$TGPHOTOS_NATIVE_ROOT/tdlib-source" \
  -DTGPHOTOS_TDLIB_BUILD_DIR="$TGPHOTOS_NATIVE_ROOT/tdlib-build"
cmake --build "$TGPHOTOS_NATIVE_ROOT/jni-build" --parallel 2

mkdir -p "$TGPHOTOS_NATIVE_ROOT/output/jniLibs/$TGPHOTOS_ABI" "$TGPHOTOS_NATIVE_ROOT/output/licenses"
cp "$TGPHOTOS_NATIVE_ROOT/tdlib-build/td/libtdjson.so" \
  "$TGPHOTOS_NATIVE_ROOT/jni-build/libtgphotos_tdjson.so" "$TGPHOTOS_NATIVE_ROOT/output/jniLibs/$TGPHOTOS_ABI/"
cp "$TGPHOTOS_NATIVE_ROOT/tdlib-source/LICENSE_1_0.txt" "$TGPHOTOS_NATIVE_ROOT/output/licenses/TDLib.txt"
cp "$TGPHOTOS_NATIVE_ROOT/openssl-source/LICENSE.txt" "$TGPHOTOS_NATIVE_ROOT/output/licenses/OpenSSL.txt"
for library in "$TGPHOTOS_NATIVE_ROOT/output/jniLibs/$TGPHOTOS_ABI/"*.so; do
  "$TGPHOTOS_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-debug --strip-unneeded "$library"
done
python3 "$TGPHOTOS_ROOT/scripts/check_native_artifacts.py" \
  "$TGPHOTOS_NATIVE_ROOT/output" "$TGPHOTOS_ABI" "$TGPHOTOS_NDK"
