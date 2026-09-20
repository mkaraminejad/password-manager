package com.securevault.passwordmanager.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores app configuration using EncryptedSharedPreferences.
 * Sensitive flags, salts, and password verification tokens are encrypted at rest with hardware keys.
 */
class AppPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_vault_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var isMasterPasswordSet: Boolean
        get() = prefs.getBoolean(KEY_IS_INITIALIZED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_INITIALIZED, value).apply()

    var kdfSaltBase64: String?
        get() = prefs.getString(KEY_KDF_SALT, null)
        set(value) = prefs.edit().putString(KEY_KDF_SALT, value).apply()

    var kdfIterations: Int
        get() = prefs.getInt(KEY_KDF_ITERATIONS, 120_000)
        set(value) = prefs.edit().putInt(KEY_KDF_ITERATIONS, value).apply()

    /**
     * A known verification ciphertext encrypted with the derived key.
     * When unlocking, if this ciphertext decrypts to the expected sentinel token,
     * the entered master password is confirmed correct without storing the master password.
     */
    var verificationTokenCiphertext: String?
        get() = prefs.getString(KEY_VERIFIER_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_VERIFIER_TOKEN, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var biometricWrappedKey: String?
        get() = prefs.getString(KEY_BIOMETRIC_WRAPPED_KEY, null)
        set(value) = prefs.edit().putString(KEY_BIOMETRIC_WRAPPED_KEY, value).apply()

    var idleTimeoutMinutes: Int
        get() = prefs.getInt(KEY_IDLE_TIMEOUT_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_IDLE_TIMEOUT_MINUTES, value).apply()

    var clipboardTimeoutSeconds: Int
        get() = prefs.getInt(KEY_CLIPBOARD_TIMEOUT_SECONDS, 30)
        set(value) = prefs.edit().putInt(KEY_CLIPBOARD_TIMEOUT_SECONDS, value).apply()

    var preventScreenshots: Boolean
        get() = prefs.getBoolean(KEY_PREVENT_SCREENSHOTS, true)
        set(value) = prefs.edit().putBoolean(KEY_PREVENT_SCREENSHOTS, value).apply()

    companion object {
        private const val KEY_IS_INITIALIZED = "is_initialized"
        private const val KEY_KDF_SALT = "kdf_salt"
        private const val KEY_KDF_ITERATIONS = "kdf_iterations"
        private const val KEY_VERIFIER_TOKEN = "verifier_token"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIOMETRIC_WRAPPED_KEY = "biometric_wrapped_key"
        private const val KEY_IDLE_TIMEOUT_MINUTES = "idle_timeout_minutes"
        private const val KEY_CLIPBOARD_TIMEOUT_SECONDS = "clipboard_timeout_seconds"
        private const val KEY_PREVENT_SCREENSHOTS = "prevent_screenshots"
    }
}
