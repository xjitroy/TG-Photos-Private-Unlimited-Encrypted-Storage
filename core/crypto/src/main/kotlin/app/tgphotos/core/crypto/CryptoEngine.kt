package app.tgphotos.core.crypto

/** No implementation until Argon2id, key wrapping, domain separation and vectors are reviewed. */
interface CryptoEngine {
    fun encryptChunk(plaintext: ByteArray, authenticatedContext: ByteArray): ByteArray
    fun decryptChunk(envelope: ByteArray, authenticatedContext: ByteArray): ByteArray
    fun lock()
}

class UnavailableCryptoEngine : CryptoEngine {
    override fun encryptChunk(plaintext: ByteArray, authenticatedContext: ByteArray): Nothing = unavailable()
    override fun decryptChunk(envelope: ByteArray, authenticatedContext: ByteArray): Nothing = unavailable()
    override fun lock() = Unit
    private fun unavailable(): Nothing = throw IllegalStateException("Reviewed encryption engine is not integrated")
}
