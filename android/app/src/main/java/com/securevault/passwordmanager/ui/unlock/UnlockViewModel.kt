package com.securevault.passwordmanager.ui.unlock

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.passwordmanager.core.security.CryptoManager
import com.securevault.passwordmanager.core.security.KeyDerivation
import com.securevault.passwordmanager.core.security.VaultKeyHolder
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.spec.SecretKeySpec

data class UnlockUiState(
    val passwordInput: String = "",
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val failedAttempts: Int = 0
)

class UnlockViewModel(
    private val appPreferences: AppPreferences,
    private val cryptoManager: CryptoManager,
    private val idleTimeoutTracker: IdleTimeoutTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UnlockUiState(isBiometricEnabled = appPreferences.isBiometricEnabled)
    )
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            passwordInput = password,
            errorMessage = null
        )
    }

    fun unlockWithPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        val passwordChars = state.passwordInput.toCharArray()

        if (passwordChars.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Please enter your master password.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val saltBase64 = appPreferences.kdfSaltBase64 ?: throw IllegalStateException("Vault not initialized")
                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
                val iterations = appPreferences.kdfIterations
                val tokenCiphertextBase64 = appPreferences.verificationTokenCiphertext
                    ?: throw IllegalStateException("Missing verification token")
                val tokenCiphertext = Base64.decode(tokenCiphertextBase64, Base64.NO_WRAP)

                val success = withContext(Dispatchers.Default) {
                    val derivedKey = KeyDerivation.deriveKey(passwordChars, salt, iterations)
                    try {
                        val secretKey = SecretKeySpec(derivedKey, "AES")
                        val decrypted = cryptoManager.decrypt(tokenCiphertext, secretKey)
                        val decryptedStr = String(decrypted, Charsets.UTF_8)
                        if (decryptedStr == "SECURE_VAULT_VALID_VERIFIER") {
                            VaultKeyHolder.setKey(derivedKey)
                            true
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    } finally {
                        KeyDerivation.wipeBytes(derivedKey)
                    }
                }

                if (success) {
                    idleTimeoutTracker.unlock()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        passwordInput = "",
                        failedAttempts = 0
                    )
                    onSuccess()
                } else {
                    val attempts = state.failedAttempts + 1
                    // Rate-limiting delay to deter local brute-force attempts
                    if (attempts > 3) {
                        delay(2000)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Incorrect master password.",
                        failedAttempts = attempts
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unlock error: ${e.localizedMessage}"
                )
            } finally {
                KeyDerivation.wipeChars(passwordChars)
            }
        }
    }

    fun onBiometricSuccess(onSuccess: () -> Unit) {
        // When hardware biometric succeeds, in production the master key wrapped via Keystore is unpacked
        idleTimeoutTracker.unlock()
        onSuccess()
    }
}
