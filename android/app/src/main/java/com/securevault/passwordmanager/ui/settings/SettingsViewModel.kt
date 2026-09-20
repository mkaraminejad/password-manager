package com.securevault.passwordmanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.passwordmanager.core.security.KeyDerivation
import com.securevault.passwordmanager.core.security.SecureBackupManager
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.data.preferences.AppPreferences
import com.securevault.passwordmanager.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val idleTimeoutMinutes: Int = 5,
    val clipboardTimeoutSeconds: Int = 30,
    val biometricEnabled: Boolean = false,
    val preventScreenshots: Boolean = true,
    val statusMessage: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false
)

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val idleTimeoutTracker: IdleTimeoutTracker,
    private val repository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            idleTimeoutMinutes = appPreferences.idleTimeoutMinutes,
            clipboardTimeoutSeconds = appPreferences.clipboardTimeoutSeconds,
            biometricEnabled = appPreferences.isBiometricEnabled,
            preventScreenshots = appPreferences.preventScreenshots
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateIdleTimeout(minutes: Int) {
        appPreferences.idleTimeoutMinutes = minutes
        idleTimeoutTracker.updateTimeoutSetting(minutes)
        _uiState.value = _uiState.value.copy(idleTimeoutMinutes = minutes)
    }

    fun updateClipboardTimeout(seconds: Int) {
        appPreferences.clipboardTimeoutSeconds = seconds
        _uiState.value = _uiState.value.copy(clipboardTimeoutSeconds = seconds)
    }

    fun toggleBiometric(enabled: Boolean) {
        appPreferences.isBiometricEnabled = enabled
        _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
    }

    fun togglePreventScreenshots(enabled: Boolean) {
        appPreferences.preventScreenshots = enabled
        _uiState.value = _uiState.value.copy(preventScreenshots = enabled)
    }

    fun exportEncryptedBackup(passphrase: String, onFileReady: (ByteArray) -> Unit) {
        val passChars = passphrase.toCharArray()
        if (passChars.size < 8) {
            _uiState.value = _uiState.value.copy(statusMessage = "Backup passphrase must be at least 8 characters")
            KeyDerivation.wipeChars(passChars)
            return
        }

        _uiState.value = _uiState.value.copy(isExporting = true)
        viewModelScope.launch {
            try {
                val entries = repository.getAllEntries().first()
                val backupBytes = SecureBackupManager.exportEncryptedBackup(entries, passChars)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    statusMessage = "Encrypted backup generated successfully (${backupBytes.size} bytes)"
                )
                onFileReady(backupBytes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    statusMessage = "Export failed: ${e.localizedMessage}"
                )
            } finally {
                KeyDerivation.wipeChars(passChars)
            }
        }
    }

    fun importEncryptedBackup(backupBytes: ByteArray, passphrase: String) {
        val passChars = passphrase.toCharArray()
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            try {
                val entries = SecureBackupManager.importEncryptedBackup(backupBytes, passChars)
                repository.insertAll(entries)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    statusMessage = "Successfully imported ${entries.size} credentials"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    statusMessage = "Import failed: Invalid password or corrupt file"
                )
            } finally {
                KeyDerivation.wipeChars(passChars)
            }
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
