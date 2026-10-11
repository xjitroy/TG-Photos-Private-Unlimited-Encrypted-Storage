#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .toolchain/backup-checks
java com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options -d .toolchain/backup-checks \
    core/crypto/src/main/java/app/tgphotos/core/crypto/AeadEnvelope.java \
    core/crypto/src/main/java/app/tgphotos/core/crypto/ChunkContext.java \
    core/backup/src/main/java/app/tgphotos/core/backup/EncryptedBackupCoordinator.java \
    core/backup/src/main/java/app/tgphotos/core/backup/EncryptedUploadJournal.java \
    core/backup/src/test/java/app/tgphotos/core/backup/UploadJournalChecks.java \
    core/backup/src/test/java/app/tgphotos/core/backup/BackupProtocolChecks.java
java -Xmx256m -cp .toolchain/backup-checks app.tgphotos.core.backup.BackupProtocolChecks

java -Xmx256m -cp .toolchain/backup-checks app.tgphotos.core.backup.UploadJournalChecks
