package app.tgphotos.core.crypto

import java.security.GeneralSecurityException
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class PasswordProtectedRootTest {
    private val codec = PasswordProtectedRoot()
    private val id = UUID.randomUUID()
    private val parameters = Argon2idParameters(65536, 3, 1)
    private val purpose = PasswordProtectedRoot.Purpose.CLOUD
    private fun password() = "synthetic personal root passphrase".toCharArray()
    private fun create() = codec.create(password(), id, 1, purpose, parameters)
    private fun unlock(record: ByteArray, password: CharArray = password(), minimum: Long = 1) =
        codec.unlock(record, password, id, 1, purpose, minimum)

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Unauthenticated or stale root was accepted") }
        catch (_: GeneralSecurityException) { /* No plaintext root returned. */ }
    }

    @Test fun freshBackendUnlocksPortableRecordAndClearsPassword() {
        val record = create()
        val input = password()
        val first = unlock(record, input)
        val second = PasswordProtectedRoot().unlock(record, password(), id, 1, purpose, 1)
        try {
            assertEquals(32, first.size)
            assertArrayEquals(first, second)
            assertTrue(input.all { it == '\u0000' })
            assertFalse(record.asList().windowed(first.size).any { it == first.asList() })
        } finally { first.fill(0); second.fill(0) }
    }

    @Test fun wrongPasswordAndCiphertextMutationNeverReturnRoot() {
        val record = create()
        val wrong = "different synthetic password".toCharArray()
        rejects { unlock(record, wrong) }
        assertTrue(wrong.all { it == '\u0000' })
        val mutated = record.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        rejects { unlock(mutated) }
    }

    @Test fun headerIdentityPurposeVersionAndRevisionAreAuthenticated() {
        val record = create()
        rejects { codec.unlock(record, password(), UUID.randomUUID(), 1, purpose, 1) }
        rejects { codec.unlock(record, password(), id, 2, purpose, 1) }
        rejects { codec.unlock(record, password(), id, 1, PasswordProtectedRoot.Purpose.VAULT, 1) }
        // A structurally valid higher revision still needs the original authentication tag.
        val changedRevision = record.copyOf().also { it[37] = 2 }
        rejects { unlock(changedRevision) }
    }

    @Test fun passwordChangePreservesRootAndRejectsOldPasswordAndStaleRecord() {
        val record = create()
        val original = unlock(record)
        val old = password()
        val new = "replacement synthetic private passphrase".toCharArray()
        val changed = codec.changePassword(record, old, new, id, 1, purpose, 1, parameters)
        val restored = unlock(changed, "replacement synthetic private passphrase".toCharArray(), 2)
        try {
            assertArrayEquals(original, restored)
            assertTrue(old.all { it == '\u0000' } && new.all { it == '\u0000' })
            rejects { unlock(changed) }
            rejects { unlock(record, minimum = 2) }
        } finally { original.fill(0); restored.fill(0) }
    }

    @Test fun malformedFramingAndOversizedKdfRejectBeforeDerivation() {
        var calls = 0
        val refusing = PasswordProtectedRoot(object : PasswordKeyDerivation {
            override fun deriveArgon2id(password: CharArray, salt: ByteArray,
                                       parameters: Argon2idParameters): ByteArray {
                calls++
                throw AssertionError("Invalid framing reached costly derivation")
            }
        })
        val record = create()
        val hugeMemory = record.copyOf().also { java.nio.ByteBuffer.wrap(it).putInt(39, Int.MAX_VALUE) }
        val unknownFormat = record.copyOf().also { it[4] = 2 }
        for (invalid in listOf(record.copyOf(record.size - 1), record + byteArrayOf(0), hugeMemory, unknownFormat)) {
            val input = password()
            rejects { refusing.unlock(invalid, input, id, 1, purpose, 1) }
            assertTrue(input.all { it == '\u0000' })
        }
        assertEquals(0, calls)
    }

    @Test fun failedPasswordChangeWipesBothCredentialsWithoutChangingRecord() {
        val record = create()
        val before = record.copyOf()
        val old = "incorrect old password".toCharArray()
        val new = "replacement synthetic private passphrase".toCharArray()
        rejects { codec.changePassword(record, old, new, id, 1, purpose, 1, parameters) }
        assertArrayEquals(before, record)
        assertTrue(old.all { it == '\u0000' } && new.all { it == '\u0000' })
    }
}
