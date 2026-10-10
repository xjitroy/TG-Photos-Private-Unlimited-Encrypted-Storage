package app.tgphotos.core.crypto;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;

/** Wrap a random root under an already derived unlock key. Password derivation must be Argon2id. */
public final class RootKeyEnvelope {
    private final AeadEnvelope aead;
    public RootKeyEnvelope(AeadEnvelope aead) { this.aead = aead; }

    public byte[] wrap(byte[] randomRoot, byte[] unlockKey, UUID keyId, long keyVersion,
                       ChunkContext.Scope purpose) throws GeneralSecurityException {
        checkPurpose(purpose);
        if (randomRoot == null || randomRoot.length != AeadEnvelope.KEY_BYTES) {
            throw new IllegalArgumentException("Root key must be random 256-bit key material");
        }
        return aead.seal(unlockKey, randomRoot,
                new ChunkContext(keyId, keyVersion, 0, AeadEnvelope.KEY_BYTES, purpose).encode());
    }

    public byte[] unwrap(byte[] envelope, byte[] unlockKey, UUID keyId, long keyVersion,
                         ChunkContext.Scope purpose) throws GeneralSecurityException {
        checkPurpose(purpose);
        byte[] root = aead.open(unlockKey, envelope,
                new ChunkContext(keyId, keyVersion, 0, AeadEnvelope.KEY_BYTES, purpose).encode());
        if (root.length != AeadEnvelope.KEY_BYTES) {
            Arrays.fill(root, (byte) 0);
            throw new GeneralSecurityException("Invalid root key envelope");
        }
        return root;
    }

    private static void checkPurpose(ChunkContext.Scope purpose) {
        if (purpose != ChunkContext.Scope.ROOT_PASSWORD_WRAP && purpose != ChunkContext.Scope.ROOT_RECOVERY_WRAP) {
            throw new IllegalArgumentException("Invalid root key wrapping purpose");
        }
    }
}
