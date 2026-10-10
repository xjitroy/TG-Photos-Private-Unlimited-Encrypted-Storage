package app.tgphotos.core.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

/** Draft hierarchy with an independently locked vault root; never serialize roots. */
public final class ScopedKeyHierarchy implements AutoCloseable {
    public enum Domain { CLOUD, VAULT }
    public enum Purpose { CONTENT, MANIFEST, THUMBNAIL, CACHE, METADATA }
    private final byte[] cloudRoot;
    private byte[] vaultRoot;
    private final byte[] salt;
    private boolean closed;

    /** Cloud operations do not require the private vault to be unlocked. */
    public ScopedKeyHierarchy(byte[] cloudRoot, byte[] salt) {
        if (cloudRoot == null || cloudRoot.length != 32 || salt == null || salt.length != 32) {
            throw new IllegalArgumentException("256-bit cloud root and salt required");
        }
        this.cloudRoot = cloudRoot.clone();
        this.salt = salt.clone();
    }

    public ScopedKeyHierarchy(byte[] cloudRoot, byte[] vaultRoot, byte[] salt) {
        this(cloudRoot, salt);
        try { unlockVault(vaultRoot); }
        catch (RuntimeException failure) { close(); throw failure; }
    }

    /** Caller must authenticate and unwrap the independent vault root before invoking this method. */
    public synchronized void unlockVault(byte[] root) {
        if (closed) throw new IllegalStateException("Key hierarchy is locked");
        if (root == null || root.length != 32 || MessageDigest.isEqual(cloudRoot, root)) {
            throw new IllegalArgumentException("Independent 256-bit roots and 256-bit salt required");
        }
        if (vaultRoot != null) throw new IllegalStateException("Vault is already unlocked");
        vaultRoot = root.clone();
    }

    /** Invalidates future derivation; consumers must separately wipe previously returned keys/previews. */
    public synchronized void lockVault() {
        if (vaultRoot != null) Arrays.fill(vaultRoot, (byte) 0);
        vaultRoot = null;
    }

    public synchronized boolean isVaultUnlocked() {
        return !closed && vaultRoot != null;
    }

    public synchronized byte[] derive(Domain domain, Purpose purpose, UUID objectId, long version)
            throws GeneralSecurityException {
        if (closed) throw new IllegalStateException("Key hierarchy is locked");
        if (domain == null || purpose == null || objectId == null || version <= 0) {
            throw new IllegalArgumentException("Invalid key scope");
        }
        if (domain == Domain.VAULT && vaultRoot == null) throw new IllegalStateException("Vault is locked");
        byte[] label = ("TGPhotos/Key/v1\u0000" + domain.name() + "\u0000" + purpose.name() + "\u0000")
                .getBytes(StandardCharsets.US_ASCII);
        byte[] info = ByteBuffer.allocate(label.length + 24).put(label)
                .putLong(objectId.getMostSignificantBits()).putLong(objectId.getLeastSignificantBits())
                .putLong(version).array();
        return HkdfSha256.derive(domain == Domain.CLOUD ? cloudRoot : vaultRoot, salt, info, 32);
    }

    @Override public synchronized void close() {
        Arrays.fill(cloudRoot, (byte) 0);
        lockVault();
        Arrays.fill(salt, (byte) 0);
        closed = true;
    }

    @Override public String toString() { return "ScopedKeyHierarchy(redacted)"; }
}
