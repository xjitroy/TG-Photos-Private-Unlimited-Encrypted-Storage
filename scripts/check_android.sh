#!/bin/sh
set -eu
# Compilation, JVM tests and lint only. Do not enable APK/AAB approval properties.
cd "$(dirname "$0")/.."
python3 scripts/check_source.py
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
    echo "BLOCKED: verified wrapper JAR is missing; run scripts/bootstrap_wrapper.py on a permitted network." >&2
    exit 2
fi
./gradlew --no-daemon :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestJavaWithJavac :core:backup:compileDebugKotlin :core:backup:compileDebugJavaWithJavac :core:database:compileDebugKotlin :core:media:compileDebugKotlin :core:media:compileDebugJavaWithJavac :core:cache:classes :core:model:test :core:crypto:test :core:telegram:test :app:lintDebug :app:lintRelease :writeResolvedDependencyInventory
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
tests = failures = errors = skipped = 0
for path in Path('core').glob('*/build/test-results/test/TEST-*.xml'):
    suite = ET.parse(path).getroot()
    tests += int(suite.get('tests', 0)); failures += int(suite.get('failures', 0))
    errors += int(suite.get('errors', 0)); skipped += int(suite.get('skipped', 0))
print(f'JVM test summary: {tests} tests, {failures} failures, {errors} errors, {skipped} skipped')
if not tests or failures or errors:
    raise SystemExit('JVM test verification failed')
PY
