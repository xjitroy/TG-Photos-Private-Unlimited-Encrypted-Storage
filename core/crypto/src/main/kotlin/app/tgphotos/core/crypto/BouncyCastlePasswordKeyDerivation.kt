package app.tgphotos.core.crypto

import org.bouncycastle.crypto.PasswordConverter
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** Draft Argon2id 1.3 backend; run off the UI thread and calibrate on-device before wiring. */
class BouncyCastlePasswordKeyDerivation : PasswordKeyDerivation {
    @Synchronized
    override fun deriveArgon2id(password: CharArray, salt: ByteArray, parameters: Argon2idParameters): ByteArray {
        var encoded: ByteArray? = null
        var builder: Argon2Parameters.Builder? = null
        var config: Argon2Parameters? = null
        val output = ByteArray(32)
        var success = false
        try {
            require(password.isNotEmpty() && password.size <= 1024)
            require(salt.size in 16..64)
            encoded = PasswordConverter.UTF8.convert(password)
            builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(parameters.memoryKiB)
                .withIterations(parameters.iterations)
                .withParallelism(parameters.parallelism)
                .withSalt(salt)
            config = builder.build()
            val generator = Argon2BytesGenerator()
            generator.init(config)
            generator.generateBytes(encoded, output)
            success = true
            return output
        } finally {
            password.fill('\u0000')
            encoded?.fill(0)
            builder?.clear()
            config?.clear()
            if (!success) output.fill(0)
            // BC resets successful work memory; exception paths/JVM copies are not an erasure guarantee.
        }
    }
}
