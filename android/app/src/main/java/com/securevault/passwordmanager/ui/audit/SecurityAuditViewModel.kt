package com.securevault.passwordmanager.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.passwordmanager.core.model.VaultAuditSummary
import com.securevault.passwordmanager.core.security.PasswordStrengthAnalyzer
import com.securevault.passwordmanager.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecurityAuditViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private val _auditSummary = MutableStateFlow<VaultAuditSummary?>(null)
    val auditSummary: StateFlow<VaultAuditSummary?> = _auditSummary.asStateFlow()

    init {
        runAudit()
    }

    fun runAudit() {
        viewModelScope.launch {
            repository.getAllEntries().collect { entries ->
                _auditSummary.value = PasswordStrengthAnalyzer.auditVault(entries)
            }
        }
    }
}
