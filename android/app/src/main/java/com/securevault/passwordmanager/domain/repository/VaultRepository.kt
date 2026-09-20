package com.securevault.passwordmanager.domain.repository

import com.securevault.passwordmanager.core.model.VaultEntry
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun getAllEntries(): Flow<List<VaultEntry>>
    suspend fun getEntryById(id: String): VaultEntry?
    fun searchEntries(query: String): Flow<List<VaultEntry>>
    fun getEntriesByFolder(folder: String): Flow<List<VaultEntry>>
    suspend fun insertEntry(entry: VaultEntry)
    suspend fun insertAll(entries: List<VaultEntry>)
    suspend fun updateEntry(entry: VaultEntry)
    suspend fun deleteEntry(entry: VaultEntry)
    suspend fun deleteById(id: String)
    suspend fun duplicateEntry(entryId: String)
}
