package com.securevault.passwordmanager

import com.securevault.passwordmanager.core.security.PasswordGenerator
import org.junit.Assert.*
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun testGeneratedPasswordHasRequestedLength() {
        val lengths = listOf(8, 16, 24, 32, 48)
        for (len in lengths) {
            val config = PasswordGenerator.GeneratorConfig(length = len)
            val password = PasswordGenerator.generatePassword(config)
            assertEquals("Password length should match requested length", len, password.length)
        }
    }

    @Test
    fun testGeneratedPasswordIncludesAllSelectedCharacterClasses() {
        val config = PasswordGenerator.GeneratorConfig(
            length = 20,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true,
            excludeAmbiguous = false
        )
        val password = PasswordGenerator.generatePassword(config)

        assertTrue("Should contain lowercase", password.any { it.isLowerCase() })
        assertTrue("Should contain uppercase", password.any { it.isUpperCase() })
        assertTrue("Should contain digit", password.any { it.isDigit() })
        assertTrue("Should contain symbol", password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun testExcludesAmbiguousCharacters() {
        val ambiguousChars = setOf('0', 'O', '1', 'l', 'I', '|')
        val config = PasswordGenerator.GeneratorConfig(
            length = 40,
            excludeAmbiguous = true
        )

        for (i in 0 until 50) {
            val password = PasswordGenerator.generatePassword(config)
            assertFalse(
                "Password must not contain ambiguous characters",
                password.any { it in ambiguousChars }
            )
        }
    }

    @Test
    fun testPassphraseGeneratesCorrectWordCountAndSeparator() {
        val config = PasswordGenerator.PassphraseConfig(
            wordCount = 5,
            separator = ".",
            capitalizeWords = true,
            includeNumber = false
        )
        val passphrase = PasswordGenerator.generatePassphrase(config)
        val parts = passphrase.split(".")

        assertEquals("Passphrase should have 5 words", 5, parts.size)
        assertTrue("Words should be capitalized", parts.all { it[0].isUpperCase() })
    }
}
