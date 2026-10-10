#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .toolchain/crypto-checks docs/evidence
# The installed runtime includes jdk.compiler even though javac is absent from PATH.
java com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options -d .toolchain/crypto-checks \
    core/crypto/src/main/java/app/tgphotos/core/crypto/AeadEnvelope.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/ChunkContext.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/RootKeyEnvelope.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/HkdfSha256.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/ScopedKeyHierarchy.java \
    core/crypto/src/test/java/app/tgphotos/core/crypto/CryptoPrimitiveChecks.java
java -Xmx256m -cp .toolchain/crypto-checks app.tgphotos.core.crypto.CryptoPrimitiveChecks
