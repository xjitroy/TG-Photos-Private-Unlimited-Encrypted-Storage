package app.tgphotos.android.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tgphotos.core.telegram.AuthorizationState
import app.tgphotos.core.telegram.NativeTdJson
import app.tgphotos.core.telegram.NativeTdJsonDriver
import app.tgphotos.core.telegram.TdLibRequestSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** No setTdlibParameters, API registration, network permission, account or real file is used. */
@RunWith(AndroidJUnit4::class)
class NativeTdLibInstrumentedTest {
    @Test fun officialJsonApiPreservesStandardUtf8AndRejectsOversizedInput() {
        NativeTdJson.load()
        for (request in listOf(
            "{\"@type\":\"setLogStream\",\"log_stream\":{\"@type\":\"logStreamEmpty\"}}",
            "{\"@type\":\"setLogVerbosityLevel\",\"new_verbosity_level\":0}",
        )) {
            assertEquals("ok", Json.parseToJsonElement(NativeTdJson.execute(request)).jsonObject["@type"]
                ?.jsonPrimitive?.content)
        }
        val original = "TG Photos · বাংলা · 😀 · \u0000"
        val request = buildJsonObject {
            put("@type", "getJsonString")
            put("json_value", buildJsonObject { put("@type", "jsonValueString"); put("value", original) })
        }
        val response = Json.parseToJsonElement(NativeTdJson.execute(request.toString())).jsonObject
        assertEquals("text", response["@type"]?.jsonPrimitive?.content)
        val decoded = Json.parseToJsonElement(response["text"]!!.jsonPrimitive.content).jsonPrimitive.content
        assertEquals(original, decoded)
        var rejected = false
        try { NativeTdJson.execute("x".repeat(4 * 1024 * 1024 + 1)) }
        catch (_: RuntimeException) { rejected = true }
        assertTrue("Oversized native request must be rejected before transport", rejected)
    }

    @Test fun nativeClientWaitsForParametersAndClosesBeforeAnotherOwnerStarts() = runBlocking {
        repeat(2) {
            val session = TdLibRequestSession(NativeTdJsonDriver(), timeoutMillis = 15_000)
            try {
                session.request(buildJsonObject { put("@type", "getAuthorizationState") })
                withTimeout(15_000) { session.authorization.first { it == AuthorizationState.WaitParameters } }
                assertEquals(AuthorizationState.WaitParameters, session.authorization.value)
            } finally { session.shutdown() }
            assertEquals(AuthorizationState.Closed, session.authorization.value)
        }
    }
}
