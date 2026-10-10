package app.tgphotos.core.telegram

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Test seam for the official JSON C API; runtime uses NativeTdJsonDriver only. */
interface TdJsonDriver {
    val ownsGlobalReceiver: Boolean get() = false
    fun createClient(): Int
    fun send(clientId: Int, request: String)
    fun receive(timeoutSeconds: Double): String?
    fun execute(request: String): String?
}

class NativeTdJsonDriver : TdJsonDriver {
    init { NativeTdJson.load() }
    override val ownsGlobalReceiver = true
    override fun createClient() = NativeTdJson.createClient()
    override fun send(clientId: Int, request: String) = NativeTdJson.send(clientId, request)
    override fun receive(timeoutSeconds: Double) = NativeTdJson.receive(timeoutSeconds)
    override fun execute(request: String) = NativeTdJson.execute(request)
}

class TdLibRequestException(val code: Int, val retryAfterSeconds: Long?) :
    Exception("Telegram request failed (code $code)")

/** Single native receiver, ordered authorization updates, bounded correlated requests; not app-wired. */
class TdLibRequestSession(private val driver: TdJsonDriver, private val timeoutMillis: Long = 30_000) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JsonObject>>()
    private val slots = Semaphore(64)
    private val closed = AtomicBoolean(false)
    private val nativeClosed = CompletableDeferred<Unit>()
    private val state = MutableStateFlow<AuthorizationState>(AuthorizationState.Unavailable)
    val authorization: StateFlow<AuthorizationState> = state
    private val clientId: Int

    init {
        require(timeoutMillis in 1..120_000)
        check(!driver.ownsGlobalReceiver || globalOwner.compareAndSet(false, true)) {
            "Only one process owner may receive TDLib updates"
        }
        try {
            for (request in listOf(TdLibRequests.noLogStream(), TdLibRequests.noVerboseLogs())) {
                val result = driver.execute(request.toString())
                check(result != null && Json.parseToJsonElement(result).jsonObject["@type"]?.jsonPrimitive?.content == "ok") {
                    "TDLib logging suppression failed"
                }
            }
            clientId = driver.createClient()
            check(clientId > 0) { "TDLib did not create a client" }
        } catch (failure: Throwable) {
            if (driver.ownsGlobalReceiver) globalOwner.set(false)
            scope.cancel()
            throw failure
        }
        scope.launch { receiveLoop() }
    }

    suspend fun request(request: JsonObject): JsonObject {
        check(!closed.get()) { "TDLib session is closed" }
        check(slots.tryAcquire()) { "Too many outstanding Telegram requests" }
        val id = UUID.randomUUID().toString()
        val response = CompletableDeferred<JsonObject>()
        try {
            check(!closed.get()) { "TDLib session is closed" }
            pending[id] = response
            val correlated = JsonObject(request + ("@extra" to JsonPrimitive(id)))
            driver.send(clientId, correlated.toString())
            val result = withTimeout(timeoutMillis) { response.await() }
            if (result["@type"]?.jsonPrimitive?.content == "error") {
                val code = result["code"]?.jsonPrimitive?.intOrNull ?: 0
                val message = result["message"]?.jsonPrimitive?.content.orEmpty()
                val retry = if (code == 429) Regex("retry after (\\d{1,10})", RegexOption.IGNORE_CASE)
                    .find(message)?.groupValues?.get(1)?.toLongOrNull() else null
                throw TdLibRequestException(code, retry)
            }
            return result
        } finally {
            pending.remove(id)
            response.cancel()
            slots.release()
        }
    }

    suspend fun shutdown() {
        if (state.value != AuthorizationState.Closed && !closed.get()) {
            try { request(TdLibRequests.simple("close")) }
            catch (failure: Exception) {
                // The authoritative Closed update may precede the correlated close acknowledgement.
                if (!closed.get()) throw failure
            }
        }
        // Authorization publication is not a receiver-cleanup barrier. Await confirmed cleanup.
        withTimeout(timeoutMillis) { nativeClosed.await() }
    }

    internal fun outstandingRequests() = pending.size

    private suspend fun receiveLoop() {
        try {
            while (currentCoroutineContext().isActive && !closed.get()) {
                val encoded = driver.receive(0.1) ?: continue
                check(encoded.length <= 4 * 1024 * 1024) { "TDLib response exceeds safe JSON bounds" }
                val update = Json.parseToJsonElement(encoded).jsonObject
                if (update["@client_id"]?.jsonPrimitive?.intOrNull != clientId) continue
                // Native updates are applied synchronously in receive order before another receive.
                val authoritative = TdLibAuthorizationReducer.reduce(update)
                if (authoritative != null && authoritative != AuthorizationState.Closed) state.value = authoritative
                update["@extra"]?.jsonPrimitive?.contentOrNull?.let { pending.remove(it)?.complete(update) }
                if (authoritative == AuthorizationState.Closed) {
                    terminate(nativeClosureConfirmed = true)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state.value = AuthorizationState.Unavailable
            // Do not reuse a native global receiver until actual closure is observed.
            runCatching { driver.send(clientId, TdLibRequests.simple("close").toString()) }
            terminate(nativeClosureConfirmed = false)
        }
    }

    private fun terminate(nativeClosureConfirmed: Boolean) {
        if (!closed.compareAndSet(false, true)) return
        for (response in pending.values) response.completeExceptionally(IllegalStateException("TDLib session closed"))
        pending.clear()
        scope.cancel()
        if (nativeClosureConfirmed) {
            if (driver.ownsGlobalReceiver) globalOwner.set(false)
            // Publish only after no old receiver can continue and its process ownership is released.
            state.value = AuthorizationState.Closed
            nativeClosed.complete(Unit)
        } else {
            nativeClosed.completeExceptionally(IllegalStateException("TDLib native closure is unconfirmed"))
        }
    }

    override fun toString() = "TdLibRequestSession(redacted)"
    companion object { private val globalOwner = AtomicBoolean(false) }
}
