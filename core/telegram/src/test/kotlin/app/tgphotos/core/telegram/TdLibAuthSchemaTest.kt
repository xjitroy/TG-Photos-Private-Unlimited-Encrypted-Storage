package app.tgphotos.core.telegram

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Exact shapes checked against the recorded official TDLib 1.8.68 schema; no live account involved. */
class TdLibAuthSchemaTest {
    @Test fun emailCodeUsesNestedOfficialAuthenticationTypeAndWipesInput() {
        val input = "123456".toCharArray()
        val request = TdLibRequests.emailCode(input)
        assertEquals("checkAuthenticationEmailCode", request["@type"]?.jsonPrimitive?.content)
        assertEquals("emailAddressAuthenticationCode", request["code"]?.jsonObject?.get("@type")?.jsonPrimitive?.content)
        assertEquals("123456", request["code"]?.jsonObject?.get("code")?.jsonPrimitive?.content)
        assertTrue(input.all { it == '\u0000' })
    }

    @Test fun malformedEmailIsRejectedAndItsMutableInputIsWiped() {
        val input = "invalid address".toCharArray()
        try { TdLibRequests.emailAddress(input); fail("Malformed email accepted") }
        catch (_: IllegalArgumentException) { assertTrue(input.all { it == '\u0000' }) }
        val valid = "fixture@example.test".toCharArray()
        assertEquals("fixture@example.test", TdLibRequests.emailAddress(valid)["email_address"]?.jsonPrimitive?.content)
        assertTrue(valid.all { it == '\u0000' })
    }

    @Test fun authoritativeEmailStateRetainsMaskedDeliveryInformation() {
        val update = buildJsonObject {
            put("@type", "updateAuthorizationState")
            putJsonObject("authorization_state") {
                put("@type", "authorizationStateWaitEmailCode")
                putJsonObject("code_info") { put("email_address_pattern", "f***@example.test"); put("length", 6) }
            }
        }
        assertEquals(AuthorizationState.WaitEmailCode("f***@example.test", 6), TdLibAuthorizationReducer.reduce(update))
        assertEquals(JsonNull, TdLibRequests.resendCode()["reason"])
    }

    @Test fun unavailableGatewayDoesNotAcceptOrRetainEmailSecrets() = runBlocking {
        val gateway = UnavailableTelegramGateway()
        for (email in listOf(true, false)) {
            val input = "private-fixture".toCharArray()
            try {
                if (email) gateway.submitEmailAddress(input) else gateway.submitEmailCode(input)
                fail("Unavailable gateway accepted authentication")
            } catch (_: IllegalStateException) { assertTrue(input.all { it == '\u0000' }) }
        }
        assertEquals(AuthorizationState.Unavailable, gateway.authorization.value)
    }
}
