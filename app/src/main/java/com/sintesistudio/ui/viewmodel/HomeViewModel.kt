package com.sintesistudio.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sintesistudio.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: DocumentRepository) : ViewModel() {
    val documents = repository.observeDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    fun importDocument(uri: Uri, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            runCatching { repository.importDocument(uri) }
                .onSuccess { onSuccess(it) }
                .onFailure { _importError.value = it.message }
            _isImporting.value = false
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch { repository.deleteDocument(id) }
    }

    fun clearError() {
        _importError.value = null
    }
}

class HomeViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository) as T
}
