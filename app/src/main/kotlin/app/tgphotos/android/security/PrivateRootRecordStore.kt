package app.tgphotos.android.security

import android.content.Context
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID

/**
 * App-private persistence draft for PasswordProtectedRoot records, never plaintext root/password bytes.
 * Authentication is performed by the root codec on every unlock, not by this storage adapter.
 * Single-process only: cross-process writers and hardware-backed rollback anchoring are not implemented.
 */
class PrivateRootRecordStore private constructor(context: Context, directory: String) {
    constructor(context: Context) : this(context, "protected-roots")

    enum class Purpose(val fileName: String, val wire: Byte) {
        CLOUD("cloud-root.enc", 1), VAULT("vault-root.enc", 2),
    }

    private val parent = context.applicationContext.noBackupFilesDir.canonicalFile
    private val root = File(parent, directory)

    /** Missing is returned explicitly; corrupt/truncated records fail without deletion or regeneration. */
    fun read(purpose: Purpose): ByteArray? = synchronized(processLock) { readLocked(purpose) }

    /** Setup is create-only: callers must not replace an existing root on retry or password failure. */
    fun create(purpose: Purpose, record: ByteArray) = synchronized(processLock) {
        check(record.size == RECORD_BYTES) { "Invalid protected root record length" }
        val snapshot = record.copyOf()
        validate(snapshot, purpose)
        check(readLocked(purpose) == null) { "Protected root already exists" }
        writeLocked(purpose, snapshot)
    }

    /** Password rotation must supply the exact previous encrypted record; stale writers fail closed. */
    fun compareAndReplace(purpose: Purpose, expected: ByteArray, replacement: ByteArray) = synchronized(processLock) {
        check(expected.size == RECORD_BYTES && replacement.size == RECORD_BYTES) { "Invalid protected root record length" }
        val old = expected.copyOf()
        val next = replacement.copyOf()
        validate(old, purpose); validate(next, purpose)
        val current = readLocked(purpose) ?: error("Protected root is missing")
        check(MessageDigest.isEqual(current, old)) { "Protected root changed; reload before rotation" }
        writeLocked(purpose, next)
    }

    private fun file(purpose: Purpose): File = File(root, purpose.fileName)

    private fun checkPaths(purpose: Purpose) {
        check(!Files.isSymbolicLink(root.toPath())) { "Invalid private root directory" }
        if (root.exists()) check(root.isDirectory && root.canonicalFile.parentFile == parent) { "Invalid private root directory" }
        val base = file(purpose)
        for (candidate in listOf(base, File(root, base.name + ".new"), File(root, base.name + ".bak"))) {
            check(!Files.isSymbolicLink(candidate.toPath())) { "Invalid private root record path" }
            if (candidate.exists()) check(candidate.isFile) { "Invalid private root record path" }
        }
    }

    private fun readLocked(purpose: Purpose): ByteArray? {
        checkPaths(purpose)
        val base = file(purpose)
        if (!base.exists() && !File(root, base.name + ".bak").exists()) return null
        val bytes = ByteArray(RECORD_BYTES + 1)
        var count = 0
        AtomicFile(base).openRead().use { input ->
            while (count < bytes.size) {
                val n = input.read(bytes, count, bytes.size - count)
                if (n < 0) break
                count += n
            }
        }
        check(count == RECORD_BYTES) { "Invalid protected root record length" }
        return bytes.copyOf(RECORD_BYTES).also { validate(it, purpose) }
    }

    private fun writeLocked(purpose: Purpose, record: ByteArray) {
        checkPaths(purpose)
        check(root.isDirectory || root.mkdir()) { "Private root directory unavailable" }
        Os.chmod(root.absolutePath, 448) // 0700
        val atomic = AtomicFile(file(purpose))
        val output = atomic.startWrite()
        try {
            output.write(record)
            output.fd.sync()
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
        atomic.finishWrite(output)
        // AtomicFile can log a failed rename rather than throw. Verify the actual promoted bytes.
        check(readLocked(purpose)?.contentEquals(record) == true) { "Protected root commit is unconfirmed" }
        Os.chmod(file(purpose).absolutePath, 384) // 0600
        syncDirectory(root)
        syncDirectory(parent)
    }

    private fun syncDirectory(directory: File) {
        val fd = Os.open(directory.absolutePath, OsConstants.O_RDONLY or OsConstants.O_DIRECTORY or OsConstants.O_NOFOLLOW, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    }

    private fun validate(record: ByteArray, purpose: Purpose) {
        // Fixed v1 wire size. This only checks framing: successful password AEAD authentication
        // remains mandatory before using any root or accepting a password change.
        check(record.size == RECORD_BYTES && record.copyOfRange(0, 4).contentEquals(MAGIC) &&
            record[4] == 1.toByte() && record[5] == purpose.wire) { "Invalid protected root framing" }
    }

    companion object {
        private const val RECORD_BYTES = 152
        private val MAGIC = byteArrayOf(84, 71, 80, 82)
        private val processLock = Any()
        internal fun isolatedFixture(context: Context, id: UUID) =
            PrivateRootRecordStore(context, "protected-roots-test-$id")
    }
}
