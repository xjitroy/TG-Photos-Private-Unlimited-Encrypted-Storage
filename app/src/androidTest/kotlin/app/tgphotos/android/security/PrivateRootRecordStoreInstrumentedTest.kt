package app.tgphotos.android.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tgphotos.core.crypto.Argon2idParameters
import app.tgphotos.core.crypto.PasswordProtectedRoot
import java.io.File
import java.security.GeneralSecurityException
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivateRootRecordStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = UUID.randomUUID()
    private val keyId = UUID.randomUUID()
    private val codec = PasswordProtectedRoot()
    private val parameters = Argon2idParameters(65536, 3, 1)
    private val storagePurpose = PrivateRootRecordStore.Purpose.CLOUD
    private val rootPurpose = PasswordProtectedRoot.Purpose.CLOUD
    private val directory = File(context.noBackupFilesDir, "protected-roots-test-$fixture")
    private fun store() = PrivateRootRecordStore.isolatedFixture(context, fixture)
    private fun password() = "account-free synthetic root password".toCharArray()
    private fun create() = codec.create(password(), keyId, 1, rootPurpose, parameters)
    private fun unlock(record: ByteArray, pass: CharArray = password(), minimum: Long = 1) =
        codec.unlock(record, pass, keyId, 1, rootPurpose, minimum)

    @After fun cleanupIsolatedFixtureOnly() {
        check(directory.name == "protected-roots-test-$fixture")
        directory.deleteRecursively()
    }

    @Test fun actualArgon2CipherRecordSurvivesFreshStoreAndPasswordRotation() {
        val original = create()
        val root = unlock(original)
        var restored: ByteArray? = null
        try {
            store().create(storagePurpose, original)
            val disk = store().read(storagePurpose)!!
            assertArrayEquals(original, disk)
            assertEquals(152, File(directory, storagePurpose.fileName).length().toInt())
            assertFalse(disk.asList().windowed(root.size).any { it == root.asList() })
            val changed = codec.changePassword(disk, password(), "new synthetic root password".toCharArray(),
                keyId, 1, rootPurpose, 1, parameters)
            store().compareAndReplace(storagePurpose, disk, changed)
            restored = unlock(store().read(storagePurpose)!!, "new synthetic root password".toCharArray(), 2)
            assertArrayEquals(root, restored)
            try { unlock(changed).fill(0); fail("Old password accepted") }
            catch (_: GeneralSecurityException) { /* Expected. */ }
            try { store().compareAndReplace(storagePurpose, original, changed); fail("Stale writer accepted") }
            catch (_: IllegalStateException) { /* Expected without overwrite. */ }
            assertArrayEquals(changed, store().read(storagePurpose))
        } finally { root.fill(0); restored?.fill(0) }
    }

    @Test fun existingOrCorruptRecordNeverRegeneratesOrOverwrites() {
        val original = create()
        store().create(storagePurpose, original)
        try { store().create(storagePurpose, original); fail("Existing root replaced") }
        catch (_: IllegalStateException) { /* Expected. */ }
        assertArrayEquals(original, store().read(storagePurpose))
        val file = File(directory, storagePurpose.fileName)
        val corrupt = byteArrayOf(1, 2, 3)
        file.writeBytes(corrupt)
        try { store().read(storagePurpose); fail("Corrupt record accepted") }
        catch (_: IllegalStateException) { /* Expected. */ }
        try { store().create(storagePurpose, original); fail("Corrupt root silently reset") }
        catch (_: IllegalStateException) { /* Expected. */ }
        assertArrayEquals(corrupt, file.readBytes())
    }
}
