package com.securevault.passwordmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_entries ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllEntries(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): VaultEntity?

    @Query("SELECT * FROM vault_entries WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_entries WHERE folder = :folder")
    fun getEntriesByFolder(folder: String): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultEntity>)

    @Update
    suspend fun updateEntry(entry: VaultEntity)

    @Delete
    suspend fun deleteEntry(entry: VaultEntity)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_entries")
    suspend fun clearAll()
}
