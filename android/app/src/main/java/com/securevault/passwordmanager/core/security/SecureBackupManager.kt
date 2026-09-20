package com.securevault.passwordmanager.core.security

import com.securevault.passwordmanager.core.model.VaultEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted export and import engine.
 * Never writes unencrypted plaintext credentials to external storage.
 * Uses authenticated AES-256-GCM encryption with PBKDF2 key derivation from a user-specified password.
 */
object SecureBackupManager {

    private const val MAGIC_HEADER = 0x53564C54 // "SVLT"
    private const val BACKUP_VERSION = 1
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 32

    private val secureRandom = SecureRandom()

    /**
     * Serializes entries to an encrypted binary envelope.
     */
    fun exportEncryptedBackup(
        entries: List<VaultEntry>,
        passphraseChars: CharArray
    ): ByteArray {
        val jsonArray = JSONArray()
        for (entry in entries) {
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("username", entry.username)
                put("password", entry.password)
                put("url", entry.url)
                put("notes", entry.notes)
                put("folder", entry.folder)
                put("isFavorite", entry.isFavorite)
                put("createdAt", entry.createdAt)
                put("updatedAt", entry.updatedAt)
                put("tags", JSONArray(entry.tags))
            }
            jsonArray.put(obj)
        }

        val plaintextBytes = jsonArray.toString().toByteArray(StandardCharsets.UTF_8)

        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        val iterations = KeyDerivation.DEFAULT_ITERATIONS

        val derivedKey = KeyDerivation.deriveKey(passphraseChars, salt, iterations)
        val secretKey = SecretKeySpec(derivedKey, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintextBytes)

        KeyDerivation.wipeBytes(derivedKey)

        val byteStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteStream)
        dataOut.writeInt(MAGIC_HEADER)
        dataOut.writeShort(BACKUP_VERSION)
        dataOut.writeInt(iterations)
        dataOut.writeInt(salt.size)
        dataOut.write(salt)
        dataOut.writeInt(iv.size)
        dataOut.write(iv)
        dataOut.writeInt(ciphertext.size)
        dataOut.write(ciphertext)
        dataOut.flush()

        return byteStream.toByteArray()
    }

    /**
     * Decrypts an encrypted binary backup and returns restored VaultEntries.
     * Throws an exception if the passphrase is wrong or the file is corrupted.
     */
    fun importEncryptedBackup(
        backupBytes: ByteArray,
        passphraseChars: CharArray
    ): List<VaultEntry> {
        val dataIn = DataInputStream(ByteArrayInputStream(backupBytes))

        val magic = dataIn.readInt()
        if (magic != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid backup file: header mismatch")
        }

        val version = dataIn.readShort().toInt()
        if (version > BACKUP_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        val iterations = dataIn.readInt()
        val saltSize = dataIn.readInt()
        val salt = ByteArray(saltSize)
        dataIn.readFully(salt)

        val ivSize = dataIn.readInt()
        val iv = ByteArray(ivSize)
        dataIn.readFully(iv)

        val ciphertextSize = dataIn.readInt()
        val ciphertext = ByteArray(ciphertextSize)
        dataIn.readFully(ciphertext)

        val derivedKey = KeyDerivation.deriveKey(passphraseChars, salt, iterations)
        val secretKey = SecretKeySpec(derivedKey, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val decryptedBytes = cipher.doFinal(ciphertext)
        KeyDerivation.wipeBytes(derivedKey)

        val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)
        val jsonArray = JSONArray(jsonString)

        val entries = mutableListOf<VaultEntry>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val tagsArray = obj.optJSONArray("tags")
            val tagsList = mutableListOf<String>()
            if (tagsArray != null) {
                for (j in 0 until tagsArray.length()) {
                    tagsList.add(tagsArray.getString(j))
                }
            }

            entries.add(
                VaultEntry(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    username = obj.optString("username", ""),
                    password = obj.getString("password"),
                    url = obj.optString("url", ""),
                    notes = obj.optString("notes", ""),
                    folder = obj.optString("folder", ""),
                    tags = tagsList,
                    isFavorite = obj.optBoolean("isFavorite", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        return entries
    }
}
