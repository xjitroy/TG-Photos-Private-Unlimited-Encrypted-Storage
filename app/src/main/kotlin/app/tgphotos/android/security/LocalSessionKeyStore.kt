package app.tgphotos.android.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LocalSessionKeyUnavailable(cause: Exception) : Exception("Protected local session key is unavailable", cause)

/** Android binding draft: random TDLib database key, Keystore-wrapped in excluded app-private storage. */
class LocalSessionKeyStore private constructor(context: Context, directory: String, private val alias: String) {
    constructor(context: Context) : this(context, "protected-session", "tgphotos.session.wrap.v1")

    private val root = File(context.applicationContext.noBackupFilesDir, directory)
    private val file = File(root, "session-key.enc")
    private val atomic = AtomicFile(file)
    private val header = byteArrayOf(0x54, 0x47, 0x53, 0x4b, 1)
    private val associatedData = "TGPhotos/LocalSession/v1".toByteArray(Charsets.UTF_8)

    /** Caller owns/wipes returned mutable key; lock/invalidation fails closed, never silently resets a session. */
    fun loadOrCreate(): ByteArray = synchronized(processLock) {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val persisted = file.exists() || File(root, file.name + ".bak").exists()
            if (persisted) {
                val key = keyStore.getKey(alias, null) as? SecretKey
                    ?: error("Session wrapper key is missing")
                val bytes = ByteArray(66)
                var count = 0
                atomic.openRead().use { stream ->
                    while (count < bytes.size) {
                        val n = stream.read(bytes, count, bytes.size - count)
                        if (n < 0) break
                        count += n
                    }
                }
                check(count == 65 && bytes.copyOfRange(0, 5).contentEquals(header)) { "Invalid session envelope" }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(5, 17)))
                cipher.updateAAD(header); cipher.updateAAD(associatedData)
                val plaintext = cipher.doFinal(bytes, 17, 48)
                if (plaintext.size != 32) { plaintext.fill(0); error("Invalid session key size") }
                return@synchronized plaintext
            }
            check(root.isDirectory || root.mkdirs()) { "Private session directory unavailable" }
            val key = keyStore.getKey(alias, null) as? SecretKey ?: generateWrapper()
            val plaintext = ByteArray(32).also { SecureRandom().nextBytes(it) }
            var released = false
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                check(cipher.iv.size == 12) { "Unexpected session nonce size" }
                cipher.updateAAD(header); cipher.updateAAD(associatedData)
                val envelope = header + cipher.iv + cipher.doFinal(plaintext)
                val output = atomic.startWrite()
                try {
                    output.write(envelope)
                    atomic.finishWrite(output)
                } catch (failure: Exception) {
                    atomic.failWrite(output)
                    throw failure
                }
                released = true
                return@synchronized plaintext
            } finally { if (!released) plaintext.fill(0) }
        } catch (failure: Exception) {
            throw LocalSessionKeyUnavailable(failure)
        }
    }

    companion object {
        // AtomicFile is not a lock. Serialize all stores in this single-process application.
        private val processLock = Any()

        // Instrumentation fixtures must never overwrite or delete a production alias/directory.
        internal fun isolatedFixture(context: Context, id: UUID): LocalSessionKeyStore =
            LocalSessionKeyStore(context, "protected-session-test-$id", "tgphotos.session.test.$id")
    }

    private fun generateWrapper(): SecretKey {
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
        if (Build.VERSION.SDK_INT >= 28) builder.setUnlockedDeviceRequired(true)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(builder.build())
        }.generateKey()
    }
}
