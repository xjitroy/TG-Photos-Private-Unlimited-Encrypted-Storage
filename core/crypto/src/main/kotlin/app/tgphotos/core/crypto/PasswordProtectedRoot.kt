package app.tgphotos.core.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.UUID

/**
 * Versioned portable password envelope for one independently generated cloud or Vault root.
 * Draft: persist atomically in app-private storage and retain a trusted minimum wrap revision.
 * Run Argon2id off the UI thread; calibrate its bounded parameters on supported devices.
 * Recovery export/import, account binding and rollback protection require additional app policy.
 */
class PasswordProtectedRoot(
    private val derivation: PasswordKeyDerivation = BouncyCastlePasswordKeyDerivation(),
    private val aead: AeadEnvelope = AeadEnvelope(),
) {
    enum class Purpose(val wire: Byte) { CLOUD(1), VAULT(2) }

    private data class Parsed(
        val header: ByteArray, val envelope: ByteArray, val salt: ByteArray,
        val parameters: Argon2idParameters, val revision: Long,
    )

    /** Creates a fresh random root; the returned record contains only public KDF framing + ciphertext. */
    fun create(password: CharArray, keyId: UUID, rootVersion: Long, purpose: Purpose,
               parameters: Argon2idParameters): ByteArray {
        var root: ByteArray? = null
        try {
            require(rootVersion > 0)
            root = aead.generateKey()
            return protect(root, password, keyId, rootVersion, purpose, 1, parameters)
        } finally {
            root?.fill(0)
            password.fill('\u0000')
        }
    }

    /** Caller owns the authenticated 32-byte result and must wipe it on account/Vault lock. */
    fun unlock(record: ByteArray, password: CharArray, keyId: UUID, rootVersion: Long,
               purpose: Purpose, minimumWrapRevision: Long): ByteArray {
        var unlockKey: ByteArray? = null
        var parsed: Parsed? = null
        var plaintext: ByteArray? = null
        var success = false
        try {
            parsed = parse(record, keyId, rootVersion, purpose, minimumWrapRevision)
            unlockKey = derivation.deriveArgon2id(password, parsed.salt, parsed.parameters)
            require(unlockKey.size == AeadEnvelope.KEY_BYTES)
            plaintext = aead.open(unlockKey, parsed.envelope, context(parsed.header))
            if (plaintext.size != AeadEnvelope.KEY_BYTES) throw invalid()
            success = true
            return plaintext
        } finally {
            password.fill('\u0000')
            unlockKey?.fill(0)
            parsed?.salt?.fill(0)
            if (!success) plaintext?.fill(0)
        }
    }

    /** Rewraps the same root/identity with a fresh salt and nonce; never rotates file encryption keys. */
    fun changePassword(record: ByteArray, oldPassword: CharArray, newPassword: CharArray,
                       keyId: UUID, rootVersion: Long, purpose: Purpose,
                       minimumWrapRevision: Long, parameters: Argon2idParameters): ByteArray {
        var root: ByteArray? = null
        try {
            require(oldPassword !== newPassword) { "Credentials must use separate mutable buffers" }
            if (record.size != RECORD_BYTES) throw invalid()
            // A private snapshot prevents a concurrently changed caller buffer altering the framing
            // between the authenticated unlock and revision increment.
            val snapshot = record.copyOf()
            val parsed = parse(snapshot, keyId, rootVersion, purpose, minimumWrapRevision)
            try {
                if (parsed.revision == Long.MAX_VALUE) throw invalid()
                root = unlock(snapshot, oldPassword, keyId, rootVersion, purpose, minimumWrapRevision)
                return protect(root, newPassword, keyId, rootVersion, purpose, parsed.revision + 1, parameters)
            } finally {
                parsed.salt.fill(0)
            }
        } finally {
            oldPassword.fill('\u0000')
            newPassword.fill('\u0000')
            root?.fill(0)
        }
    }

    private fun protect(root: ByteArray, password: CharArray, keyId: UUID, rootVersion: Long,
                        purpose: Purpose, revision: Long, parameters: Argon2idParameters): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        var unlockKey: ByteArray? = null
        try {
            val header = ByteBuffer.allocate(HEADER_BYTES).put(MAGIC).put(FORMAT).put(purpose.wire)
                .putLong(keyId.mostSignificantBits).putLong(keyId.leastSignificantBits)
                .putLong(rootVersion).putLong(revision).put(ARGON_VERSION)
                .putInt(parameters.memoryKiB).putInt(parameters.iterations).putInt(parameters.parallelism)
                .put(salt).array()
            unlockKey = derivation.deriveArgon2id(password, salt, parameters)
            require(unlockKey.size == AeadEnvelope.KEY_BYTES)
            return header + aead.seal(unlockKey, root, context(header))
        } finally {
            password.fill('\u0000')
            unlockKey?.fill(0)
            salt.fill(0)
        }
    }

    private fun parse(record: ByteArray, keyId: UUID, rootVersion: Long, purpose: Purpose,
                      minimumRevision: Long): Parsed {
        // Reject unknown formats, lengths and excessive KDF work before allocation or derivation.
        if (rootVersion <= 0 || minimumRevision <= 0 || record.size != RECORD_BYTES) throw invalid()
        val snapshot = record.copyOf()
        val input = ByteBuffer.wrap(snapshot)
        val magic = ByteArray(4).also { input.get(it) }
        if (!magic.contentEquals(MAGIC) || input.get() != FORMAT || input.get() != purpose.wire) throw invalid()
        if (UUID(input.long, input.long) != keyId || input.long != rootVersion) throw invalid()
        val revision = input.long
        if (revision < minimumRevision || input.get() != ARGON_VERSION) throw invalid()
        val parameters = try { Argon2idParameters(input.int, input.int, input.int) }
        catch (_: IllegalArgumentException) { throw invalid() }
        val salt = ByteArray(SALT_BYTES).also { input.get(it) }
        return Parsed(snapshot.copyOfRange(0, HEADER_BYTES), snapshot.copyOfRange(HEADER_BYTES, RECORD_BYTES),
            salt, parameters, revision)
    }

    private fun context(header: ByteArray) = DOMAIN + header
    private fun invalid() = GeneralSecurityException("Invalid protected root record")

    companion object {
        private val MAGIC = byteArrayOf(84, 71, 80, 82) // TGPR
        private const val FORMAT: Byte = 1
        private const val ARGON_VERSION: Byte = 0x13
        private const val SALT_BYTES = 32
        private const val HEADER_BYTES = 4 + 1 + 1 + 16 + 8 + 8 + 1 + 12 + SALT_BYTES
        private const val RECORD_BYTES = HEADER_BYTES + AeadEnvelope.KEY_BYTES + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES
        private val DOMAIN = "TGPhotos/PasswordProtectedRoot/v1\u0000".toByteArray(StandardCharsets.UTF_8)
    }
}
