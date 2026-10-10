package app.tgphotos.core.cache

/** Persist only authenticated ciphertext in app-private storage. Decode after authentication in memory. */
interface EncryptedCache {
    suspend fun readEnvelope(opaqueId: String): ByteArray?
    suspend fun writeEnvelope(opaqueId: String, envelope: ByteArray)
    suspend fun evict(opaqueId: String)
    suspend fun clear()
}
