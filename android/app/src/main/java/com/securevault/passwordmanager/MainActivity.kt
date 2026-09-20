package com.securevault.passwordmanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.securevault.passwordmanager.core.security.VaultKeyHolder
import com.securevault.passwordmanager.data.local.VaultDatabase
import com.securevault.passwordmanager.data.repository.VaultRepositoryImpl
import com.securevault.passwordmanager.ui.audit.SecurityAuditViewModel
import com.securevault.passwordmanager.ui.entry.EntryViewModel
import com.securevault.passwordmanager.ui.navigation.AppNavGraph
import com.securevault.passwordmanager.ui.navigation.Screen
import com.securevault.passwordmanager.ui.settings.SettingsViewModel
import com.securevault.passwordmanager.ui.setup.SetupViewModel
import com.securevault.passwordmanager.ui.theme.SecureVaultTheme
import com.securevault.passwordmanager.ui.unlock.UnlockViewModel
import com.securevault.passwordmanager.ui.vault.VaultViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var app: PasswordManagerApp
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as PasswordManagerApp

        // Security requirement 8: Prevent screenshots & hide sensitive data from app switcher preview
        if (app.appPreferences.preventScreenshots) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setupBiometricPrompt()

        val isInitialized = app.appPreferences.isMasterPasswordSet
        val startDestination = if (!isInitialized) {
            Screen.Setup.route
        } else if (!VaultKeyHolder.hasKey()) {
            Screen.Unlock.route
        } else {
            Screen.Vault.route
        }

        setContent {
            SecureVaultTheme {
                val navController = rememberNavController()

                // Create Repositories & ViewModels
                val dbKey = VaultKeyHolder.getKey() ?: ByteArray(32)
                val database = VaultDatabase.getInstance(this, dbKey)
                val repository = VaultRepositoryImpl(database.vaultDao())

                val setupViewModel = SetupViewModel(app.appPreferences, app.cryptoManager)
                val unlockViewModel = UnlockViewModel(app.appPreferences, app.cryptoManager, app.idleTimeoutTracker)
                val vaultViewModel = VaultViewModel(repository, app.secureClipboardManager, app.appPreferences, app.idleTimeoutTracker)
                val entryViewModel = EntryViewModel(repository, app.idleTimeoutTracker)
                val auditViewModel = SecurityAuditViewModel(repository)
                val settingsViewModel = SettingsViewModel(app.appPreferences, app.idleTimeoutTracker, repository)

                // Observe lock state changes to navigate back to Unlock screen automatically
                lifecycleScope.launch {
                    app.idleTimeoutTracker.isLocked.collectLatest { locked ->
                        if (locked && app.appPreferences.isMasterPasswordSet) {
                            VaultDatabase.closeDatabase()
                            navController.navigate(Screen.Unlock.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    setupViewModel = setupViewModel,
                    unlockViewModel = unlockViewModel,
                    vaultViewModel = vaultViewModel,
                    entryViewModel = entryViewModel,
                    auditViewModel = auditViewModel,
                    settingsViewModel = settingsViewModel,
                    onTriggerBiometric = { triggerBiometricUnlock(unlockViewModel) }
                )
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        app.idleTimeoutTracker.onUserActivity()
    }

    override fun onStop() {
        super.onStop()
        app.idleTimeoutTracker.onAppBackground()
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                app.idleTimeoutTracker.unlock()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    private fun triggerBiometricUnlock(unlockViewModel: UnlockViewModel) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
