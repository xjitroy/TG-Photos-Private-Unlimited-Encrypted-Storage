package app.tgphotos.core.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Adapter must map official TDLib authorization updates; UI cannot declare Ready. */
sealed interface AuthorizationState {
    data object Unavailable : AuthorizationState
    data object WaitParameters : AuthorizationState
    data class Unsupported(val stateType: String) : AuthorizationState
    data object WaitPhone : AuthorizationState
    data class WaitCode(val deliveryDescription: String, val expectedLength: Int?) : AuthorizationState
    data class WaitPassword(val hint: String) : AuthorizationState
    data object WaitEmail : AuthorizationState
    data object WaitEmailCode : AuthorizationState
    data object Ready : AuthorizationState
    data object LoggingOut : AuthorizationState
    data object Closed : AuthorizationState
}

interface TelegramGateway {
    val authorization: StateFlow<AuthorizationState>
    suspend fun submitPhone(e164: String)
    suspend fun submitCode(code: CharArray)
    suspend fun submitTwoFactorPassword(password: CharArray)
    suspend fun logout()
}

/** No fake OTP, synthetic session or successful login in the application wiring. */
class UnavailableTelegramGateway : TelegramGateway {
    override val authorization: StateFlow<AuthorizationState> = MutableStateFlow(AuthorizationState.Unavailable)
    override suspend fun submitPhone(e164: String): Nothing = unavailable()
    override suspend fun submitCode(code: CharArray): Nothing {
        code.fill('\u0000')
        unavailable()
    }
    override suspend fun submitTwoFactorPassword(password: CharArray): Nothing {
        password.fill('\u0000')
        unavailable()
    }
    override suspend fun logout(): Nothing = unavailable()
    private fun unavailable(): Nothing = throw IllegalStateException("Official TDLib adapter is not integrated")
}
