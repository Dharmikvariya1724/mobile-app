package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaEntity
import com.example.data.model.SortOption
import com.example.data.model.VaultConfigEntity
import com.example.data.repository.AuthResult
import com.example.data.repository.MediaRepository
import com.example.data.repository.VaultRepository
import com.example.security.VaultSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val isUnlocked: StateFlow<Boolean> = VaultSession.isUnlocked

    private val _isInitialized = MutableStateFlow<Boolean?>(null)
    val isInitialized: StateFlow<Boolean?> = _isInitialized.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _lockoutSeconds = MutableStateFlow<Long>(0L)
    val lockoutSeconds: StateFlow<Long> = _lockoutSeconds.asStateFlow()

    private var lockoutJob: Job? = null

    val config: StateFlow<VaultConfigEntity?> = vaultRepository.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val albums: StateFlow<List<AlbumEntity>> = mediaRepository.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVaultSize: StateFlow<Long> = mediaRepository.getTotalVaultSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val photoCount: StateFlow<Int> = mediaRepository.getPhotoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val videoCount: StateFlow<Int> = mediaRepository.getVideoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaIds: StateFlow<Set<Long>> = _selectedMediaIds.asStateFlow()

    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    val allMedia: StateFlow<List<MediaEntity>> = combine(
        mediaRepository.getAllMedia(),
        _searchQuery,
        _sortOption
    ) { list, query, sort ->
        val filtered = if (query.isBlank()) list else list.filter {
            it.originalDisplayName.contains(query, ignoreCase = true)
        }
        sortMedia(filtered, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkInitialization()
    }

    fun checkInitialization() {
        viewModelScope.launch {
            val initialized = vaultRepository.isVaultInitialized()
            _isInitialized.value = initialized
            val currentConfig = vaultRepository.getConfig()
            if (currentConfig != null) {
                VaultSession.autoLockTimeoutSeconds = currentConfig.autoLockTimeoutSeconds
                val now = System.currentTimeMillis()
                if (currentConfig.lockoutUntilTimestamp > now) {
                    startLockoutCountdown((currentConfig.lockoutUntilTimestamp - now) / 1000 + 1)
                }
            }
        }
    }

    fun setupPassword(pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            vaultRepository.setupVault(pin)
                .onSuccess {
                    _isInitialized.value = true
                    _authError.value = null
                    onDone()
                }
                .onFailure {
                    _authError.value = it.message ?: "Failed to initialize vault"
                }
        }
    }

    fun login(pin: String) {
        if (_lockoutSeconds.value > 0) return
        viewModelScope.launch {
            _authError.value = null
            when (val result = vaultRepository.authenticate(pin)) {
                is AuthResult.Success -> {
                    _authError.value = null
                }
                is AuthResult.InvalidPassword -> {
                    _authError.value = "Incorrect PIN. ${result.remainingAttempts} attempt(s) remaining."
                }
                is AuthResult.LockedOut -> {
                    startLockoutCountdown(result.remainingSeconds)
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
        }
    }

    fun loginBiometric(onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = vaultRepository.authenticateWithBiometric()) {
                is AuthResult.Success -> {
                    _authError.value = null
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                }
                else -> {}
            }
        }
    }

    private fun startLockoutCountdown(seconds: Long) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var s = seconds
            while (s > 0) {
                _lockoutSeconds.value = s
                _authError.value = "Too many failed attempts. Locked out for ${s}s."
                delay(1000)
                s--
            }
            _lockoutSeconds.value = 0
            _authError.value = null
        }
    }

    fun lock() {
        VaultSession.lock()
        clearSelection()
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        clearSelection()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // --- Multi-Selection ---

    fun toggleSelection(id: Long) {
        val current = _selectedMediaIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedMediaIds.value = current
    }

    fun selectAll(items: List<MediaEntity>) {
        _selectedMediaIds.value = items.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
    }

    // --- Media Actions ---

    fun importMediaUris(context: Context, uris: List<Uri>, albumId: Long? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importProgress.value = Pair(0, uris.size)
            mediaRepository.importMediaList(context, uris, albumId) { current, total ->
                _importProgress.value = Pair(current, total)
            }
            _importProgress.value = null
        }
    }

    fun toggleFavorite(media: MediaEntity) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(media)
        }
    }

    fun deleteSelectedItems(context: Context) {
        val selectedIds = _selectedMediaIds.value
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val itemsToDelete = allMedia.value.filter { selectedIds.contains(it.id) }
            mediaRepository.deleteMediaItems(context, itemsToDelete)
            clearSelection()
        }
    }

    fun deleteSingleItem(context: Context, media: MediaEntity) {
        viewModelScope.launch {
            mediaRepository.deleteMediaItems(context, listOf(media))
        }
    }

    fun moveSelectedToAlbum(albumId: Long?) {
        val selectedIds = _selectedMediaIds.value.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.moveItemsToAlbum(selectedIds, albumId)
            clearSelection()
        }
    }

    // --- Album Actions ---

    fun createAlbum(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mediaRepository.createAlbum(name)
        }
    }

    fun renameAlbum(album: AlbumEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            mediaRepository.renameAlbum(album, newName)
        }
    }

    fun deleteAlbum(context: Context, album: AlbumEntity, deleteContainedMedia: Boolean) {
        viewModelScope.launch {
            mediaRepository.deleteAlbum(context, album, deleteContainedMedia)
        }
    }

    // --- Settings Actions ---

    fun changePassword(currentPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            vaultRepository.changePassword(currentPass, newPass)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Failed to change password") }
        }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            vaultRepository.setBiometricEnabled(enabled)
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        viewModelScope.launch {
            vaultRepository.setAutoLockTimeout(seconds)
        }
    }

    fun wipeAllVaultData(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            vaultRepository.wipeAllVaultData(context)
            _isInitialized.value = false
            onSuccess()
        }
    }

    private fun sortMedia(list: List<MediaEntity>, sort: SortOption): List<MediaEntity> {
        return when (sort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.createdAt }
            SortOption.DATE_ASC -> list.sortedBy { it.createdAt }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.fileSize }
            SortOption.SIZE_ASC -> list.sortedBy { it.fileSize }
            SortOption.NAME_ASC -> list.sortedBy { it.originalDisplayName.lowercase() }
        }
    }
}

class VaultViewModelFactory(
    private val vaultRepository: VaultRepository,
    private val mediaRepository: MediaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VaultViewModel(vaultRepository, mediaRepository) as T
    }
}
