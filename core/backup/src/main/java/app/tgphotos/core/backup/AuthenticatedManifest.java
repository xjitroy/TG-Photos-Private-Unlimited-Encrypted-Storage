package app.tgphotos.core.backup;

import app.tgphotos.core.crypto.AeadEnvelope;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/** Manifest authenticated by the backup coordinator, not evidence of a complete independent restore. */
public final class AuthenticatedManifest implements AutoCloseable {
    public final UUID fileId;
    public final long version;
    public final long totalBytes;
    private final byte[] hash;
    private final byte[] metadata;
    private final int[] lengths;
    private final String[] pointers;
    private boolean closed;

    private AuthenticatedManifest(UUID id, long version, long total, byte[] hash, byte[] metadata,
            int[] lengths, String[] pointers) {
        fileId = id; this.version = version; totalBytes = total;
        this.hash = hash; this.metadata = metadata; this.lengths = lengths; this.pointers = pointers;
    }
    static AuthenticatedManifest parse(byte[] plaintext, EncryptedBackupCoordinator.Commit commit) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(plaintext));
        if (in.readInt() != 1 || !new UUID(in.readLong(), in.readLong()).equals(commit.fileId) ||
                in.readLong() != commit.version || in.readLong() != commit.bytes) throw new IOException("Manifest identity mismatch");
        byte[] hash = new byte[32]; in.readFully(hash);
        int metadataBytes = in.readInt();
        if (metadataBytes < 0 || metadataBytes > 65536 || metadataBytes > in.available()) throw new IOException("Invalid manifest metadata");
        byte[] metadata = new byte[metadataBytes]; in.readFully(metadata);
        boolean retained = false;
        try {
            int count = in.readInt();
            if (count < 0 || count > 100000 || count > in.available() / 7) throw new IOException("Invalid manifest chunk count");
            int[] lengths = new int[count]; String[] pointers = new String[count]; long total = 0;
            for (int index = 0; index < count; index++) {
                int bytes = in.readInt();
                if (bytes < 1 || bytes > AeadEnvelope.MAX_PLAINTEXT_BYTES) throw new IOException("Invalid manifest chunk length");
                lengths[index] = bytes;
                pointers[index] = in.readUTF();
                String pointer = pointers[index];
                if (pointer.isEmpty() || pointer.length() > 1024 || !pointer.matches("[A-Za-z0-9_:/.-]+"))
                    throw new IOException("Invalid opaque manifest pointer");
                total = Math.addExact(total, bytes);
            }
            if (in.available() != 0 || total != commit.bytes) throw new IOException("Manifest total/trailing bytes mismatch");
            AuthenticatedManifest result = new AuthenticatedManifest(commit.fileId, commit.version, total, hash, metadata, lengths, pointers);
            retained = true; return result;
        } finally { if (!retained) { Arrays.fill(metadata, (byte) 0); Arrays.fill(hash, (byte) 0); } }
    }
    public synchronized int[] chunkLengths() { requireOpen(); return lengths.clone(); }
    public synchronized String[] chunkPointers() { requireOpen(); return pointers.clone(); }
    public synchronized byte[] sha256() { requireOpen(); return hash.clone(); }
    /** Caller owns/wipes its returned in-memory metadata copy. No plaintext persistence implied. */
    public synchronized byte[] metadata() { requireOpen(); return metadata.clone(); }
    private void requireOpen() { if (closed) throw new IllegalStateException("Authenticated manifest is closed"); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true; Arrays.fill(hash, (byte) 0); Arrays.fill(metadata, (byte) 0);
        Arrays.fill(lengths, 0); Arrays.fill(pointers, null);
    }
    @Override public String toString() { return "AuthenticatedManifest(redacted)"; }
}
