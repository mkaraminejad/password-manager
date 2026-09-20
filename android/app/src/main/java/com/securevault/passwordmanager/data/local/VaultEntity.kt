package com.securevault.passwordmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.securevault.passwordmanager.core.model.VaultEntry

/**
 * Room database entity for encrypted vault records.
 * The entire SQLite file is encrypted at rest using SQLCipher with AES-256-CBC.
 */
@Entity(tableName = "vault_entries")
data class VaultEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val folder: String,
    val tags: String, // Comma-separated tags
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): VaultEntry {
        return VaultEntry(
            id = id,
            title = title,
            username = username,
            password = password,
            url = url,
            notes = notes,
            folder = folder,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(entry: VaultEntry): VaultEntity {
            return VaultEntity(
                id = entry.id,
                title = entry.title,
                username = entry.username,
                password = entry.password,
                url = entry.url,
                notes = entry.notes,
                folder = entry.folder,
                tags = entry.tags.joinToString(","),
                isFavorite = entry.isFavorite,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt
            )
        }
    }
}
