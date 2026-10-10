package app.tgphotos.core.media

enum class VideoLayout(val aspectRatio: Float) { REELS(9f / 16f), SQUARE(1f), NORMAL(16f / 9f) }

/** Future Media3 DataSource must authenticate an entire bounded chunk before returning any bytes. */
interface AuthenticatedMediaSource {
    suspend fun readAuthenticatedChunk(fileId: String, version: Long, chunkIndex: Long): ByteArray
}

data class PlaybackPosition(val opaqueFileId: String, val contentVersion: Long, val positionMs: Long)
