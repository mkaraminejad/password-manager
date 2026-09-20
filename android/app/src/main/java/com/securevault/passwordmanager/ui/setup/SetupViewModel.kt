package com.securevault.passwordmanager.ui.setup

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.passwordmanager.core.security.CryptoManager
import com.securevault.passwordmanager.core.security.KeyDerivation
import com.securevault.passwordmanager.core.security.PasswordStrengthAnalyzer
import com.securevault.passwordmanager.core.security.VaultKeyHolder
import com.securevault.passwordmanager.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.spec.SecretKeySpec

data class SetupUiState(
    val passwordInput: String = "",
    val confirmInput: String = "",
    val isBiometricAvailable: Boolean = false,
    val enableBiometrics: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class SetupViewModel(
    private val appPreferences: AppPreferences,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            passwordInput = password,
            errorMessage = null
        )
    }

    fun onConfirmPasswordChanged(confirm: String) {
        _uiState.value = _uiState.value.copy(
            confirmInput = confirm,
            errorMessage = null
        )
    }

    fun onToggleBiometrics(enable: Boolean) {
        _uiState.value = _uiState.value.copy(enableBiometrics = enable)
    }

    fun completeSetup(onSuccess: () -> Unit) {
        val state = _uiState.value
        val passwordChars = state.passwordInput.toCharArray()
        val confirmChars = state.confirmInput.toCharArray()

        if (passwordChars.size < 12) {
            _uiState.value = state.copy(errorMessage = "Master password must be at least 12 characters long.")
            KeyDerivation.wipeChars(passwordChars)
            KeyDerivation.wipeChars(confirmChars)
            return
        }

        if (!passwordChars.contentEquals(confirmChars)) {
            _uiState.value = state.copy(errorMessage = "Passwords do not match.")
            KeyDerivation.wipeChars(passwordChars)
            KeyDerivation.wipeChars(confirmChars)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    val salt = KeyDerivation.generateSalt()
                    val derivedKey = KeyDerivation.deriveKey(passwordChars, salt, KeyDerivation.DEFAULT_ITERATIONS)

                    // Encrypt a sentinel verification token "SECURE_VAULT_VALID_VERIFIER" using derivedKey
                    val secretKey = SecretKeySpec(derivedKey, "AES")
                    val sentinelBytes = "SECURE_VAULT_VALID_VERIFIER".toByteArray(Charsets.UTF_8)
                    val verificationCiphertext = cryptoManager.encrypt(sentinelBytes, secretKey)

                    appPreferences.kdfSaltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
                    appPreferences.kdfIterations = KeyDerivation.DEFAULT_ITERATIONS
                    appPreferences.verificationTokenCiphertext = Base64.encodeToString(verificationCiphertext, Base64.NO_WRAP)
                    appPreferences.isBiometricEnabled = state.enableBiometrics
                    appPreferences.isMasterPasswordSet = true

                    // Load transient key into memory
                    VaultKeyHolder.setKey(derivedKey)

                    KeyDerivation.wipeBytes(derivedKey)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Setup failed: ${e.localizedMessage ?: "Unknown security error"}"
                )
            } finally {
                KeyDerivation.wipeChars(passwordChars)
                KeyDerivation.wipeChars(confirmChars)
            }
        }
    }
}
