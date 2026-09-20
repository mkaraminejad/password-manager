package com.securevault.passwordmanager.core.model

enum class PasswordStrength(val label: String, val score: Int) {
    VERY_WEAK("Very Weak", 1),
    WEAK("Weak", 2),
    FAIR("Fair", 3),
    STRONG("Strong", 4),
    VERY_STRONG("Very Strong", 5)
}

data class PasswordAuditResult(
    val entryId: String,
    val title: String,
    val username: String,
    val strength: PasswordStrength,
    val isReused: Boolean,
    val reuseCount: Int = 1,
    val issues: List<String>
)

data class VaultAuditSummary(
    val totalEntries: Int,
    val weakPasswordsCount: Int,
    val reusedPasswordsCount: Int,
    val strongPasswordsCount: Int,
    val results: List<PasswordAuditResult>
)
