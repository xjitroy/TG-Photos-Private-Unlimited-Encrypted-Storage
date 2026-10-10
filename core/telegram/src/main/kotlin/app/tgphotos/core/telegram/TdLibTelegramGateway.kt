package app.tgphotos.core.telegram

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

/** Real TDLib request/state adapter draft. Native ABI build, local session store and UI are separate gates. */
class TdLibTelegramGateway(
    private val session: TdLibRequestSession,
    private val credentials: TelegramApiCredentials,
    private val configuration: TdLibSessionConfiguration,
) : TelegramGateway {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow<AuthorizationState>(AuthorizationState.Unavailable)
    override val authorization: StateFlow<AuthorizationState> = state
    private var parametersSubmitted = false

    init {
        scope.launch {
            session.authorization.collect { authoritative ->
                state.value = authoritative
                if (authoritative == AuthorizationState.Closed) configuration.close()
                if (authoritative == AuthorizationState.WaitParameters && !parametersSubmitted) {
                    parametersSubmitted = true
                    try { session.request(TdLibRequests.parameters(credentials, configuration)) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { state.value = AuthorizationState.Unavailable }
                }
            }
        }
    }

    override suspend fun submitPhone(e164: String) {
        check(authorization.value == AuthorizationState.WaitPhone)
        session.request(TdLibRequests.phone(e164))
    }
    override suspend fun submitCode(code: CharArray) {
        try {
            check(authorization.value is AuthorizationState.WaitCode)
            session.request(TdLibRequests.code(code))
        } finally { code.fill('\u0000') }
    }
    override suspend fun submitTwoFactorPassword(password: CharArray) {
        try {
            check(authorization.value is AuthorizationState.WaitPassword)
            session.request(TdLibRequests.twoFactor(password))
        } finally { password.fill('\u0000') }
    }
    override suspend fun logout() { session.request(TdLibRequests.simple("logOut")) }
    override suspend fun submitEmailAddress(address: CharArray) {
        try {
            check(authorization.value == AuthorizationState.WaitEmail)
            session.request(TdLibRequests.emailAddress(address))
        } finally { address.fill('\u0000') }
    }
    override suspend fun submitEmailCode(code: CharArray) {
        try {
            check(authorization.value is AuthorizationState.WaitEmailCode)
            session.request(TdLibRequests.emailCode(code))
        } finally { code.fill('\u0000') }
    }
    override suspend fun resendCode() {
        check(authorization.value is AuthorizationState.WaitCode)
        session.request(TdLibRequests.resendCode())
    }
    suspend fun shutdown() {
        session.shutdown()
        configuration.close()
        scope.cancel()
    }
    override fun toString() = "TdLibTelegramGateway(redacted)"
}
