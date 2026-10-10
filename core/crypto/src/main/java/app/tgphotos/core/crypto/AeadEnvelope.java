package app.tgphotos.core.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Bounded versioned AES-256-GCM envelope using the platform's JCA provider.
 * Never streams unauthenticated plaintext. This primitive is not a password KDF,
 * complete E2EE protocol, Android Keystore integration or independently audited implementation.
 */
public final class AeadEnvelope {
    public static final int KEY_BYTES = 32;
    public static final int MAX_PLAINTEXT_BYTES = 16 * 1024 * 1024;
    public static final int MAX_CONTEXT_BYTES = 4096;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BYTES = 16;
    private static final int HEADER_BYTES = 4 + 1 + NONCE_BYTES + 4;
    private static final byte[] MAGIC = new byte[] {'T', 'G', 'P', 'A'};
    private static final byte VERSION = 1;
    private static final byte[] DOMAIN = "TGPhotos/Aead/v1\u0000".getBytes(StandardCharsets.UTF_8);
    private final SecureRandom random;

    public AeadEnvelope() { this(new SecureRandom()); }
    AeadEnvelope(SecureRandom random) { this.random = random; }

    public byte[] generateKey() {
        byte[] key = new byte[KEY_BYTES];
        random.nextBytes(key);
        return key;
    }

    /** A fresh CSPRNG nonce for every invocation. Retry persisted ciphertext, not encryption state. */
    public byte[] seal(byte[] key, byte[] plaintext, byte[] context) throws GeneralSecurityException {
        validateKeyAndContext(key, context);
        if (plaintext == null || plaintext.length > MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("Plaintext exceeds the bounded envelope size");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        byte[] header = ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).put(VERSION)
                .put(nonce).putInt(plaintext.length).array();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * 8, nonce));
        authenticate(cipher, header, context);
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(header.length + ciphertext.length).put(header).put(ciphertext).array();
    }

    /** Strict parse, bound allocation and authenticate before returning any plaintext. */
    public byte[] open(byte[] key, byte[] envelope, byte[] context) throws GeneralSecurityException {
        validateKeyAndContext(key, context);
        if (envelope == null || envelope.length < HEADER_BYTES + TAG_BYTES ||
                envelope.length > HEADER_BYTES + MAX_PLAINTEXT_BYTES + TAG_BYTES) {
            throw new GeneralSecurityException("Invalid encrypted envelope");
        }
        ByteBuffer input = ByteBuffer.wrap(envelope);
        byte[] magic = new byte[MAGIC.length];
        input.get(magic);
        if (!Arrays.equals(magic, MAGIC) || input.get() != VERSION) {
            throw new GeneralSecurityException("Unsupported encrypted envelope");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        input.get(nonce);
        int declaredBytes = input.getInt();
        if (declaredBytes < 0 || declaredBytes > MAX_PLAINTEXT_BYTES ||
                declaredBytes != input.remaining() - TAG_BYTES) {
            throw new GeneralSecurityException("Invalid encrypted envelope length");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * 8, nonce));
        authenticate(cipher, Arrays.copyOf(envelope, HEADER_BYTES), context);
        // doFinal returns bytes only after the tag passes. No update() plaintext output is exposed.
        return cipher.doFinal(envelope, HEADER_BYTES, envelope.length - HEADER_BYTES);
    }

    private static void authenticate(Cipher cipher, byte[] header, byte[] context) {
        cipher.updateAAD(DOMAIN);
        cipher.updateAAD(header);
        cipher.updateAAD(ByteBuffer.allocate(4).putInt(context.length).array());
        cipher.updateAAD(context);
    }

    private static void validateKeyAndContext(byte[] key, byte[] context) {
        if (key == null || key.length != KEY_BYTES) {
            throw new IllegalArgumentException("AES-256 requires a 32-byte key");
        }
        if (context == null || context.length > MAX_CONTEXT_BYTES) {
            throw new IllegalArgumentException("Invalid authenticated context");
        }
    }
}
