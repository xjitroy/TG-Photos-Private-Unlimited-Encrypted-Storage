package app.tgphotos.core.media;

import app.tgphotos.core.backup.AuthenticatedManifest;
import app.tgphotos.core.backup.EncryptedBackupCoordinator;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Connects actual authenticated remote backup descriptors to bounded gallery/video/document reads.
 * Caller must supply the same owner-authorized transport as the coordinator and unlocked scoped keys.
 * Lock closes every reader and prevents pending manifest fetches from publishing a new reader.
 * This is not full restore proof, biometric authentication or an Android lifecycle integration.
 */
public final class EncryptedBackupMediaSession implements AutoCloseable {
    private final EncryptedBackupCoordinator coordinator;
    private final EncryptedBackupCoordinator.Transport transport;
    private final EncryptedBackupCoordinator.Commit commit;
    private final byte[] fileKey;
    private final byte[] manifestKey;
    private final Set<AuthenticatedRangeReader> readers = new HashSet<>();
    private int opening;
    private boolean closed;

    public EncryptedBackupMediaSession(EncryptedBackupCoordinator coordinator, EncryptedBackupCoordinator.Transport transport,
            EncryptedBackupCoordinator.Commit commit, byte[] unlockedFileKey, byte[] unlockedManifestKey) {
        if (coordinator == null || transport == null || commit == null || unlockedFileKey == null || unlockedFileKey.length != 32 ||
                unlockedManifestKey == null || unlockedManifestKey.length != 32 || Arrays.equals(unlockedFileKey, unlockedManifestKey))
            throw new IllegalArgumentException("Invalid unlocked backup media session");
        this.coordinator = coordinator; this.transport = transport; this.commit = commit;
        fileKey = unlockedFileKey.clone(); manifestKey = unlockedManifestKey.clone();
    }

    public AuthenticatedRangeReader openReader() throws IOException, GeneralSecurityException {
        byte[] operationFileKey, operationManifestKey;
        synchronized (this) {
            requireOpen();
            readers.removeIf(AuthenticatedRangeReader::isClosed);
            if (readers.size() + opening >= 8) throw new IOException("Too many active authenticated media readers");
            opening++;
            operationFileKey = fileKey.clone(); operationManifestKey = manifestKey.clone();
        }
        AuthenticatedRangeReader reader = null;
        try {
            try (AuthenticatedManifest manifest = coordinator.openManifest(commit, operationManifestKey)) {
                String[] pointers = manifest.chunkPointers();
                int[] lengths = manifest.chunkLengths();
                reader = new AuthenticatedRangeReader(commit.fileId, commit.version, lengths, operationFileKey,
                        (index, maximum) -> transport.downloadCiphertext(pointers[index], maximum));
            }
            synchronized (this) {
                requireOpen(); readers.add(reader);
                AuthenticatedRangeReader published = reader; reader = null; return published;
            }
        } finally {
            if (reader != null) reader.close();
            Arrays.fill(operationFileKey, (byte) 0); Arrays.fill(operationManifestKey, (byte) 0);
            synchronized (this) { opening--; }
        }
    }
    private void requireOpen() throws IOException { if (closed) throw new IOException("Encrypted backup media session locked"); }
    public synchronized boolean isClosed() { return closed; }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true; Arrays.fill(fileKey, (byte) 0); Arrays.fill(manifestKey, (byte) 0);
        for (AuthenticatedRangeReader reader : readers) reader.close();
        readers.clear();
    }
    @Override public String toString() { return "EncryptedBackupMediaSession(redacted)"; }
}
