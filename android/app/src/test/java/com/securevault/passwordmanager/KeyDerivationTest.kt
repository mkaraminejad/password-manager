package com.securevault.passwordmanager

import com.securevault.passwordmanager.core.security.KeyDerivation
import org.junit.Assert.*
import org.junit.Test

class KeyDerivationTest {

    @Test
    fun testKeyDerivationIsDeterministicForSameInput() {
        val password = "StrongMasterPassword123!".toCharArray()
        val salt = KeyDerivation.generateSalt()
        val iterations = 5000 // Reduced for fast unit testing

        val key1 = KeyDerivation.deriveKey(password, salt, iterations)
        val key2 = KeyDerivation.deriveKey(password, salt, iterations)

        assertArrayEquals("Derived keys with identical password and salt must match", key1, key2)
        assertEquals(32, key1.size) // 256 bits = 32 bytes
    }

    @Test
    fun testKeyDerivationDiffersWithDifferentSalt() {
        val password = "StrongMasterPassword123!".toCharArray()
        val salt1 = KeyDerivation.generateSalt()
        val salt2 = KeyDerivation.generateSalt()
        val iterations = 5000

        val key1 = KeyDerivation.deriveKey(password, salt1, iterations)
        val key2 = KeyDerivation.deriveKey(password, salt2, iterations)

        assertFalse("Keys with different salts must differ", key1.contentEquals(key2))
    }

    @Test
    fun testWipeZeroizesMemoryArrays() {
        val chars = "SecretData".toCharArray()
        KeyDerivation.wipeChars(chars)
        assertTrue("CharArray should be wiped with null bytes", chars.all { it == '\u0000' })

        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        KeyDerivation.wipeBytes(bytes)
        assertTrue("ByteArray should be zeroized", bytes.all { it == 0.toByte() })
    }
}
