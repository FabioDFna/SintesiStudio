package com.sintesistudio.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sintesistudio.data.entity.ChatMessageEntity
import com.sintesistudio.data.entity.DocumentEntity
import com.sintesistudio.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(
    private val repository: DocumentRepository,
    private val documentId: Long
) : ViewModel() {

    private val _document = MutableStateFlow<DocumentEntity?>(null)
    val document: StateFlow<DocumentEntity?> = _document.asStateFlow()

    val messages = repository.observeMessages(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _document.value = repository.getDocument(documentId)
        }
    }

    fun generateSummary() = runAi { repository.generateSummary(documentId) }

    fun generateAudio() = runAi { repository.generateAudioScript(documentId) }

    fun ask(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.ask(documentId, question) }
                .onFailure { error.value = it.message }
            loading.value = false
            refresh()
        }
    }

    private fun runAi(block: suspend () -> DocumentEntity) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { block() }
                .onSuccess { _document.value = it }
                .onFailure { error.value = it.message }
            loading.value = false
        }
    }
}

class DocumentViewModelFactory(
    private val repository: DocumentRepository,
    private val documentId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DocumentViewModel(repository, documentId) as T
}
