package com.securevault.passwordmanager.core.security

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Modern Key Derivation Function wrapper using PBKDF2WithHmacSHA256.
 * Recommended standard for mobile vault key derivation without native Argon2 overhead.
 * Uses 120,000 iterations to provide high brute-force resistance while remaining responsive on mobile.
 */
object KeyDerivation {

    const val DEFAULT_ITERATIONS = 120_000
    const val SALT_LENGTH_BYTES = 32
    const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure 256-bit salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit symmetric key from a master password CharArray.
     * Operates on CharArray rather than String to allow explicit memory zeroing.
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun deriveKey(
        passwordChars: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): ByteArray {
        val spec = PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = factory.generateSecret(spec)
        val encoded = secretKey.encoded
        spec.clearPassword()
        return encoded
    }

    /**
     * Wipes a char array from memory.
     */
    fun wipeChars(chars: CharArray) {
        chars.fill('\u0000')
    }

    /**
     * Wipes a byte array from memory.
     */
    fun wipeBytes(bytes: ByteArray) {
        bytes.fill(0)
    }
}
