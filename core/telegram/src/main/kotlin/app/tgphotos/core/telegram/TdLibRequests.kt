package app.tgphotos.core.telegram

import kotlinx.serialization.json.*
import java.io.File
import java.util.Base64

/** Runtime credentials only. No BuildConfig, source constants, saved UI state or diagnostic serialization. */
class TelegramApiCredentials(val apiId: Int, private val apiHash: String) {
    init { require(apiId > 0); require(apiHash.matches(Regex("[a-fA-F0-9]{32}"))) }
    internal fun putInto(builder: JsonObjectBuilder) { builder.put("api_id", apiId); builder.put("api_hash", apiHash) }
    override fun toString(): String = "TelegramApiCredentials(redacted)"
}

class TdLibSessionConfiguration(
    val appPrivateRoot: File,
    val databaseDirectory: File,
    val filesDirectory: File,
    private val databaseKey: ByteArray,
) {
    init {
        require(appPrivateRoot.isAbsolute && databaseDirectory.isAbsolute && filesDirectory.isAbsolute)
        val root = appPrivateRoot.canonicalFile.toPath()
        require(databaseDirectory.canonicalFile.toPath().startsWith(root))
        require(filesDirectory.canonicalFile.toPath().startsWith(root))
        require(databaseKey.size == 32)
    }
    internal fun encryptedDatabaseKey(): String = Base64.getEncoder().encodeToString(databaseKey)
    override fun toString(): String = "TdLibSessionConfiguration(redacted)"
}

object TdLibRequests {
    fun parameters(credentials: TelegramApiCredentials, session: TdLibSessionConfiguration): JsonObject = buildJsonObject {
        put("@type", "setTdlibParameters")
        put("use_test_dc", false)
        put("database_directory", session.databaseDirectory.canonicalPath)
        put("files_directory", session.filesDirectory.canonicalPath)
        put("database_encryption_key", session.encryptedDatabaseKey())
        put("use_file_database", true)
        put("use_chat_info_database", false)
        put("use_message_database", false)
        put("use_secret_chats", false)
        credentials.putInto(this)
        put("system_language_code", "en")
        put("device_model", "Android")
        put("system_version", "")
        put("application_version", "TG Photos 0.1.0")
    }

    fun phone(e164: String): JsonObject {
        require(e164.matches(Regex("\\+[1-9][0-9]{6,14}")))
        return buildJsonObject { put("@type", "setAuthenticationPhoneNumber"); put("phone_number", e164); put("settings", JsonNull) }
    }
    fun code(code: CharArray): JsonObject = credentialRequest("checkAuthenticationCode", "code", code)
    fun twoFactor(password: CharArray): JsonObject = credentialRequest("checkAuthenticationPassword", "password", password)
    fun simple(type: String): JsonObject {
        require(type in setOf("getAuthorizationState", "close", "logOut", "resendAuthenticationCode"))
        return buildJsonObject { put("@type", type) }
    }
    fun noLogStream(): JsonObject = buildJsonObject {
        put("@type", "setLogStream"); putJsonObject("log_stream") { put("@type", "logStreamEmpty") }
    }
    fun noVerboseLogs(): JsonObject = buildJsonObject { put("@type", "setLogVerbosityLevel"); put("new_verbosity_level", 0) }
    private fun credentialRequest(type: String, field: String, value: CharArray): JsonObject {
        try {
            require(value.isNotEmpty() && value.size <= 1024)
            return buildJsonObject { put("@type", type); put(field, String(value)) }
        } finally { value.fill('\u0000') }
        // JSON/native libraries create immutable string copies; do not claim complete JVM memory erasure.
    }
}

object TdLibAuthorizationReducer {
    fun reduce(update: JsonObject): AuthorizationState? {
        if (update["@type"]?.jsonPrimitive?.content != "updateAuthorizationState") return null
        val state = update["authorization_state"]?.jsonObject ?: return AuthorizationState.Unavailable
        val type = state["@type"]?.jsonPrimitive?.content ?: return AuthorizationState.Unavailable
        return when (type) {
            "authorizationStateWaitTdlibParameters" -> AuthorizationState.WaitParameters
            "authorizationStateWaitPhoneNumber" -> AuthorizationState.WaitPhone
            "authorizationStateWaitCode" -> {
                val delivery = state["code_info"]?.jsonObject?.get("type")?.jsonObject
                AuthorizationState.WaitCode(delivery?.get("@type")?.jsonPrimitive?.content ?: "Unknown delivery method",
                    delivery?.get("length")?.jsonPrimitive?.intOrNull)
            }
            "authorizationStateWaitPassword" -> AuthorizationState.WaitPassword(state["password_hint"]?.jsonPrimitive?.content ?: "")
            "authorizationStateWaitEmailAddress" -> AuthorizationState.WaitEmail
            "authorizationStateWaitEmailCode" -> AuthorizationState.WaitEmailCode
            "authorizationStateReady" -> AuthorizationState.Ready
            "authorizationStateLoggingOut" -> AuthorizationState.LoggingOut
            "authorizationStateClosed" -> AuthorizationState.Closed
            else -> AuthorizationState.Unsupported(type)
        }
    }
}
