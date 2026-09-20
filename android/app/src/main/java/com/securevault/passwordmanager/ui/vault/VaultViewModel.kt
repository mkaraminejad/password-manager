package com.securevault.passwordmanager.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.passwordmanager.core.model.VaultEntry
import com.securevault.passwordmanager.core.security.SecureClipboardManager
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.data.preferences.AppPreferences
import com.securevault.passwordmanager.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    FAVORITES_FIRST,
    RECENTLY_UPDATED,
    TITLE_A_TO_Z
}

data class VaultUiState(
    val entries: List<VaultEntry> = emptyList(),
    val filteredEntries: List<VaultEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedFolder: String = "All",
    val availableFolders: List<String> = listOf("All"),
    val sortOrder: SortOrder = SortOrder.FAVORITES_FIRST,
    val unmaskedEntryIds: Set<String> = emptySet(),
    val toastMessage: String? = null
)

class VaultViewModel(
    private val repository: VaultRepository,
    private val clipboardManager: SecureClipboardManager,
    private val appPreferences: AppPreferences,
    private val idleTimeoutTracker: IdleTimeoutTracker
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolder = MutableStateFlow("All")
    private val _sortOrder = MutableStateFlow(SortOrder.FAVORITES_FIRST)
    private val _unmaskedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VaultUiState> = combine(
        repository.getAllEntries(),
        _searchQuery,
        _selectedFolder,
        _sortOrder,
        _unmaskedIds,
        _toastMessage
    ) { rawEntries, query, folder, sort, unmasked, toast ->
        idleTimeoutTracker.onUserActivity()

        val folders = listOf("All") + rawEntries.mapNotNull { it.folder.ifBlank { null } }.distinct().sorted()

        var filtered = if (query.isBlank()) {
            rawEntries
        } else {
            rawEntries.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }

        if (folder != "All") {
            filtered = filtered.filter { it.folder.equals(folder, ignoreCase = true) }
        }

        val sorted = when (sort) {
            SortOrder.FAVORITES_FIRST -> filtered.sortedWith(
                compareByDescending<VaultEntry> { it.isFavorite }
                    .thenByDescending { it.updatedAt }
            )
            SortOrder.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            SortOrder.TITLE_A_TO_Z -> filtered.sortedBy { it.title.lowercase() }
        }

        VaultUiState(
            entries = rawEntries,
            filteredEntries = sorted,
            searchQuery = query,
            selectedFolder = folder,
            availableFolders = folders,
            sortOrder = sort,
            unmaskedEntryIds = unmasked,
            toastMessage = toast
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        idleTimeoutTracker.onUserActivity()
    }

    fun onSelectFolder(folder: String) {
        _selectedFolder.value = folder
        idleTimeoutTracker.onUserActivity()
    }

    fun onSortOrderChanged(order: SortOrder) {
        _sortOrder.value = order
        idleTimeoutTracker.onUserActivity()
    }

    fun togglePasswordVisibility(entryId: String) {
        idleTimeoutTracker.onUserActivity()
        val current = _unmaskedIds.value.toMutableSet()
        if (current.contains(entryId)) {
            current.remove(entryId)
        } else {
            current.add(entryId)
        }
        _unmaskedIds.value = current
    }

    fun copyPassword(entry: VaultEntry) {
        idleTimeoutTracker.onUserActivity()
        val timeout = appPreferences.clipboardTimeoutSeconds
        clipboardManager.copyToClipboard("Password", entry.password, timeout)
        _toastMessage.value = "Password copied. Auto-clearing in ${timeout}s"
    }

    fun copyUsername(entry: VaultEntry) {
        idleTimeoutTracker.onUserActivity()
        clipboardManager.copyToClipboard("Username", entry.username, 60)
        _toastMessage.value = "Username copied"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun duplicateEntry(entry: VaultEntry) {
        idleTimeoutTracker.onUserActivity()
        viewModelScope.launch {
            repository.duplicateEntry(entry.id)
            _toastMessage.value = "Entry duplicated"
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        idleTimeoutTracker.onUserActivity()
        viewModelScope.launch {
            repository.deleteEntry(entry)
            _toastMessage.value = "Entry deleted"
        }
    }

    fun toggleFavorite(entry: VaultEntry) {
        idleTimeoutTracker.onUserActivity()
        viewModelScope.launch {
            repository.updateEntry(entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis()))
        }
    }

    fun lockVaultNow() {
        idleTimeoutTracker.lockVault()
    }
}
