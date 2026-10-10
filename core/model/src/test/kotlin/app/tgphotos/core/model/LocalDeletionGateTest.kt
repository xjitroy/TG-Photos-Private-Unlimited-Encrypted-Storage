package app.tgphotos.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDeletionGateTest {
    private val hash = "a".repeat(64)
    private val valid = RestoreEvidence("file", 7, 1024, 1024, hash, hash, true, true, true)

    @Test fun onlyRestorableStateCanRequestConsent() {
        BackupState.entries.forEach { state ->
            val allowed = LocalDeletionGate.mayRequestConfirmation(state, "file", 7, valid)
            if (state == BackupState.RESTORABLE) assertTrue(allowed) else assertFalse(allowed)
        }
    }
    @Test fun incompleteCorruptOrLocallyOnlyEvidenceCannotPermitDeletion() {
        val invalid = listOf(
            valid.copy(restoredBytes = 1023), valid.copy(restoredSha256 = "b".repeat(64)),
            valid.copy(allChunksAuthenticated = false), valid.copy(manifestAuthenticated = false),
            valid.copy(independentlyDownloaded = false), valid.copy(expectedSha256 = "", restoredSha256 = ""),
            valid.copy(expectedBytes = -1, restoredBytes = -1), valid.copy(contentVersion = 6),
            valid.copy(fileId = "another-file"),
        )
        invalid.forEach { assertFalse(LocalDeletionGate.mayRequestConfirmation(BackupState.RESTORABLE, "file", 7, it)) }
        assertFalse(LocalDeletionGate.mayRequestConfirmation(BackupState.RESTORABLE, "file", 7, null))
    }
    @Test fun verifiedEmptyFileStillRequiresAuthenticatedRemoteEvidence() {
        assertTrue(LocalDeletionGate.mayRequestConfirmation(BackupState.RESTORABLE, "file", 7, valid.copy(expectedBytes = 0, restoredBytes = 0)))
        assertFalse(LocalDeletionGate.mayRequestConfirmation(BackupState.RESTORABLE, "", 7, valid.copy(fileId = "")))
        assertFalse(LocalDeletionGate.mayRequestConfirmation(BackupState.RESTORABLE, "file", 0, valid.copy(contentVersion = 0)))
    }
}
