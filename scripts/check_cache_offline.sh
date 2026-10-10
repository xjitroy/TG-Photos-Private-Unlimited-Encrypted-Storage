#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .toolchain/cache-checks
java com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options -d .toolchain/cache-checks \
    core/crypto/src/main/java/app/tgphotos/core/crypto/AeadEnvelope.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/ChunkContext.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/HkdfSha256.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/ScopedKeyHierarchy.java \
    core/cache/src/main/java/app/tgphotos/core/cache/AuthenticatedDiskCache.java \
    core/cache/src/test/java/app/tgphotos/core/cache/DiskCacheChecks.java
java -Xmx256m -cp .toolchain/cache-checks app.tgphotos.core.cache.DiskCacheChecks
