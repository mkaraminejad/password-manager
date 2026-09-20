package com.securevault.passwordmanager.core.model

import java.util.UUID

/**
 * Domain representation of an encrypted credential item stored in the secure vault.
 * All sensitive values in memory should be discarded or overwritten when no longer needed.
 */
data class VaultEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    val folder: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
