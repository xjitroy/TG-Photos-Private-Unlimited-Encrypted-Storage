package app.tgphotos.android.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android provider checks; fixtures contain no account, owner credentials or production keys. */
@RunWith(AndroidJUnit4::class)
class LocalSessionKeyStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var id: UUID
    private lateinit var root: File
    private lateinit var alias: String
    private val returnedKeys = mutableListOf<ByteArray>()

    @Before fun isolate() {
        id = UUID.randomUUID()
        root = File(context.noBackupFilesDir, "protected-session-test-$id")
        alias = "tgphotos.session.test.$id"
    }

    @After fun removeFixture() {
        returnedKeys.forEach { it.fill(0) }
        // Names are generated locally; never touch the production alias/directory.
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        root.deleteRecursively()
    }

    private fun load(): ByteArray = LocalSessionKeyStore.isolatedFixture(context, id).loadOrCreate()
        .also { returnedKeys += it }

    @Test fun envelopeSurvivesFreshInstance() {
        val first = load()
        val second = load()
        assertEquals(32, first.size)
        assertTrue("Fresh instance must recover the same random session key", first.contentEquals(second))
        val envelope = File(root, "session-key.enc").readBytes()
        assertEquals(65, envelope.size)
        assertTrue("Persistent bytes must be an authenticated envelope", envelope.take(5)
            .toByteArray().contentEquals(byteArrayOf(0x54, 0x47, 0x53, 0x4b, 1)))
        assertTrue("Session key must not occur verbatim in the envelope", envelope.asList()
            .windowed(first.size).none { it.toByteArray().contentEquals(first) })
    }

    @Test fun simultaneousInstancesDoNotCreateDifferentKeys() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures = executor.invokeAll(List(2) { Callable {
                LocalSessionKeyStore.isolatedFixture(context, id).loadOrCreate()
            } }, 30, TimeUnit.SECONDS)
            val keys = futures.map { it.get(1, TimeUnit.SECONDS).also(returnedKeys::add) }
            assertTrue("Concurrent stores must agree", keys[0].contentEquals(keys[1]))
            assertTrue("Persisted key must agree", keys[0].contentEquals(load()))
        } finally { executor.shutdownNow() }
    }

    @Test fun corruptEnvelopeFailsWithoutReset() {
        load()
        val file = File(root, "session-key.enc")
        val changed = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        file.writeBytes(changed)
        expectUnavailable()
        assertTrue("Corruption must not trigger key replacement", changed.contentEquals(file.readBytes()))
    }

    @Test fun missingWrapperFailsWithoutReset() {
        load()
        val file = File(root, "session-key.enc")
        val unchanged = file.readBytes()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        expectUnavailable()
        assertTrue("Missing wrapper must not reset the session", unchanged.contentEquals(file.readBytes()))
    }

    private fun expectUnavailable() {
        var failedClosed = false
        try { load() } catch (_: LocalSessionKeyUnavailable) { failedClosed = true }
        assertTrue("Invalid session storage must fail closed", failedClosed)
    }
}
