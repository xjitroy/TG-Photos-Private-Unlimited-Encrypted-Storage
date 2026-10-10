package app.tgphotos.core.telegram

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class TdLibRequestSessionTest {
    private class Driver(
        private val loggingResult: String? = "{\"@type\":\"ok\"}",
        override val ownsGlobalReceiver: Boolean = false,
        private val acknowledgeClose: Boolean = true,
    ) : TdJsonDriver {
        val outbound = LinkedBlockingQueue<JsonObject>()
        val inbound = LinkedBlockingQueue<String>()
        var createdClients = 0
        override fun createClient(): Int { createdClients++; return 7 }
        override fun execute(request: String) = loggingResult
        override fun receive(timeoutSeconds: Double): String? = inbound.poll(100, TimeUnit.MILLISECONDS)
        override fun send(clientId: Int, request: String) {
            val json = Json.parseToJsonElement(request).jsonObject
            if (json["@type"]?.jsonPrimitive?.content == "close") {
                if (acknowledgeClose) reply(json)
                auth("authorizationStateClosed")
            } else outbound.add(json)
        }
        fun reply(request: JsonObject, client: Int = 7, type: String = "ok", fields: Map<String, JsonElement> = emptyMap()) {
            inbound.add(JsonObject(fields + mapOf("@type" to JsonPrimitive(type), "@client_id" to JsonPrimitive(client),
                "@extra" to request.getValue("@extra"))).toString())
        }
        fun auth(type: String) {
            inbound.add(buildJsonObject {
                put("@type", "updateAuthorizationState"); put("@client_id", 7)
                putJsonObject("authorization_state") { put("@type", type) }
            }.toString())
        }
        suspend fun sent(): JsonObject = withContext(Dispatchers.IO) {
            outbound.poll(2, TimeUnit.SECONDS) ?: error("Request was not sent")
        }
    }
    private fun request() = TdLibRequests.simple("getAuthorizationState")

    @Test fun absentOrUnexpectedLoggingAcknowledgementPreventsClientCreation() {
        for (result in listOf(null, "{\"@type\":\"error\",\"message\":\"private fixture\"}", "{\"@type\":\"unknown\"}")) {
            val driver = Driver(result)
            try { TdLibRequestSession(driver); fail("Unconfirmed log suppression accepted") }
            catch (failure: IllegalStateException) {
                assertEquals(0, driver.createdClients)
                assertFalse(failure.message.orEmpty().contains("private fixture"))
            }
        }
    }

    @Test fun confirmedShutdownAllowsImmediateNextReceiverOwner() = runBlocking {
        repeat(25) {
            val session = TdLibRequestSession(Driver(ownsGlobalReceiver = true), timeoutMillis = 2000)
            session.shutdown()
            assertEquals(AuthorizationState.Closed, session.authorization.value)
            // Idempotent shutdown must await the same complete cleanup, not just the state flag.
            session.shutdown()
        }
    }

    @Test fun authoritativeClosureBeforeAcknowledgementStillCompletesCleanup() = runBlocking {
        val session = TdLibRequestSession(Driver(ownsGlobalReceiver = true, acknowledgeClose = false),
            timeoutMillis = 2000)
        session.shutdown()
        assertEquals(AuthorizationState.Closed, session.authorization.value)
        val next = TdLibRequestSession(Driver(ownsGlobalReceiver = true), timeoutMillis = 2000)
        next.shutdown()
    }

    @Test fun responsesCorrelateWhenReceivedOutOfOrder() = runBlocking {
        val driver = Driver(); val session = TdLibRequestSession(driver)
        try {
            val a = async { session.request(request()) }; val first = driver.sent()
            val b = async { session.request(request()) }; val second = driver.sent()
            driver.reply(second, fields = mapOf("value" to JsonPrimitive(2)))
            driver.reply(first, fields = mapOf("value" to JsonPrimitive(1)))
            assertEquals(1, a.await()["value"]?.jsonPrimitive?.int)
            assertEquals(2, b.await()["value"]?.jsonPrimitive?.int)
            assertEquals(0, session.outstandingRequests())
        } finally { session.shutdown() }
    }

    @Test fun anotherClientCannotCompleteThisClientRequest() = runBlocking {
        val driver = Driver(); val session = TdLibRequestSession(driver)
        try {
            val result = async { session.request(request()) }; val sent = driver.sent()
            driver.reply(sent, client = 99, fields = mapOf("value" to JsonPrimitive(999)))
            driver.reply(sent, fields = mapOf("value" to JsonPrimitive(7)))
            assertEquals(7, result.await()["value"]?.jsonPrimitive?.int)
        } finally { session.shutdown() }
    }

    @Test fun onlyOfficialAuthorizationUpdatesCanChangeAuthorization() = runBlocking {
        val driver = Driver(); val session = TdLibRequestSession(driver)
        try {
            val result = async { session.request(request()) }; val sent = driver.sent()
            driver.reply(sent, type = "authorizationStateReady"); result.await()
            assertEquals(AuthorizationState.Unavailable, session.authorization.value)
            driver.auth("authorizationStateWaitPhoneNumber")
            withTimeout(2000) { session.authorization.first { it == AuthorizationState.WaitPhone } }
            driver.auth("authorizationStateReady")
            withTimeout(2000) { session.authorization.first { it == AuthorizationState.Ready } }
            assertEquals(AuthorizationState.Ready, session.authorization.value)
        } finally { session.shutdown() }
    }

    @Test fun serverErrorCarriesRetryDelayWithoutEchoingRawMessage() = runBlocking {
        val driver = Driver(); val session = TdLibRequestSession(driver)
        try {
            val result = async {
                try { session.request(request()); error("Server error accepted") }
                catch (error: TdLibRequestException) { error }
            }
            val sent = driver.sent()
            driver.reply(sent, type = "error", fields = mapOf("code" to JsonPrimitive(429),
                "message" to JsonPrimitive("Too Many Requests: retry after 17; private fixture details")))
            val error = result.await()
            assertEquals(429, error.code); assertEquals(17L, error.retryAfterSeconds)
            assertFalse(error.message.orEmpty().contains("private fixture"))
        } finally { session.shutdown() }
    }

    @Test fun timeoutCleansPendingCorrelationAndClosureIsConfirmed() = runBlocking {
        val driver = Driver(); val session = TdLibRequestSession(driver, timeoutMillis = 300)
        try {
            try { session.request(request()); fail("Request did not time out") }
            catch (_: TimeoutCancellationException) { assertEquals(0, session.outstandingRequests()) }
        } finally { session.shutdown() }
        assertEquals(AuthorizationState.Closed, session.authorization.value)
    }

    @Test fun malformedNativeResponseFailsClosed() = runBlocking {
        val driver = Driver(); val session = TdLibRequestSession(driver)
        val result = async {
            try { session.request(request()); false }
            catch (_: IllegalStateException) { true }
        }
        driver.sent(); driver.inbound.add("{invalid native JSON")
        assertTrue(withTimeout(2000) { result.await() })
        assertEquals(AuthorizationState.Unavailable, session.authorization.value)
        assertEquals(0, session.outstandingRequests())
    }
}
