package app.tgphotos.core.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Canonical associated data; separates file identity/version/order and encrypted object purpose. */
public final class ChunkContext {
    public enum Scope { FILE, MANIFEST, THUMBNAIL, CACHE, VAULT, ROOT_PASSWORD_WRAP, ROOT_RECOVERY_WRAP }
    private static final byte[] DOMAIN = "TGPhotos/Object/v1\u0000".getBytes(StandardCharsets.UTF_8);
    private final UUID fileId;
    private final long version;
    private final long index;
    private final int plaintextBytes;
    private final Scope scope;

    public ChunkContext(UUID fileId, long version, long index, int plaintextBytes, Scope scope) {
        if (fileId == null || scope == null || version <= 0 || index < 0 || plaintextBytes < 0 ||
                plaintextBytes > AeadEnvelope.MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("Invalid authenticated object context");
        }
        this.fileId = fileId;
        this.version = version;
        this.index = index;
        this.plaintextBytes = plaintextBytes;
        this.scope = scope;
    }

    public byte[] encode() {
        byte[] scopeName = scope.name().getBytes(StandardCharsets.US_ASCII);
        return ByteBuffer.allocate(DOMAIN.length + 4 + scopeName.length + 16 + 8 + 8 + 4)
                .put(DOMAIN).putInt(scopeName.length).put(scopeName)
                .putLong(fileId.getMostSignificantBits()).putLong(fileId.getLeastSignificantBits())
                .putLong(version).putLong(index).putInt(plaintextBytes).array();
    }
}
