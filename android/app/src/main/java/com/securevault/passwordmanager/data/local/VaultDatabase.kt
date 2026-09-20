package com.securevault.passwordmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportFactory

@Database(entities = [VaultEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao

    companion object {
        private const val DB_NAME = "secure_vault_encrypted.db"

        @Volatile
        private var INSTANCE: VaultDatabase? = null

        /**
         * Builds an instance of RoomDatabase backed by SQLCipher using the provided byte passphrase.
         * The passphrase is never stored on disk.
         */
        fun getInstance(context: Context, passphraseBytes: ByteArray): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext, passphraseBytes).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context, passphraseBytes: ByteArray): VaultDatabase {
            val factory = SupportFactory(passphraseBytes)
            return Room.databaseBuilder(context, VaultDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Closes the active database connection and resets the singleton instance upon vault lock.
         */
        @Synchronized
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
