package app.tgphotos.core.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.Assert.*
import org.junit.Test

class PasswordKeyDerivationTest {
    private val backend = BouncyCastlePasswordKeyDerivation()
    private val parameters = Argon2idParameters(65536, 3, 1)
    private fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun rfc9106Argon2idKnownAnswer() {
        // RFC 9106 section 5.3. Low-cost parameters belong only to this published vector.
        val config = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3).withMemoryAsKB(32).withParallelism(4)
            .withSalt(ByteArray(16) { 2 }).withSecret(ByteArray(8) { 3 })
            .withAdditional(ByteArray(12) { 4 }).build()
        val generator = Argon2BytesGenerator()
        generator.init(config)
        val output = ByteArray(32)
        generator.generateBytes(ByteArray(32) { 1 }, output)
        assertArrayEquals(hex("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659"), output)
        config.clear()
    }

    @Test fun unicodePassphraseIsDeterministicAndMutableInputIsWiped() {
        val first = "ব্যক্তিগত নিরাপদ পাসফ্রেজ 🔐".toCharArray()
        val second = first.copyOf()
        val salt = ByteArray(32) { it.toByte() }
        val a = backend.deriveArgon2id(first, salt, parameters)
        val b = backend.deriveArgon2id(second, salt, parameters)
        assertEquals(32, a.size)
        assertArrayEquals(a, b)
        assertTrue(first.all { it == '\u0000' } && second.all { it == '\u0000' })
        a.fill(0); b.fill(0)
    }

    @Test fun changedSaltOrPasswordChangesUnlockKey() {
        val a = backend.deriveArgon2id("long test password A".toCharArray(), ByteArray(32) { 1 }, parameters)
        val b = backend.deriveArgon2id("long test password A".toCharArray(), ByteArray(32) { 2 }, parameters)
        val c = backend.deriveArgon2id("long test password B".toCharArray(), ByteArray(32) { 1 }, parameters)
        assertFalse(a.contentEquals(b)); assertFalse(a.contentEquals(c))
        a.fill(0); b.fill(0); c.fill(0)
    }

    @Test fun invalidSaltFailsAndStillClearsPassphrase() {
        val input = "do not retain this fixture".toCharArray()
        try { backend.deriveArgon2id(input, ByteArray(8), parameters); fail("Short salt accepted") }
        catch (_: IllegalArgumentException) { assertTrue(input.all { it == '\u0000' }) }
    }

    @Test fun weakOrUnboundedKdfParametersAreRejected() {
        for (candidate in listOf(Triple(32768, 3, 1), Triple(65536, 2, 1), Triple(65536, 3, 0), Triple(524288, 3, 1))) {
            try { Argon2idParameters(candidate.first, candidate.second, candidate.third); fail("Unsafe parameters accepted") }
            catch (_: IllegalArgumentException) { /* Expected before costly allocation. */ }
        }
    }

    @Test fun freshBackendRestoresWrappedRootAndWrongPasswordFails() {
        val salt = ByteArray(32) { it.toByte() }
        val key = backend.deriveArgon2id("a long synthetic recovery test password".toCharArray(), salt, parameters)
        val aead = AeadEnvelope()
        val root = aead.generateKey()
        val id = java.util.UUID.randomUUID()
        val envelope = RootKeyEnvelope(aead).wrap(root, key, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP)
        key.fill(0)
        val restoredKey = BouncyCastlePasswordKeyDerivation().deriveArgon2id(
            "a long synthetic recovery test password".toCharArray(), salt, parameters)
        assertArrayEquals(root, RootKeyEnvelope(AeadEnvelope()).unwrap(envelope, restoredKey, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP))
        restoredKey.fill(0)
        val wrong = backend.deriveArgon2id("a different synthetic recovery password".toCharArray(), salt, parameters)
        try { RootKeyEnvelope(aead).unwrap(envelope, wrong, id, 1, ChunkContext.Scope.ROOT_PASSWORD_WRAP); fail("Wrong password accepted") }
        catch (_: java.security.GeneralSecurityException) { /* No unauthenticated root released. */ }
        finally { root.fill(0); wrong.fill(0) }
    }
}
