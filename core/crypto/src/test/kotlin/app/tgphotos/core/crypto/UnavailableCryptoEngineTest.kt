package app.tgphotos.core.crypto

import org.junit.Test

class UnavailableCryptoEngineTest {
    @Test(expected = IllegalStateException::class)
    fun encryptionCannotReturnPlaintextAsFakeCiphertext() { UnavailableCryptoEngine().encryptChunk(byteArrayOf(1, 2), byteArrayOf()) }
    @Test(expected = IllegalStateException::class)
    fun decryptionCannotReturnUnauthenticatedBytes() { UnavailableCryptoEngine().decryptChunk(byteArrayOf(1, 2), byteArrayOf()) }
}
