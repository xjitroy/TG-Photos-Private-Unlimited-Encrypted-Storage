package app.tgphotos.core.crypto

import java.util.UUID

/** Draft primitive bridge; intentionally not wired to app or unavailable production CryptoEngine. */
class JcaChunkCipher(private val aead: AeadEnvelope = AeadEnvelope()) {
    fun encrypt(key: ByteArray, plaintext: ByteArray, fileId: UUID, version: Long, index: Long,
                scope: ChunkContext.Scope = ChunkContext.Scope.FILE): ByteArray =
        aead.seal(key, plaintext, ChunkContext(fileId, version, index, plaintext.size, scope).encode())

    fun decrypt(key: ByteArray, envelope: ByteArray, fileId: UUID, version: Long, index: Long,
                expectedPlaintextBytes: Int, scope: ChunkContext.Scope = ChunkContext.Scope.FILE): ByteArray =
        aead.open(key, envelope, ChunkContext(fileId, version, index, expectedPlaintextBytes, scope).encode())
}
