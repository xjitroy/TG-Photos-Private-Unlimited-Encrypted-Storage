package app.tgphotos.core.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

/** Draft hierarchy requiring independently generated cloud and vault roots; never serialize roots. */
public final class ScopedKeyHierarchy implements AutoCloseable {
    public enum Domain { CLOUD, VAULT }
    public enum Purpose { CONTENT, MANIFEST, THUMBNAIL, CACHE, METADATA }
    private final byte[] cloudRoot;
    private final byte[] vaultRoot;
    private final byte[] salt;
    private boolean closed;

    public ScopedKeyHierarchy(byte[] cloudRoot, byte[] vaultRoot, byte[] salt) {
        if (cloudRoot == null || cloudRoot.length != 32 || vaultRoot == null || vaultRoot.length != 32 ||
                salt == null || salt.length != 32 || MessageDigest.isEqual(cloudRoot, vaultRoot)) {
            throw new IllegalArgumentException("Independent 256-bit roots and 256-bit salt required");
        }
        this.cloudRoot = cloudRoot.clone();
        this.vaultRoot = vaultRoot.clone();
        this.salt = salt.clone();
    }

    public synchronized byte[] derive(Domain domain, Purpose purpose, UUID objectId, long version)
            throws GeneralSecurityException {
        if (closed) throw new IllegalStateException("Key hierarchy is locked");
        if (domain == null || purpose == null || objectId == null || version <= 0) {
            throw new IllegalArgumentException("Invalid key scope");
        }
        byte[] label = ("TGPhotos/Key/v1\u0000" + domain.name() + "\u0000" + purpose.name() + "\u0000")
                .getBytes(StandardCharsets.US_ASCII);
        byte[] info = ByteBuffer.allocate(label.length + 24).put(label)
                .putLong(objectId.getMostSignificantBits()).putLong(objectId.getLeastSignificantBits())
                .putLong(version).array();
        return HkdfSha256.derive(domain == Domain.CLOUD ? cloudRoot : vaultRoot, salt, info, 32);
    }

    @Override public synchronized void close() {
        Arrays.fill(cloudRoot, (byte) 0);
        Arrays.fill(vaultRoot, (byte) 0);
        Arrays.fill(salt, (byte) 0);
        closed = true;
    }

    @Override public String toString() { return "ScopedKeyHierarchy(redacted)"; }
}
