package app.tgphotos.core.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.UUID

/** Original Media3 1.8.0 adapter draft. Uses an opaque URI; never opens a remote URL or plaintext file. */
@UnstableApi
class EncryptedMediaDataSource(
    private val fileId: UUID,
    private val version: Long,
    private val createReader: () -> AuthenticatedRangeReader,
    isNetwork: Boolean,
) : BaseDataSource(isNetwork) {
    private val expectedUri = Uri.Builder().scheme("tgphotos").authority("media")
        .appendPath(fileId.toString()).appendPath(version.toString()).build()
    private var reader: AuthenticatedRangeReader? = null
    private var position = 0L
    private var remaining = 0L

    init { require(version > 0) }

    override fun open(dataSpec: DataSpec): Long {
        check(reader == null) { "Encrypted media source is already open" }
        transferInitializing(dataSpec)
        if (dataSpec.uri != expectedUri || dataSpec.httpMethod != DataSpec.HTTP_METHOD_GET ||
            dataSpec.httpBody != null || dataSpec.httpRequestHeaders.isNotEmpty()) {
            throw IOException("Invalid opaque media request")
        }
        val opened = createReader()
        try {
            if (opened.fileId != fileId || opened.version != version || dataSpec.position < 0 ||
                dataSpec.position > opened.totalBytes) throw IOException("Encrypted media descriptor/range mismatch")
            position = dataSpec.position
            val available = opened.totalBytes - position
            remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) available else minOf(available, dataSpec.length)
            reader = opened
            transferStarted(dataSpec)
            return remaining
        } catch (failure: Exception) {
            reader = null; opened.close(); throw failure
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val active = reader ?: throw IOException("Encrypted media source is closed")
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length)
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val count = try { active.readAt(position, buffer, offset, minOf(length.toLong(), remaining).toInt()) }
        catch (failure: GeneralSecurityException) { throw IOException("Media chunk authentication failed", failure) }
        if (count < 1) throw IOException("Encrypted media ended before its authenticated descriptor")
        position += count; remaining -= count; bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri? = if (reader == null) null else expectedUri
    override fun close() {
        val active = reader ?: return
        reader = null; position = 0; remaining = 0
        active.close(); transferEnded()
    }
}
