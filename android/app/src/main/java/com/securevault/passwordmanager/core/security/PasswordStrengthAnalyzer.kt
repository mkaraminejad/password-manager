package com.securevault.passwordmanager.core.security

import com.securevault.passwordmanager.core.model.PasswordAuditResult
import com.securevault.passwordmanager.core.model.PasswordStrength
import com.securevault.passwordmanager.core.model.VaultAuditSummary
import com.securevault.passwordmanager.core.model.VaultEntry
import kotlin.math.log2

/**
 * Evaluates password entropy, detects weak passwords, and identifies reused credentials across the vault.
 */
object PasswordStrengthAnalyzer {

    private val COMMON_PATTERNS = listOf(
        "password", "123456", "12345678", "qwerty", "admin", "welcome",
        "football", "iloveyou", "monkey", "dragon", "starwars", "master"
    )

    fun evaluate(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.VERY_WEAK
        if (password.length < 8) return PasswordStrength.VERY_WEAK

        val lowerLower = password.lowercase()
        for (pattern in COMMON_PATTERNS) {
            if (lowerLower.contains(pattern)) return PasswordStrength.WEAK
        }

        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        // Entropy in bits = length * log2(poolSize)
        val entropy = password.length * log2(poolSize.toDouble().coerceAtLeast(1.0))

        return when {
            entropy < 36 || password.length < 10 -> PasswordStrength.WEAK
            entropy < 56 || password.length < 12 -> PasswordStrength.FAIR
            entropy < 75 || password.length < 16 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    /**
     * Audits an entire list of vault entries for vulnerabilities.
     */
    fun auditVault(entries: List<VaultEntry>): VaultAuditSummary {
        // Count frequencies of passwords to detect reuse
        val passwordCountMap = mutableMapOf<String, Int>()
        for (entry in entries) {
            if (entry.password.isNotEmpty()) {
                passwordCountMap[entry.password] = (passwordCountMap[entry.password] ?: 0) + 1
            }
        }

        val results = mutableListOf<PasswordAuditResult>()
        var weakCount = 0
        var reusedCount = 0
        var strongCount = 0

        for (entry in entries) {
            val strength = evaluate(entry.password)
            val reuseCount = passwordCountMap[entry.password] ?: 1
            val isReused = reuseCount > 1

            val issues = mutableListOf<String>()
            if (entry.password.length < 12) {
                issues.add("Short length (${entry.password.length} chars, 16+ recommended)")
            }
            if (strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK) {
                issues.add("Low entropy / predictable composition")
            }
            if (isReused) {
                issues.add("Password reused in $reuseCount accounts")
            }

            if (strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK) {
                weakCount++
            } else if (strength >= PasswordStrength.STRONG) {
                strongCount++
            }

            if (isReused) {
                reusedCount++
            }

            results.add(
                PasswordAuditResult(
                    entryId = entry.id,
                    title = entry.title,
                    username = entry.username,
                    strength = strength,
                    isReused = isReused,
                    reuseCount = reuseCount,
                    issues = issues
                )
            )
        }

        return VaultAuditSummary(
            totalEntries = entries.size,
            weakPasswordsCount = weakCount,
            reusedPasswordsCount = reusedCount,
            strongPasswordsCount = strongCount,
            results = results
        )
    }
}
