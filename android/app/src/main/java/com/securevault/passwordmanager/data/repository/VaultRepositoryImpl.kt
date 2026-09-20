package com.securevault.passwordmanager.data.repository

import com.securevault.passwordmanager.core.model.VaultEntry
import com.securevault.passwordmanager.data.local.VaultDao
import com.securevault.passwordmanager.data.local.VaultEntity
import com.securevault.passwordmanager.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class VaultRepositoryImpl(
    private val vaultDao: VaultDao
) : VaultRepository {

    override fun getAllEntries(): Flow<List<VaultEntry>> {
        return vaultDao.getAllEntries().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: String): VaultEntry? {
        return vaultDao.getEntryById(id)?.toDomain()
    }

    override fun searchEntries(query: String): Flow<List<VaultEntry>> {
        return vaultDao.searchEntries(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEntriesByFolder(folder: String): Flow<List<VaultEntry>> {
        return vaultDao.getEntriesByFolder(folder).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertEntry(entry: VaultEntry) {
        vaultDao.insertEntry(VaultEntity.fromDomain(entry))
    }

    override suspend fun insertAll(entries: List<VaultEntry>) {
        vaultDao.insertAll(entries.map { VaultEntity.fromDomain(it) })
    }

    override suspend fun updateEntry(entry: VaultEntry) {
        vaultDao.updateEntry(VaultEntity.fromDomain(entry))
    }

    override suspend fun deleteEntry(entry: VaultEntry) {
        vaultDao.deleteEntry(VaultEntity.fromDomain(entry))
    }

    override suspend fun deleteById(id: String) {
        vaultDao.deleteById(id)
    }

    override suspend fun duplicateEntry(entryId: String) {
        val existing = vaultDao.getEntryById(entryId)?.toDomain() ?: return
        val duplicate = existing.copy(
            id = UUID.randomUUID().toString(),
            title = "${existing.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        insertEntry(duplicate)
    }
}
