package com.securevault.passwordmanager

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManagerTest {

    private val secureRandom = SecureRandom()

    @Test
    fun testAesGcmEncryptionDecryptionRoundtrip() {
        val keyBytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val plaintext = "HighlyConfidentialPassword!987".toByteArray(Charsets.UTF_8)

        // Encrypt with 12-byte IV and 128-bit tag
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val packed = ByteBuffer.allocate(iv.size + ciphertext.size).put(iv).put(ciphertext).array()

        // Decrypt
        val decIv = packed.copyOfRange(0, 12)
        val decCiphertext = packed.copyOfRange(12, packed.size)

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, decIv))
        val decrypted = decCipher.doFinal(decCiphertext)

        assertEquals("Decrypted text must match plaintext", String(plaintext), String(decrypted))
    }

    @Test(expected = AEADBadTagException::class)
    fun testTamperedCiphertextThrowsAEADBadTagException() {
        val keyBytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val plaintext = "TamperResistanceTest".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Tamper with the last byte (authentication tag)
        ciphertext[ciphertext.size - 1] = (ciphertext[ciphertext.size - 1] + 1).toByte()

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        decCipher.doFinal(ciphertext) // Must throw AEADBadTagException
    }
}
