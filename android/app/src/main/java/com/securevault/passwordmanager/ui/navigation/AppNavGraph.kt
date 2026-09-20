package com.securevault.passwordmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.securevault.passwordmanager.ui.audit.SecurityAuditScreen
import com.securevault.passwordmanager.ui.audit.SecurityAuditViewModel
import com.securevault.passwordmanager.ui.entry.EntryDetailEditScreen
import com.securevault.passwordmanager.ui.entry.EntryViewModel
import com.securevault.passwordmanager.ui.settings.SettingsScreen
import com.securevault.passwordmanager.ui.settings.SettingsViewModel
import com.securevault.passwordmanager.ui.setup.SetupScreen
import com.securevault.passwordmanager.ui.setup.SetupViewModel
import com.securevault.passwordmanager.ui.unlock.UnlockScreen
import com.securevault.passwordmanager.ui.unlock.UnlockViewModel
import com.securevault.passwordmanager.ui.vault.VaultScreen
import com.securevault.passwordmanager.ui.vault.VaultViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    setupViewModel: SetupViewModel,
    unlockViewModel: UnlockViewModel,
    vaultViewModel: VaultViewModel,
    entryViewModel: EntryViewModel,
    auditViewModel: SecurityAuditViewModel,
    settingsViewModel: SettingsViewModel,
    onTriggerBiometric: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                viewModel = setupViewModel,
                onSetupComplete = {
                    navController.navigate(Screen.Vault.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Unlock.route) {
            UnlockScreen(
                viewModel = unlockViewModel,
                onTriggerBiometric = onTriggerBiometric,
                onUnlockSuccess = {
                    navController.navigate(Screen.Vault.route) {
                        popUpTo(Screen.Unlock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Vault.route) {
            VaultScreen(
                viewModel = vaultViewModel,
                onNavigateToAddEntry = {
                    entryViewModel.loadEntry(null)
                    navController.navigate(Screen.EntryEdit.createRoute(null))
                },
                onNavigateToEditEntry = { entryId ->
                    entryViewModel.loadEntry(entryId)
                    navController.navigate(Screen.EntryEdit.createRoute(entryId))
                },
                onNavigateToAudit = {
                    auditViewModel.runAudit()
                    navController.navigate(Screen.SecurityAudit.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.EntryEdit.route,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            EntryDetailEditScreen(
                viewModel = entryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SecurityAudit.route) {
            SecurityAuditScreen(
                viewModel = auditViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
