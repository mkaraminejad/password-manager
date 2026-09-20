package com.securevault.passwordmanager

import com.securevault.passwordmanager.core.model.PasswordStrength
import com.securevault.passwordmanager.core.model.VaultEntry
import com.securevault.passwordmanager.core.security.PasswordStrengthAnalyzer
import org.junit.Assert.*
import org.junit.Test

class PasswordStrengthAnalyzerTest {

    @Test
    fun testIdentifiesWeakPasswords() {
        assertEquals(PasswordStrength.VERY_WEAK, PasswordStrengthAnalyzer.evaluate("12345"))
        assertEquals(PasswordStrength.WEAK, PasswordStrengthAnalyzer.evaluate("password123"))
        assertEquals(PasswordStrength.WEAK, PasswordStrengthAnalyzer.evaluate("qwertyuiop"))
    }

    @Test
    fun testIdentifiesStrongPasswords() {
        val strong = "Tr0ub4dor&3#K9zP!"
        val strength = PasswordStrengthAnalyzer.evaluate(strong)
        assertTrue(
            "Expected Strong or Very Strong for 17-char high-entropy string",
            strength == PasswordStrength.STRONG || strength == PasswordStrength.VERY_STRONG
        )
    }

    @Test
    fun testAuditDetectsReusedPasswordsAcrossVault() {
        val entries = listOf(
            VaultEntry(id = "1", title = "Site A", username = "user1", password = "ReusedPassword!1"),
            VaultEntry(id = "2", title = "Site B", username = "user2", password = "ReusedPassword!1"),
            VaultEntry(id = "3", title = "Site C", username = "user3", password = "UniqueComplexPassword#2026")
        )

        val summary = PasswordStrengthAnalyzer.auditVault(entries)
        assertEquals(3, summary.totalEntries)
        assertEquals(2, summary.reusedPasswordsCount)

        val resultA = summary.results.first { it.entryId == "1" }
        assertTrue("Entry 1 should be marked as reused", resultA.isReused)
        assertEquals(2, resultA.reuseCount)

        val resultC = summary.results.first { it.entryId == "3" }
        assertFalse("Entry 3 should not be reused", resultC.isReused)
    }
}
