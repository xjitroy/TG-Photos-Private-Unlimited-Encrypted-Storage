package app.tgphotos.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Offline executable security checks; not an audit or a replacement for Android/provider QA. */
public final class CryptoPrimitiveChecks {
    private static int passed;
    private interface CheckedAction { void run() throws Exception; }
    private static void check(String name, CheckedAction action) throws Exception {
        action.run(); passed++; System.out.println("PASS " + name);
    }
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Check failed"); }
    private static void rejects(CheckedAction action) throws Exception {
        try { action.run(); } catch (GeneralSecurityException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid encrypted input was accepted");
    }
    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(value.substring(2*i, 2*i+2), 16);
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        AeadEnvelope aead = new AeadEnvelope();
        byte[] key = aead.generateKey();
        UUID id = UUID.randomUUID();
        byte[] plaintext = "ব্যক্তিগত ছবি — TG Photos".getBytes(StandardCharsets.UTF_8);
        byte[] context = new ChunkContext(id, 1, 0, plaintext.length, ChunkContext.Scope.FILE).encode();
        byte[] envelope = aead.seal(key, plaintext, context);
        check("AES-256-GCM known-answer vector (empty plaintext)", () -> {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, new byte[12]));
            require(Arrays.equals(hex("530f8afbc74536b9a963b4f1c4cb738b"), cipher.doFinal(new byte[0])));
        });
        check("Unicode binary plaintext round trip", () -> require(Arrays.equals(plaintext, aead.open(key, envelope, context))));
        check("Empty file round trip", () -> {
            byte[] c = new ChunkContext(id, 1, 0, 0, ChunkContext.Scope.FILE).encode();
            require(aead.open(key, aead.seal(key, new byte[0], c), c).length == 0);
        });
        check("Ciphertext differs from plaintext", () -> require(!Arrays.equals(plaintext, envelope)));
        check("Wrong key fails authentication", () -> rejects(() -> aead.open(aead.generateKey(), envelope, context)));
        check("Every envelope byte is authenticated or rejected", () -> {
            for (int i = 0; i < envelope.length; i++) {
                byte[] corrupt = envelope.clone(); corrupt[i] ^= 1;
                rejects(() -> aead.open(key, corrupt, context));
            }
        });
        check("Truncation and trailing-byte injection rejected", () -> {
            for (int size = 0; size < envelope.length; size++) {
                byte[] truncated = Arrays.copyOf(envelope, size);
                rejects(() -> aead.open(key, truncated, context));
            }
            rejects(() -> aead.open(key, Arrays.copyOf(envelope, envelope.length + 1), context));
        });
        check("File substitution fails", () -> rejects(() -> aead.open(key, envelope,
                new ChunkContext(UUID.randomUUID(), 1, 0, plaintext.length, ChunkContext.Scope.FILE).encode())));
        check("Content version replay fails", () -> rejects(() -> aead.open(key, envelope,
                new ChunkContext(id, 2, 0, plaintext.length, ChunkContext.Scope.FILE).encode())));
        check("Chunk reorder fails", () -> rejects(() -> aead.open(key, envelope,
                new ChunkContext(id, 1, 1, plaintext.length, ChunkContext.Scope.FILE).encode())));
        check("Vault/gallery purpose substitution fails", () -> rejects(() -> aead.open(key, envelope,
                new ChunkContext(id, 1, 0, plaintext.length, ChunkContext.Scope.VAULT).encode())));
        check("Expected plaintext length is bound", () -> rejects(() -> aead.open(key, envelope,
                new ChunkContext(id, 1, 0, plaintext.length + 1, ChunkContext.Scope.FILE).encode())));
        check("Repeated encryptions use distinct nonce/ciphertext", () -> require(!Arrays.equals(envelope, aead.seal(key, plaintext, context))));
        check("AES-128 and unbounded context rejected", () -> {
            rejects(() -> aead.seal(new byte[16], plaintext, context));
            rejects(() -> aead.seal(key, plaintext, new byte[AeadEnvelope.MAX_CONTEXT_BYTES + 1]));
        });
        check("Oversized plaintext rejected before encryption", () -> rejects(() -> aead.seal(key, new byte[AeadEnvelope.MAX_PLAINTEXT_BYTES + 1], context)));
        check("Maximum bounded chunk round trip", () -> {
            byte[] big = new byte[AeadEnvelope.MAX_PLAINTEXT_BYTES];
            Arrays.fill(big, (byte) 7);
            byte[] c = new ChunkContext(id, 1, 0, big.length, ChunkContext.Scope.FILE).encode();
            require(Arrays.equals(big, aead.open(key, aead.seal(key, big, c), c)));
        });
        check("Root key restore using a fresh service instance", () -> {
            byte[] root = aead.generateKey(); byte[] unlock = aead.generateKey();
            byte[] wrapped = new RootKeyEnvelope(aead).wrap(root, unlock, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP);
            require(Arrays.equals(root, new RootKeyEnvelope(new AeadEnvelope()).unwrap(wrapped, unlock, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP)));
        });
        check("Recovery/password wrap domain separation", () -> {
            byte[] root = aead.generateKey(); byte[] unlock = aead.generateKey();
            byte[] wrapped = new RootKeyEnvelope(aead).wrap(root, unlock, id, 1, ChunkContext.Scope.ROOT_RECOVERY_WRAP);
            rejects(() -> new RootKeyEnvelope(aead).unwrap(wrapped, unlock, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP));
        });
        check("Invalid root length rejected", () -> rejects(() -> new RootKeyEnvelope(aead).wrap(new byte[16], key, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP)));
        Arrays.fill(key, (byte) 0);
        System.out.println("RESULT " + passed + " checks passed. Java/JCA only; Android/KDF/full recovery not verified.");
    }
}
