package app.tgphotos.core.crypto;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 5869 HKDF with platform HMAC-SHA-256; this is not a password KDF. */
public final class HkdfSha256 {
    private HkdfSha256() {}

    public static byte[] derive(byte[] inputKey, byte[] salt, byte[] info, int outputBytes)
            throws GeneralSecurityException {
        if (inputKey == null || inputKey.length == 0 || salt == null || info == null ||
                info.length > 4096 || outputBytes < 1 || outputBytes > 255 * 32) {
            throw new IllegalArgumentException("Invalid HKDF input or output bounds");
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] extractSalt = salt.length == 0 ? new byte[32] : salt.clone();
        byte[] prk = null;
        byte[] block = new byte[0];
        byte[] output = new byte[outputBytes];
        boolean success = false;
        try {
            mac.init(new SecretKeySpec(extractSalt, "HmacSHA256"));
            prk = mac.doFinal(inputKey);
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            int offset = 0;
            for (int counter = 1; offset < outputBytes; counter++) {
                mac.update(block);
                mac.update(info);
                mac.update((byte) counter);
                byte[] next = mac.doFinal();
                Arrays.fill(block, (byte) 0);
                block = next;
                int count = Math.min(block.length, outputBytes - offset);
                System.arraycopy(block, 0, output, offset, count);
                offset += count;
            }
            success = true;
            return output;
        } finally {
            Arrays.fill(extractSalt, (byte) 0);
            if (prk != null) Arrays.fill(prk, (byte) 0);
            Arrays.fill(block, (byte) 0);
            if (!success) Arrays.fill(output, (byte) 0);
            // Provider key copies make JVM erasure best effort.
        }
    }
}
