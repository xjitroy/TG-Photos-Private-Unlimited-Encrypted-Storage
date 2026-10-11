#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .toolchain/media-checks
java com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options -d .toolchain/media-checks \
    core/crypto/src/main/java/app/tgphotos/core/crypto/AeadEnvelope.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/ChunkContext.java \
    core/backup/src/main/java/app/tgphotos/core/backup/AuthenticatedManifest.java \
    core/backup/src/main/java/app/tgphotos/core/backup/EncryptedUploadJournal.java \
    core/backup/src/main/java/app/tgphotos/core/backup/EncryptedBackupCoordinator.java \
    core/media/src/main/java/app/tgphotos/core/media/EncryptedBackupMediaSession.java \
    core/media/src/main/java/app/tgphotos/core/media/AuthenticatedRangeReader.java \
    core/media/src/test/java/app/tgphotos/core/media/EncryptedMediaPipelineChecks.java \
    core/media/src/test/java/app/tgphotos/core/media/AuthenticatedRangeChecks.java
java -Xmx256m -cp .toolchain/media-checks app.tgphotos.core.media.AuthenticatedRangeChecks

java -Xmx256m -cp .toolchain/media-checks app.tgphotos.core.media.EncryptedMediaPipelineChecks
