package app.tgphotos.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class MediaKind { PHOTO, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER }
enum class BackupState {
    DISCOVERED, QUEUED, CHUNKING, ENCRYPTING, UPLOADING, VERIFYING,
    COMMITTED, RESTORABLE, RETRY_WAIT, PAUSED, PERMISSION_NEEDED, ACCOUNT_LIMITED, FAILED,
}

/** Proof must come from an independent, complete remote restore of this exact version. */
data class RestoreEvidence(
    val fileId: String,
    val contentVersion: Long,
    val expectedBytes: Long,
    val restoredBytes: Long,
    val expectedSha256: String,
    val restoredSha256: String,
    val allChunksAuthenticated: Boolean,
    val manifestAuthenticated: Boolean,
    val independentlyDownloaded: Boolean,
) {
    fun permitsDeletionOf(id: String, version: Long): Boolean =
        fileId.isNotBlank() && fileId == id && contentVersion > 0 && contentVersion == version && expectedBytes >= 0 &&
            expectedBytes == restoredBytes && expectedSha256.matches(Regex("[a-f0-9]{64}")) &&
            expectedSha256 == restoredSha256 && allChunksAuthenticated &&
            manifestAuthenticated && independentlyDownloaded
}

object LocalDeletionGate {
    fun mayRequestConfirmation(
        state: BackupState, fileId: String, version: Long, evidence: RestoreEvidence?,
    ): Boolean = state == BackupState.RESTORABLE && evidence?.permitsDeletionOf(fileId, version) == true
    // This is eligibility only. Scoped-storage delete consent is a separate Android operation.
}

data class PreviewMedia(val id: Int, val title: String, val kind: MediaKind, val favorite: Boolean = false)
