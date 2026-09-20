package com.securevault.passwordmanager.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Unlock : Screen("unlock")
    object Vault : Screen("vault")
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    object EntryEdit : Screen("entry_edit?entryId={entryId}") {
        fun createRoute(entryId: String? = null) = if (entryId != null) "entry_edit?entryId=$entryId" else "entry_edit"
    }
    object SecurityAudit : Screen("security_audit")
    object Settings : Screen("settings")
}
