package app.tgphotos.core.crypto

/** Argon2id parameters are versioned, bounded and device-calibrated; no PBKDF2 or hash fallback. */
data class Argon2idParameters(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
    init {
        require(memoryKiB in 65536..262144)
        require(iterations in 3..10)
        require(parallelism in 1..4)
    }
}

interface PasswordKeyDerivation {
    /** Implementations must wipe mutable credential/intermediate buffers where possible. */
    fun deriveArgon2id(password: CharArray, salt: ByteArray, parameters: Argon2idParameters): ByteArray
}

class UnavailablePasswordKeyDerivation : PasswordKeyDerivation {
    override fun deriveArgon2id(password: CharArray, salt: ByteArray, parameters: Argon2idParameters): Nothing {
        password.fill('\u0000')
        throw IllegalStateException("Reviewed Argon2id backend is not integrated")
    }
}
