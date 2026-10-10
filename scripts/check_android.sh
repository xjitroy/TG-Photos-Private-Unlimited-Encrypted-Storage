#!/bin/sh
set -eu
# Compilation, JVM tests and lint only. Do not enable APK/AAB approval properties.
cd "$(dirname "$0")/.."
python3 scripts/check_source.py
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
    echo "BLOCKED: verified wrapper JAR is missing; run scripts/bootstrap_wrapper.py on a permitted network." >&2
    exit 2
fi
./gradlew --no-daemon :app:compileDebugKotlin :core:backup:compileDebugKotlin :core:database:compileDebugKotlin :core:media:compileDebugKotlin :core:model:test :core:crypto:test :app:lintDebug :app:lintRelease
